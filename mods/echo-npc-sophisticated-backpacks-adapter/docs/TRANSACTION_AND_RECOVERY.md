# Транзакции, persistence и recovery

Journal находится в
`<world>/data/echo_npc_sophisticated_backpacks.dat`. Запись выполняется
синхронно во временный файл, flush + `FileChannel.force(true)`, затем atomic
replace (с безопасным fallback, если filesystem не поддерживает atomic move).

Persisted record содержит ActorId, attachment/transition/command/reservation
IDs, exact `ItemStack` NBT, attachment point, source/destination, ожидаемый
fingerprint, navigation move, marker receipts, phase, status, diagnostic and
revision.

## Destructive boundaries

- До удаления source: payload stack и UUID уже находятся в journal.
- Source fingerprint проверяется повторно непосредственно перед remove.
- После placement: destination receipt сохраняется, пока payload ещё есть в
  record; только следующим durable write очищает attachment.
- Любая ошибка после destructive boundary переводит record в
  `RECOVERY_REQUIRED`, не выбрасывая item и не заменяя занятый блок.

## Recovery matrix

| Мир | Persisted payload | Решение |
|---|---:|---|
| source существует | нет | source оставить, безопасно завершить ошибкой |
| source отсутствует | есть | продолжить с persisted payload |
| matching source существует | есть | удалить duplicate source и продолжить |
| source отсутствует | нет | manual recovery required |
| destination содержит expected UUID | есть | подтвердить placement и очистить attachment |
| destination занят другим block | есть | payload оставить на BACK, recovery required |

При startup все незавершённые records проходят эту матрицу до обычного tick
processing. `/echo_npc_backpack recover <npc_id>` повторяет безопасную проверку.
Recovery никогда молча не ставит рюкзак поверх занятого destination.

## Idempotency and reservations

Command ID — durable idempotency key; active и terminal повторные вызовы не
создают второй transition. Semantic marker receipt имеет ключ
`markerId@playbackId:playbackRevision`; точный duplicate игнорируется.
Reservation принадлежит одному ActorId и охватывает exact dimension + block
position до terminal/recovery решения. Один ActorId не может владеть более чем
одним payload, один `contentsUuid` не должен иметь двух владельцев.
