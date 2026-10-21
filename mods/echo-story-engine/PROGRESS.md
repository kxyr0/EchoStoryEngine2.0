# Progress

## 1.11.0 EA1 Sound authoring

- Общий Sound binder создаёт external `echo_audio:cue` из линейного OnStart.
- Gameplay, декодирование, сеть и playback не добавлялись в Story core.
- Проверки EA1 фиксируются в canonical `D:/JDHSeason/docs/runtime/AUDIO_DSL.md`.
- Root check и production jarJar прошли; 202 теста, failures/errors/skipped = 0.

## 1.8.0 ES2 high-level StoryResource

- New stories use file-inferred identity plus one `OnStart()` intent section.
- Optional `Id("namespace:path")` preserves an existing stable Story ID.
- The compiler infers the strict start/action/finish chain and supports high-level `Story.Set`, `Story.Require`,
  `Dialogue.Load`, domain sugar, and generic `Action` calls without authored node IDs or `next` links.
- Legacy `Build()` with `Nodes()` or method `OnStart()` remains readable; mixed profiles are rejected.
- Version advanced to `1.8.0`.
- `:story-echoscript-adapter:check`, `sourceShapeCheck`, `checkWhitespace`, root `check`, and
  `:story-forge-1.20.1:jarJar` passed without launching Forge or Minecraft.

## 1.7.0 implicit dialogue audience and catalog command

- `Dialogue.Load("namespace:id")` no longer requires `audienceId`; Story emits only the authored optional override.
- `/echo_story list` is an in-game alias for the authored Story ID catalog.
- Current-player resolution remains owned by the independent Story-Dialogue adapter.
- Version advanced to `1.7.0`.
- Verification passed: `:story-echoscript-adapter:test`, `:story-forge-1.20.1:test`, `sourceShapeCheck`, and
  `:story-forge-1.20.1:jarJar`.

## 1.5.1 authoritative time integration

- StoryResource authoring binds the Echo Time set/add/wait/pause/resume actions without adding clock behavior to
  Story core.
- Terminal lifecycle publication lets the independent time adapter release execution-owned pause tokens after normal
  completion and recovered terminal-state audit.
- Intrinsic lowering is split by real declared packages; shared linear binding entries remove synthetic package
  splitting while preserving the existing authoring behavior.
- Verification passed: `sourceShapeCheck`, `:story-echoscript-adapter:test`,
  `:story-echoscript-adapter:checkstyleMain`, and
  `:story-forge-1.20.1:forgeJarContainsRuntimeClasses`.

## 1.3.0 backpack action authoring

- `StoryResource.OnStart` and `Nodes` accept `Npc.EquipBackpack` and `Npc.UnequipBackpack`.
- `Position(x, y, z)` lowers to scalar external-action payload coordinates.
- Runtime ownership remains in the independent backpack Story extension.

## Current Milestone

Post-M5 external JDH content pack bootstrap is manual-reload only after removing automatic external hot reload.

## Completed

- Execution plan created: `docs/plans/0001-initial-architecture-foundation.md`.
- Gradle module split created: `story-contracts`, `story-core`, `story-forge-1.20.1`.
- Minimal story-only vertical slice implemented:
  neutral input -> state transition -> result + diagnostic -> Forge logging adapter.
- Existing MDK example gameplay/config code removed from active sources.
- Forge MDK example identity replaced with canonical Echo Story Engine identity.
- Documentation and ADR set created.
- Quality gates created: JUnit 5, ArchUnit, Checkstyle, PMD, source-shape check, whitespace check.
- Generated Forge runtime logs from verification were removed and future `logs` artifacts are ignored.
- Architecture acceptance audit completed:
  canonical identity applied, dependency graph verified, production type audit completed.
- M1 deterministic hello story implemented:
  start input -> variable write -> condition -> external action request -> Waiting -> completion -> Finished.
- External action completion categories, duplicate completion protection, missing action diagnostics,
  and transition budget failure are covered by story-core tests.
- M2 execution plan created: `docs/plans/0004-m2-story-source-pipeline.md`.
- M2 source pipeline ADR accepted: `docs/decisions/0011-story-source-pipeline-boundary.md`.
- `story-definition` module added for strict JSON source parsing, structural validation, semantic validation,
  source diagnostics, and compilation into core `StoryDefinition`.
- Runtime external action execution ids are now generated per execution instead of being authored in source or
  compiled nodes.
- M2 verification completed through root Gradle commands, explicit repository scans, and diff review.
- M3 execution plan created: `docs/plans/0005-m3-durable-persistence-recovery.md`.
- M3 durable persistence ADR accepted: `docs/decisions/0012-durable-persistence-recovery.md`.
- `story-persistence` module added for save schema V1, strict JSON checkpoint codec, durable file repository,
  persistence diagnostics, and recovery planning.
