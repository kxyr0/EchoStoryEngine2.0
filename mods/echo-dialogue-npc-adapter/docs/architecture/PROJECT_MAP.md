# Project map

Root:

- `settings.gradle` - independent Gradle root name `echo-dialogue-npc-adapter`;
- `build.gradle` - Forge build, public artifact orchestration and verification gates;
- `gradle.properties` - project identity, sibling-relative paths and versions;
- `src/main/resources/META-INF/mods.toml` - Forge metadata and mandatory dependencies.

Production Java:

- `src/main/java/io/github/kxyr0/echodialoguenpcadapter/EchoDialogueNpcAdapterMod.java` - Forge entrypoint and D6 event listener;
- `diagnostic/AdapterDiagnostics.java` - bounded Russian warnings;
- `identity/SpeakerActorIds.java` - exact `SpeakerId` to `ActorId` conversion;
- `npc/NpcServiceAccess.java` - lookup boundary for tests and production session;
- `npc/NpcServiceLookup.java` - neutral lookup result;
- `npc/ForgeNpcServiceAccess.java` - public Echo NPC Forge bridge call;
- `presentation/ActorStatusPolicy.java` - public `NpcActorView.Status` decision table;
- `presentation/NpcSpeakerPresentation.java` - display name and visual mapping;
- `presentation/NpcSpeakerPresentationProviderFactory.java` - D6 provider descriptor and session creation;
- `presentation/NpcSpeakerPresentationSession.java` - D6 request handling and `NpcService.query`.

N5 Echo NPC form fields are consumed as optional presentation metadata:
`ActorId` remains the speaker identity, while active form snapshot portrait and
face profile can override visual selection when available.

Tests:

- `src/test/java/.../NpcSpeakerPresentationSessionTest.java` - plain JVM contract tests;
- `src/architectureTest/java/.../AdapterArchitectureTest.java` - dependency, forbidden text and cycle checks.
- `src/gameTest/java/.../DialogueNpcPresentationGameTests.java` - Forge GameTest server checks.
- `src/acceptanceHarness/java/.../N4AcceptanceHarnessMod.java` - test-only JavaFML runtime harness, excluded from production JAR.

Documentation:

- `docs/integration/*` - identity, status, presentation, fallback, restart, threading and diagnostics;
- `docs/forge/*` - registration, public dependencies and isolated testing;
- `docs/adr/*` - durable N4 design decisions.
