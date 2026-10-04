# Football Match Intelligence

A compact football analytics engine written in Java + Spring Boot. It turns raw
match results into **team form summaries**, **head-to-head comparisons** and
short, data-grounded **insight sentences** — the kind of thing a match-preview
widget or a scouting dashboard would sit on top of.

Part of the [Commerce Engineering Lab](https://ritangkar.github.io/#commerce-lab) —
17 small, production-minded engineering capabilities by Ritangkar Dey.

---

## 1. Project Overview

Given a season of match results, the service answers three questions:

- **How is this team playing right now?** (`GET /api/teams/{id}/form`)
- **How have these two teams historically matched up?** (`GET /api/head-to-head`)
- **What does the underlying data actually show, in plain English?** (the `insights` field on both responses)

## 2. Problem

Raw match results (scorelines, shots, possession) are hard to reason about at a
glance. Turning them into "team A is unbeaten in 5" or "this fixture is usually
high-scoring" by hand doesn't scale past a handful of teams, and doing it
inconsistently (or by eyeballing a spreadsheet) doesn't hold up under scrutiny.

## 3. Solution

A small, in-memory analytics layer that:

1. Loads a bundled synthetic dataset (8 teams, 56 matches — a full double
   round-robin) at startup.
2. Computes deterministic aggregates: wins/draws/losses, points, goal
   difference, shot accuracy, home/away goal split, rolling form string.
3. Converts those aggregates into short insight sentences using explicit,
   documented thresholds — never an LLM guess, never a hidden heuristic.

## 4. Key Features

- Rolling **team form** over a configurable window (`lastN`, 1–20 matches)
- **Head-to-head** series record and average scoreline between any two teams
- Home/away goal-scoring split per team
- Shot accuracy (shots on target ÷ total shots)
- Every insight sentence is traceable to the numbers that produced it
- Clean 404/400 error responses for unknown team ids or bad input

## 5. Architecture

```
Sample Data (JSON)
        │
        ▼
FootballDataRepository  (loads once, serves from memory)
        │
        ▼
FootballAnalyticsService (form, head-to-head, insight generation)
        │
        ▼
REST Controllers (/api/teams, /api/matches, /api/head-to-head)
```

See [`docs/architecture.md`](docs/architecture.md) for the full diagram and data flow.

## 6. Technical Approach

- Java 21 records for every immutable model (`Team`, `MatchResult`,
  `TeamFormSummary`, `HeadToHead`, `TeamFormEntry`)
- No database: the dataset is small, static and ships with the repo, so an
  in-memory repository is the honest choice (see design decisions)
- Insight generation is a set of small, named, unit-testable methods —
  not a single 200-line function

## 7. Design Decisions

See [`docs/design-decisions.md`](docs/design-decisions.md). Highlights:

- In-memory data over a database (dataset is static and small)
- Deterministic insight thresholds over an LLM-written summary (explainability)
- Records over builders/Lombok (no extra dependency, immutability by default)

## 8. Sample Input

```
GET /api/teams/T01/form?lastN=5
GET /api/head-to-head?teamA=T01&teamB=T02
```

## 9. Sample Output

```json
{
  "teamId": "T01",
  "teamName": "Ironbridge FC",
  "matchesConsidered": 5,
  "wins": 3,
  "draws": 1,
  "losses": 1,
  "points": 10,
  "goalsFor": 8,
  "goalsAgainst": 4,
  "goalDifference": 4,
  "formString": "WDWLW",
  "avgPossessionPct": 54.2,
  "shotAccuracyPct": 41.3,
  "homeGoalsPerMatch": 2.0,
  "awayGoalsPerMatch": 1.3,
  "insights": [
    "Ironbridge FC scores more at home — 2.0 goals/match at home vs 1.3 away, in this window."
  ]
}
```

## 10. How to Run

```bash
git clone <your-repo-url>
cd football-match-intelligence
mvn spring-boot:run
```

The API is available at `http://localhost:8081`.

## 11. How to Test

```bash
mvn test
```

Tests cover controlled fixture scenarios (via Mockito) and structural
invariants checked against the real bundled dataset (e.g. wins + draws +
losses always equals matches considered, for every team).

## 12. API Documentation

| Method | Path                          | Description                              |
|--------|-------------------------------|-------------------------------------------|
| GET    | `/api/teams`                  | List all teams                            |
| GET    | `/api/teams/{id}/form`        | Form summary; `lastN` query param (1–20)  |
| GET    | `/api/matches`                | All matches in the sample season          |
| GET    | `/api/matches/team/{id}`      | All matches for one team                  |
| GET    | `/api/head-to-head`           | `teamA`, `teamB` query params             |

## 13. Portfolio Demo

The Commerce Engineering Lab demo (`lab/demos/football-match-intelligence.js`)
ports this exact form/insight logic to client-side JavaScript against a
trimmed copy of the same sample dataset, so it runs on GitHub Pages with no
backend call. See `docs/demo.md`.

## 14. Limitations

- Synthetic data only — no live football data provider is used or implied
- Single league, single season slice (56 matches); not built for large-scale
  historical archives
- Insight thresholds are illustrative, not tuned against real-world analytics benchmarks

## 15. Future Enhancements

- League table / standings endpoint
- Expected-goals (xG)-style shot-quality modelling
- Multi-season trend tracking

## 16. License

MIT — see [`LICENSE`](LICENSE).
