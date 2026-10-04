package com.ritangkar.footballintel.web;

import com.ritangkar.footballintel.model.HeadToHead;
import com.ritangkar.footballintel.service.FootballAnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HeadToHeadController {

    private final FootballAnalyticsService analyticsService;

    public HeadToHeadController(FootballAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /** GET /api/head-to-head?teamA=T01&teamB=T02 */
    @GetMapping("/api/head-to-head")
    public HeadToHead headToHead(@RequestParam String teamA, @RequestParam String teamB) {
        if (teamA.equals(teamB)) {
            throw new IllegalArgumentException("teamA and teamB must be different teams");
        }
        return analyticsService.headToHead(teamA, teamB);
    }
}
