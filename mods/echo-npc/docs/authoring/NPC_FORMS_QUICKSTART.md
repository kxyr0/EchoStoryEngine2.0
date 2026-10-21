# NPC Forms Quickstart

N5 вводит формы NPC: один durable NPC остается тем же `ActorId`, но его тело,
скин, габариты, anchors, анимационный профиль и portrait берутся из активной
`NpcFormId`.

Минимальный набор для Саши:

```echoscript
return Npc(
    id: "jdh:sasha",
    display: "Саша",
    skin: Skin("jdh:npc/sasha/adult"),
    defaultForm: DefaultForm("jdh:sasha/child")
);
```

Формы кладутся в `data/<namespace>/echo_npc/forms/**/*.form.es`.

```echoscript
return Form(
    id: "jdh:sasha/child",
    skin: Skin("jdh:npc/sasha/child"),
    model: "echo_npc:humanoid",
    dimensions: Dimensions(width: 0.42, height: 1.22, eyeHeight: 1.06),
    bodyScale: 0.74,
    animationGraph: "jdh:sasha/child_locomotion",
    faceProfile: "jdh:sasha/child_face",
    portrait: "jdh:sasha/child"
);
```

После `/reload` проверь:

- `/echo_npc forms list`;
- `/echo_npc forms inspect jdh:sasha/child`;
- `/echo_npc form get jdh:sasha`;
- `/echo_npc form set jdh:sasha jdh:sasha/adult`.

`ActorId` при смене формы не меняется. Диалоги, сюжетные flags и сохраненный
actor state продолжают ссылаться на того же NPC.
