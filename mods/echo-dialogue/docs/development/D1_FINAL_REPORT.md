# D1 final report

## 1. Краткий итог D1

Milestone D1 `Standalone Linear Dialogue Foundation` реализован как отдельный Forge 1.20.1 мод `Echo Dialogue`.

Проверенный поток:

`.dialogue.es` -> EchoScript parser -> DialogueResource semantic binding -> immutable `CompiledDialogue` -> server-authoritative session -> Forge network sync -> centered client overlay -> Space/ЛКМ advance -> server validation -> explicit `COMPLETED`.

Echo Story Engine не подключался и не импортировался.

## 2. Фактическая структура репозитория

- `dialogue-contracts` - public immutable contracts.
- `dialogue-core` - plain Java compiled model and deterministic session engine.
- `dialogue-echoscript-adapter` - EchoScript DialogueResource semantic adapter.
- `dialogue-forge-1.20.1` - Forge entrypoint, resource reload, host, commands, networking, client overlay, packaging.
- `docs/architecture`, `docs/dialogue`, `docs/forge`, `docs/development`, `docs/decisions`, `docs/plans` - русская документация и ADR.

## 3. Gradle dependency graph

```text
dialogue-contracts
  <- dialogue-core
      <- dialogue-echoscript-adapter
          <- dialogue-forge-1.20.1

dialogue-echoscript-adapter -> io.github.kxyr0.echoscript:echo-script-api:0.1.0-SNAPSHOT
dialogue-echoscript-adapter -> io.github.kxyr0.echoscript:echo-script-language:0.1.0-SNAPSHOT
dialogue-forge-1.20.1 -> net.minecraftforge:forge:1.20.1-47.4.10
```

`.\gradlew.bat :dialogue-forge-1.20.1:dependencies --configuration runtimeClasspath` подтвердил эти связи.

## 4. DialogueResource syntax

Канонический путь ресурса:

```text
data/<namespace>/echo_dialogue/dialogues/**/*.dialogue.es
```

Поддержанный D1-профиль:

```csharp
class SampleGreeting : DialogueResource
{
    public override DialogueResourceDef Build()
    {
        return Dialogue("sample:greeting");
    }

    private void Init()
    {
        Start("greeting");
        Alias("father", "sample:father", display: "Отец");
        Alias("player", "sample:player", display: "Игрок");
    }

    private NodeDef Greeting()
    {
        var node = new Node("greeting");
        Line("father", "Где ты был?");
        Line("player", "Гулял.");
        Line("father", "Понятно.");
        return node;
    }
}
```

## 5. Supported and unsupported features

Поддержано: один `DialogueResource` на source, `Dialogue`, `Start`, `Alias`, `Node`, ordered `Line`, namespaced ids, alias fallback display, immutable compiled output.

Не поддержано: choices, branching, conditions, effects, portraits, `face`, animation, audio, arbitrary script execution, NPC/entity binding, Story integration.

## 6. Source reload pipeline

Forge server reload ищет UTF-8 `.dialogue.es`, компилирует все candidates, собирает immutable candidate catalog, reject при ERROR/duplicate `DialogueId`, atomic apply при успехе, previous known-good catalog сохраняется при ошибке. Active sessions pinned к compiled definition, с которой стартовали.

## 7. Compiled dialogue model

`CompiledDialogue` содержит `DialogueId`, start node id и immutable lines. `CompiledDialogueLine` содержит stable speaker id, display name и text. Core model не содержит Forge/Minecraft/EchoScript syntax/tree/raw parser state.

## 8. Session lifecycle

Phases: `ACTIVE`, `COMPLETED`, `CANCELLED`, `FAILED`.

Start открывает первую line. `ADVANCE` двигает ровно на одну line при совпадении session id, audience и revision. После последней line session переходит в `COMPLETED`. Повторный/stale/wrong advance возвращает explicit status/diagnostic и не двигает состояние.

## 9. Server/client ownership

