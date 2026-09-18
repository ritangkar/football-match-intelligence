package com.ritangkar.footballintel.model;

import java.time.LocalDate;

/**
 * One match from a team's recent-form list, already resolved to that team's
 * point of view (venue, result, goals for/against).
 */
public record TeamFormEntry(
        String matchId,
        LocalDate matchDate,
        String opponentId,
        String opponentName,
        String venue,      // "HOME" or "AWAY"
        char result,       // 'W', 'D' or 'L'
        int goalsFor,
        int goalsAgainst,
        int points
) {
}
