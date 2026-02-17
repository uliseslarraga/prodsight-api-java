package com.prodsight.api.idempotency.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodsight.api.common.exception.ConflictException;
import com.prodsight.api.common.util.Hashing;
import com.prodsight.api.idempotency.persistence.IdempotencyKeyEntity;
import com.prodsight.api.idempotency.persistence.IdempotencyKeyRepository;
import com.prodsight.api.users.persistence.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyService {

  public record StoredResponse(int statusCode, Map<String, Object> body) {}

  private final IdempotencyKeyRepository repo;
  private final ObjectMapper objectMapper;

  public IdempotencyService(IdempotencyKeyRepository repo, ObjectMapper objectMapper) {
    this.repo = repo;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public Optional<StoredResponse> findIfExists(UUID userId, String idempotencyKey, Object requestBodyForHash) {
    String hash = requestHash(requestBodyForHash);
    return repo.findByUserIdAndIdempotencyKey(userId, idempotencyKey).map(r -> {
      if (!r.getRequestHash().equals(hash)) {
        throw new ConflictException("Idempotency-Key reused with a different payload");
      }
      return new StoredResponse(r.getResponseCode(), r.getResponseBody());
    });
  }

  @Transactional
  public void store(UserEntity user, String idempotencyKey, Object requestBodyForHash, int statusCode, Map<String, Object> responseBody) {
    IdempotencyKeyEntity rec = new IdempotencyKeyEntity();
    rec.setUser(user);
    rec.setIdempotencyKey(idempotencyKey);
    rec.setRequestHash(requestHash(requestBodyForHash));
    rec.setResponseCode(statusCode);
    rec.setResponseBody(responseBody);
    repo.save(rec);
  }

  private String requestHash(Object body) {
    try {
      // This is “good enough” canonicalization for MVP.
      String json = objectMapper.writeValueAsString(body);
      return Hashing.sha256Hex(json);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to hash request body", e);
    }
  }
}