Server owns catalog, sessions, validation, completion and cancellation. Client owns only read model display and bounded input request. Client never selects next line and never marks completion.

## 10. Networking protocol

Forge channel: `echo_dialogue:main`, protocol version `1`.

Messages:

- `DialogueOpenMessage` - server -> client open/update presentation.
- `DialogueCloseMessage` - server -> client close.
- `DialogueAdvanceMessage` - client -> server advance request with session id and expected revision.

Server validates sender ownership, active session, session id, expected revision and duplicate/stale requests.

## 11. Centered overlay behavior

`ClientDialogueClient` registers a client-only overlay above all GUI. Panel is centered, responsive width, dark readable background, speaker display name, wrapped line text, and hint `ЛКМ / Пробел - продолжить`.

## 12. Input behavior

Space or left mouse while no other screen is open sends one advance request. `ClientDialogueState` locks while waiting for server revision update and unlocks on new open/update packet.

## 13. Commands

```text
/echo_dialogue health
/echo_dialogue dialogues
/echo_dialogue play <dialogueId> [player]
/echo_dialogue active
/echo_dialogue cancel [player]
/echo_dialogue diagnostics [limit]
```

`dialogueId` uses `ResourceLocationArgument`, so `sample:greeting` parses unquoted. Console must pass `[player]`; source player may omit it.

## 14. Diagnostics

Structured diagnostics are returned by contracts/core/adapter and emitted through Forge diagnostic sink plus bounded in-memory buffer. No separate `dialogue.log`, `ui.log`, `network.log`, or `parser.log` exists.

## 15. Every production type and one exact responsibility

- `DiagnosticCategory` - diagnostic category enum.
- `DiagnosticSeverity` - diagnostic severity enum.
- `DiagnosticSubject` - diagnostic source/subject value.
- `DialogueDiagnostic` - immutable diagnostic record.
- `AudienceId` - neutral target audience id.
- `DialogueId` - validated namespaced dialogue id.
- `DialogueNodeId` - validated node id.
- `DialogueSessionId` - validated session id.
- `NamespacedId` - shared namespaced-id parser/invariants.
- `SpeakerId` - validated speaker id.
- `ActiveDialogueSessions` - one-active-session collection and policy.
- `CompiledDialogue` - immutable compiled dialogue aggregate.
- `CompiledDialogueLine` - immutable dialogue line.
- `DialoguePresentation` - core read model for current line.
- `DialogueInput` - immutable session input.
- `DialogueInputKind` - supported input kinds.
- `DialogueSessionEngine` - deterministic transition engine.
- `DialogueSessionPhase` - session phase enum.
- `DialogueSessionState` - immutable session state.
- `DialogueTransition` - transition result.
- `DialogueTransitionStatus` - transition outcome enum.
- `DialogueCompileResult` - adapter compile result.
- `EchoScriptDialogueCompiler` - EchoScript parse and semantic compile facade.
- `AliasTable` - speaker alias resolution table.
- `CallSyntax` - normalized supported invocation view.
- `DialogueResourceBinder` - semantic binder for DialogueResource.
- `InitMethodReader` - reads `Start`/`Alias` declarations.
- `MethodCatalog` - class method lookup for binder.
- `NodeLineReader` - reads ordered `Line` calls from node methods.
- `DialogueSemanticDiagnostics` - semantic diagnostic factory.
- `EchoScriptDiagnosticAdapter` - EchoScript diagnostic mapper.
- `EchoDialogueMod` - Forge mod entrypoint.
- `CatalogInstallResult` - catalog install result.
- `CompiledDialogueCatalogSnapshot` - immutable catalog snapshot.
- `DialogueCatalogCandidate` - candidate catalog before atomic apply.
- `DialogueCatalogState` - current known-good catalog holder.
- `ClientDialogueClient` - client overlay, packet events and input bridge.
- `ClientDialogueState` - current client presentation state and wait lock.
- `EchoDialogueCommands` - `/echo_dialogue` command tree.
- `ForgeDialogueCompositionRoot` - server-scoped composition root.
- `ForgeDiagnosticFormatter` - Russian diagnostic/user command formatting.
- `ForgeDiagnosticSink` - Forge logger plus in-memory diagnostic emission.
- `InMemoryDiagnosticBuffer` - bounded recent diagnostic buffer.
- `ForgeDialogueHost` - server session host and network presentation sender.
- `DialogueNetwork` - Forge packet channel registration/send helpers.
- `ClientboundDialogueCloseEvent` - client close event bridge.
- `ClientboundDialogueOpenEvent` - client open/update event bridge.
- `ServerboundDialogueAdvanceEvent` - server advance event bridge.
- `DialogueAdvanceMessage` - advance packet DTO.
- `DialogueCloseMessage` - close packet DTO.
- `DialogueOpenMessage` - open/update packet DTO.
- `DialogueResourceCandidateCompiler` - compiles discovered resource texts.
- `DialogueResourceDiscovery` - discovers `.dialogue.es` server resources.
- `DialogueResourceReadResult` - resource read result.
- `DialogueResourceText` - resource id plus UTF-8 text.
- `ForgeDialogueResourceReloadListener` - reload listener and atomic catalog apply.

