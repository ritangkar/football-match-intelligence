# Portfolio Demo

**Demo level:** 2 — Portfolio simulation (bundled sample data, client-side).

The portfolio demo (`lab/demos/football-match-intelligence.js` in the
portfolio repository) re-implements `teamForm()` and the insight thresholds
in vanilla JavaScript against a trimmed copy of `sample-data/matches.json`
(the same synthetic season), so a recruiter can:

1. Pick a team from a dropdown
2. Choose a form window (3 / 5 / 10 matches)
3. See the same wins/draws/losses, form string and insight sentences this
   Java service would return from `GET /api/teams/{id}/form`

No backend call is made from the portfolio — this keeps GitHub Pages fast and
means the demo never depends on an API being online. The real Spring Boot API
in this repository is what a recruiter would run locally (`mvn spring-boot:run`)
to see the full REST surface, including `/api/head-to-head`.
