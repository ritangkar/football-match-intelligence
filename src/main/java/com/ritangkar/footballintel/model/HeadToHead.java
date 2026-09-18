package com.ritangkar.footballintel.model;

import java.util.List;

/**
 * Historical comparison between two teams across every meeting in the dataset.
 */
public record HeadToHead(
        String teamAId,
        String teamAName,
        String teamBId,
        String teamBName,
        int meetings,
        int teamAWins,
        int teamBWins,
        int draws,
        int teamAGoals,
        int teamBGoals,
        List<TeamFormEntry> meetingsList,
        List<String> insights
) {
}
