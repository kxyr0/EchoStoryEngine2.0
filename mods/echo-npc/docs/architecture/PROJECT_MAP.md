# Project Map

## Root

- `settings.gradle` - root name и список modules.
- `build.gradle` - repositories, quality gates, publication и checks.
- `gradle.properties` - версии, mod identity и publication identity.
- `config/checkstyle/checkstyle.xml` - style checks.
- `config/pmd/ruleset.xml` - PMD rules.

## Modules

- `npc-contracts/src/main/java/io/github/kxyr0/echonpc/api/identity`
  - `ActorId`, `NpcDefinitionId`, `NpcSkinId`, `NpcWorldId`, `NpcDefinitionFingerprint`.
- `npc-contracts/src/main/java/io/github/kxyr0/echonpc/api/service`
  - `NpcService`, request/result types и read models.
- `npc-contracts/src/main/java/io/github/kxyr0/echonpc/api/appearance`
  - name color и public `Npc.ChangeSkin` request/result contracts.
- `npc-contracts/src/main/java/io/github/kxyr0/echonpc/api/block`
  - `BlockId` для exact authored bed validation.
- `npc-contracts/src/main/java/io/github/kxyr0/echonpc/api/spatial`
  - `BlockPosition`, `NpcPosition`, `NpcRotation`.
- `npc-contracts/src/main/java/io/github/kxyr0/echonpc/api/scene`
  - `NpcMaterializationService`, `NpcSceneCommandId`, `NpcSceneReadinessView`.
- `npc-contracts/src/main/java/io/github/kxyr0/echonpc/api/rest`
  - `NpcRestService`, `NpcRestQueryService`, provider registry и rest attachment view.
- `npc-contracts/src/main/java/io/github/kxyr0/echonpc/api/form`
  - `NpcFormId`, form status/result/catalog contracts.
- `npc-contracts/src/main/java/io/github/kxyr0/echonpc/api/form/body`
  - body dimensions, semantic body parts, transforms and anchors.
- `npc-contracts/src/main/java/io/github/kxyr0/echonpc/api/form/definition`
  - authored form definitions, snapshots and `NpcFormChangeRequest`.
- `npc-core/src/main/java/io/github/kxyr0/echonpc/core/definition`
  - definitions, fingerprints, catalog state.
- `npc-core/src/main/java/io/github/kxyr0/echonpc/core/actor`
  - desired actor state, entity binding id, runtime registry и occupied rest lookup.
- `npc-core/src/main/java/io/github/kxyr0/echonpc/core/rest`
  - durable `NpcRestAttachment`.
- `npc-persistence/src/main/java/io/github/kxyr0/echonpc/persistence/schema`
  - actor save schema, schema v2 rest records, mapper and deterministic JSON codec.
- `npc-persistence/src/main/java/io/github/kxyr0/echonpc/persistence/file`
  - world-scoped actor file repository, atomic writes, backup and tombstone flow.
- `npc-echoscript-adapter/src/main/java/io/github/kxyr0/echonpc/echoscript`
  - `.npc.es` and `.form.es` compiler facades.
- `npc-forge-api-1.20.1/src/main/java/io/github/kxyr0/echonpc/forge/api`
  - Forge service lookup bridge.
- `npc-forge-1.20.1/src/main/java/io/github/kxyr0/echonpc/forge`
  - mod entrypoint, host, persistence queue, recovery, entity, commands, resources, client renderer.

## N4 Runtime

- `forge/spawn/NpcSpawnPositionValidator.java`
  - validation for `Npc.SpawnAt`.
- `forge/operation/ForgeNpcRestOperations.java`
  - `Npc.SleepAt` and `Npc.WakeUp` mutation path.
- `forge/rest/ForgeRestRuntime.java`
  - provider resolution, attachment application, release and wake position.
- `forge/rest/VanillaBedRestProvider.java`
  - exact vanilla bed provider.
- `forge/command/EchoNpcSceneCommandActions.java`
  - `/echo_npc spawn_at`, `sleep_at`, `wake_up`, `inspect_rest`, `inspect_bed`, `rest_providers`.

## N5 Runtime

- `forge/operation/form/ForgeNpcFormOperations.java`
  - `Npc.SetForm` validation and save-before-materialization mutation.
- `forge/operation/place/ForgeNpcPlaceOperations.java`
  - `Npc.PlaceAt` placement with active form dimensions.
- `forge/entity/EchoNpcFormState.java`
  - compact synchronized form snapshot for renderer.
- `forge/client/form`
  - client-side humanoid model part transforms.
- `forge/command/EchoNpcFormCommandActions.java`
  - `/echo_npc forms`, `/echo_npc form get/set/validate_space`.

## Dynamic Skins

- `forge/operation/skin/ForgeNpcSkinOperations.java`
  - durable save-before-publication implementation of `Npc.ChangeSkin`.
- `forge/command/EchoNpcSkinCommandActions.java`
  - `/echo_npc skin get/set`.
- `forge/entity/EchoNpcEntity.java`
  - synchronized active texture ID; body/form identity остаются отдельными.

## Resources

- `npc-forge-1.20.1/src/main/resources/data/sample/echo_npc/npcs`
  - sample `.npc.es` definitions.
- `npc-forge-1.20.1/src/main/resources/data/*/echo_npc/forms`
  - authored `.form.es` definitions.
- `npc-forge-1.20.1/src/main/resources/assets/*/textures/echo_npc/skins`
  - sample skin and fallback skin.

## External Adapters

- `D:/JDHSeason/mods/echo-story-npc-adapter`
  - Story extension action ids `echo_npc:spawn_at`, `echo_npc:sleep_at`, `echo_npc:wake_up`.
