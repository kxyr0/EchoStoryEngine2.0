# Form Camera Anchors

Anchors описывают точки тела, к которым могут привязываться камера, взгляд,
портретная рамка или cutscene focus.

Встроенные anchors:

- `feet`;
- `chest`;
- `head`;
- `eyes`.

Пример:

```echoscript
anchors: [
    Anchor(id: "feet", x: 0, y: 0, z: 0),
    Anchor(id: "eyes", x: 0, y: 1.06, z: 0)
]
```

Для детской и взрослой формы anchors должны быть разными, потому что eye height
и камера меняются. Если anchors не указаны, compiler создает базовые anchors из
dimensions, но для важных сцен лучше задавать их явно.
