# N1 Limitations

N1 намеренно не реализует:

- durable persistence across restart;
- restart recovery;
- animation state machines;
- animation clips;
- GeckoLib integration;
- Blockbench playback;
- navigation;
- autonomous movement;
- AI goals;
- schedules;
- dialogue interaction;
- Story actions;
- combat;
- inventory;
- equipment;
- wardrobe;
- vehicles;
- cinematics.

Не добавляйте пустые classes, interfaces или commands для этих систем в N1.

Cross-dimension teleport в текущей реализации отклоняется как
`PLATFORM_REJECTED`; скрытая пересоздаваемая entity не используется.