- `ExecutionStateRepository` now uses explicit checkpoint load/save result values and optimistic revision checks.
- Runtime save-before-dispatch ordering implemented: commands are exposed only after checkpoint save success.
- Durable recovery returns a WAITING recovery command with the same `ActionExecutionId`.
- Bounded duplicate-completion data now stores the last completed action id instead of an unbounded set.
- `CompiledStoryFingerprint` added as a neutral contracts value; core computes deterministic SHA-256 fingerprints
  from compiled story definitions.
- M3 tests cover file persistence success/failure paths, crash windows, recovery invariants, fingerprint stability,
  command dispatch suppression, and architecture boundaries.
- Forge jar packaging now embeds `story-contracts` and `story-core` classes so the distributable mod jar contains
  `StoryEngineApi`, `DiagnosticSink`, and other Java-only runtime classes needed at Forge load time.
- ForgeGradle userdev runs now register `story-contracts` and `story-core` source sets as part of the mod source so
  `runClient` does not load only `story-forge-1.20.1/build/resources/main`.
- M3.6 execution plan created: `docs/plans/0006-m3-6-story-echoscript-integration.md`.
- `story-echoscript-adapter` module added as the EchoScript `.es` StoryResource source frontend.
- EchoScript is consumed as the published Maven artifact
  `io.github.kxyr0.echoscript:echo-script-language:0.1.0-SNAPSHOT`; no EchoScript source or jars are copied into
  this repository.
- `story-definition` now owns neutral source DTOs, validators, diagnostics, compiler boundary, and
  `SourceStoryCompiler`; the old strict JSON production frontend and Gson dependency were removed from this module.
- `StorySourceCompiler` is now the frontend interface implemented by `EchoScriptStorySourceCompiler`.
- The supported StoryResource profile binds `Story`, `Start`, `Set`, `Require`, `Action`, and `Finish` intrinsics into
  the existing `StoryDefinition` model.
- Adapter diagnostics map EchoScript syntax/language failures and StoryResource binding failures into `ESE-SOURCE-*`
  diagnostics without logging or source-specific files.
- M3.6 tests cover `.es` compilation, runtime execution, duplicate/unknown completion safety, fingerprint invariants,
  real file-repository recovery, incompatible fingerprint rejection, EchoScript containment, JSON source removal, and
  DialogueResource boundary rejection.
- M4 execution plan created: `docs/plans/0007-m4-forge-host-resource-loading-packaging.md`.
- M4 durability/platform ADR accepted: `docs/decisions/0013-forge-host-resource-persistence-packaging.md`.
- Forge `@Mod` entrypoint is now thin lifecycle wiring; hardcoded programmatic hello-story startup was removed.
- Forge server host added for server lifecycle, immutable catalog state, active-definition pinning, bounded persistence
  queue scheduling, and save-before-dispatch preservation.
- Forge resource reload now discovers UTF-8 `.es` StoryResource files from
  `data/<namespace>/echo_story/stories/**/*.es`, compiles them through the EchoScript adapter, and atomically installs
  accepted catalog snapshots.
- Forge durable execution checkpoints are bound to the active world under
  `<world>/data/echo_story_engine/executions` through `FileExecutionStateRepository`.
- Read-only `/echo_story health|stories|executions|execution|diagnostics` command tree added with Russian UTF-8
  operator output.
- Production external action handling remains intentionally absent in M4 and is diagnosed with
  `ESE-FORGE-ACTION-001`.
- Forge GameTest source set added with required M4 server-side host execution, recovery, invalid-reload, and
  live command-dispatcher tests.
- Forge distributable jar packaging now embeds Java-only Story Engine modules and EchoScript runtime, relies on
  Forge/Minecraft-provided Gson at server runtime, and checks that Forge/Minecraft, Gson, and GameTest-only classes are
  absent from the jar.
- Isolated Forge 1.20.1 production-JAR smoke passed with exactly one mod jar, `.es` resource loading, diagnostic
  commands, reload, and clean stop.
- Focused Forge docs added under `docs/forge`.
- Source-shape hardening now enforces a maximum of seven Java/`.es` source scripts per folder.
- Forge `.es` resource discovery now rejects resource folders with more than seven scripts using
  `ESE-FORGE-RESOURCE-005`; folders are counted by namespace plus exact parent path.
- Overloaded source folders were split by responsibility: core persistence ports, EchoScript binding/diagnostic/
  intrinsic code, Forge host catalog/readmodel/execution code, Forge resource discovery, and Forge diagnostic
  catalogs.
