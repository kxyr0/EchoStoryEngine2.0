# D5 Execution Plan: Optional Speaker Portrait and Minecraft Skin Head Presentation

## Audit summary

- Текущий compiled speaker model не содержит отдельной `CompiledSpeaker`: `Alias(...)` разворачивается в каждую `CompiledDialogueLine` как `SpeakerId`, display name и text.
- Текущая presentation model `DialoguePresentation` содержит `LINE` или `CHOICE`, speaker display name, prompt text, choices и input flags.
- Save Schema V2 хранит только session authority: session/dialogue/audience, revision, node, presentation kind, line ordinal, terminal fields и compiled fingerprint.
- Network channel `echo_dialogue:main` сейчас использует protocol `2`; client-to-server packets не содержат presentation metadata.
- Client overlay уже split по state, animation, layout, draw и renderer; пакет `forge.client` уже достиг лимита 7 production Java files.
- `dialogue-core/.../model` и `dialogue-echoscript-adapter/.../binding` тоже на лимите 7 файлов, поэтому новые D5 types должны идти в новые leaf packages или в существующие файлы без раздувания ответственности.

## Neutral visual reference

Добавить в `dialogue-core` новый leaf package `core.visual`:

- `SpeakerVisualKind` с ровно `PORTRAIT` и `SKIN_HEAD`;
- `SpeakerVisualId`, immutable wrapper над `NamespacedId`;
- `SpeakerVisualRef`, immutable pair kind/id.

Pure modules не получают `ResourceLocation`, `Path`, `File`, `NativeImage`, texture objects, URL, `GameProfile` или renderer state.

## DialogueResource syntax

Расширить только `Alias(...)`:

```csharp
Alias("father", "sample:father", display: "Отец", visual: SkinHead("sample:father"));
Alias("merchant", "sample:merchant", display: "Торговец", visual: Portrait("sample:merchant"));
Alias("player", "sample:player", display: "Игрок");
```

`visual` отсутствует -> text-only. `Line(...)` visual override остаётся unsupported. Unknown visual intrinsic, missing ID,
malformed namespaced ID, non-call visual value и лишние arguments дают semantic diagnostics и не попадают в `CompiledDialogue`.

## Compiled and presentation model

Из-за текущей line-based compiled architecture visual reference будет храниться в `CompiledDialogueLine` как
`Optional<SpeakerVisualRef>`. Это не создаёт отдельный speaker registry и не копирует Minecraft/client state.

`DialogueSessionEngine` будет переносить visual из текущей line в `DialoguePresentation`:

- `LINE`: visual текущего speaker;
- `CHOICE` с prompt line: visual prompt speaker;
- `CHOICE` без prompt line: no visual, без client-side reuse.

## Asset convention

- `Portrait("sample:merchant")` -> `assets/sample/textures/echo_dialogue/portraits/merchant.png`.
- `SkinHead("sample:father")` -> `assets/sample/textures/echo_dialogue/skins/father.png`.
- Source содержит только namespaced ID; `.png` добавляет client resolver.
- PNG bytes не идут через network и не входят в fingerprint.

## Fingerprint

Canonical fingerprint добавить:

- visual absence/presence для каждой compiled line;
- `SpeakerVisualKind`;
- `SpeakerVisualId`.

Изменение kind/id блокирует incompatible recovery. Замена PNG bytes под тем же ID fingerprint не меняет.

## Persistence

Schema V2 остаётся без изменений. Visual metadata не сохраняется в snapshots, потому что presentation восстанавливается
из compatible compiled dialogue definition, а fingerprint защищает semantic identity.

## Network protocol

Поднять protocol до `3`. `DialogueOpenMessage` получает optional visual descriptor:

- visual kind string;
- visual ID string.

Serverbound advance/choice packets не меняются. Invalid visual descriptor на client decode/render path отбрасывается как
text-only, если можно сохранить readable dialogue text.

## Client resource resolution and rendering

Добавить новый package `forge.client.visual` с узкими responsibilities:

- mapping `SpeakerVisualRef` descriptor -> `ResourceLocation`;
- bounded cache availability/dimensions;
- static portrait rendering;
- 64x64 skin head rendering: base face UV `(8,8,8,8)` и outer head UV `(40,8,8,8)`;
- clear cache on client resource reload/logout.

Portrait сохраняет aspect ratio внутри bounded square. SkinHead fallback на text-only при missing/invalid dimensions.
Legacy 64x32 не включается в D5, пока не будет отдельного tested path.

## Layout

Panel остаётся centered. Если visual resolved:

- visual слева;
- text/choices справа;
- hitboxes считаются тем же layout, что render;
- no empty visual slot без visual;
- narrow screen deterministic fallback: скрыть visual и отрисовать text-only, если visual + text не помещаются в margins.

Существующий text-only layout должен остаться совместимым с D4.

## Packaging

Final JAR должен содержать client visual classes и public sample/test-only policy. Test-only fixture assets не должны попасть
в production JAR; production sample assets можно добавлять только как явные public samples.

## Tests

- Core: visual id/ref, presentation mapping, fingerprint changes.
- EchoScript adapter: Alias без visual, Portrait, SkinHead, invalid intrinsic/id/value, unsupported line visual, русский текст.
- Forge/network: packet round-trip без visual/Portrait/SkinHead, invalid descriptor fallback.
- GameTest: compile visual resources, descriptor transfer for LINE/CHOICE, no visual speaker, visual fingerprint recovery block, existing D4 outcomes.
- Architecture/source scans: no Minecraft/Forge in pure modules, no URL/HTTP skin lookup, sourceShape and whitespace.

## Manual acceptance

Пользователь проверяет real client:

- text-only regression;
- static portrait;
- skin head face + outer layer;
- missing-resource fallback;
- client resource reload;
- LINE/CHOICE restart.

оператор не будет отмечать visual render checks как passed без фактического наблюдения.

## Risks

- `ClientDialogueRenderer` уже близок к монолиту; visual rendering нельзя встраивать как resource resolution inside renderer.
- `DialogueOpenMessage` может вырасти выше 180 logical LOC; при необходимости descriptor вынести в отдельный network message leaf file.
- Forge client resource reload event должен оставаться client-only, иначе dedicated server smoke поймает classloading defect.

## Rollback

No commit/branch/tag/push. Откатывать только D5 files targeted patch-ами. Если client resource rendering ломает dedicated server,
сначала убрать client event wiring, сохранив neutral core/syntax/network tests для точной диагностики.

## Explicit non-goals

NPC/entity lookup, live player skin lookup, GameProfile/Mojang API, HTTP/URL/local file textures, full body, 3D head,
portrait animation, expressions, lip sync, voice, camera, movement locking, Story-specific arguments, generic provider
system, Fabric, NeoForge, Kotlin и mixins не реализуются.
