# Cutscene runtime

Runtime состоит из двух независимых половин.

Сервер:

- валидирует `CutsceneId`;
- находит `CutsceneDefinition` и `CameraTrackSnapshot`;
- создает `CutscenePlayback`;
- ставит body lock через `BodyLockStore`;
- отправляет клиенту полный immutable camera track;
- завершает playback по duration или stop/skip.

Клиент:

- получает `ClientboundCutsceneStartPacket`;
- компилирует track в `CompiledCameraCurve`;
- каждый client tick считает `CameraPose`;
- применяет позу к detached camera rig;
- подавляет gameplay input, если нет открытого screen.

Lifecycle events публикуются через `CutsceneLifecycleEvent`. Сервисный lookup идет через
`CutsceneServiceLookupEvent`; это единственный supported способ для внешних модов получить `CutsceneService`.
