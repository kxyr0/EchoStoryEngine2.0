# Echo Debug Project Map

## Modules

| Module | Responsibility | Dependencies | Does not own |
| --- | --- | --- | --- |
| `debug-core` | selection, source/path/render plans | Echo Trigger `trigger-contracts` | Minecraft, Forge, EchoScript, Trigger runtime |
| `debug-forge-1.20.1` | item, packets, UI, file IO, public service bridge | Forge, `debug-core`, Echo Trigger public Forge API | Trigger internals, EchoScript parser |

## Client flow

```text
wand held
-> key/click input
-> server request or server interaction event
-> pending is not accepted locally
-> ClientboundSelectionPacket
-> ClientSelectionMirror
-> HUD/world overlay
```

## Creation flow

```text
TriggerWandSession
-> TriggerCreationScreen
-> ServerboundCreateTriggerPacket
-> TriggerCreationService
-> TriggerSourceRenderer
-> TriggerAuthoringService.validate
-> AtomicTriggerSourceWriter
-> TriggerAuthoringService.reload
-> catalog verification
-> ClientboundCreationResultPacket
```

## Rendering flow

```text
Echo Trigger AreaCatalogView
-> ClientboundAreaCatalogPacket
-> ClientAreaCatalog
-> TriggerAreaRenderPlanner
-> TriggerWorldOverlayRenderer
```

## File map

- Core packages: `D:/JDHSeason/mods/echo-debug/debug-core/src/main/java/io/github/kxyr0/echodebug/core`
- Forge packages: `D:/JDHSeason/mods/echo-debug/debug-forge-1.20.1/src/main/java/io/github/kxyr0/echodebug/forge`
- Assets: `D:/JDHSeason/mods/echo-debug/debug-forge-1.20.1/src/main/resources/assets/echo_debug`
- Generated trigger root: `<gameDir>/story/triggers`
- Tests: `debug-core/src/test`, `debug-forge-1.20.1/src/test`
- Architecture tests: `*/src/architectureTest`

## Dependency map

```text
Echo Debug -> Echo Trigger public contracts/API
Echo Debug -X-> io.github.kxyr0.echotrigger.core
Echo Debug -X-> io.github.kxyr0.echoscript
Echo Trigger -X-> Echo Debug
```

Optional N5 debugging may display Echo NPC active form id, dimensions and
anchors through Echo NPC public API. Echo Debug must not depend on
`EchoNpcEntity` or renderer internals for this.

## Failure map

- Validation failure: no file write.
- Existing file/write failure: no success ACK.
- Reload failure: final file rollback.
- Catalog missing: no success ACK, rollback after write.
- Rollback failure: reported as diagnostic; fake client catalog entry is not created.
