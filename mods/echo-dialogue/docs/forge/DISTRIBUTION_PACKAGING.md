# Distribution packaging D2

## Final JAR

```text
D:\JDHSeason\mods\echo-dialogue\dialogue-forge-1.20.1\build\libs\echo-dialogue-0.1.0-SNAPSHOT-all.jar
```

Сборка:

```powershell
.\gradlew.bat :dialogue-forge-1.20.1:jar --console=plain
.\gradlew.bat :dialogue-forge-1.20.1:jarJar --console=plain
.\gradlew.bat :dialogue-forge-1.20.1:verifyProductionJarContents --console=plain
```

`check` зависит от `verifyProductionJarContents`.

## Packaging strategy

Forge module embeds:

- `dialogue-contracts`;
- `dialogue-core`;
- `dialogue-persistence`;
- `dialogue-echoscript-adapter`;
- `dialogue-forge-api-1.20.1`;
- nested EchoScript API through Forge Jar-in-Jar;
- nested EchoScript language implementation through Forge Jar-in-Jar;
- nested Echo UI Animations through Forge Jar-in-Jar;
- Forge platform classes for Echo Dialogue;
- client overlay/network classes;
- client speaker visual resolver/renderers;
- durable persistence classes;
- `META-INF/mods.toml`;
- bundled sample dialogue resources;
- public sample visual assets under `assets/sample/textures/echo_dialogue/**`.

EchoScript is not unpacked as root-level `io/github/kxyr0/echoscript/**` classes. It is packaged under
`META-INF/jarjar` so Echo Dialogue can run together with Echo Story Engine without Java module split-package conflicts.
Echo UI Animations is also packaged under `META-INF/jarjar`; users do not copy a separate `echo-ui-animations.jar`
for Echo Dialogue production runtime.

Gson используется как runtime dependency из Minecraft/Forge classpath и не shaded в final JAR, чтобы не встраивать duplicate `com/google/gson/**`.

## Forbidden artifact content

`verifyProductionJarContents` rejects:

- GameTest implementation classes;
- root-level `io/github/kxyr0/echoscript/**`;
- root-level `io/github/kxyr0/echouianimations/**`;
- JUnit/OpenTest4J/API Guardian;
- ArchUnit, PMD, Checkstyle;
- Minecraft and Forge classes;
- Echo Story Engine classes;
- duplicate `com/google/gson/**`;
- `module-info.class`;
- `META-INF/versions/**`;
- `META-INF/maven/**`;
- `docs/**`, `build/**`, `run/**`;
- root planning/status files.
- test-only `assets/echo_dialogue_test/**` fixtures.

## Build-local public artifacts

External adapter builds use generated public Maven artifacts instead of sibling project dependencies:

```powershell
.\gradlew.bat publishDialogueIntegrationArtifacts --console=plain
```

Generated coordinates:

```text
io.github.kxyr0.echodialogue:dialogue-contracts:0.1.0-SNAPSHOT
io.github.kxyr0.echodialogue:dialogue-presentation-api:0.1.0-SNAPSHOT
io.github.kxyr0.echodialogue:dialogue-forge-api-1.20.1:0.1.0-SNAPSHOT
```

`dialogue-contracts` contains neutral public service/session/observation contracts and public `SpeakerVisualRef`.
`dialogue-presentation-api` contains neutral D6 provider contracts. `dialogue-forge-api-1.20.1` contains public Forge
events, including `EchoDialogueServiceLookupEvent` and `EchoDialogueSpeakerPresentationRegistrationEvent`; concrete
runtime catalogs remain inside the final `echo-dialogue` Forge runtime JAR.

D6 separate test provider:

`integration-tests/echo-dialogue-presentation-test-mod/build/libs/echo-dialogue-presentation-test-0.1.0-SNAPSHOT.jar`

It must not be embedded into Echo Dialogue final JAR and must not bundle public API class copies.

Repository path:

```text
D:\JDHSeason\mods\echo-dialogue\build\integration-maven
```

## Isolated final-JAR server procedure

1. Build the JAR.
2. Create or reuse a clean Forge `1.20.1-47.4.10` server directory under:

```text
D:\JDHSeason\mods\echo-dialogue\dialogue-forge-1.20.1\build\smoke\server
```

3. Ensure `mods` contains only `echo-dialogue-0.1.0-SNAPSHOT-all.jar`.
4. Do not copy project module jars, EchoScript jars or Echo UI Animations jars separately.
5. Do not accept EULA automatically; if needed, stop and ask the user to edit `eula.txt`.
6. Start server with `run.bat --nogui`.
7. Verify logs:
   - `echo_dialogue` mod discovered;
   - `EDLG-FORGE-006`;
   - `EDLG-FORGE-003 Reload диалогов принят` with bundled D5 resources;
   - persistence root points to the isolated world save;
   - no `ClassNotFoundException`;
   - no `NoClassDefFoundError`;
   - no split-package/module error;
   - no client classloading crash.
8. Stop with `stop`.
9. Verify process exit code `0` and `EDLG-FORGE-007`.

## Artifact inspection commands

```powershell
jar tf .\dialogue-forge-1.20.1\build\libs\echo-dialogue-0.1.0-SNAPSHOT-all.jar
.\gradlew.bat :dialogue-forge-1.20.1:verifyProductionJarContents --console=plain
```
