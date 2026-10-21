# Fire Magic 0.1.1

## Что уже реализовано

- школа: `echo_fire_magic:fire`;
- spell: `echo_fire_magic:fire_bolt`;
- damage type: `echo_fire_magic:fire_magic`, отмечен как magic и fire;
- particle style: `echo_fire_magic:fire_flame`;
- texture: круг с жёлто-белым центром и тёмно-красно-оранжевыми краями;
- один projectile, `4.0` damage, дальность `22` блока, cooldown `20` ticks;
- trail живёт `10` ticks и поднимается примерно на `0.4` блока за это время;
- выдача и выбор: `/echo_fire_magic setup <player>`.

## Где настраивается движение огненной частицы

Параметры заклинания находятся в `fire_bolt.es`. Скорость самостоятельного подъёма кругового sprite
находится в `FireMagicClientBootstrap`: `MagicParticleMotion(0.0, 0.04, 0.0)`.

Движение не привязано к ID внутри generic engine. Поэтому новая школа может зарегистрировать
тот же generic billboard с другим вектором, а старый конструктор provider оставляет частицу
неподвижной. Void Magic продолжает использовать именно старое поведение.

## Следующий этап

Текущий общий `Hit` поддерживает только typed damage. Поджигание цели на несколько секунд,
звуки и explosion/AoE потребуют новых общих DSL-contracts, а не проверки конкретного spell ID
в движке.
