Echo Animation Exporter 3.3.1 RU

НОВОЕ: ИМПОРТ .ES → BLENDER ACTION
- File > Import > «Анимация Echo (.es)» и кнопка прямо в панели Echo.
- Можно выбрать готовый .animation.es: importer сам находит Main/Enter/Exit clip-файлы и собирает их в один Action.
- Можно выбрать .animation-clip.es напрямую. Если рядом найден .animation.es, длительность берётся из него; иначе используется поле «Длительность одиночного клипа».
- Импорт понимает schemaVersion 2; новый JDH Humanoid v2 экспортируется как schemaVersion 3, где body_lower и body имеют независимую семантику. Matrix(...) остаётся доступен для произвольных runtime/deform skeleton.
- Поддерживаются старые legacy-файлы с Duration/Track/Key.
- Восстанавливаются Loop / Hold / One Shot / Lifecycle и настройки быстрого повторного экспорта.
- Echo STEP/LINEAR/SMOOTH/CUBIC переводятся в Blender Constant/Linear/Bezier.
- Неизвестные кости можно пропускать или считать ошибкой.
- Важно для сложных control rig: .es хранит итоговые runtime/deform-трансформы, а не исходное положение IK/FK-контролов. Если bone управляется constraint, importer предупреждает. Есть явная опция отключить constraints на импортируемых костях, но она меняет rig и по умолчанию выключена.

Echo Animation Exporter 3.1.1 RU

MODERN MOTION 3.1.1
- В панели Echo появился блок «Modern анимации».
- «Создать Modern Idle» создаёт 4-секундный бесшовный idle: дыхание, перенос веса, микродвижения головы/плеч/локтей/коленей.
- «Создать Modern Walk» создаёт современный цикл шага примерно 0.9 сек: таз, контрвращение корпуса, плечи, локти, кисти, колени и стопы.
- Это не ванильный маятник руками/ногами: движение распределено по всему телу и слегка асимметрично.
- JDH Humanoid v2 использует body_lower + body, поэтому Modern Idle/Walk сразу анимируют обе половины корпуса. TheRig 2.6 дополнительно использует кисти и стопы.
- Лицо/ресницы/служебные lattice-кости намеренно не дёргаются «ради галочки»: они наследуют движение тела и не получают бессмысленный шум.
- Оба пресета создают новый Action и сразу сохраняют настройки экспорта как настоящий Loop. Нажмите «Быстрый экспорт» — Enter/Exit не нужны.
- Пресет не двигает персонажа вперёд: только локальный bob/sway. Фактическое перемещение NPC остаётся за runtime/pathfinding.

------------------------------------------------------------

Echo Animation Exporter 3.1.0 RU

ГЛАВНОЕ ИЗМЕНЕНИЕ 3.1.0
- One Shot, Loop и Hold теперь являются прямыми режимами Main.
- Для idle / walk / run НЕ создаются фальшивые Enter и Exit клипы.
- Loop использует настоящий runtime MainMode.LOOP_UNTIL_STOP.
- Hold использует настоящий runtime MainMode.HOLD_UNTIL_STOP: Action доходит до последнего кадра и держит его до Stop. Никаких 100000 hold-тиков.
- Enter/Main/Exit вынесены в отдельный режим «Фазы» и появляются только когда реально нужны.
- Новые Action с именами idle, walk, run, sprint, sneak, crawl, jog или locomotion автоматически определяются как Loop.
- Настройки по-прежнему сохраняются отдельно в каждом Blender Action. Старые 3.0.x настройки мигрируются автоматически; огромный legacy hold переводится в настоящий Hold.

Быстрый путь аниматора:
1. Выберите Armature и Action.
2. Для idle/walk/run просто нажмите «Быстрый экспорт» — режим Loop определяется по имени.
3. Для позы откройте настройки один раз и выберите Hold. Дальше Action это запомнит.
4. Режим «Фазы» используйте только для настоящего Enter -> Main -> Exit.

------------------------------------------------------------

Echo Animation Exporter 3.0.2 — coordinate conversion fix

INSTALL
1. Install the ZIP or copy this folder to Blender scripts/addons.
2. Enable "Echo Animation Exporter" in Blender Preferences.
3. Open the Echo panel in 3D View > N > Echo.

THERIG 2.6 QUICK START
1. Open TheRig2.6.blend and select the armature named TheRig.
2. Choose Export Echo Skinned Model. When the armature is active, the exporter
   automatically collects visible render meshes driven either directly by the
   armature or through TheRig's Lattice modifiers.
3. Keep "Write AnimationRig definition" enabled. The exporter writes the exact
   full evaluated deform/lattice *.animation-rig.es contract used by the model and animation clips.
