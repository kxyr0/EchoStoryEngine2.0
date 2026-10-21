# Collision

`Collision(blocks: STOP, entities: FIRST_VALID_TARGET)` включает swept segment от previous до next position. Сначала вычисляются ближайшие block и entity intersections, затем выбирается фактически ближайшая.

Стекло имеет collision shape и останавливает projectile. Вода и лава игнорируются, потому что используется `ClipContext.Fluid.NONE`.

