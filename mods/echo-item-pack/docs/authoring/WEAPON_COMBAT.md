# Weapon combat

Live-reloadable combat fields:

- `damage`
- `attackDamage`
- `attacksPerSecond`
- `attackSpeedModifier`
- `knockback`
- `durability`
- `enchantability`
- `repairIngredient`
- `rarity`
- `glint`
- `fireResistant`

`damage` и `attackDamage` являются alias одной характеристики. Их нельзя указывать вместе.

`attacksPerSecond` задает пользовательское число ударов в секунду. Forge item превращает его в
Minecraft attribute modifier через формулу:

```text
attackSpeedModifier = attacksPerSecond - 4.0
```

Если указан `attackSpeedModifier`, `attacksPerSecond` указывать нельзя.

Hot combat reload:

1. Игрок держит уже зарегистрированный `EchoWeaponItem`.
2. `.weapon.es` меняет `damage` или `attacksPerSecond`.
3. `/echo_item reload` публикует новый immutable catalog revision.
4. Тот же Java object читает новое значение из catalog без re-equip.

Недопустимые значения отклоняют весь batch, а previous known-good остается активным.
