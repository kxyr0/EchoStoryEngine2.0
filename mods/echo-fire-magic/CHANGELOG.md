# Changelog

## 0.3.2

- Blacksmith's Hand now prefers a smeltable held item and completes in the cast tick.
- The immediate result uses the vanilla smelting recipe and a compact heat burst.

## 0.3.1

- Все `.es` заклинания, включая `Руку кузнеца`, перенесены в JDH story content pack.
- Школьный JAR сохраняет runtime нагрева, частицы, damage type и block resources, но больше не дублирует spell definitions.

## 0.3.0

- Добавлено утилитарное заклинание `Рука кузнеца` для техники `POINT`.
- После пяти секунд видимого прогрева заклинание переплавляет ровно один предмет по обычному рецепту печи.
- Добавлена нагреваемая `Кузнечная заготовка` с холодным и раскалённым состояниями.
- Сторонние блоки подключаются через тег `echo_fire_magic:blacksmith_heatable` и событие `BlacksmithHeatBlockEvent`.

## 0.2.2

- Для ванильного particle-режима добавлена огненная палитра `vanillaColor`.

## 0.2.1

- Добавлено расширяющееся `Fire Ring` для техники `CIRCLE`.

## 0.2.0

- Fire Bolt поджигает цель на 80 ticks.
- Добавлен `Fire Sweep` для жеста `ARC`: веерная огненная атака перед игроком.

## 0.1.1

- Вытянутая flame-texture заменена круглой particle в общем стиле школ магии.
- Новый fire-gradient: горячий жёлто-белый центр, оранжевое свечение и тёмно-красно-оранжевые края.
- Стабильный ID `echo_fire_magic:fire_flame` сохранён для совместимости DSL и сохранений.
- Движение частицы вверх `0.04` блока за тик сохранено.

## 0.1.0

- Добавлена первая школа `echo_fire_magic:fire`.
- Добавлено DSL-заклинание `echo_fire_magic:fire_bolt`.
- Добавлены огненный damage type и частица `echo_fire_magic:fire_flame`.
- Пламя получает независимое движение вверх через публичный API Echo Magic 0.1.1.
- Добавлена команда `/echo_fire_magic setup <player>`.
