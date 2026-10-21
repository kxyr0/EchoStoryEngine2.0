# Progress

- D1: complete. Standalone linear dialogue pipeline implemented.
- D1.1: complete and accepted. Client overlay/input, GameTests, dedicated server, isolated final-JAR checks, invalid reload retention and packaging verification are treated as baseline.
- D2: implementation complete in this changeset; automated gates, GameTests, normal load smoke and isolated final-JAR server smoke passed. Manual visual restart/reconnect remains explicitly user-pending.
- D3: complete through public API bridge and external adapter compatibility.
- D4: complete through choices/outcomes, protocol `2`, Schema V2 and user-confirmed choice flow.
- D5: active during implementation. Optional speaker visuals are complete only after real client portrait/skin-head/fallback/reload acceptance.

Final evidence is recorded in `docs/development/D2_FINAL_REPORT.md`.
# ES2 high-level dialogue simplification

2026-08-26: implemented resource-context identity inference, typed high-level binding/lowering, deterministic entry and
node semantics, symbolic `GoTo`, sparse dynamic speaker presentation, source provenance, explicit `Formerly` save
migrations, collision diagnostics, legacy compatibility, templates/tooling docs, and regression coverage. Version
advanced to `0.12.0-SNAPSHOT`.
