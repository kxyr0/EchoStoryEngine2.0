# Dependency Rules D6

## Разрешённые зависимости

- `dialogue-contracts`: без project dependencies.
- `dialogue-presentation-api`: зависит только от `dialogue-contracts`; содержит provider API version, descriptor, request, result и session contract.
- `dialogue-core`: зависит только от `dialogue-contracts`; содержит authored compiled/session model без provider lifecycle.
- `dialogue-echoscript-adapter`: зависит от `dialogue-core`, `dialogue-contracts` и EchoScript syntax artifacts.
- `dialogue-persistence`: зависит от `dialogue-core`, `dialogue-contracts` и Gson как codec dependency.
- `dialogue-forge-api-1.20.1`: зависит от `dialogue-contracts`, `dialogue-presentation-api` и Forge/Minecraft API только для typed public events/context.
- `dialogue-forge-1.20.1`: зависит от всех production modules, public Forge API bridge и Forge/Minecraft API.

## Запреты

- `dialogue-core` не зависит от `dialogue-persistence`, Forge, Minecraft, EchoScript, Gson, `Path`, `File`, NBT или JSON DTO.
- `dialogue-contracts/.../api/visual` и `dialogue-presentation-api/.../presentation/api` не содержат `ResourceLocation`, texture objects, `Path`, `File`, `NativeImage`, `GameProfile`, URL или byte arrays.
- `dialogue-persistence` не зависит от Forge, Minecraft, EchoScript AST/runtime, client UI, network packets или Story Engine.
- `dialogue-echoscript-adapter` не зависит от `dialogue-persistence`.
- `dialogue-forge-api-1.20.1` не содержит host implementation, persistence, catalog mutation, networking, client overlay,
  commands, source compilation или service registry.
- External adapters must use only `dialogue-contracts`, `dialogue-presentation-api` and `dialogue-forge-api-1.20.1`. They must not import
  `dialogue-forge-1.20.1` implementation packages such as `forge.host`, `forge.service`, `forge.network` or
  `forge.client`.
- Provider registration must use `EchoDialogueSpeakerPresentationRegistrationEvent`; ServiceLoader, reflection, classpath scanning, static mutable provider registries, IMC, priorities and registration-order winners are forbidden.
- Pure modules не логируют напрямую и не создают отдельные log files.
- Нельзя использовать `Serializable`, `ObjectOutputStream`, Java class names в persisted data, callbacks или object graph serialization.
- Story Engine imports запрещены во всех D2 production modules.
- Client visual resolver/renderers живут только в `dialogue-forge-1.20.1/.../forge/client/visual` и не импортируют persistence, Story, HTTP clients или `GameProfile`.

## Проверки

`architectureTest` и `sourceShapeCheck` должны подтверждать:

- отсутствие module cycles;
- отсутствие Forge/Minecraft imports вне Forge module;
- отсутствие persistence implementation dependency в core;
- отсутствие EchoScript/source/client DTO в persistence schema;
- отсутствие mutable static runtime state;
- leaf package limit: максимум семь production `.java` files;
- production class limit: максимум 180 logical LOC.
- D5 visual descriptor не открывает raw path/URL и не добавляет serverbound visual packets.
- D6 provider output не входит в persistence DTO, compiled fingerprint или network packet schema.

`publishDialogueIntegrationArtifacts` публикует только public API artifacts в `build/integration-maven` для внешних adapter builds:

- `io.github.kxyr0.echodialogue:dialogue-contracts`;
- `io.github.kxyr0.echodialogue:dialogue-presentation-api`;
- `io.github.kxyr0.echodialogue:dialogue-forge-api-1.20.1`.

Forge runtime provides `DialogueService` through a typed synchronous lookup event for one `MinecraftServer`; the public
API module stays bridge-only and does not hold a global mutable registry.

`verifyProductionJarContents` подтверждает, что final JAR содержит Echo Dialogue runtime modules, включая
`dialogue-persistence`, `dialogue-presentation-api` и public Forge API bridge, но не содержит Forge/Minecraft/JUnit/GameTest/Story Engine/Echo NPC/test-provider classes и не встраивает duplicate Gson classes.