- M5 execution plan created: `docs/plans/0008-m5-external-extension-contract-adapter-mod.md`.
- M5 extension contract ADR accepted: `docs/decisions/0014-external-extension-contract-forge-bridge.md`.
- `story-extension-api` module added as loader-neutral public API version `1.0` for descriptors, capabilities,
  start/recovery/cancel requests, invocation results, immutable completions, and thread-safe completion sink.
- `story-extension-forge-1.20.1` module added as the public Forge registration event bridge for external adapter mods.
- Forge host startup now posts one server-scoped extension registration event, closes registration, validates API
  compatibility and duplicate owners, creates server-scoped sessions, and installs an immutable action catalog.
- External commands are dispatched to extension handlers only after durable save; completions return through a bounded
  server-thread queue and are applied on server tick.
- Durable server-start recovery now lists saved execution ids from the file repository and sends WAITING recovery
  requests with the same `ActionExecutionId` after extension sessions are ready.
- Missing providers during recovery preserve WAITING and emit blocked recovery diagnostics; duplicate extension ids and
  duplicate action owners are diagnosed before handler activation.
- Read-only `/echo_story extensions` and `/echo_story extension <extensionId>` commands added.
- Public extension artifacts publish to `build/integration-maven` through `publishExtensionIntegrationArtifacts`.
- Separate external adapter build added under `integration-tests/echo-story-extension-test-mod`; it consumes published
  artifacts instead of root project dependencies and declares a mandatory runtime dependency on `echo_story_engine`.
- Monolithic post-M5 classes were split by responsibility: host runtime flow, host recovery dispatch, persistence file
  layout/index, extension catalog read models, and extension diagnostic catalogs.
- Generic external action output binding added: `Action(..., output: "...")` stores the matching
  `ActionCompletion.outputs()` value as a Story variable before normal `Require` branching.
- Forge host completion handling now preserves extension completion outputs when converting to neutral runtime input.
- Missing declared output keys now fail the execution with `ESE-ACTION-006`; actions without `output` keep the old
  completion behavior.
- StoryResource `Action(...)` now preserves additional named literal arguments as flat
  `Map<String, String>` action payload for external adapters; `type`, `output`, and `next` remain reserved Story
  fields.
- Forge runtime now initializes `<gameDir>/story/content-pack/jdh` from the installed
  `echo_story_content_pack` seed and registers that external folder as a required server data pack.
- Automatic external content hot reload was removed. Changes under `story/content-pack/jdh` are applied by manual
  `/reload`, and the external folder is not registered as `CLIENT_RESOURCES`.
- Story/Dialogue/NPC/Animation candidate compilers now keep persistent `sourceId` + source text caches, so unchanged
  `.es` files reuse their last compile result while changed/new files are compiled separately and deleted source ids are
  evicted before the next candidate catalog is built.
- The embedded content pack seed is not used as a runtime fallback after external initialization, so deleting an
  external `.es` removes it from the next runtime reload instead of resurrecting the JAR copy.
- StoryResource `Nodes()` now accepts `Npc.Move(...)` and `Npc.Stop(...)` sugar that lowers to neutral external action
  nodes. Story core still knows only external action IDs, not NPC or Navigation implementation classes.
- StoryResource supports linear `View.*` sugar for visibility arm,
  disarm, waits, predicates, and blind-window assertion. The frontend emits
  neutral external action IDs only; visibility behavior remains outside Story
  Engine.

## In Progress

- None.

## Next

- Decide how CI/release builds will resolve the EchoScript snapshot or its promoted release coordinates from a
  reproducible Maven repository.
- Decide whether to replace or remove inherited Forge `LICENSE.txt` and `CREDITS.txt` with project-specific
  legal/credit files. This task did not change licensing.
- Add automated dedicated server smoke only when the local/generated run directory can satisfy EULA handling without
  automation accepting it.

## Blockers

- None known.

## Accepted Decisions

- Canonical identity: `echo_story_engine`, `Echo Story Engine`, `io.github.kxyr0.echostoryengine`,
  `echo-story-engine`.
- Keep ForgeGradle only in `story-forge-1.20.1`.
- Use Java-only contracts/core for the initial scaffold.
- Use JUnit 5, ArchUnit, Checkstyle, PMD, and custom source-shape checks.

## Known Risks

- ForgeGradle may create local build/debug artifacts during verification; they must not be committed.
- The M1 external action support list is explicit wiring, not a production adapter registry.
- EchoScript currently resolves from Maven local for development; CI and release builds need the same coordinates in a
  reproducible repository.
- External adapter GameTest currently verifies the existing host path and command/recovery behavior; a broader
  two-mod GameTest scenario with the separate adapter jar remains a future hardening target.
- Windows console/log output can display Russian diagnostics as mojibake even when Java source/tests are UTF-8.

