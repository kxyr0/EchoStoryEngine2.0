# Project Map D6

`DialogueResource` backpack authoring lowers `Npc.EquipBackpack` and `Npc.UnequipBackpack` into durable external
wait commands owned at runtime by the independent Sophisticated Backpacks adapter.

## Production Modules

- `dialogue-contracts`: neutral IDs, diagnostics, service/session contracts, outcomes, choices, public `api.visual`
  types and external command payload encoding.
- `dialogue-presentation-api`: dynamic speaker presentation provider API `1.0`.
- `dialogue-core`: compiled dialogue graph, external command waits, presentation read model, session engine,
  fingerprint and persistence port.
- `dialogue-persistence`: save schema V1/V2, JSON codec, repository and recovery planning.
- `dialogue-echoscript-adapter`: EchoScript syntax-only adapter for `.dialogue.es`.
- `dialogue-forge-api-1.20.1`: public Forge events for `DialogueService` lookup, external wait dispatch and speaker
  presentation provider registration.
- `dialogue-forge-1.20.1`: Forge host, resource reload, commands, persistence scheduling, D6 provider catalog, networking, client UI and final packaging.

## Integration Test Build

`integration-tests/echo-dialogue-presentation-test-mod` is a separate Gradle build. It compiles against `build/integration-maven` public artifacts only and produces `echo-dialogue-presentation-test-0.1.0-SNAPSHOT.jar`.

It is not a root subproject and is not embedded into the Echo Dialogue production JAR.
