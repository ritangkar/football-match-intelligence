package com.ritangkar.footballintel.model;

import java.util.List;

/**
 * Aggregated form for one team over its most recent N matches.
 */
public record TeamFormSummary(
        String teamId,
        String teamName,
        int matchesConsidered,
        int wins,
        int draws,
        int losses,
        int points,
        int goalsFor,
        int goalsAgainst,
        int goalDifference,
        String formString,          // e.g. "WWDLW", most recent last
        double avgPossessionPct,
        double shotAccuracyPct,     // shots on target / shots, across the window
        double homeGoalsPerMatch,
        double awayGoalsPerMatch,
        List<TeamFormEntry> recentMatches,
        List<String> insights
) {
}