## Intentional Technical Debt

- Durable SavedData/NBT persistence is not implemented in Forge; M4 uses the portable file repository under the world
  data directory.
- No GUI, networking, or production action extension registry exists yet.
- No timeout scheduler or durable completion inbox exists yet.

## Verification Status

- `.\gradlew.bat projects`: passed; root is `echo-story-engine` with three modules.
- `.\gradlew.bat :story-contracts:dependencies --configuration compileClasspath`: passed; no dependencies.
- `.\gradlew.bat :story-core:dependencies --configuration compileClasspath`: passed; depends on `story-contracts`.
- `.\gradlew.bat :story-forge-1.20.1:dependencies --configuration compileClasspath`: passed; depends on
  `story-core`, `story-contracts`, and Forge/Minecraft.
- `.\gradlew.bat test`: passed after canonical package rename.
- `.\gradlew.bat architectureTest`: passed.
- `.\gradlew.bat check`: passed.
- `.\gradlew.bat build`: passed; Forge jar artifact is `echo-story-engine-0.1.0.jar`.
- `.\gradlew.bat checkWhitespace`: passed.
- `git diff --check`: passed.
- Stale identity scan for old MDK identifiers: passed.
- contracts/core forbidden import scan: passed.
- non-build log artifact scan: passed.
- Dedicated server/GameTest: not run as a separate smoke test because no GameTest or server smoke scenario exists
  in the initial scaffold.
- M1 `.\gradlew.bat test`: passed.
- M1 `.\gradlew.bat architectureTest`: passed.
- M1 `.\gradlew.bat check`: passed.
- M1 `.\gradlew.bat build`: passed.
- M1 `.\gradlew.bat checkWhitespace`: passed.
- M1 `git diff --check`: passed.
- M1 contracts/core forbidden import scan: passed.
- M1 production type count after implementation: `story-contracts` 15, `story-core` 10,
  `story-forge-1.20.1` 5.
- M2 `.\gradlew.bat :story-definition:compileJava`: passed.
- M2 `.\gradlew.bat :story-definition:checkstyleMain`: passed.
- M2 `.\gradlew.bat :story-definition:checkstyleTest`: passed.
- M2 `.\gradlew.bat :story-definition:test`: passed.
- M2 `.\gradlew.bat :story-core:test`: passed.
- M2 `.\gradlew.bat sourceShapeCheck`: passed.
- M2 `.\gradlew.bat architectureTest`: passed.
- M2 `.\gradlew.bat projects`: passed; root now contains `story-contracts`, `story-core`, `story-definition`,
  and `story-forge-1.20.1`.
- M2 `.\gradlew.bat test`: passed.
- M2 `.\gradlew.bat check`: passed.
- M2 `.\gradlew.bat build`: passed; Forge jar build still succeeds.
- M2 `.\gradlew.bat checkWhitespace`: passed.
- M2 `git diff --check`: passed with line-ending warnings only.
- M2 forbidden Minecraft import scan outside Forge: passed.
- M2 forbidden Forge import scan outside Forge: passed.
- M2 JSON parser type scan in contracts/core: passed.
- M2 source DTO reference scan in contracts/core runtime code: passed.
- M2 log-file creation scan in production code: passed.
- M2 forbidden package-name scan: passed.
- M2 `examplemod` / `com.example` scan: passed.
- M2 TODO/FIXME scan: passed.
- M2 mutable static field rule: passed through ArchUnit; explicit static scan returned only static methods/factories.
- M2 production size scan: passed; no production type exceeds the 120 logical-line review threshold.
- M2 production type count: `story-contracts` 15, `story-core` 10, `story-definition` 16,
  `story-forge-1.20.1` 5.
- M3 `.\gradlew.bat projects`: passed; root now contains `story-contracts`, `story-core`, `story-definition`,
  `story-forge-1.20.1`, and `story-persistence`.
- M3 focused `.\gradlew.bat :story-core:compileJava :story-persistence:compileJava`: passed.
- M3 focused `.\gradlew.bat :story-persistence:test`: passed.
- M3 focused `.\gradlew.bat :story-core:test :story-persistence:test :story-definition:test
  :story-forge-1.20.1:test`: passed.