## 16. Production type counts by module/package

```text
dialogue-contracts/api.diagnostic: 4
dialogue-contracts/api.identity: 6
dialogue-core/host: 1
dialogue-core/model: 3
dialogue-core/session: 7
dialogue-echoscript-adapter/echoscript: 2
dialogue-echoscript-adapter/binding: 6
dialogue-echoscript-adapter/diagnostic: 2
dialogue-forge-1.20.1/forge: 1
dialogue-forge-1.20.1/catalog: 4
dialogue-forge-1.20.1/client: 2
dialogue-forge-1.20.1/command: 1
dialogue-forge-1.20.1/composition: 1
dialogue-forge-1.20.1/diagnostic: 3
dialogue-forge-1.20.1/host: 1
dialogue-forge-1.20.1/network: 1
dialogue-forge-1.20.1/network.event: 3
dialogue-forge-1.20.1/network.message: 3
dialogue-forge-1.20.1/resource: 5
```

No production package exceeds 7 Java files.

## 17. Plain-JVM tests

XML result count: 8 files, 31 tests, 0 failures, 0 errors, 0 skipped.

Covered: id invariants, immutable collections/model, session start/current/advance/revision/completion/cancel/wrong/stale/duplicate/second-session policy, Russian UTF-8 preservation, parser diagnostics, semantic binding, missing start node, duplicate node, unresolved alias, empty dialogue, unsupported intrinsic, unsupported `face`, invalid `DialogueId`, deterministic compile, malformed source no output.

## 18. GameTest names and counts

GameTests:

- `ForgeDialogueGameTests.echoDialogueCommandsExist`
- `ForgeDialogueGameTests.invalidCandidateRetainsCatalog`
- `ForgeDialogueGameTests.dialogueResourceCompilesInForgeRuntime`
- `ForgeDialogueGameTests.controlledAudienceSessionLifecycle`

Latest `runGameTestServer`: discovered=4, executed=4, passed=4, failed=0.

## 19. Dedicated-server smoke result

Isolated Forge server with final JAR reached `Done`, loaded one dialogue (`Reload диалогов принят: 1`) and shut down cleanly via `stop`.

## 20. Real client overlay smoke result

Real client smoke completed. Evidence:

- screenshot: `dialogue-forge-1.20.1/run/screenshots/2026-07-12_23.53.43.png`;
- overlay centered;
- speaker `Отец` visible;
- line `Где ты был?` visible;
- user confirmed the dialogue advanced through all lines and closed.

## 21. Final JAR path and packaging

Final JAR:

```text
D:\JDHSeason\mods\echo-dialogue\dialogue-forge-1.20.1\build\libs\echo-dialogue-0.1.0-SNAPSHOT.jar
```

