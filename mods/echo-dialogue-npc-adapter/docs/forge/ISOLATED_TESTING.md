# Isolated testing

Build with sibling public artifacts:

```powershell
.\gradlew.bat n4IntegrationBuild --console=plain
```

Forge GameTest server:

```powershell
$env:GRADLE_OPTS='-Dnet.minecraftforge.gradle.check.certs=false'
.\gradlew.bat runGameTestServer --console=plain
```

This is not the production packaging proof. It uses Forge userdev and therefore
loads Echo Dialogue, Echo NPC and the adapter from exploded class/resource
directories through `MOD_CLASSES`. The expected task summary is
`N4 GameTests verified: discovered=9, executed=9, passed=9`.

Runtime smoke mod set:

- Echo Dialogue final Forge JAR;
- Echo NPC final Forge JAR;
- Echo Dialogue NPC Adapter JAR;

```powershell
$env:GRADLE_OPTS='-Dnet.minecraftforge.gradle.check.certs=false'
.\gradlew.bat runIsolatedProductionSmokeServer -PskipSiblingBuild --console=plain
```

The pure packaging proof uses exactly three production JARs and no content pack:

- `echo-dialogue-0.1.0-SNAPSHOT-all.jar`;
- `echo-npc-0.1.0-SNAPSHOT-all.jar`;
- `echo-dialogue-npc-adapter-0.1.0-SNAPSHOT.jar`.

Expected runtime:

- `/echo_dialogue providers` includes `echo_dialogue_npc_adapter:npc_presentation`;
- `/echo_npc health` reports active service;
- no `ClassNotFoundException` or `NoClassDefFoundError`;
- clean `stop`, exit code 0.

Dynamic LINE/CHOICE behavior still needs an acceptance server/client with content
that has matching `SpeakerId` and `ActorId`.

Headless N4 acceptance runtime:

```powershell
.\gradlew.bat runN4AcceptanceStoryPackServer -PskipSiblingBuild --console=plain
.\gradlew.bat runN4AcceptanceRestartChoiceServer -PskipSiblingBuild --console=plain
```

This task stages the Echo Story Content Pack acceptance runtime, then adds:

- `echo-dialogue-npc-adapter-0.1.0-SNAPSHOT.jar`;
- `echo-dialogue-npc-adapter-acceptance-harness-0.1.0-SNAPSHOT.jar`.

Expected markers:

- `EDNA-ACCEPTANCE PASS dynamic-line-choice actorId=jdh:content_pack_father`;
- `ESCPC-ACCEPTANCE AUTO_PASS scenarios=accepted,refused`.

It proves real server-side `NpcService` lookup and provider resolution against a
materialized actor. It does not prove the client overlay or actual rendered
pixels.

Dev-client visual framebuffer proof:

```powershell
.\gradlew.bat runClient -Pn4ClientVisualAcceptance=true -PskipSiblingBuild --console=plain
```

This uses userdev/exploded classpaths and a test-only client mod
`echo_dialogue_npc_adapter_client_acceptance`. It renders LINE and CHOICE via
the actual Echo Dialogue client renderer and writes PNG files under
`run-client-visual/n4-client-acceptance/`. Expected evidence:

- `n4-client-line.png` and `n4-client-choice.png`;
- `build/reports/runtime/n4-client-visual-acceptance-latest.log`;
- `EDNA-CLIENT-ACCEPTANCE PASS ... display="Отец" visual=jdh:john_father`;
- resource marker
  `assets/jdh/textures/echo_npc/skins/john_father.png`.

This is real client-pixel evidence, but it is not the production final-JAR
isolated client proof.

Production final-JAR client visual proof:

```powershell
.\gradlew.bat stageN4ProductionClientVisualAcceptance -PskipSiblingBuild --console=plain
.\gradlew.bat verifyN4ProductionClientVisualAcceptanceOutputs --console=plain
```

The staged set under `build/n4-production-client-visual/mods` contains the same
eight C1 production final JARs plus one test-only production client harness JAR.
The accepted run used an isolated Forge 1.20.1 / 47.4.10 client and wrote:

- `build/n4-production-client-run/n4-production-client-acceptance/n4-production-client-line.png`;
- `build/n4-production-client-run/n4-production-client-acceptance/n4-production-client-choice.png`;
- `build/reports/runtime/n4-production-client-visual-acceptance-latest.log`.

Expected markers:

- `EDNA-PROD-CLIENT-ACCEPTANCE START visual=jdh:john_father display="Отец"`;
- `EDNA-PROD-CLIENT-ACCEPTANCE SCREENSHOT phase=LINE`;
- `EDNA-PROD-CLIENT-ACCEPTANCE SCREENSHOT phase=CHOICE`;
- `EDNA-PROD-CLIENT-ACCEPTANCE PASS ... display="Отец" visual=jdh:john_father`;
- resource marker `assets/jdh/textures/echo_npc/skins/john_father.png`.

This proves final-JAR client loading and public Echo Dialogue skin-head
resolution/rendering from the Echo NPC-owned skin path. The actual
`ClientDialogueRenderer` pixel proof remains the dev-client visual test above.

`runN4AcceptanceRestartChoiceServer` additionally preserves the same server
world across two Forge server processes. Expected markers:

- first run: `ESCPC-ACCEPTANCE PAUSE_CHOICE scenario=accepted` with
  `revision=2`;
- second run: `ESCPC-ACCEPTANCE RESUME_CHOICE scenario=accepted`;
- second run: `ESCPC-ACCEPTANCE DIALOGUE_ACTIVE ... revision=2`;
- second run: `ESCPC-ACCEPTANCE DIALOGUE_CHOICE ... choiceId=accept`;
- second run: `ESCPC-ACCEPTANCE PASS scenario=accepted`;
- both runs: `EDNA-ACCEPTANCE PASS dynamic-line-choice actorId=jdh:content_pack_father`.

C1 production Story Pack compatibility smoke:

```powershell
.\gradlew.bat runN4ProductionStoryPackSmokeServer -PskipSiblingBuild --console=plain
```

This task stages 8 production JARs under `build/n4-production-story-pack/mods`:
the C1 production story pack set plus `echo-dialogue-npc-adapter`. Expected
markers include:

- `Provider: echo_dialogue_npc_adapter:npc_presentation`;
- `jdh:chapter_1_home_arrival`;
- `jdh:john_father display="Отец" skin=true`;
- `Echo Animation diagnostics: none`.

This is the headless compatibility proof for C1 content. Final-JAR skin-head
pixels are covered by the production final-JAR client visual proof above.

Manual operator client staging for the same final JAR set:

```powershell
.\gradlew.bat stageN4ProductionStoryPack -PskipSiblingBuild --console=plain
```

Copy the eight JARs from `build/n4-production-story-pack/mods` into a real Forge
1.20.1 / 47.4.10 launcher instance or another isolated production client
profile. ForgeGradle `runClient` is userdev-mapped and is not a valid final-JAR
client proof for this pack.