- M3 `.\gradlew.bat test`: passed.
- M3 `.\gradlew.bat architectureTest`: passed.
- M3 `.\gradlew.bat check`: passed.
- M3 `.\gradlew.bat build`: passed.
- M3 `.\gradlew.bat checkWhitespace`: passed.
- M3 `git diff --check`: passed with CRLF conversion warnings only.
- M3 `.\gradlew.bat sourceShapeCheck`: passed.
- M3 forbidden platform import scan outside Forge production code: passed.
- M3 JSON/storage API leakage scan in contracts/core: passed.
- M3 Java object serialization API scan: passed.
- M3 Path/File/JSON/NBT token scan in core port APIs: passed.
- M3 persistence DTO scan in contracts/core runtime APIs: passed.
- M3 source DTO scan in persistence module: passed.
- M3 mutable static field scan: passed.
- M3 log-file/raw absolute path production scan: passed.
- M3 TODO/FIXME scan: passed.
- M3 stale MDK identity production/docs scan: passed.
- M3 dependency reports passed: `story-definition` and `story-persistence` depend on `story-core`,
  `story-contracts`, and Gson; Forge depends on `story-core`, `story-contracts`, and Forge/Minecraft.
- M3 production type count: `story-contracts` 16, `story-core` 19, `story-definition` 16,
  `story-forge-1.20.1` 5, `story-persistence` 19.
- Forge jar runtime class packaging check: passed; `echo-story-engine-0.1.0.jar` contains `StoryEngineApi.class`
  and `DiagnosticSink.class`.
- ForgeGradle userdev source-set configuration check: passed through `:story-forge-1.20.1:compileJava`,
  `:story-forge-1.20.1:processResources`, `:story-forge-1.20.1:jar`, and `forgeJarContainsRuntimeClasses`.
- M3.6 focused `.\gradlew.bat :story-echoscript-adapter:compileJava :story-definition:compileJava`: passed.
- M3.6 focused `.\gradlew.bat :story-echoscript-adapter:checkstyleMain
  :story-echoscript-adapter:checkstyleTest :story-definition:checkstyleMain :story-definition:checkstyleTest`:
  passed.
- M3.6 focused `.\gradlew.bat :story-definition:test :story-echoscript-adapter:test`: passed.
- M3.6 production type count: `story-contracts` 16, `story-core` 19, `story-definition` 14,
  `story-echoscript-adapter` 12, `story-forge-1.20.1` 5, `story-persistence` 19.
- M3.6 `.\gradlew.bat projects`: passed; root now contains `story-contracts`, `story-core`, `story-definition`,
  `story-echoscript-adapter`, `story-forge-1.20.1`, and `story-persistence`.
- M3.6 dependency reports passed: `story-echoscript-adapter` depends on `story-definition`, `story-core`,
  `story-contracts`, and EchoScript `echo-script-language`/`echo-script-api`; `story-definition` depends only on
  `story-core` and `story-contracts`; `story-persistence` depends on `story-core`, `story-contracts`, and Gson.
- M3.6 `.\gradlew.bat test`: passed.
- M3.6 `.\gradlew.bat architectureTest`: passed.
- M3.6 `.\gradlew.bat check`: passed.
- M3.6 `.\gradlew.bat build`: passed.
- M3.6 `.\gradlew.bat checkWhitespace`: passed.
- M3.6 `git diff --check`: passed with LF/CRLF conversion warnings only.
- M3.6 `.\gradlew.bat sourceShapeCheck`: passed.
- M3.6 forbidden platform import scan outside Forge production code: passed.
- M3.6 EchoScript production leakage scan outside `story-echoscript-adapter`: passed.
- M3.6 copied EchoScript package-source scan: passed.
- M3.6 committed jar scan excluding build outputs and Gradle wrapper: passed.
- M3.6 absolute Gradle path scan: passed.
- M3.6 JSON source frontend production-leftover scan in `story-definition`: passed.
- M3.6 Gson isolation scan outside persistence schema/module usage: passed.
- M3.6 mutable static production field scan: passed.
- M3.6 direct logging API scan outside Forge production code: passed.
- M3.6 DialogueResource/Line/Alias production semantics scan: passed.
- M3.6 TODO/FIXME scan excluding build/run artifacts: passed.
- M4 focused `.\gradlew.bat :story-forge-1.20.1:compileJava`: passed.
- M4 focused `.\gradlew.bat :story-forge-1.20.1:test`: passed.
- M4 focused `.\gradlew.bat :story-forge-1.20.1:check`: passed.
- M4 focused `.\gradlew.bat :story-forge-1.20.1:gameTestClasses`: passed.
- M4 acceptance repair focused `.\gradlew.bat :story-forge-1.20.1:test :story-forge-1.20.1:checkstyleMain
  :story-forge-1.20.1:checkstyleTest :story-forge-1.20.1:checkstyleGameTest :story-forge-1.20.1:pmdMain
  :story-forge-1.20.1:pmdTest :story-forge-1.20.1:pmdGameTest`: passed.
- M4 acceptance repair command output polish `.\gradlew.bat :story-forge-1.20.1:test
  :story-forge-1.20.1:checkstyleMain :story-forge-1.20.1:checkstyleTest`: passed after translating
  operator-facing command labels to Russian.
