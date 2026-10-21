'use strict';

const vscode = require('vscode');
const {
  propertyOnLine,
  typeValueOnLine,
  wordRangeAtPosition,
  referenceKindForField
} = require('./languageContext');
const { resolveReference } = require('./referenceResolver');
const { isLocalizationKeyField } = require('./valueCompletionModel');
const {
  hoverSummaryForWord,
  signatureForWord
} = require('./echoscriptSupport');

class EchoStoryHoverProvider {
  constructor(catalog, index) {
    this.catalog = catalog;
    this.index = index;
  }

  provideHover(document, position) {
    const line = document.lineAt(position.line).text;
    const property = propertyOnLine(line);
    const wordRange = wordRangeAtPosition(document, position);
    const word = wordRange ? document.getText(wordRange) : '';

    if (document.languageId === 'echoscript') {
      const namedArgument = echoScriptNamedArgumentHover(document, position, word, wordRange);
      if (namedArgument) {
        return namedArgument;
      }
      const signature = signatureForWord(word);
      if (signature) {
        const summary = hoverSummaryForWord(word);
        return new vscode.Hover(markdown([
          `**${word}**`,
          summary,
          ...signatureMarkdown(signature)
        ]), wordRange);
      }
      const localMethod = localMethodSignature(document, word);
      if (localMethod) {
        return new vscode.Hover(markdown([
          `**${word}**`,
          'Локальный EchoScript метод в этом файле. Используй его для reusable resource fragments, effects, nodes или typed helper code.',
          `\`${localMethod}\``
        ]), wordRange);
      }
    }

    const typeValue = typeValueOnLine(line);
    if (typeValue && word === typeValue) {
      return this.typeHover(typeValue);
    }

    if (property && word === property && this.catalog.fieldDocs[property]) {
      return new vscode.Hover(markdown([
        `**${property}**`,
        this.catalog.fieldDocs[property]
      ]), wordRange);
    }

    if (property) {
      if (isLocalizationKeyField(property)) {
        const hover = this.localizationKeyHover(word);
        if (hover) {
          return hover;
        }
      }
      const referenceKind = referenceKindForField(property);
      if (referenceKind) {
        const hover = this.referenceHover(referenceKind, word);
        if (hover) {
          return hover;
        }
      }
    }

    const typeHover = this.typeHover(word);
    if (typeHover) {
      return typeHover;
    }
    return undefined;
  }

  typeHover(typeValue) {
    const definition = this.catalog.effectTypes.get(typeValue)
      || this.catalog.conditionTypes.get(typeValue)
      || this.catalog.triggerTypes.get(typeValue)
      || this.catalog.sceneStepTypes.get(typeValue)
      || this.catalog.npcCommandTypes.get(typeValue)
      || this.catalog.worldCommandTypes.get(typeValue);
    if (!definition) {
      return undefined;
    }

    const lines = [
      `**type: \`${typeValue}\`**`,
      definition.description
    ];
    if (definition.alias) {
      lines.push(`Alias of \`${definition.canonical}\`.`);
    }
    if (definition.required && definition.required.length > 0) {
      lines.push(`Required fields: \`${definition.required.join('`, `')}\`.`);
    }
    if (definition.anyRequired && definition.anyRequired.length > 0) {
      lines.push(`One-of groups: ${definition.anyRequired.map(group => `\`${group.join('` / `')}\``).join(', ')}.`);
    }
    return new vscode.Hover(markdown(lines));
  }

  referenceHover(kind, id) {
    if (!id) {
      return undefined;
    }
    const resolved = resolveReference(this.index, kind, id);
    if (!resolved) {
      return undefined;
    }
    return new vscode.Hover(markdown([
      `**${resolved.kind}: \`${resolved.id}\`**`,
      'Ссылка найдена в story index. Используй Go to Definition, чтобы открыть источник.'
    ]));
  }

  localizationKeyHover(key) {
    if (!key || !this.index.localizationKeyLocation) {
      return undefined;
    }
    const location = this.index.localizationKeyLocation(key);
    if (!location) {
      return undefined;
    }
    return new vscode.Hover(markdown([
      `**localizationKey: \`${key}\`**`,
      location.preview ? location.preview : '',
      'Ключ найден в localization index. Используй Go to Definition, чтобы открыть источник.'
    ]));
  }
}

