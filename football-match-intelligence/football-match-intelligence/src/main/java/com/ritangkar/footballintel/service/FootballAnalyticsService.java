package com.ritangkar.footballintel.service;

import com.ritangkar.footballintel.model.*;
import com.ritangkar.footballintel.repository.FootballDataRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Turns raw match results into team form summaries, head-to-head comparisons
 * and short, data-grounded insight sentences.
 *
 * Every number in an insight can be traced back to the match list that
 * produced it — nothing here is guessed or LLM-generated.
 */
@Service
public class FootballAnalyticsService {

    private final FootballDataRepository repository;

    public FootballAnalyticsService(FootballDataRepository repository) {
        this.repository = repository;
    }

    public TeamFormSummary teamForm(String teamId, int lastN) {
        Team team = repository.findTeamById(teamId)
                .orElseThrow(() -> new NoSuchElementException("Unknown team id: " + teamId));

        List<MatchResult> allTeamMatches = repository.findMatchesForTeam(teamId);
        int windowSize = Math.min(lastN, allTeamMatches.size());
        List<MatchResult> window = allTeamMatches.subList(allTeamMatches.size() - windowSize, allTeamMatches.size());

        int wins = 0, draws = 0, losses = 0, goalsFor = 0, goalsAgainst = 0, points = 0;
        int shotsTotal = 0, shotsOnTargetTotal = 0, possessionTotal = 0;
        int homeMatches = 0, homeGoals = 0, awayMatches = 0, awayGoals = 0;
        StringBuilder form = new StringBuilder();
        List<TeamFormEntry> entries = new ArrayList<>();

        for (MatchResult m : window) {
            char result = m.resultFor(teamId);
            int gf = m.goalsFor(teamId);
            int ga = m.goalsAgainst(teamId);
            int pts = m.pointsFor(teamId);

            switch (result) {
                case 'W' -> wins++;
                case 'D' -> draws++;
                default -> losses++;
            }
            goalsFor += gf;
            goalsAgainst += ga;
            points += pts;
            shotsTotal += m.shotsOf(teamId);
            shotsOnTargetTotal += m.shotsOnTargetOf(teamId);
            possessionTotal += m.possessionOf(teamId);
            form.append(result);

            boolean isHome = m.isHomeTeam(teamId);
            if (isHome) {
                homeMatches++;
                homeGoals += gf;
            } else {
                awayMatches++;
                awayGoals += gf;
            }

            String opponentId = m.opponentOf(teamId);
            String opponentName = repository.findTeamById(opponentId).map(Team::name).orElse(opponentId);
            entries.add(new TeamFormEntry(
                    m.id(), m.matchDate(), opponentId, opponentName,
                    isHome ? "HOME" : "AWAY", result, gf, ga, pts
            ));
        }

        double avgPossession = windowSize == 0 ? 0 : round1(possessionTotal / (double) windowSize);
        double shotAccuracy = shotsTotal == 0 ? 0 : round1(100.0 * shotsOnTargetTotal / shotsTotal);
        double homeGoalsPerMatch = homeMatches == 0 ? 0 : round1(homeGoals / (double) homeMatches);
        double awayGoalsPerMatch = awayMatches == 0 ? 0 : round1(awayGoals / (double) awayMatches);

        List<String> insights = buildFormInsights(team, wins, draws, losses, windowSize, form.toString(),
                homeGoalsPerMatch, awayGoalsPerMatch, shotAccuracy);

        return new TeamFormSummary(
                teamId, team.name(), windowSize, wins, draws, losses, points,
                goalsFor, goalsAgainst, goalsFor - goalsAgainst,
                form.toString(), avgPossession, shotAccuracy,
                homeGoalsPerMatch, awayGoalsPerMatch, entries, insights
        );
    }

