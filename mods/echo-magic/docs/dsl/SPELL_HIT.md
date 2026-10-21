# Hit

`Hit(Damage(type: "namespace:type", amount: 2.0))` создаёт data-driven `DamageSource` и вызывает `LivingEntity.hurt`. Прямое изменение health запрещено.

`MagicHitLedger` допускает одну damage execution на `SpellProjectileId`; impact particles не наносят AoE damage.

Дополнительные вложенные вызовы обрабатываются generic engine:

```csharp
Hit(
    Damage(type: "namespace:type", amount: 2.0),
    Ignite(duration: Ticks(80)),
    Extinguish(false),
    Knockback(strength: 1.0, direction: FORWARD),
    DeflectProjectiles(false)
);
```

`Ignite` задаёт длительность горения, `Extinguish(true)` тушит цель, `Knockback` толкает вперёд либо вбок (`LATERAL`), а `DeflectProjectiles(true)` отклоняет стрелы и другие projectile рядом с траекторией spell.