const ECHOSCRIPT_ARGUMENT_DOCS = {
  'Navigation.SetTarget': {
    targetId: 'Стабильный id navigation target. Лучше читаемый id вроде `quest.main_gate`, чтобы обновления заменяли ту же цель.',
    x: 'X координата GPS marker.',
    y: 'Y координата GPS marker.',
    z: 'Z координата GPS marker.',
    label: 'Текст, который игрок увидит для цели. Если не указать, runtime возьмет target id.',
    title: 'Alias для `label`, compiler его принимает.',
    name: 'Alias для `label`, compiler его принимает.',
    dimensionId: 'Minecraft dimension id цели. Указывай, если цель не в default/current dimension игрока.',
    dimension: 'Alias для `dimensionId`, compiler его принимает.',
    world: 'Alias для `dimensionId`, compiler его принимает.',
    icon: 'Необязательный icon id для navigation UI.',
    iconId: 'Необязательный icon id для navigation UI.',
    profile: 'Предпочтения pathfinding для маршрута. Частые значения: `prefer_roads`, `quest_safe`.',
    navigationProfile: 'Alias для `profile`, compiler его принимает.'
  },
  'World.SetNavigationTarget': {
    targetId: 'Стабильный id navigation target. Лучше читаемый id вроде `quest.main_gate`, чтобы обновления заменяли ту же цель.',
    x: 'X координата GPS marker.',
    y: 'Y координата GPS marker.',
    z: 'Z координата GPS marker.',
    label: 'Текст, который игрок увидит для цели. Если не указать, runtime возьмет target id.',
    title: 'Alias для `label`, compiler его принимает.',
    name: 'Alias для `label`, compiler его принимает.',
    dimensionId: 'Minecraft dimension id цели. Указывай, если цель не в default/current dimension игрока.',
    dimension: 'Alias для `dimensionId`, compiler его принимает.',
    world: 'Alias для `dimensionId`, compiler его принимает.',
    icon: 'Необязательный icon id для navigation UI.',
    iconId: 'Необязательный icon id для navigation UI.',
    profile: 'Предпочтения pathfinding для маршрута. Частые значения: `prefer_roads`, `quest_safe`.',
    navigationProfile: 'Alias для `profile`, compiler его принимает.'
  },
  'Npc.MoveTo': {
    speed: 'Необязательная скорость движения NPC.',
    completionDistance: 'Необязательная дистанция, на которой точка считается достигнутой.',
    pauseTicks: 'Необязательная пауза после достижения точки.',
    repathAfterTicks: 'Необязательный интервал пересчета пути в ticks.',
    maxRepathAttempts: 'Необязательный лимит попыток пересчета пути.',
    teleportToTargetIfStuck: 'Если true, runtime может телепортировать NPC к цели при застревании.',
    openDoors: 'Если true, NPC может открывать двери по пути.',
    canFloat: 'Если true, navigation может использовать floating path.',
    targetSearchRadius: 'Радиус поиска ближайшей достижимой точки вокруг целевой позиции.',
    thenDialogue: 'DialogueRef/id, который стартует после прибытия.',
    thenDialogueId: 'Alias для `thenDialogue`, compiler его принимает.'
  },
  'Npc.MoveToPlayer': {
    stopDistance: 'Необязательная дистанция остановки рядом с игроком.',
    speed: 'Необязательная скорость движения к игроку.',
    repathTicks: 'Как часто пересчитывать путь к движущемуся игроку.',
    timeoutTicks: 'Через сколько ticks approach считается проваленным.',
    lookAtPlayer: 'Если true, NPC смотрит на игрока во время подхода.',
    warpWhenUnseen: 'Если true, runtime может варпнуть NPC, когда игрок его не видит.',
    warpDistance: 'Дистанция, после которой разрешается warp fallback.',
    warpSearchRadius: 'Радиус поиска безопасной точки для warp.',
    thenDialogue: 'DialogueRef/id, который стартует после подхода.',
    thenDialogueId: 'Alias для `thenDialogue`, compiler его принимает.'
  },
  'Npc.Rotate': {
    rotation: 'Vector3 для поворота: x=pitch, y=yaw, z=roll. Можно передать переменную, например `targetRotate`.',
    rot: 'Alias для `rotation`, compiler его принимает.',
    axes: 'Alias для `rotation`, compiler его принимает.',
    yaw: 'Целевой yaw, если не используешь Vector3.',
    targetYaw: 'Alias для `yaw`, compiler его принимает.',
    rotationYaw: 'Alias для `yaw`, compiler его принимает.',
    facingYaw: 'Alias для `yaw`, compiler его принимает.',
    pitch: 'Необязательный pitch, если не используешь Vector3.',
    targetPitch: 'Alias для `pitch`, compiler его принимает.',
    rotationPitch: 'Alias для `pitch`, compiler его принимает.',
    roll: 'Необязательный roll. Сейчас сохраняется в IR; обычный entity визуально применяет pitch/yaw.',
    targetRoll: 'Alias для `roll`, compiler его принимает.',
    rotationRoll: 'Alias для `roll`, compiler его принимает.',
    speed: 'Скорость поворота в degrees per tick. Значение > 0 включает плавный head-first поворот.',
    turnSpeed: 'Alias для `speed`, compiler его принимает.',
    rotationSpeed: 'Alias для `speed`, compiler его принимает.',
    speedDegreesPerTick: 'Alias для `speed`, compiler его принимает.',
    degreesPerTick: 'Alias для `speed`, compiler его принимает.',
    lock: 'Если true, NPC останется rotation locked после поворота.',
    locked: 'Alias для `lock`, compiler его принимает.',
    keepLocked: 'Alias для `lock`, compiler его принимает.'
  },
  'Npc.UseBlock': {
    action: 'Необязательный action/mode для block interaction, например `use`.',
    mode: 'Alias для `action`, compiler его принимает.'
  },
  'Npc.PlayAnimation': {
    layer: 'Необязательный body animation layer, например `Layer.Overlay`.',
    fadeInTicks: 'Необязательный fade-in анимации.',
    fadeOutTicks: 'Необязательный fade-out после stop/duration.',
    durationTicks: 'Необязательная длительность проигрывания.'
  },
  'Npc.StopAnimation': {
    layer: 'Необязательный body animation layer, например `Layer.Overlay`.',
    fadeOutTicks: 'Необязательный fade-out остановки.'
  },
  'Npc.PlayFaceAnimation': {
    layer: 'Необязательный face animation layer: `FaceLayer.Base`, `FaceLayer.Overlay`, `FaceLayer.Detail`.',
    fadeInTicks: 'Необязательный fade-in face animation.',
    fadeOutTicks: 'Необязательный fade-out после stop/duration.',
    durationTicks: 'Необязательная длительность проигрывания.'
  },
  'Npc.StopFaceAnimation': {
    layer: 'Необязательный face animation layer.',
    fadeOutTicks: 'Необязательный fade-out остановки.'
  },
  'Npc.LookAtPlayers': {
    enabled: 'Если true, NPC будет смотреть на ближайших игроков; false отключает override.'
  },
  'This.Rotate': {
    rotation: 'Vector3 для поворота текущего NPC actor: x=pitch, y=yaw, z=roll.',
    rot: 'Alias для `rotation`, compiler его принимает.',
    axes: 'Alias для `rotation`, compiler его принимает.',
    yaw: 'Целевой yaw, если не используешь Vector3.',
    pitch: 'Необязательный pitch, если не используешь Vector3.',
    roll: 'Необязательный roll. Сейчас сохраняется в IR; обычный entity визуально применяет pitch/yaw.',
    speed: 'Скорость поворота в degrees per tick. Значение > 0 включает плавный head-first поворот.',
    turnSpeed: 'Alias для `speed`, compiler его принимает.',
    lock: 'Если true, текущий NPC останется rotation locked после поворота.'
  }
};

