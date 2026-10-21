'use strict';

const SIGNATURES = {
  Story: [
    signature(
      'Story(id: StoryId | string, start: string) -> StoryResourceDef',
      'Builds the root modular story graph resource. The start argument names the first graph node.',
      [
        ['id', 'Stable story resource id, for example "jdh:chapter_1_home_arrival".'],
        ['start', 'Node id where this story graph starts.']
      ]
    )
  ],
  Start: [
    signature(
      'Start(id: string, next?: string) -> StoryNodeDef',
      'Builds a story graph start node.',
      [
        ['id', 'Local story node id.'],
        ['next', 'Next local story node id.']
      ]
    ),
    signature(
      'Start(node: NodeDef | string) -> void',
      'Sets the starting node inside Dialogue Init().',
      [
        ['node', 'Dialogue node id or node reference.']
      ]
    ),
    signature(
      'Start(method: MethodRef) -> void',
      'Sets the sequence method inside TriggerResource.Init().',
      [
        ['method', 'TriggerResource sequence method name.']
      ]
    )
  ],
  Action: [
    signature(
      'Action(id: string, type: string, ...fields, output?: string, next?: string) -> StoryNodeDef',
      'Builds a modular story graph action node that delegates to an engine adapter such as echo_dialogue:play, echo_npc:spawn, or echo_animation:play_animation.',
      [
        ['id', 'Local story node id.'],
        ['type', 'Engine adapter type id.'],
        ['fields', 'Adapter-specific payload fields.'],
        ['output', 'Optional output binding slot.'],
        ['next', 'Next local story node id.']
      ]
    ),
    signature(
      'Action() -> ActionDef',
      'Builds the older interaction action definition.',
      []
    )
  ],
  Set: [
    signature(
      'Set(id: string, variable: string, value: bool | number | string, next?: string) -> StoryNodeDef',
      'Builds a story graph node that writes state.',
      [
        ['id', 'Local story node id.'],
        ['variable', 'Story state key to write.'],
        ['value', 'Value assigned to the state key.'],
        ['next', 'Next local story node id.']
      ]
    ),
    signature(
      'Set(state: StateRef | string, value: any) -> Effect',
      'Builds a runtime state-write effect.',
      [
        ['state', 'StateRef or state key.'],
        ['value', 'Value assigned to the state key.']
      ]
    )
  ],
  Finish: [
    signature(
      'Finish(id: string) -> StoryNodeDef',
      'Builds a terminal story graph node.',
      [
        ['id', 'Local finish node id.']
      ]
    )
  ],
  Dialogue: [
    signature(
      'Dialogue(id: DialogueId | string) -> DialogueResourceDef',
      'Builds a modular dialogue resource.',
      [
        ['id', 'Stable dialogue resource id.']
      ]
    )
  ],
  Npc: [
    signature(
      'Npc(id: NpcDefinitionId | string, display: string, skin?: SkinDef, nameColor?: NpcNameColor, defaultForm?: NpcDefaultFormRef) -> NpcResourceDef',
      'Builds an Echo NPC definition resource.',
      [
        ['id', 'Stable NPC definition id.'],
        ['display', 'Display fallback used by tools and runtime UI.'],
        ['skin', 'NPC skin visual, usually Skin("id").'],
        ['nameColor', 'NPC display-name color, usually NameColor("#ffffff").'],
        ['defaultForm', 'Default authored NPC form, usually DefaultForm("namespace:path").']
      ]
    )
  ],
  NpcAnimationBinding: [
    signature(
      'NpcAnimationBinding(id: NpcAnimationBindingId | string, npc?: NpcDefinitionId | string, actor?: ActorId | string, profile: AnimationProfileId | string) -> NpcAnimationBindingDef',
      'Binds an NPC definition or actor to an animation profile.',
      [
        ['id', 'Stable binding resource id.'],
        ['npc', 'NPC definition id.'],
        ['actor', 'Optional specific actor id override.'],
        ['profile', 'Animation profile id.']
      ]
    )
  ],
  Animation: [
    signature(
      'Animation(id: AnimationId | string, channels?: ChannelDef[], sources?: SourceDef[], entries?: EntryDef[], main?: MainDef, exits?: ExitDef[], target?: string, recovery?: RecoveryDef, replay?: string) -> AnimationDef',
      'Builds a modular animation lifecycle resource from entry clips, main playback, exits, recovery, and replay policy.',
      [
        ['id', 'Stable animation resource id.'],
        ['channels', 'Declared animation channels.'],
        ['sources', 'Graph source states.'],
        ['entries', 'Entry phase definitions.'],
        ['main', 'Main playback phase.'],
        ['exits', 'Exit phase definitions.'],
        ['target', 'Target state after recovery.'],
        ['recovery', 'Recovery policy.'],
        ['replay', 'Replay policy.']
      ]
    )
  ],
  AnimationProfile: [
    signature(
      'AnimationProfile(id: AnimationProfileId | string, rig: AnimationRigId | string, graph?: AnimationRigId | FaceRigId | string, render?: string, initialStates?: InitialStateDef[], requiredChannels?: ChannelDef[], metadata?: MetaDef[]) -> AnimationProfileDef',
      'Builds an animation subject profile.',
      [
        ['id', 'Stable animation profile id.'],
        ['rig', 'Animation rig id.'],
        ['graph', 'Animation graph id.'],
        ['render', 'Render model kind.'],
        ['initialStates', 'Initial graph states.'],
        ['requiredChannels', 'Required animation channels.'],
        ['metadata', 'Authoring metadata.']
      ]
    )
  ],
  AnimationRig: [
    signature(
      'AnimationRig(id: AnimationRigId | string, kind?: string, skin?: string, bones?: BoneDef[], sockets?: SocketDef[]) -> AnimationRigDef',
      'Builds a body animation rig resource.',
      [
        ['id', 'Stable animation rig id.'],
        ['kind', 'Rig kind.'],
        ['skin', 'Humanoid skin variant or rig skin mode.'],
        ['bones', 'Custom bones.'],
        ['sockets', 'Attachment sockets.']
      ]
    )
  ],
  AnimationClip: [
    signature(
      'AnimationClip(id: AnimationClipId | string, rig: AnimationRigId | FaceRigId | string, tracks?: TrackDef[], schemaVersion?: int, loopSeam?: string, authoredSpeedBlocksPerSecond?: number) -> AnimationClipDef',
      'Builds a normalized animation clip for a body or face rig.',
      [
        ['id', 'Stable animation clip id.'],
        ['rig', 'Body animation rig or face rig id.'],
        ['tracks', 'Animation tracks.'],
        ['schemaVersion', 'Clip schema version.'],
        ['loopSeam', 'Loop seam policy.'],
        ['authoredSpeedBlocksPerSecond', 'Authored locomotion speed.']
      ]
    )
  ],
  FacialAnimation: [
    signature(
      'FacialAnimation(id: FaceAnimationId | string, rig?: FaceRigId | string, surfaces?: FacialAnimationSurfaceDef[], tracks?: TrackDef[], durationTicks?: int, durationMillis?: int, end?: string) -> FacialAnimationDef',
      'Builds a facial animation resource.',
      [
        ['id', 'Stable face animation id.'],
        ['rig', 'Face rig id.'],
        ['surfaces', 'Surface classes or surface definitions.'],
        ['tracks', 'Default face tracks.'],
        ['durationTicks', 'Default duration in ticks.'],
        ['durationMillis', 'Default duration in milliseconds.'],
        ['end', 'End behavior.']
      ]
    )
  ],
  FacePose: [
    signature(
      'FacePose(id: FacePoseId | string, rig: FaceRigId | string, bones?: BoneDeltaDef[], controls?: ControlDeltaDef[]) -> FacePoseDef',
      'Builds a face pose resource.',
      [
        ['id', 'Stable face pose id.'],
        ['rig', 'Face rig id.'],
        ['bones', 'Bone deltas.'],
        ['controls', 'Control value deltas.']
      ]
    )
  ],
  Entry: [
    signature(
      'Entry(source: string, clip: AnimationClipId | string, durationTicks?: int) -> AnimationEntryDef',
      'Builds an animation entry phase.',
      [
        ['source', 'Source state id.'],
        ['clip', 'Animation clip id.'],
        ['durationTicks', 'Entry duration in ticks.']
      ]
    )
  ],
  Main: [
    signature(
      'Main(clip: AnimationClipId | string, mode?: string, durationTicks?: int, exitPoints?: ExitPointDef[]) -> MainAnimationDef',
      'Builds the main animation phase.',
      [
        ['clip', 'Main animation clip id.'],
        ['mode', 'Playback mode.'],
        ['durationTicks', 'Main phase duration.'],
        ['exitPoints', 'Named exit points.']
      ]
    )
  ],
  Exit: [
    signature(
      'Exit(id: string, clip: AnimationClipId | string, durationTicks?: int) -> AnimationExitDef',
      'Builds an animation exit phase.',
      [
        ['id', 'Exit id.'],
        ['clip', 'Exit animation clip id.'],
        ['durationTicks', 'Exit duration in ticks.']
      ]
    )
  ],
  Skin: [
    signature(
      'Skin(id: SkinId | string) -> SkinVisualDef',
      'References an NPC skin resource.',
      [
        ['id', 'Skin resource id.']
      ]
    )
  ],
  SkinHead: [
    signature(
      'SkinHead(id: SkinId | string) -> SkinVisualDef',
      'References the head visual derived from a skin.',
      [
        ['id', 'Skin resource id.']
      ]
    )
  ],
  NameColor: [
    signature(
      'NameColor(color: string) -> NpcNameColor',
      'Builds an NPC display-name color value for Npc(...).',
      [
        ['color', 'Hex color such as "#RRGGBB" or "#RRGGBBAA".']
      ]
    )
  ],
  nameColor: [
    signature(
      'nameColor(color: string) -> NpcNameColor',
      'Lowercase alias accepted by Npc(...) for NPC display-name color.',
      [
        ['color', 'Hex color such as "#RRGGBB" or "#RRGGBBAA".']
      ]
    )
  ],
  Portrait: [
    signature(
      'Portrait(id: SkinId | string) -> VisualDef',
      'References a portrait visual.',
      [
        ['id', 'Skin or portrait resource id.']
      ]
    )
  ],
  SetAnimation: [
    signature(
      'Line(...).SetAnimation(animation: AnimationId | string) -> LineDef',
      'Sets the body animation for a dialogue line.',
      [
        ['animation', 'Animation resource id.']
      ]
    )
  ],
  SetAnimationFace: [
    signature(
      'Line(...).SetAnimationFace(face: FaceAnimationId | string) -> LineDef',
      'Sets the face animation for a dialogue line.',
      [
        ['face', 'Face animation resource id.']
      ]
    )
  ],
  SetFaceAnimation: [
    signature(
      'Line(...).SetFaceAnimation(face: FaceAnimationId | string) -> LineDef',
      'Sets the face animation for a dialogue line.',
      [
        ['face', 'Face animation resource id.']
      ]
    ),
    signature(
      'SetFaceAnimation(subject: NpcRef | string, animation: FaceAnimationRef | string) -> DialogueCommand',
      'Runs a dialogue command that starts face animation on a subject.',
      [
        ['subject', 'NPC subject, alias, or actor id.'],
        ['animation', 'Face animation resource id.']
      ]
    )
  ],
  SetAnimationFaceClear: [
    signature(
      'Line(...).SetAnimationFaceClear() -> LineDef',
      'Clears the face animation for a dialogue line.',
      []
    )
  ],
  ClearFaceAnimation: [signature('ClearFaceAnimation(subject: NpcRef | string) -> DialogueCommand', 'Clears active face animation on one subject.', [['subject', 'NPC subject, alias, or actor id.']])],
  ClearFaceAnimations: [signature('ClearFaceAnimations() -> DialogueCommand', 'Clears active face animations for all subjects in this dialogue flow.', [])],
  SetBlinkTiming: [signature('SetBlinkTiming(subject: NpcRef | string, periodTicks: int, closeTicks: int, holdTicks: int, openTicks: int) -> DialogueCommand', 'Sets custom blink timing on a subject.', [['subject', 'NPC subject, alias, or actor id.'], ['periodTicks', 'Blink period in ticks.'], ['closeTicks', 'Close duration in ticks.'], ['holdTicks', 'Closed hold duration in ticks.'], ['openTicks', 'Open duration in ticks.']])],
  SetIdleBlinkTiming: [signature('SetIdleBlinkTiming(subject: NpcRef | string, periodTicks: int, closeTicks: int, holdTicks: int, openTicks: int) -> DialogueCommand', 'Sets custom idle blink timing on a subject.', [['subject', 'NPC subject, alias, or actor id.'], ['periodTicks', 'Blink period in ticks.'], ['closeTicks', 'Close duration in ticks.'], ['holdTicks', 'Closed hold duration in ticks.'], ['openTicks', 'Open duration in ticks.']])],
  ClearBlinkTiming: [signature('ClearBlinkTiming(subject: NpcRef | string) -> DialogueCommand', 'Clears custom blink timing on one subject.', [['subject', 'NPC subject, alias, or actor id.']])],
  ClearIdleBlinkTiming: [signature('ClearIdleBlinkTiming(subject: NpcRef | string) -> DialogueCommand', 'Clears custom idle blink timing on one subject.', [['subject', 'NPC subject, alias, or actor id.']])],
  StopFaceAnimation: [
    signature(
      'Line(...).StopFaceAnimation() -> LineDef',
      'Stops the active face animation for a dialogue line.',
      []
    )
  ],
  LineAfterMove: [
    signature(
      'LineAfterMove(speaker: alias | SpeakerRef, text: string, key?: localizationKey, npc?: NpcRef) -> LineDef',
      'Adds a dialogue line that is shown only after the matching Npc.Move(...) has arrived. Normal Line(...) can be shown while the NPC is still moving.',
      [
        ['speaker', 'Dialogue speaker alias used by the line and, by default, the NPC whose movement is awaited.'],
        ['text', 'Visible line text.'],
        ['key', 'Optional localization key.'],
        ['npc', 'Optional NPC/speaker alias to wait for when it differs from the line speaker.']
      ]
    )
  ],
  'Npc.Spawn': [
    signature(
      'Npc.Spawn(npc: NpcRef, dimension: DimensionRef, x: int, y: int, z: int, yaw?: number, pitch?: number) -> Effect',
      'Спавнит NPC по координатам. Компилятор ожидает NPC, измерение и отдельные x/y/z аргументы.',
      [
        ['npc', 'NPC или speaker ref, например Sasha или Ben.'],
        ['dimension', 'Измерение, лучше символом Overworld/Nether/End, а не сырой строкой.'],
        ['x', 'X координата спавна.'],
        ['y', 'Y координата спавна.'],
        ['z', 'Z координата спавна.'],
        ['yaw', 'Необязательный yaw поворота NPC после спавна.'],
        ['pitch', 'Необязательный pitch поворота NPC после спавна.']
      ]
    )
  ],
  'Npc.SpawnNearPlayer': [
    signature(
      'Npc.SpawnNearPlayer(npc: NpcRef, forwardOffset?: number, sideOffset?: number, yOffset?: number) -> Effect',
      'Спавнит NPC рядом с игроком. Удобно для сцен, где точные координаты не важны.',
      [
        ['npc', 'NPC или speaker ref для спавна.'],
        ['forwardOffset', 'Смещение вперед от взгляда/позиции игрока.'],
        ['sideOffset', 'Смещение вбок от игрока.'],
        ['yOffset', 'Вертикальное смещение.']
      ]
    )
  ],
  'Npc.Move': [
    signature(
      'Npc.Move(npc: NpcRef, targetPosition: Vector3, speed?: number, completionDistance?: number, pauseTicks?: int, repathAfterTicks?: int, maxRepathAttempts?: int, teleportToTargetIfStuck?: bool, openDoors?: bool, canFloat?: bool, targetSearchRadius?: int, thenDialogue?: DialogueRef) -> Effect',
      'Alias для Npc.MoveTo. Двигает NPC к одной точке и при необходимости запускает диалог после прибытия.',
      [
        ['npc', 'NPC, которого нужно двигать.'],
        ['targetPosition', 'Целевая позиция как Vector3/new Vector3(...).'],
        ['speed', 'Скорость движения.'],
        ['completionDistance', 'Дистанция, на которой путь считается завершенным.'],
        ['pauseTicks', 'Пауза после достижения точки.'],
        ['repathAfterTicks', 'Через сколько ticks пересчитывать путь.'],
        ['maxRepathAttempts', 'Максимум попыток пересчета пути.'],
        ['teleportToTargetIfStuck', 'Телепортировать к цели, если NPC застрял.'],
        ['openDoors', 'Открывать двери по пути.'],
        ['canFloat', 'Разрешить floating navigation.'],
        ['targetSearchRadius', 'Радиус поиска ближайшей достижимой точки около цели.'],
        ['thenDialogue', 'Диалог, который стартует после прибытия.']
      ]
    ),
    signature(
      'Npc.Move(npc: NpcRef, x: number, y: number, z: number, dimension?: string, output?: string, next?: string) -> Effect',
      'Каноническая форма echo_navigation:npc_move для Dialogue/Trigger; в Story graph может быть Npc.Move(id, npc, x, y, z, next: "...").',
      [
        ['npc', 'NPC, которого нужно двигать.'],
        ['x', 'Целевая координата X.'],
        ['y', 'Целевая координата Y.'],
        ['z', 'Целевая координата Z.'],
        ['dimension', 'Dimension id; если не указан, используется minecraft:overworld.'],
        ['output', 'Output binding slot для результата story/dialogue wait.'],
        ['next', 'Следующий local story node id для Story graph формы.']
      ]
    )
  ],
  'Npc.MoveTo': [
    signature(
      'Npc.MoveTo(npc: NpcRef, targetPosition: Vector3, speed?: number, completionDistance?: number, pauseTicks?: int, repathAfterTicks?: int, maxRepathAttempts?: int, teleportToTargetIfStuck?: bool, openDoors?: bool, canFloat?: bool, targetSearchRadius?: int, thenDialogue?: DialogueRef) -> Effect',
      'Двигает NPC к одной точке. Предпочтительное имя вместо старого Npc.Move.',
      [
        ['npc', 'NPC, которого нужно двигать. Обязательный аргумент.'],
        ['targetPosition', 'Целевая позиция Vector3. Обязательный аргумент.'],
        ['speed', 'Необязательная скорость движения.'],
        ['completionDistance', 'Необязательная дистанция завершения.'],
        ['pauseTicks', 'Необязательная пауза после достижения точки.'],
        ['repathAfterTicks', 'Необязательный интервал пересчета пути.'],
        ['maxRepathAttempts', 'Необязательный лимит пересчетов пути.'],
        ['teleportToTargetIfStuck', 'Необязательно: телепортировать к цели при застревании.'],
        ['openDoors', 'Необязательно: открывать двери по пути.'],
        ['canFloat', 'Необязательно: разрешить floating navigation.'],
        ['targetSearchRadius', 'Необязательный радиус поиска достижимой точки около цели.'],
        ['thenDialogue', 'Необязательный DialogueRef после прибытия.']
      ]
    )
  ],
  'Npc.MoveToPlayer': [
    signature(
      'Npc.MoveToPlayer(npc: NpcRef, stopDistance?: number, speed?: number, repathTicks?: int, timeoutTicks?: int, lookAtPlayer?: bool, warpWhenUnseen?: bool, warpDistance?: number, warpSearchRadius?: int, thenDialogue?: DialogueRef) -> Effect',
      'Подводит NPC к игроку. Предпочтительное имя вместо старого Npc.ApproachPlayer.',
      [
        ['npc', 'NPC, который идет к игроку. Обязательный аргумент.'],
        ['stopDistance', 'Необязательная дистанция остановки около игрока.'],
        ['speed', 'Необязательная скорость движения.'],
        ['repathTicks', 'Необязательный интервал пересчета пути.'],
        ['timeoutTicks', 'Необязательный timeout подхода.'],
        ['lookAtPlayer', 'Необязательно: смотреть на игрока во время подхода.'],
        ['warpWhenUnseen', 'Необязательно: варпнуть NPC, если его не видно.'],
        ['warpDistance', 'Необязательная дистанция для warp fallback.'],
        ['warpSearchRadius', 'Необязательный радиус поиска точки warp.'],
        ['thenDialogue', 'Необязательный DialogueRef после подхода.']
      ]
    )
  ],
  'Npc.ApproachPlayer': [
    signature(
      'Npc.ApproachPlayer(npc: NpcRef, stopDistance?: number, speed?: number, repathTicks?: int, timeoutTicks?: int, lookAtPlayer?: bool, warpWhenUnseen?: bool, warpDistance?: number, warpSearchRadius?: int, thenDialogue?: DialogueRef) -> Effect',
      'Alias для Npc.MoveToPlayer. Подводит NPC к игроку.',
      [
        ['npc', 'NPC, который идет к игроку.'],
        ['stopDistance', 'Дистанция остановки около игрока.'],
        ['speed', 'Скорость движения.'],
        ['repathTicks', 'Интервал пересчета пути.'],
        ['timeoutTicks', 'Timeout подхода.'],
        ['lookAtPlayer', 'Смотреть на игрока во время подхода.'],
        ['warpWhenUnseen', 'Варпнуть NPC, если его не видно.'],
        ['warpDistance', 'Дистанция для warp fallback.'],
        ['warpSearchRadius', 'Радиус поиска точки warp.'],
        ['thenDialogue', 'Диалог после подхода.']
      ]
    )
  ],
  'Npc.Path': [
    signature(
      'Npc.Path(npc: NpcRef, points: Vector3[], speed?: number, completionDistance?: number, openDoors?: bool, thenDialogue?: DialogueRef) -> Effect',
      'Ведет NPC по маршруту из нескольких точек. Используй для постановочных проходов.',
      [
        ['npc', 'NPC, который идет по маршруту.'],
        ['points', 'Массив точек Vector3.'],
        ['speed', 'Скорость движения.'],
        ['completionDistance', 'Дистанция завершения каждой точки.'],
        ['openDoors', 'Открывать двери по пути.'],
        ['thenDialogue', 'Диалог после завершения маршрута.']
      ]
    )
  ],
  'Npc.HoldItem': [
    signature(
      'Npc.HoldItem(npc: NpcRef, item: ItemRef, count?: int = 1, hand?: HandRef) -> Effect',
      'Кладет предмет в руку NPC. Используй для визуального действия в сцене или диалоге.',
      [
        ['npc', 'NPC, которому нужно выдать предмет в руку.'],
        ['item', 'ItemRef, например Items.SparkGrass.'],
        ['count', 'Количество предметов.'],
        ['hand', 'Рука, обычно Hand.Main.']
      ]
    )
  ],
  'Npc.PlayAnimation': [
    signature(
      'Npc.PlayAnimation(npc: NpcRef, animation: AnimationRef, layer?: LayerRef, fadeInTicks?: int, fadeOutTicks?: int, durationTicks?: int) -> Effect',
      'Запускает анимацию на NPC. Обычно layer: Layer.Overlay нужен для поверхностных жестов.',
      [
        ['npc', 'NPC, на котором запускается анимация.'],
        ['animation', 'AnimationRef, например Animations.NpcOfferItemHold.'],
        ['layer', 'Слой анимации, например Layer.Overlay.'],
        ['fadeInTicks', 'Необязательный fade-in.'],
        ['fadeOutTicks', 'Необязательный fade-out после stop/duration.'],
        ['durationTicks', 'Необязательная длительность проигрывания.']
      ]
    )
  ],
  'Npc.StopAnimation': [
    signature(
      'Npc.StopAnimation(npc: NpcRef, layer?: LayerRef, fadeOutTicks?: int) -> Effect',
      'Останавливает body animation layer у NPC.',
      [
        ['npc', 'NPC, на котором останавливается анимация. Обязательный аргумент.'],
        ['layer', 'Необязательный слой, например Layer.Overlay.'],
        ['fadeOutTicks', 'Необязательный fade-out.']
      ]
    )
  ],
  'Npc.PlayFaceAnimation': [
    signature(
      'Npc.PlayFaceAnimation(npc: NpcRef, animation: FaceAnimationRef, layer?: FaceLayerRef, fadeInTicks?: int, fadeOutTicks?: int, durationTicks?: int) -> Effect',
      'Запускает face animation на конкретном NPC.',
      [
        ['npc', 'NPC, на котором запускается face animation. Обязательный аргумент.'],
        ['animation', 'FaceAnimationRef. Обязательный аргумент.'],
        ['layer', 'Необязательный face layer: FaceLayer.Base, FaceLayer.Overlay или FaceLayer.Detail.'],
        ['fadeInTicks', 'Необязательный fade-in.'],
        ['fadeOutTicks', 'Необязательный fade-out после stop/duration.'],
        ['durationTicks', 'Необязательная длительность проигрывания.']
      ]
    )
  ],
  'Npc.StopFaceAnimation': [
    signature(
      'Npc.StopFaceAnimation(npc: NpcRef, layer?: FaceLayerRef, fadeOutTicks?: int) -> Effect',
      'Останавливает face animation layer у NPC.',
      [
        ['npc', 'NPC, на котором останавливается face animation. Обязательный аргумент.'],
        ['layer', 'Необязательный face layer.'],
        ['fadeOutTicks', 'Необязательный fade-out.']
      ]
    )
  ],
  'Npc.UseBlock': [
    signature(
      'Npc.UseBlock(npc: NpcRef, position: Vector3, action?: string) -> Effect',
      'Выполняет interaction с блоком от имени NPC.',
      [
        ['npc', 'NPC actor. Обязательный аргумент.'],
        ['position', 'Координаты блока Vector3. Обязательный аргумент.'],
        ['action', 'Необязательный режим/action, например "use". Также принимается named `mode`.']
      ]
    )
  ],
  'Npc.OpenDoor': [
    signature(
      'Npc.OpenDoor(npc: NpcRef, position: Vector3) -> Effect',
      'Открывает дверь или совместимый блок от имени NPC.',
      [
        ['npc', 'NPC actor. Обязательный аргумент.'],
        ['position', 'Координаты двери/блока Vector3. Обязательный аргумент.']
      ]
    )
  ],
  'Npc.Rotate': [
    signature(
      'Npc.Rotate(npc: NpcRef, rotation: Vector3 | yaw, speed?: number, lock?: bool, pitch?: number, roll?: number) -> Effect',
      'Поворачивает NPC в любой момент. Vector3 означает x=pitch, y=yaw, z=roll; speed включает плавный поворот: голова сначала, тело следом.',
      [
        ['npc', 'NPC, которого нужно повернуть. Обязательный аргумент.'],
        ['rotation', 'Обязательный Vector3(pitch, yaw, roll) или yaw number. Можно передать переменную.'],
        ['speed', 'Необязательная скорость в degrees per tick. Если не указать или 0, поворот мгновенный.'],
        ['lock', 'Необязательно: оставить rotation lock после поворота.'],
        ['pitch', 'Необязательный pitch, если используешь yaw/named args вместо Vector3.'],
        ['roll', 'Необязательный roll: сохраняется в IR; entity сейчас визуально применяет pitch/yaw.']
      ]
    )
  ],
  'This.Rotate': [
    signature(
      'This.Rotate(rotation: Vector3 | yaw, speed?: number, lock?: bool, pitch?: number, roll?: number) -> Effect',
      'Поворачивает текущего NPC action actor. Vector3: x=pitch, y=yaw, z=roll.',
      [
        ['rotation', 'Обязательный Vector3(pitch, yaw, roll) или yaw number. Можно передать переменную.'],
        ['speed', 'Необязательная скорость в degrees per tick.'],
        ['lock', 'Необязательно: оставить rotation lock после поворота.'],
        ['pitch', 'Необязательный pitch при named/yaw форме.'],
        ['roll', 'Необязательный roll, сохраняется в IR.']
      ]
    )
  ],
  'Navigation.SetTarget': [
    signature(
      'Navigation.SetTarget(targetId: string, x: int, y: int, z: int, label?: string, dimensionId?: DimensionRef | string, profile?: string) -> Effect',
      'Ставит GPS/navigation цель игроку. Используй для квестовой метки, маршрута или подсказки куда идти.',
      [
        ['targetId', 'Стабильный id цели, например "quest.main_gate".'],
        ['x', 'X координата цели.'],
        ['y', 'Y координата цели.'],
        ['z', 'Z координата цели.'],
        ['label', 'Текст цели в UI.'],
        ['dimensionId', 'Измерение цели, например "minecraft:overworld".'],
        ['profile', 'Профиль маршрута, например "prefer_roads".']
      ]
    )
  ],
  'World.SetNavigationTarget': [
    signature(
      'World.SetNavigationTarget(targetId: string, x: int, y: int, z: int, label?: string) -> Effect',
      'Короткая world-command форма для установки navigation цели.',
      [
        ['targetId', 'Стабильный id цели.'],
        ['x', 'X координата цели.'],
        ['y', 'Y координата цели.'],
        ['z', 'Z координата цели.'],
        ['label', 'Текст цели в UI.']
      ]
    )
  ],
  'Quest.Update': [
    signature(
      'Quest.Update(quest: QuestRef, status?: QuestStatus, title?: string, body?: string, stage?: QuestStageRef, showNotification?: bool) -> Effect',
      'Обновляет состояние и текст квеста. Используй для смены статуса, этапа и уведомления игрока.',
      [
        ['quest', 'QuestRef/id квеста.'],
        ['status', 'Статус, например QuestStatus.Active или QuestStatus.Completed.'],
        ['title', 'Заголовок квеста.'],
        ['body', 'Описание квеста.'],
        ['stage', 'Текущий этап квеста.'],
        ['showNotification', 'Показать уведомление игроку.']
      ]
    )
  ]
};

