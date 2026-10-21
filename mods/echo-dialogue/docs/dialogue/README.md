# Диалоги D7

Echo Dialogue поддерживает линейные и branching dialogue graph sessions:

1. `.dialogue.es` читается как UTF-8 server data resource.
2. EchoScript parser возвращает syntax tree.
3. Echo Dialogue adapter связывает `DialogueResource`.
4. Core создаёт immutable `CompiledDialogue` graph с optional neutral speaker visual refs.
5. Сервер стартует session для одной audience.
6. Сервер принимает только явный `Advance` или `SelectChoice`.
7. Последняя строка node открывает choices или завершает session.
8. `ChoiceId` выбирает только авторский visible branch target.
9. Terminal node завершает session с `DialogueOutcomeId`.
10. D4 сохраняет новый durable checkpoint до отправки open/update/close.
11. Client overlay resolves optional visual resources только на physical client.
12. Client overlay закрывается только после server close/update.

NPC lookup, live player skins, remote portraits, voice, quests и Story Engine integration не входят в Echo Dialogue. Story integration живёт в отдельном adapter repository.

D1.1 не добавляет gameplay features. Он фиксирует acceptance hardening:

- invalid reload сохраняет previous known-good catalog;
- diagnostics содержат stable code, source id и line/column, когда EchoScript даёт span;
- client input ждёт новую server revision и не принимает stale update;
- host lifecycle очищается на shutdown/disconnect;
- final JAR проверяется automated task.

D2 добавил durability/recovery:

- `dialogue-persistence` хранит portable schema V1 без Forge/Minecraft/Gson в core;
- Forge пишет snapshots в world-scoped path `data/echo_dialogue/sessions`;
- start/advance/cancel/complete не отправляют presentation, пока соответствующий snapshot не сохранён;
- logout не отменяет ACTIVE session, а переводит presentation в detached/offline режим до reattach;
- terminal result остаётся queryable до explicit acknowledgement или bounded cleanup.

D4 добавил choices/outcomes:

- `Choice("id", "text", next: "node")` в legacy authoring;
- `Complete("namespace:path")`;
- persisted `presentationKind` и terminal outcome в schema V2;
- network protocol `2` с serverbound choice packet;
- public `DialogueSessionView.terminalOutcomeId()` для adapter output mapping.

D5 добавляет speaker visuals:

- `visual: Portrait("namespace:path")` и `visual: SkinHead("namespace:path")` только на `Alias(...)`;
- `SpeakerVisualRef` хранит только kind/id;

D6 добавляет dynamic speaker presentation provider boundary:

- `SpeakerId` остаётся neutral integration key;
- external provider может заменить display name и optional `SpeakerVisualRef` перед отправкой LINE/CHOICE;
- line text, choices, targets, outcome и revision не меняются;
- provider output не сохраняется и не входит в fingerprint;
- no provider, unavailable, failure или ambiguity всегда дают authored fallback.
- protocol `5` передаёт optional visual descriptor, optional speaker name color и line cues в `DialogueOpenMessage`;
- client resource resolver использует `assets/<namespace>/textures/echo_dialogue/portraits|skins/<path>.png`;
- Save Schema V2 не хранит visual/cache state.

D7 добавляет compact method-based authoring:

- `Id("namespace:path")` вместо обязательного `Build()`;
- `Start(Method)` вместо `Start("node")`;
- private `void` methods становятся nodes;
- `Choice("id", "text", TargetMethod)` становится canonical target style;
- `Line("speaker", "text", "explicit.key", thought: true)` поддерживает optional key/thought;
- `Line(...).SetAnimation(...)` и `Line(...).SetAnimationFace(...)` сохраняются как neutral cues;
- legacy `Build()`, `Nodes()`, `new Node(...)` и named `key:` временно читаются с warning.
