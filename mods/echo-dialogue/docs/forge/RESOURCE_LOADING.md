# Resource Loading

Forge layer регистрирует `SimplePreparableReloadListener`.

Pipeline:

1. `ResourceManager.listResources("echo_dialogue/dialogues", *.dialogue.es)`.
2. Чтение каждого ресурса как UTF-8.
3. Компиляция через `EchoScriptDialogueCompiler`.
4. Сборка `DialogueCatalogCandidate`.
5. Atomic apply через `DialogueCatalogState`.

D5 visual metadata is server-side neutral metadata only:

- `Alias(..., visual: Portrait("namespace:path"))` и `SkinHead("namespace:path")` валидируются как IDs;
- server `/reload` не ищет и не декодирует `assets/**.png`;
- missing client asset не отклоняет valid DialogueResource;
- client resource reload очищает visual cache отдельно от server dialogue catalog reload.

Если candidate содержит ERROR diagnostic или duplicate `DialogueId`, reload отклоняется и предыдущий known-good catalog остаётся видимым.

D2 known-good retention policy:

- valid candidate увеличивает catalog revision на `1`;
- invalid candidate возвращает `CatalogInstallResult.installed=false`;
- current snapshot identity/revision остаётся прежним;
- partial dialogues из invalid candidate не публикуются;
- active sessions продолжают использовать compiled definition, с которой стартовали;
- durable ACTIVE recovery сравнивает snapshot fingerprint с текущим compiled catalog;
- missing или incompatible compiled definition не удаляет snapshot, а блокирует recovery diagnostic;
- diagnostics включают stable code, category, logical source id и line/column when available;
- raw source text не логируется.

Automated coverage:

- unit/integration test компилирует valid source, применяет catalog, затем отклоняет unresolved alias candidate и применяет fixed candidate;
- GameTest `ForgeDialogueGameTests.invalidCandidateRetainsCatalog` проверяет тот же retention flow в Forge runtime.
- GameTest `ForgeDialogueVisualGameTests.invalidVisualRetainsCatalog` проверяет, что invalid visual metadata не заменяет known-good catalog.
