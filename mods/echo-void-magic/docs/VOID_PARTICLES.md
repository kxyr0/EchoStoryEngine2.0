# Void particles

`echo_void_magic:void_circle` использует общий `MagicBillboardParticleType`: camera-facing, full-bright, no physics, additive blend. Texture содержит radial green gradient: яркий neon-green центр и глубокий тёмно-зелёный край. Trail: spacing 0.18, size 0.30, lifetime 8, tint `#0F6B32`, alpha 0.90→0. Core: size 0.16, tint `#45FF4A`.

`jdh:void_sword` использует ту же частицу для клиентского следа взмаха. На каждом шаге атаки частицы размещаются вдоль текущего положения клинка и между соседними положениями его кончика. Lifetime равен 5 тикам, поэтому дуга остаётся заметной, но исчезает примерно за 0,25 секунды. Если предмет с ID `jdh:void_sword` отсутствует, обработчик ничего не делает.

Для Epic Fight используется отдельный `echo-void-magic-epicfight-adapter`. Он читает фактическое состояние `attacking` и время текущей `AttackAnimation`, поэтому не зависит от ванильных `swinging/swingTime`. При нанесении реального урона адаптер дополнительно рассылает с сервера короткий пустотный разрез на цели; этот hit-эффект работает и в ванильном режиме боя.

Impact создаёт восемь cosmetic particles. Частицы никогда не выполняют collision или damage.
