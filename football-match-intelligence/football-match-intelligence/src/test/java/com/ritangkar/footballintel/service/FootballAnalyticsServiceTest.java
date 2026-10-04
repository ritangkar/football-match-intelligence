package com.ritangkar.footballintel.service;

import com.ritangkar.footballintel.model.MatchResult;
import com.ritangkar.footballintel.model.Team;
import com.ritangkar.footballintel.model.TeamFormSummary;
import com.ritangkar.footballintel.model.HeadToHead;
import com.ritangkar.footballintel.repository.FootballDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class FootballAnalyticsServiceTest {

    private static final Team TEAM_A = new Team("T01", "Ironbridge FC", "Ironbridge", "Synthetic Premier Division", 1901);
    private static final Team TEAM_B = new Team("T02", "Meridian United", "Meridian", "Synthetic Premier Division", 1911);

    @Nested
    @DisplayName("teamForm() with a small, fully controlled fixture list")
    class TeamFormWithControlledData {

        private FootballDataRepository repository;
        private FootballAnalyticsService service;

        @BeforeEach
        void setUp() {
            repository = Mockito.mock(FootballDataRepository.class);
            service = new FootballAnalyticsService(repository);

            when(repository.findTeamById("T01")).thenReturn(Optional.of(TEAM_A));
            when(repository.findTeamById("T02")).thenReturn(Optional.of(TEAM_B));

            // T01 wins 3 in a row: two at home, one away. Deliberately unbeaten window.
            List<MatchResult> matches = List.of(
                    new MatchResult("M1", LocalDate.of(2025, 8, 1), "T01", "T02",
                            2, 0, 12, 8, 6, 2, 55, 45),
                    new MatchResult("M2", LocalDate.of(2025, 8, 8), "T02", "T01",
                            0, 1, 9, 11, 3, 5, 48, 52),
                    new MatchResult("M3", LocalDate.of(2025, 8, 15), "T01", "T02",
                            3, 1, 14, 10, 8, 4, 60, 40)
            );
            when(repository.findMatchesForTeam("T01")).thenReturn(matches);
        }

        @Test
        @DisplayName("aggregates wins, points, goal difference and the chronological form string")
        void aggregatesCoreStats() {
            TeamFormSummary summary = service.teamForm("T01", 5);

            assertThat(summary.matchesConsidered()).isEqualTo(3);
            assertThat(summary.wins()).isEqualTo(3);
            assertThat(summary.draws()).isEqualTo(0);
            assertThat(summary.losses()).isEqualTo(0);
            assertThat(summary.points()).isEqualTo(9);
            assertThat(summary.goalsFor()).isEqualTo(6);
            assertThat(summary.goalsAgainst()).isEqualTo(1);
            assertThat(summary.goalDifference()).isEqualTo(5);
            assertThat(summary.formString()).isEqualTo("WWW");
        }

        @Test
        @DisplayName("flags an unbeaten run and a three-win streak in the generated insights")
        void flagsUnbeatenRun() {
            TeamFormSummary summary = service.teamForm("T01", 5);

            assertThat(summary.insights())
                    .anyMatch(i -> i.contains("unbeaten"))
                    .anyMatch(i -> i.contains("won its last three"));
        }

        @Test
        @DisplayName("windows to the requested lastN when more matches exist than requested")
        void honoursLastNWindow() {
            TeamFormSummary summary = service.teamForm("T01", 2);

            assertThat(summary.matchesConsidered()).isEqualTo(2);
            // Only the most recent two matches (M2, M3) should be considered.
            assertThat(summary.recentMatches())
                    .extracting(e -> e.matchId())
                    .containsExactly("M2", "M3");
        }

        @Test
        @DisplayName("throws for an unknown team id instead of silently returning empty data")
        void throwsForUnknownTeam() {
            when(repository.findTeamById("T99")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.teamForm("T99", 5))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("headToHead() with a controlled fixture list")
    class HeadToHeadWithControlledData {

        @Test
        @DisplayName("computes the series record from teamA's perspective")
        void computesSeriesRecord() {
            FootballDataRepository repository = Mockito.mock(FootballDataRepository.class);
            FootballAnalyticsService service = new FootballAnalyticsService(repository);

            when(repository.findTeamById("T01")).thenReturn(Optional.of(TEAM_A));
            when(repository.findTeamById("T02")).thenReturn(Optional.of(TEAM_B));
            when(repository.findMatchesBetween("T01", "T02")).thenReturn(List.of(
                    new MatchResult("M1", LocalDate.of(2025, 1, 1), "T01", "T02", 2, 1, 10, 8, 5, 3, 52, 48),
                    new MatchResult("M2", LocalDate.of(2025, 3, 1), "T02", "T01", 1, 1, 9, 9, 4, 4, 50, 50)
            ));

            HeadToHead h2h = service.headToHead("T01", "T02");

            assertThat(h2h.meetings()).isEqualTo(2);
            assertThat(h2h.teamAWins()).isEqualTo(1);
            assertThat(h2h.draws()).isEqualTo(1);
            assertThat(h2h.teamBWins()).isEqualTo(0);
            assertThat(h2h.teamAGoals()).isEqualTo(3);
            assertThat(h2h.teamBGoals()).isEqualTo(2);
            assertThat(h2h.insights()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Invariant checks against the real bundled sample data")
    class RealBundledDataInvariants {

        private final FootballDataRepository realRepository = new FootballDataRepository();
        private final FootballAnalyticsService service = new FootballAnalyticsService(realRepository);

        @Test
        @DisplayName("every team's form summary is internally consistent")
        void formSummaryIsInternallyConsistentForEveryTeam() {
            for (Team team : realRepository.findAllTeams()) {
                TeamFormSummary summary = service.teamForm(team.id(), 10);

                assertThat(summary.wins() + summary.draws() + summary.losses())
                        .as("W+D+L must equal matches considered for " + team.name())
                        .isEqualTo(summary.matchesConsidered());

                assertThat(summary.points())
                        .as("points must equal 3*wins + draws for " + team.name())
                        .isEqualTo(summary.wins() * 3 + summary.draws());

                assertThat(summary.goalDifference())
                        .isEqualTo(summary.goalsFor() - summary.goalsAgainst());

                assertThat(summary.formString()).hasSize(summary.matchesConsidered());
                assertThat(summary.insights()).isNotEmpty();
            }
        }

        @Test
        @DisplayName("head-to-head win counts always sum to the number of meetings")
        void headToHeadRecordIsConsistent() {
            List<Team> teams = realRepository.findAllTeams();
            HeadToHead h2h = service.headToHead(teams.get(0).id(), teams.get(1).id());

            assertThat(h2h.teamAWins() + h2h.teamBWins() + h2h.draws()).isEqualTo(h2h.meetings());
            assertThat(h2h.meetings()).isGreaterThan(0);
        }
    }
}
