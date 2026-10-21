# Magic.GrantToPlayer

`Magic.GrantToPlayer(spellId)` использует игрока из текущего EchoScript execution context. Grant идемпотентен и сохраняется в SavedData.

При отсутствии player context возвращается диагностика `MAGIC_PLAYER_CONTEXT_MISSING`.

