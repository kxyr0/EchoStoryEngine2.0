# Progress

- Standalone adapter repository scaffolded.
- Dependencies are public Maven artifacts from `echo-dialogue` and `echo-trigger`.
- Inline trigger IDs are deterministic from Dialogue session context.
- T3 reactive trigger callback invocations are consumed through `trigger-forge-api-1.20.1`.
- `Dialogue.Start(...)` actions are routed through Echo Dialogue public service lookup, without implementation imports.
