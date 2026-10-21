# PROGRESS

## N4 progress

Статус: N4 complete. Реализован production adapter, unit/architecture/build
gates, Forge GameTest, build-local public artifact orchestration, dev-client
visual acceptance screenshots и isolated production final-JAR client visual
acceptance.

Принятые prerequisites:

- D6 complete в Echo Dialogue;
- N1 complete в Echo NPC;
- N2 complete в Echo NPC.

Сделано:

- создан независимый root `echo-dialogue-npc-adapter`;
- добавлен Forge mod `echo_dialogue_npc_adapter`;
- зарегистрирован provider `echo_dialogue_npc_adapter:npc_presentation`;
- provider compile-only зависит от public Dialogue/NPC artifacts;
- `SpeakerId` преобразуется в `ActorId` только exact canonical string;
- `NpcService` используется только через public `EchoNpcServiceBridge.lookup(server)`;
- runtime вызывает только `NpcService.query(actorId)`;
- no-skin actor сохраняет authored fallback visual из request;
- service unavailable и missing actor возвращают `NOT_HANDLED`;
- blocked/corrupt actor возвращает fallback resolution с diagnostic;
- добавлены unit tests и architecture tests;
- добавлены Forge GameTests для LINE, CHOICE, no-skin fallback, missing actor,
  unavailable service, absent actor, stopped service, query failure и
  blocked/corrupt actor;
- добавлен JAR verification gate.
- добавлены Russian docs, `PROJECT_MAP.md`, `RESTART.md` и ADRs.
- добавлен isolated production smoke для ровно трех final JARs:
  Echo Dialogue, Echo NPC, Echo Dialogue NPC Adapter.
- добавлен test-only N4 acceptance harness и runtime с Echo Story Content Pack
  acceptance resources; harness проверяет real `NpcService`, materialized actor,
  LINE/CHOICE `RESOLVED` и `SKIN_HEAD` через production adapter session.
- добавлен headless restart-at-CHOICE acceptance с тем же persisted server world:
  первый запуск останавливается на `revision=2`, второй запуск выбирает choice и
  завершает тот же `DialogueSessionId`.
- добавлен N4 production Story Pack smoke: C1 production content + production
  N4 adapter без test harness.
- `stageN4ProductionStoryPack` готовит C1 production + N4 final JAR set для
  real Forge launcher/client profile.
- добавлен test-only dev-client visual harness:
  `echo_dialogue_npc_adapter_client_acceptance` запускает настоящий Forge
  client в userdev, добавляет bridge class в dev module `echo_dialogue`, вызывает
  фактический `ClientDialogueRenderer`, сохраняет LINE/CHOICE screenshots и
  проверяет `SKIN_HEAD` resource из
  `assets/jdh/textures/echo_npc/skins/john_father.png`.
- добавлен test-only production final-JAR client visual harness:
  `echo_dialogue_npc_adapter_production_client_acceptance` запускается в
  isolated Forge 1.20.1 / 47.4.10 client с final production JARs, проверяет
  public Echo Dialogue `ClientSpeakerVisualResolver` /
  `ClientSpeakerVisualRenderer`, сохраняет LINE/CHOICE screenshots и проверяет
  тот же Echo NPC skin resource.
- добавлен verifier `verifyN4ProductionClientVisualAcceptanceOutputs` для
  production-client PNG/log evidence.

Выполненные проверки:

- `.\gradlew.bat test --console=plain`: OK.
- `.\gradlew.bat check --console=plain`: OK.
- `$env:GRADLE_OPTS='-Dnet.minecraftforge.gradle.check.certs=false'; .\gradlew.bat n4IntegrationBuild --console=plain`: OK.
- `$env:GRADLE_OPTS='-Dnet.minecraftforge.gradle.check.certs=false'; .\gradlew.bat stageIsolatedProductionMods --console=plain`: OK, rebuilt sibling final Dialogue/NPC JARs and staged 3 JARs.
- `$env:GRADLE_OPTS='-Dnet.minecraftforge.gradle.check.certs=false'; .\gradlew.bat runIsolatedProductionSmokeServer -PskipSiblingBuild --console=plain`: OK, UTF-8 log, clean stop.
- `$env:GRADLE_OPTS='-Dnet.minecraftforge.gradle.check.certs=false'; .\gradlew.bat runGameTestServer --console=plain`: OK, discovered=9, executed=9, passed=9.
- `.\gradlew.bat acceptanceHarnessJar --console=plain`: OK.
- `.\gradlew.bat runN4AcceptanceStoryPackServer -PskipSiblingBuild --console=plain`: OK, N4 acceptance harness PASS and Story acceptance accepted/refused PASS.
- `.\gradlew.bat runN4AcceptanceRestartChoiceServer -PskipSiblingBuild --console=plain`: OK, restart at CHOICE resumes the same session and finishes accepted outcome.
- `.\gradlew.bat runN4ProductionStoryPackSmokeServer -PskipSiblingBuild --console=plain`: OK, C1 production content loads with N4 provider active.
- `.\gradlew.bat productionClientHarnessJar stageN4ProductionClientVisualAcceptance -PskipSiblingBuild --console=plain`: OK, staged 8 C1 production final JARs plus test-only production client harness.
- isolated Forge production client launch with
  `-DechoDialogueNpcAdapterProductionClientAcceptance.auto=true @build/n4-production-client-install/n4-production-client.args`: OK, saved LINE/CHOICE screenshots and stopped cleanly.
- `.\gradlew.bat verifyN4ProductionClientVisualAcceptanceOutputs --console=plain`: OK.
- `.\gradlew.bat n4IntegrationBuild --console=plain`: OK after production-client verifier/docs changes.
- `.\gradlew.bat projects dependencies --console=plain`: OK, root has no subprojects; dependency graph uses public Dialogue/NPC artifacts.
- `$env:GRADLE_OPTS='-Dnet.minecraftforge.gradle.check.certs=false'; .\gradlew.bat projects dependencies test architectureTest check build sourceShapeCheck checkWhitespace verifyProductionJarContents --console=plain`: OK.
- `.\gradlew.bat compileClientDialogueAccessJava compileClientAcceptanceHarnessJava processClientAcceptanceHarnessResources --console=plain`: OK.
- `.\gradlew.bat prepareRunClient -Pn4ClientVisualAcceptance=true -PskipSiblingBuild --console=plain`: OK.
- `.\gradlew.bat runClient -Pn4ClientVisualAcceptance=true -PskipSiblingBuild --console=plain`: OK, Forge GUI client started, saved LINE/CHOICE screenshots and stopped cleanly.
- Historical failed attempt, not a supported command:
  `.\gradlew.bat runClient -Pn4ProductionClientAcceptance=true --console=plain`
  launched ForgeGradle userdev client and crashed on reobfuscated final Echo NPC
  JAR with `NoSuchMethodError EntityType$Builder.m_20704_`; this is not
  accepted as N4 evidence.
- `.\gradlew.bat :dialogue-forge-1.20.1:compileJava --console=plain` в `../echo-dialogue`: OK для client resolver change.
- `git diff --check` в adapter root: OK.
- `git diff --check` по затронутым Echo Dialogue files: OK.
- `jar tf build/libs/echo-dialogue-npc-adapter-0.1.0-SNAPSHOT.jar`: содержит только adapter classes, `META-INF/mods.toml` и `pack.mcmeta`.

Runtime evidence:

- staged mods: `build/isolated-production-smoke/mods`;
- staged set: `echo-dialogue-0.1.0-SNAPSHOT-all.jar`, `echo-npc-0.1.0-SNAPSHOT-all.jar`, `echo-dialogue-npc-adapter-0.1.0-SNAPSHOT.jar`;
- runtime log: `build/reports/runtime/isolated-production-smoke-latest.log`;
- log markers: `Done`, `EDLG-PRESENT-003`, `echo_dialogue_npc_adapter:npc_presentation`, `Echo NPC: active=true`, `display="Отец"`, `Stopping server`.

Forge GameTest evidence:

- task: `runGameTestServer`;
- runtime: exploded userdev `echo_dialogue`, `echo_npc`, `echo_dialogue_npc_adapter`;
- external runtime classpath: EchoScript and Echo UI Animations only for GameTest server;
- discovered/executed/passed: 9/9/9;
- executed tests:
  `dynamicLineUsesNpcPresentation`, `dynamicChoiceUsesNpcPresentation`,
  `actorWithoutSkinKeepsAuthoredVisual`, `missingActorDoesNotClaimSpeaker`,
  `unavailableNpcServiceDoesNotClaimSpeaker`, `absentActorDoesNotClaimSpeaker`,
  `stoppedNpcServiceUsesTemporaryFallback`, `failedNpcQueryUsesFailureFallback`,
  `blockedActorUsesFailureFallback`;
- registration marker during same server run:
  `Provider принят: echo_dialogue_npc_adapter:npc_presentation`.

N4 acceptance runtime evidence:

- task: `runN4AcceptanceStoryPackServer -PskipSiblingBuild`;
- staged mods: `build/n4-acceptance-story-pack/mods`;
- staged set: 10 JARs including Echo Dialogue, Echo NPC, N4 adapter,
  N4 acceptance harness, Echo Story Content Pack acceptance JAR, Story
  acceptance harness, Echo Story Engine, Story-Dialogue Adapter,
  Story-NPC Adapter and Echo Animation;
- runtime log: `build/reports/runtime/n4-acceptance-story-pack-server-latest.log`;
- N4 marker:
  `EDNA-ACCEPTANCE PASS dynamic-line-choice actorId=jdh:content_pack_father status=MATERIALIZED display="Отец" visual=jdh:content_pack_father`;
- Story markers:
  `ESCPC-ACCEPTANCE PASS scenario=accepted`,
  `ESCPC-ACCEPTANCE PASS scenario=refused`,
  `ESCPC-ACCEPTANCE AUTO_PASS scenarios=accepted,refused`;
- this is headless server evidence only; it does not prove client pixels.

N4 restart-at-CHOICE evidence:

- task: `runN4AcceptanceRestartChoiceServer -PskipSiblingBuild`;
- staged mods: `build/n4-acceptance-story-pack/mods`;
- first runtime log:
  `build/reports/runtime/n4-acceptance-restart-choice-first.log`;
- second runtime log:
  `build/reports/runtime/n4-acceptance-restart-choice-second.log`;
- first-run markers:
  `story-action/jdh:n4_restart_choice/play_greeting`,
  `revision=2`,
  `ESCPC-ACCEPTANCE PAUSE_CHOICE scenario=accepted`;
- second-run markers:
  `ESCPC-ACCEPTANCE RESUME_CHOICE scenario=accepted`,
  `ESCPC-ACCEPTANCE DIALOGUE_ACTIVE ... revision=2 expectedRevision=2`,
  `ESCPC-ACCEPTANCE DIALOGUE_CHOICE ... expectedRevision=2 choiceId=accept`,
  `ESCPC-ACCEPTANCE DIALOGUE_ACTIVE ... revision=3 expectedRevision=3`,
  `ESCPC-ACCEPTANCE PASS scenario=accepted`,
  `outcome=jdh:content_pack_accepted`;
- N4 marker appears in both runs:
  `EDNA-ACCEPTANCE PASS dynamic-line-choice actorId=jdh:content_pack_father status=MATERIALIZED display="Отец" visual=jdh:content_pack_father`;
- this proves headless restart recovery at CHOICE only; it does not prove
  client pixels.

C1 production Story Pack smoke evidence:

- task: `runN4ProductionStoryPackSmokeServer -PskipSiblingBuild`;
- staged mods: `build/n4-production-story-pack/mods`;
- staged set: 8 production JARs: Echo Story Engine, Echo Dialogue,
  Story-Dialogue Adapter, Echo NPC, Story-NPC Adapter, Echo Animation,
  Echo Story Content Pack and Echo Dialogue NPC Adapter;
- runtime log: `build/reports/runtime/n4-production-story-pack-smoke-latest.log`;
- markers:
  `Provider: echo_dialogue_npc_adapter:npc_presentation`,
  `jdh:chapter_1_home_arrival`,
  `jdh:john_father display="Отец" skin=true`,
  `Echo Animation diagnostics: none`,
  `Stopping server`;
