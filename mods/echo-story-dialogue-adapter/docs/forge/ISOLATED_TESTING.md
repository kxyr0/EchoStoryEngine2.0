# Isolated Testing

Изолированный runtime должен использовать final JARs, а не sibling source sets:

- Echo Story Engine production JAR: `echo-story-engine-0.1.0-all.jar`
- Echo Dialogue production JAR: `echo-dialogue-0.1.0-SNAPSHOT-all.jar`
- Echo Story Dialogue Adapter production JAR

Проверяемые сценарии:

- dedicated server starts with all three mods
- Story dispatches `echo_dialogue:play`
- Echo Dialogue opens overlay
- player completes linear dialogue
- Story receives completion and continues
- restart after WAITING before adapter start
- restart while Dialogue session is ACTIVE
- restart after Dialogue terminal before Story completion
- active cancellation

Зафиксированный headless result:

- 2026-07-17 10:57 MSK: isolated server
  `D:\JDHSeason\mods\echostoryengine\story-forge-1.20.1\build\isolated-production-smoke`
  содержал ровно `echo-story-engine-0.1.0-all.jar` (414 446 bytes),
  `echo-dialogue-0.1.0-SNAPSHOT-all.jar` (283 816 bytes) и
  `echo-story-dialogue-adapter-0.1.0-SNAPSHOT.jar` (18 515 bytes).
- `.\run.bat --nogui` reached `Done (5.689s)`.
- Log showed `JarInJarDependencyLocator: Found 2 dependencies`, `EDLG-FORGE-006`, `ESE-FORGE-EXT-004` and
  `ESE-FORGE-HOST-001`.
- `stop` produced `EDLG-FORGE-007`, `ESE-FORGE-HOST-002`, saved all dimensions and exited with code 0.

- 2026-07-18 18:23 MSK: repeated isolated server
  `D:\JDHSeason\mods\echostoryengine\story-forge-1.20.1\build\isolated-production-smoke`
  with exactly the same three production mod JARs:
  `echo-story-engine-0.1.0-all.jar` (414 446 bytes),
  `echo-dialogue-0.1.0-SNAPSHOT-all.jar` (283 816 bytes) and
  `echo-story-dialogue-adapter-0.1.0-SNAPSHOT.jar` (18 515 bytes).
- `.\run.bat --nogui` reached `Done (6.415s)`, logged
  `JarInJarDependencyLocator: Found 2 dependencies`, `EDLG-FORGE-006`, `ESE-FORGE-EXT-004` and
  `ESE-FORGE-HOST-001`.
- `stop` logged `EDLG-FORGE-007`, `ESE-FORGE-HOST-002`,
  `ThreadedAnvilChunkStorage: All dimensions are saved`, stopped RCON and exited with code 0.

Visual client, acceptance content completion and restart/recovery acceptance still need separate execution.