Size: 192252 bytes.

Contains `META-INF/mods.toml`, sample dialogue resource, production classes from contracts/core/adapter/Forge and EchoScript API/language classes. Does not contain test/JUnit/GameTest entries or `net/minecraft*` / `net/minecraftforge*` classes.

Embedded EchoScript `module-info.class` entries are excluded from the merged mod JAR; inspection result: `HasModuleInfo=0`. This avoids Java module-layer package export conflicts when the final JAR is loaded as a Forge mod.

## 22. Isolated server/client results

- Isolated server with only final Echo Dialogue JAR in `mods` passed.
- Isolated production client was installed under `dialogue-forge-1.20.1/build/smoke/client` with the Forge installer. Its `mods` directory contained only `echo-dialogue-0.1.0-SNAPSHOT.jar`.
- Production client logs found the final JAR as a valid `echo_dialogue` mod, loaded the Forge client, accepted `echo_dialogue:main` protocol version `1`, and reached `Connected to a modded server`.
- Isolated server logs accepted `echo_dialogue:main` protocol version `1` from the client and recorded `EchoSmoke joined the game`.
- `/echo_dialogue play sample:greeting EchoSmoke` was not repeated in this production-client run because the server batch session had closed stdin. The actual overlay and line progression are covered by the real client overlay smoke in section 20.

## 23. Every verification command and result

```text
.\gradlew.bat projects - success
.\gradlew.bat test - success
.\gradlew.bat architectureTest - success
.\gradlew.bat check - success
.\gradlew.bat build - success
.\gradlew.bat check build - success
.\gradlew.bat sourceShapeCheck - success
.\gradlew.bat checkWhitespace - success
.\gradlew.bat :dialogue-forge-1.20.1:runGameTestServer - success, 4/4
.\gradlew.bat :dialogue-forge-1.20.1:dependencies --configuration runtimeClasspath - success
git diff --check - success
jar tf dialogue-forge-1.20.1/build/libs/echo-dialogue-0.1.0-SNAPSHOT.jar - inspected
production-JAR dedicated server smoke - success
isolated production-client final-JAR load/connect smoke - success
real client overlay smoke - success
```

## 24. Checks not executed and exact reasons

No source, build, JVM-test, architecture-test, GameTest, packaging-inspection, dedicated-server smoke, final-JAR client load/connect smoke, or real overlay smoke remains intentionally unexecuted.

The machine has no standard `%APPDATA%\.minecraft\versions` launcher installation path, so the final-JAR client smoke used an isolated Forge installer layout in `build/smoke/client` instead of a normal launcher profile. That layout loaded only the final Echo Dialogue JAR and connected to the isolated server.

The production-client run did not repeat `/echo_dialogue play sample:greeting EchoSmoke` because the server batch process was started without an open stdin stream. This does not reduce D1 overlay coverage: the real client overlay smoke in section 20 already verified centered presentation, Russian text, input advancement and close after the final line.

## 25. Remaining architectural risks

- The final-JAR client was verified through an isolated Forge installer layout rather than a normal user launcher profile because no standard launcher installation exists on this machine.
- Overlay is intentionally minimal and has no portrait/reserved art behavior yet.
- EchoScript SNAPSHOT artifacts are embedded by zip merge, not Forge JarJar metadata.

## 26. Deferred features

Choices, branching, NPC lookup, portraits, speaker entity binding, animations, facial states, camera/cinematic mode, movement lock, voice/audio, typewriter, localization framework, history, quests, inventory, wardrobe, editor, generic UI module, persistence, arbitrary scripting.

## 27. Story Engine confirmation

Echo Story Engine integration was not implemented. Production scans found no Story Engine imports. The only `echostoryengine` reference is in an ArchUnit rule that forbids such dependencies.

## 28. Next milestone

D2 - Story-Dialogue Adapter using the Echo Story Engine M5 extension API.
