# Projectile

`Projectile(speed:, range:, radius:, gravity:, pierce:)` описывает generic `MagicProjectileEntity`. Значения проверяются server limits; parser при cast не вызывается.

Projectile хранит pinned revision, caster, cast/projectile ID, движение, дальность и hit receipt. Частица не является projectile.

