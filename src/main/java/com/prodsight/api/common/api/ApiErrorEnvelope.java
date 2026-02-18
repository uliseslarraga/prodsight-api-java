package com.prodsight.api.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorEnvelope(ApiError error) {

  public record ApiError(
      String code,
      String message,
      String traceId,
      Map<String, Object> details
  ) {}
}
