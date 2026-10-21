# Targets

`Targets(LIVING_EXCEPT_CASTER)` исключает caster, мёртвые, removed, spectator и marker armor stands. При `FriendlyFire(false)` сначала опрашиваются `MagicTargetRelationProvider`, затем vanilla team relation.

Имя Java-класса NPC не используется: любая настоящая `LivingEntity` проходит общий damage pipeline.

