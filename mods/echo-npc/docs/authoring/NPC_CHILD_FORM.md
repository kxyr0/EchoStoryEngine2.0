# Child Form

Детская форма описывает маленькое тело без создания нового NPC.

Рекомендуемые параметры для human-like child:

- `dimensions.width`: 0.40-0.46;
- `dimensions.height`: 1.15-1.30;
- `dimensions.eyeHeight`: немного ниже `height`, обычно 1.00-1.12;
- `bodyScale`: 0.70-0.80;
- `head` можно сделать чуть крупнее, чтобы силуэт читался как ребенок.

Пример:

```echoscript
return Form(
    id: "jdh:sasha/child",
    skin: Skin("jdh:npc/sasha/child"),
    model: "echo_npc:humanoid",
    dimensions: Dimensions(width: 0.42, height: 1.22, eyeHeight: 1.06),
    bodyScale: 0.74,
    parts: [
        Part(id: "head", scale: 1.18, pivotY: -0.08),
        Part(id: "body", scaleX: 0.84, scaleY: 0.88, scaleZ: 0.84)
    ],
    animationGraph: "jdh:sasha/child_locomotion",
    faceProfile: "jdh:sasha/child_face",
    portrait: "jdh:sasha/child",
    tags: ["sasha", "child"]
);
```

Коллизия и eye height серверные. Если взрослая позиция проходит, детская обычно
тоже пройдет, но `SetForm` все равно проверяет место перед durable записью.
