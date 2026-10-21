# Layers

Face layers задаются через `Layer(...)` в `.face-rig.es`.

Поля слоя:

- local layer ID;
- parent bone;
- texture ID;
- role;
- draw order;
- локальный placement transform;
- atlas rectangle;
- optional visibility.

`x/y/w/h` являются atlas coordinates внутри PNG. Чтобы двигать brows, sclera, pupils, eyelids или glasses на лице,
редактируйте placement fields в том же `Layer(...)`:

```text
placeX: "-0.07", placeY: "0.30", placeZ: "0.63"
```

`placeZ` управляет renderer depth placement. `order` по-прежнему управляет детерминированным draw ordering при
перекрытии слоев.

Production draw planner рендерит по `order`, `role` и ID.
