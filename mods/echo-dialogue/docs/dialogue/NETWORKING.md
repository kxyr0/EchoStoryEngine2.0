# Networking D6

Целевой протокол:

- server-to-client open/update presentation;
- server-to-client close presentation;
- client-to-server request advance с `sessionId` и expected `revision`;
- client-to-server select choice с `sessionId`, expected `revision` и `ChoiceId`;
- отдельная protocol version, независимая от mod version и EchoScript version.

Сервер обязан проверять sender ownership, session ID, revision, phase, visible choice membership и duplicate requests. Клиент не выбирает следующую строку, branch target или terminal outcome.

Текущий статус: `SimpleChannel` protocol реализован:

- `DialogueOpenMessage`;
- `DialogueCloseMessage`;
- `DialogueAdvanceMessage`.
- `DialogueSelectChoiceMessage`.

Serverbound advance/choice обрабатываются через server-side events и `ForgeDialogueHost`.

D1.1 validation rules:

- `DialogueAdvanceMessage` содержит только `sessionId` и `expectedRevision`;
- `DialogueSelectChoiceMessage` содержит только `sessionId`, `expectedRevision` и `choiceId`;
- sender identity берётся из Forge `NetworkEvent.Context`, а не из packet payload;
- `ForgeDialogueHost` применяет request только к session аудитории sender;
- wrong session ID не двигает cursor;
- duplicate/stale revision не двигает cursor;
- unknown или invisible `ChoiceId` не двигает branch;
- packet после shutdown игнорируется, потому что host отсутствует или stopped;
- client state отправляет следующий advance только после новой server revision или close;
- client state отправляет choice только для текущей visible choice и блокируется до новой server revision или close;
- stale open/update не может разблокировать wait lock;
- stale close с меньшей revision не закрывает текущий overlay.

D2 ordering:

- network messages не являются persistence model;
- start/advance/cancel/complete сначала получают durable repository result;
- `DialogueOpenMessage`/update отправляется только после successful save;
- `DialogueCloseMessage` отправляется только после terminal save;
- при save failure клиент resync получает только previous durable authoritative presentation;
- player reattach отправляет текущую line presentation без изменения session revision.
- player reattach может отправить текущую choice presentation без изменения session revision.

D5 visual descriptor:

- `DialogueOpenMessage.visual` отсутствует для text-only;
- when present: `kind` (`PORTRAIT` или `SKIN_HEAD`) и bounded `id` (`namespace:path`);
- packet не содержит raw path, URL, PNG bytes, texture object, target node, outcome, entity или `GameProfile`;
- invalid visual descriptor на client decode отбрасывается как no visual, если readable text можно сохранить.

D6 dynamic presentation reuse:

Protocol 5 note:

- `DialogueOpenMessage.cues` now carries bounded line `name/value` pairs;
- `.SetAnimationFace(...)` is sent as `faceAnimation = namespace:path`;
- `DialogueOpenMessage.speakerNameColor` carries optional 24-bit RGB speaker-name color;
- this changed the packet schema, so clients and servers must both use
  protocol `5`.

- protocol updated to `5` because optional speaker name color is now part of `DialogueOpenMessage`;
- `speakerNameColor` is resolved server-side and remains empty when neither authored `Alias(...)` nor provider supplies a color;
- dynamic resolution записывает результат в существующие поля speaker display name, optional name color и optional visual descriptor;
- packet не передаёт provider ID, provider API version, ActorId, Entity, NPC state, ambiguity details или provider status;
- client rendering не различает authored/dynamic/fallback origin.

Client-to-server packets не менялись: клиент не может отправить visual ID или выбрать branch target/outcome через visual metadata.

Protocol channel: `echo_dialogue:main`, version `5`.