- M4 acceptance repair rerun after Russian docs/output updates `.\gradlew.bat test architectureTest check build
  sourceShapeCheck checkWhitespace`: passed.
- M4 acceptance repair earlier host-only rerun after Russian docs/output updates
  `.\gradlew.bat :story-forge-1.20.1:runGameTestServer`: passed before the live command-dispatcher GameTest was
  added; guard verified `discovered=3, executed=3, passed=3`.
- M4 acceptance repair `.\gradlew.bat :story-forge-1.20.1:compileGameTestJava
  :story-forge-1.20.1:checkstyleGameTest`: passed after adding the live command-dispatcher GameTest.
- M4 acceptance repair `.\gradlew.bat :story-forge-1.20.1:runGameTestServer`: passed; guard verified
  `discovered=4, executed=4, passed=4` and required markers for
  `ForgeHostGameTests.hostExecutesStoryThroughDurableSave`,
  `ForgeHostGameTests.hostRecreationRecoversWaitingAction`,
  `ForgeHostGameTests.invalidCandidateRetainsCatalog`, and
  `ForgeHostGameTests.echoStoryCommandsExecuteOnServerDispatcher`.
- M4 acceptance repair `.\gradlew.bat :story-forge-1.20.1:forgeJarContainsRuntimeClasses`: passed.
- M4 acceptance repair `.\gradlew.bat :story-forge-1.20.1:runServer --console=plain`: passed normal dedicated
  server smoke after human EULA acceptance in the ForgeGradle `run` directory. RCON verified `echo_story health`,
  `echo_story stories`, `echo_story diagnostics 20`, `reload`, post-reload command output, and clean `stop`; logs
  showed host start/catalog install/reload/host stop and no `NoClassDefFoundError`, client-only load marker,
  `ERROR`, `FATAL`, or shutdown hang.
- M4 acceptance repair `.\gradlew.bat test architectureTest check build`: passed.
- M4 `.\gradlew.bat projects`: passed; root contains `story-contracts`, `story-core`, `story-definition`,
  `story-echoscript-adapter`, `story-forge-1.20.1`, and `story-persistence`.
- M4 `.\gradlew.bat :story-forge-1.20.1:dependencies --configuration compileClasspath`: passed; Forge depends on
  `story-definition`, `story-echoscript-adapter`, `story-persistence`, EchoScript, Gson, and Forge/Minecraft.
- M4 `.\gradlew.bat :story-forge-1.20.1:dependencies --configuration embeddedExternalRuntime`: passed; embedded
  external runtime contains EchoScript language/API. Gson remains a normal compile/runtime dependency supplied by the
  Forge/Minecraft server runtime, not an embedded jar entry.
- M4 `.\gradlew.bat test`: passed.
- M4 `.\gradlew.bat architectureTest`: passed.
- M4 `.\gradlew.bat check`: passed.
- M4 `.\gradlew.bat build`: passed; Forge jar artifact is
  `story-forge-1.20.1/build/libs/echo-story-engine-0.1.0.jar`.
- M4 `.\gradlew.bat checkWhitespace`: passed.
- M4 `.\gradlew.bat sourceShapeCheck`: passed.
- M4 `git diff --check`: passed with LF/CRLF conversion warnings only.
- M4 dedicated server smoke: passed after `story-forge-1.20.1/run/eula.txt` was manually accepted by a human.
- M4 isolated production-JAR smoke: installed a standalone Forge 1.20.1 server and verified `mods` contains exactly
  `echo-story-engine-0.1.0.jar`. Initial isolated launch exposed a Java module split-package failure from embedded
  Gson; packaging was fixed, `forgeJarContainsRuntimeClasses` now forbids `com/google/gson/`, and the isolated launch
  now stops only at its own generated EULA gate.
- M4 acceptance repair final packaging rerun `.\gradlew.bat test architectureTest check build sourceShapeCheck
  checkWhitespace`: passed after removing embedded Gson from the Forge jar.
- M4 acceptance repair final `.\gradlew.bat :story-forge-1.20.1:runGameTestServer`: passed; guard verified
  `discovered=4, executed=4, passed=4`.
- M4 final jar inspection: `jar tf story-forge-1.20.1/build/libs/echo-story-engine-0.1.0.jar` contains Story Engine,
  EchoScript, Forge host, and command classes; `com/google/gson/` is absent.
- M4 isolated production-JAR classload rerun with the fresh final jar: passed packaging/classload phase and stopped at
  `D:\JDHSeason\mods\echostoryengine\story-forge-1.20.1\build\isolated-production-smoke\eula.txt` containing
  `eula=false`.
