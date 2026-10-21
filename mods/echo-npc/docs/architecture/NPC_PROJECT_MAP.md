# NPC project map N5

Рабочий репозиторий: `D:/JDHSeason/mods/echo-npc`.

Новые public contracts:

- `npc-contracts/.../api/block/BlockId.java`
- `npc-contracts/.../api/spatial/BlockPosition.java`
- `npc-contracts/.../api/scene/NpcMaterializationService.java`
- `npc-contracts/.../api/rest/NpcRestService.java`
- `npc-contracts/.../api/rest/NpcRestQueryService.java`
- `npc-contracts/.../api/rest/NpcRestProviderRegistry.java`
- `npc-contracts/.../api/form/NpcFormId.java`
- `npc-contracts/.../api/form/definition/NpcFormSnapshot.java`
- `npc-contracts/.../api/form/body/NpcBodyDimensions.java`

Runtime ownership:

- NPC identity: `ActorId`, `NpcDefinitionId`;
- materialization: `ForgeNpcMutationService`, `ForgeNpcMutationFlow`, `ForgeActorMaterializer`;
- bed provider: `forge/rest/VanillaBedRestProvider`;
- durable rest attachment: `core/rest/NpcRestAttachment`, `persistence/schema/RestSaveRecord`;
- forms catalog: `NpcDefinitionCatalog.forms`;
- active form state: `ActorDesiredState.activeFormId`, `activeFormRevision`, `activeFormFingerprint`;
- Forge form ops: `forge/operation/form/ForgeNpcFormOperations`;
- Forge placement ops: `forge/operation/place/ForgeNpcPlaceOperations`;
- Story execution adapter: `D:/JDHSeason/mods/echo-story-npc-adapter`.

Команды Forge: `EchoNpcCommands`, `EchoNpcSceneCommandActions` и
`EchoNpcFormCommandActions`.
