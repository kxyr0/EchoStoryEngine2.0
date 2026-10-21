# Echo Dialogue Trigger Adapter

Standalone adapter between Echo Dialogue public API and Echo Trigger public API.

The adapter does not depend on Dialogue or Trigger implementation modules. Build it with
`buildAdapterAgainstPublishedArtifacts` to publish sibling public artifacts first and then compile against them.

Version `0.3.0` listens for Echo Trigger reactive callback invocation events and maps neutral
`Dialogue.Start("namespace:path")` actions to Echo Dialogue `DialogueService.start(...)`.
