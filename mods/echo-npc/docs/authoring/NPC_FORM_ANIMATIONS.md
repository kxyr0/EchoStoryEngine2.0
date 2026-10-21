# Form Animations

`animationGraph` в форме выбирает граф анимаций для текущего тела. Детская форма
может иметь более короткий шаг, а взрослая - обычную locomotion-схему.

```echoscript
animationGraph: "jdh:sasha/child_locomotion"
```

Правила authoring:

- graph id должен быть стабильным resource id;
- bone names остаются semantic: `head`, `body`, `left_arm`, `right_arm`,
  `left_leg`, `right_leg`;
- изменение формы должно заново выбрать graph на стороне animation adapter;
- idle/walk poses не должны хранить durable identity.

Если animation adapter не знает graph id, NPC все равно материализуется. Ошибка
остается в animation diagnostics, а Echo NPC продолжает держать форму.
