# Local transforms

A0 использует local transforms, которые компонуются через parent hierarchy.

`TransformEvaluator` рекурсивно вычисляет parent world transforms, затем добавляет child translation, Z rotation и
scale. Значения должны быть finite. Non-finite transforms отклоняются при создании contract object.

EchoScript integer transform fields задаются в тысячных: `x: 70` означает `0.07`. Для ручного authoring также
принимаются quoted decimals: `x: "0.07"`, `z: "0.63"`, `rotationZ: "11.7"`.

Для `Layer(...)` используйте `placeX/placeY/placeZ` вместо `x/y/z`, потому что layer `x/y/w/h` являются atlas
coordinates.
