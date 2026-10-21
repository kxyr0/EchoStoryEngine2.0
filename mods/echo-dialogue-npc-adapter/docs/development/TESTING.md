# Testing

Fast checks:

```powershell
.\gradlew.bat test --console=plain
.\gradlew.bat architectureTest --console=plain
.\gradlew.bat sourceShapeCheck checkWhitespace --console=plain
```

Full local build:

```powershell
.\gradlew.bat check --console=plain
```

Forge GameTest:

```powershell
$env:GRADLE_OPTS='-Dnet.minecraftforge.gradle.check.certs=false'
.\gradlew.bat runGameTestServer --console=plain
```

`runGameTestServer` uses exploded userdev outputs for Echo Dialogue, Echo NPC
and the adapter. `run/mods` stays empty for this task because reobfuscated
production JARs are not valid in Forge userdev. The task verifies nine required
tests: dynamic LINE, dynamic CHOICE, no-skin authored visual, missing actor,
unavailable service, absent actor, stopped service, query failure and
blocked/corrupt actor.

Sibling artifact build:

```powershell
.\gradlew.bat n4IntegrationBuild --console=plain
```

Isolated three-mod production server:

```powershell
$env:GRADLE_OPTS='-Dnet.minecraftforge.gradle.check.certs=false'
.\gradlew.bat runIsolatedProductionSmokeServer -PskipSiblingBuild --console=plain
```

N4 headless acceptance runtime with content:

```powershell
.\gradlew.bat acceptanceHarnessJar --console=plain
.\gradlew.bat runN4AcceptanceStoryPackServer -PskipSiblingBuild --console=plain
.\gradlew.bat runN4AcceptanceRestartChoiceServer -PskipSiblingBuild --console=plain
```

`runN4AcceptanceStoryPackServer` stages Echo Story Content Pack acceptance
resources plus the production N4 adapter and a test-only N4 harness. The harness
waits for real `jdh:content_pack_father` materialization, creates the production
provider session through the public D6 creation context, and verifies LINE and
CHOICE resolve to display `Отец` with `SKIN_HEAD jdh:content_pack_father`.
The same server also runs the Story accepted/refused auto scenarios.

This is still headless server evidence. It does not prove client rendering,
texture pixels or the visual overlay.

N4 dev-client visual screenshots:

```powershell
.\gradlew.bat runClient -Pn4ClientVisualAcceptance=true -PskipSiblingBuild --console=plain
```

This command uses ForgeGradle `forgeclientuserdev` with exploded dev outputs,
not production final JARs. The test-only client harness starts a real Forge
client, attaches a small bridge class to the dev `echo_dialogue` module, calls
the actual `ClientDialogueRenderer`, saves:

- `run-client-visual/n4-client-acceptance/n4-client-line.png`;
- `run-client-visual/n4-client-acceptance/n4-client-choice.png`;
- `build/reports/runtime/n4-client-visual-acceptance-latest.log`.

Expected markers are `EDNA-CLIENT-ACCEPTANCE SCREENSHOT phase=LINE`,
`EDNA-CLIENT-ACCEPTANCE SCREENSHOT phase=CHOICE` and
`EDNA-CLIENT-ACCEPTANCE PASS ... display="Отец" visual=jdh:john_father`.
The generated test resources copy the C1 Echo NPC skin PNG from
`assets/jdh/textures/echo_npc/skins/john_father.png`; no production Dialogue
skin duplicate is introduced.

This proves client framebuffer pixels for LINE and CHOICE in userdev. It is not
an isolated final-JAR launcher proof.

N4 production final-JAR client visual screenshots:

```powershell
.\gradlew.bat stageN4ProductionClientVisualAcceptance -PskipSiblingBuild --console=plain
.\gradlew.bat verifyN4ProductionClientVisualAcceptanceOutputs --console=plain
```

`stageN4ProductionClientVisualAcceptance` prepares the eight C1 production final
JARs plus the test-only
`echo-dialogue-npc-adapter-production-client-harness-0.1.0-SNAPSHOT.jar`.
The accepted production launcher run used a real Forge 1.20.1 / 47.4.10 client
outside ForgeGradle userdev with:

```text
-DechoDialogueNpcAdapterProductionClientAcceptance.auto=true
@build/n4-production-client-install/n4-production-client.args
```

The harness verifies final-JAR client loading and Echo Dialogue public
`ClientSpeakerVisualResolver` / `ClientSpeakerVisualRenderer` resolution for
`SKIN_HEAD jdh:john_father`. It writes:

- `build/n4-production-client-run/n4-production-client-acceptance/n4-production-client-line.png`;
- `build/n4-production-client-run/n4-production-client-acceptance/n4-production-client-choice.png`;
- `build/reports/runtime/n4-production-client-visual-acceptance-latest.log`.

Expected markers are `EDNA-PROD-CLIENT-ACCEPTANCE SCREENSHOT phase=LINE`,
`EDNA-PROD-CLIENT-ACCEPTANCE SCREENSHOT phase=CHOICE` and
`EDNA-PROD-CLIENT-ACCEPTANCE PASS ... display="Отец" visual=jdh:john_father`.
Forbidden markers include `NoSuchMethodError`, `ResolutionException`,
`ClassNotFoundException`, `NoClassDefFoundError` and
`EDNA-PROD-CLIENT-ACCEPTANCE FAIL`.

This is final-JAR client visual-resource evidence. The actual Dialogue overlay
renderer is covered by the dev-client visual proof above because it is
package-private in the production Echo Dialogue mod.

`runN4AcceptanceRestartChoiceServer` uses the same staged acceptance runtime and
one persisted server world. The first server run executes
`echo_story_content_pack_acceptance run_until_choice accepted jdh:n4_restart_choice`
and stops after `ESCPC-ACCEPTANCE PAUSE_CHOICE ... revision=2`. The second run
executes `echo_story_content_pack_acceptance resume_choice accepted
jdh:n4_restart_choice`, waits until the recovered Dialogue session is ACTIVE at
`revision=2`, posts `choiceId=accept`, verifies `revision=3` and terminal
`outcome=jdh:content_pack_accepted`.

C1 production content compatibility smoke:

```powershell
.\gradlew.bat runN4ProductionStoryPackSmokeServer -PskipSiblingBuild --console=plain
```

This stages production C1 content plus the production N4 adapter under
`build/n4-production-story-pack/mods`. The server verifies provider registration,
`jdh:chapter_1_home_arrival`, `jdh:john_father`, NPC definitions and clean Echo
Animation diagnostics.

Manual acceptance must cover:

- LINE dynamic name and skin;
- CHOICE prompt dynamic name and skin;
- no-skin actor keeps authored portrait;
- missing actor uses authored fallback;
- unavailable Echo NPC service uses authored fallback;
- blocked/corrupt actor uses authored fallback with diagnostic;
- adapter removed uses authored fallback.

For an operator-driven C1 production pack check, use
`build/n4-production-story-pack/mods` in both server and client. Use a real Forge
1.20.1 / 47.4.10 launcher profile or equivalent isolated client with those final
JARs:

```powershell
.\gradlew.bat stageN4ProductionStoryPack -PskipSiblingBuild --console=plain
```

Do not use ForgeGradle `runClient` with these final JARs as a production client
proof. `runClient` runs in userdev mappings; reobfuscated final JARs can fail
there even when they are valid for a real Forge launcher.

In an operator client, a minimal dynamic visual check is:

```text
/echo_dialogue providers
/echo_npc definitions
/echo_npc spawn jdh:john_father jdh:john_father
/echo_npc actor jdh:john_father
/echo_dialogue play jdh:chapter_1_home_arrival
```

Expected client observation: the first two father LINEs and the CHOICE prompt
use the current NPC display name and the skin head from
`assets/jdh/textures/echo_npc/skins/john_father.png`. Without the N4 adapter JAR,
the same dialogue must fall back to the authored `Portrait("jdh:john_father")`.
