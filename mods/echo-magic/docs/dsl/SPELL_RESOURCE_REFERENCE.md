# SpellResource

`SpellResource` — Init-only описание заклинания. Метод `Init()` вызывает `Id`, `DisplayName`, `School`, опциональный `Technique`, `Cooldown`, `Cast`, `Burst`, `Projectile`, `Collision`, `Targets`, `FriendlyFire`, `Trail`, `Core` и `Hit`. `Build()`, callbacks и Java executor не нужны. Если `Technique` не указан, используется `LINE` для совместимости со старыми spell-файлами.

Файл проходит EchoScript parser ровно один раз, затем binder создаёт immutable candidate и validator проверяет server limits. Ошибочный reload не заменяет last-known-good каталог.
