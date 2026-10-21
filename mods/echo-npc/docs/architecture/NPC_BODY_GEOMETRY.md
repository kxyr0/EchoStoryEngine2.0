# NPC Body Geometry

`NpcBodyGeometry` состоит из:

- `globalScale`;
- списка `NpcBodyPartTransform`;
- server dimensions: `width`, `height`, `eyeHeight`;
- body anchors.

Semantic parts:

- `HEAD`;
- `BODY`;
- `LEFT_ARM`;
- `RIGHT_ARM`;
- `LEFT_LEG`;
- `RIGHT_LEG`.

Transforms не являются физикой. Физические проверки используют
`NpcBodyDimensions`. Поэтому визуальный head scale может быть 1.18, а collision
box все равно берется из `dimensions`.

Renderer должен считать отсутствующий transform как uniform scale 1.0. Это
позволяет формам задавать только отличающиеся части.
