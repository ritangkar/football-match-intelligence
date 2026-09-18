package com.ritangkar.footballintel.web;

import com.ritangkar.footballintel.model.Team;
import com.ritangkar.footballintel.model.TeamFormSummary;
import com.ritangkar.footballintel.repository.FootballDataRepository;
import com.ritangkar.footballintel.service.FootballAnalyticsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final FootballDataRepository repository;
    private final FootballAnalyticsService analyticsService;

    public TeamController(FootballDataRepository repository, FootballAnalyticsService analyticsService) {
        this.repository = repository;
        this.analyticsService = analyticsService;
    }

    /** GET /api/teams — list every team in the synthetic league. */
    @GetMapping
    public List<Team> listTeams() {
        return repository.findAllTeams();
    }

    /** GET /api/teams/{teamId}/form?lastN=5 — recent form summary + insights. */
    @GetMapping("/{teamId}/form")
    public TeamFormSummary teamForm(@PathVariable String teamId,
                                     @RequestParam(defaultValue = "5") int lastN) {
        int bounded = Math.max(1, Math.min(lastN, 20));
        return analyticsService.teamForm(teamId, bounded);
    }
}
