# Magic projectiles

В production существует один generic `MagicProjectileEntity`. Payload содержит typed IDs, revision, caster UUID, движение, collision/damage и visual parameters. Вид projectile строится локально, без packet на каждую trail point.

Projectile не сохраняется при world shutdown (`shouldBeSaved=false`).

