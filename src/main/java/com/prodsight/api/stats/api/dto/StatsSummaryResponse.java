package com.prodsight.api.stats.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StatsSummaryResponse(
    UUID userId,
    Instant from,
    Instant to,
    Totals totals,
    List<ByType> byType,
    List<SeriesPoint> series
) {
  public record Totals(long eventCount, long durationSeconds) {}
  public record ByType(String type, long eventCount, long durationSeconds) {}
  public record SeriesPoint(Instant bucketStart, long eventCount, long durationSeconds) {}
}
