# Runtime Smoke

Дата последнего запуска: 2026-07-21.

Environment:

- Minecraft `1.20.1`;
- Forge `47.4.10`;
- Java `21.0.10`;
- production server directory: `build/production-runtime-server`;
- acceptance server directory: `build/acceptance-runtime-server`;
- production runtime JAR source: `build/story-pack/mods`;
- acceptance runtime JAR source: `build/acceptance-story-pack/mods`.

Production task:

```powershell
.\gradlew.bat runProductionStoryPackSmokeServer -PskipSiblingBuild --console=plain
```

Production JAR set:

- `echo-animation-0.1.0-SNAPSHOT-all.jar`;
- `echo-dialogue-0.1.0-SNAPSHOT-all.jar`;
- `echo-npc-0.1.0-SNAPSHOT-all.jar`;
- `echo-story-content-pack-0.1.0-SNAPSHOT.jar`;
- `echo-story-dialogue-adapter-0.1.0-SNAPSHOT.jar`;
- `echo-story-engine-0.1.0-all.jar`;
- `echo-story-npc-adapter-0.1.0-SNAPSHOT.jar`.

Production smoke result:

- all seven production JARs discovered by Forge;
- no authoring, acceptance or GameTest JAR is present;
- content pack loaded through `lowcodefml`;
- server reached `Done`;
- Story catalog listed `jdh:chapter_1_home_arrival`;
- Dialogue catalog listed `jdh:chapter_1_home_arrival`;
- NPC definitions listed `jdh:john_father`;
- Echo Animation catalog loaded `rigs=1`, `poses=1`, `animations=1`, `diagnostics=0`;
- `jdh:john_father` FaceRig contains sclera, pupil, eyelid, brow and glasses layer bindings;
- `/echo_animation health`, `face_rigs`, `face_rig`, `face_poses`, `face_animations`, `diagnostics` returned the authored `jdh` resources;
- `stop` completed with exit code 0.

Acceptance task:

```powershell
.\gradlew.bat runAcceptanceStoryPackServer -PskipSiblingBuild --console=plain
```

Acceptance harness JAR set:

- the six dependency/runtime JARs from production smoke;
- `echo-story-content-pack-0.1.0-SNAPSHOT-acceptance.jar`;
- `echo-story-content-pack-acceptance-harness-0.1.0-SNAPSHOT.jar`.

Acceptance result:

- server reached `Done`;
- accepted branch passed with outcome `jdh:content_pack_accepted`;
- refused branch passed with outcome `jdh:content_pack_refused`;
- NPC actor reached `MATERIALIZED`;
- Dialogue advanced through real server-side advance/choice paths;
- harness printed `ESCPC-ACCEPTANCE AUTO_PASS scenarios=accepted,refused passes=2`;
- runner stopped the server through console `stop` with exit code 0;
- final `latest.log` strict scan found no `ERROR`, `FATAL`, `ClassNotFoundException`, `NoClassDefFoundError`, `NoSuchMethodError`, `Failed to start` or `encountered an error`.

Not covered by headless smoke:

- client-side Dialogue UI rendering;
- visual portrait fallback rendering;
- visual Echo Animation layer rendering in a real client.

Ручная клиентская проверка face layers:

1. Положите staged production JARs из `build/story-pack/mods` в один client `mods` folder.
2. Проверьте, что `echo-animation-0.1.0-SNAPSHOT-all.jar` и `echo-story-content-pack-0.1.0-SNAPSHOT.jar` свежие.
3. Запустите мир или подключитесь к server с тем же staged production JAR set.
4. Загрузите NPC `jdh:john_father` и посмотрите на front head.
5. Подтвердите видимые sclera, pupils, eyelids, brows и glasses; затем настраивайте `placeX/placeY/placeZ` в
   `src/main/resources/data/jdh/echo_animation/face_rigs/chapter_1/family/john_father.face-rig.es`.

Evidence files:

- production smoke log: `build/reports/runtime/production-story-pack-smoke-latest.log`;
- acceptance server log: `build/reports/runtime/acceptance-story-pack-server-latest.log`;
- live production Forge log: `build/production-runtime-server/logs/latest.log`;
- live acceptance Forge log: `build/acceptance-runtime-server/logs/latest.log`.
