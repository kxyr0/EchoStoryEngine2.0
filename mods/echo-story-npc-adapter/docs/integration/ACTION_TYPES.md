# Типы Action

Production-адаптер предоставляет ровно:

- `echo_npc:spawn`;
- `echo_npc:teleport`;
- `echo_npc:despawn`.

`echo_npc:move` не объявлен и не должен добавляться в N3.

Все значения payload являются строками, которые передает Story Runtime.
Неизвестные payload keys отклоняются до отправки NPC mutation.
