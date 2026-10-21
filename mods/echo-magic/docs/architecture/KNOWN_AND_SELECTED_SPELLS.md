# Known и selected spells

`KnownSpellRegistry`, `SelectedSpellRegistry` и `SpellCooldownRegistry` разделены. Снимки содержат `MagicPlayerRef` и revision. Forge SavedData сохраняет known set, optional selection и ready ticks.

При отсутствующем content provider stale selection безопасно очищается после login/reload.

