# Жизненный цикл cast

Статусы отделены от outcomes: `CREATED/VALIDATING/ACCEPTED/EMITTING/ACTIVE_PROJECTILES/COMPLETED/CANCELLED/FAILED`. Intent проверяется на сервере; cooldown и swing начинаются только после acceptance.

Смерть caster отменяет remaining emissions и удаляет его активные projectile.

