package com.prodsight.api.stats.api;

import com.prodsight.api.stats.api.dto.StatsSummaryResponse;
import com.prodsight.api.stats.service.StatsService;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{userId}/stats")
public class StatsController {

  private final StatsService statsService;

  public StatsController(StatsService statsService) {
    this.statsService = statsService;
  }

  @GetMapping("/summary")
  public StatsSummaryResponse summary(
      @PathVariable UUID userId,
      @RequestParam Instant from,
      @RequestParam Instant to,
      @RequestParam(defaultValue = "day") String groupBy,
      @RequestParam(required = false) List<String> type
  ) {
    return statsService.summary(userId, from, to, groupBy, type);
  }
}