- this proves headless C1 compatibility only; it does not prove client pixels.

N4 dev-client visual evidence:

- task: `runClient -Pn4ClientVisualAcceptance=true -PskipSiblingBuild`;
- runtime: ForgeGradle `forgeclientuserdev` with exploded dev classes for
  Echo Dialogue, Echo NPC, Echo Dialogue NPC Adapter and test-only client
  harness;
- bridge: `build/classes/java/clientDialogueAccess` is attached to the
  `echo_dialogue` dev module only for this run, avoiding a production package
  change and avoiding Java module split-package errors;
- runtime log: `build/reports/runtime/n4-client-visual-acceptance-latest.log`;
- screenshots:
  `run-client-visual/n4-client-acceptance/n4-client-line.png` (854x480,
  28283 bytes),
  `run-client-visual/n4-client-acceptance/n4-client-choice.png` (854x480,
  31248 bytes);
- markers:
  `EDNA-CLIENT-ACCEPTANCE START visual=jdh:john_father display="Отец"`,
  `EDNA-CLIENT-ACCEPTANCE SCREENSHOT phase=LINE`,
  `EDNA-CLIENT-ACCEPTANCE SCREENSHOT phase=CHOICE`,
  `EDNA-CLIENT-ACCEPTANCE PASS ... display="Отец" visual=jdh:john_father resource=assets/jdh/textures/echo_npc/skins/john_father.png`;
- visual observation: screenshots show Dialogue overlay LINE and CHOICE with
  display `Отец`, Russian text/choices and rendered skin head from the Echo NPC
  skin path;
- this is real client framebuffer evidence, but it is userdev/exploded runtime
  evidence, not an isolated final-JAR launcher proof.

N4 production final-JAR client visual evidence:

- tasks:
  `productionClientHarnessJar stageN4ProductionClientVisualAcceptance -PskipSiblingBuild`,
  isolated Java/Forge client launch, then
  `verifyN4ProductionClientVisualAcceptanceOutputs`;
- runtime: isolated Forge 1.20.1 / 47.4.10 production client outside
  ForgeGradle userdev;
- staged mods:
  `build/n4-production-client-visual/mods`;
- staged set: the 8 C1 production final JARs plus
  `echo-dialogue-npc-adapter-production-client-harness-0.1.0-SNAPSHOT.jar`;
- runtime log:
  `build/reports/runtime/n4-production-client-visual-acceptance-latest.log`;
- screenshots:
  `build/n4-production-client-run/n4-production-client-acceptance/n4-production-client-line.png`
  (854x480, 30150 bytes),
  `build/n4-production-client-run/n4-production-client-acceptance/n4-production-client-choice.png`
  (854x480, 30991 bytes);
- markers:
  `EDNA-PROD-CLIENT-ACCEPTANCE START visual=jdh:john_father display="Отец"`,
  `EDNA-PROD-CLIENT-ACCEPTANCE SCREENSHOT phase=LINE`,
  `EDNA-PROD-CLIENT-ACCEPTANCE SCREENSHOT phase=CHOICE`,
  `EDNA-PROD-CLIENT-ACCEPTANCE PASS ... display="Отец" visual=jdh:john_father resource=assets/jdh/textures/echo_npc/skins/john_father.png`;
- forbidden markers absent in `latest.log`: `NoSuchMethodError`,
  `ResolutionException`, `ClassNotFoundException`, `NoClassDefFoundError`,
  `EDNA-PROD-CLIENT-ACCEPTANCE FAIL`;
- visual observation: screenshots show Russian `Отец`, LINE/CHOICE text,
  Russian choices and a full skin head without clipping;
- this proves final-JAR client loading and public Echo Dialogue skin-head
  resolver/renderer behavior against the C1 Echo NPC skin path. The actual
  package-private `ClientDialogueRenderer` is covered by the dev-client proof
  above.

Оставшиеся operator-only проверки, принятые отдельно: full external-launcher
Dialogue UI walkthrough, adapter-removed visual fallback and world-switch visual
observation.

Следующее milestone после N4: N5 - Dialogue-NPC Interaction Lifecycle.
Animation остается deferred и не входит в N4.
