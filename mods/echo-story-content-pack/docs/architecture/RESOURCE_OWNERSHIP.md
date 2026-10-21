# Resource ownership

Story files владеют:

- story flow;
- state variables;
- external action calls;
- action result binding через `output`;
- completion.

Story files не владеют:

- full Dialogue line content;
- complete NPC definitions;
- PNG paths;
- renderer behavior;
- navigation;
- animation.

Dialogue files владеют:

- speaker aliases;
- `SpeakerId`;
- authored display fallback;
- optional authored `Portrait(...)` или `SkinHead(...)`;
- lines;
- choices;
- `DialogueOutcomeId`.

Dialogue files не владеют:

- NPC spawning;
- actor persistence;
- entity lookup;
- Story execution IDs.

NPC files владеют:

- `NpcDefinitionId`;
- display name;
- optional `NpcSkinId`.

NPC files не владеют:

- `ActorId` placement;
- Story actions;
- Dialogue lines;
- AI, schedules, navigation, inventory или equipment.

Definitions inside resource files являются authority. Отдельный duplicate content manifest не вводится.

