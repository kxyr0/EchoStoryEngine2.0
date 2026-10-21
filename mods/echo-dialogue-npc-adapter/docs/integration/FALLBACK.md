# Fallback policy

Provider returns `NOT_HANDLED` for:

- non-NPC speakers;
- missing actor;
- durable actor with `desiredPresence == ABSENT`;
- unavailable Echo NPC service;
- duplicate Echo NPC service provider.

Provider returns `TEMPORARILY_UNAVAILABLE` or `FAILURE` for known actor states
that should fall back to authored Dialogue presentation while surfacing a bounded
provider diagnostic.

`NpcResults.QueryStatus.SERVICE_STOPPED` maps to `TEMPORARILY_UNAVAILABLE`.
`NpcResults.QueryStatus.FAILURE`, query exceptions and corrupt actor states map
to `FAILURE`.

Fallback is owned by Echo Dialogue host resolution. The adapter does not render
or mutate authored presentation itself.
