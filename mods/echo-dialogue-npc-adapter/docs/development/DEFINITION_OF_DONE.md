# Definition of Done

N4 is done when:

- adapter compiles on Java 17 and Forge 1.20.1 / 47.4.10;
- `test`, `architectureTest`, `sourceShapeCheck`, `checkWhitespace`, `check` pass;
- `runGameTestServer` passes and reports 9 discovered / 9 executed / 9 passed
  N4 GameTests;
- `verifyProductionJarContents` confirms no copied public API or forbidden systems;
- `verifySiblingPublicArtifacts` confirms Dialogue/NPC generated Maven artifacts;
- `runIsolatedProductionSmokeServer` passes with exactly three production JARs;
- `runN4AcceptanceStoryPackServer` passes and reports
  `EDNA-ACCEPTANCE PASS dynamic-line-choice` with materialized
  `jdh:content_pack_father`;
- `runN4AcceptanceRestartChoiceServer` passes and proves the same
  `story-action/jdh:n4_restart_choice/play_greeting` session pauses at
  `revision=2`, resumes after restart, advances to `revision=3` and completes
  `outcome=jdh:content_pack_accepted`;
- `runN4ProductionStoryPackSmokeServer` passes with C1 production content and
  active `echo_dialogue_npc_adapter:npc_presentation`;
- `runClient -Pn4ClientVisualAcceptance=true -PskipSiblingBuild` passes in
  ForgeGradle userdev and saves LINE/CHOICE screenshots with `display="Отец"`,
  `visual=jdh:john_father` and the Echo NPC skin resource marker;
- `stageN4ProductionStoryPack` prepares the same eight C1 production + N4 final
  JARs for a real Forge launcher/client profile;
- `stageN4ProductionClientVisualAcceptance` prepares the eight C1 production
  final JARs plus one test-only production client harness JAR;
- `verifyN4ProductionClientVisualAcceptanceOutputs` passes against an isolated
  production Forge client run and reports `EDNA-PROD-CLIENT-ACCEPTANCE PASS`
  with `display="Отец"`, `visual=jdh:john_father` and
  `assets/jdh/textures/echo_npc/skins/john_father.png`;
- production JAR has mandatory deps `echo_dialogue`, `echo_npc`, `forge`, `minecraft`;
- runtime acceptance proves LINE and CHOICE dynamic speaker presentation;
- no-skin, missing actor, service unavailable, absent actor, stopped service,
  query failure and blocked/corrupt fallbacks are observed;
- content pack docs explain the `SpeakerId == ActorId` contract and skin ownership.
