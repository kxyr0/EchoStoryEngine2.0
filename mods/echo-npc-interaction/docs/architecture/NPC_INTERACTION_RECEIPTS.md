# NPC Interaction Receipts

Receipt сохраняет результат interaction и нужен для idempotency:

- command id;
- npc id;
- provider id;
- target position;
- block id;
- before fingerprint;
- after fingerprint;
- interaction result;
- verification policy;
- outcome;
- revision.

Если receipt существует после restart, runtime возвращает сохранённый result и
не вызывает use pipeline повторно.
# Receipt v2

NI2 receipt сохраняет consequence evidence: kinds, spawned entities, proxy vehicle before/after, ride handoff strategy, Echo anchor, occupancy, passenger confirmation и cleanup status.

После restart существующий receipt возвращается без повторного block use. Для ride consequence повторный use запрещён, если уже есть подтверждённая occupancy/attachment.