Object.assign(ECHOSCRIPT_ARGUMENT_DOCS, {
  Story: {
    start: 'First local graph node executed by this story resource.'
  },
  Start: {
    next: 'Next local story node id after the start node.'
  },
  Action: {
    type: 'Engine adapter type id, for example `echo_dialogue:play`, `echo_npc:spawn`, or `echo_animation:play_animation`.',
    actorId: 'Runtime actor id used by NPC action adapters.',
    definitionId: 'NPC definition resource id used by spawn actions.',
    dialogueId: 'Dialogue resource id to play.',
    audienceId: 'Audience/player id for dialogue playback.',
    worldId: 'Minecraft world or dimension id.',
    x: 'Adapter X coordinate, usually serialized as a string.',
    y: 'Adapter Y coordinate, usually serialized as a string.',
    z: 'Adapter Z coordinate, usually serialized as a string.',
    yaw: 'Spawn or animation target yaw.',
    pitch: 'Spawn or animation target pitch.',
    animation: 'Animation resource id used by animation action adapters.',
    target: 'Target actor id or animation graph state, depending on the adapter.',
    await: 'Animation wait policy such as `STARTED` or `COMPLETED`.',
    output: 'Name of the output slot written by this action node.',
    next: 'Next local story node id.'
  },
  Set: {
    variable: 'Story state key written by this graph node.',
    value: 'Value assigned to the state key.',
    next: 'Next local story node id.'
  },
  Dialogue: {
    start: 'Optional dialogue start node id when supported by the resource builder.'
  },
  Npc: {
    id: 'Stable NPC definition id.',
    display: 'Display fallback used by tools and runtime UI.',
    skin: 'NPC skin visual, usually `Skin("id")`.'
  },
  NpcAnimationBinding: {
    actor: 'Optional specific actor id override.',
    npc: 'NPC definition id bound to an animation profile.',
    profile: 'Animation profile id.'
  },
  Animation: {
    channels: 'Declared animation channels.',
    sources: 'Animation graph source states.',
    entries: 'Entry phases that transition into main playback.',
    main: 'Main animation playback phase.',
    exits: 'Exit phases available from the main animation.',
    target: 'Target graph state after recovery.',
    recovery: 'Recovery policy for interrupted or resumed playback.',
    replay: 'Replay policy when the animation is already running.'
  },
  AnimationProfile: {
    rig: 'Animation rig id.',
    graph: 'Animation graph id.',
    render: 'Render model kind.',
    initialStates: 'Initial graph states for channels.',
    requiredChannels: 'Channels required by this profile.',
    metadata: 'Authoring metadata.'
  },
  AnimationRig: {
    kind: 'Rig kind.',
    skin: 'Humanoid skin variant or rig skin mode.',
    bones: 'Custom bone definitions.',
    sockets: 'Attachment sockets.'
  },
  AnimationClip: {
    rig: 'Body animation rig or face rig id.',
    tracks: 'Animation tracks for bones or face controls.',
    schemaVersion: 'Clip schema version.',
    loopSeam: 'Loop seam policy.',
    authoredSpeedBlocksPerSecond: 'Authored locomotion speed for the clip.'
  },
  Entry: {
    clip: 'Animation clip id used for this entry phase.',
    durationTicks: 'Entry phase duration in ticks.'
  },
  Main: {
    clip: 'Animation clip id used for main playback.',
    mode: 'Main playback mode.',
    durationTicks: 'Main phase duration in ticks.',
    exitPoints: 'Named exit points from main playback.'
  },
  Exit: {
    clip: 'Animation clip id used for this exit phase.',
    durationTicks: 'Exit phase duration in ticks.'
  },
  FacePose: {
    rig: 'Face rig id.',
    bones: 'Face bone deltas.',
    controls: 'Face control value deltas.'
  },
  FacialAnimation: {
    rig: 'Face rig id.',
    surfaces: 'Surface classes or surface definitions where this facial animation can play.',
    tracks: 'Default face animation tracks.',
    durationTicks: 'Default duration in ticks.',
    durationMillis: 'Default duration in milliseconds.',
    end: 'End behavior.'
  }
});