Object.assign(SIGNATURES, {
  Environment: [
    signature(
      'Environment(id: EnvironmentId | string, dimension: DimensionRef | string, states: EnvironmentStateDef[]) -> EnvironmentResourceDef',
      'Builds an environment resource made of named block-change states.',
      [
        ['id', 'Stable environment resource id.'],
        ['dimension', 'Dimension/world id for the environment.'],
        ['states', 'Environment state definitions.']
      ]
    )
  ],
  Form: [
    signature(
      'Form(id: NpcFormId | string, skin?: SkinDef, model?: string, dimensions?: DimensionsDef, bodyScale?: number, parts?: PartDef[], anchors?: AnchorDef[], animationGraph?: string, faceProfile?: string, portrait?: VisualDef, tags?: string[]) -> NpcFormResourceDef',
      'Builds an authored NPC form resource.',
      [
        ['id', 'Stable NPC form resource id.'],
        ['skin', 'Skin visual used by this form.'],
        ['model', 'Runtime model id.'],
        ['dimensions', 'Collision and eye dimensions.'],
        ['bodyScale', 'Whole-body scale.'],
        ['parts', 'Authored model parts.'],
        ['anchors', 'Named attachment anchors.'],
        ['animationGraph', 'Animation graph/profile id.'],
        ['faceProfile', 'Face profile/rig id.'],
        ['portrait', 'Portrait visual.'],
        ['tags', 'Authoring/runtime tags.']
      ]
    )
  ],
  Dimensions: [
    signature(
      'Dimensions(width: number, height: number, eyeHeight: number) -> DimensionsDef',
      'Defines NPC form collision and eye dimensions.',
      [
        ['width', 'Entity collision width.'],
        ['height', 'Entity collision height.'],
        ['eyeHeight', 'Entity eye height.']
      ]
    )
  ],
  Part: [
    signature(
      'Part(id: string, scale?: number, scaleX?: number, scaleY?: number, scaleZ?: number, pivotX?: number, pivotY?: number, pivotZ?: number, rotationX?: number, rotationY?: number, rotationZ?: number) -> NpcFormPartDef',
      'Defines one authored NPC form part.',
      [
        ['id', 'Part id.'],
        ['scale', 'Uniform scale.'],
        ['scaleX', 'X scale.'],
        ['scaleY', 'Y scale.'],
        ['scaleZ', 'Z scale.'],
        ['pivotX', 'Pivot X.'],
        ['pivotY', 'Pivot Y.'],
        ['pivotZ', 'Pivot Z.'],
        ['rotationX', 'Rotation X.'],
        ['rotationY', 'Rotation Y.'],
        ['rotationZ', 'Rotation Z.']
      ]
    ),
    ...(SIGNATURES.Part || [])
  ],
  Anchor: [
    signature(
      'Anchor(id: string, x: number, y: number, z: number) -> NpcFormAnchorDef',
      'Defines one named NPC form attachment anchor.',
      [
        ['id', 'Anchor id.'],
        ['x', 'Anchor X.'],
        ['y', 'Anchor Y.'],
        ['z', 'Anchor Z.']
      ]
    )
  ],
  DefaultForm: [
    signature(
      'DefaultForm(form: NpcFormId | string) -> NpcDefaultFormRef',
      'References an NPC form from an Npc(...) definition.',
      [
        ['form', 'NPC form resource id.']
      ]
    )
  ],
  BlockPosition: [
    signature(
      'new BlockPosition(x: int | string, y: int | string, z: int | string) -> BlockPosition',
      'Создает block position для dialogue-side Npc.Interact и других block-target команд.',
      [
        ['x', 'Block X.'],
        ['y', 'Block Y.'],
        ['z', 'Block Z.']
      ]
    )
  ],
  Position: [
    signature(
      'Position(x: int, y: int, z: int) -> PositionDef',
      'Builds an environment block position.',
      [
        ['x', 'Block X.'],
        ['y', 'Block Y.'],
        ['z', 'Block Z.']
      ]
    )
  ],
  Rotation: [
    signature(
      'Frame(tick).Rotation(z: number) -> KeyframeDef',
      'Задает Z-rotation для face/body frame transform.',
      [
        ['z', 'Z rotation value.']
      ]
    ),
    signature(
      'Frame(tick).Rotation(x: 0, y: 0, z: number) -> KeyframeDef',
      'Полная форма rotation modifier. Для face frames x/y должны оставаться 0.',
      [
        ['x', 'X rotation; for face frames keep 0.'],
        ['y', 'Y rotation; for face frames keep 0.'],
        ['z', 'Z rotation value.']
      ]
    )
  ],
  Scale: [
    signature(
      'Frame(tick).Scale(x: number, y: number, z: number) -> KeyframeDef',
      'Задает scale-вектор для frame transform.',
      [
        ['x', 'X scale.'],
        ['y', 'Y scale.'],
        ['z', 'Z scale.']
      ]
    )
  ],
  State: [
    signature(
      'State(id: string, changes: EnvironmentChangeDef[]) -> EnvironmentStateDef',
      'Builds one environment-local state.',
      [
        ['id', 'Environment-local state id.'],
        ['changes', 'Block changes applied by this state.']
      ]
    ),
    ...(SIGNATURES.State || [])
  ],
  SetBlock: [
    signature(
      'SetBlock(position: PositionDef, blockId: string, properties?: PropertiesDef, nbt?: string) -> EnvironmentChangeDef',
      'Sets one block in an environment state.',
      [
        ['position', 'Block position.'],
        ['blockId', 'Minecraft block id.'],
        ['properties', 'Optional block state properties.'],
        ['nbt', 'Optional block NBT.']
      ]
    )
  ],
  ClearBlock: [
    signature(
      'ClearBlock(position: PositionDef) -> EnvironmentChangeDef',
      'Clears one block in an environment state.',
      [
        ['position', 'Block position.']
      ]
    )
  ],
  Properties: [
    signature(
      'Properties(properties: PropertyDef...) -> PropertiesDef',
      'Builds block-state properties for SetBlock.',
      [
        ['properties', 'Property(...) entries.']
      ]
    )
  ],
  Property: [
    signature(
      'Property(name: string, value: string) -> PropertyDef',
      'Builds one block-state property.',
      [
        ['name', 'Property name.'],
        ['value', 'Property value.']
      ]
    )
  ],
  'Story.Set': [
    signature(
      'Story.Set(stateKey: string, value: any, next?: string) -> Effect',
      'Writes story state from linear story startup code.',
      [
        ['stateKey', 'Story state key.'],
        ['value', 'Value to write.'],
        ['next', 'Next local story node id when used in graph contexts.']
      ]
    )
  ],
  'Story.Load': [
    signature(
      'Story.Load(storyId: StoryId | string, executionId?: string, next?: string) -> Effect',
      'Starts another StoryResource through the story runtime.',
      [
        ['storyId', 'Story resource id to start.'],
        ['executionId', 'Optional explicit child execution id.'],
        ['next', 'Next local story node id.']
      ]
    )
  ],
  'Dialogue.Load': [
    signature(
      'Dialogue.Load(dialogueId: DialogueId | string, audienceId?: string, output?: string, next?: string) -> Effect',
      'Loads a modular dialogue through the external action intrinsic.',
      [
        ['dialogueId', 'Dialogue resource id.'],
        ['audienceId', 'Dialogue audience id.'],
        ['output', 'Output binding slot.'],
        ['next', 'Next local story node id.']
      ]
    )
  ],
  'Dialogue.Start': [
    signature(
      'Dialogue.Start(dialogue: DialogueId | string, sessionId?: string, audienceId?: string) -> Effect',
      'Запускает DialogueResource. В TriggerResource callbacks этот action обслуживается Echo Dialogue trigger adapter.',
      [
        ['dialogue', 'Dialogue resource id or field.'],
        ['sessionId', 'Optional explicit dialogue session id.'],
        ['audienceId', 'Optional explicit dialogue audience id.']
      ]
    )
  ],
  'Environment.Set': [
    signature(
      'Environment.Set(environmentId: EnvironmentId | string, stateId: string, next?: string) -> Effect',
      'Applies a named state from an environment resource.',
      [
        ['environmentId', 'Environment resource id.'],
        ['stateId', 'Environment-local state id.'],
        ['next', 'Next local story node id.']
      ]
    )
  ],
  'Screen.SetBlack': [
    signature(
      'Screen.SetBlack(canvasGroup: string, alpha?: number, time?: int, durationMillis?: int, output?: string, next?: string) -> Effect',
      'Drives the black-screen overlay.',
      [
        ['canvasGroup', 'Screen canvas group id.'],
        ['alpha', 'Target alpha.'],
        ['time', 'Transition duration.'],
        ['durationMillis', 'Transition duration in milliseconds.'],
        ['output', 'Output binding slot.'],
        ['next', 'Next local story node id.']
      ]
    )
  ],
  'Dialogue.GoTo': [signature('Dialogue.GoTo(node: MethodRef) -> DialogueTransition', 'Transitions the current dialogue node to another node method.', [['node', 'Target dialogue node method.']])],
  GoTo: [signature('GoTo(node: MethodRef) -> DialogueTransition', 'Переходит к high-level Node по символу; rename и Go to Definition сохраняют ссылку типизированной.', [['node', 'Имя целевого Node в том же DialogueResource.']])],
  Formerly: [signature('Formerly(oldId: string, node: MethodRef) -> void', 'Сопоставляет прежний сохраненный node id с новым high-level Node.', [['oldId', 'Старый нормализованный node id.'], ['node', 'Текущий Node-символ.']])],
  NodeId: [signature('NodeId(id: string) -> void', 'Marks or documents a dialogue node id; the compiler treats it as a no-op marker.', [['id', 'Dialogue node id.']])],
  'Cutscene.Start': [signature('Cutscene.Start(cutscene: CutsceneRef | string) -> DialogueCommand', 'Starts a cutscene command from dialogue.', [['cutscene', 'Cutscene resource id or field.']])],
  'Cutscene.Play': [signature('Cutscene.Play(cutscene: CutsceneRef | string) -> DialogueCommand', 'Plays a cutscene command from dialogue.', [['cutscene', 'Cutscene resource id or field.']])],
  'Cutscene.Wait': [signature('Cutscene.Wait(cutscene: CutsceneRef | string) -> DialogueCommand', 'Waits for a cutscene command from dialogue.', [['cutscene', 'Cutscene resource id or field.']])],
  'Cutscene.WaitPrepared': [signature('Cutscene.WaitPrepared(cutscene: CutsceneRef | string) -> DialogueCommand', 'Waits for a prepared cutscene stage.', [['cutscene', 'Cutscene resource id or field.']])],
  'Cutscene.Stop': [signature('Cutscene.Stop(cutscene: CutsceneRef | string) -> DialogueCommand', 'Stops a cutscene command from dialogue.', [['cutscene', 'Cutscene resource id or field.']])],
  'Player.SpawnAt': [
    signature(
      'Player.SpawnAt(x: number, y: number, z: number, yaw?: number, pitch?: number, output?: string, next?: string) -> Effect',
      'Sets or moves the player spawn/action position.',
      [
        ['x', 'Target X.'],
        ['y', 'Target Y.'],
        ['z', 'Target Z.'],
        ['yaw', 'Target yaw.'],
        ['pitch', 'Target pitch.'],
        ['output', 'Output binding slot.'],
        ['next', 'Next local story node id.']
      ]
    )
  ],
  'Player.SetHotbarItem': [
    signature(
      'Player.SetHotbarItem(slot: int, itemId: string, count?: int, replace?: bool, next?: string) -> Effect',
      'Writes one player hotbar slot.',
      [
        ['slot', 'Hotbar slot index.'],
        ['itemId', 'Minecraft item id.'],
        ['count', 'Stack count.'],
        ['replace', 'Whether to replace the existing stack.'],
        ['next', 'Next local story node id.']
      ]
    )
  ],
  'Player.SelectHotbarSlot': [
    signature(
      'Player.SelectHotbarSlot(slot: int, next?: string) -> Effect',
      'Selects the active hotbar slot.',
      [
        ['slot', 'Hotbar slot index.'],
        ['next', 'Next local story node id.']
      ]
    )
  ],
  'Player.SitAt': [
    signature(
      'Player.SitAt(x: number, y: number, z: number, timeoutTicks?: int, next?: string) -> Effect',
      'Seats the player at a position.',
      [
        ['x', 'Seat X.'],
        ['y', 'Seat Y.'],
        ['z', 'Seat Z.'],
        ['timeoutTicks', 'Timeout in ticks.'],
        ['next', 'Next local story node id.']
      ]
    )
  ],
  'Blackspace.Create': [
    signature(
      'Blackspace.Create(instanceId?: string, id?: string, playerName?: string, playerUuid?: string, output?: string, next?: string) -> Effect',
      'Creates or binds a blackspace instance.',
      [
        ['instanceId', 'Blackspace instance id.'],
        ['id', 'Alias for instanceId.'],
        ['playerName', 'Player name.'],
        ['playerUuid', 'Player UUID.'],
        ['output', 'Output binding slot.'],
        ['next', 'Next local story node id.']
      ]
    )
  ],
  'Player.Teleport': [
    signature(
      'Player.Teleport(instanceId?: string, target?: string, playerName?: string, playerUuid?: string, output?: string, next?: string) -> Effect',
      'Teleports the player through blackspace routing.',
      [
        ['instanceId', 'Blackspace instance id.'],
        ['target', 'Teleport target id.'],
        ['playerName', 'Player name.'],
        ['playerUuid', 'Player UUID.'],
        ['output', 'Output binding slot.'],
        ['next', 'Next local story node id.']
      ]
    )
  ],
  'Player.Rotate': [signature('Player.Rotate(yaw: number, pitch: number) -> Effect', 'Rotates the scripted player actor.', [['yaw', 'Target yaw.'], ['pitch', 'Target pitch.']])],
  'Player.LookAt': [signature('Player.LookAt(x: number, y: number, z: number) -> Effect', 'Makes the player look at a world position.', [['x', 'Target X.'], ['y', 'Target Y.'], ['z', 'Target Z.']])],
  'Player.Move': [signature('Player.Move(x: number, y: number, z: number, dimension?: string) -> Effect', 'Moves the scripted player actor to a world position.', [['x', 'Target X.'], ['y', 'Target Y.'], ['z', 'Target Z.'], ['dimension', 'Dimension id.']])],
  'Player.Stop': [signature('Player.Stop() -> Effect', 'Stops the current scripted player actor command.', [])],
  'Player.Interact': [signature('Player.Interact(x: number, y: number, z: number, face?: string, hand?: string, maxDistance?: number) -> Effect', 'Runs a scripted player block interaction.', [['x', 'Target X.'], ['y', 'Target Y.'], ['z', 'Target Z.'], ['face', 'Block face.'], ['hand', 'Interaction hand.'], ['maxDistance', 'Maximum interaction distance.']])],
  'Player.StandUp': [signature('Player.StandUp() -> Effect', 'Releases the player from scripted sitting.', [])],
  'Player.StopUsingItem': [signature('Player.StopUsingItem() -> Effect', 'Stops the player using an item.', [])],
  'Player.IsMoving': [signature('Player.IsMoving() -> Predicate', 'Checks whether the scripted player actor is moving.', [])],
  'Player.IsSitting': [signature('Player.IsSitting() -> Predicate', 'Checks whether the scripted player actor is sitting.', [])],
  'Player.SelectedHotbarSlot': [signature('Player.SelectedHotbarSlot() -> Predicate', 'Reads or checks the selected hotbar slot.', [])],
  'Player.IsUsingItem': [signature('Player.IsUsingItem() -> Predicate', 'Checks whether the player is using an item.', [])],
  'Player.SetFatigue': [signature('Player.SetFatigue(value: int) -> Effect', 'Sets player fatigue.', [['value', 'Non-negative fatigue value.']])],
  SetFatigue: [signature('SetFatigue(value: int) -> Effect', 'Sets player fatigue.', [['value', 'Non-negative fatigue value.']])],
  'Player.AddFatigue': [signature('Player.AddFatigue(value: int) -> Effect', 'Adds player fatigue.', [['value', 'Non-negative fatigue delta.']])],
  AddFatigue: [signature('AddFatigue(value: int) -> Effect', 'Adds player fatigue.', [['value', 'Non-negative fatigue delta.']])],
  'Player.ReduceFatigue': [signature('Player.ReduceFatigue(value: int) -> Effect', 'Reduces player fatigue.', [['value', 'Non-negative fatigue delta.']])],
  ReduceFatigue: [signature('ReduceFatigue(value: int) -> Effect', 'Reduces player fatigue.', [['value', 'Non-negative fatigue delta.']])],
  'Player.RestoreFatigue': [signature('Player.RestoreFatigue() -> Effect', 'Restores player fatigue.', [])],
  RestoreFatigue: [signature('RestoreFatigue() -> Effect', 'Restores player fatigue.', [])],
  'Npc.SpawnAt': [
    signature(
      'Npc.SpawnAt(npc: NpcRef, x: number, y: number, z: number, yaw?: number, pitch?: number, worldId?: string) -> Effect',
      'Spawns an NPC through the new external action intrinsic.',
      [
        ['npc', 'NPC resource/reference.'],
        ['x', 'Spawn X.'],
        ['y', 'Spawn Y.'],
        ['z', 'Spawn Z.'],
        ['yaw', 'Spawn yaw.'],
        ['pitch', 'Spawn pitch.'],
        ['worldId', 'Spawn world/dimension id.']
      ]
    )
  ],
  'Npc.SleepAt': [
    signature(
      'Npc.SleepAt(npc: NpcRef, bedX: number, bedY: number, bedZ: number, expectedBlock?: string, worldId?: string) -> Effect',
      'Puts an NPC to sleep at a bed/block position.',
      [
        ['npc', 'NPC resource/reference.'],
        ['bedX', 'Bed X.'],
        ['bedY', 'Bed Y.'],
        ['bedZ', 'Bed Z.'],
        ['expectedBlock', 'Expected bed/block id.'],
        ['worldId', 'Bed world/dimension id.']
      ]
    )
  ],
  'Npc.WakeUp': [
    signature(
      'Npc.WakeUp(npc: NpcRef) -> Effect',
      'Wakes an NPC from a SleepAt action.',
      [
        ['npc', 'NPC resource/reference.']
      ]
    )
  ],
  'Npc.SetForm': [
    signature(
      'Npc.SetForm(npc: NpcRef, form: NpcFormRef | string) -> DialogueCommand',
      'Changes the active NPC form from a dialogue command.',
      [
        ['npc', 'NPC resource/reference.'],
        ['form', 'NPC form resource id or field.']
      ]
    )
  ],
  'Npc.Interact': [
    signature(
      'Npc.Interact(npc: NpcRef, x: number, y: number, z: number, face?: string, hand?: string, approach?: string, dimension?: string, output?: string, next?: string) -> Effect',
      'Запускает durable NPC interaction с точной block target позицией.',
      [
        ['npc', 'NPC resource/reference.'],
        ['x', 'Interaction X.'],
        ['y', 'Interaction Y.'],
        ['z', 'Interaction Z.'],
        ['face', 'Block face; AUTO by default.'],
        ['hand', 'Interaction hand; MAIN_HAND by default.'],
        ['approach', 'Approach policy; AUTO by default.'],
        ['dimension', 'Interaction dimension; minecraft:overworld by default.'],
        ['output', 'Output binding slot.'],
        ['next', 'Next local story node id.']
      ]
    ),
    signature(
      'Npc.Interact(npc: NpcRef, position: BlockPosition, face?: string, hand?: string, approach?: string, dimension?: string, output?: string, next?: string) -> Effect',
      'Dialogue-side форма с BlockPosition field/new BlockPosition(...): adapter разворачивает ее в x/y/z.',
      [
        ['npc', 'NPC resource/reference.'],
        ['position', 'BlockPosition field или new BlockPosition(x, y, z).'],
        ['face', 'Block face; AUTO by default.'],
        ['hand', 'Interaction hand; MAIN_HAND by default.'],
        ['approach', 'Approach policy; AUTO by default.'],
        ['dimension', 'Interaction dimension; minecraft:overworld by default.'],
        ['output', 'Output binding slot.'],
        ['next', 'Next local story node id.']
      ]
    )
  ],
  'Npc.StopInteraction': [
    signature(
      'Npc.StopInteraction(npc: NpcRef, output?: string, next?: string) -> Effect',
      'Stops an active NPC interaction.',
      [
        ['npc', 'NPC resource/reference.'],
        ['output', 'Output binding slot.'],
        ['next', 'Next local story node id.']
      ]
    )
  ],
  'Npc.StandUp': [
    signature(
      'Npc.StandUp(npc: NpcRef, output?: string, next?: string) -> Effect',
      'Снимает NPC с interaction seat anchor, созданного Npc.Interact, и освобождает occupancy.',
      [
        ['npc', 'NPC actor/resource reference.'],
        ['output', 'Output binding slot.'],
        ['next', 'Next local story node id.']
      ]
    )
  ],
  'Npc.Stop': [
    signature(
      'Npc.Stop(npc: NpcRef, output?: string, next?: string) -> Effect',
      'Stops the active NPC action.',
      [
        ['npc', 'NPC resource/reference.'],
        ['output', 'Output binding slot.'],
        ['next', 'Next local story node id.']
      ]
    )
  ],
  'Sleep.Arm': [signature('Sleep.Arm(scenario?: string, player?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect', 'Arms a sleep scenario.', [['scenario', 'Sleep scenario id.'], ['player', 'Player context.'], ['owner', 'Owner id.'], ['command', 'Command id.'], ['output', 'Output binding slot.'], ['next', 'Next local story node id.']])],
  'Sleep.Disarm': [signature('Sleep.Disarm(player?: string, scenario?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect', 'Disarms the active sleep scenario.', [['player', 'Player context.'], ['scenario', 'Sleep scenario id.'], ['owner', 'Owner id.'], ['command', 'Command id.'], ['output', 'Output binding slot.'], ['next', 'Next local story node id.']])],
  'Sleep.CloseEyes': [signature('Sleep.CloseEyes(duration?: int, player?: string, scenario?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect', 'Closes the sleep overlay eyes.', [['duration', 'Close duration.'], ['player', 'Player context.'], ['scenario', 'Sleep scenario id.'], ['owner', 'Owner id.'], ['command', 'Command id.'], ['output', 'Output binding slot.'], ['next', 'Next local story node id.']])],
  'Sleep.WaitUntilClosed': [signature('Sleep.WaitUntilClosed(player?: string, scenario?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect', 'Waits until eyes are closed.', [['player', 'Player context.'], ['scenario', 'Sleep scenario id.'], ['owner', 'Owner id.'], ['command', 'Command id.'], ['output', 'Output binding slot.'], ['next', 'Next local story node id.']])],
  'Sleep.EnterDream': [signature('Sleep.EnterDream(target?: string, blackspaceInstanceId?: string, blackspaceId?: string, dimension?: string, worldId?: string, x?: number, y?: number, z?: number, yaw?: number, pitch?: number, output?: string, next?: string) -> Effect', 'Enters a sleep dream target.', [['target', 'Dream target id.'], ['blackspaceInstanceId', 'Blackspace instance id.'], ['blackspaceId', 'Blackspace definition id.'], ['dimension', 'Target dimension.'], ['worldId', 'Target world/dimension id.'], ['x', 'Target X.'], ['y', 'Target Y.'], ['z', 'Target Z.'], ['yaw', 'Target yaw.'], ['pitch', 'Target pitch.'], ['output', 'Output binding slot.'], ['next', 'Next local story node id.']])],
  'Sleep.OpenEyes': [signature('Sleep.OpenEyes(duration?: int, sleepyBlink?: bool, showHud?: bool, player?: string, scenario?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect', 'Opens the sleep overlay eyes.', [['duration', 'Open duration.'], ['sleepyBlink', 'Use sleepy blink.'], ['showHud', 'Restore HUD.'], ['player', 'Player context.'], ['scenario', 'Sleep scenario id.'], ['owner', 'Owner id.'], ['command', 'Command id.'], ['output', 'Output binding slot.'], ['next', 'Next local story node id.']])],
  'Sleep.WaitUntilOpen': [signature('Sleep.WaitUntilOpen(player?: string, scenario?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect', 'Waits until eyes are open.', [['player', 'Player context.'], ['scenario', 'Sleep scenario id.'], ['owner', 'Owner id.'], ['command', 'Command id.'], ['output', 'Output binding slot.'], ['next', 'Next local story node id.']])],
  'Sleep.Wait': [signature('Sleep.Wait(seconds?: number, duration?: int, ticks?: int, player?: string, scenario?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect', 'Waits within the sleep flow.', [['seconds', 'Wait seconds.'], ['duration', 'Wait duration.'], ['ticks', 'Wait ticks.'], ['player', 'Player context.'], ['scenario', 'Sleep scenario id.'], ['owner', 'Owner id.'], ['command', 'Command id.'], ['output', 'Output binding slot.'], ['next', 'Next local story node id.']])],
  'Sleep.WaitSeconds': [signature('Sleep.WaitSeconds(seconds?: number, duration?: int, ticks?: int, player?: string, scenario?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect', 'Waits within the sleep flow in seconds.', [['seconds', 'Wait seconds.'], ['duration', 'Wait duration.'], ['ticks', 'Wait ticks.'], ['player', 'Player context.'], ['scenario', 'Sleep scenario id.'], ['owner', 'Owner id.'], ['command', 'Command id.'], ['output', 'Output binding slot.'], ['next', 'Next local story node id.']])],
  'Sleep.Blink': [signature('Sleep.Blink(count: int, duration?: int, closedWait?: number, openWait?: number, sleepyBlink?: bool, showHud?: bool, player?: string, scenario?: string, owner?: string, command?: string) inside OnStart', 'Linear OnStart helper that expands into close/wait/open sleep-eye actions.', [['count', 'Blink count.'], ['duration', 'Close/open duration.'], ['closedWait', 'Seconds to wait while closed.'], ['openWait', 'Seconds to wait while open.'], ['sleepyBlink', 'Use sleepy blink.'], ['showHud', 'Restore HUD.'], ['player', 'Player context.'], ['scenario', 'Sleep scenario id.'], ['owner', 'Owner id.'], ['command', 'Command id.']])],
  'Sleep.ReturnToBed': [signature('Sleep.ReturnToBed(mode?: string, player?: string, scenario?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect', 'Returns the player to bed.', [['mode', 'Return mode.'], ['player', 'Player context.'], ['scenario', 'Sleep scenario id.'], ['owner', 'Owner id.'], ['command', 'Command id.'], ['output', 'Output binding slot.'], ['next', 'Next local story node id.']])],
  'Sleep.CompleteNight': [signature('Sleep.CompleteNight(player?: string, scenario?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect', 'Completes the night sleep flow.', [['player', 'Player context.'], ['scenario', 'Sleep scenario id.'], ['owner', 'Owner id.'], ['command', 'Command id.'], ['output', 'Output binding slot.'], ['next', 'Next local story node id.']])],
  'Sleep.Complete': [signature('Sleep.Complete(player?: string, scenario?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect', 'Completes the sleep flow.', [['player', 'Player context.'], ['scenario', 'Sleep scenario id.'], ['owner', 'Owner id.'], ['command', 'Command id.'], ['output', 'Output binding slot.'], ['next', 'Next local story node id.']])],
  'Sleep.Cancel': [signature('Sleep.Cancel(player?: string, scenario?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect', 'Cancels the sleep flow.', [['player', 'Player context.'], ['scenario', 'Sleep scenario id.'], ['owner', 'Owner id.'], ['command', 'Command id.'], ['output', 'Output binding slot.'], ['next', 'Next local story node id.']])],
  'Sleep.IsActive': [signature('Sleep.IsActive(output?: string, player?: string, scenario?: string, owner?: string, command?: string, next?: string) -> Effect', 'Writes whether sleep is active.', [['output', 'Output binding slot.'], ['player', 'Player context.'], ['scenario', 'Sleep scenario id.'], ['owner', 'Owner id.'], ['command', 'Command id.'], ['next', 'Next local story node id.']])],
  'Sleep.IsArmed': [signature('Sleep.IsArmed(output?: string, player?: string, scenario?: string, owner?: string, command?: string, next?: string) -> Effect', 'Writes whether sleep is armed.', [['output', 'Output binding slot.'], ['player', 'Player context.'], ['scenario', 'Sleep scenario id.'], ['owner', 'Owner id.'], ['command', 'Command id.'], ['next', 'Next local story node id.']])],
  'Sleep.IsEyesClosed': [signature('Sleep.IsEyesClosed(output?: string, player?: string, scenario?: string, owner?: string, command?: string, next?: string) -> Effect', 'Writes whether sleep eyes are closed.', [['output', 'Output binding slot.'], ['player', 'Player context.'], ['scenario', 'Sleep scenario id.'], ['owner', 'Owner id.'], ['command', 'Command id.'], ['next', 'Next local story node id.']])]
});

