# Definition of Done

D1 implementation считается завершённым: standalone linear dialogue pipeline реализован без Echo Story Engine.

D1.1 считается завершённым только когда доказаны все пункты:

- четыре production-модуля сохраняют направление зависимостей;
- Echo Story Engine не является dependency и не импортируется;
- `.dialogue.es` resources загружаются через Forge reload;
- invalid reload сохраняет previous known-good catalog и не меняет catalog revision;
- diagnostics содержат stable code, logical source id и line/column when available;
- server-authoritative session стартует, advances, completes и cancels;
- stale/wrong/duplicate advance не меняет state;
- Forge networking синхронизирует presentation одному player;
- client overlay показывает speaker и Russian line text;
- click/Space отправляет bounded advance request;
- stale open/update и stale close не ломают client state;
- client state очищается на disconnect/logout;
- final line закрывает overlay;
- host shutdown идемпотентен и чистит session holder;
- `runGameTestServer` обнаруживает и выполняет все обязательные GameTests;
- normal dedicated server smoke выполнен или честно помечен pending с причиной;
- isolated final-JAR server smoke выполнен;
- isolated final-JAR client load/connect smoke выполнен;
- visual overlay acceptance выполнен пользователем или помечен pending;
- final Forge JAR содержит runtime dependencies и не содержит test/GameTest/Minecraft/Forge/Story classes;
- `verifyProductionJarContents` проходит.

Нельзя закрывать D1.1, если обязательная проверка не была выполнена или осталась pending.

## D2

D2 считается завершённым только когда доказаны все пункты:

- `dialogue-persistence` добавлен как portable production module без Forge/Minecraft/EchoScript/Story imports;
- core не знает `Path`, JSON, Gson, Forge или Minecraft;
- compiled dialogue fingerprint считается по canonical compiled meaning, а не raw source/path;
- save schema V1 документирована и validated;
- repository поддерживает explicit load/save/ack/cleanup results и optimistic revision;
- file adapter использует primary/backup/temp policy, bounded backups и backup fallback;
- startup recovery восстанавливает `ACTIVE` как detached, terminal как retained, incompatible/missing as blocked;
- duplicate `ACTIVE` snapshots для одной audience дают deterministic conflict diagnostic;
- player logout не отменяет durable ACTIVE session;
- player login reattach отправляет current line без revision increment;
- start/advance/cancel/complete сохраняют checkpoint до open/update/close;
- terminal result остаётся queryable до acknowledgement или bounded cleanup;
- cleanup не удаляет ACTIVE/blocked snapshots;
- Forge использует host-owned bounded serial persistence scheduler, а не portable background pool;
- shutdown имеет finite flush timeout и не висит бесконечно;
- `/echo_dialogue active`/`health` не раскрывают raw JSON/path/source, но показывают read-only recovery counts;
- GameTest server обнаруживает и выполняет все обязательные D2 GameTests;
- normal dedicated server smoke выполнен или честно помечен pending с причиной;
- isolated final-JAR server smoke выполнен;
- isolated final-JAR client/reconnect/manual visual пункты выполнены пользователем или помечены pending;
- final Forge JAR содержит production classes, не содержит test/GameTest/Minecraft/Forge/Story classes и не embeds duplicate Gson;
- `verifyProductionJarContents` проходит;
- документация и финальный отчёт перечисляют выполненные команды и pending acceptance без преувеличения.

Нельзя закрывать D2, если обязательная автоматическая проверка не была выполнена или осталась failing.

## D5

D5 считается завершённым только когда доказаны все пункты:

- `Alias(..., visual: Portrait("namespace:path"))` и `SkinHead("namespace:path")` компилируются;
- invalid visual metadata отклоняет candidate и сохраняет previous known-good catalog;
- neutral `SpeakerVisualRef` находится в `dialogue-contracts` без Minecraft/Forge/resource/file/URL types;
- compiled fingerprint включает visual absence/presence, kind и ID;
- Save Schema V2 остаётся без visual/cache fields;
- network protocol поднят до `3`;
- `DialogueOpenMessage` содержит только optional visual kind/id descriptor;
- serverbound packets не содержат visual metadata;
- client resolver мапит IDs в `assets/<namespace>/textures/echo_dialogue/portraits|skins/<path>.png`;
- resolver cache bounded и очищается на client resource reload/logout;
- missing/corrupt asset даёт text-only fallback без session progression changes;
- 64x64 skin-head renderer рисует base face и outer head layer;
- LINE и CHOICE prompt presentation получают visual только из server presentation;
- text-only layout не получает empty visual slot;
- existing D1-D4 session, choices, outcomes, persistence и adapter behavior остаются зелёными;
- GameTest server обнаруживает и выполняет все обязательные D5 markers;
- final JAR содержит client visual code и public sample assets, но не содержит test-only fixtures, GameTest/JUnit/Minecraft/Forge/Story classes;
- real client static portrait, skin-head, missing-resource, resource reload и restart acceptance выполнены пользователем или честно помечены pending.

Нельзя закрывать D5, если visual-only client checks не выполнены и не отмечены явно как pending.

## D6

D6 считается завершённым только когда доказаны все пункты:

- `dialogue-presentation-api` добавлен как plain Java public module и зависит только от `dialogue-contracts`;
- public API version равна `SpeakerPresentationApiVersion` major `1`, minor `2`;
- public neutral types содержат provider ID, descriptor, request, full resolved presentation, explicit resolution variants и provider session contract;
- `SpeakerPresentationRequest` содержит только session/dialogue/speaker/audience/kind/authored fallback display/authored fallback visual;
- `ResolvedSpeakerPresentation` содержит только display name и optional `SpeakerVisualRef`;
- `SpeakerVisualRef` является public neutral contract в `dialogue-contracts`;
- Forge API содержит typed `EchoDialogueSpeakerPresentationRegistrationEvent`, factory и creation context;
- registration event server-scoped, closes after posting and rejects late registration;
- provider descriptors/API versions/duplicate IDs/session creation failures are validated and isolated;
- provider sessions are created once per host and closed on stop;
- resolution runs on server thread before `DialogueNetwork.sendOpen(...)`;
- providers are queried in deterministic provider-ID order;
- no provider handled means authored fallback;
- one `RESOLVED` means dynamic display/visual is used;
- unavailable/failure/null/exception means authored fallback with bounded diagnostic;
- two or more claiming providers mean authored fallback and deterministic ambiguity diagnostic;
- dynamic result cannot change `SpeakerId`, line text, choices, target, outcome or revision;
- CHOICE without speaker context does not reuse stale speaker visual;
- Save Schema V2 remains unchanged;
- provider output is absent from persistence and compiled fingerprint;
- network protocol remains `3` because packet shape is unchanged;
- read-only `/echo_dialogue providers` and `/echo_dialogue provider <providerId>` commands exist;
- public artifacts are published to `build/integration-maven`;
- separate provider test mod builds as a separate JAR against public artifacts only;
- provider test JAR contains no Echo Dialogue API class copies;
- production Echo Dialogue JAR contains presentation API and Forge bridge, but no provider test classes;
- exact Forge GameTest task discovers, executes and passes all required D1-D6 tests;
- isolated Echo Dialogue-only server/client behavior remains valid;
- isolated Echo Dialogue + provider server/client visual checks are executed or clearly marked user-pending;
- Echo NPC integration, ActorId, NpcService lookup, entity lookup, distance checks, priorities, partial merge, remote skins and automatic same-line refresh are not implemented.

Нельзя закрывать D6, если separate external provider acceptance не выполнен или visual-only client checks скрыты как “автоматически пройденные”.
