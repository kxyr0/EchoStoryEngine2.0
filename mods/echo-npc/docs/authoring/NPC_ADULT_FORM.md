# Adult Form

Взрослая форма того же NPC использует тот же `ActorId`, но другой `NpcFormId`.
Для обычного humanoid оставляй габариты близкими к vanilla player:

- `dimensions.width`: 0.60;
- `dimensions.height`: 1.80;
- `dimensions.eyeHeight`: 1.62;
- `bodyScale`: 1.0.

Пример:

```echoscript
return Form(
    id: "jdh:sasha/adult",
    skin: Skin("jdh:npc/sasha/adult"),
    model: "echo_npc:humanoid",
    dimensions: Dimensions(width: 0.60, height: 1.80, eyeHeight: 1.62),
    bodyScale: 1.0,
    animationGraph: "jdh:sasha/adult_locomotion",
    faceProfile: "jdh:sasha/adult_face",
    portrait: "jdh:sasha/adult",
    tags: ["sasha", "adult"]
);
```

Если форма должна появиться после time skip, вызывай `Npc.SetForm` только после
черного экрана или другого закрывающего transition. Игрок не должен видеть
мгновенную замену тела в кадре.
