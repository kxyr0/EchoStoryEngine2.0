# Dynamic Speaker Presentation D6

D6 добавляет runtime-only boundary для внешних модов, которым нужно подменить текущее представление `SpeakerId` перед отправкой LINE или CHOICE клиенту.

Provider может вернуть только полный `ResolvedSpeakerPresentation`:

- display name;
- optional `SpeakerNameColor`;
- optional `SpeakerVisualRef`.

Provider не может менять:

- `SpeakerId`;
- `DialogueSessionId`;
- `DialogueId`;
- `AudienceId`;
- line text;
- choices, их порядок и target node;
- `DialogueOutcomeId`;
- session revision;
- persistence snapshot;
- network protocol.

Authored fallback остаётся обязательным и берётся из `Alias(...)`: display name и optional D5 `Portrait(...)` или `SkinHead(...)`.

Authored fallback name color also comes from `Alias(... nameColor: nameColor("#RRGGBB"))`.
Opaque CSS-style `#RRGGBBAA` input is accepted and normalized to `#RRGGBB`.

## Public API

Published artifact:

`io.github.kxyr0.echodialogue:dialogue-presentation-api:0.1.0-SNAPSHOT`

API version:

`SpeakerPresentationApiVersion.current()` -> major `1`, minor `2`.

Neutral types:

- `SpeakerPresentationProviderId`;
- `SpeakerPresentationApiVersion`;
- `SpeakerPresentationProviderDescriptor`;
- `SpeakerPresentationRequest`;
- `ResolvedSpeakerPresentation`;
- `SpeakerPresentationResolution`;
- `SpeakerPresentationProviderSession`.

`SpeakerPresentationRequest` contains:

- `DialogueSessionId`;
- `DialogueId`;
- `SpeakerId`;
- `AudienceId`;
- kind `LINE` or `CHOICE`;
- authored fallback display name;
- optional authored fallback `SpeakerNameColor`;
- optional authored fallback `SpeakerVisualRef`.

It intentionally does not contain line text, choice list, target node, outcome, compiled dialogue object, server/player/entity object, network packet or arbitrary metadata map.

## Lifecycle

Provider sessions are created once per Echo Dialogue host after `MinecraftServer` exists. They are closed on server stop and recreated on the next integrated-server world or dedicated-server restart.

Resolution runs on the server thread only. Providers must return quickly and must not block on disk, network or another thread.

Dynamic output is not persisted and is not cached as authoritative state. D6 does not implement automatic same-line refresh when external state changes.