Object.assign(ECHOSCRIPT_ARGUMENT_DOCS, {
  Environment: {
    id: 'Stable environment resource id.',
    dimension: 'Dimension/world where this environment applies.',
    states: 'Environment-local state definitions.'
  },
  State: {
    id: 'Environment-local state id.',
    changes: 'Block changes applied by this environment state.'
  },
  SetBlock: {
    position: 'Block position to set.',
    blockId: 'Minecraft block id to place.',
    properties: 'Optional block-state properties.',
    nbt: 'Optional block NBT payload.'
  },
  ClearBlock: {
    position: 'Block position to clear.'
  },
  Property: {
    name: 'Block-state property name.',
    value: 'Block-state property value.'
  },
  Npc: {
    ...(ECHOSCRIPT_ARGUMENT_DOCS.Npc || {}),
    nameColor: 'NPC display-name color. Use `NameColor("#RRGGBB")` or `nameColor("#RRGGBB")` rather than a raw string.',
    defaultForm: 'Default authored form for this NPC, usually `DefaultForm(NpcForms.Form)`.'
  },
  Form: {
    id: 'Stable NPC form resource id.',
    skin: 'Skin visual used by this form.',
    model: 'Runtime model id.',
    dimensions: 'Collision and eye dimensions.',
    bodyScale: 'Whole-body scale.',
    parts: 'Authored model parts.',
    anchors: 'Named attachment anchors.',
    animationGraph: 'Animation graph/profile id.',
    faceProfile: 'Face profile/rig id.',
    portrait: 'Portrait visual.',
    tags: 'Authoring/runtime tags.'
  },
  Dimensions: {
    width: 'Entity collision width.',
    height: 'Entity collision height.',
    eyeHeight: 'Entity eye height.'
  },
  Part: {
    id: 'NPC form part id.',
    scale: 'Uniform part scale.',
    scaleX: 'X scale.',
    scaleY: 'Y scale.',
    scaleZ: 'Z scale.',
    pivotX: 'Part pivot X.',
    pivotY: 'Part pivot Y.',
    pivotZ: 'Part pivot Z.',
    rotationX: 'Part rotation X.',
    rotationY: 'Part rotation Y.',
    rotationZ: 'Part rotation Z.'
  },
  Anchor: {
    id: 'NPC form anchor id.',
    x: 'Anchor X.',
    y: 'Anchor Y.',
    z: 'Anchor Z.'
  },
  'Story.Set': {
    stateKey: 'Story state key to write.',
    value: 'Value assigned to the story state key.',
    next: 'Next local story node id.'
  },
  'Story.Load': {
    storyId: 'Story resource id to start.',
    executionId: 'Optional explicit child execution id.',
    next: 'Next local story node id.'
  },
  'Dialogue.Load': {
    dialogueId: 'Dialogue resource id to load.',
    audienceId: 'Dialogue audience/player id.',
    output: 'Output binding slot.',
    next: 'Next local story node id.'
  },
  'Environment.Set': {
    environmentId: 'Environment resource id.',
    stateId: 'Environment-local state id to apply.',
    next: 'Next local story node id.'
  },
  'Screen.SetBlack': {
    canvasGroup: 'Screen canvas group id.',
    alpha: 'Target black overlay alpha.',
    time: 'Transition duration.',
    durationMillis: 'Transition duration in milliseconds.',
    output: 'Output binding slot.',
    next: 'Next local story node id.'
  },
  'Player.SpawnAt': {
    x: 'Target X coordinate.',
    y: 'Target Y coordinate.',
    z: 'Target Z coordinate.',
    yaw: 'Target yaw.',
    pitch: 'Target pitch.',
    output: 'Output binding slot.',
    next: 'Next local story node id.'
  },
  'Player.SetHotbarItem': {
    slot: 'Hotbar slot index.',
    itemId: 'Minecraft item id.',
    count: 'Stack count.',
    replace: 'Whether to replace an existing stack.',
    next: 'Next local story node id.'
  },
  'Player.SelectHotbarSlot': {
    slot: 'Hotbar slot index.',
    next: 'Next local story node id.'
  },
  'Player.SitAt': {
    x: 'Seat X coordinate.',
    y: 'Seat Y coordinate.',
    z: 'Seat Z coordinate.',
    timeoutTicks: 'Timeout in ticks.',
    next: 'Next local story node id.'
  },
  'Blackspace.Create': {
    instanceId: 'Blackspace instance id.',
    id: 'Alias for instanceId.',
    playerName: 'Player name.',
    playerUuid: 'Player UUID.',
    output: 'Output binding slot.',
    next: 'Next local story node id.'
  },
  'Player.Teleport': {
    instanceId: 'Blackspace instance id.',
    target: 'Teleport target id.',
    playerName: 'Player name.',
    playerUuid: 'Player UUID.',
    output: 'Output binding slot.',
    next: 'Next local story node id.'
  },
  'Npc.SpawnAt': {
    yaw: 'NPC spawn yaw.',
    pitch: 'NPC spawn pitch.',
    worldId: 'Spawn world/dimension id.'
  },
  'Npc.SleepAt': {
    expectedBlock: 'Expected bed/block id.',
    worldId: 'Bed world/dimension id.'
  },
  'Npc.Interact': {
    npc: 'NPC resource/reference.',
    position: 'BlockPosition field or new BlockPosition(x, y, z); adapter expands it into x/y/z.',
    block: 'Alias for position.',
    targetBlock: 'Alias for position.',
    dimension: 'Interaction dimension.',
    x: 'Interaction X.',
    y: 'Interaction Y.',
    z: 'Interaction Z.',
    face: 'Block face; AUTO by default.',
    hand: 'Interaction hand; MAIN_HAND by default.',
    approach: 'Approach policy; AUTO by default.',
    output: 'Output binding slot.',
    next: 'Next local story node id.'
  },
  'Npc.StopInteraction': {
    npc: 'NPC resource/reference.',
    output: 'Output binding slot.',
    next: 'Next local story node id.'
  },
  'Npc.Stop': {
    npc: 'NPC resource/reference.',
    output: 'Output binding slot.',
    next: 'Next local story node id.'
  }
});