Object.assign(SIGNATURES, {
  PlayerProfile: [
    signature(
      'PlayerProfile(id: PlayerProfileTemplateId | string, nameMode?: ACCOUNT | CUSTOM, displayName?: string, spark?: int, maxSpark?: int, skinMode?: ACCOUNT | RESOURCE, skinId?: string, skinModel?: CLASSIC | SLIM) -> PlayerProfile',
      'Builds an Echo Content Player profile template payload.',
      [
        ['id', 'Stable player profile template id.'],
        ['nameMode', 'ACCOUNT or CUSTOM.'],
        ['displayName', 'Required when nameMode is CUSTOM.'],
        ['spark', 'Current spark value.'],
        ['maxSpark', 'Maximum spark value.'],
        ['skinMode', 'ACCOUNT or RESOURCE.'],
        ['skinId', 'Resource skin id when skinMode is RESOURCE.'],
        ['skinModel', 'CLASSIC or SLIM.']
      ]
    )
  ],
  Weapon: [
    signature(
      'Weapon(id: EchoItemId | string, name: string, texture: string, description?: string, tooltip?: string[], model?: string, damage?: number, attacksPerSecond?: number, durability?: int, rarity?: COMMON | UNCOMMON | RARE | EPIC) -> WeaponDefinition',
      'Builds an Echo Item Pack weapon definition.',
      [
        ['id', 'Stable weapon id.'],
        ['name', 'Display name.'],
        ['texture', 'Texture resource id.'],
        ['description', 'Description text.'],
        ['tooltip', 'Tooltip lines.'],
        ['model', 'Optional item model.'],
        ['damage', 'Attack damage.'],
        ['attacksPerSecond', 'Attacks per second.'],
        ['durability', 'Durability points.'],
        ['rarity', 'COMMON, UNCOMMON, RARE, or EPIC.']
      ]
    )
  ],
  Id: [
    ...(SIGNATURES.Id || []),
    signature('Id(id: TriggerId | string) -> void', 'Declares the id inside TriggerResource.Init().', [['id', 'Stable trigger id.']])
  ],
  Scope: [signature('Scope(GLOBAL | SERVER | PLAYER | DIALOGUE_SESSION) -> void', 'Sets TriggerResource scope.', [['scope', 'Trigger scope.']])],
  Mode: [signature('Mode(ONCE | REPEAT) -> void', 'Sets TriggerResource mode.', [['mode', 'Trigger mode.']])],
  Subscribe: [signature('Subscribe(selector: TriggerEventSelector) -> void', 'Subscribes a TriggerResource to a routed story event.', [['selector', 'PlayerSleep.* or Player seating selector.']])],
  StartStory: [signature('StartStory(story: StoryId | string) -> void', 'Starts a story when the subscription fires.', [['story', 'Story resource id.']])],
  StartDialogue: [
    signature(
      'StartDialogue(dialogue: DialogueId | string) -> void',
      'Запускает DialogueResource, когда subscription trigger сработает. Сейчас поддерживается для Player seating selectors.',
      [
        ['dialogue', 'Dialogue resource id or DialogueId field.']
      ]
    ),
    signature(
      'StartDialogue(dialogue: DialogueId | string, sessionId?: string, audienceId?: string) -> TriggerCallbackAction',
      'Alias для Dialogue.Start(...) внутри OnTriggerEnter/Exit/Stay callback.',
      [
        ['dialogue', 'Dialogue resource id or field.'],
        ['sessionId', 'Optional explicit dialogue session id.'],
        ['audienceId', 'Optional explicit dialogue audience id.']
      ]
    )
  ],
  DialogueStart: [
    signature(
      'DialogueStart(dialogue: DialogueId | string, sessionId?: string, audienceId?: string) -> TriggerCallbackAction',
      'Alias для Dialogue.Start(...) внутри TriggerResource callback.',
      [
        ['dialogue', 'Dialogue resource id or field.'],
        ['sessionId', 'Optional explicit dialogue session id.'],
        ['audienceId', 'Optional explicit dialogue audience id.']
      ]
    )
  ],
  'Debug.Mark': [
    signature(
      'Debug.Mark(label: string) -> TriggerCallbackAction',
      'Добавляет debug marker action в TriggerResource callback для тестов и диагностики callback-потока.',
      [
        ['label', 'Marker label.']
      ]
    )
  ],
  AllowManualFire: [signature('AllowManualFire() -> void', 'Allows manual trigger firing through runtime tools.', [])],
  Cooldown: [signature('Cooldown(ticks: int | Ticks(int)) -> void', 'Sets trigger cooldown ticks.', [['ticks', 'Cooldown duration.']])],
  Dimension: [signature('Dimension(dimensionId: string) -> void', 'Binds the reactive trigger dimension.', [['dimensionId', 'Minecraft dimension id.']])],
  Bind: [signature('Bind(areaField: Area | AreaShape) -> void', 'Binds the primary reactive area.', [['areaField', 'Area, BoxArea, SphereArea, CylinderArea, CircleArea, or PolygonArea field.']])],
  FireOnlyOnce: [signature('FireOnlyOnce() -> void', 'Allows the trigger to fire only once.', [])],
  FireOnlyOncePerPlayer: [signature('FireOnlyOncePerPlayer() -> void', 'Alias для FireOnlyOnce(): ограничивает TriggerResource одним успешным fire в scope/player-контексте.', [])],
  EnterOnlyOnce: [signature('EnterOnlyOnce() -> void', 'Allows OnPlayerEnter to run only once.', [])],
  ExitOnlyOnce: [signature('ExitOnlyOnce() -> void', 'Allows OnPlayerExit to run only once.', [])],
  StayOnlyOnce: [signature('StayOnlyOnce() -> void', 'Allows OnPlayerStay to run only once.', [])],
  FireCount: [signature('FireCount(count: int) -> void', 'Limits total fire count.', [['count', 'Positive count.']])],
  StayInterval: [signature('StayInterval(ticks: int | Ticks(int)) -> void', 'Sets PlayerStay polling interval.', [['ticks', 'Positive interval.']])],
  StayPolicy: [signature('StayPolicy(SKIP_WHILE_RUNNING | QUEUE_ONE) -> void', 'Sets PlayerStay scheduling policy.', [['policy', 'Stay scheduling policy.']])],
  Ticks: [signature('Ticks(value: int) -> TriggerTickDuration', 'Wraps a tick duration.', [['value', 'Tick count.']])],
  Area: [signature('Area(dimension: string, x: int, y: int, z: int, sx?: int, sy?: int, sz?: int) -> Area', 'Builds a block or sized trigger area.', [['dimension', 'Dimension id.'], ['x', 'Origin X.'], ['y', 'Origin Y.'], ['z', 'Origin Z.'], ['sx', 'Size X.'], ['sy', 'Size Y.'], ['sz', 'Size Z.']])],
  BoxArea: [signature('BoxArea(min: Vector3, max: Vector3) -> AreaShape', 'Builds a box trigger shape.', [['min', 'Minimum corner.'], ['max', 'Maximum corner.']])],
  SphereArea: [signature('SphereArea(center: Vector3, radius: number) -> AreaShape', 'Builds a sphere trigger shape.', [['center', 'Center point.'], ['radius', 'Radius.']])],
  CylinderArea: [signature('CylinderArea(center: Vector3, radius: number, minY?: number, maxY?: number) -> AreaShape', 'Builds a cylinder trigger shape.', [['center', 'Center point.'], ['radius', 'Radius.'], ['minY', 'Minimum Y.'], ['maxY', 'Maximum Y.']])],
  CircleArea: [signature('CircleArea(center: Vector3, radius: number, minY?: number, maxY?: number) -> AreaShape', 'Alias for CylinderArea.', [['center', 'Center point.'], ['radius', 'Radius.'], ['minY', 'Minimum Y.'], ['maxY', 'Maximum Y.']])],
  PolygonArea: [signature('PolygonArea(points: Vector3[], minY?: number, maxY?: number) -> AreaShape', 'Builds a polygon trigger shape.', [['points', 'Vector3 point array.'], ['minY', 'Minimum Y.'], ['maxY', 'Maximum Y.']])],
  Any: [signature('Any(expressions: TriggerExpression...) -> TriggerExpression', 'Matches when any nested expression matches.', [['expressions', 'Nested trigger expressions.']])],
  All: [signature('All(expressions: TriggerExpression...) -> TriggerExpression', 'Matches when all nested expressions match.', [['expressions', 'Nested trigger expressions.']])],
  Not: [signature('Not(expression: TriggerExpression) -> TriggerExpression', 'Negates a trigger expression.', [['expression', 'Nested trigger expression.']])],
  Wait: [
    ...(SIGNATURES.Wait || []),
    signature('Wait(expression: TriggerExpression) -> TriggerSequenceStep', 'Waits for a trigger expression/event in a TriggerResource sequence.', [['expression', 'Trigger expression or event.']])
  ],
  'PlayerSleep.Confirmed': [signature('PlayerSleep.Confirmed(scenario: string, sleepCount?: int, sleepCountAtLeast?: int) -> TriggerEventSelector', 'Selects confirmed player sleep events.', [['scenario', 'Scenario string or field alias.'], ['sleepCount', 'Exact sleep count.'], ['sleepCountAtLeast', 'Minimum sleep count.']])],
  'PlayerSleep.EnteredDream': [signature('PlayerSleep.EnteredDream(scenario: string, sleepCount?: int, sleepCountAtLeast?: int) -> TriggerEventSelector', 'Selects entered-dream player sleep events.', [['scenario', 'Scenario string or field alias.'], ['sleepCount', 'Exact sleep count.'], ['sleepCountAtLeast', 'Minimum sleep count.']])],
  'PlayerSleep.Returned': [signature('PlayerSleep.Returned(scenario: string, sleepCount?: int, sleepCountAtLeast?: int) -> TriggerEventSelector', 'Selects returned-from-dream player sleep events.', [['scenario', 'Scenario string or field alias.'], ['sleepCount', 'Exact sleep count.'], ['sleepCountAtLeast', 'Minimum sleep count.']])],
  'PlayerSleep.Completed': [signature('PlayerSleep.Completed(scenario: string, sleepCount?: int, sleepCountAtLeast?: int) -> TriggerEventSelector', 'Selects completed player sleep events.', [['scenario', 'Scenario string or field alias.'], ['sleepCount', 'Exact sleep count.'], ['sleepCountAtLeast', 'Minimum sleep count.']])],
  'PlayerSleep.Failed': [signature('PlayerSleep.Failed(scenario: string, sleepCount?: int, sleepCountAtLeast?: int) -> TriggerEventSelector', 'Selects failed player sleep events.', [['scenario', 'Scenario string or field alias.'], ['sleepCount', 'Exact sleep count.'], ['sleepCountAtLeast', 'Minimum sleep count.']])],
  'Player.SeatedIn': [signature('Player.SeatedIn(zone: SeatZoneId | string) -> TriggerEventSelector', 'Срабатывает, когда игрок сел в любую точку указанной SeatZoneResource.', [['zone', 'Seat zone id, string literal or SeatZoneId field.']])],
  'Player.SeatedAt': [signature('Player.SeatedAt(zone: SeatZoneId | string, slot: SeatSlotId | string) -> TriggerEventSelector', 'Срабатывает, когда игрок сел в конкретный Slot указанной SeatZoneResource.', [['zone', 'Seat zone id, string literal or SeatZoneId field.'], ['slot', 'Seat slot id, string literal or SeatSlotId field.']])],
  'Player.StoodUpIn': [signature('Player.StoodUpIn(zone: SeatZoneId | string) -> TriggerEventSelector', 'Срабатывает, когда игрок встал из любого места указанной SeatZoneResource.', [['zone', 'Seat zone id, string literal or SeatZoneId field.']])],
  'Player.StoodUpFrom': [signature('Player.StoodUpFrom(zone: SeatZoneId | string, slot: SeatSlotId | string) -> TriggerEventSelector', 'Срабатывает, когда игрок встал из конкретного Slot указанной SeatZoneResource.', [['zone', 'Seat zone id, string literal or SeatZoneId field.'], ['slot', 'Seat slot id, string literal or SeatSlotId field.']])],
  'Player.ChangedSeatIn': [signature('Player.ChangedSeatIn(zone: SeatZoneId | string) -> TriggerEventSelector', 'Срабатывает, когда игрок сменил место внутри указанной SeatZoneResource.', [['zone', 'Seat zone id, string literal or SeatZoneId field.']])]
});

