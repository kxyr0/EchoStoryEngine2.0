# Client sync

SimpleChannel protocol 1 передаёт cast intent, acceptance/rejection, player state snapshot и один impact event. Положение projectile синхронизирует стандартная entity network.

Client игнорирует stale state revisions и никогда не применяет damage.