    private List<String> buildFormInsights(Team team, int wins, int draws, int losses, int windowSize,
                                            String formString, double homeGoalsPerMatch,
                                            double awayGoalsPerMatch, double shotAccuracy) {
        List<String> insights = new ArrayList<>();
        if (windowSize == 0) {
            insights.add(team.name() + " has no recorded matches in the sample data.");
            return insights;
        }

        if (losses == 0 && windowSize >= 3) {
            insights.add(team.name() + " is unbeaten in its last " + windowSize + " matches ("
                    + wins + "W " + draws + "D).");
        } else if (wins == 0 && windowSize >= 3) {
            insights.add(team.name() + " is winless in its last " + windowSize + " matches ("
                    + draws + "D " + losses + "L).");
        }

        double goalGap = homeGoalsPerMatch - awayGoalsPerMatch;
        if (Math.abs(goalGap) >= 0.4) {
            String more = goalGap > 0 ? "more at home" : "more away from home";
            insights.add(String.format("%s scores %s — %.1f goals/match at home vs %.1f away, in this window.",
                    team.name(), more, homeGoalsPerMatch, awayGoalsPerMatch));
        }

        if (shotAccuracy >= 45) {
            insights.add(String.format("%s is converting chances efficiently: %.0f%% of shots are on target.",
                    team.name(), shotAccuracy));
        } else if (shotAccuracy > 0 && shotAccuracy <= 25) {
            insights.add(String.format("%s is generating shots but only %.0f%% are on target — a volume-over-accuracy pattern.",
                    team.name(), shotAccuracy));
        }

        if (formString.endsWith("WWW")) {
            insights.add(team.name() + " has won its last three consecutive matches.");
        } else if (formString.endsWith("LLL")) {
            insights.add(team.name() + " has lost its last three consecutive matches.");
        }

        if (insights.isEmpty()) {
            insights.add(team.name() + " shows no strongly one-sided pattern over its last " + windowSize + " matches.");
        }
        return insights;
    }

    public HeadToHead headToHead(String teamAId, String teamBId) {
        Team teamA = repository.findTeamById(teamAId)
                .orElseThrow(() -> new NoSuchElementException("Unknown team id: " + teamAId));
        Team teamB = repository.findTeamById(teamBId)
                .orElseThrow(() -> new NoSuchElementException("Unknown team id: " + teamBId));

        List<MatchResult> meetings = repository.findMatchesBetween(teamAId, teamBId);

        int teamAWins = 0, teamBWins = 0, draws = 0, teamAGoals = 0, teamBGoals = 0;
        List<TeamFormEntry> entries = new ArrayList<>();

        for (MatchResult m : meetings) {
            char resultForA = m.resultFor(teamAId);
            switch (resultForA) {
                case 'W' -> teamAWins++;
                case 'L' -> teamBWins++;
                default -> draws++;
            }
            teamAGoals += m.goalsFor(teamAId);
            teamBGoals += m.goalsFor(teamBId);

            boolean isHomeForA = m.isHomeTeam(teamAId);
            entries.add(new TeamFormEntry(
                    m.id(), m.matchDate(), teamBId, teamB.name(),
                    isHomeForA ? "HOME" : "AWAY", resultForA,
                    m.goalsFor(teamAId), m.goalsAgainst(teamAId), m.pointsFor(teamAId)
            ));
        }

        List<String> insights = buildHeadToHeadInsights(teamA, teamB, meetings.size(), teamAWins, teamBWins, draws,
                teamAGoals, teamBGoals);

        return new HeadToHead(teamAId, teamA.name(), teamBId, teamB.name(), meetings.size(),
                teamAWins, teamBWins, draws, teamAGoals, teamBGoals, entries, insights);
    }

    private List<String> buildHeadToHeadInsights(Team teamA, Team teamB, int meetings, int teamAWins,
                                                  int teamBWins, int draws, int teamAGoals, int teamBGoals) {
        List<String> insights = new ArrayList<>();
        if (meetings == 0) {
            insights.add(teamA.name() + " and " + teamB.name() + " have not met in the sample data.");
            return insights;
        }

        if (teamAWins > teamBWins) {
            insights.add(String.format("%s leads the series %d-%d-%d (W-D-L) across %d meeting%s.",
                    teamA.name(), teamAWins, draws, teamBWins, meetings, meetings == 1 ? "" : "s"));
        } else if (teamBWins > teamAWins) {
            insights.add(String.format("%s leads the series %d-%d-%d (W-D-L) across %d meeting%s.",
                    teamB.name(), teamBWins, draws, teamAWins, meetings, meetings == 1 ? "" : "s"));
        } else {
            insights.add(String.format("The series is level: %d wins each and %d draw%s across %d meeting%s.",
                    teamAWins, draws, draws == 1 ? "" : "s", meetings, meetings == 1 ? "" : "s"));
        }

        double avgGoalsA = round1(teamAGoals / (double) meetings);
        double avgGoalsB = round1(teamBGoals / (double) meetings);
        insights.add(String.format("Average scoreline: %s %.1f – %.1f %s.",
                teamA.name(), avgGoalsA, avgGoalsB, teamB.name()));

        if (teamAGoals + teamBGoals >= meetings * 3) {
            insights.add("These fixtures tend to be high-scoring, averaging "
                    + round1((teamAGoals + teamBGoals) / (double) meetings) + " goals per meeting.");
        }

        return insights;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
