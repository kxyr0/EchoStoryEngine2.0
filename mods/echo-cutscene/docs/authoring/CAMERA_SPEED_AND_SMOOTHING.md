# Speed and smoothing

Скорость playback хранится в `CutsceneDefinition.speed`. Значение `1.0` означает реальное время track-а.

Interpolation задается на keyframe:

- `LINEAR` - ровное движение между точками;
- `SMOOTH` - ease-in/ease-out;
- `CATMULL_ROM` - плавная пространственная кривая через соседние точки.

Yaw unwrap выполняется до evaluation. Поэтому при переходе через 0 градусов камера не делает лишний полный оборот.

Для C1 preview встроенный track использует `SMOOTH` и длительность 6000 ms.
