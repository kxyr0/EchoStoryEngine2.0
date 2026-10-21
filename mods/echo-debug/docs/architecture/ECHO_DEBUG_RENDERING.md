# Echo Debug rendering

Core строит immutable render plans:

- `TriggerAreaRenderPlanner`
- `TriggerAreaRenderPlan`
- `RenderLine`
- `RenderLabel`

Forge renderer:

- `TriggerWorldOverlayRenderer`
- `EchoDebugHudOverlay`
- `ClientAreaCatalog`
- `ClientSelectionMirror`

## Источник данных

Existing zones приходят от Echo Trigger public `AreaCatalogView` через `ClientboundAreaCatalogPacket`.
Echo Debug не читает `.trigger.es` для визуализации.

Current preview строится из authoritative `ClientboundSelectionPacket`.

## Performance counters

Core render plan фиксирует:

```text
sourceParserCalls = 0
```

При wand не в руке Forge renderer не строит plans и не рисует lines.
