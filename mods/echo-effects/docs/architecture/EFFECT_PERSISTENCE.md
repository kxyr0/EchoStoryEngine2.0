# Effect Persistence

Persistence должна сохранять:

- `EffectInstanceId`;
- `EffectId`;
- target;
- definition revision;
- start tick или remaining duration;
- fired timeline ledger.

Текущий core runtime держит deterministic identity и lifecycle state; Forge persistence boundary остается отдельным слоем.
