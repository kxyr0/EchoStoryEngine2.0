# Echo Story NPC Adapter

Echo Story NPC Adapter - независимый Forge 1.20.1 мод, который связывает
публичный Story Extension API с публичным Echo NPC API.

Каноническое расширение:

- extension ID: `echo_story_npc_adapter:bridge`;
- display name: `Echo Story NPC Adapter`;
- action types:
  - `echo_npc:spawn`;
  - `echo_npc:teleport`;
  - `echo_npc:despawn`.

Адаптер не реализует движение, навигацию, поиск пути, анимацию, диалоги, AI,
расписания, бой, инвентарь, гардероб или кинематики.

Production-сборка должна зависеть только от публичных artifacts:

- `io.github.kxyr0.echostoryengine:story-contracts`;
- `io.github.kxyr0.echostoryengine:story-extension-api`;
- `io.github.kxyr0.echostoryengine:story-extension-forge-1.20.1`;
- `io.github.kxyr0.echonpc:npc-contracts`;
- `io.github.kxyr0.echonpc:npc-forge-api-1.20.1`.

Ни implementation-классы Story Engine, ни implementation-классы Echo NPC не
должны попадать в adapter JAR.