for (const sleepTarget of ['Arm', 'Disarm', 'CloseEyes', 'WaitUntilClosed', 'EnterDream', 'OpenEyes', 'WaitUntilOpen', 'Wait', 'WaitSeconds', 'ReturnToBed', 'CompleteNight', 'Complete', 'Cancel', 'IsActive', 'IsArmed', 'IsEyesClosed']) {
  ECHOSCRIPT_ARGUMENT_DOCS[`Sleep.${sleepTarget}`] = {
    player: 'Player id/name context.',
    scenario: 'Sleep scenario id.',
    owner: 'Sleep flow owner id.',
    command: 'Sleep command id.',
    output: 'Output binding slot.',
    next: 'Next local story node id.',
    duration: 'Duration accepted by the sleep action.',
    seconds: 'Wait duration in seconds.',
    ticks: 'Wait duration in ticks.',
    target: 'Dream target id.',
    blackspaceInstanceId: 'Blackspace instance id.',
    blackspaceId: 'Blackspace definition id.',
    dimension: 'Target dimension.',
    worldId: 'Target world/dimension id.',
    x: 'Target X coordinate.',
    y: 'Target Y coordinate.',
    z: 'Target Z coordinate.',
    yaw: 'Target yaw.',
    pitch: 'Target pitch.',
    sleepyBlink: 'Whether to use sleepy blink.',
    showHud: 'Whether to restore HUD.',
    mode: 'Return-to-bed mode.'
  };
}