- M4 isolated production-JAR smoke after manual EULA acceptance:
  `D:\JDHSeason\mods\echostoryengine\story-forge-1.20.1\build\isolated-production-smoke\run.bat --nogui` passed with
  process exit code `0`; `mods` contained exactly `echo-story-engine-0.1.0.jar`; RCON on port `25576` verified
  `echo_story health`, `echo_story stories`, `echo_story diagnostics 20`, `reload`, post-reload command output, and
  `stop`.
- M4 isolated manual command-contract smoke: passed; RCON verified `echo_story health`, `echo_story stories`,
  `echo_story executions`, `echo_story execution sample:missing`, `echo_story diagnostics`,
  `echo_story diagnostics 20`, invalid `diagnostics` limits `0`, `-1`, `abc`, `51`, `reload`, post-reload read
  commands, and clean `stop`.
- M4 isolated production-JAR smoke log scan: passed; `latest.log` showed datapack discovery, catalog install, server
  `Done`, RCON running, host start, reload catalog install, host stop, and RCON listener stop; no
  `NoClassDefFoundError`, `ClassNotFoundException`, `ERROR`, `FATAL`, client-only marker, or shutdown hang marker was
  found.
- M4 final jar negative inspection: passed; no `com/google/gson/`, Minecraft, Forge, GameTest implementation, JUnit,
  generated run/report/docs, eula/log, or Gradle artifact prefixes were found in
  `story-forge-1.20.1/build/libs/echo-story-engine-0.1.0.jar`.