Object.assign(SIGNATURES, {
  SeatZoneResource: [
    signature(
      'class Name : SeatZoneResource { private void Init() { Id(...); Dimension(...); Area(Box(...)); Slot(...); } }',
      'Описывает Echo Seating zone: dimension, bounds, named seat slots and matching policy.',
      []
    )
  ],
  SeatZoneId: [
    signature(
      'private SeatZoneId Zone = "namespace:path"',
      'Typed field alias for SeatZoneResource Id(...).',
      [['Zone', 'Stable seat zone id.']]
    )
  ],
  SeatSlotId: [
    signature(
      'private SeatSlotId Chair = "chair"',
      'Typed field alias for SeatZoneResource Slot(...).',
      [['Chair', 'Stable seat slot id inside the zone.']]
    )
  ],
  Id: [
    ...(SIGNATURES.Id || []),
    signature('Id(id: SeatZoneId | string) -> void', 'Declares the id inside SeatZoneResource.Init().', [['id', 'Seat zone id or SeatZoneId field.']])
  ],
  Dimension: [
    ...(SIGNATURES.Dimension || []),
    signature('Dimension(dimensionId: string) -> void', 'Sets the Minecraft dimension for SeatZoneResource.', [['dimensionId', 'Dimension id, for example "minecraft:overworld".']])
  ],
  Area: [
    ...(SIGNATURES.Area || []),
    signature('Area(box: SeatZoneBox) -> void', 'Sets SeatZoneResource bounds.', [['box', 'Box(Position(min), Position(max)).']])
  ],
  Box: [
    signature(
      'Box(min: Position, max: Position) -> SeatZoneBox',
      'Builds SeatZoneResource box bounds.',
      [
        ['min', 'Minimum corner Position(...).'],
        ['max', 'Maximum corner Position(...).']
      ]
    )
  ],
  Detect: [
    signature('Detect(RIDING_ANCHOR | EXACT_SLOT_ONLY) -> void', 'Sets the seat detection policy.', [['policy', 'RIDING_ANCHOR or EXACT_SLOT_ONLY.']])
  ],
  UnknownSeats: [
    signature('UnknownSeats(ALLOW | REJECT) -> void', 'Sets how unnamed seats inside the zone are handled.', [['policy', 'ALLOW or REJECT.']])
  ],
  Slot: [
    ...(SIGNATURES.Slot || []),
    signature(
      'Slot(id: SeatSlotId | string, anchor: Position) -> void',
      'Adds a named seat slot to SeatZoneResource.',
      [
        ['id', 'Seat slot id or SeatSlotId field.'],
        ['anchor', 'Slot anchor Position(...).']
      ]
    )
  ],
  MatchRadius: [
    signature('MatchRadius(radius: number) -> void', 'Sets how far a riding anchor may be from a named Slot.', [['radius', 'Positive matching radius in blocks.']])
  ],
  Position: [
    ...(SIGNATURES.Position || []),
    signature('Position(x: number, y: number, z: number) -> Position', 'Builds a SeatZoneResource coordinate.', [['x', 'X coordinate.'], ['y', 'Y coordinate.'], ['z', 'Z coordinate.']])
  ]
});

