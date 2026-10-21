# Distribution Packaging

## Goal

Users install one Forge mod jar. The current production runtime artifact is the Forge Jar-in-Jar artifact:

```text
story-forge-1.20.1/build/libs/echo-story-engine-0.1.0-all.jar
```

It contains Echo Story Engine runtime code and nested EchoScript runtime jars under `META-INF/jarjar`.

Forge, Minecraft, GameTest, JUnit, generated runtime artifacts, and local machine paths must not be packaged.
Gson is intentionally not unpacked into the mod jar because Forge/Minecraft already supplies `com.google.gson` on the
server runtime path. Embedding Gson creates a Java module split-package conflict.

## Jar Contents

`story-forge-1.20.1:jarJar` embeds production outputs from:

- `story-contracts`;
- `story-core`;
- `story-definition`;
- `story-echoscript-adapter`;
- `story-persistence`.

It also includes EchoScript through Forge Jar-in-Jar metadata, not as root-level classes:

- `io.github.kxyr0.echoscript:echo-script-api`;
- `io.github.kxyr0.echoscript:echo-script-language`.

The embedded external runtime configuration excludes:

- `com.google.code.gson:gson`;
- `com.google.errorprone:error_prone_annotations`.

`story-persistence` still compiles against Gson, but the Forge distributable relies on the Gson already present in the
Forge/Minecraft server runtime.

## Content Check

Run:

```powershell
.\gradlew.bat :story-forge-1.20.1:forgeJarContainsRuntimeClasses
```

The check requires Story Engine classes, Forge entrypoint classes, host classes, command classes, `META-INF/mods.toml`
and `META-INF/jarjar/metadata.json`.

The check rejects production jar entries for:

- `com/google/gson/`;
- root-level `io/github/kxyr0/echoscript/`;
- Minecraft classes;
- Forge classes;
- GameTest implementation classes;
- JUnit;
- generated `run`/`reports`/`docs` artifacts;
- `AGENTS.md`;
- `eula.txt`;
- `latest.log`;
- stale MDK identifiers `examplemod` and `com.example`;
- absolute local paths;
- Maven local metadata;
- Gradle cache strings.

Latest result on 2026-07-17: `forgeJarContainsRuntimeClasses` passed against
`echo-story-engine-0.1.0-all.jar` after moving EchoScript to Forge Jar-in-Jar.

## Isolated Production-JAR Smoke

An isolated Forge 1.20.1 dedicated server was installed under:

```text
D:\JDHSeason\mods\echostoryengine\story-forge-1.20.1\build\isolated-production-smoke
```

The isolated `mods` directory contains exactly one mod:

```text
echo-story-engine-0.1.0-all.jar
```

The first isolated launch found a packaging defect: the mod jar embedded `com/google/gson/**`, and ModLauncher failed
with a Java module split-package error between `echo_story_engine` and `com.google.gson`.

After the packaging fix, the isolated launch reached Minecraft server startup and stopped only at the generated EULA
gate:

```text
D:\JDHSeason\mods\echostoryengine\story-forge-1.20.1\build\isolated-production-smoke\eula.txt
```

The isolated runtime directory is prepared with:

- `server-port=25566`;
- `enable-rcon=true`;
- `rcon.port=25576`;
- `rcon.password=echo_story_isolated_smoke`;
- datapack `world/datapacks/echo_story_smoke`;
- exactly one mod jar in `mods`.

After manual EULA acceptance, the isolated smoke passed:

- command: `.\run.bat --nogui`;
- process exit code: `0`;
- `mods` contained exactly `echo-story-engine-0.1.0.jar`;
- datapack `echo_story_smoke` was discovered;
- catalog revision `1` loaded one `.es` story;
- RCON verified `echo_story health`, `echo_story stories`, `echo_story diagnostics 20`, `reload`, post-reload command
  output, and `stop`;
- final command-contract RCON smoke also verified `echo_story executions`, `echo_story execution sample:missing`,
  `echo_story diagnostics` without an argument, and invalid `diagnostics` limits `0`, `-1`, `abc`, and `51`;
- reload installed catalog revision `2` with one source and zero errors;
- `latest.log` showed clean host stop and RCON listener stop;
- negative log scan found no `NoClassDefFoundError`, `ClassNotFoundException`, `ERROR`, `FATAL`, client-only marker,
  or shutdown hang marker.

## D3 Three-Mod Smoke

On 2026-07-17 the same isolated server was also used for a three-production-mod adapter smoke. The `mods` directory
contained exactly:

```text
echo-story-engine-0.1.0-all.jar
echo-dialogue-0.1.0-SNAPSHOT-all.jar
echo-story-dialogue-adapter-0.1.0-SNAPSHOT.jar
```

The first attempts exposed two real packaging defects:

- adapter `mods.toml` required `echo_dialogue [0.1.0,)`, which rejected `0.1.0-SNAPSHOT`;
- Story and Dialogue both unpacked EchoScript root packages, causing a Java module split-package failure.

After switching Story and Dialogue production runtime artifacts to Forge Jar-in-Jar EchoScript, `.\run.bat --nogui`
reached `Done`, loaded the Story host, Dialogue host and adapter extension, then stopped cleanly with `stop`.
