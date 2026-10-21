# Camera track format

Внутренний формат:

- `CameraTrackSnapshot` содержит `CameraTrackMetadata` и список `CameraKeyframe`;
- keyframe хранит `timeMillis`, `CameraPose`, `CameraInterpolation`;
- position использует `io.github.kxyr0.echonavigation.api.position.Vector3`.

Поддержанные interpolation:

- `LINEAR`;
- `SMOOTH`;
- `CATMULL_ROM`.

Yaw разворачивается в core compiler, поэтому переход `359 -> 1` идет коротким путем и не делает полный оборот назад.