- M4 lower-module platform import scan outside Forge production code: passed.
- M4 EchoScript leakage scan outside `story-echoscript-adapter` and Forge production code: passed.
- M4 Forge production client-only import scan: passed.
- M4 Fabric/NeoForge/Architectury production scan: passed.
- M4 committed jar scan excluding Gradle wrapper: passed.
- M4 stale MDK identity scan excluding build/run artifacts: passed.
- M4 subsystem log filename scan excluding build/run artifacts: passed.
- M4 absolute path scan excluding build/run artifacts found only an intentional test assertion guarding against
  `D:\` paths in Gradle files.
- M4 broad suppression/disabled check scan found only the project PMD ruleset file.
- M4 mutable static server/host scan found only static methods, not mutable static fields.
- M4 forbidden gameplay-domain production scan in lower modules: passed.
- M4 acceptance repair production type count: `story-contracts` 16, `story-core` 19, `story-definition` 14,
  `story-echoscript-adapter` 12, `story-forge-1.20.1` 36, `story-persistence` 19.
- Post-M4 source-shape hardening production type count: `story-contracts` 16, `story-core` 19,
  `story-definition` 14, `story-echoscript-adapter` 12, `story-forge-1.20.1` 44, `story-persistence` 19.
- Post-M4 source-shape hardening `.\gradlew.bat compileJava sourceShapeCheck`: passed.
- Post-M4 source-shape hardening `.\gradlew.bat test`: passed.
- Post-M4 source-shape hardening `.\gradlew.bat architectureTest check build`: passed.
- Post-M4 source-shape hardening `.\gradlew.bat checkWhitespace sourceShapeCheck`: passed after docs updates.
- Post-M4 source-shape hardening `git diff --check`: passed with a CRLF/LF conversion warning for
  `StoryResourceCandidateCompilerTest.java`.
- M5 focused `.\gradlew.bat :story-extension-api:test :story-forge-1.20.1:test buildExtensionTestAdapter`: passed.
- M5 `.\gradlew.bat sourceShapeCheck`: passed after splitting post-M5 monolithic classes.
- M5 `.\gradlew.bat test`: passed.
- M5 `.\gradlew.bat architectureTest`: passed.
- M5 `.\gradlew.bat check`: passed.
- M5 `.\gradlew.bat projects`: passed; root now contains `story-extension-api` and
  `story-extension-forge-1.20.1`.
- M5 `.\gradlew.bat build`: passed.
- M5 `.\gradlew.bat dependencies`: passed; root has no configurations.
- M5 `.\gradlew.bat buildExtensionTestAdapter`: passed; public artifacts were published to
  `build/integration-maven` and the independent Forge adapter project built successfully.
- M5 `.\gradlew.bat :story-forge-1.20.1:runGameTestServer`: passed; guard verified
  `discovered=4, executed=4, passed=4`.
- M5 final `.\gradlew.bat check build`: passed.
- M5 final `git diff --check`: passed with CRLF/LF conversion warnings for `settings.gradle` and
  `StoryResourceCandidateCompilerTest.java`.
- M5 production type count: `story-contracts` 16, `story-core` 19, `story-definition` 14,
  `story-echoscript-adapter` 12, `story-extension-api` 14, `story-extension-forge-1.20.1` 4,
  `story-forge-1.20.1` 61, `story-persistence` 20.
- Post-M5 generic output binding focused `.\gradlew.bat :story-definition:checkstyleTest
  :story-definition:test --console=plain`: passed.
- Post-M5 generic output binding `.\gradlew.bat check sourceShapeCheck checkWhitespace --console=plain`: passed.
- Post-M5 generic output binding `.\gradlew.bat build --console=plain`: passed.
- Post-M5 generic output binding `git diff --check`: passed with existing CRLF/LF warnings for `settings.gradle`
  and `StoryResourceCandidateCompilerTest.java`.
- Post-M5 generic output binding production type count: `story-contracts` 16, `story-core` 20,
  `story-definition` 14, `story-echoscript-adapter` 12, `story-extension-api` 14,
  `story-extension-forge-1.20.1` 4, `story-forge-1.20.1` 62, `story-persistence` 20.
- External content pack/hot reload focused `.\gradlew.bat :story-forge-1.20.1:test --tests
  "io.github.kxyr0.echostoryengine.story.forge.externalcontent.*" --console=plain`: passed.
- External content pack/hot reload incremental compiler cache `.\gradlew.bat :story-forge-1.20.1:test --tests
  "io.github.kxyr0.echostoryengine.story.forge.resource.StoryResourceCandidateCompilerTest" --console=plain`:
  passed.
- External content pack/hot reload sibling compiler cache checks:
  `.\gradlew.bat :dialogue-forge-1.20.1:test --console=plain`,
  `.\gradlew.bat :npc-forge-1.20.1:test --console=plain`, and
  `.\gradlew.bat :animation-forge-1.20.1:test --console=plain`: passed.
- External content pack/hot reload `.\gradlew.bat :story-forge-1.20.1:check --console=plain`: passed.
- External content pack/hot reload production smoke through sibling content pack
  `.\gradlew.bat runProductionStoryPackSmokeServer --console=plain`: passed; log showed first extraction to
  `build/production-runtime-server/story/content-pack/jdh`, automatic data pack selection, loaded
  Story/Dialogue/NPC/Animation catalogs, and clean stop.
- External content manual-reload removal focused `.\gradlew.bat :story-forge-1.20.1:test --tests
  "io.github.kxyr0.echostoryengine.story.forge.externalcontent.*" --console=plain`: passed.
- External content manual-reload removal `.\gradlew.bat sourceShapeCheck checkWhitespace --console=plain`: passed.
- External content manual-reload removal `.\gradlew.bat :story-forge-1.20.1:check --console=plain`: passed.
- External content manual-reload removal `git diff --check`: passed.
- External content pack manual-reload production type count: `story-forge-1.20.1` 66.
- VIS1 Story authoring `:story-echoscript-adapter:test
  :story-echoscript-adapter:assemble checkWhitespace --console=plain`: passed.
- VIS1 root `check build` remains blocked by pre-existing source-shape and
  Checkstyle debt in oversized Story files; the focused compiler tests pass.
- EO1 Observation integration: added parser-valid `Observation.Activate`, `Deactivate`, `Enable`, `Disable`,
  `IsActive`, and `IsCompleted` intrinsics backed by durable external action nodes. Engine version advanced to
  `1.6.0`; focused compiler test passed against EchoScript `0.3.0-SNAPSHOT`.

- ES2 Feature.Unlock/Lock/Set shorthand added in the frontend only; aliases canvas/magic lower to echo_core:set_flag with implicit @a and automatic ordered transitions. Version 1.9.0. Full offline check and Forge jarJar passed, including compiler rejection/graph tests and architecture checks. No in-game smoke test was run for this syntax change.
- ES2 feature shorthand: root offline build (including check) passed with mod_version and engine_version both 1.9.0. Production JAR contains FeatureActionIntrinsics and metadata 1.9.0; installed Prism JAR checksum verified. Existing dependent-mod version ranges accept 1.9.0.

2026-10-11: 1.10.0 — добавлен маркер .archived для внешнего JDH content-pack.
SERVER_DATA/CLIENT_RESOURCES не регистрируют архивный pack; seed extractor
не дополняет архивный каталог. Проверены отключение, восстановление и защита
от seed extraction. `check` и `:story-forge-1.20.1:jarJar` прошли.
Minecraft для ручной проверки не запускался.

2026-10-21: 1.13.0 — Story frontend признаёт внешние class-level When-блоки.
Их действия не включаются в граф OnStart. Сборка Story/Dialogue/Trigger/Doctor/
Events Integration прошла (333 tasks); native frontend, inline binder, datapack
ResourceManager и LKG reload тесты успешны. Проверки архитектуры, Checkstyle,
PMD и source shape пройдены. Полный мир/GameTest не запускался.
