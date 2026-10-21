# Speaker Visuals D6

D5 добавляет optional visual на speaker alias. D6 делает visual reference публичным neutral contract в `dialogue-contracts` для dynamic speaker presentation providers. Visual принадлежит Echo Dialogue и не передаётся в Story Engine или Story-Dialogue Adapter.

## Syntax

```csharp
Alias("father", "sample:father", display: "Отец", visual: SkinHead("sample:father"));
Alias("merchant", "sample:merchant", display: "Торговец", visual: Portrait("sample:merchant"));
Alias("player", "sample:player", display: "Игрок");
```

Поддерживаются ровно два intrinsic:

- `Portrait("namespace:path")`;
- `SkinHead("namespace:path")`.

`visual` отсутствует -> text-only. `Line(..., visual: ...)`, URL, raw paths, `.png` в ID, unknown intrinsic и malformed IDs отклоняются semantic diagnostics.

Name color syntax uses the same alias-level fallback model:

```csharp
Alias("player", "sample:player", display: "Player", nameColor: nameColor("#57C7FF"));
```

`nameColor` is optional and accepts RGB hex through the `nameColor(...)` intrinsic. Opaque
CSS-style `#RRGGBBAA` input is accepted and normalized to `#RRGGBB`.

## Model

Neutral types находятся в `dialogue-core`:

- `SpeakerVisualKind`: `PORTRAIT`, `SKIN_HEAD`;
- `SpeakerVisualId`: bounded namespaced ID;
- `SpeakerVisualRef`: immutable kind/id pair.

Java package:

`io.github.kxyr0.echodialogue.api.visual`

D6 provider result may return optional `SpeakerVisualRef`. It still cannot return `ResourceLocation`, raw path, URL, PNG bytes, `GameProfile`, Entity or Minecraft skin service objects.

For provider-returned `SKIN_HEAD`, the client tries the normal Dialogue skin path
and then `textures/echo_npc/skins`. This is only a client resource lookup rule;
the neutral public contract still carries `SpeakerVisualRef` and not a raw path.

Core не содержит `ResourceLocation`, `Path`, texture object, `NativeImage`, `GameProfile`, URL или image bytes.

## Presentation

`DialoguePresentation` переносит optional visual descriptor только когда есть speaker context:

- `LINE`: visual текущей строки;
- `CHOICE`: visual prompt line, если prompt есть;
- `CHOICE` без prompt: no visual.

Клиент не запоминает старого speaker для выбора и не выводит empty visual slot.