4. To export animation, make an Action active on TheRig and choose Export Echo
   Animation. FK, IK, pole targets, drivers and constraints are evaluated first;
   only the resulting runtime-bone transforms are written.

TheRig 2.6 is detected automatically from its bone signature. The exporter keeps
all deform bones required by the Armature/Lattice deformation hierarchy, required
ancestors, and explicit item attachment bones. Blender-only IK/FK/pole/driver
controls are evaluated in Blender and their final local matrices are baked directly
into EchoScript. Runtime skeletons may contain up to 512 bones; palettes above the
current GPU shader limit automatically use Echo's CPU skinning fallback.
The authoring rig and the .blend file are never modified.

SOFT LATTICE WEIGHTS
TheRig's main Body, Head, arm and leg meshes have no ordinary Armature modifier or
vertex groups. Their soft deformation lives on armature-driven lattice cages. The
exporter transfers cage interpolation and point weights onto every mesh vertex,
keeps the real lattice/deform bone targets, keeps the strongest four influences and
normalizes them. Direct facial meshes retain their original deform-bone weights.
Hidden render variants are excluded when exporting from the active armature;
explicitly selected meshes are honored.

SKINNED MODEL EXPORT
- File > Export > Echo Skinned Model (.skin.json), or the Echo sidebar.
- All exported meshes need an active UV layer and must resolve to one armature.
- Validate Echo Skin builds and validates everything without writing files.
- Assign Rigid Attachment overrides selected vertices with one 100% runtime bone.
- Non-uniform mesh-object scale is baked into positions and normals. The shared
  armature object must keep uniform, non-zero scale.

RIG DEFINITION OUTPUT
TheRig uses the default resource ID jdh:therig_2_6. Its model resolves to:

  assets/jdh/echo_animation/models/therig_2_6.skin.json

When the skin is exported below an assets folder, its companion is placed at:

  data/jdh/echo_animation/animation_rigs/therig_2_6.animation-rig.es

For an arbitrary folder the companion is written beside the skin. It contains the
same bone names, parents and bind-local transforms, plus hand sockets.

OTHER RIG SCHEMAS
The Armature data block exposes echo_rig_id and echo_rig_schema:

- JDH Humanoid v2: 14 runtime-костей; корпус разделён на body_lower и body (верх).
- JDH Humanoid v1: старый 13-костный режим только для совместимости; в панели есть миграция в v2.
- TheRig 2.6: full deform/lattice profile with exact evaluated matrix baking (up to 512 runtime bones).
- Runtime / Deform hierarchy: deterministic custom hierarchy, up to 512 bones.

Custom bones may use echo_runtime, echo_runtime_name, echo_aliases,
echo_bone_class and echo_runtime_index ID properties. Runtime names must be lower
snake_case and match the corresponding AnimationRigDefinition.

SKIN.JSON V2 CONTRACT
The output follows SkinnedModelReader exactly and contains only:

  formatVersion: 2
  bones: name, parent, bindLocal, inverseBind
  meshes: name, vertices, triangles
  vertices: position, normal, uv, boneIndices, boneWeights

Matrices are column-major. Blender X/right, Y/forward, Z/up becomes Echo X/right,
Y/down, Z/forward. Positions and bind translations use 16 Minecraft pixels per
Blender unit. UV V is flipped. Normals use inverse-transpose conversion.

VALIDATION
- 1..512 bones, one or more roots, valid parent order and inverse-bind matrices.
- Unique mesh and bone names, UVs, finite geometry and valid triangle indices.
- One to four normalized positive influences per vertex.
- Unknown groups are reported; unweighted vertices use a profile fallback.

ANIMATION EXPORT
JDH Humanoid v2 exports body_lower and body independently. TheRig and non-legacy schemas sample source bones
after Blender evaluation and write exact row-major local 4x4 Matrix(...) frames;
there is no TRS decompose/recompose step, so evaluated shear/non-uniform affine
results are preserved. AUTO and TIMELINE modes, Enter/Main/Exit, authored neutral
poses and hold optimization continue to work.


SYSTEM SIT POSE (3.3.1)
------------------------
- Назовите Blender Action `pose_sit`.
- Быстрый экспорт автоматически выберет HOLD_UNTIL_STOP.
- Генерируется clip ID `jdh:pose_sit_main`.
- Runtime 0.7.1+ автоматически использует этот clip как базовую позу любого NPC, у которого sitting=true.
- Не нужны Enter/Exit и не нужен огромный holdTicks.
- Диалоговые/сюжетные клипы поверх сидения заменяют только те кости, которые реально анимируют.