Object.assign(SIGNATURES, {
  Presentation: [
    signature(
      'Presentation(nameKey: string, descriptionKey: string, icon: string) -> void',
      'Задает отображаемые данные Echo Effect resource для UI, команд и диагностики.',
      [
        ['nameKey', 'Localization key названия эффекта.'],
        ['descriptionKey', 'Localization key описания эффекта.'],
        ['icon', 'Resource location текстуры иконки эффекта.']
      ]
    )
  ],
  PlayerHudConfigResource: [
    signature(
      'class Name : PlayerHudConfigResource { private void Build() { PlayerHud(...); Bar(...); } }',
      'Описывает Echo Player HUD layout: общие параметры canvas и набор HUD bars.',
      []
    )
  ],
  PlayerHud: [
    signature(
      'PlayerHud(id: string, referenceWidth?: int, referenceHeight?: int, scaleWithResolution?: bool) -> void',
      'Задает базовые параметры HUD layout.',
      [
        ['id', 'Stable HUD config id.'],
        ['referenceWidth', 'Ширина reference canvas, по умолчанию 1920.'],
        ['referenceHeight', 'Высота reference canvas, по умолчанию 1080.'],
        ['scaleWithResolution', 'Масштабировать layout под текущее разрешение.']
      ]
    )
  ],
  Bar: [
    signature(
      'Bar(id: string, source: string, width: int, height: int, enabled?: bool, drawWhenUnavailable?: bool, icon?: string, iconWidth?: int, iconHeight?: int, iconSpacing?: int, iconOffsetX?: number, iconOffsetY?: number, anchor?: string, x?: int, y?: int, orientation?: string, fill?: string, background?: string, borderWidth?: int, borderColor?: string, gradientStart?: string, gradientEnd?: string, gradientSteps?: int) -> void',
      'Добавляет HUD bar для выбранной метрики игрока.',
      [
        ['id', 'Stable bar id.'],
        ['source', 'Metric id: health, spark, fatigue, stamina или custom id.'],
        ['width', 'Ширина bar в reference canvas.'],
        ['height', 'Высота bar в reference canvas.'],
        ['enabled', 'Показывать bar.'],
        ['drawWhenUnavailable', 'Рисовать bar, если metric сейчас недоступна.'],
        ['icon', 'Resource location иконки.'],
        ['iconWidth', 'Ширина иконки.'],
        ['iconHeight', 'Высота иконки.'],
        ['iconSpacing', 'Отступ между иконкой и bar.'],
        ['iconOffsetX', 'Дополнительный X-offset иконки.'],
        ['iconOffsetY', 'Дополнительный Y-offset иконки.'],
        ['anchor', 'HUD anchor: BOTTOM_LEFT, TOP_RIGHT, CENTER и т.д.'],
        ['x', 'X-offset от anchor.'],
        ['y', 'Y-offset от anchor.'],
        ['orientation', 'HORIZONTAL или VERTICAL.'],
        ['fill', 'LEFT_TO_RIGHT, RIGHT_TO_LEFT, TOP_TO_BOTTOM или BOTTOM_TO_TOP.'],
        ['background', 'Цвет фона bar.'],
        ['borderWidth', 'Толщина рамки.'],
        ['borderColor', 'Цвет рамки.'],
        ['gradientStart', 'Начальный цвет fill gradient.'],
        ['gradientEnd', 'Конечный цвет fill gradient.'],
        ['gradientSteps', 'Количество шагов gradient.']
      ]
    )
  ],
  Description: [signature('Description() -> string', 'Локальный helper WeaponResource для поля description.', [])],
  Tooltip: [signature('Tooltip() -> string[]', 'Локальный helper WeaponResource для поля tooltip.', [])],
  Damage: [signature('Damage() -> int', 'Локальный helper WeaponResource для поля damage.', [])],
  AttackDamage: [signature('AttackDamage() -> number', 'Локальный helper WeaponResource для поля attackDamage.', [])],
  AttacksPerSecond: [signature('AttacksPerSecond() -> number | string', 'Локальный helper WeaponResource для поля attacksPerSecond.', [])],
  AttackSpeedModifier: [signature('AttackSpeedModifier() -> number', 'Локальный helper WeaponResource для поля attackSpeedModifier.', [])],
  Knockback: [signature('Knockback() -> number', 'Локальный helper WeaponResource для поля knockback.', [])],
  Durability: [signature('Durability() -> int', 'Локальный helper WeaponResource для поля durability.', [])],
  Unbreakable: [signature('Unbreakable() -> bool', 'Локальный helper WeaponResource для поля unbreakable.', [])],
  Enchantability: [signature('Enchantability() -> int', 'Локальный helper WeaponResource для поля enchantability.', [])],
  RepairIngredient: [signature('RepairIngredient() -> string', 'Локальный helper WeaponResource для поля repairIngredient.', [])],
  Rarity: [signature('Rarity() -> WeaponRarity', 'Локальный helper WeaponResource для поля rarity.', [])],
  Glint: [signature('Glint() -> bool', 'Локальный helper WeaponResource для поля glint.', [])],
  FireResistant: [signature('FireResistant() -> bool', 'Локальный helper WeaponResource для поля fireResistant.', [])],
  HiddenFromCreative: [signature('HiddenFromCreative() -> bool', 'Локальный helper WeaponResource для поля hiddenFromCreative.', [])],
  CreativeOrder: [signature('CreativeOrder() -> int', 'Локальный helper WeaponResource для порядка item в creative tab.', [])]
});

