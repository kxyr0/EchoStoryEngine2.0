# Dynamic speakers

D6 в Echo Dialogue добавляет server-side provider boundary для runtime-only speaker presentation.

Authored fallback задается в `Alias(...)`:

```csharp
Alias("father", "jdh:john_father", display: "Отец", visual: Portrait("jdh:john_father"));
```

Provider может заменить:

- display name;
- optional `SpeakerVisualRef`.

Provider не меняет:

- `SpeakerId`;
- `DialogueId`;
- line text;
- choices;
- `DialogueOutcomeId`;
- persistence snapshot;
- compiled fingerprint.

Без N4 provider-а authored fallback работает нормально.

N4 живет в отдельном repo `echo-dialogue-npc-adapter`. Он связывает speaker с NPC
только когда canonical `SpeakerId` равен canonical `ActorId`:

```text
SpeakerId("jdh:john_father") == ActorId("jdh:john_father")
```

Если actor найден, N4 возвращает текущее `NpcActorView.displayName()` и, когда
есть `NpcSkinId`, `SpeakerVisualRef.skinHead(npcSkinId)`. Для `jdh:john_father`
skin PNG остается в NPC-owned path:

```text
assets/jdh/textures/echo_npc/skins/john_father.png
```

Authored portrait в `Alias(...)` остается fallback. Если у NPC нет skin, N4
сохраняет authored visual из request, поэтому portrait не исчезает.
