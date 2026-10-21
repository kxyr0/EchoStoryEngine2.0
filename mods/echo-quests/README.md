# Echo Quests

`echo_quests` is the dedicated quest runtime. It is not a Story Engine alias: quests have persistent player-scoped
state, explicit objectives, event transitions, timed condition branches, and completed or failed terminal states.

Timed branches are driven by Echo Time occurrences. At the authored `HH:mm`, the Forge host can check the player's
dimension and position plus carried item counts, then move to either the success or fallback state.

Quest resources live at `data/<namespace>/echo_quests/quests/**/*.quest.es`. See
`docs/authoring/WRITING_QUESTS.md` for the ES2 format.