Object.assign(ECHOSCRIPT_ARGUMENT_DOCS, {
  PlayerProfile: {
    id: 'Stable player profile template id.',
    nameMode: 'Player name source. Common values: `ACCOUNT` or `CUSTOM`.',
    displayName: 'Custom visible player name when `nameMode` is `CUSTOM`.',
    spark: 'Initial/current spark value.',
    maxSpark: 'Maximum spark value.',
    skinMode: 'Player skin source. Common values: `ACCOUNT` or `RESOURCE`.',
    skinId: 'Skin resource id when `skinMode` is `RESOURCE`.',
    skinModel: 'Minecraft skin model. Common values: `CLASSIC` or `SLIM`.'
  },
  Weapon: {
    id: 'Stable Echo Item Pack weapon id.',
    name: 'Visible weapon display name.',
    texture: 'Texture resource path/id used by generated item assets.',
    description: 'Optional weapon description text.',
    tooltip: 'Optional tooltip line list.',
    model: 'Optional custom item model id/path.',
    damage: 'Attack damage value.',
    attackDamage: 'Alias for attack damage.',
    attacksPerSecond: 'Weapon attack rate.',
    attackSpeedModifier: 'Raw attack speed modifier override.',
    knockback: 'Attack knockback value.',
    durability: 'Durability points.',
    unbreakable: 'Whether this weapon should not lose durability.',
    enchantability: 'Enchantability value.',
    repairIngredient: 'Item id or tag used for repairing.',
    rarity: 'Item rarity. Common values: `COMMON`, `UNCOMMON`, `RARE`, `EPIC`.',
    glint: 'Whether the item should render with enchantment glint.',
    fireResistant: 'Whether the item resists fire/lava destruction.',
    equipmentSlot: 'Preferred equipment slot, for example `MAINHAND` or armor slots.',
    hiddenFromCreative: 'Whether to hide the generated item from creative tabs.',
    creativeOrder: 'Ordering weight inside creative item lists.'
  },
  Area: {
    dimension: 'Minecraft dimension id.',
    x: 'Area origin X.',
    y: 'Area origin Y.',
    z: 'Area origin Z.',
    sx: 'Area size X.',
    sy: 'Area size Y.',
    sz: 'Area size Z.'
  },
  Vector3: {
    x: 'Vector X coordinate.',
    y: 'Vector Y coordinate.',
    z: 'Vector Z coordinate.'
  },
  BoxArea: {
    min: 'Minimum corner vector.',
    max: 'Maximum corner vector.'
  },
  SphereArea: {
    center: 'Shape center vector.',
    radius: 'Shape radius.'
  },
  CylinderArea: {
    center: 'Shape center vector.',
    radius: 'Shape radius.',
    minY: 'Lower vertical bound.',
    maxY: 'Upper vertical bound.'
  },
  CircleArea: {
    center: 'Shape center vector.',
    radius: 'Shape radius.',
    minY: 'Lower vertical bound.',
    maxY: 'Upper vertical bound.'
  },
  PolygonArea: {
    points: 'Polygon points as Vector3 values.',
    minY: 'Lower vertical bound.',
    maxY: 'Upper vertical bound.'
  },
  StartStory: {
    story: 'Story resource id to start when the trigger fires.'
  },
  StartDialogue: {
    dialogue: 'Dialogue resource id to start when the subscription trigger fires.',
    sessionId: 'Optional explicit dialogue session id for callback actions.',
    audienceId: 'Optional explicit dialogue audience id for callback actions.'
  },
  DialogueStart: {
    dialogue: 'Dialogue resource id to start from a trigger callback.',
    sessionId: 'Optional explicit dialogue session id.',
    audienceId: 'Optional explicit dialogue audience id.'
  },
  'Dialogue.Start': {
    dialogue: 'Dialogue resource id to start.',
    sessionId: 'Optional explicit dialogue session id.',
    audienceId: 'Optional explicit dialogue audience id.'
  },
  'Npc.StandUp': {
    npc: 'NPC actor/resource reference to remove from an interaction seat anchor.',
    output: 'Output binding slot.',
    next: 'Next local story node id.'
  },
  'Debug.Mark': {
    label: 'Debug marker label recorded as a TriggerResource callback action.'
  },
  Cooldown: {
    ticks: 'Cooldown duration in ticks or `Ticks(...)` wrapper.'
  },
  Dimension: {
    dimensionId: 'Minecraft dimension id bound to the trigger.'
  },
  FireCount: {
    count: 'Maximum number of trigger fires.'
  },
  StayInterval: {
    ticks: 'PlayerStay polling interval in ticks.'
  },
  PlayerInside: {
    area: 'Area field or shape checked against the player.'
  },
  HasItem: {
    itemId: 'Item id required in inventory.',
    count: 'Required item count.'
  },
  PlayerEnter: {
    area: 'Area field or shape that must be entered.'
  },
  PlayerExit: {
    area: 'Area field or shape that must be exited.'
  },
  PlayerStay: {
    area: 'Area field or shape where the player must stay.'
  }
});

