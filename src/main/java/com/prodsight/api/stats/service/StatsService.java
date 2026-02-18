package com.prodsight.api.stats.service;

import com.prodsight.api.stats.api.dto.StatsSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class StatsService {

  @Transactional(readOnly = true)
  public StatsSummaryResponse summary(UUID userId, Instant from, Instant to, String groupBy, List<String> types) {
    // TODO: implement with JPQL/native queries; start with simple totals + byType
    return new StatsSummaryResponse(
        userId, from, to,
        new StatsSummaryResponse.Totals(0, 0),
        List.of(),
        List.of()
    );
  }
}
