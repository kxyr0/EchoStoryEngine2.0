# Dedicated Server Testing

## Command

ForgeGradle server run:

```powershell
.\gradlew.bat :story-forge-1.20.1:runServer --console=plain
```

Automation does not accept the Minecraft EULA. A dedicated server smoke may run only after the relevant generated
`eula.txt` already contains `eula=true`.

## Normal Smoke Result

Status on 2026-07-12: passed for the development ForgeGradle run directory.

EULA file:

```text
D:\JDHSeason\mods\echostoryengine\story-forge-1.20.1\run\eula.txt
```

Observed state:

```text
eula=true
```

The smoke used a local runtime datapack with one `.es` StoryResource:

```text
story-forge-1.20.1/run/world/datapacks/echo_story_smoke/data/sample/echo_story/stories/server_smoke.es
```

Because Gradle console stdin did not forward server commands in this environment, the run used local RCON from
`story-forge-1.20.1/run/server.properties` to issue the acceptance commands.

Verified commands:

- `echo_story health`
- `echo_story stories`
- `echo_story diagnostics 20`
- `reload`
- `echo_story health` after reload
- `echo_story stories` after reload
- `echo_story diagnostics 20` after reload
- `stop`

Observed command results:

- server reached `Done`;
- RCON started;
- host became `ACTIVE`;
- `.es` resources loaded from the datapack;
- health reported catalog `ACCEPTED`, story count `1`, active executions `0`, waiting executions `0`;
- persistence queue reported `0/64`;
- server work queue reported `0/0` because no separate server-thread queue is used;
- recent warning/error count was `0`;
- `stories` listed `sample:server_smoke`;
- `diagnostics 20` listed host-started and catalog-installed diagnostics;
- `reload` installed catalog revision `2` with one source and zero errors;
- `stop` disposed the host cleanly.

`story-forge-1.20.1/run/logs/latest.log` was scanned for startup/shutdown evidence and for failure markers. The
smoke found no `NoClassDefFoundError`, `ClassNotFoundException`, client-only class loading marker, `ERROR`, `FATAL`,
or shutdown hang. Only standard Forge logs were created; no application-owned subsystem log files were produced.

## Isolated Smoke Result

Status on 2026-07-12: passed for the isolated production-JAR server directory.

The isolated production-JAR smoke uses its own generated server directory and separate EULA file:

```text
D:\JDHSeason\mods\echostoryengine\story-forge-1.20.1\build\isolated-production-smoke\eula.txt
```

Observed state after manual acceptance:

```text
eula=true
```

The isolated `mods` directory contained exactly one mod jar:

```text
echo-story-engine-0.1.0.jar
```

The isolated server was started with:

```powershell
.\run.bat --nogui
```

RCON on port `25576` verified `echo_story health`, `echo_story stories`, `echo_story diagnostics 20`, `reload`,
post-reload command output, and `stop`. The process exited with code `0`.

The final isolated command-contract smoke also verified `echo_story executions`, `echo_story execution sample:missing`,
`echo_story diagnostics` without an argument, and invalid `diagnostics` limits `0`, `-1`, `abc`, and `51`.

`build/isolated-production-smoke/logs/latest.log` showed datapack discovery, catalog install, server `Done`, RCON
running, host start, reload catalog install, host stop, and RCON listener stop. The isolated smoke found no
`NoClassDefFoundError`, `ClassNotFoundException`, client-only class loading marker, `ERROR`, `FATAL`, or shutdown hang
marker.
