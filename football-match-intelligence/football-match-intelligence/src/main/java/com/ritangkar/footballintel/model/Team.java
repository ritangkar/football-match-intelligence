package com.ritangkar.footballintel.model;

/**
 * A club competing in the synthetic league. Loaded verbatim from sample-data/teams.json.
 */
public record Team(
        String id,
        String name,
        String city,
        String league,
        int foundedYear
) {
}
