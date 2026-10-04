# Design Decisions

---

**Decision:** Store the season's data as bundled JSON and load it into memory
at startup, rather than standing up a database.

**Why:** The dataset is small (8 teams, 56 matches), static, and ships with
the repository as a fixture. A database would add operational surface
(schema, migrations, a running process) for a read-only dataset that already
fits comfortably in memory.

**Trade-off:** This does not scale to a multi-season historical archive or
concurrent writes. That's a different problem with a different shape.

**Future:** If the project ever needs to track live, growing data, swap
`FootballDataRepository`'s internals for a JPA repository — the service layer
already depends only on its method signatures, not its storage.

---

**Decision:** Generate insight sentences with explicit, named threshold
methods (e.g. "unbeaten if `losses == 0` and `windowSize >= 3`") instead of
asking an LLM to summarize the numbers.

**Why:** The insights need to be exactly reproducible and directly traceable
to the aggregated stats. A generative summary could phrase things
persuasively even when the underlying signal is weak or contradictory.

**Trade-off:** The sentences are more templated and less varied than
free-form generated text.

**Future:** An LLM could be layered on top purely for *phrasing* variety,
constrained to only rephrase — never invent — the facts this service already
computed. That would be an explicit, separate "narration" step, not a
replacement for the deterministic analysis.

---

**Decision:** Model domain types as Java records rather than classes with
Lombok-generated boilerplate.

**Why:** Zero extra dependency, immutability by default, and the structural
`equals`/`hashCode`/`toString` records provide made the unit tests
straightforward to write (see `FootballAnalyticsServiceTest`).

**Trade-off:** None significant for this project's size.

**Future:** N/A — this pattern is reused across the other 16 projects in the
Commerce Engineering Lab collection for consistency.