Object.assign(SIGNATURES, {
  DialogueOverlayStyleConfig: [
    signature(
      'class Name : DialogueOverlayStyleConfig { public override DialogueOverlayStyleDef Build() { return DialogueOverlayStyle(...); } }',
      'Описывает клиентский стиль compact dialogue overlay.',
      []
    )
  ],
  DialogueOverlayStyle: [
    signature(
      'DialogueOverlayStyle(textScale?: number, textShadow?: bool, textAppearDelayMs?: int, typewriterMsPerCharacter?: int, typewriterMinDurationMs?: int, typewriterMaxDurationMs?: int, anchorX?: number, anchorY?: number, anchorOffsetX?: int, anchorOffsetY?: int, crosshairPaddingY?: int, screenMarginX?: int, screenMarginY?: int, minTextWidth?: int, maxTextWidth?: int, paddingX?: int, paddingY?: int, lineGap?: int, choiceSectionGap?: int, choicePaddingX?: int, choicePaddingY?: int, choiceGap?: int, visualSize?: int, visualGap?: int, visualTextOffsetY?: int, visualOffsetX?: int, visualOffsetY?: int, backgroundColor?: string, choiceBackgroundColor?: string, choiceBorderColor?: string, speakerColor?: string, textColor?: string, choiceNumberColor?: string, choiceTextColor?: string) -> DialogueOverlayStyleDef',
      'Задает размеры, тайминги, позицию и цвета compact dialogue overlay.',
      [
        ['textScale', 'Масштаб текста overlay.'],
        ['textShadow', 'Рисовать тень текста.'],
        ['textAppearDelayMs', 'Задержка перед появлением текста.'],
        ['typewriterMsPerCharacter', 'Скорость typewriter в миллисекундах на символ.'],
        ['typewriterMinDurationMs', 'Минимальная длительность typewriter.'],
        ['typewriterMaxDurationMs', 'Максимальная длительность typewriter.'],
        ['anchorX', 'Горизонтальный anchor панели в долях экрана.'],
        ['anchorY', 'Вертикальный anchor панели в долях экрана.'],
        ['anchorOffsetX', 'X-offset от anchor.'],
        ['anchorOffsetY', 'Y-offset от anchor.'],
        ['crosshairPaddingY', 'Вертикальный отступ от crosshair зоны.'],
        ['screenMarginX', 'Горизонтальный screen margin.'],
        ['screenMarginY', 'Вертикальный screen margin.'],
        ['minTextWidth', 'Минимальная ширина текста.'],
        ['maxTextWidth', 'Максимальная ширина текста.'],
        ['paddingX', 'Горизонтальный padding панели.'],
        ['paddingY', 'Вертикальный padding панели.'],
        ['lineGap', 'Расстояние между строками.'],
        ['choiceSectionGap', 'Расстояние между текстом и секцией choices.'],
        ['choicePaddingX', 'Горизонтальный padding choice.'],
        ['choicePaddingY', 'Вертикальный padding choice.'],
        ['choiceGap', 'Расстояние между choices.'],
        ['visualSize', 'Размер visual портрета/иконки.'],
        ['visualGap', 'Расстояние между visual и текстом.'],
        ['visualTextOffsetY', 'Y-offset текста относительно visual.'],
        ['visualOffsetX', 'Дополнительный X-offset visual.'],
        ['visualOffsetY', 'Дополнительный Y-offset visual.'],
        ['backgroundColor', 'Цвет фона панели.'],
        ['choiceBackgroundColor', 'Цвет фона choice.'],
        ['choiceBorderColor', 'Цвет рамки choice.'],
        ['speakerColor', 'Цвет имени speaker.'],
        ['textColor', 'Цвет основного текста.'],
        ['choiceNumberColor', 'Цвет номера choice.'],
        ['choiceTextColor', 'Цвет текста choice.']
      ]
    )
  ],
  PlayerVitalsConfigResource: [
    signature(
      'class Name : PlayerVitalsConfigResource { private void Build() { PlayerFatigue(...); } }',
      'Описывает runtime-настройки Echo Player Vitals.',
      []
    )
  ],
  PlayerFatigue: [
    signature(
      'PlayerFatigue(id: string, maximum: int, restedReserveAbove?: number, tiredReserveAtMost?: number, tiredReserveAbove?: number, exhaustedReserveAtMost?: number, tiredMovementMultiplier?: number, exhaustedMovementMultiplier?: number, exhaustedNausea?: number) -> void',
      'Настраивает fatigue maximum, stage thresholds и gameplay эффекты усталости.',
      [
        ['id', 'Stable fatigue config id.'],
        ['maximum', 'Максимальное значение fatigue.'],
        ['restedReserveAbove', 'Reserve ratio выше которого игрок считается rested.'],
        ['tiredReserveAtMost', 'Reserve ratio, с которого начинается tired.'],
        ['tiredReserveAbove', 'Reserve ratio выше exhausted порога.'],
        ['exhaustedReserveAtMost', 'Reserve ratio, с которого начинается exhausted.'],
        ['tiredMovementMultiplier', 'Множитель скорости для tired.'],
        ['exhaustedMovementMultiplier', 'Множитель скорости для exhausted.'],
        ['exhaustedNausea', 'Интенсивность nausea для exhausted.']
      ]
    )
  ],
  PortraitOverlayConfig: [
    signature(
      'class Name : PortraitOverlayConfig { public override PortraitOverlayDef Build() { return PortraitOverlay(...); } }',
      'Описывает клиентский portrait overlay для visual id.',
      []
    )
  ],
  PortraitOverlay: [
    signature(
      'PortraitOverlay(visualId: string, centerX?: number, centerY?: number, offsetX?: number, offsetY?: number, facePixels?: number, layers?: Layer[], idle?: Blink[]) -> PortraitOverlayDef',
      'Настраивает позиционирование лица, слои и idle blink для портретного overlay.',
      [
        ['visualId', 'Visual id, к которому применяется overlay.'],
        ['centerX', 'X-центр лица в портрете.'],
        ['centerY', 'Y-центр лица в портрете.'],
        ['offsetX', 'Глобальный X-offset overlay.'],
        ['offsetY', 'Глобальный Y-offset overlay.'],
        ['facePixels', 'Размер face-pixel шага для overlay.'],
        ['layers', 'Массив Layer(...) override-слоев.'],
        ['idle', 'Массив Blink-блоков для idle animation.']
      ]
    )
  ],
  Layer: [
    ...(SIGNATURES.Layer || []),
    signature(
      'Layer(id: string, placeX?: number, placeY?: number, offsetX?: number, offsetY?: number, scaleX?: number, scaleY?: number, w?: number, h?: number, visible?: bool) -> PortraitLayerConfig',
      'Описывает override-слой внутри PortraitOverlay(...).',
      [
        ['id', 'Layer id из portrait/face rig.'],
        ['placeX', 'Нормализованная X-позиция слоя.'],
        ['placeY', 'Нормализованная Y-позиция слоя.'],
        ['offsetX', 'X-offset слоя.'],
        ['offsetY', 'Y-offset слоя.'],
        ['scaleX', 'X-scale слоя.'],
        ['scaleY', 'Y-scale слоя.'],
        ['w', 'Override width.'],
        ['h', 'Override height.'],
        ['visible', 'Показывать слой.']
      ]
    )
  ]
});

const PANEL_BACKGROUND_OVERLAY_PARAMETERS = [
  ['mode', 'Режим отрисовки: solid, sliced или tiled.'],
  ['texture', 'Resource location текстуры панели.'],
  ['textureWidth', 'Ширина исходной текстуры.'],
  ['textureHeight', 'Высота исходной текстуры.'],
  ['sliceLeft', 'Левая граница nine-slice.'],
  ['sliceRight', 'Правая граница nine-slice.'],
  ['sliceTop', 'Верхняя граница nine-slice.'],
  ['sliceBottom', 'Нижняя граница nine-slice.'],
  ['tileScaleX', 'Горизонтальный scale для tiled режима.'],
  ['tileScaleY', 'Вертикальный scale для tiled режима.'],
  ['tileOriginX', 'X-origin тайлинга.'],
  ['tileOriginY', 'Y-origin тайлинга.'],
  ['tileFullOnly', 'Рисовать только полные tiles.'],
  ['fillDirection', 'Направление заполнения: LEFT_TO_RIGHT, RIGHT_TO_LEFT, TOP_TO_BOTTOM или BOTTOM_TO_TOP.'],
  ['fillAmount', 'Доля заполнения от 0.0 до 1.0.']
];

for (const panelBackgroundCall of ['DialoguePanelBackgroundOverlay', 'DialogueChoiceBackgroundOverlay']) {
  SIGNATURES[panelBackgroundCall] = [
    signature(
      `${panelBackgroundCall}(mode?: string, texture?: string, textureWidth?: int, textureHeight?: int, sliceLeft?: int, sliceRight?: int, sliceTop?: int, sliceBottom?: int, tileScaleX?: number, tileScaleY?: number, tileOriginX?: int, tileOriginY?: int, tileFullOnly?: bool, fillDirection?: string, fillAmount?: number) -> DialoguePanelBackgroundConfig`,
      panelBackgroundCall === 'DialoguePanelBackgroundOverlay'
        ? 'Настраивает фон основной dialogue panel: texture mode, nine-slice/tile параметры и fill progress.'
        : 'Настраивает фон панели choices: texture mode, nine-slice/tile параметры и fill progress.',
      PANEL_BACKGROUND_OVERLAY_PARAMETERS
    )
  ];
}