for (const selector of ['Confirmed', 'EnteredDream', 'Returned', 'Completed', 'Failed']) {
  ECHOSCRIPT_ARGUMENT_DOCS[`PlayerSleep.${selector}`] = {
    scenario: 'Sleep scenario id.',
    sleepCount: 'Exact sleep count required by this selector.',
    sleepCountAtLeast: 'Minimum sleep count required by this selector.'
  };
}

Object.assign(ECHOSCRIPT_ARGUMENT_DOCS, {
  Duration: {
    duration: 'EffectResource duration: `Ticks(...)`, `Seconds(...)`, or `InfiniteDuration()`.'
  },
  Key: {
    time: 'Curve key time, usually `Ticks(...)` or `Seconds(...)`.',
    value: 'Curve value at this key.',
    interpolation: 'Interpolation between this key and the next one: `STEP`, `LINEAR`, `SMOOTH`, or `CUBIC`.'
  },
  'Effect.ApplyToPlayer': {
    intensity: 'Optional effect intensity multiplier. The compiler defaults it to `1.0`.'
  },
  'Effect.RemoveFromPlayer': {
    fadeOut: 'Optional fade-out duration, usually `Ticks(...)` or `Seconds(...)`.'
  },
  'Camera.Shake': {
    amplitude: 'Curve controlling camera shake amplitude over the effect lifetime.'
  },
  'Screen.Tint': {
    color: 'Tint color helper, usually `Color("#RRGGBB")`.',
    opacity: 'Opacity curve for the tint, usually `Curve(Key(...), ...)`.'
  },
  'Screen.Vignette': {
    color: 'Vignette color helper, usually `Color("#RRGGBB")`.',
    opacity: 'Opacity curve for the vignette, usually `Curve(Key(...), ...)`.'
  },
  'Sound.PlayAt': {
    time: 'Timeline time for the sound event.',
    sound: 'Minecraft sound id.',
    volume: 'Sound volume, default `1.0`.',
    pitch: 'Sound pitch, default `1.0`.'
  },
  'VanillaEffect.Apply': {
    time: 'Timeline time for applying the vanilla MobEffect.',
    effect: 'Minecraft MobEffect id, for example `minecraft:nausea`.',
    duration: 'Vanilla effect duration, usually `Ticks(...)` or `Seconds(...)`.',
    amplifier: 'Vanilla amplifier: `0` is level I, `1` is level II.',
    ambient: 'Vanilla ambient flag, default `false`.',
    visible: 'Whether vanilla particles/visual traces are visible, default `true`.',
    showIcon: 'Whether the vanilla HUD icon is shown, default `true`.'
  },
  Match: {
    face: 'Face wearable selection matched by this outfit variant.',
    top: 'Top wearable selection matched by this outfit variant.',
    bottom: 'Bottom wearable selection matched by this outfit variant.',
    feet: 'Feet wearable selection matched by this outfit variant.'
  },
  PortraitStrip: {
    srcX: 'Source X in the portrait texture.',
    srcY: 'Source Y in the portrait texture.',
    srcW: 'Source width in the portrait texture.',
    srcH: 'Source height in the portrait texture.',
    dstX: 'Destination X in the generated portrait composition.',
    dstY: 'Destination Y in the generated portrait composition.',
    layer: '`BASE` draws below outer layers; `OUTER` draws above base portrait pixels.',
    pixelXOffset: 'Optional horizontal pixel offset.',
    pixelYOffset: 'Optional vertical pixel offset.',
    pixelLeftInset: 'Optional left inset used for portrait fitting.',
    pixelRightInset: 'Optional right inset used for portrait fitting.',
    targetHeightScale: 'Optional scaling factor for target portrait height.'
  },
  Surface: {
    rig: 'Optional FaceRig id for this surface; falls back to the animation rig if omitted.',
    enter: 'Enter phase, usually `Enter(...)`.',
    stay: 'Stay phase, usually `Stay(...)`.',
    exit: 'Exit phase, usually `Exit(...)`.'
  },
  Phase: {
    durationTicks: 'Phase duration in ticks.',
    durationMillis: 'Phase duration in milliseconds. Do not mix with `durationTicks` in the same call.',
    end: 'Phase end behavior such as `HOLD`.',
    tracks: 'Face animation tracks used by this phase.'
  },
  Enter: {
    durationTicks: 'Enter phase duration in ticks.',
    durationMillis: 'Enter phase duration in milliseconds.',
    end: 'Enter phase end behavior.',
    tracks: 'Tracks used by the enter phase.'
  },
  Stay: {
    durationTicks: 'Stay phase duration in ticks.',
    durationMillis: 'Stay phase duration in milliseconds.',
    end: 'Stay phase end behavior.',
    tracks: 'Tracks used by the stay phase.'
  },
  Exit: {
    durationTicks: 'Exit phase duration in ticks, or animation exit duration depending on context.',
    durationMillis: 'Exit phase duration in milliseconds.',
    end: 'Exit phase end behavior.',
    tracks: 'Tracks used by the exit phase.'
  },
  BlinkControl: {
    upperTravel: 'Upper eyelid travel used by blink control.',
    lowerTravel: 'Lower eyelid travel used by blink control.',
    idle: 'Whether this control can blink during idle.',
    periodTicks: 'Idle blink period.',
    closeTicks: 'Ticks spent closing the eyelids.',
    holdTicks: 'Ticks spent holding the eyelids closed.',
    openTicks: 'Ticks spent opening the eyelids.'
  }
});

