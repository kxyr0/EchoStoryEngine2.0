# Trail и Core

`Trail` задаёт particle style ID, spacing, size, lifetime, runtime tint и alpha curve. `Core` задаёт яркую частицу центра.
Обе секции принимают необязательный `vanillaColor`; при его отсутствии используется обычный `color`.

Можно написать `particle: "minecraft:dust"`, чтобы получить ванильную частицу с точным `vanillaColor`, или выбрать
обычную simple particle вроде `minecraft:flame`, `minecraft:splash` либо `minecraft:cloud`. Для simple particle цвет
задаёт сама Minecraft. Клиентская настройка `use_vanilla_colored_particles` заменяет все spell-defined стили на
цветную `minecraft:dust` без изменения `.es`.

Клиентские `echo_particle_scale` и `vanilla_particle_scale` независимо регулируют размер Echo billboard и
`minecraft:dust`. Defaults: `0.7` и `3.0`; их можно переключать в экране настроек Echo Magic без перезапуска мира.

Клиент переносит остаток spacing на следующий segment, поэтому скорость и FPS не создают крупных разрывов. Particle budget меняет только косметическую плотность.

Сам `.es` задаёт вид следа, но не собственную скорость sprite после появления. Content-мод может зарегистрировать generic billboard через `MagicBillboardParticleProvider` и передать `MagicParticleMotion(x, y, z)`. Например, Fire Magic использует `y = 0.04`, поэтому пламя тянется вверх. Старый конструктор provider использует `MagicParticleMotion.NONE`, поэтому существующие Void-частицы не двигаются сами по себе.
