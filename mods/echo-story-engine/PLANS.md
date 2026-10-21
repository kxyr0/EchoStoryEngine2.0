# Execution Plans

Execution plans are required when a change:

- touches more than one Gradle module;
- changes public API, persistence, networking, dependency graph, or module structure;
- adds a production dependency;
- adds an integration or migration;
- changes more than about five production files;
- needs an architecture exception.

Plans live in `docs/plans`.

Each plan must contain:

- goal;
- non-goals;
- current state;
- constraints;
- architecture decision;
- steps;
- planned files;
- risks;
- verification;
- rollback;
- documentation updates;
- final status.

Do not rewrite a completed plan to hide discoveries. Add final notes and deviations instead.
