# Recovery D5

Recovery выполняется после создания Forge host и загрузки текущего compiled dialogue catalogue.

## ACTIVE

ACTIVE snapshot восстанавливается только при exact fingerprint match. Host создаёт detached server-side session с тем же:

- `DialogueSessionId`;
- `AudienceId`;
- `revision`;
- `currentNodeId`;
- `presentationKind`;
- `lineOrdinal`.

Overlay не открывается, пока matching player не войдёт. Reattach отправляет current line или choice presentation, включая optional visual descriptor из compatible compiled definition, и не увеличивает domain revision.

D6 reattach выполняет dynamic speaker presentation resolution заново перед network send. Same `DialogueSessionId`, cursor и revision сохраняются. Provider removal/unavailable после restart не блокирует recovery: клиент получает authored fallback. Dynamic provider output не участвует в fingerprint compatibility.

## COMPLETED и CANCELLED

Terminal snapshots не открывают overlay. Они остаются queryable до explicit acknowledgement или bounded cleanup. Это сохраняет result после crash window, пока будущий external adapter не прочитает completion/cancellation.

## Blocked

Snapshot остаётся blocked и не удаляется, если:

- compiled dialogue отсутствует;
- fingerprint отличается;
- authored visual kind/id изменился и поэтому fingerprint отличается;
- current node/line/choice presentation не существует в совместимой definition;
- есть duplicate ACTIVE snapshots для одной audience.

Blocked recovery never restarts dialogue, never moves to closest line and never opens an incorrect overlay.

## Corrupt

Malformed JSON, unsupported schema, invalid IDs, invalid phase/cursor, corrupt primary+backup и runtime load failure дают diagnostic и не создают partial session.

Primary corrupt + valid backup loads backup with diagnostic. Backup-only snapshot после interrupted replacement участвует в `loadAll()` recovery.

## World Isolation

Storage root расположен внутри world save:

```text
<world-root>/data/echo_dialogue/sessions/
```

World B не читает snapshots world A, потому что repository получает другой root path.
