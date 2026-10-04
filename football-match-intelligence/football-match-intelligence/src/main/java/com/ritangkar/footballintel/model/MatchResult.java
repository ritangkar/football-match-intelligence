package com.ritangkar.footballintel.model;

import java.time.LocalDate;

/**
 * A single completed fixture. Loaded verbatim from sample-data/matches.json.
 * Possession figures are percentages that sum to 100 for a given match.
 */
public record MatchResult(
        String id,
        LocalDate matchDate,
        String homeTeamId,
        String awayTeamId,
        int homeGoals,
        int awayGoals,
        int homeShots,
        int awayShots,
        int homeShotsOnTarget,
        int awayShotsOnTarget,
        int homePossession,
        int awayPossession
) {

    public boolean involves(String teamId) {
        return homeTeamId.equals(teamId) || awayTeamId.equals(teamId);
    }

    public String opponentOf(String teamId) {
        if (homeTeamId.equals(teamId)) return awayTeamId;
        if (awayTeamId.equals(teamId)) return homeTeamId;
        throw new IllegalArgumentException("Team " + teamId + " did not play in match " + id);
    }

    public boolean isHomeTeam(String teamId) {
        return homeTeamId.equals(teamId);
    }

    public int goalsFor(String teamId) {
        return isHomeTeam(teamId) ? homeGoals : awayGoals;
    }

    public int goalsAgainst(String teamId) {
        return isHomeTeam(teamId) ? awayGoals : homeGoals;
    }

    public int possessionOf(String teamId) {
        return isHomeTeam(teamId) ? homePossession : awayPossession;
    }

    public int shotsOf(String teamId) {
        return isHomeTeam(teamId) ? homeShots : awayShots;
    }

    public int shotsOnTargetOf(String teamId) {
        return isHomeTeam(teamId) ? homeShotsOnTarget : awayShotsOnTarget;
    }

    /** W, D or L from the perspective of the given team. */
    public char resultFor(String teamId) {
        int gf = goalsFor(teamId);
        int ga = goalsAgainst(teamId);
        if (gf > ga) return 'W';
        if (gf < ga) return 'L';
        return 'D';
    }

    public int pointsFor(String teamId) {
        return switch (resultFor(teamId)) {
            case 'W' -> 3;
            case 'D' -> 1;
            default -> 0;
        };
    }
}
