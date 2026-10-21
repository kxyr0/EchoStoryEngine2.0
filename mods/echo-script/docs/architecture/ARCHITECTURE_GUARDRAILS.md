# Architecture Guardrails

Hard gates:

- `gradlew check` runs tests, Checkstyle, PMD, architecture tests, whitespace checks, and source-shape checks.
- Production class logical-line hard limit: 180.
- Production method logical-line hard limit: 40.
- Production package hard type limit: 12.
- Wildcard imports are forbidden.
- Mutable static production fields are forbidden.
- Forbidden generic package/type names are checked.

Quality gates must not be weakened to make a build pass.
