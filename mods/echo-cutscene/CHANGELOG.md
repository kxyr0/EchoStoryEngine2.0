# Changelog

## 0.2.2

- Added client-side black fade-out/fade-in when a stop packet returns camera control to the player.

## 0.2.1

- Added explicit hold-last-frame playback mode for cutscenes that stop via `Cutscene.Stop(id)`.
- Added id-checked cutscene stop behavior while preserving active skip/command stop.

## 0.2.0

- Added C2 camera mode, target, and framing tracks while preserving old FREE camera tracks.
- Added authoring look-at modes for player and fixed-point targets.

## 0.1.0

- Создан C1-срез Echo Cutscene: detached camera, body lock, playback contracts, Forge API, runtime host и authoring hooks.
