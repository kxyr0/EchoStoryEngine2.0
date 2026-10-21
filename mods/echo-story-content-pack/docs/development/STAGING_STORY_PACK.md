# Staging story pack

Команда:

```powershell
.\gradlew.bat stageStoryPack
```

Output:

`build/story-pack/`

Содержимое:

- `mods/`
- `README.txt`
- `versions.txt`

Ожидаемые production JARs:

- `echo-story-engine-0.1.0-all.jar`
- `echo-dialogue-0.1.0-SNAPSHOT-all.jar`
- `echo-story-dialogue-adapter-0.1.0-SNAPSHOT.jar`
- `echo-npc-0.1.0-SNAPSHOT-all.jar`
- `echo-story-npc-adapter-0.1.0-SNAPSHOT.jar`
- `echo-story-content-pack-0.1.0-SNAPSHOT.jar`

`stageStoryPack` не копирует:

- API JARs;
- contracts/core/internal module JARs;
- sources JARs;
- GameTest JARs;
- acceptance mods;
- separate EchoScript JARs.

Sibling dirs можно переопределить Gradle properties:

- `story_engine_project_dir`
- `echo_dialogue_project_dir`
- `echo_npc_project_dir`
- `story_dialogue_adapter_project_dir`
- `story_npc_adapter_project_dir`

Для использования уже собранных sibling JARs можно добавить `-PskipSiblingBuild=true`.

Acceptance staging:

```powershell
.\gradlew.bat stageAcceptanceStoryPack
```

Output:

`build/acceptance-story-pack/`

Эта задача берет те же five final runtime JARs и заменяет production content JAR на:

`echo-story-content-pack-0.1.0-SNAPSHOT-acceptance.jar`

Этот pack предназначен только для runtime acceptance canonical IDs `jdh:content_pack_*`.