Object.assign(SIGNATURES, {
  Effect: [
    ...(SIGNATURES.Effect || []),
    signature('Effect(id: EffectId | string) -> EffectDefinition', 'Собирает id Echo Effects resource внутри EffectResource.Build().', [['id', 'Stable effect resource id.']])
  ],
  Duration: [signature('Duration(duration: Ticks | Seconds | InfiniteDuration) -> void', 'Задает длительность EffectResource.', [['duration', 'Ticks(...), Seconds(...), or InfiniteDuration().']])],
  Stacking: [signature('Stacking(policy: IGNORE_NEW | REFRESH_DURATION | RESTART | STACK_INTENSITY | ALLOW_MULTIPLE | REPLACE_OLD) -> void', 'Задает stacking policy эффекта.', [['policy', 'Effect stacking policy.']])],
  Lifetime: [signature('Lifetime(policy: INDEPENDENT | OWNER_BOUND | SESSION_BOUND | UNTIL_REMOVED) -> void', 'Задает lifetime policy эффекта.', [['policy', 'Effect lifetime policy.']])],
  EndOn: [signature('EndOn(conditions: EffectEndCondition...) -> void', 'Добавляет условия завершения эффекта.', [['conditions', 'PLAYER_DEATH, WORLD_EXIT, DISCONNECT, DIMENSION_CHANGE, OWNER_CANCELLED, OWNER_COMPLETED.']])],
  Ticks: [
    ...(SIGNATURES.Ticks || []),
    signature('Ticks(value: number) -> EffectTime', 'Оборачивает время эффекта в ticks.', [['value', 'Tick count.']])
  ],
  Seconds: [signature('Seconds(value: number) -> EffectTime', 'Оборачивает время эффекта в seconds.', [['value', 'Seconds.']])],
  InfiniteDuration: [signature('InfiniteDuration() -> EffectDuration', 'Делает EffectResource бесконечным до явного завершения.', [])],
  Curve: [signature('Curve(keys: EffectCurveKey...) -> EffectFloatCurve', 'Собирает кривую значений для effect channel.', [['keys', 'Key(...) values.']])],
  Color: [signature('Color(hex: string) -> EffectColor', 'Оборачивает hex цвет для screen effect channels.', [['hex', 'Color such as "#RRGGBB" or "#RRGGBBAA".']])],
  Key: [
    ...(SIGNATURES.Key || []),
    signature('Key(time: Ticks | Seconds, value: number, interpolation?: STEP | LINEAR | SMOOTH | CUBIC) -> EffectCurveKey', 'Точка кривой EffectResource.', [['time', 'Ticks(...) or Seconds(...).'], ['value', 'Curve value at time.'], ['interpolation', 'Interpolation kind.']])
  ],
  'Movement.SpeedMultiplier': [signature('Movement.SpeedMultiplier(curve: EffectFloatCurve) -> void', 'Задает множитель скорости движения игрока по кривой.', [['curve', 'Curve(Key(...), ...).']])],
  'Camera.FovOffset': [signature('Camera.FovOffset(curve: EffectFloatCurve) -> void', 'Задает FOV offset камеры по кривой.', [['curve', 'Curve(Key(...), ...).']])],
  'Camera.Roll': [signature('Camera.Roll(curve: EffectFloatCurve) -> void', 'Задает roll камеры по кривой.', [['curve', 'Curve(Key(...), ...).']])],
  'Camera.Shake': [signature('Camera.Shake(amplitude: EffectFloatCurve) -> void', 'Задает амплитуду camera shake по кривой.', [['amplitude', 'Curve(Key(...), ...).']])],
  'Screen.Tint': [signature('Screen.Tint(color: Color, opacity: EffectFloatCurve) -> void', 'Задает screen tint channel.', [['color', 'Color("#RRGGBB") style helper.'], ['opacity', 'Opacity curve.']])],
  'Screen.Vignette': [signature('Screen.Vignette(color: Color, opacity: EffectFloatCurve) -> void', 'Задает screen vignette channel.', [['color', 'Color("#RRGGBB") style helper.'], ['opacity', 'Opacity curve.']])],
  'Sound.PlayAt': [signature('Sound.PlayAt(time?: Ticks | Seconds, sound: string, volume?: number, pitch?: number) -> void', 'Добавляет sound event в timeline эффекта.', [['time', 'Ticks(...) or Seconds(...).'], ['sound', 'Minecraft sound id.'], ['volume', 'Sound volume.'], ['pitch', 'Sound pitch.']])],
  'VanillaEffect.Apply': [signature('VanillaEffect.Apply(time?: Ticks | Seconds, effect: string, duration: Ticks | Seconds, amplifier?: number, ambient?: bool, visible?: bool, showIcon?: bool) -> void', 'Добавляет vanilla MobEffect в timeline эффекта.', [['time', 'Ticks(...) or Seconds(...).'], ['effect', 'Minecraft MobEffect id, e.g. minecraft:nausea.'], ['duration', 'Ticks(...) or Seconds(...).'], ['amplifier', '0 is level I, 1 is level II.'], ['ambient', 'Vanilla ambient flag.'], ['visible', 'Show vanilla particles.'], ['showIcon', 'Show vanilla HUD icon.']])],
  'Effect.ApplyToPlayer': [signature('Effect.ApplyToPlayer(effectId: EffectId | string, intensity?: number) -> EffectCommand', 'Применяет Echo Effect к игроку.', [['effectId', 'Effect resource id.'], ['intensity', 'Optional intensity, default 1.0.']])],
  'Effect.RemoveFromPlayer': [signature('Effect.RemoveFromPlayer(effectId: EffectId | string, fadeOut?: Ticks | Seconds) -> EffectCommand', 'Снимает Echo Effect с игрока.', [['effectId', 'Effect resource id.'], ['fadeOut', 'Optional fade-out duration.']])],
  'Effect.WaitOnPlayer': [signature('Effect.WaitOnPlayer(effectId: EffectId | string) -> EffectCommand', 'Ждет effect state на игроке.', [['effectId', 'Effect resource id.']])],
  'Effect.IsActiveOnPlayer': [signature('Effect.IsActiveOnPlayer(effectId: EffectId | string) -> EffectCommand', 'Проверяет, активен ли effect на игроке.', [['effectId', 'Effect resource id.']])],
  'Effect.HasCompletedOnPlayer': [signature('Effect.HasCompletedOnPlayer(effectId: EffectId | string) -> EffectCommand', 'Проверяет, завершился ли effect на игроке.', [['effectId', 'Effect resource id.']])],
  Id: [
    ...(SIGNATURES.Id || []),
    signature('Id(id: string) -> void', 'Задает id wardrobe resource внутри Init().', [['id', 'Stable resource id.']])
  ],
  Profile: [signature('Profile(profileId: string) -> void', 'Ссылает wearable/outfit на wardrobe profile.', [['profileId', 'Wardrobe profile id.']])],
  Item: [signature('Item(itemId: string) -> void', 'Задает item id wearable.', [['itemId', 'Minecraft item id.']])],
  Slot: [
    ...(SIGNATURES.Slot || []),
    signature('Slot(FACE | TOP | BOTTOM | FEET) -> void', 'Задает wardrobe slot wearable.', [['slot', 'Wardrobe slot.']])
  ],
  EquipmentSlot: [signature('EquipmentSlot(HEAD | CHEST | LEGS | FEET) -> void', 'Задает equipment slot wearable.', [['slot', 'Minecraft equipment slot.']])],
  Collection: [signature('Collection(collection: symbol) -> void', 'Помечает wearable коллекцией.', [['collection', 'Collection symbol.']])],
  Model: [signature('Model(modelId: string) -> void', 'Задает model id wearable.', [['modelId', 'Wearable model id.']])],
  ModelPosition: [signature('ModelPosition(x: number, y: number, z: number) -> void', 'Смещение модели wearable.', [['x', 'X offset.'], ['y', 'Y offset.'], ['z', 'Z offset.']])],
  ModelRotation: [signature('ModelRotation(x: number, y: number, z: number) -> void', 'Поворот модели wearable.', [['x', 'X rotation.'], ['y', 'Y rotation.'], ['z', 'Z rotation.']])],
  ModelScale: [signature('ModelScale(x: number, y: number, z: number) -> void', 'Масштаб модели wearable.', [['x', 'X scale.'], ['y', 'Y scale.'], ['z', 'Z scale.']])],
  PortraitTexture: [signature('PortraitTexture(textureId: string) -> void', 'Задает texture для portrait contribution.', [['textureId', 'Portrait texture id.']])],
  PortraitStrip: [signature('PortraitStrip(srcX: int, srcY: int, srcW: int, srcH: int, dstX: int, dstY: int, layer: BASE | OUTER, pixelXOffset?: number, pixelYOffset?: number, pixelLeftInset?: number, pixelRightInset?: number, targetHeightScale?: number) -> void', 'Добавляет strip wearable в portrait.', [['srcX', 'Source X.'], ['srcY', 'Source Y.'], ['srcW', 'Source width.'], ['srcH', 'Source height.'], ['dstX', 'Target X.'], ['dstY', 'Target Y.'], ['layer', 'BASE or OUTER.'], ['pixelXOffset', 'Optional X offset.'], ['pixelYOffset', 'Optional Y offset.'], ['pixelLeftInset', 'Optional left inset.'], ['pixelRightInset', 'Optional right inset.'], ['targetHeightScale', 'Optional target height scale.']])],
  BaseSkin: [signature('BaseSkin(skinId: string) -> void', 'Задает base skin wardrobe profile.', [['skinId', 'Skin id.']])],
  Coverage: [signature('Coverage(REPORT | STRICT) -> void', 'Задает coverage policy wardrobe profile.', [['policy', 'REPORT or STRICT.']])],
  Slots: [signature('Slots(slots: WardrobeSlot...) -> void', 'Перечисляет поддержанные wardrobe slots.', [['slots', 'FACE, TOP, BOTTOM, FEET.']])],
  Match: [signature('Match(face?: string, top?: string, bottom?: string, feet?: string) -> void', 'Задает outfit selection match.', [['face', 'Face wearable selection.'], ['top', 'Top wearable selection.'], ['bottom', 'Bottom wearable selection.'], ['feet', 'Feet wearable selection.']])],
  WorldSkin: [signature('WorldSkin(skinId: string) -> void', 'Задает world skin outfit variant.', [['skinId', 'Skin id.']])],
  FaceSkin: [signature('FaceSkin(skinId: string) -> void', 'Задает face skin outfit variant.', [['skinId', 'Skin id.']])],
  Skin: [
    ...(SIGNATURES.Skin || []),
    signature('Skin(skinId: string) -> void', 'В OutfitVariantResource задает world и face skin одним вызовом.', [['skinId', 'Skin id.']])
  ],
  FaceRig: [signature('FaceRig(id: FaceRigId | string, bones?: BoneDef[], layers?: LayerDef[], controls?: BlinkControlDef[]) -> FaceRigDef', 'Собирает FaceRig resource.', [['id', 'Face rig id.'], ['bones', 'Bone definitions.'], ['layers', 'Layer definitions.'], ['controls', 'Blink controls.']])],
  FacialAnimationSurface3d: [signature('class Name : FacialAnimationSurface3d', 'Базовый surface-класс для 3D facial animation. Внутри можно объявить Rig(), Enter(), Stay() и Exit().', [])],
  FacialAnimationSurfacePortrait: [signature('class Name : FacialAnimationSurfacePortrait', 'Базовый surface-класс для portrait facial animation. Внутри можно объявить Rig(), Enter(), Stay() и Exit().', [])],
  Rig: [signature('Rig(id: FaceRigId | string) -> FaceRigId', 'Helper rig id внутри facial animation surface class.', [['id', 'Face rig id.']])],
  Surface: [signature('Surface(name: string, rig?: FaceRigId | string, enter?: PhaseDef, stay?: PhaseDef, exit?: PhaseDef) -> FacialAnimationSurfaceDef', 'Описывает facial animation surface.', [['name', 'Surface name.'], ['rig', 'Optional face rig id.'], ['enter', 'Enter phase.'], ['stay', 'Stay phase.'], ['exit', 'Exit phase.']])],
  Phase: [signature('Phase(durationTicks?: int, durationMillis?: int, end?: string, tracks?: TrackDef[]) -> FacialAnimationPhaseDef', 'Описывает фазу facial animation.', [['durationTicks', 'Duration in ticks.'], ['durationMillis', 'Duration in milliseconds.'], ['end', 'End behavior.'], ['tracks', 'Phase tracks.']])],
  Enter: [signature('Enter(durationTicks?: int, durationMillis?: int, end?: string, tracks?: TrackDef[]) -> FacialAnimationPhaseDef', 'Enter phase facial animation.', [['durationTicks', 'Duration in ticks.'], ['durationMillis', 'Duration in milliseconds.'], ['end', 'End behavior.'], ['tracks', 'Phase tracks.']])],
  Stay: [
    ...(SIGNATURES.Stay || []),
    signature('Stay(durationTicks?: int, durationMillis?: int, end?: string, tracks?: TrackDef[]) -> FacialAnimationPhaseDef', 'Stay phase facial animation.', [['durationTicks', 'Duration in ticks.'], ['durationMillis', 'Duration in milliseconds.'], ['end', 'End behavior.'], ['tracks', 'Phase tracks.']])
  ],
  Exit: [
    ...(SIGNATURES.Exit || []),
    signature('Exit(durationTicks?: int, durationMillis?: int, end?: string, tracks?: TrackDef[]) -> FacialAnimationPhaseDef', 'Exit phase facial animation.', [['durationTicks', 'Duration in ticks.'], ['durationMillis', 'Duration in milliseconds.'], ['end', 'End behavior.'], ['tracks', 'Phase tracks.']])
  ],
  Socket: [signature('Socket(id: string, bone: string, x?: number, y?: number, z?: number, rotX?: number, rotY?: number, rotZ?: number, scaleX?: number, scaleY?: number, scaleZ?: number, pivotX?: number, pivotY?: number, pivotZ?: number) -> AnimationSocketDef', 'Описывает body rig socket.', [['id', 'Socket id.'], ['bone', 'Parent bone id.'], ['x', 'Local X.'], ['y', 'Local Y.'], ['z', 'Local Z.']])],
  BlinkControl: [signature('BlinkControl(id: string, upperTravel?: number, lowerTravel?: number, gazeMinX?: number, gazeMinY?: number, gazeMaxX?: number, gazeMaxY?: number, upperBone?: string, lowerBone?: string, idle?: bool, periodTicks?: int, closeTicks?: int, holdTicks?: int, openTicks?: int) -> FaceControlDef', 'Описывает blink/gaze control.', [['id', 'Control id.'], ['upperTravel', 'Upper eyelid travel.'], ['lowerTravel', 'Lower eyelid travel.'], ['gazeMinX', 'Minimum gaze X.'], ['gazeMinY', 'Minimum gaze Y.'], ['gazeMaxX', 'Maximum gaze X.'], ['gazeMaxY', 'Maximum gaze Y.'], ['upperBone', 'Upper eyelid bone.'], ['lowerBone', 'Lower eyelid bone.'], ['idle', 'Enable idle blink.'], ['periodTicks', 'Blink period.'], ['closeTicks', 'Close duration.'], ['holdTicks', 'Closed hold duration.'], ['openTicks', 'Open duration.']])],
  Layer: [
    ...(SIGNATURES.Layer || []),
    signature('Layer(id: string, bone: string, texture: string, role?: string, order?: int, visible?: bool, x?: int, y?: int, w?: int, h?: int) -> FaceLayerDef', 'Описывает layer внутри FaceRig.', [['id', 'Layer id.'], ['bone', 'Bone id.'], ['texture', 'Texture id.'], ['role', 'Layer role.'], ['order', 'Draw order.'], ['visible', 'Default visibility.']])
  ],
  BoneDelta: [
    ...(SIGNATURES.BoneDelta || []),
    signature('BoneDelta(id: string, x?: number, y?: number, z?: number, rotationZ?: number, scaleX?: number, scaleY?: number, scaleZ?: number) -> BoneDeltaDef', 'Задает delta transform face bone.', [['id', 'Bone id.'], ['x', 'X delta.'], ['y', 'Y delta.'], ['z', 'Z delta.'], ['rotationZ', 'Z rotation.']])
  ],
  ControlDelta: [
    ...(SIGNATURES.ControlDelta || []),
    signature('ControlDelta(id: string, value: number) -> ControlDeltaDef', 'Задает значение face control.', [['id', 'Control id.'], ['value', 'Control value.']])
  ],
  Textures: [signature('Textures(root: string) -> void', 'Root path текстур high-level face DSL.', [['root', 'Texture root.']])],
  Texture: [signature('Texture(textureId: string) -> void', 'Texture id для visual face part.', [['textureId', 'Texture id.']])],
  TextureArea: [signature('TextureArea(x: int, y: int, width: int, height: int) -> void', 'Source rectangle in a face texture.', [['x', 'Texture X.'], ['y', 'Texture Y.'], ['width', 'Rectangle width.'], ['height', 'Rectangle height.']])],
  Size: [signature('Size(width: int, height: int) -> void', 'Pixel size of a face part.', [['width', 'Width.'], ['height', 'Height.']])],
  Order: [signature('Order(value: int) -> void', 'Draw order в high-level face DSL.', [['value', 'Order value.']])],
  Role: [signature('Role(role: string) -> void', 'Layer role в high-level face DSL.', [['role', 'LayerRole value.']])],
  Parent: [signature('Parent(parentBoneId: string) -> void', 'Parent bone для дополнительной face bone.', [['parentBoneId', 'Parent bone id.']])],
  Pivot: [signature('Pivot(x: number, y: number) -> void', '2D pivot in high-level face DSL.', [['x', 'Pivot X.'], ['y', 'Pivot Y.']])],
  Travel: [signature('Travel(x: number, y: number) -> void', 'Blink travel vector.', [['x', 'Travel X.'], ['y', 'Travel Y.']])],
  Gaze: [signature('Gaze(x: number, y: number) -> void', 'Blink gaze vector.', [['x', 'Gaze X.'], ['y', 'Gaze Y.']])],
  Timing: [signature('Timing(periodTicks: int, closeTicks: int, holdTicks: int, openTicks: int) -> void', 'Blink timing.', [['periodTicks', 'Blink period.'], ['closeTicks', 'Close duration.'], ['holdTicks', 'Closed hold duration.'], ['openTicks', 'Open duration.']])],
  PeriodTicks: [signature('PeriodTicks(value: int) -> void', 'Blink period override.', [['value', 'Tick count.']])],
  CloseTicks: [signature('CloseTicks(value: int) -> void', 'Blink close duration override.', [['value', 'Tick count.']])],
  HoldTicks: [signature('HoldTicks(value: int) -> void', 'Blink hold duration override.', [['value', 'Tick count.']])],
  OpenTicks: [signature('OpenTicks(value: int) -> void', 'Blink open duration override.', [['value', 'Tick count.']])]
});

