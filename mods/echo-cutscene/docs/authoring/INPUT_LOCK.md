# Input lock

Во время cutscene клиент блокирует gameplay input:

- movement;
- jump/sneak/sprint;
- attack/use;
- drop/swap;
- hotbar slots;
- save/load toolbar keys.

Если открыт screen, suppressor не отменяет input. Это нужно для Dialogue UI: игрок может продолжить выбирать реплики,
но не может двигать тело или использовать предметы в мире.

Серверная защита дублирует это body lock-ом. Даже если клиент отправит движение, `ServerPlayer` возвращается в
сохраненную позицию.
