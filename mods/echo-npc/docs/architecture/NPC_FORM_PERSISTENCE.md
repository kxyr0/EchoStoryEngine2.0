# NPC Form Persistence

Actor save schema v3 добавляет optional block `activeForm`:

- `formId`;
- `revision`;
- `fingerprint`.

Сохранение не дублирует полный `NpcFormDefinition`. Runtime хранит ссылку на
authored form snapshot и сверяет ее с текущим catalog при восстановлении.

Поведение восстановления:

- form есть и fingerprint совместим - actor материализуется с этой формой;
- form отсутствует - actor остается durable, получает blocked diagnostic;
- старые schema v1/v2 без `activeForm` читаются как actor без пиннутой формы;
- default form применяется при новом spawn, а не при silent migration старых
  сохранений.

Это защищает durable identity: сохраненный `ActorId` не заменяется новым NPC при
обновлении контента.