ECHOSCRIPT_ARGUMENT_DOCS['Npc.Move'] = {
  npc: 'NPC resource/reference.',
  x: 'Target X coordinate.',
  y: 'Target Y coordinate.',
  z: 'Target Z coordinate.',
  dimension: 'Target dimension; minecraft:overworld by default.',
  output: 'Output binding slot.',
  next: 'Next local story node id.',
  ...ECHOSCRIPT_ARGUMENT_DOCS['Npc.MoveTo']
};
ECHOSCRIPT_ARGUMENT_DOCS['Npc.WalkTo'] = ECHOSCRIPT_ARGUMENT_DOCS['Npc.MoveTo'];
ECHOSCRIPT_ARGUMENT_DOCS['Npc.ApproachPlayer'] = ECHOSCRIPT_ARGUMENT_DOCS['Npc.MoveToPlayer'];
ECHOSCRIPT_ARGUMENT_DOCS['Npc.WalkToPlayer'] = ECHOSCRIPT_ARGUMENT_DOCS['Npc.MoveToPlayer'];
ECHOSCRIPT_ARGUMENT_DOCS['Npc.Turn'] = ECHOSCRIPT_ARGUMENT_DOCS['Npc.Rotate'];
ECHOSCRIPT_ARGUMENT_DOCS['This.Turn'] = ECHOSCRIPT_ARGUMENT_DOCS['This.Rotate'];
ECHOSCRIPT_ARGUMENT_DOCS['Npc.InteractBlock'] = ECHOSCRIPT_ARGUMENT_DOCS['Npc.UseBlock'];

function echoScriptNamedArgumentHover(document, position, word, wordRange) {
  if (!word || !wordRange) {
    return undefined;
  }
  const line = document.lineAt(position.line).text;
  const afterWord = line.slice(wordRange.end.character);
  if (!/^\s*:/.test(afterWord)) {
    return undefined;
  }
  const target = callTargetBeforeOffset(document.getText(), document.offsetAt(wordRange.start));
  const docs = ECHOSCRIPT_ARGUMENT_DOCS[target];
  const description = docs ? docs[word] : '';
  if (!description) {
    return undefined;
  }
  return new vscode.Hover(markdown([
    `**${target}.${word}**`,
    description
  ]), wordRange);
}

function callTargetBeforeOffset(text, offset) {
  const masked = maskStringsAndLineComments(String(text || ''));
  const openIndex = innermostCallOpenBefore(masked, offset);
  if (openIndex < 0) {
    return '';
  }
  return callTargetBeforeOpen(masked, openIndex);
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

function markdown(lines) {
  const md = new vscode.MarkdownString(lines.filter(Boolean).join('\n\n'));
  md.isTrusted = false;
  return md;
}

function signatureMarkdown(signature) {
  return String(signature)
    .split(/\r?\n/)
    .filter(Boolean)
    .map(line => looksLikeSignatureLine(line) ? `\`${line}\`` : line);
}

function looksLikeSignatureLine(line) {
  return /^(\w|\.)/.test(line)
    && (
      line.includes('(')
      || line.includes('->')
      || /^[A-Z][A-Za-z0-9_]*\./.test(line)
      || /^private\s+/.test(line)
      || /^public\s+/.test(line)
    );
}

function localMethodSignature(document, word) {
  if (!word || !/^[A-Za-z_][A-Za-z0-9_]*$/.test(word)) {
    return '';
  }
  const escaped = word.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const pattern = new RegExp(`^\\s*(?:(?:public|private|static|override)\\s+)*([A-Za-z_][A-Za-z0-9_.]*(?:\\[\\])?)\\s+(${escaped})\\s*\\(([^)]*)\\)`);
  for (let lineIndex = 0; lineIndex < document.lineCount; lineIndex += 1) {
    const line = document.lineAt(lineIndex).text;
    const match = pattern.exec(line);
    if (match) {
      return `${match[1]} ${match[2]}(${match[3].trim()})`;
    }
  }
  return '';
}

module.exports = {
  EchoStoryHoverProvider
};
