package com.ritangkar.footballintel.web;

import com.ritangkar.footballintel.model.MatchResult;
import com.ritangkar.footballintel.repository.FootballDataRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final FootballDataRepository repository;

    public MatchController(FootballDataRepository repository) {
        this.repository = repository;
    }

    /** GET /api/matches — every match in the synthetic season. */
    @GetMapping
    public List<MatchResult> listMatches() {
        return repository.findAllMatches();
    }

    /** GET /api/matches/team/{teamId} — every match involving one team, oldest first. */
    @GetMapping("/team/{teamId}")
    public List<MatchResult> matchesForTeam(@PathVariable String teamId) {
        return repository.findMatchesForTeam(teamId);
    }
}
