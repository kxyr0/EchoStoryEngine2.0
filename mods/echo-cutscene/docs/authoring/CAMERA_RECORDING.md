# Camera recording

Core содержит primitives для recorder pipeline:

- `CameraRecordingSession` принимает raw samples;
- `CameraSampleResampler` переводит variable FPS в фиксированную частоту;
- `CameraTrackSimplifier` убирает лишние keyframes и сохраняет endpoints;
- `CameraSourceWriter` пишет `.camera.es` source.

Forge runtime `0.1.0` уже проигрывает такие tracks. Полный in-game authoring UI с F4/HUD должен быть следующим
слоем поверх этих core классов и `echo-debug`.

Ручная приемка в Minecraft обязательна: нужно проверить, что камера отрывается, тело остается в мире, gameplay input
не проходит, а Dialogue screen продолжает принимать ввод.
