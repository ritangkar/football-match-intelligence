package com.ritangkar.footballintel.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ritangkar.footballintel.model.MatchResult;
import com.ritangkar.footballintel.model.Team;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads the bundled synthetic dataset once at startup and serves it from memory.
 *
 * There is no database here on purpose: the dataset is small, read-only and
 * ships with the repository, so an in-memory repository keeps the project's
 * footprint honest (see docs/design-decisions.md).
 */
@Repository
public class FootballDataRepository {

    private final Map<String, Team> teamsById;
    private final List<MatchResult> matches;

    public FootballDataRepository() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        try {
            List<Team> loadedTeams = readList(mapper, "sample-data/teams.json", Team[].class);
            List<MatchResult> loadedMatches = readList(mapper, "sample-data/matches.json", MatchResult[].class);

            Map<String, Team> byId = new LinkedHashMap<>();
            for (Team t : loadedTeams) {
                byId.put(t.id(), t);
            }
            this.teamsById = Collections.unmodifiableMap(byId);
            this.matches = List.copyOf(loadedMatches);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load bundled sample data", e);
        }
    }

    private <T> List<T> readList(ObjectMapper mapper, String classpathLocation, Class<T[]> arrayType) throws IOException {
        try (InputStream in = new ClassPathResource(classpathLocation).getInputStream()) {
            T[] arr = mapper.readValue(in, arrayType);
            return List.of(arr);
        }
    }

    public List<Team> findAllTeams() {
        return List.copyOf(teamsById.values());
    }

    public Optional<Team> findTeamById(String teamId) {
        return Optional.ofNullable(teamsById.get(teamId));
    }

    public List<MatchResult> findAllMatches() {
        return matches;
    }

    /** All matches involving the given team, ordered oldest to newest. */
    public List<MatchResult> findMatchesForTeam(String teamId) {
        return matches.stream()
                .filter(m -> m.involves(teamId))
                .sorted((a, b) -> a.matchDate().compareTo(b.matchDate()))
                .toList();
    }

    /** All matches between two given teams, ordered oldest to newest. */
    public List<MatchResult> findMatchesBetween(String teamAId, String teamBId) {
        return matches.stream()
                .filter(m -> m.involves(teamAId) && m.involves(teamBId))
                .sorted((a, b) -> a.matchDate().compareTo(b.matchDate()))
                .toList();
    }
}
