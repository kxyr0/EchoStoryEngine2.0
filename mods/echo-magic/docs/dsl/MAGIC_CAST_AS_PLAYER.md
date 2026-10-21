# Magic.CastAsPlayer

`Magic.CastAsPlayer(spellId)` проверяет player context, known spell и cooldown, затем создаёт deterministic command ID. Script продолжает работу после принятия cast и не ждёт завершения projectile.

Отдельные Story/Dialogue executors не создаются: хост передаёт общий `MagicScriptExecutionContext`.