for (const triggerName of [
  'PlayerInside',
  'HasItem', 'ItemCountAtLeast', 'ItemCountAtMost', 'ItemCountExactly', 'HasEmptyInventorySlot',
  'Holding', 'HoldingMainHand', 'HoldingOffHand',
  'Wearing', 'HasAnyArmor', 'HasFullArmorSet',
  'HealthAtMost', 'HealthAtLeast', 'FoodAtMost', 'FoodAtLeast',
  'IsSneaking', 'IsSprinting', 'IsSwimming', 'IsSleeping', 'IsOnGround', 'IsBurning',
  'HasEffect', 'EffectAmplifierAtLeast', 'EffectDurationAtLeast',
  'AdvancementCompleted', 'ExperienceLevelAtLeast',
  'InDimension', 'GameModeIs', 'TimeBetween', 'DayAtLeast', 'WeatherIs',
  'PlayerEnter', 'PlayerExit', 'PlayerStay',
  'ItemAdded', 'ItemRemoved', 'ItemPickedUp', 'ItemDropped', 'ItemUsed', 'ItemConsumed',
  'InventoryBecameFull', 'InventorySlotFreed',
  'HeldItemChanged', 'MainHandChanged', 'OffHandChanged',
  'EquipmentChanged', 'EquipmentEquipped', 'EquipmentUnequipped',
  'PlayerDamaged', 'PlayerHealed', 'PlayerDied', 'PlayerRespawned', 'PlayerJumped',
  'SneakStarted', 'SneakStopped', 'SprintStarted', 'SprintStopped',
  'SwimmingStarted', 'SwimmingStopped', 'SleepStarted', 'SleepFinished',
  'PlayerStartedBurning', 'PlayerStoppedBurning',
  'EffectAdded', 'EffectRemoved', 'EffectAmplifierChanged',
  'ExperienceLevelReached', 'AdvancementGranted',
  'DimensionChanged', 'DimensionEntered', 'DimensionLeft', 'GameModeChanged',
  'BlockInteracted', 'BlockBroken', 'BlockPlaced', 'ContainerOpened', 'BedEntered', 'BedLeft',
  'TimeReached', 'DayReached', 'WeatherChanged'
]) {
  SIGNATURES[triggerName] = [signature(
    `${triggerName}(area?: AreaRef, target?: string, count?: int, value?: number, level?: int, day?: int, start?: int, end?: int) -> TriggerExpression`,
    'Builds a TriggerResource predicate/event expression.',
    [
      ['area', 'Area field for spatial player expressions.'],
      ['target', 'Target item/block/effect/dimension/weather/mode depending on expression.'],
      ['count', 'Item count threshold.'],
      ['value', 'Numeric threshold value.'],
      ['level', 'Experience level threshold.'],
      ['day', 'Day threshold.'],
      ['start', 'Range start.'],
      ['end', 'Range end.']
    ]
  )];
}

SIGNATURES['Camera.Shake'] = [
  signature(
    'Camera.Shake(amplitude: EffectFloatCurve, frequency?: number, seed?: string) -> void',
    'Задает camera shake: амплитуду по кривой и дополнительные параметры частоты/seed.',
    [
      ['amplitude', 'Curve(Key(...), ...) для силы shake.'],
      ['frequency', 'Частота shake, если поддержана каналом эффекта.'],
      ['seed', 'Seed/режим стабильности shake, например STABLE.']
    ]
  )
];
SIGNATURES['Screen.Tint'] = [
  signature(
    'Screen.Tint(color: Color, opacity: EffectFloatCurve, layer?: BELOW_UI | ABOVE_UI) -> void',
    'Задает screen tint channel: цвет, opacity-кривую и слой отрисовки.',
    [
      ['color', 'Color("#RRGGBB") style helper.'],
      ['opacity', 'Curve(Key(...), ...) для прозрачности tint.'],
      ['layer', 'Слой screen effect, например BELOW_UI.']
    ]
  )
];
SIGNATURES['Screen.Vignette'] = [
  signature(
    'Screen.Vignette(color: Color, opacity: EffectFloatCurve, layer?: BELOW_UI | ABOVE_UI) -> void',
    'Задает screen vignette channel: цвет, opacity-кривую и слой отрисовки.',
    [
      ['color', 'Color("#RRGGBB") style helper.'],
      ['opacity', 'Curve(Key(...), ...) для прозрачности vignette.'],
      ['layer', 'Слой screen effect, например BELOW_UI.']
    ]
  )
];
SIGNATURES['VanillaEffect.Apply'] = [
  signature(
    'VanillaEffect.Apply(time?: Ticks | Seconds, effect: string, duration: Ticks | Seconds, amplifier?: number, ambient?: bool, visible?: bool, showIcon?: bool) -> void',
    'Добавляет vanilla MobEffect в timeline эффекта.',
    [
      ['time', 'Когда применить vanilla effect относительно старта Echo Effects instance.'],
      ['effect', 'Minecraft MobEffect id, например minecraft:nausea или minecraft:blindness.'],
      ['duration', 'Длительность vanilla эффекта через Ticks(...) или Seconds(...).'],
      ['amplifier', '0 = уровень I, 1 = уровень II.'],
      ['ambient', 'Vanilla ambient flag.'],
      ['visible', 'Показывать vanilla particles/визуальный след.'],
      ['showIcon', 'Показывать vanilla HUD icon.']
    ]
  )
];

SIGNATURES['Npc.WalkTo'] = SIGNATURES['Npc.MoveTo'];
SIGNATURES['Npc.WalkToPlayer'] = SIGNATURES['Npc.MoveToPlayer'];
SIGNATURES['Npc.Turn'] = SIGNATURES['Npc.Rotate'];
SIGNATURES['This.Turn'] = SIGNATURES['This.Rotate'];
SIGNATURES['Npc.InteractBlock'] = SIGNATURES['Npc.UseBlock'];
SIGNATURES['Npc.CloseDoor'] = SIGNATURES['Npc.OpenDoor'];
SIGNATURES['Npc.ToggleDoor'] = SIGNATURES['Npc.OpenDoor'];
SIGNATURES['Npc.UseDoor'] = SIGNATURES['Npc.OpenDoor'];

function echoScriptSignatureHelp(text, offset) {
  const source = String(text || '');
  const safeOffset = Math.max(0, Math.min(offset, source.length));
  const masked = maskStringsAndLineComments(source);
  const openIndex = innermostCallOpenBefore(masked, safeOffset);
  if (openIndex < 0) {
    return undefined;
  }
  const target = callTargetBeforeOpen(masked, openIndex);
  const signatures = SIGNATURES[target];
  if (!signatures || signatures.length === 0) {
    return undefined;
  }
  const activeArgument = activeArgumentInfo(masked, openIndex + 1, safeOffset);
  const activeSignature = activeSignatureIndex(target, signatures, activeArgument);
  const signatureInfo = signatures[activeSignature] || signatures[0];
  const activeParameter = activeParameterIndex(signatureInfo, activeArgument);
  return {
    target,
    activeSignature,
    activeParameter,
    signatures
  };
}

function signature(label, documentation, parameters) {
  return {
    label,
    documentation,
    parameters: parameters.map(([name, doc]) => ({
      name,
      label: name,
      documentation: doc
    }))
  };
}

function activeSignatureIndex(target, signatures, activeArgument) {
  return 0;
}

function activeParameterIndex(signatureInfo, activeArgument) {
  if (activeArgument.name) {
    const namedIndex = signatureInfo.parameters.findIndex(parameter => parameter.name === activeArgument.name);
    if (namedIndex >= 0) {
      return namedIndex;
    }
  }
  return Math.max(0, Math.min(activeArgument.index, signatureInfo.parameters.length - 1));
}

function activeArgumentInfo(text, start, offset) {
  let index = 0;
  let depth = 0;
  let currentStart = start;
  for (let cursor = start; cursor < offset; cursor += 1) {
    const char = text[cursor];
    if (char === '(' || char === '[' || char === '{') {
      depth += 1;
    } else if (char === ')' || char === ']' || char === '}') {
      depth = Math.max(0, depth - 1);
    } else if (char === ',' && depth === 0) {
      index += 1;
      currentStart = cursor + 1;
    }
  }
  const current = text.slice(currentStart, offset);
  const named = /^\s*([A-Za-z_][A-Za-z0-9_]*)\s*:/.exec(current);
  return {
    index,
    name: named ? named[1] : ''
  };
}

function innermostCallOpenBefore(text, offset) {
  let depth = 0;
  for (let index = Math.min(offset - 1, text.length - 1); index >= 0; index -= 1) {
    const char = text[index];
    if (char === ')' || char === ']' || char === '}') {
      depth += 1;
    } else if (char === '(' || char === '[' || char === '{') {
      if (depth === 0 && char === '(') {
        return index;
      }
      depth = Math.max(0, depth - 1);
    }
  }
  return -1;
}

function callTargetBeforeOpen(text, openIndex) {
  let end = openIndex;
  while (end > 0 && /\s/.test(text[end - 1])) {
    end -= 1;
  }
  const member = identifierBefore(text, end);
  if (!member) {
    return '';
  }
  let start = end - member.length;
  let dot = start;
  while (dot > 0 && /\s/.test(text[dot - 1])) {
    dot -= 1;
  }
  if (dot > 0 && text[dot - 1] === '.') {
    let namespaceEnd = dot - 1;
    while (namespaceEnd > 0 && /\s/.test(text[namespaceEnd - 1])) {
      namespaceEnd -= 1;
    }
    const namespace = identifierBefore(text, namespaceEnd);
    if (namespace) {
      return `${namespace}.${member}`;
    }
  }
  return member;
}

function identifierBefore(text, end) {
  let start = end;
  while (start > 0 && /[A-Za-z0-9_]/.test(text[start - 1])) {
    start -= 1;
  }
  const value = text.slice(start, end);
  return /^[A-Za-z_][A-Za-z0-9_]*$/.test(value) ? value : '';
}

function maskStringsAndLineComments(text) {
  return text
    .replace(/"([^"\\]|\\.)*"|'([^'\\]|\\.)*'/g, value => ' '.repeat(value.length))
    .replace(/\/\/[^\r\n]*/g, value => ' '.repeat(value.length));
}

function signatureHelpTargets() {
  return Object.keys(SIGNATURES);
}

module.exports = {
  echoScriptSignatureHelp,
  signatureHelpTargets
};
