# Magic damage

Damage type берётся из dynamic registry по `MagicDamageTypeId`. Источник содержит direct generic projectile и caster; стандартный `hurt` pipeline сохраняет броню, щит и invulnerability semantics damage type.

Spell/cast/projectile metadata остаются в bounded `MagicHitReceipt`, так как vanilla `DamageSource` не имеет безопасного extension-поля.

