'use strict';

const { eventCompletions } = require('./eventCompletions');

const {
  NAMED_STATIC_NAMESPACES,
  STATIC_NAMESPACE_MEMBERS
} = require('./echoscriptStaticSymbols');

const RESOURCE_KINDS = [
  ['story', 'Stories'],
  ['environment', 'Environments'],
  ['dialogue', 'Dialogues'],
  ['scene', 'Scenes'],
  ['npc', ''],
  ['npcForm', 'NpcForms'],
  ['speaker', 'Speakers'],
  ['animation', 'Animations'],
  ['faceAnimation', 'FaceAnimations'],
  ['animationProfile', 'AnimationProfiles'],
  ['animationRig', 'AnimationRigs'],
  ['animationClip', 'AnimationClips'],
  ['faceRig', 'FaceRigs'],
  ['facePose', 'FacePoses'],
  ['npcAnimationBinding', 'NpcAnimationBindings'],
  ['playerProfileTemplate', 'PlayerProfileTemplates'],
  ['weapon', 'Weapons'],
  ['trigger', 'Triggers'],
  ['skin', 'Skins'],
  ['cameraPath', 'CameraPaths']
];

const RESOURCE_NAMESPACE_BY_KIND = new Map(RESOURCE_KINDS);

const POSITIONAL_REFERENCE_KINDS = {
  DefaultForm: { 0: ['npcForm'] },
  'Dialogue.Load': { 0: ['dialogue'] },
  'Dialogue.Start': { 0: ['dialogue'] },
  DialogueStart: { 0: ['dialogue'] },
  'Environment.Set': { 0: ['environment'] },
  'Story.Load': { 0: ['story'] },
  StartDialogue: { 0: ['dialogue'] },
  StartStory: { 0: ['story'] },
  'Scene.Start': { 0: ['scene'] },
  Alias: { 1: ['speaker', 'npc'] },
  CameraPathCue: { 0: ['cameraPath'] },
  Face: { 0: ['faceAnimation'] },
  InteractionDialogues: { '*': ['dialogue'] },
  Line: { 0: ['speaker', 'npc'] },
  LineAfterMove: { 0: ['speaker', 'npc'] },
  'Monologue.Show': { 0: ['speaker', 'npc'] },
  NpcPath: { 0: ['npc'] },
  'Npc.ApproachPlayer': { 0: ['npc'] },
  'Npc.ClearHeldItem': { 0: ['npc'] },
  'Npc.ClearLookAtPlayers': { 0: ['npc'] },
  'Npc.CloseDoor': { 0: ['npc'] },
  'Npc.Despawn': { 0: ['npc'] },
  'Npc.EnterNearestVehicle': { 0: ['npc'] },
  'Npc.EnterVehicle': { 0: ['npc'] },
  'Npc.EnterVehicleAt': { 0: ['npc'] },
  'Npc.HoldItem': { 0: ['npc'] },
  'Npc.InteractBlock': { 0: ['npc'] },
  'Npc.LeaveVehicle': { 0: ['npc'] },
  'Npc.LookAtPlayers': { 0: ['npc'] },
  'Npc.Move': { 0: ['npc'] },
  'Npc.MoveTo': { 0: ['npc'] },
  'Npc.MoveToPlayer': { 0: ['npc'] },
  'Npc.OpenDoor': { 0: ['npc'] },
  'Npc.Path': { 0: ['npc'] },
  'Npc.PlayFaceAnimation': { 0: ['npc'], 1: ['faceAnimation'] },
  'Npc.PlayAnimation': { 0: ['npc'], 1: ['animation'] },
  'Npc.Rotate': { 0: ['npc'] },
  'Npc.Spawn': { 0: ['npc'] },
  'Npc.SpawnAt': { 0: ['npc'] },
  'Npc.SpawnNearPlayer': { 0: ['npc'] },
  'Npc.Stay': { 0: ['npc'] },
  'Npc.SleepAt': { 0: ['npc'] },
  'Npc.Stop': { 0: ['npc'] },
  'Npc.Interact': { 0: ['npc'] },
  'Npc.StandUp': { 0: ['npc'] },
  'Npc.StopInteraction': { 0: ['npc'] },
  'Npc.StopFaceAnimation': { 0: ['npc'] },
  'Npc.StopAnimation': { 0: ['npc'] },
  'Npc.ToggleDoor': { 0: ['npc'] },
  'Npc.Turn': { 0: ['npc'] },
  'Npc.UseBlock': { 0: ['npc'] },
  'Npc.UseDoor': { 0: ['npc'] },
  'Npc.WalkTo': { 0: ['npc'] },
  'Npc.WalkToPlayer': { 0: ['npc'] },
  'This.PlayAnimation': { 0: ['animation'] },
  Portrait: { 0: ['skin'] },
  SetAnimation: { 0: ['animation'] },
  SetAnimationFace: { 0: ['faceAnimation'] },
  SetFaceAnimation: { 0: ['faceAnimation'] },
  Skin: { 0: ['skin'] },
  SkinHead: { 0: ['skin'] },
  'Vehicle.Sequence': { 0: ['npc'] },
  'Vehicle.Trip': { 0: ['npc'] }
};

const NAMED_REFERENCE_KINDS = {
  actor: ['npc'],
  anim: ['animation'],
  animation: ['animation'],
  animationClip: ['animationClip'],
  animationClipId: ['animationClip'],
  animationGraph: ['animationProfile', 'animationRig'],
  animationId: ['animation'],
  cameraPath: ['cameraPath'],
  cameraPathId: ['cameraPath'],
  cinematic: ['cameraPath'],
  cinematicPath: ['cameraPath'],
  cinematicPathId: ['cameraPath'],
  dialogue: ['dialogue'],
  dialogueId: ['dialogue'],
  definitionId: ['npc'],
  defaultForm: ['npcForm'],
  defaultFormId: ['npcForm'],
  playerProfile: ['playerProfileTemplate'],
  playerProfileId: ['playerProfileTemplate'],
  playerProfileTemplate: ['playerProfileTemplate'],
  playerProfileTemplateId: ['playerProfileTemplate'],
  template: ['playerProfileTemplate'],
  templateId: ['playerProfileTemplate'],
  weapon: ['weapon'],
  weaponId: ['weapon'],
  exitAnimationId: ['animation'],
  environment: ['environment'],
  environmentId: ['environment'],
  face: ['faceAnimation'],
  faceAnimation: ['faceAnimation'],
  faceAnimationId: ['faceAnimation'],
  actorId: ['npc'],
  animationProfileId: ['animationProfile'],
  animationRigId: ['animationRig'],
  clip: ['animationClip'],
  clipId: ['animationClip'],
  faceRigId: ['faceRig'],
  facePoseId: ['facePose'],
  faceProfile: ['faceRig'],
  graph: ['animationRig', 'faceRig'],
  form: ['npcForm'],
  formId: ['npcForm'],
  npc: ['npc'],
  npcId: ['npc'],
  profile: ['animationProfile'],
  rig: ['animationRig', 'faceRig'],
  scene: ['scene'],
  sceneId: ['scene'],
  skin: ['skin'],
  speaker: ['speaker', 'npc'],
  speakerId: ['speaker', 'npc'],
  story: ['story'],
  storyId: ['story'],
  thenDialogue: ['dialogue'],
  thenDialogueId: ['dialogue']
};

const TRIGGER_PREDICATES = [
  'PlayerInside',
  'HasItem', 'ItemCountAtLeast', 'ItemCountAtMost', 'ItemCountExactly', 'HasEmptyInventorySlot',
  'Holding', 'HoldingMainHand', 'HoldingOffHand',
  'Wearing', 'HasAnyArmor', 'HasFullArmorSet',
  'HealthAtMost', 'HealthAtLeast', 'FoodAtMost', 'FoodAtLeast',
  'IsSneaking', 'IsSprinting', 'IsSwimming', 'IsSleeping', 'IsOnGround', 'IsBurning',
  'HasEffect', 'EffectAmplifierAtLeast', 'EffectDurationAtLeast',
  'AdvancementCompleted', 'ExperienceLevelAtLeast',
  'InDimension', 'GameModeIs', 'TimeBetween', 'DayAtLeast', 'WeatherIs'
];

const TRIGGER_EVENTS = [
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
];

const PLAYER_SLEEP_SELECTORS = ['Confirmed', 'EnteredDream', 'Returned', 'Completed', 'Failed'];
const PLAYER_SEATING_SELECTORS = ['SeatedIn', 'SeatedAt', 'StoodUpIn', 'StoodUpFrom', 'ChangedSeatIn'];

const GLOBAL_SNIPPETS = [
  {
    label: 'Node declaration',
    insertText: 'Node ${1:NextNode}()\n{\n    Line("${2:john}", "${3:Text}");\n}',
    detail: 'High-level dialogue Node; id is normalized from its symbol',
    kind: 'snippet',
    sortText: '000_nodes_field'
  },
  {
    label: 'start Node declaration',
    insertText: 'start Node ${1:Opening}()\n{\n    Line("${2:john}", "${3:Text}");\n    GoTo(${4:Continue});\n}',
    detail: 'Explicit entry Node for a multi-node DialogueResource',
    kind: 'snippet',
    sortText: '000_start_node'
  },
  {
    label: 'Speaker override',
    insertText: 'Speaker("${1:john}", actor: "${2:jdh:john}", display: "${3:John}");',
    detail: 'Optional sparse high-level speaker override',
    kind: 'snippet',
    sortText: '000_speaker_override'
  },
  {
    label: 'Formerly node migration',
    insertText: 'Formerly("${1:old_node_id}", ${2:CurrentNode});',
    detail: 'Map a saved legacy node id to a current Node symbol',
    kind: 'snippet',
    sortText: '000_formerly'
  },
  {
    label: 'Story.Set',
    insertText: 'Story.Set("${1:state_key}", ${2:true});',
    detail: 'Set story state from OnStart linear code',
    kind: 'snippet',
    sortText: '001_story_set'
  },
  {
    label: 'Story.Load',
    insertText: 'Story.Load(${1:Stories.Story});',
    detail: 'Start another StoryResource from the current story',
    kind: 'snippet',
    sortText: '001a_story_load'
  },
  {
    label: 'Environment.Set',
    insertText: 'Environment.Set(${1:Environments.Environment}, "${2:state_id}");',
    detail: 'Apply an EnvironmentResource state',
    kind: 'snippet',
    sortText: '002_environment_set'
  },
  {
    label: 'Dialogue.Load',
    insertText: 'Dialogue.Load(${1:Dialogues.Dialogue}, audienceId: "${2:player}");',
    detail: 'Load a modular dialogue action',
    kind: 'snippet',
    sortText: '003_dialogue_load'
  },
  {
    label: 'Screen.SetBlack',
    insertText: 'Screen.SetBlack("${1:default}", alpha: ${2:1.0}, durationMillis: ${3:500});',
    detail: 'Drive the screen black overlay',
    kind: 'snippet',
    sortText: '004_screen_set_black'
  },
  {
    label: 'Effect.ApplyToPlayer',
    insertText: 'Effect.ApplyToPlayer("${1:jdh:effect_id}", intensity: ${2:1.0});',
    detail: 'Apply an Echo Effect resource to the player',
    kind: 'snippet',
    sortText: '004a_effect_apply_to_player'
  },
  {
    label: 'Movement.SpeedMultiplier',
    insertText: 'Movement.SpeedMultiplier(Curve(Key(Ticks(${1:0}), ${2:1.0}), Key(Ticks(${3:20}), ${4:0.5})));',
    detail: 'Drive player movement speed in an EffectResource',
    kind: 'snippet',
    sortText: '004b_movement_speed_multiplier'
  },
  {
    label: 'Screen.Tint',
    insertText: 'Screen.Tint(color: Color("${1:#000000}"), opacity: Curve(Key(Ticks(${2:0}), ${3:0.0}), Key(Ticks(${4:20}), ${5:1.0})));',
    detail: 'Drive screen tint in an EffectResource',
    kind: 'snippet',
    sortText: '004c_screen_tint'
  },
  {
    label: 'Sound.PlayAt',
    insertText: 'Sound.PlayAt(time: Ticks(${1:0}), sound: "${2:minecraft:block.note_block.pling}", volume: ${3:1.0}, pitch: ${4:1.0});',
    detail: 'Add a sound event to an EffectResource',
    kind: 'snippet',
    sortText: '004d_sound_play_at'
  },
  {
    label: 'VanillaEffect.Apply',
    insertText: 'VanillaEffect.Apply(time: Ticks(${1:0}), effect: "${2:minecraft:nausea}", duration: Ticks(${3:140}), amplifier: ${4:0});',
    detail: 'Apply a vanilla MobEffect from an EffectResource',
    kind: 'snippet',
    sortText: '004e_vanilla_effect_apply'
  },
  {
    label: 'Player.SpawnAt',
    insertText: 'Player.SpawnAt(${1:x}, ${2:y}, ${3:z}, yaw: ${4:0}, pitch: ${5:0});',
    detail: 'Move the player spawn point/action position',
    kind: 'snippet',
    sortText: '005_player_spawn_at'
  },
  {
    label: 'Npc.SpawnAt',
    insertText: 'Npc.SpawnAt(${1:Sasha}, ${2:x}, ${3:y}, ${4:z}, yaw: ${5:0}, pitch: ${6:0});',
    detail: 'Spawn an NPC through the new external action intrinsic',
    kind: 'snippet',
    sortText: '006_npc_spawn_at'
  },
  {
    label: 'Sleep.EnterDream',
    insertText: 'Sleep.EnterDream(target: "${1:chapter_1_dream}", x: ${2:x}, y: ${3:y}, z: ${4:z}, yaw: ${5:0}, pitch: ${6:0});',
    detail: 'Enter a sleep/dream target',
    kind: 'snippet',
    sortText: '007_sleep_enter_dream'
  },
  {
    label: 'Blackspace.Create',
    insertText: 'Blackspace.Create(instanceId: "${1:chapter_1_blackspace}", playerName: "${2:player}");',
    detail: 'Create a blackspace instance',
    kind: 'snippet',
    sortText: '008_blackspace_create'
  },
  {
    label: 'Player.Teleport',
    insertText: 'Player.Teleport(instanceId: "${1:chapter_1_blackspace}", target: "${2:entry}");',
    detail: 'Teleport the player through blackspace routing',
    kind: 'snippet',
    sortText: '009_player_teleport'
  },
  {
    label: 'NameColor',
    insertText: 'NameColor("${1:#ffffff}")',
    detail: 'NPC display-name color helper',
    kind: 'snippet',
    sortText: '010_name_color'
  },
  {
    label: 'nameColor',
    insertText: 'nameColor("${1:#ffffff}")',
    detail: 'Lowercase NPC display-name color helper',
    kind: 'snippet',
    sortText: '011_name_color_lower'
  },
  {
    label: 'TriggerResource',
    insertText: [
      'namespace JDH.Triggers.${1:Chapter};',
      '',
      'class ${2:ExampleTrigger} : TriggerResource',
      '{',
      '    private Area HomeDoor = Area("minecraft:overworld", ${3:x}, ${4:y}, ${5:z}, ${6:sx}, ${7:sy}, ${8:sz});',
      '',
      '    private void Init()',
      '    {',
      '        Id("${9:jdh:example_trigger}");',
      '        Scope(PLAYER);',
      '        Mode(REPEAT);',
      '        Start(${10:Sequence});',
      '    }',
      '',
      '    private void ${10:Sequence}()',
      '    {',
      '        Wait(PlayerEnter(HomeDoor));',
      '    }',
      '}'
    ].join('\n'),
    detail: 'New Echo TriggerResource template',
    kind: 'snippet',
    sortText: '010_trigger_resource'
  },
  {
    label: 'PlayerProfileTemplate',
    insertText: [
      'namespace EchoContentPlayer.${1:Jdh};',
      '',
      'class ${2:DefaultPlayerProfile} : PlayerProfileTemplate',
      '{',
      '    public override PlayerProfile Build()',
      '    {',
      '        return PlayerProfile(',
      '            id: "${3:jdh:player/default}",',
      '            nameMode: ${4:ACCOUNT},',
      '            spark: ${5:100},',
      '            maxSpark: ${6:100},',
      '            skinMode: ${7:ACCOUNT}',
      '        );',
      '    }',
      '}'
    ].join('\n'),
    detail: 'Echo Content Player .player.es template',
    kind: 'snippet',
    sortText: '011_player_profile_template'
  },
  {
    label: 'WeaponResource',
    insertText: [
      'namespace EchoItemPack.${1:Jdh}.Items.Weapons;',
      '',
      'class ${2:ExampleWeapon} : WeaponResource',
      '{',
      '    public override WeaponDefinition Build()',
      '    {',
      '        return Weapon(',
      '            id: "${3:jdh:example_weapon}",',
      '            name: "${4:Example Weapon}",',
      '            texture: "${5:jdh:item/example_weapon}",',
      '            damage: ${6:7},',
      '            attacksPerSecond: ${7:1.6},',
      '            durability: ${8:250},',
      '            rarity: ${9:COMMON}',
      '        );',
      '    }',
      '}'
    ].join('\n'),
    detail: 'Echo Item Pack .weapon.es template',
    kind: 'snippet',
    sortText: '012_weapon_resource'
  },
  {
    label: 'SeatZoneResource',
    insertText: [
      'namespace EchoSeating.${1:Jdh}.Zones;',
      '',
      'class ${2:ExampleSeatZone} : SeatZoneResource',
      '{',
      '    private SeatZoneId Zone = "${3:jdh:example_seat_zone}";',
      '    private SeatSlotId Chair = "${4:chair}";',
      '',
      '    private void Init()',
      '    {',
      '        Id(Zone);',
      '        Dimension("${5:minecraft:overworld}");',
      '        Area(Box(Position(${6:0}, ${7:64}, ${8:0}), Position(${9:2}, ${10:66}, ${11:2})));',
      '        Detect(${12:RIDING_ANCHOR});',
      '        UnknownSeats(${13:ALLOW});',
      '        MatchRadius(${14:0.85});',
      '        Slot(Chair, Position(${15:1}, ${16:64}, ${17:1}));',
      '    }',
      '}'
    ].join('\n'),
    detail: 'Echo Seating .seat-zone.es template',
    kind: 'snippet',
    sortText: '012a_seat_zone_resource'
  },
  {
    label: 'PlayerHudConfigResource',
    insertText: [
      'namespace JDH.Configs.PlayerHud;',
      '',
      'class ${1:PlayerVitalsHudConfig} : PlayerHudConfigResource',
      '{',
      '    private void Build()',
      '    {',
      '        PlayerHud(',
      '            id: "${2:jdh:player_vitals_hud}",',
      '            referenceWidth: ${3:1920},',
      '            referenceHeight: ${4:1080}',
      '        );',
      '',
      '        Bar(',
      '            id: "${5:echo_player_hud:health_bar}",',
      '            source: "${6:health}",',
      '            enabled: ${7:true},',
      '            drawWhenUnavailable: ${8:true},',
      '            anchor: "${9:BOTTOM_LEFT}",',
      '            x: ${10:36},',
      '            y: ${11:126},',
      '            width: ${12:260},',
      '            height: ${13:18},',
      '            orientation: "${14:HORIZONTAL}",',
      '            fill: "${15:LEFT_TO_RIGHT}",',
      '            background: "${16:0x99000000}",',
      '            borderWidth: ${17:1},',
      '            borderColor: "${18:#CC24151A}",',
      '            gradientStart: "${19:#B23A48}",',
      '            gradientEnd: "${20:#E85D75}"',
      '        );',
      '    }',
      '}'
    ].join('\n'),
    detail: 'Echo Player HUD .es template',
    kind: 'snippet',
    sortText: '012b_player_hud_config_resource'
  },
  {
    label: 'PlayerVitalsConfigResource',
    insertText: [
      'namespace JDH.Configs.PlayerVitals;',
      '',
      'class ${1:PlayerFatigueConfig} : PlayerVitalsConfigResource',
      '{',
      '    private void Build()',
      '    {',
      '        PlayerFatigue(',
      '            id: "${2:jdh:player_fatigue}",',
      '            maximum: ${3:100},',
      '            restedReserveAbove: ${4:0.50},',
      '            tiredReserveAtMost: ${5:0.50},',
      '            tiredReserveAbove: ${6:0.25},',
      '            exhaustedReserveAtMost: ${7:0.25},',
      '            tiredMovementMultiplier: ${8:0.95},',
      '            exhaustedMovementMultiplier: ${9:0.85},',
      '            exhaustedNausea: ${10:0.35}',
      '        );',
      '    }',
      '}'
    ].join('\n'),
    detail: 'Echo Player Vitals .es template',
    kind: 'snippet',
    sortText: '012c_player_vitals_config_resource'
  },
  {
    label: 'DialogueOverlayStyleConfig',
    insertText: [
      'namespace JDH.Configs.EchoDialogue;',
      '',
      'class ${1:DialogueOverlayStyle} : DialogueOverlayStyleConfig',
      '{',
      '    public override DialogueOverlayStyleDef Build()',
      '    {',
      '        return DialogueOverlayStyle(',
      '            textScale: "${2:1.0}",',
      '            textShadow: ${3:true},',
      '            textAppearDelayMs: ${4:80},',
      '            typewriterMsPerCharacter: ${5:34},',
      '            typewriterMinDurationMs: ${6:450},',
      '            typewriterMaxDurationMs: ${7:5000},',
      '            anchorX: "${8:0.5}",',
      '            anchorY: "${9:0.5}",',
      '            anchorOffsetX: ${10:0},',
      '            anchorOffsetY: ${11:20},',
      '            minTextWidth: ${12:30},',
      '            maxTextWidth: ${13:256},',
      '            backgroundColor: "${14:0xB0101010}",',
      '            speakerColor: "${15:0xFF6A00}",',
      '            textColor: "${16:0xFFFFFF}"',
      '        );',
      '    }',
      '}'
    ].join('\n'),
    detail: 'Echo Dialogue overlay style .es template',
    kind: 'snippet',
    sortText: '012d_dialogue_overlay_style_config'
  },
  {
    label: 'DialoguePanelBackgroundOverlay',
    insertText: [
      'return DialoguePanelBackgroundOverlay(',
      '    mode: "${1:sliced}",',
      '    texture: "${2:jdh:textures/echo_dialogue/gui/panel/dialogue_panel.png}",',
      '    textureWidth: ${3:29},',
      '    textureHeight: ${4:28},',
      '    sliceLeft: ${5:6},',
      '    sliceRight: ${6:6},',
      '    sliceTop: ${7:6},',
      '    sliceBottom: ${8:8}',
      ');'
    ].join('\n'),
    detail: 'Echo Dialogue panel background overlay config call',
    kind: 'snippet',
    sortText: '012e_dialogue_panel_background_overlay'
  },
  {
    label: 'DialogueChoiceBackgroundOverlay',
    insertText: [
      'return DialogueChoiceBackgroundOverlay(',
      '    mode: "${1:sliced}",',
      '    texture: "${2:jdh:textures/echo_dialogue/gui/panel/dialogue_choice.png}",',
      '    textureWidth: ${3:29},',
      '    textureHeight: ${4:28},',
      '    sliceLeft: ${5:6},',
      '    sliceRight: ${6:6},',
      '    sliceTop: ${7:6},',
      '    sliceBottom: ${8:8}',
      ');'
    ].join('\n'),
    detail: 'Echo Dialogue choice background overlay config call',
    kind: 'snippet',
    sortText: '012f_dialogue_choice_background_overlay'
  },
  {
    label: 'PortraitOverlayConfig',
    insertText: [
      'namespace JDH.Configs.EchoAnimation;',
      '',
      'class ${1:CharacterPortraitOverlay} : PortraitOverlayConfig',
      '{',
      '    public override PortraitOverlayDef Build()',
      '    {',
      '        return PortraitOverlay("${2:jdh:character}",',
      '            centerX: "${3:0.50}",',
      '            centerY: "${4:0.49}",',
      '            offsetX: ${5:0},',
      '            offsetY: ${6:0},',
      '            facePixels: "${7:4}",',
      '            layers: [',
      '                Layer("${8:pupil_left}", placeX: "${9:-0.062}", placeY: "${10:0.002}", scaleX: "${11:0.50}", scaleY: "${12:0.50}")',
      '            ],',
      '            idle: [',
      '                Blink "${13:eye_closure}"',
      '                {',
      '                    Travel(${14:20}, ${15:-20});',
      '                    Gaze(${16:1}, ${17:1});',
      '                    Idle(${18:true});',
      '                    PeriodTicks(${19:50});',
      '                    CloseTicks(${20:2});',
      '                    HoldTicks(${21:2});',
      '                    OpenTicks(${22:2});',
      '                }',
      '            ]',
      '        );',
      '    }',
      '}'
    ].join('\n'),
    detail: 'Echo Animation portrait overlay .es template',
    kind: 'snippet',
    sortText: '012e_portrait_overlay_config'
  },
  {
    label: 'Vehicle.EnsureSpawn',
    insertText: 'Vehicle.EnsureSpawn("${1:sasha_car}");',
    detail: 'Создать или проверить стабильный vehicle spawn',
    kind: 'snippet',
    sortText: '013_vehicle_ensure'
  },
  {
    label: 'Vehicle.Trip',
    insertText: [
      'Vehicle.Trip(',
      '    ${1:Sasha},',
      '    "${2:sasha_car}",',
      '    "${3:from_road_segment}",',
      '    "${4:to_road_segment}",',
      '    tripId: "${5:sasha_home}",',
      '    category: "car",',
      '    backend: "native_preferred"',
      ');'
    ].join('\n'),
    detail: 'Запустить road vehicle trip',
    kind: 'snippet',
    sortText: '014_vehicle_trip'
  },
  {
    label: 'Vehicle.Sequence',
    insertText: [
      'Vehicle.Sequence(',
      '    ${1:Sasha},',
      '    "${2:sasha_car}",',
      '    "${3:from_road_segment}",',
      '    "${4:destination_id}",',
      '    sequenceId: "${5:arrival_sequence}",',
      '    passenger: "player",',
      '    passengerSeat: "seat-1",',
      '    cinematic: "${6:camera_path_id}"',
      ');'
    ].join('\n'),
    detail: 'Запустить cinematic vehicle sequence',
    kind: 'snippet',
    sortText: '015_vehicle_sequence'
  },
  {
    label: 'Npc.EnterVehicleAt',
    insertText: 'Npc.EnterVehicleAt(${1:Sasha}, new Vector3(${2:x}, ${3:y}, ${4:z}), role: "driver", seat: "driver", radius: ${5:3});',
    detail: 'Посадить NPC в транспорт рядом с координатами',
    kind: 'snippet',
    sortText: '016_npc_vehicle_at'
  },
  {
    label: 'Npc.MoveTo',
    insertText: [
      'Npc.MoveTo(',
      '    ${1:Sasha},',
      '    new Vector3(${2:x}, ${3:y}, ${4:z}),',
      '    speed: ${5:1.2},',
      '    completionDistance: ${6:0.7},',
      '    openDoors: true,',
      '    targetSearchRadius: ${7:1}',
      ');'
    ].join('\n'),
    detail: 'Переместить NPC к точке',
    kind: 'snippet',
    sortText: '017_npc_move_to'
  },
  {
    label: 'Npc.Move',
    insertText: 'Npc.Move(${1:Sasha}, ${2:x}, ${3:y}, ${4:z});',
    detail: 'Переместить NPC к координатам x/y/z',
    kind: 'snippet',
    sortText: '017a_npc_move'
  },
  {
    label: 'Npc.Path',
    insertText: [
      'Npc.Path(',
      '    ${1:Sasha},',
      '    [',
      '        new Vector3(${2:x1}, ${3:y1}, ${4:z1}),',
      '        new Vector3(${5:x2}, ${6:y2}, ${7:z2})',
      '    ],',
      '    speed: ${8:1.15},',
      '    completionDistance: ${9:0.7},',
      '    openDoors: true,',
      '    canFloat: ${10:false},',
      '    repathAfterTicks: ${11:30},',
      '    maxRepathAttempts: ${12:3}',
      ');'
    ].join('\n'),
    detail: 'Провести NPC по маршруту',
    kind: 'snippet',
    sortText: '018_npc_path'
  },
  {
    label: 'Npc.MoveToPlayer',
    insertText: [
      'Npc.MoveToPlayer(',
      '    ${1:Sasha},',
      '    stopDistance: ${2:2.3},',
      '    speed: ${3:1.1},',
      '    lookAtPlayer: true,',
      '    thenDialogue: ${4:Dialogues.Dialogue}',
      ');'
    ].join('\n'),
    detail: 'Подвести NPC к игроку',
    kind: 'snippet',
    sortText: '019_npc_move_to_player'
  },
  {
    label: 'Npc.Rotate',
    insertText: [
      'var ${1:targetRotate} = new Vector3(${2:0}, ${3:135}, ${4:0});',
      '',
      'Npc.Rotate(',
      '    ${5:Sasha},',
      '    ${1:targetRotate},',
      '    speed: ${6:4.0},',
      '    lock: ${7:true}',
      ');'
    ].join('\n'),
    detail: 'Повернуть NPC по Vector3: x=pitch, y=yaw, z=roll',
    kind: 'snippet',
    sortText: '020_npc_rotate'
  },
  {
    label: 'Npc.OpenDoor',
    insertText: 'Npc.OpenDoor(${1:Sasha}, new Vector3(${2:x}, ${3:y}, ${4:z}));',
    detail: 'Открыть дверь/блок от имени NPC',
    kind: 'snippet',
    sortText: '021_npc_open_door'
  },
  {
    label: 'Npc.UseBlock',
    insertText: 'Npc.UseBlock(${1:Sasha}, new Vector3(${2:x}, ${3:y}, ${4:z}), action: "${5:use}");',
    detail: 'Выполнить block interaction от имени NPC',
    kind: 'snippet',
    sortText: '022_npc_use_block'
  },
  {
    label: 'Navigation.SetTarget',
    insertText: [
      'Navigation.SetTarget(',
      '    "${1:quest.main_gate}",',
      '    x: ${2:x},',
      '    y: ${3:y},',
      '    z: ${4:z},',
      '    label: "${5:Main Gate}",',
      '    dimensionId: "minecraft:overworld",',
      '    profile: "${6:prefer_roads}"',
      ');'
    ].join('\n'),
    detail: 'Поставить GPS/navigation цель игроку',
    kind: 'snippet',
    sortText: '023_navigation_target'
  },
  {
    label: 'Player.Pose',
    insertText: 'Player.Pose("${1:none}");',
    detail: 'Поставить presentation pose игрока',
    kind: 'snippet',
    sortText: '024_player_pose'
  },
  {
    label: 'InventoryHasItem',
    insertText: 'InventoryHasItem("${1:chapter_1.item.found}", ${2:Items.SparkGrass}, count: ${3:1})',
    detail: 'Триггер, когда у игрока есть предмет',
    kind: 'snippet',
    sortText: '025_trigger_inventory_has'
  },
  {
    label: 'InventoryMissing',
    insertText: 'InventoryMissing("${1:chapter_1.item.spent}", ${2:Items.SparkGrass}, count: ${3:1})',
    detail: 'Триггер, когда у игрока нет предмета',
    kind: 'snippet',
    sortText: '026_trigger_inventory_missing'
  },
  {
    label: 'MobKilled',
    insertText: 'MobKilled("${1:chapter_1.kill_mob}", "${2:minecraft:zombie}")',
    detail: 'Триггер убийства mob/entity',
    kind: 'snippet',
    sortText: '027_trigger_mob_killed'
  },
  {
    label: 'Do',
    insertText: [
      'Do(',
      '    ${1:EffectStatement}',
      ')'
    ].join('\n'),
    detail: 'Scene step, который применяет effects',
    kind: 'snippet',
    sortText: '028_scene_do'
  }
];

const SCENE_BUILDER_SNIPPETS = [
  ['Id', 'Id("${1:scene_id}")', 'Override scene id'],
  ['LockPlayer', 'LockPlayer(${1:true})', 'Lock player movement'],
  ['Skippable', 'Skippable(${1:true})', 'Allow skipping'],
  ['Replayable', 'Replayable(${1:true})', 'Allow replaying'],
  ['Camera', 'Camera(${1:CameraDef})', 'Set opening camera'],
  ['OpeningCamera', 'OpeningCamera(${1:CameraDef})', 'Set opening camera alias'],
  ['Cutscene', 'Cutscene(${1:CutsceneDef})', 'Attach cutscene settings'],
  ['Alias', 'Alias("${1:s}", ${2:SpeakerRef})', 'Attach one scene speaker alias'],
  ['SpeakerAlias', 'SpeakerAlias("${1:s}", ${2:SpeakerRef})', 'Attach one scene speaker alias'],
  ['Aliases', 'Aliases(${1:Alias("s", SpeakerRef)})', 'Attach scene speaker aliases'],
  ['SpeakerAliases', 'SpeakerAliases(Alias("${1:s}", ${2:SpeakerRef}))', 'Attach scene speaker aliases'],
  ['Start', 'Start(${1:EffectStatement})', 'Стартовые эффекты сцены'],
  ['StartEffects', 'StartEffects(${1:EffectStatement})', 'Alias стартовых эффектов сцены'],
  ['Complete', 'Complete(${1:EffectStatement})', 'Эффекты завершения сцены'],
  ['CompleteEffects', 'CompleteEffects(${1:EffectStatement})', 'Alias эффектов завершения сцены'],
  ['Abort', 'Abort(${1:EffectStatement})', 'Эффекты отмены сцены'],
  ['AbortEffects', 'AbortEffects(${1:EffectStatement})', 'Alias эффектов отмены сцены'],
  ['Step', 'Step("${1:type}", Field("${2:key}", ${3:value}))', 'Add a raw step'],
  ['Steps', 'Steps(${1:Do(EffectStatement)})', 'Attach scene steps']
];

const DIALOGUE_BUILDER_SNIPPETS = [
  ['Id', 'Id("${1:dialogue_id}")', 'Override dialogue id'],
  ['Cutscene', 'Cutscene(${1:CutsceneDef})', 'Attach cutscene settings']
];

const MEMBER_SNIPPETS = {
  Story: [
    ['Set', 'Set("${1:state_key}", ${2:true});', 'Write story state from OnStart linear code']
  ],
  Environment: [
    ['Set', 'Set(${1:Environments.Environment}, "${2:state_id}");', 'Apply an EnvironmentResource state']
  ],
  Screen: [
    ['SetBlack', 'SetBlack("${1:default}", alpha: ${2:1.0}, durationMillis: ${3:500});', 'Drive the black screen overlay']
  ],
  Sleep: [
    ['Arm', 'Arm(scenario: "${1:default}");', 'Arm a sleep scenario'],
    ['Disarm', 'Disarm();', 'Disarm the active sleep scenario'],
    ['CloseEyes', 'CloseEyes(duration: ${1:20});', 'Close the sleep overlay eyes'],
    ['WaitUntilClosed', 'WaitUntilClosed();', 'Wait until eyes are closed'],
    ['EnterDream', 'EnterDream(target: "${1:chapter_1_dream}", x: ${2:x}, y: ${3:y}, z: ${4:z}, yaw: ${5:0}, pitch: ${6:0});', 'Enter a dream target'],
    ['OpenEyes', 'OpenEyes(duration: ${1:20}, showHud: ${2:true});', 'Open the sleep overlay eyes'],
    ['WaitUntilOpen', 'WaitUntilOpen();', 'Wait until eyes are open'],
    ['Wait', 'Wait(seconds: ${1:1.0});', 'Wait in the sleep scenario'],
    ['Blink', 'Blink(${1:1}, duration: ${2:8}, closedWait: ${3:0.12}, openWait: ${4:0.18});', 'Blink the sleep eye overlay in OnStart'],
    ['WaitSeconds', 'WaitSeconds(seconds: ${1:1.0});', 'Wait in seconds in the sleep scenario'],
    ['ReturnToBed', 'ReturnToBed(mode: "${1:default}");', 'Return the player to bed'],
    ['CompleteNight', 'CompleteNight();', 'Complete the night sleep flow'],
    ['Complete', 'Complete();', 'Complete the sleep flow'],
    ['Cancel', 'Cancel();', 'Cancel the sleep flow'],
    ['IsActive', 'IsActive(output: "${1:sleepActive}");', 'Write whether sleep is active'],
    ['IsArmed', 'IsArmed(output: "${1:sleepArmed}");', 'Write whether sleep is armed'],
    ['IsEyesClosed', 'IsEyesClosed(output: "${1:eyesClosed}");', 'Write whether eyes are closed']
  ],
  Blackspace: [
    ['Create', 'Create(instanceId: "${1:chapter_1_blackspace}", playerName: "${2:player}");', 'Create a blackspace instance']
  ],
  PlayerSleep: PLAYER_SLEEP_SELECTORS.map(selector => [
    selector,
    `${selector}(${selector === 'Confirmed' ? '${1:SleepScenario}' : '${1:SleepScenario}'})`,
    `Player sleep subscription selector: ${selector}`
  ]),
  Action: [
    ['Label', 'Label("${1:Prompt text}")', 'Set the prompt text'],
    ['LabelKey', 'LabelKey("${1:intro.action}")', 'Set a localization key for the prompt'],
    ['ActionKey', 'ActionKey("${1:intro.action}")', 'Set the stable action signal key'],
    ['Priority', 'Priority(${1:0})', 'Set action ordering priority'],
    ['Cost', 'Cost(Money(${1:10}))', 'Require a money cost'],
    ['UnavailableLabel', 'UnavailableLabel("${1:Not available}")', 'Set failure text'],
    ['UnavailableLabelKey', 'UnavailableLabelKey("${1:intro.action.unavailable}")', 'Set localized failure text'],
    ['SuccessLabel', 'SuccessLabel("${1:Done}")', 'Set success text'],
    ['SuccessLabelKey', 'SuccessLabelKey("${1:intro.action.success}")', 'Set localized success text'],
    ['Effects', 'Effects(${1:EffectStatement})', 'Run effects when action is used'],
    ['FireOnce', 'FireOnce(${1:true})', 'Mark the generated action event as one-shot'],
    ['Visible', 'Visible();', 'Expose an action from an if block']
  ],
  Speaker: [
    ['DisplayName', 'DisplayName("${1:Speaker}")', 'Set speaker display name'],
    ['DisplayNameKey', 'DisplayNameKey("${1:speaker.name}")', 'Set localized display name'],
    ['NameColor', 'NameColor("${1:#ffffff}")', 'Set speaker name color'],
    ['Portrait', 'Portrait(${1:Portraits.Buyer})', 'Set portrait id'],
    ['TalkDistance', 'TalkDistance(${1:4.0})', 'Set interaction/talk distance'],
    ['InteractionDialogues', 'InteractionDialogues(${1:Dialogues.Dialogue})', 'Attach interaction dialogues'],
    ['Behavior', 'Behavior(${1:BehaviorDef})', 'Attach NPC behavior'],
    ['Actions', 'Actions(${1:ActionDef})', 'Attach interaction actions']
  ],
  Event: [
    ['Id', 'Id("${1:event_id}")', 'Override event id'],
    ['Trigger', 'Trigger(${1:Manual("event.key")})', 'Set event trigger'],
    ['Effects', 'Effects(${1:EffectStatement})', 'Attach effects'],
    ['FireOnce', 'FireOnce(${1:true})', 'Run once after the first successful trigger'],
    ['CooldownTicks', 'CooldownTicks(${1:1200})', 'Throttle repeated event triggers']
  ],
  PlayerProfile: [
    ['Id', 'id: "${1:jdh:player/default}"', 'Stable player profile template id'],
    ['NameMode', 'nameMode: ${1:ACCOUNT}', 'ACCOUNT or CUSTOM display-name mode'],
    ['DisplayName', 'displayName: "${1:Display Name}"', 'Custom display name'],
    ['Spark', 'spark: ${1:100}', 'Current spark amount'],
    ['MaxSpark', 'maxSpark: ${1:100}', 'Maximum spark amount'],
    ['SkinMode', 'skinMode: ${1:ACCOUNT}', 'ACCOUNT or RESOURCE skin mode'],
    ['SkinId', 'skinId: "${1:echo_content_player:player/base}"', 'Authored resource skin id'],
    ['SkinModel', 'skinModel: ${1:CLASSIC}', 'CLASSIC or SLIM skin model']
  ],
  Weapon: [
    ['Id', 'id: "${1:jdh:weapon_id}"', 'Stable weapon id'],
    ['Name', 'name: "${1:Weapon Name}"', 'Display name'],
    ['Texture', 'texture: "${1:jdh:item/weapon}"', 'Item texture id'],
    ['Description', 'description: "${1:Description}"', 'Description text'],
    ['Tooltip', 'tooltip: ["${1:Tooltip line}"]', 'Tooltip lines'],
    ['Damage', 'damage: ${1:7}', 'Attack damage'],
    ['AttacksPerSecond', 'attacksPerSecond: ${1:1.6}', 'Attack speed'],
    ['Durability', 'durability: ${1:250}', 'Durability points'],
    ['Rarity', 'rarity: ${1:COMMON}', 'COMMON, UNCOMMON, RARE, or EPIC']
  ],
  Camera: [
    ['Preset', 'Preset("${1:third_person_back}")', 'Use a named camera preset'],
    ['PresetId', 'PresetId("${1:third_person_back}")', 'Use a named camera preset id'],
    ['Position', 'Position(Vec(${1:x}, ${2:y}, ${3:z}))', 'Set camera position'],
    ['Focus', 'Focus(Vec(${1:x}, ${2:y}, ${3:z}))', 'Set camera focus point'],
    ['PositionSource', 'PositionSource("${1:player}")', 'Set position source'],
    ['FadeIn', 'FadeIn(${1:20})', 'Set fade-in ticks'],
    ['FadeOut', 'FadeOut(${1:20})', 'Set fade-out ticks'],
    ['FadeInTicks', 'FadeInTicks(${1:20})', 'Set fade-in ticks'],
    ['FadeOutTicks', 'FadeOutTicks(${1:20})', 'Set fade-out ticks'],
    ['BlendTicks', 'BlendTicks(${1:10})', 'Set camera blend ticks'],
    ['DurationTicks', 'DurationTicks(${1:80})', 'Set camera cue duration'],
    ['TargetYaw', 'TargetYaw(${1:180})', 'Set target yaw'],
    ['TargetPitch', 'TargetPitch(${1:8})', 'Set target pitch'],
    ['LockMouse', 'LockMouse(${1:true})', 'Lock mouse while camera is active'],
    ['LockControls', 'LockControls(${1:true})', 'Lock controls while camera is active']
  ],
  Animation: [
    ['Id', 'Id("${1:animation_id}")', 'Override animation id'],
    ['LengthTicks', 'LengthTicks(${1:80})', 'Set animation length in ticks'],
    ['Loop', 'Loop(${1:true})', 'Mark animation as looping'],
    ['End', 'End("${1:hold}")', 'Set animation end behavior'],
    ['ExitAnimationId', 'ExitAnimationId(${1:Animations.NpcOfferItemHold})', 'Set exit animation'],
    ['Interpolation', 'Interpolation("${1:linear}")', 'Set interpolation mode'],
    ['BlendMode', 'BlendMode("${1:replace}")', 'Set blend mode'],
    ['Editor', 'Editor(${1:Obj(Field("key", "value"))})', 'Attach editor metadata'],
    ['Bone', 'Bone("${1:head}", ${2:Frame(Pose(tick: 0))})', 'Add one bone track'],
    ['Bones', 'Bones(${1:Bone("head").Frames(Frame(Pose(tick: 0)))})', 'Add multiple bone tracks'],
    ['Gun', 'Gun(${1:[Frame(Pose(tick: 0))]})', 'Attach gun animation track'],
    ['Items', 'Items(${1:Obj(Field("slot", Obj()))})', 'Attach item animation data'],
    ['Ik', 'Ik(${1:Obj(Field("enabled", true))})', 'Attach IK data']
  ],
  FaceAnimation: [
    ['Id', 'Id("${1:face_animation_id}")', 'Override face animation id'],
    ['LengthTicks', 'LengthTicks(${1:20})', 'Set face animation length in ticks'],
    ['LengthSeconds', 'LengthSeconds(${1:0.3})', 'Set face animation length in seconds'],
    ['Loop', 'Loop(${1:true})', 'Mark face animation as looping'],
    ['End', 'End("${1:hold}")', 'Set face animation end behavior'],
    ['Interpolation', 'Interpolation("${1:linear}")', 'Set interpolation mode'],
    ['BlinkTravelY', 'BlinkTravelY(Field("${1:upperEyelids}", ${2:1.0}))', 'Attach blink travel values'],
    ['Part', 'Part("${1:upperEyelids}").Frames(${2:Frame("start", FacePose())})', 'Add one face part track'],
    ['Parts', 'Parts(${1:Part("upperEyelids").Frames(Frame("start", FacePose()))})', 'Add multiple face part tracks']
  ],
  CameraPath: [
    ['Id', 'Id("${1:camera_path_id}")', 'Override camera path id'],
    ['Format', 'Format("${1:authored}")', 'Set camera path format'],
    ['LengthTicks', 'LengthTicks(${1:200})', 'Set camera path length'],
    ['Sample', 'Sample(${1:0}, Vec(${2:x}, ${3:y}, ${4:z}), yaw: ${5:0}, pitch: ${6:0})', 'Add one camera sample'],
    ['Samples', 'Samples(${1:CameraSample(0, Vec(x, y, z))})', 'Add multiple camera samples']
  ],
  Behavior: [
    ['LookAtPlayers', 'LookAtPlayers(${1:true})', 'Make NPC look at nearby players'],
    ['RecoveryTicks', 'RecoveryTicks(${1:40})', 'Set behavior recovery ticks'],
    ['Idle', 'Idle(${1:States.Idle}, ${2:Animations.LocomotionIdle})', 'Set idle state and optional animation'],
    ['Schedules', 'Schedules(${1:Schedule(0, 24000)})', 'Attach behavior schedules']
  ],
  Schedule: [
    ['State', 'State(${1:States.Idle})', 'Set scheduled state'],
    ['Animation', 'Animation(${1:Animations.LocomotionIdle})', 'Set scheduled animation'],
    ['Route', 'Route(${1:Vec(x, y, z)})', 'Set scheduled route points']
  ],
  Cutscene: [
    ['Actor', 'Actor(${1:true})', 'Enable or configure cutscene actor'],
    ['Input', 'Input("${1:cinematic}")', 'Set input mode'],
    ['Release', 'Release("${1:scene_end}")', 'Set release mode'],
    ['SuppressHud', 'SuppressHud(${1:true})', 'Hide HUD during cutscene'],
    ['AllowDialogueInput', 'AllowDialogueInput(${1:true})', 'Allow dialogue input during cutscene'],
    ['LogTag', 'LogTag("${1:scene}")', 'Set debug log tag'],
    ['Camera', 'Camera(${1:CameraDef})', 'Attach cutscene camera'],
    ['CameraPath', 'CameraPath(${1:CameraPathCue("path_id")})', 'Attach camera path cue'],
    ['Start', 'Start(${1:Cutscenes.Cutscene});', 'Start a cutscene command'],
    ['Play', 'Play(${1:Cutscenes.Cutscene});', 'Play a cutscene command'],
    ['Wait', 'Wait(${1:Cutscenes.Cutscene});', 'Wait for a cutscene command'],
    ['WaitPrepared', 'WaitPrepared(${1:Cutscenes.Cutscene});', 'Wait for a prepared cutscene command'],
    ['Stop', 'Stop(${1:Cutscenes.Cutscene});', 'Stop a cutscene command']
  ],
  Localization: [
    ['Locale', 'Locale("${1:en_us}")', 'Set localization locale'],
    ['Default', 'Default(${1:true})', 'Mark localization as default'],
    ['Prefix', 'Prefix("${1:intro}")', 'Set key prefix'],
    ['Entry', 'Entry("${1:key}", "${2:Text}")', 'Add one localization entry'],
    ['Entries', 'Entries(${1:Entry("key", "Text")})', 'Add multiple localization entries']
  ],
  Line: [
    ['Id', 'Id("${1:line_id}")', 'Set line id'],
    ['Key', 'Key("${1:dialogue.line}")', 'Set localization key'],
    ['TextKey', 'TextKey("${1:dialogue.line}")', 'Set localization key'],
    ['Delay', 'Delay(${1:20})', 'Set line delay'],
    ['DelayTicks', 'DelayTicks(${1:20})', 'Set line delay in ticks'],
    ['Thought', 'Thought(${1:true})', 'Mark line as thought'],
    ['Voice', 'Voice("${1:minecraft:entity.villager.ambient}")', 'Set voice sound'],
    ['Portrait', 'Portrait(${1:Portraits.Buyer})', 'Set portrait'],
    ['Fx', 'Fx("${1:effect_id}")', 'Set line effect'],
    ['NameColor', 'NameColor("${1:#ffffff}")', 'Set displayed name color'],
    ['Anim', 'Anim(${1:Animations.NpcOfferItemHold})', 'Set body animation'],
    ['Animation', 'Animation(${1:Animations.NpcOfferItemHold})', 'Set body animation'],
    ['SetAnimation', 'SetAnimation(${1:Animations.NpcOfferItemHold})', 'Set body animation'],
    ['Face', 'Face(${1:FaceAnimations.DialogueFaceSoft})', 'Set face animation'],
    ['SetAnimationFace', 'SetAnimationFace(${1:FaceAnimations.DialogueFaceSoft})', 'Set face animation'],
    ['SetFaceAnimation', 'SetFaceAnimation(${1:FaceAnimations.DialogueFaceSoft})', 'Set face animation alias'],
    ['SetAnimationFaceClear', 'SetAnimationFaceClear()', 'Clear face animation'],
    ['FaceStop', 'FaceStop(${1:true})', 'Stop current face animation'],
    ['StopFace', 'StopFace(${1:true})', 'Stop current face animation'],
    ['StopFaceAnimation', 'StopFaceAnimation()', 'Stop current face animation'],
    ['Path', 'Path(${1:NpcPath(Sasha, [Vec(x, y, z)])})', 'Attach NPC path to line'],
    ['Effects', 'Effects(${1:EffectStatement})', 'Attach line effects']
  ],
  Choice: [
    ['Text', 'Text("${1:Choice text}")', 'Set choice text'],
    ['Label', 'Label("${1:Choice text}")', 'Set choice text'],
    ['Key', 'Key("${1:dialogue.choice}")', 'Set localization key'],
    ['TextKey', 'TextKey("${1:dialogue.choice}")', 'Set localization key'],
    ['If', 'If(${1:condition})', 'Attach choice condition'],
    ['Next', 'Next("${1:next_node}")', 'Go to another node'],
    ['Goto', 'Goto("${1:next_node}")', 'Go to another node'],
    ['Effects', 'Effects(${1:EffectStatement})', 'Attach choice effects']
  ],
  Node: [
    ['Speaker', 'Speaker(${1:SpeakerRef})', 'Set node speaker'],
    ['Line', 'Line(${1:SpeakerRef}, "${2:Text}")', 'Add one line'],
    ['LineAfterMove', 'LineAfterMove(${1:SpeakerRef}, "${2:Text}")', 'Add one line after the matching NPC move arrives'],
    ['Lines', 'Lines(${1:Line(SpeakerRef, "Text")})', 'Add multiple lines'],
    ['Choice', 'Choice("${1:choice_id}", "${2:Text}", "${3:next_node}")', 'Add one choice'],
    ['Choices', 'Choices(${1:Choice("choice_id", "Text", "next_node")})', 'Add multiple choices'],
    ['Next', 'Next("${1:next_node}")', 'Set next node'],
    ['Goto', 'Goto("${1:next_node}")', 'Set next node'],
    ['EnterEffects', 'EnterEffects(${1:EffectStatement})', 'Attach node enter effects'],
    ['Timer', 'Timer(${1:Timer(100).OnTimeout("timeout_choice")})', 'Attach dialogue timer'],
    ['Kind', 'Kind("${1:normal}")', 'Set node kind']
  ],
  Timer: [
    ['OnTimeout', 'OnTimeout("${1:choice_id}")', 'Choose this option when timer expires'],
    ['DefaultChoice', 'DefaultChoice("${1:choice_id}")', 'Set default timer choice']
  ],
  Bone: [
    ['Frame', 'Frame(${1:Pose(tick: 0)})', 'Add one animation frame'],
    ['Frames', 'Frames(${1:Frame(Pose(tick: 0))})', 'Add animation frames'],
    ['Ik', 'Ik(${1:true})', 'Enable IK for this bone track']
  ],
  Part: [
    ['Frame', 'Frame("${1:start}", ${2:FacePose()})', 'Add one face part frame'],
    ['Frames', 'Frames(${1:Frame("start", FacePose())})', 'Add face part frames']
  ],
  StoryIndex: [
    ['Speakers', 'Speakers("${1:speakers/sasha.es}")', 'Index speaker files'],
    ['Npcs', 'Npcs("${1:npcs/sasha.es}")', 'Index NPC files'],
    ['Dialogues', 'Dialogues("${1:dialogues/intro.es}")', 'Index dialogue files'],
    ['Scenes', 'Scenes("${1:scenes/intro.es}")', 'Index scene files'],
    ['Events', 'Events("${1:events/intro.es}")', 'Index event files'],
    ['Animations', 'Animations("${1:animations/idle.es}")', 'Index animation files'],
    ['FaceAnimations', 'FaceAnimations("${1:face_animations/blink.es}")', 'Index face animation files'],
    ['CameraPaths', 'CameraPaths("${1:camera_paths/path.es}")', 'Index camera path files'],
    ['Localization', 'Localization("${1:localization/en_us.es}")', 'Index localization files']
  ],
  Vehicle: [
    ['EnsureSpawn', 'EnsureSpawn("${1:sasha_car}");', 'Ensure a stable vehicle spawn exists'],
    ['Trip', 'Trip(${1:Sasha}, "${2:sasha_car}", "${3:from_road_segment}", "${4:to_road_segment}", tripId: "${5:sasha_home}", category: "car", backend: "native_preferred");', 'Start a road vehicle trip'],
    ['Sequence', 'Sequence(${1:Sasha}, "${2:sasha_car}", "${3:from_road_segment}", "${4:destination_id}", sequenceId: "${5:arrival_sequence}", passenger: "player", passengerSeat: "seat-1", cinematic: "${6:camera_path_id}");', 'Start a cinematic vehicle sequence']
  ],
  Npc: [
    ['SpawnAt', 'SpawnAt(${1:Sasha}, ${2:x}, ${3:y}, ${4:z}, yaw: ${5:0}, pitch: ${6:0});', 'Spawn an NPC through the new external action intrinsic'],
    ['SleepAt', 'SleepAt(${1:Sasha}, ${2:x}, ${3:y}, ${4:z}, expectedBlock: "${5:minecraft:red_bed}");', 'Put an NPC to sleep at a bed position'],
    ['WakeUp', 'WakeUp(${1:Sasha});', 'Wake an NPC from SleepAt'],
    ['SetForm', 'SetForm(${1:Sasha}, ${2:NpcForms.Form});', 'Change an NPC form'],
    ['Interact', 'Interact(${1:Sasha}, ${2:x}, ${3:y}, ${4:z}, face: "AUTO", hand: "MAIN_HAND");', 'Run an NPC interaction action'],
    ['StopInteraction', 'StopInteraction(npc: ${1:Sasha});', 'Stop an NPC interaction action'],
    ['StandUp', 'StandUp(${1:Sasha});', 'Снять NPC с interaction seat anchor'],
    ['Stop', 'Stop(npc: ${1:Sasha});', 'Stop the active NPC action'],
    ['Spawn', 'Spawn(${1:Sasha}, ${2:Overworld}, ${3:x}, ${4:y}, ${5:z});', 'Заспавнить NPC в измерении и координатах'],
    ['SpawnNearPlayer', 'SpawnNearPlayer(${1:Sasha}, forwardOffset: ${2:3.0}, sideOffset: ${3:0.0}, yOffset: ${4:0.0});', 'Spawn an NPC relative to the player'],
    ['Despawn', 'Despawn(${1:Sasha});', 'Despawn an NPC'],
    ['MoveTo', 'MoveTo(${1:Sasha}, new Vector3(${2:x}, ${3:y}, ${4:z}), speed: ${5:1.2}, completionDistance: ${6:0.7}, openDoors: true);', 'Move an NPC to a fixed point'],
    ['Move', 'Move(${1:Sasha}, ${2:x}, ${3:y}, ${4:z});', 'Move an NPC to block coordinates'],
    ['WalkTo', 'WalkTo(${1:Sasha}, new Vector3(${2:x}, ${3:y}, ${4:z}), speed: ${5:1.2}, completionDistance: ${6:0.7}, openDoors: true);', 'Alias for MoveTo'],
    ['MoveToPlayer', 'MoveToPlayer(${1:Sasha}, stopDistance: ${2:2.3}, speed: ${3:1.1}, lookAtPlayer: true);', 'Move an NPC toward the player'],
    ['ApproachPlayer', 'ApproachPlayer(${1:Sasha}, stopDistance: ${2:2.3}, speed: ${3:1.1}, lookAtPlayer: true);', 'Alias for MoveToPlayer'],
    ['WalkToPlayer', 'WalkToPlayer(${1:Sasha}, stopDistance: ${2:2.3}, speed: ${3:1.1}, lookAtPlayer: true);', 'Alias for MoveToPlayer'],
    ['Path', 'Path(${1:Sasha}, [new Vector3(${2:x}, ${3:y}, ${4:z})], speed: ${5:1.15}, completionDistance: ${6:0.7}, openDoors: true);', 'Move an NPC along a semantic route'],
    ['Stay', 'Stay(${1:Sasha}, ${2:true});', 'Toggle NPC stay-still behavior'],
    ['Rotate', 'Rotate(${1:Sasha}, new Vector3(${2:0}, ${3:135}, ${4:0}), speed: ${5:4.0}, lock: ${6:true});', 'Rotate an NPC: x=pitch, y=yaw, z=roll'],
    ['Turn', 'Turn(${1:Sasha}, new Vector3(${2:0}, ${3:135}, ${4:0}), speed: ${5:4.0});', 'Alias for Rotate'],
    ['UseBlock', 'UseBlock(${1:Sasha}, new Vector3(${2:x}, ${3:y}, ${4:z}), action: "${5:use}");', 'Use or interact with a block as an NPC'],
    ['InteractBlock', 'InteractBlock(${1:Sasha}, new Vector3(${2:x}, ${3:y}, ${4:z}), action: "${5:use}");', 'Alias for UseBlock'],
    ['OpenDoor', 'OpenDoor(${1:Sasha}, new Vector3(${2:x}, ${3:y}, ${4:z}));', 'Open a door/block as an NPC'],
    ['CloseDoor', 'CloseDoor(${1:Sasha}, new Vector3(${2:x}, ${3:y}, ${4:z}));', 'Close a door/block as an NPC'],
    ['ToggleDoor', 'ToggleDoor(${1:Sasha}, new Vector3(${2:x}, ${3:y}, ${4:z}));', 'Toggle a door/block as an NPC'],
    ['UseDoor', 'UseDoor(${1:Sasha}, new Vector3(${2:x}, ${3:y}, ${4:z}));', 'Use a door/block as an NPC'],
    ['LookAtPlayers', 'LookAtPlayers(${1:Sasha}, ${2:true});', 'Toggle NPC look-at-players behavior'],
    ['ClearLookAtPlayers', 'ClearLookAtPlayers(${1:Sasha});', 'Clear NPC look-at-players override'],
    ['HoldItem', 'HoldItem(${1:Sasha}, ${2:Items.SparkGrass}, count: ${3:1}, hand: ${4:Hand.Main});', 'Set a specific NPC held item'],
    ['ClearHeldItem', 'ClearHeldItem(${1:Sasha}, hand: ${2:Hand.Main});', 'Clear a specific NPC held item'],
    ['PlayAnimation', 'PlayAnimation(${1:Sasha}, ${2:Animations.NpcOfferItemHold}, layer: ${3:Layer.Overlay}, fadeInTicks: ${4:6}, fadeOutTicks: ${5:8});', 'Play animation on a specific NPC'],
    ['StopAnimation', 'StopAnimation(${1:Sasha}, layer: ${2:Layer.Overlay}, fadeOutTicks: ${3:8});', 'Stop animation on a specific NPC'],
    ['PlayFaceAnimation', 'PlayFaceAnimation(${1:Sasha}, ${2:FaceAnimations.DialogueFaceSoft}, layer: ${3:FaceLayer.Overlay}, fadeInTicks: ${4:4}, fadeOutTicks: ${5:6});', 'Play face animation on a specific NPC'],
    ['StopFaceAnimation', 'StopFaceAnimation(${1:Sasha}, layer: ${2:FaceLayer.Overlay}, fadeOutTicks: ${3:6});', 'Stop face animation on a specific NPC'],
    ['EnterVehicle', 'EnterVehicle(${1:Sasha}, "${2:sasha_car}", role: "${3:driver}", seat: "${4:driver}");', 'Mount an NPC into a known vehicle entity'],
    ['EnterVehicleAt', 'EnterVehicleAt(${1:Sasha}, new Vector3(${2:x}, ${3:y}, ${4:z}), role: "driver", seat: "driver", radius: ${5:3});', 'Mount an NPC into a vehicle near coordinates'],
    ['EnterNearestVehicle', 'EnterNearestVehicle(${1:Sasha}, radius: ${2:12}, role: "passenger", seat: "${3:seat-1}");', 'Mount an NPC into a nearby vehicle'],
    ['LeaveVehicle', 'LeaveVehicle(${1:Sasha});', 'Dismount an NPC from its current vehicle']
  ],
  State: [
    ['Memory', 'Memory("${1:chapter_1.flag}")', 'Create a memory StateRef'],
    ['Ref', 'Ref("${1:debug.flag}")', 'Create an exact StateRef'],
    ['Set', 'Set(${1:stateRef}, ${2:true});', 'Write story state'],
    ['Remove', 'Remove(${1:stateRef});', 'Remove story state'],
    ['Clear', 'Clear(${1:stateRef});', 'Clear story state'],
    ['Add', 'Add(${1:relationshipSasha}, ${2:1});', 'Adjust numeric story state'],
    ['Increment', 'Increment(${1:counterState});', 'Increment numeric story state'],
    ['Decrement', 'Decrement(${1:counterState});', 'Decrement numeric story state']
  ],
  Dialogue: [
    ['Start', 'Start(${1:DialogueRef});', 'Start a dialogue'],
    ['Load', 'Load(${1:Dialogues.Dialogue}, audienceId: "${2:player}");', 'Load a modular dialogue action'],
    ['GoTo', 'GoTo(${1:NodeMethod});', 'Go to another dialogue node method']
  ],
  Scene: [
    ['Start', 'Start(${1:SceneRef});', 'Start a scene']
  ],
  Player: [
    ['SpawnAt', 'SpawnAt(${1:x}, ${2:y}, ${3:z}, yaw: ${4:0}, pitch: ${5:0});', 'Move the player spawn point/action position'],
    ['SetHotbarItem', 'SetHotbarItem(slot: ${1:0}, itemId: "${2:minecraft:stick}", count: ${3:1}, replace: ${4:true});', 'Set one player hotbar slot'],
    ['SelectHotbarSlot', 'SelectHotbarSlot(slot: ${1:0});', 'Select one player hotbar slot'],
    ['SitAt', 'SitAt(${1:x}, ${2:y}, ${3:z}, timeoutTicks: ${4:120});', 'Seat the player at a position'],
    ['Rotate', 'Rotate(${1:yaw}, ${2:pitch});', 'Rotate the player actor'],
    ['LookAt', 'LookAt(${1:x}, ${2:y}, ${3:z});', 'Look at a world position'],
    ['Move', 'Move(${1:x}, ${2:y}, ${3:z}, dimension: "minecraft:overworld");', 'Move the scripted player actor'],
    ['Stop', 'Stop();', 'Stop the scripted player actor'],
    ['Interact', 'Interact(${1:x}, ${2:y}, ${3:z}, face: "${4:UP}", hand: "MAIN_HAND");', 'Interact with a block as the player'],
    ['StandUp', 'StandUp();', 'Release scripted sitting'],
    ['StopUsingItem', 'StopUsingItem();', 'Stop using the current item'],
    ['IsMoving', 'IsMoving();', 'Check scripted player movement'],
    ['IsSitting', 'IsSitting();', 'Check scripted player sitting'],
    ['SelectedHotbarSlot', 'SelectedHotbarSlot();', 'Read selected hotbar slot'],
    ['IsUsingItem', 'IsUsingItem();', 'Check item use state'],
    ['SetFatigue', 'SetFatigue(${1:50});', 'Set player fatigue'],
    ['AddFatigue', 'AddFatigue(${1:10});', 'Add player fatigue'],
    ['ReduceFatigue', 'ReduceFatigue(${1:5});', 'Reduce player fatigue'],
    ['RestoreFatigue', 'RestoreFatigue();', 'Restore player fatigue'],
    ['Teleport', 'Teleport(instanceId: "${1:chapter_1_blackspace}", target: "${2:entry}");', 'Teleport the player through blackspace routing'],
    ['Pose', 'Pose("${1:none}");', 'Set the player presentation pose'],
    ['GiveItem', 'GiveItem(${1:Items.SparkGrass}, count: ${2:1}, autoEquip: ${3:true});', 'Give an item to the player'],
    ['SeatedIn', 'SeatedIn(${1:SeatZone});', 'Trigger selector: player seated anywhere in a seat zone'],
    ['SeatedAt', 'SeatedAt(${1:SeatZone}, ${2:SeatSlot});', 'Trigger selector: player seated at a named seat slot'],
    ['StoodUpIn', 'StoodUpIn(${1:SeatZone});', 'Trigger selector: player stood up inside a seat zone'],
    ['StoodUpFrom', 'StoodUpFrom(${1:SeatZone}, ${2:SeatSlot});', 'Trigger selector: player stood up from a named seat slot'],
    ['ChangedSeatIn', 'ChangedSeatIn(${1:SeatZone});', 'Trigger selector: player changed seat inside a seat zone']
  ],
  This: [
    ['HoldItem', 'HoldItem(${1:Items.SparkGrass}, count: ${2:1}, hand: ${3:Hand.Main});', 'Set the current NPC held item'],
    ['ClearHeldItem', 'ClearHeldItem(hand: ${1:Hand.Main});', 'Clear the current NPC held item'],
    ['PlayAnimation', 'PlayAnimation(${1:Animations.NpcOfferItemHold}, layer: ${2:Layer.Overlay}, fadeInTicks: ${3:6}, fadeOutTicks: ${4:8});', 'Play animation on the current NPC'],
    ['StopAnimation', 'StopAnimation(layer: ${1:Layer.Overlay}, fadeOutTicks: ${2:8});', 'Stop animation on the current NPC'],
    ['Rotate', 'Rotate(new Vector3(${1:0}, ${2:135}, ${3:0}), speed: ${4:4.0}, lock: ${5:true});', 'Rotate the current NPC: x=pitch, y=yaw, z=roll'],
    ['Turn', 'Turn(new Vector3(${1:0}, ${2:135}, ${3:0}), speed: ${4:4.0});', 'Alias for Rotate']
  ],
  Navigation: [
    ['SetTarget', 'SetTarget("${1:quest.main_gate}", x: ${2:x}, y: ${3:y}, z: ${4:z}, label: "${5:Main Gate}", dimensionId: "minecraft:overworld", profile: "${6:prefer_roads}");', 'Set the GPS/navigation target'],
    ['ClearTarget', 'ClearTarget();', 'Clear the GPS/navigation target']
  ],
  Quest: [
    ['Update', 'Update(${1:QuestRef}, title: "${2:Title}", body: "${3:Body}", stage: ${4:StageRef}, status: ${5:QuestStatus.Active}, showNotification: true);', 'Update quest status and text']
  ],
  Monologue: [
    ['Show', 'Show(${1:SpeakerRef}, "${2:Text}", key: "${3:line.key}", durationTicks: ${4:80});', 'Show a monologue line']
  ],
  Fog: [
    ['Show', 'Show(${1:0.0}, ${2:32.0}, color: "${3:#000000}");', 'Show story fog'],
    ['Hide', 'Hide();', 'Hide story fog']
  ],
  BlackScreen: [
    ['Show', 'Show(state: "${1:wake_prompt}", fadeInTicks: ${2:20}, durationTicks: ${3:40}, fadeOutTicks: ${4:20});', 'Show black-screen overlay'],
    ['Hide', 'Hide();', 'Hide black-screen overlay']
  ],
  Video: [
    ['Play', 'Play("${1:https://example/video.mp4}", title: "${2:Video}", durationTicks: ${3:200}, skippable: true);', 'Play a video']
  ],
  World: [
    ['TeleportPlayer', 'TeleportPlayer(${1:Overworld}, new Vector3(${2:x}, ${3:y}, ${4:z}));', 'Teleport the player'],
    ['RemovePlayerItem', 'RemovePlayerItem(${1:Items.SparkGrass}, count: ${2:1});', 'Remove an item from the player'],
    ['SetNavigationTarget', 'SetNavigationTarget("${1:quest.main_gate}", ${2:x}, ${3:y}, ${4:z}, "${5:Main Gate}");', 'Set the GPS/navigation target through a world command'],
    ['ClearNavigationTarget', 'ClearNavigationTarget();', 'Clear the GPS/navigation target through a world command']
  ],
  Debug: [
    ['Mark', 'Mark("${1:label}");', 'Debug marker action for TriggerResource callbacks']
  ]
};

MEMBER_SNIPPETS.LineAfterMove = MEMBER_SNIPPETS.Line;
MEMBER_SNIPPETS.Frame = [
  ['Position', 'Position(${1:x}, ${2:y}, ${3:z})', 'Set frame position'],
  ['Rotation', 'Rotation(${1:z})', 'Set frame Z rotation'],
  ['Scale', 'Scale(${1:x}, ${2:y}, ${3:z})', 'Set frame scale']
];
MEMBER_SNIPPETS.Effect = [
  ['ApplyToPlayer', 'ApplyToPlayer("${1:jdh:effect_id}", intensity: ${2:1.0});', 'Apply an Echo Effect resource to the player'],
  ['RemoveFromPlayer', 'RemoveFromPlayer("${1:jdh:effect_id}", fadeOut: Ticks(${2:10}));', 'Remove an Echo Effect resource from the player'],
  ['WaitOnPlayer', 'WaitOnPlayer("${1:jdh:effect_id}");', 'Wait for an Echo Effect on the player'],
  ['IsActiveOnPlayer', 'IsActiveOnPlayer("${1:jdh:effect_id}");', 'Check whether an Echo Effect is active on the player'],
  ['HasCompletedOnPlayer', 'HasCompletedOnPlayer("${1:jdh:effect_id}");', 'Check whether an Echo Effect has completed on the player']
];
MEMBER_SNIPPETS.Movement = [
  ['SpeedMultiplier', 'SpeedMultiplier(Curve(Key(Ticks(${1:0}), ${2:1.0}), Key(Ticks(${3:20}), ${4:0.5})));', 'Drive player movement speed multiplier in an EffectResource']
];
MEMBER_SNIPPETS.Sound = [
  ['PlayAt', 'PlayAt(time: Ticks(${1:0}), sound: "${2:minecraft:block.note_block.pling}", volume: ${3:1.0}, pitch: ${4:1.0});', 'Add a sound timeline event to an EffectResource']
];
MEMBER_SNIPPETS.VanillaEffect = [
  ['Apply', 'Apply(time: Ticks(${1:0}), effect: "${2:minecraft:nausea}", duration: Ticks(${3:140}), amplifier: ${4:0});', 'Apply a vanilla MobEffect from an EffectResource']
];
MEMBER_SNIPPETS.Camera.push(
  ['FovOffset', 'FovOffset(Curve(Key(Ticks(${1:0}), ${2:0.0}), Key(Ticks(${3:20}), ${4:10.0})));', 'Drive camera FOV offset in an EffectResource'],
  ['Roll', 'Roll(Curve(Key(Ticks(${1:0}), ${2:0.0}), Key(Ticks(${3:20}), ${4:8.0})));', 'Drive camera roll in an EffectResource'],
  ['Shake', 'Shake(amplitude: Curve(Key(Ticks(${1:0}), ${2:0.0}), Key(Ticks(${3:10}), ${4:1.0})));', 'Drive camera shake amplitude in an EffectResource']
);
MEMBER_SNIPPETS.Screen.push(
  ['Tint', 'Tint(color: Color("${1:#000000}"), opacity: Curve(Key(Ticks(${2:0}), ${3:0.0}), Key(Ticks(${4:20}), ${5:1.0})));', 'Drive screen tint in an EffectResource'],
  ['Vignette', 'Vignette(color: Color("${1:#000000}"), opacity: Curve(Key(Ticks(${2:0}), ${3:0.0}), Key(Ticks(${4:20}), ${5:1.0})));', 'Drive screen vignette in an EffectResource']
);

const BUILDER_MEMBER_SNIPPETS = {
  Action: MEMBER_SNIPPETS.Action,
  Camera: MEMBER_SNIPPETS.Camera,
  Dialogue: DIALOGUE_BUILDER_SNIPPETS,
  Event: MEMBER_SNIPPETS.Event,
  PlayerProfile: MEMBER_SNIPPETS.PlayerProfile,
  Scene: SCENE_BUILDER_SNIPPETS,
  Speaker: MEMBER_SNIPPETS.Speaker,
  Weapon: MEMBER_SNIPPETS.Weapon
};

const TYPE_TO_BUILDER = new Map([
  ['ActionDef', 'Action'],
  ['AnimationClipDef', 'AnimationClip'],
  ['AnimationDef', 'Animation'],
  ['AnimationProfileDef', 'AnimationProfile'],
  ['AnimationRigDef', 'AnimationRig'],
  ['BehaviorDef', 'Behavior'],
  ['BoneTimelineDef', 'Bone'],
  ['CameraDef', 'Camera'],
  ['CameraPathDef', 'CameraPath'],
  ['ChoiceDef', 'Choice'],
  ['CutsceneDef', 'Cutscene'],
  ['DialogueDef', 'Dialogue'],
  ['EventDef', 'Event'],
  ['FacePoseDef', 'FacePose'],
  ['FaceAnimationDef', 'FaceAnimation'],
  ['FacialAnimationDef', 'FacialAnimation'],
  ['LineDef', 'Line'],
  ['LocalizationDef', 'Localization'],
  ['NodeDef', 'Node'],
  ['NpcAnimationBindingDef', 'NpcAnimationBinding'],
  ['NpcResourceDef', 'Npc'],
  ['PlayerProfile', 'PlayerProfile'],
  ['PlayerProfileDef', 'PlayerProfile'],
  ['ScheduleDef', 'Schedule'],
  ['SceneDef', 'Scene'],
  ['SpeakerDef', 'Speaker'],
  ['StoryResourceDef', 'Story'],
  ['StoryIndexDef', 'StoryIndex'],
  ['TimerDef', 'Timer'],
  ['WeaponDefinition', 'Weapon'],
  ['WeaponDef', 'Weapon']
]);

const ARGUMENT_SNIPPETS = {
  Action: [
    ['type', 'type: "${1:echo_dialogue:play}"', 'External action adapter type'],
    ['actorId', 'actorId: "${1:jdh:actor_id}"', 'NPC actor id'],
    ['definitionId', 'definitionId: "${1:jdh:npc_definition}"', 'NPC definition id'],
    ['dialogueId', 'dialogueId: "${1:jdh:dialogue_id}"', 'Dialogue resource id'],
    ['audienceId', 'audienceId: "${1:jdh:local_author}"', 'Dialogue audience id'],
    ['worldId', 'worldId: "minecraft:overworld"', 'Minecraft world/dimension id'],
    ['x', 'x: "${1:0}"', 'Adapter X coordinate'],
    ['y', 'y: "${1:64}"', 'Adapter Y coordinate'],
    ['z', 'z: "${1:0}"', 'Adapter Z coordinate'],
    ['yaw', 'yaw: "${1:0}"', 'Adapter yaw'],
    ['pitch', 'pitch: "${1:0}"', 'Adapter pitch'],
    ['output', 'output: "${1:result}"', 'Output binding slot'],
    ['next', 'next: "${1:next_node}"', 'Next story node id']
  ],
  Animation: [
    ['channels', 'channels: [Channel("${1:body}")]', 'Declared animation channels'],
    ['sources', 'sources: [Source("${1:body/idle}")]', 'Source graph states'],
    ['entries', 'entries: [Entry("${1:body/idle}", clip: "${2:jdh:clip_id}", durationTicks: ${3:6})]', 'Entry variants'],
    ['main', 'main: Main(clip: "${1:jdh:loop_clip}", mode: "LOOP_UNTIL_STOP", durationTicks: ${2:24})', 'Main animation phase'],
    ['exits', 'exits: [Exit("${1:neutral}", clip: "${2:jdh:exit_clip}", durationTicks: ${3:6})]', 'Exit variants'],
    ['target', 'target: "${1:body/idle}"', 'Target state after recovery'],
    ['recovery', 'recovery: Recovery("BOUNDED", checkpointIntervalTicks: ${1:8})', 'Recovery policy'],
    ['replay', 'replay: "REJECT_IF_RUNNING"', 'Replay policy']
  ],
  AnimationClip: [
    ['rig', 'rig: "${1:jdh:humanoid}"', 'Animation rig id'],
    ['tracks', 'tracks: [Track("${1:bone_id}", frames: [Frame("0.0").Rotation(0, 0, 0)])]', 'Bone tracks'],
    ['schemaVersion', 'schemaVersion: ${1:1}', 'Clip schema version'],
    ['loopSeam', 'loopSeam: "HOLD"', 'Loop seam policy'],
    ['authoredSpeedBlocksPerSecond', 'authoredSpeedBlocksPerSecond: ${1:1.0}', 'Authored locomotion speed']
  ],
  AnimationProfile: [
    ['rig', 'rig: "${1:jdh:humanoid}"', 'Animation rig id'],
    ['graph', 'graph: "${1:jdh:humanoid}"', 'Animation graph id'],
    ['render', 'render: "SEGMENTED_HUMANOID"', 'Render model kind'],
    ['initialStates', 'initialStates: [InitialState("${1:body}", state: "${2:body/idle}")]', 'Initial graph states'],
    ['requiredChannels', 'requiredChannels: [Channel("${1:body}")]', 'Required animation channels'],
    ['metadata', 'metadata: [Meta("${1:authoring_note}", value: "${2:Note}")]', 'Profile metadata']
  ],
  AnimationRig: [
    ['kind', 'kind: "SEGMENTED_HUMANOID"', 'Rig kind'],
    ['skin', 'skin: "CLASSIC"', 'Humanoid skin variant'],
    ['bones', 'bones: [Bone("${1:bone_id}", parent: "${2:parent_id}")]', 'Custom bones'],
    ['sockets', 'sockets: [Socket("${1:main_hand}", bone: "${2:right_forearm}")]', 'Attachment sockets']
  ],
  'AreaEnterDelayed': [
    ['delayTicks', 'delayTicks: ${1:40}', 'Delay the area trigger after entry']
  ],
  'BlackScreen.Show': [
    ['state', 'state: "${1:wake_prompt}"', 'Black-screen state key'],
    ['visible', 'visible: ${1:true}', 'Set overlay visibility'],
    ['fadeInTicks', 'fadeInTicks: ${1:20}', 'Fade-in duration'],
    ['durationTicks', 'durationTicks: ${1:40}', 'Covered duration'],
    ['fadeOutTicks', 'fadeOutTicks: ${1:20}', 'Fade-out duration'],
    ['text', 'text: "${1:Text}"', 'Overlay text'],
    ['textKey', 'textKey: "${1:intro.text}"', 'Localized overlay text key'],
    ['button', 'button: "${1:Continue}"', 'Wake/continue button text'],
    ['buttonKey', 'buttonKey: "${1:intro.button}"', 'Localized button text key'],
    ['playerPoseOnWake', 'playerPoseOnWake: "${1:none}"', 'Player pose after wake'],
    ['onWakeStartedSignal', 'onWakeStartedSignal: "${1:intro.wake.started}"', 'Signal when wake starts'],
    ['onCoveredSignal', 'onCoveredSignal: "${1:intro.covered}"', 'Signal when screen is covered'],
    ['onClearedSignal', 'onClearedSignal: "${1:intro.cleared}"', 'Signal when screen clears']
  ],
  'Choice': [
    ['key', 'key: "${1:dialogue.choice}"', 'Localization key'],
    ['textKey', 'textKey: "${1:dialogue.choice}"', 'Localization key alias'],
    ['if', 'if: ${1:condition}', 'Choice condition']
  ],
  Dialogue: [
    ['start', 'start: "${1:start}"', 'Start node id']
  ],
  Entry: [
    ['clip', 'clip: "${1:jdh:clip_id}"', 'Animation clip id'],
    ['durationTicks', 'durationTicks: ${1:6}', 'Entry duration']
  ],
  Exit: [
    ['clip', 'clip: "${1:jdh:exit_clip}"', 'Animation clip id'],
    ['durationTicks', 'durationTicks: ${1:6}', 'Exit duration']
  ],
  FacePose: [
    ['rig', 'rig: "${1:jdh:face_rig}"', 'Face rig id'],
    ['bones', 'bones: [BoneDelta("${1:face_root}")]', 'Face bone deltas'],
    ['controls', 'controls: [ControlDelta("${1:eye_closure}", value: ${2:0})]', 'Face control deltas']
  ],
  FacialAnimation: [
    ['rig', 'rig: "${1:jdh:face_rig}"', 'Face rig id'],
    ['durationTicks', 'durationTicks: ${1:12}', 'Default duration in ticks'],
    ['durationMillis', 'durationMillis: ${1:600}', 'Default duration in milliseconds'],
    ['end', 'end: "HOLD"', 'End behavior'],
    ['tracks', 'tracks: [Track("${1:bone_id}", frames: [Frame(0).Position(0, 0, 0)])]', 'Default face tracks'],
    ['surfaces', 'surfaces: [World, Portrait]', 'Surface classes or Surface(...) calls']
  ],
  Main: [
    ['clip', 'clip: "${1:jdh:loop_clip}"', 'Animation clip id'],
    ['mode', 'mode: "LOOP_UNTIL_STOP"', 'Main phase mode'],
    ['durationTicks', 'durationTicks: ${1:24}', 'Main phase duration'],
    ['exitPoints', 'exitPoints: [ExitPoint("${1:neutral}", progress: "0.0")]', 'Exit points']
  ],
  'CameraPathCue': [
    ['durationTicks', 'durationTicks: ${1:200}', 'Camera path duration'],
    ['fadeIn', 'fadeIn: ${1:4}', 'Fade-in ticks'],
    ['fadeOut', 'fadeOut: ${1:6}', 'Fade-out ticks'],
    ['lockControls', 'lockControls: ${1:true}', 'Lock controls while playing'],
    ['lockMouse', 'lockMouse: ${1:true}', 'Lock mouse while playing']
  ],
  'InventoryHasItem': [
    ['count', 'count: ${1:1}', 'Required item count']
  ],
  'InventoryMissing': [
    ['count', 'count: ${1:1}', 'Required missing item count']
  ],
  'Line': [
    ['id', 'id: "${1:line_id}"', 'Line id'],
    ['key', 'key: "${1:dialogue.line}"', 'Localization key'],
    ['textKey', 'textKey: "${1:dialogue.line}"', 'Localization key alias'],
    ['delay', 'delay: ${1:20}', 'Line delay'],
    ['delayTicks', 'delayTicks: ${1:20}', 'Line delay in ticks'],
    ['thought', 'thought: ${1:false}', 'Mark line as thought'],
    ['voice', 'voice: "${1:minecraft:entity.villager.ambient}"', 'Voice sound id'],
    ['portrait', 'portrait: "${1:portrait_id}"', 'Portrait id'],
    ['fx', 'fx: "${1:effect_id}"', 'Line effect id'],
    ['nameColor', 'nameColor: "${1:#ffffff}"', 'Name color'],
    ['anim', 'anim: ${1:Animations.NpcOfferItemHold}', 'Body animation'],
    ['animation', 'animation: ${1:Animations.NpcOfferItemHold}', 'Body animation alias'],
    ['face', 'face: ${1:FaceAnimations.DialogueFaceSoft}', 'Face animation'],
    ['faceStop', 'faceStop: ${1:true}', 'Stop face animation'],
    ['path', 'path: ${1:NpcPath(Sasha, [Vec(x, y, z)])}', 'NPC path for the line']
  ],
  Npc: [
    ['id', 'id: "${1:jdh:npc_id}"', 'NPC definition id'],
    ['display', 'display: "${1:Display name}"', 'Authored display fallback'],
    ['skin', 'skin: Skin("${1:jdh:npc_skin}")', 'NPC skin resource']
  ],
  NpcAnimationBinding: [
    ['actor', 'actor: "${1:jdh:actor_id}"', 'Specific NPC actor override'],
    ['npc', 'npc: "${1:jdh:npc_definition}"', 'NPC definition binding'],
    ['profile', 'profile: "${1:jdh:animation_profile}"', 'Animation profile id']
  ],
  'Monologue.Show': [
    ['key', 'key: "${1:monologue.line}"', 'Localization key'],
    ['portrait', 'portrait: "${1:portrait_id}"', 'Portrait id'],
    ['thought', 'thought: ${1:false}', 'Thought line flag'],
    ['delay', 'delay: ${1:20}', 'Line delay'],
    ['durationTicks', 'durationTicks: ${1:80}', 'Display duration']
  ],
  'Navigation.SetTarget': [
    ['x', 'x: ${1:0}', 'Target X coordinate'],
    ['y', 'y: ${1:64}', 'Target Y coordinate'],
    ['z', 'z: ${1:0}', 'Target Z coordinate'],
    ['label', 'label: "${1:Target}"', 'Navigation label'],
    ['dimensionId', 'dimensionId: "minecraft:overworld"', 'Target dimension id'],
    ['dimension', 'dimension: ${1:Overworld}', 'Target dimension'],
    ['icon', 'icon: "${1:quest}"', 'Navigation icon id'],
    ['profile', 'profile: "${1:prefer_roads}"', 'Navigation profile']
  ],
  'Npc.ApproachPlayer': [
    ['stopDistance', 'stopDistance: ${1:2.5}', 'Stop distance from player'],
    ['speed', 'speed: ${1:1.15}', 'Movement speed'],
    ['repathTicks', 'repathTicks: ${1:20}', 'Repath interval'],
    ['timeoutTicks', 'timeoutTicks: ${1:200}', 'Approach timeout'],
    ['lookAtPlayer', 'lookAtPlayer: ${1:true}', 'Look at player while approaching'],
    ['warpWhenUnseen', 'warpWhenUnseen: ${1:false}', 'Warp if unseen'],
    ['warpDistance', 'warpDistance: ${1:24.0}', 'Warp distance'],
    ['warpSearchRadius', 'warpSearchRadius: ${1:12}', 'Warp search radius'],
    ['thenDialogue', 'thenDialogue: ${1:Dialogues.Dialogue}', 'Dialogue after arrival']
  ],
  'Npc.ClearHeldItem': [
    ['hand', 'hand: ${1:Hand.Main}', 'Hand slot']
  ],
  'Npc.EnterVehicle': [
    ['role', 'role: "${1:driver}"', 'Vehicle role'],
    ['seat', 'seat: "${1:driver}"', 'Seat id']
  ],
  'Npc.EnterNearestVehicle': [
    ['radius', 'radius: ${1:12}', 'Vehicle search radius'],
    ['role', 'role: "${1:passenger}"', 'Vehicle role'],
    ['seat', 'seat: "${1:seat-1}"', 'Seat id']
  ],
  'Npc.EnterVehicleAt': [
    ['radius', 'radius: ${1:3}', 'Vehicle search radius'],
    ['role', 'role: "${1:driver}"', 'Vehicle role'],
    ['seat', 'seat: "${1:driver}"', 'Seat id'],
    ['dimension', 'dimension: ${1:Overworld}', 'Search dimension']
  ],
  'Npc.HoldItem': [
    ['count', 'count: ${1:1}', 'Item count'],
    ['hand', 'hand: ${1:Hand.Main}', 'Hand slot']
  ],
  'Npc.Move': [
    ['x', 'x: ${1:0}', 'Target X coordinate'],
    ['y', 'y: ${1:64}', 'Target Y coordinate'],
    ['z', 'z: ${1:0}', 'Target Z coordinate'],
    ['dimension', 'dimension: "minecraft:overworld"', 'Target dimension id'],
    ['output', 'output: "${1:result}"', 'Output binding slot'],
    ['next', 'next: "${1:next_node}"', 'Next local story node id'],
    ['speed', 'speed: ${1:1.15}', 'Movement speed'],
    ['completionDistance', 'completionDistance: ${1:0.7}', 'Completion distance'],
    ['pauseTicks', 'pauseTicks: ${1:0}', 'Pause at target'],
    ['repathAfterTicks', 'repathAfterTicks: ${1:30}', 'Repath interval'],
    ['maxRepathAttempts', 'maxRepathAttempts: ${1:3}', 'Maximum repath attempts'],
    ['teleportToTargetIfStuck', 'teleportToTargetIfStuck: ${1:false}', 'Teleport if stuck'],
    ['openDoors', 'openDoors: ${1:true}', 'Open doors while moving'],
    ['canFloat', 'canFloat: ${1:false}', 'Allow floating path'],
    ['targetSearchRadius', 'targetSearchRadius: ${1:16}', 'Search radius'],
    ['thenDialogue', 'thenDialogue: ${1:Dialogues.Dialogue}', 'Dialogue after arrival']
  ],
  'Npc.Path': [
    ['speed', 'speed: ${1:1.15}', 'Movement speed'],
    ['completionDistance', 'completionDistance: ${1:0.7}', 'Completion distance'],
    ['pauseTicks', 'pauseTicks: ${1:0}', 'Pause at each point'],
    ['repathAfterTicks', 'repathAfterTicks: ${1:30}', 'Repath interval'],
    ['maxRepathAttempts', 'maxRepathAttempts: ${1:3}', 'Maximum repath attempts'],
    ['teleportToTargetIfStuck', 'teleportToTargetIfStuck: ${1:false}', 'Teleport if stuck'],
    ['openDoors', 'openDoors: ${1:true}', 'Open doors while moving'],
    ['canFloat', 'canFloat: ${1:false}', 'Allow floating path'],
    ['targetSearchRadius', 'targetSearchRadius: ${1:16}', 'Search radius'],
    ['thenDialogue', 'thenDialogue: ${1:Dialogues.Dialogue}', 'Dialogue after route']
  ],
  'Npc.PlayAnimation': [
    ['layer', 'layer: ${1:Layer.Overlay}', 'Animation layer'],
    ['fadeInTicks', 'fadeInTicks: ${1:6}', 'Fade-in ticks'],
    ['fadeOutTicks', 'fadeOutTicks: ${1:8}', 'Fade-out ticks'],
    ['durationTicks', 'durationTicks: ${1:40}', 'Limit playback duration']
  ],
  'Npc.PlayFaceAnimation': [
    ['layer', 'layer: ${1:FaceLayer.Overlay}', 'Face animation layer'],
    ['fadeInTicks', 'fadeInTicks: ${1:4}', 'Fade-in ticks'],
    ['fadeOutTicks', 'fadeOutTicks: ${1:6}', 'Fade-out ticks'],
    ['durationTicks', 'durationTicks: ${1:40}', 'Limit playback duration']
  ],
  'Npc.Rotate': [
    ['rotation', 'rotation: new Vector3(${1:0}, ${2:135}, ${3:0})', 'Rotation axes: x=pitch, y=yaw, z=roll'],
    ['rot', 'rot: new Vector3(${1:0}, ${2:135}, ${3:0})', 'Alias for rotation'],
    ['axes', 'axes: new Vector3(${1:0}, ${2:135}, ${3:0})', 'Alias for rotation'],
    ['yaw', 'yaw: ${1:135}', 'Target yaw if not using Vector3'],
    ['pitch', 'pitch: ${1:0}', 'Optional target pitch'],
    ['roll', 'roll: ${1:0}', 'Optional target roll stored in IR'],
    ['speed', 'speed: ${1:4.0}', 'Degrees per tick; enables smooth head-first turn'],
    ['turnSpeed', 'turnSpeed: ${1:4.0}', 'Alias for speed'],
    ['lock', 'lock: ${1:true}', 'Keep NPC rotation locked after turning']
  ],
  'Npc.SpawnNearPlayer': [
    ['forwardOffset', 'forwardOffset: ${1:3.0}', 'Forward offset from player'],
    ['sideOffset', 'sideOffset: ${1:0.0}', 'Side offset from player'],
    ['yOffset', 'yOffset: ${1:0.0}', 'Vertical offset from player']
  ],
  'Npc.StopAnimation': [
    ['layer', 'layer: ${1:Layer.Overlay}', 'Animation layer'],
    ['fadeOutTicks', 'fadeOutTicks: ${1:8}', 'Fade-out ticks']
  ],
  'Npc.StopFaceAnimation': [
    ['layer', 'layer: ${1:FaceLayer.Overlay}', 'Face animation layer'],
    ['fadeOutTicks', 'fadeOutTicks: ${1:6}', 'Fade-out ticks']
  ],
  'Npc.UseBlock': [
    ['action', 'action: "${1:use}"', 'Block action/mode to run'],
    ['mode', 'mode: "${1:use}"', 'Alias for action']
  ],
  'Npc.LookAtPlayers': [
    ['enabled', 'enabled: ${1:true}', 'Enable or disable look-at-players behavior']
  ],
  'Player.GiveItem': [
    ['count', 'count: ${1:1}', 'Item count'],
    ['autoEquip', 'autoEquip: ${1:true}', 'Auto-equip item'],
    ['preferredSlot', 'preferredSlot: ${1:0}', 'Preferred inventory slot'],
    ['replacePreferredSlot', 'replacePreferredSlot: ${1:true}', 'Replace preferred slot item']
  ],
  'Quest.Update': [
    ['title', 'title: "${1:Title}"', 'Quest title'],
    ['titleKey', 'titleKey: "${1:quest.title}"', 'Localized quest title key'],
    ['body', 'body: "${1:Body}"', 'Quest body'],
    ['bodyKey', 'bodyKey: "${1:quest.body}"', 'Localized quest body key'],
    ['stage', 'stage: ${1:QuestStageRef}', 'Quest stage reference'],
    ['stageTitle', 'stageTitle: "${1:Stage}"', 'Quest stage title'],
    ['stageTitleKey', 'stageTitleKey: "${1:quest.stage}"', 'Localized stage title key'],
    ['status', 'status: ${1:QuestStatus.Active}', 'Quest status'],
    ['showNotification', 'showNotification: ${1:true}', 'Show quest notification'],
    ['marker', 'marker: ${1:QuestMarker(Overworld, x: 0, y: 64, z: 0)}', 'Quest marker']
  ],
  'QuestMarker': [
    ['x', 'x: ${1:0}', 'Marker X coordinate'],
    ['y', 'y: ${1:64}', 'Marker Y coordinate'],
    ['z', 'z: ${1:0}', 'Marker Z coordinate'],
    ['label', 'label: "${1:Marker}"', 'Marker label'],
    ['labelKey', 'labelKey: "${1:quest.marker}"', 'Localized marker label key']
  ],
  'TimeReached': [
    ['dimension', 'dimension: ${1:Overworld}', 'Dimension id/reference'],
    ['dayTime', 'dayTime: ${1:6000}', 'Minecraft day time']
  ],
  'Timer': [
    ['delayTicks', 'delayTicks: ${1:600}', 'Initial timer delay'],
    ['delay', 'delay: ${1:600}', 'Initial timer delay alias'],
    ['repeat', 'repeat: ${1:1200}', 'Repeat interval ticks'],
    ['scope', 'scope: "${1:PLAYER}"', 'Timer scope']
  ],
  'This.ClearHeldItem': [
    ['hand', 'hand: ${1:Hand.Main}', 'Hand slot'],
    ['fadeOutTicks', 'fadeOutTicks: ${1:8}', 'Fade-out ticks']
  ],
  'This.HoldItem': [
    ['count', 'count: ${1:1}', 'Item count'],
    ['hand', 'hand: ${1:Hand.Main}', 'Hand slot']
  ],
  'This.PlayAnimation': [
    ['layer', 'layer: ${1:Layer.Overlay}', 'Animation layer'],
    ['fadeInTicks', 'fadeInTicks: ${1:6}', 'Fade-in ticks'],
    ['fadeOutTicks', 'fadeOutTicks: ${1:8}', 'Fade-out ticks'],
    ['durationTicks', 'durationTicks: ${1:40}', 'Limit playback duration']
  ],
  'This.Rotate': [
    ['rotation', 'rotation: new Vector3(${1:0}, ${2:135}, ${3:0})', 'Rotation axes: x=pitch, y=yaw, z=roll'],
    ['rot', 'rot: new Vector3(${1:0}, ${2:135}, ${3:0})', 'Alias for rotation'],
    ['axes', 'axes: new Vector3(${1:0}, ${2:135}, ${3:0})', 'Alias for rotation'],
    ['yaw', 'yaw: ${1:135}', 'Target yaw if not using Vector3'],
    ['pitch', 'pitch: ${1:0}', 'Optional target pitch'],
    ['roll', 'roll: ${1:0}', 'Optional target roll stored in IR'],
    ['speed', 'speed: ${1:4.0}', 'Degrees per tick; enables smooth head-first turn'],
    ['turnSpeed', 'turnSpeed: ${1:4.0}', 'Alias for speed'],
    ['lock', 'lock: ${1:true}', 'Keep NPC rotation locked after turning']
  ],
  'This.StopAnimation': [
    ['layer', 'layer: ${1:Layer.Overlay}', 'Animation layer'],
    ['fadeOutTicks', 'fadeOutTicks: ${1:8}', 'Fade-out ticks']
  ],
  'Vehicle.Sequence': [
    ['sequenceId', 'sequenceId: "${1:sequence_id}"', 'Sequence id'],
    ['passenger', 'passenger: "${1:player}"', 'Passenger actor'],
    ['passengerSeat', 'passengerSeat: "${1:seat-1}"', 'Passenger seat id'],
    ['category', 'category: "${1:car}"', 'Vehicle category'],
    ['backend', 'backend: "${1:native_preferred}"', 'Motion backend'],
    ['speed', 'speed: ${1:1.0}', 'Drive speed'],
    ['exitOnArrival', 'exitOnArrival: ${1:true}', 'Exit on arrival'],
    ['walkToBuilding', 'walkToBuilding: ${1:false}', 'Walk into target building'],
    ['cinematic', 'cinematic: ${1:CameraPaths.Path}', 'Cinematic camera path'],
    ['durationTicks', 'durationTicks: ${1:200}', 'Cinematic duration'],
    ['keepCamera', 'keepCamera: ${1:false}', 'Keep camera after trip'],
    ['completionPolicy', 'completionPolicy: "${1:wait_for_camera}"', 'Completion policy'],
    ['collisionPolicy', 'collisionPolicy: "${1:ghost}"', 'Camera collision policy'],
    ['completionSignal', 'completionSignal: "${1:vehicle.sequence.complete}"', 'Completion signal'],
    ['onStartedSignal', 'onStartedSignal: "${1:vehicle.sequence.started}"', 'Started signal'],
    ['onCompletedSignal', 'onCompletedSignal: "${1:vehicle.sequence.completed}"', 'Completed signal'],
    ['onFailedSignal', 'onFailedSignal: "${1:vehicle.sequence.failed}"', 'Failed signal']
  ],
  'Vehicle.Trip': [
    ['tripId', 'tripId: "${1:trip_id}"', 'Trip id'],
    ['category', 'category: "${1:car}"', 'Vehicle category'],
    ['backend', 'backend: "${1:native_preferred}"', 'Motion backend'],
    ['speed', 'speed: ${1:1.0}', 'Drive speed'],
    ['mountRadius', 'mountRadius: ${1:12}', 'Vehicle mount radius'],
    ['parkingRadius', 'parkingRadius: ${1:4}', 'Arrival parking radius'],
    ['parkingTicks', 'parkingTicks: ${1:40}', 'Parking ticks'],
    ['exitOnArrival', 'exitOnArrival: ${1:true}', 'Exit on arrival'],
    ['walkToBuilding', 'walkToBuilding: ${1:false}', 'Walk into target building'],
    ['seat', 'seat: "${1:seat-1}"', 'Seat id'],
    ['building', 'building: Vec(${1:x}, ${2:y}, ${3:z})', 'Building entry position'],
    ['parking', 'parking: Vec(${1:x}, ${2:y}, ${3:z})', 'Parking position'],
    ['stops', 'stops: ${1:[]}', 'Intermediate stops'],
    ['motion', 'motion: ${1:Obj(Field("profile", "default"))}', 'Motion profile']
  ],
  'Video.Play': [
    ['title', 'title: "${1:Video}"', 'Video title'],
    ['durationTicks', 'durationTicks: ${1:200}', 'Playback duration'],
    ['skippable', 'skippable: ${1:true}', 'Allow skipping'],
    ['openExternal', 'openExternal: ${1:false}', 'Open externally'],
    ['onCompleteSignal', 'onCompleteSignal: "${1:video.complete}"', 'Completion signal']
  ],
  'World.SetNavigationTarget': [
    ['x', 'x: ${1:0}', 'Target X coordinate'],
    ['y', 'y: ${1:64}', 'Target Y coordinate'],
    ['z', 'z: ${1:0}', 'Target Z coordinate'],
    ['label', 'label: "${1:Target}"', 'Navigation label'],
    ['dimensionId', 'dimensionId: "minecraft:overworld"', 'Target dimension id'],
    ['dimension', 'dimension: ${1:Overworld}', 'Target dimension'],
    ['icon', 'icon: "${1:quest}"', 'Navigation icon id'],
    ['profile', 'profile: "${1:prefer_roads}"', 'Navigation profile']
  ]
};

const STORY_ACTION_CONTROL_ARGUMENTS = [
  ['next', 'next: "${1:next_node}"', 'Next local story node id'],
  ['output', 'output: "${1:result}"', 'Output binding slot']
];

const SLEEP_CONTEXT_ARGUMENTS = [
  ['player', 'player: "${1:player}"', 'Player id/name context'],
  ['scenario', 'scenario: "${1:default}"', 'Sleep scenario id'],
  ['owner', 'owner: "${1:story}"', 'Sleep flow owner id'],
  ['command', 'command: "${1:command}"', 'Sleep command id'],
  ...STORY_ACTION_CONTROL_ARGUMENTS
];

ARGUMENT_SNIPPETS.Npc.push(
  ['defaultForm', 'defaultForm: DefaultForm(${1:NpcForms.Form})', 'Default authored NPC form'],
  ['nameColor', 'nameColor: NameColor("${1:#ffffff}")', 'NPC display-name color']
);

ARGUMENT_SNIPPETS.Form = [
  ['id', 'id: "${1:jdh:npc/form}"', 'Stable NPC form resource id'],
  ['skin', 'skin: Skin("${1:jdh:npc_skin}")', 'Skin visual used by this form'],
  ['model', 'model: "${1:default}"', 'Runtime model id'],
  ['dimensions', 'dimensions: Dimensions(${1:0.6}, ${2:1.8}, ${3:1.62})', 'Collision/eye dimensions'],
  ['bodyScale', 'bodyScale: ${1:1.0}', 'Whole-body scale'],
  ['parts', 'parts: [Part("${1:head}", scale: ${2:1.0})]', 'Authored model parts'],
  ['anchors', 'anchors: [Anchor("${1:dialogue}", x: ${2:0}, y: ${3:1.8}, z: ${4:0})]', 'Named attachment anchors'],
  ['animationGraph', 'animationGraph: "${1:jdh:standard_humanoid}"', 'Animation graph/profile id'],
  ['faceProfile', 'faceProfile: "${1:jdh:john_father}"', 'Face profile/rig id'],
  ['portrait', 'portrait: Portrait("${1:jdh:npc_skin}")', 'Portrait visual'],
  ['tags', 'tags: ["${1:story}"]', 'Authoring/runtime tags']
];

ARGUMENT_SNIPPETS.Dimensions = [
  ['width', 'width: ${1:0.6}', 'Entity collision width'],
  ['height', 'height: ${1:1.8}', 'Entity collision height'],
  ['eyeHeight', 'eyeHeight: ${1:1.62}', 'Entity eye height']
];

ARGUMENT_SNIPPETS.Part = [
  ['id', 'id: "${1:part_id}"', 'Part id'],
  ['scale', 'scale: ${1:1.0}', 'Uniform part scale'],
  ['scaleX', 'scaleX: ${1:1.0}', 'X scale'],
  ['scaleY', 'scaleY: ${1:1.0}', 'Y scale'],
  ['scaleZ', 'scaleZ: ${1:1.0}', 'Z scale'],
  ['pivotX', 'pivotX: ${1:0}', 'Pivot X'],
  ['pivotY', 'pivotY: ${1:0}', 'Pivot Y'],
  ['pivotZ', 'pivotZ: ${1:0}', 'Pivot Z'],
  ['rotationX', 'rotationX: ${1:0}', 'Rotation X'],
  ['rotationY', 'rotationY: ${1:0}', 'Rotation Y'],
  ['rotationZ', 'rotationZ: ${1:0}', 'Rotation Z']
];

ARGUMENT_SNIPPETS.Anchor = [
  ['id', 'id: "${1:anchor_id}"', 'Anchor id'],
  ['x', 'x: ${1:0}', 'Anchor X'],
  ['y', 'y: ${1:0}', 'Anchor Y'],
  ['z', 'z: ${1:0}', 'Anchor Z']
];

ARGUMENT_SNIPPETS.PlayerProfile = [
  ['id', 'id: "${1:jdh:player/default}"', 'Stable player profile template id'],
  ['nameMode', 'nameMode: ${1:ACCOUNT}', 'ACCOUNT or CUSTOM display-name mode'],
  ['displayName', 'displayName: "${1:Display Name}"', 'Required when nameMode is CUSTOM'],
  ['spark', 'spark: ${1:100}', 'Current spark amount'],
  ['maxSpark', 'maxSpark: ${1:100}', 'Maximum spark amount'],
  ['skinMode', 'skinMode: ${1:ACCOUNT}', 'ACCOUNT or RESOURCE skin mode'],
  ['skinId', 'skinId: "${1:echo_content_player:player/base}"', 'Required when skinMode is RESOURCE'],
  ['skinModel', 'skinModel: ${1:CLASSIC}', 'Skin model when using a resource skin']
];

ARGUMENT_SNIPPETS.Weapon = [
  ['id', 'id: "${1:jdh:weapon_id}"', 'Stable weapon id'],
  ['name', 'name: "${1:Weapon Name}"', 'Display name'],
  ['texture', 'texture: "${1:jdh:item/weapon}"', 'Texture resource id'],
  ['description', 'description: "${1:Description}"', 'Description text'],
  ['tooltip', 'tooltip: ["${1:Tooltip line}"]', 'Tooltip lines'],
  ['model', 'model: "${1:handheld}"', 'Optional item model'],
  ['damage', 'damage: ${1:7}', 'Attack damage'],
  ['attackDamage', 'attackDamage: ${1:7}', 'Alias for damage'],
  ['attacksPerSecond', 'attacksPerSecond: ${1:1.6}', 'Attacks per second'],
  ['attackSpeedModifier', 'attackSpeedModifier: ${1:-2.4}', 'Minecraft attack speed modifier'],
  ['knockback', 'knockback: ${1:0}', 'Attack knockback'],
  ['durability', 'durability: ${1:250}', 'Durability points'],
  ['unbreakable', 'unbreakable: ${1:false}', 'Whether durability is disabled'],
  ['enchantability', 'enchantability: ${1:0}', 'Enchantability value'],
  ['repairIngredient', 'repairIngredient: "${1:minecraft:iron_ingot}"', 'Repair ingredient id'],
  ['rarity', 'rarity: ${1:COMMON}', 'COMMON, UNCOMMON, RARE, or EPIC'],
  ['glint', 'glint: ${1:false}', 'Always render enchantment glint'],
  ['fireResistant', 'fireResistant: ${1:false}', 'Fire-resistant item flag'],
  ['equipmentSlot', 'equipmentSlot: ${1:CHEST}', 'HEAD, CHEST, LEGS, or FEET'],
  ['hiddenFromCreative', 'hiddenFromCreative: ${1:false}', 'Hide from creative tab'],
  ['creativeOrder', 'creativeOrder: ${1:10}', 'Creative tab sort order']
];

ARGUMENT_SNIPPETS.Id = [
  ['id', '"${1:namespace:path}"', 'Stable resource id']
];
ARGUMENT_SNIPPETS.Scope = [
  ['scope', '${1:PLAYER}', 'GLOBAL, SERVER, PLAYER, or DIALOGUE_SESSION']
];
ARGUMENT_SNIPPETS.Mode = [
  ['mode', '${1:REPEAT}', 'ONCE or REPEAT']
];
ARGUMENT_SNIPPETS.Start = [
  ['method', '${1:Sequence}', 'Trigger sequence method or dialogue node id']
];
ARGUMENT_SNIPPETS.Subscribe = [
  ['selector', 'PlayerSleep.Confirmed(${1:SleepScenario})', 'Player sleep subscription selector'],
  ['seatedIn', 'Player.SeatedIn(${1:SeatZone})', 'Player seated anywhere in a SeatZoneResource'],
  ['seatedAt', 'Player.SeatedAt(${1:SeatZone}, ${2:SeatSlot})', 'Player seated at a named SeatZone slot'],
  ['stoodUpIn', 'Player.StoodUpIn(${1:SeatZone})', 'Player stood up inside a SeatZoneResource'],
  ['stoodUpFrom', 'Player.StoodUpFrom(${1:SeatZone}, ${2:SeatSlot})', 'Player stood up from a named SeatZone slot'],
  ['changedSeatIn', 'Player.ChangedSeatIn(${1:SeatZone})', 'Player changed seat inside a SeatZoneResource']
];
ARGUMENT_SNIPPETS.StartStory = [
  ['story', '${1:Stories.Story}', 'Story id or StoryId field to start']
];
ARGUMENT_SNIPPETS.StartDialogue = [
  ['dialogue', '${1:Dialogues.Dialogue}', 'Dialogue id or DialogueId field to start'],
  ['sessionId', 'sessionId: "${1:trigger_session}"', 'Explicit dialogue session id for callback actions'],
  ['audienceId', 'audienceId: "${1:player}"', 'Explicit dialogue audience id for callback actions']
];
ARGUMENT_SNIPPETS.DialogueStart = ARGUMENT_SNIPPETS.StartDialogue;
ARGUMENT_SNIPPETS.Cooldown = [
  ['ticks', 'Ticks(${1:20})', 'Cooldown duration in ticks']
];
ARGUMENT_SNIPPETS.FireCount = [
  ['count', '${1:1}', 'Positive fire count']
];
ARGUMENT_SNIPPETS.StayInterval = [
  ['ticks', 'Ticks(${1:20})', 'PlayerStay interval in ticks']
];
ARGUMENT_SNIPPETS.StayPolicy = [
  ['policy', '${1:SKIP_WHILE_RUNNING}', 'SKIP_WHILE_RUNNING or QUEUE_ONE']
];
ARGUMENT_SNIPPETS.Wait = [
  ['expression', 'PlayerEnter(${1:AreaName})', 'Trigger expression/event to wait for']
];
ARGUMENT_SNIPPETS.Area = [
  ['dimension', '"minecraft:overworld"', 'Dimension id'],
  ['x', '${1:0}', 'Origin X'],
  ['y', '${1:64}', 'Origin Y'],
  ['z', '${1:0}', 'Origin Z'],
  ['sx', '${1:1}', 'Size X'],
  ['sy', '${1:2}', 'Size Y'],
  ['sz', '${1:1}', 'Size Z'],
  ['box', 'Box(Position(${1:0}, ${2:64}, ${3:0}), Position(${4:2}, ${5:66}, ${6:2}))', 'SeatZoneResource bounds']
];
ARGUMENT_SNIPPETS.Vector3 = [
  ['x', '${1:0}', 'X coordinate'],
  ['y', '${1:64}', 'Y coordinate'],
  ['z', '${1:0}', 'Z coordinate']
];
ARGUMENT_SNIPPETS.BoxArea = [
  ['min', 'min: Vector3(${1:0}, ${2:64}, ${3:0})', 'Minimum corner'],
  ['max', 'max: Vector3(${4:1}, ${5:66}, ${6:1})', 'Maximum corner']
];
ARGUMENT_SNIPPETS.SphereArea = [
  ['center', 'center: Vector3(${1:0}, ${2:64}, ${3:0})', 'Center point'],
  ['radius', 'radius: ${1:3}', 'Radius']
];
ARGUMENT_SNIPPETS.CylinderArea = [
  ['center', 'center: Vector3(${1:0}, ${2:64}, ${3:0})', 'Center point'],
  ['radius', 'radius: ${1:3}', 'Radius'],
  ['minY', 'minY: ${1:64}', 'Minimum Y'],
  ['maxY', 'maxY: ${1:66}', 'Maximum Y']
];
ARGUMENT_SNIPPETS.CircleArea = ARGUMENT_SNIPPETS.CylinderArea;
ARGUMENT_SNIPPETS.PolygonArea = [
  ['points', 'points: [Vector3(${1:0}, ${2:64}, ${3:0}), Vector3(${4:1}, ${5:64}, ${6:1})]', 'Polygon points'],
  ['minY', 'minY: ${1:64}', 'Minimum Y'],
  ['maxY', 'maxY: ${1:66}', 'Maximum Y']
];
for (const selector of PLAYER_SLEEP_SELECTORS) {
  ARGUMENT_SNIPPETS[`PlayerSleep.${selector}`] = [
    ['scenario', '${1:SleepScenario}', 'Scenario string or alias field'],
    ['sleepCount', 'sleepCount: ${1:1}', 'Exact positive sleep count'],
    ['sleepCountAtLeast', 'sleepCountAtLeast: ${1:1}', 'Minimum positive sleep count']
  ];
}
for (const selector of PLAYER_SEATING_SELECTORS) {
  const slotRequired = selector === 'SeatedAt' || selector === 'StoodUpFrom';
  ARGUMENT_SNIPPETS[`Player.${selector}`] = slotRequired
    ? [
      ['zone', '${1:SeatZone}', 'SeatZone id or SeatZoneId field'],
      ['slot', '${1:SeatSlot}', 'SeatSlot id or SeatSlotId field']
    ]
    : [
      ['zone', '${1:SeatZone}', 'SeatZone id or SeatZoneId field']
    ];
}
for (const triggerName of [...TRIGGER_PREDICATES, ...TRIGGER_EVENTS]) {
  ARGUMENT_SNIPPETS[triggerName] = [
    ['area', 'area: ${1:AreaName}', 'Named area field for spatial player expressions'],
    ['target', '"${1:minecraft:stone}"', 'Target item, block, effect, dimension, weather, or mode depending on expression'],
    ['count', 'count: ${1:1}', 'Item count threshold'],
    ['value', 'value: ${1:10}', 'Numeric threshold value'],
    ['level', 'level: ${1:1}', 'Experience level threshold'],
    ['day', 'day: ${1:1}', 'Day threshold'],
    ['start', 'start: ${1:0}', 'Range start, used by TimeBetween'],
    ['end', 'end: ${1:12000}', 'Range end, used by TimeBetween']
  ];
}
for (const compositeName of ['Any', 'All', 'Not']) {
  const defaultExpression = compositeName === 'Not' ? 'PlayerInside' : 'PlayerEnter';
  ARGUMENT_SNIPPETS[compositeName] = [
    ['expression', `${defaultExpression}(\${1:AreaName})`, 'Nested trigger expression']
  ];
}

ARGUMENT_SNIPPETS.Environment = [
  ['id', 'id: "${1:jdh:environment}"', 'Stable environment resource id']
];
ARGUMENT_SNIPPETS.State = [
  ['id', 'id: "${1:state_id}"', 'Environment-local state id']
];
ARGUMENT_SNIPPETS.Position = [
  ['x', 'x: ${1:0}', 'Block X'],
  ['y', 'y: ${1:64}', 'Block Y'],
  ['z', 'z: ${1:0}', 'Block Z']
];
ARGUMENT_SNIPPETS.Box = [
  ['min', 'Position(${1:0}, ${2:64}, ${3:0})', 'Minimum SeatZone corner'],
  ['max', 'Position(${4:2}, ${5:66}, ${6:2})', 'Maximum SeatZone corner']
];
ARGUMENT_SNIPPETS.Detect = [
  ['policy', '${1:RIDING_ANCHOR}', 'RIDING_ANCHOR or EXACT_SLOT_ONLY']
];
ARGUMENT_SNIPPETS.UnknownSeats = [
  ['policy', '${1:ALLOW}', 'ALLOW or REJECT']
];
ARGUMENT_SNIPPETS.MatchRadius = [
  ['radius', '${1:0.85}', 'Seat slot matching radius']
];
ARGUMENT_SNIPPETS.Presentation = [
  ['nameKey', 'nameKey: "${1:effect.jdh.effect_name}"', 'Effect display-name localization key'],
  ['descriptionKey', 'descriptionKey: "${1:effect.jdh.effect_name.description}"', 'Effect description localization key'],
  ['icon', 'icon: "${1:jdh:textures/echo_effects/icons/effect.png}"', 'Effect icon texture']
];
ARGUMENT_SNIPPETS.PlayerHud = [
  ['id', 'id: "${1:jdh:player_vitals_hud}"', 'HUD config id'],
  ['referenceWidth', 'referenceWidth: ${1:1920}', 'Reference canvas width'],
  ['referenceHeight', 'referenceHeight: ${1:1080}', 'Reference canvas height'],
  ['scaleWithResolution', 'scaleWithResolution: ${1:true}', 'Scale HUD with current resolution']
];
ARGUMENT_SNIPPETS.Bar = [
  ['id', 'id: "${1:echo_player_hud:health_bar}"', 'HUD bar id'],
  ['source', 'source: "${1:health}"', 'Metric source: health, spark, fatigue, stamina, or custom id'],
  ['metric', 'metric: "${1:health}"', 'Alias for source'],
  ['enabled', 'enabled: ${1:true}', 'Show this bar'],
  ['drawWhenUnavailable', 'drawWhenUnavailable: ${1:true}', 'Draw even when metric is unavailable'],
  ['icon', 'icon: "${1:echo_player_hud:textures/gui/health.png}"', 'Bar icon texture'],
  ['iconWidth', 'iconWidth: ${1:16}', 'Icon width'],
  ['iconHeight', 'iconHeight: ${1:16}', 'Icon height'],
  ['iconSpacing', 'iconSpacing: ${1:6}', 'Spacing between icon and bar'],
  ['iconOffsetX', 'iconOffsetX: ${1:0.0}', 'Icon X offset'],
  ['iconOffsetY', 'iconOffsetY: ${1:0.0}', 'Icon Y offset'],
  ['anchor', 'anchor: "${1:BOTTOM_LEFT}"', 'HUD anchor'],
  ['x', 'x: ${1:36}', 'X offset from anchor'],
  ['y', 'y: ${1:126}', 'Y offset from anchor'],
  ['width', 'width: ${1:260}', 'Bar width'],
  ['height', 'height: ${1:18}', 'Bar height'],
  ['orientation', 'orientation: "${1:HORIZONTAL}"', 'HORIZONTAL or VERTICAL'],
  ['fill', 'fill: "${1:LEFT_TO_RIGHT}"', 'Fill direction'],
  ['background', 'background: "${1:0x99000000}"', 'Background color'],
  ['borderWidth', 'borderWidth: ${1:1}', 'Border width'],
  ['borderColor', 'borderColor: "${1:#CC24151A}"', 'Border color'],
  ['gradientStart', 'gradientStart: "${1:#B23A48}"', 'Gradient start color'],
  ['gradientEnd', 'gradientEnd: "${1:#E85D75}"', 'Gradient end color'],
  ['gradientSteps', 'gradientSteps: ${1:1}', 'Gradient step count']
];
ARGUMENT_SNIPPETS.PlayerFatigue = [
  ['id', 'id: "${1:jdh:player_fatigue}"', 'Player fatigue config id'],
  ['maximum', 'maximum: ${1:100}', 'Maximum fatigue value'],
  ['restedReserveAbove', 'restedReserveAbove: ${1:0.50}', 'Reserve ratio above rested threshold'],
  ['tiredReserveAtMost', 'tiredReserveAtMost: ${1:0.50}', 'Reserve ratio at most for tired'],
  ['tiredReserveAbove', 'tiredReserveAbove: ${1:0.25}', 'Reserve ratio above exhausted threshold'],
  ['exhaustedReserveAtMost', 'exhaustedReserveAtMost: ${1:0.25}', 'Reserve ratio at most for exhausted'],
  ['tiredMovementMultiplier', 'tiredMovementMultiplier: ${1:0.95}', 'Movement multiplier while tired'],
  ['exhaustedMovementMultiplier', 'exhaustedMovementMultiplier: ${1:0.85}', 'Movement multiplier while exhausted'],
  ['exhaustedNausea', 'exhaustedNausea: ${1:0.35}', 'Nausea intensity while exhausted']
];
ARGUMENT_SNIPPETS.DialogueOverlayStyle = [
  ['textScale', 'textScale: "${1:1.0}"', 'Text scale'],
  ['textShadow', 'textShadow: ${1:true}', 'Draw text shadow'],
  ['textAppearDelayMs', 'textAppearDelayMs: ${1:80}', 'Text appear delay'],
  ['typewriterMsPerCharacter', 'typewriterMsPerCharacter: ${1:34}', 'Typewriter speed'],
  ['typewriterMinDurationMs', 'typewriterMinDurationMs: ${1:450}', 'Minimum typewriter duration'],
  ['typewriterMaxDurationMs', 'typewriterMaxDurationMs: ${1:5000}', 'Maximum typewriter duration'],
  ['anchorX', 'anchorX: "${1:0.5}"', 'Panel anchor X'],
  ['anchorY', 'anchorY: "${1:0.5}"', 'Panel anchor Y'],
  ['anchorOffsetX', 'anchorOffsetX: ${1:0}', 'Anchor X offset'],
  ['anchorOffsetY', 'anchorOffsetY: ${1:20}', 'Anchor Y offset'],
  ['crosshairPaddingY', 'crosshairPaddingY: ${1:10}', 'Crosshair vertical padding'],
  ['screenMarginX', 'screenMarginX: ${1:8}', 'Screen X margin'],
  ['screenMarginY', 'screenMarginY: ${1:8}', 'Screen Y margin'],
  ['minTextWidth', 'minTextWidth: ${1:30}', 'Minimum text width'],
  ['maxTextWidth', 'maxTextWidth: ${1:256}', 'Maximum text width'],
  ['paddingX', 'paddingX: ${1:8}', 'Panel horizontal padding'],
  ['paddingY', 'paddingY: ${1:4}', 'Panel vertical padding'],
  ['lineGap', 'lineGap: ${1:2}', 'Gap between text lines'],
  ['choiceSectionGap', 'choiceSectionGap: ${1:7}', 'Gap before choices'],
  ['choicePaddingX', 'choicePaddingX: ${1:6}', 'Choice horizontal padding'],
  ['choicePaddingY', 'choicePaddingY: ${1:4}', 'Choice vertical padding'],
  ['choiceGap', 'choiceGap: ${1:4}', 'Gap between choices'],
  ['visualSize', 'visualSize: ${1:28}', 'Visual size'],
  ['visualGap', 'visualGap: ${1:8}', 'Gap between visual and text'],
  ['visualTextOffsetY', 'visualTextOffsetY: ${1:1}', 'Text Y offset relative to visual'],
  ['visualOffsetX', 'visualOffsetX: ${1:0}', 'Visual X offset'],
  ['visualOffsetY', 'visualOffsetY: ${1:0}', 'Visual Y offset'],
  ['backgroundColor', 'backgroundColor: "${1:0xB0101010}"', 'Panel background color'],
  ['choiceBackgroundColor', 'choiceBackgroundColor: "${1:0x80181818}"', 'Choice background color'],
  ['choiceBorderColor', 'choiceBorderColor: "${1:0x806A6A6A}"', 'Choice border color'],
  ['speakerColor', 'speakerColor: "${1:0xFF6A00}"', 'Speaker name color'],
  ['textColor', 'textColor: "${1:0xFFFFFF}"', 'Text color'],
  ['choiceNumberColor', 'choiceNumberColor: "${1:0xFF6A00}"', 'Choice number color'],
  ['choiceTextColor', 'choiceTextColor: "${1:0xFFE6E6E6}"', 'Choice text color']
];
const DIALOGUE_PANEL_BACKGROUND_ARGUMENTS = [
  ['mode', 'mode: "${1:sliced}"', 'Background render mode: solid, sliced, or tiled'],
  ['texture', 'texture: "${1:jdh:textures/echo_dialogue/gui/panel/dialogue_panel.png}"', 'Panel texture resource location'],
  ['textureWidth', 'textureWidth: ${1:29}', 'Source texture width'],
  ['textureHeight', 'textureHeight: ${1:28}', 'Source texture height'],
  ['sliceLeft', 'sliceLeft: ${1:6}', 'Nine-slice left border'],
  ['sliceRight', 'sliceRight: ${1:6}', 'Nine-slice right border'],
  ['sliceTop', 'sliceTop: ${1:6}', 'Nine-slice top border'],
  ['sliceBottom', 'sliceBottom: ${1:8}', 'Nine-slice bottom border'],
  ['tileScaleX', 'tileScaleX: "${1:1.0}"', 'Tiled mode X scale'],
  ['tileScaleY', 'tileScaleY: "${1:1.0}"', 'Tiled mode Y scale'],
  ['tileOriginX', 'tileOriginX: ${1:0}', 'Tiled mode X origin'],
  ['tileOriginY', 'tileOriginY: ${1:0}', 'Tiled mode Y origin'],
  ['tileFullOnly', 'tileFullOnly: ${1:true}', 'Draw only full tiles'],
  ['fillDirection', 'fillDirection: "${1:LEFT_TO_RIGHT}"', 'Fill direction'],
  ['fillAmount', 'fillAmount: "${1:1.0}"', 'Fill amount from 0.0 to 1.0']
];
ARGUMENT_SNIPPETS.DialoguePanelBackgroundOverlay = DIALOGUE_PANEL_BACKGROUND_ARGUMENTS;
ARGUMENT_SNIPPETS.DialogueChoiceBackgroundOverlay = DIALOGUE_PANEL_BACKGROUND_ARGUMENTS;
ARGUMENT_SNIPPETS.PortraitOverlay = [
  ['visualId', '"${1:jdh:character}"', 'Portrait visual id'],
  ['centerX', 'centerX: "${1:0.50}"', 'Face center X'],
  ['centerY', 'centerY: "${1:0.49}"', 'Face center Y'],
  ['offsetX', 'offsetX: ${1:0}', 'Global overlay X offset'],
  ['offsetY', 'offsetY: ${1:0}', 'Global overlay Y offset'],
  ['facePixels', 'facePixels: "${1:4}"', 'Face-pixel step'],
  ['layers', 'layers: [Layer("${1:pupil_left}", placeX: "${2:-0.062}", placeY: "${3:0.002}", scaleX: "${4:0.50}", scaleY: "${5:0.50}")]', 'Portrait overlay layers'],
  ['idle', 'idle: [Blink "${1:eye_closure}" { Travel(${2:20}, ${3:-20}); Gaze(${4:1}, ${5:1}); Idle(${6:true}); }]', 'Idle blink blocks']
];
ARGUMENT_SNIPPETS.Layer = [
  ['bone', 'bone: "${1:face_root}"', 'FaceRig parent bone'],
  ['texture', 'texture: "${1:eyes}"', 'FaceRig layer texture'],
  ['role', 'role: "${1:pupil}"', 'FaceRig layer role'],
  ['order', 'order: ${1:0}', 'FaceRig draw order'],
  ['placeX', 'placeX: "${1:0.0}"', 'Portrait layer normalized X position'],
  ['placeY', 'placeY: "${1:0.0}"', 'Portrait layer normalized Y position'],
  ['offsetX', 'offsetX: "${1:0.0}"', 'Portrait layer X offset'],
  ['offsetY', 'offsetY: "${1:0.0}"', 'Portrait layer Y offset'],
  ['scaleX', 'scaleX: "${1:1.0}"', 'Portrait layer X scale'],
  ['scaleY', 'scaleY: "${1:1.0}"', 'Portrait layer Y scale'],
  ['w', 'w: "${1:1.0}"', 'Portrait layer width override'],
  ['h', 'h: "${1:1.0}"', 'Portrait layer height override'],
  ['visible', 'visible: ${1:true}', 'Layer visibility']
];
ARGUMENT_SNIPPETS.Slot = [
  ...(ARGUMENT_SNIPPETS.Slot || []),
  ['id', '"${1:chair}"', 'SeatZone slot id'],
  ['anchor', 'Position(${1:1}, ${2:64}, ${3:1})', 'SeatZone slot anchor']
];
ARGUMENT_SNIPPETS.SetBlock = [
  ['properties', 'properties: Properties(Property("${1:facing}", "${2:north}"))', 'Block state properties'],
  ['nbt', 'nbt: "${1:{}}"', 'Optional block NBT']
];
ARGUMENT_SNIPPETS.Property = [
  ['name', 'name: "${1:key}"', 'Property name'],
  ['value', 'value: "${1:value}"', 'Property value']
];

ARGUMENT_SNIPPETS.Duration = [
  ['duration', 'Ticks(${1:20})', 'Effect duration']
];
ARGUMENT_SNIPPETS.Stacking = [
  ['policy', '${1:REFRESH_DURATION}', 'IGNORE_NEW, REFRESH_DURATION, RESTART, STACK_INTENSITY, ALLOW_MULTIPLE, REPLACE_OLD']
];
ARGUMENT_SNIPPETS.Lifetime = [
  ['policy', '${1:OWNER_BOUND}', 'INDEPENDENT, OWNER_BOUND, SESSION_BOUND, UNTIL_REMOVED']
];
ARGUMENT_SNIPPETS.EndOn = [
  ['condition', '${1:OWNER_COMPLETED}', 'Effect end condition']
];
ARGUMENT_SNIPPETS.Ticks = [
  ['value', '${1:20}', 'Tick count']
];
ARGUMENT_SNIPPETS.Seconds = [
  ['value', '${1:1.0}', 'Seconds']
];
ARGUMENT_SNIPPETS.Color = [
  ['hex', '"${1:#000000}"', 'Hex color']
];
ARGUMENT_SNIPPETS.Curve = [
  ['key', 'Key(Ticks(${1:0}), ${2:1.0})', 'Curve key']
];
ARGUMENT_SNIPPETS.Key = [
  ['time', 'Ticks(${1:0})', 'Key time'],
  ['value', '${1:1.0}', 'Key value'],
  ['interpolation', '${1:LINEAR}', 'STEP, LINEAR, SMOOTH, CUBIC']
];
ARGUMENT_SNIPPETS['Movement.SpeedMultiplier'] = [
  ['curve', 'Curve(Key(Ticks(${1:0}), ${2:1.0}), Key(Ticks(${3:20}), ${4:0.5}))', 'Speed multiplier curve']
];
ARGUMENT_SNIPPETS['Camera.FovOffset'] = ARGUMENT_SNIPPETS['Movement.SpeedMultiplier'];
ARGUMENT_SNIPPETS['Camera.Roll'] = ARGUMENT_SNIPPETS['Movement.SpeedMultiplier'];
ARGUMENT_SNIPPETS['Camera.Shake'] = [
  ['amplitude', 'amplitude: Curve(Key(Ticks(${1:0}), ${2:0.0}), Key(Ticks(${3:10}), ${4:1.0}))', 'Shake amplitude curve'],
  ['frequency', 'frequency: ${1:5.5}', 'Shake frequency'],
  ['seed', 'seed: ${1:STABLE}', 'Shake seed/stability mode']
];
ARGUMENT_SNIPPETS['Screen.Tint'] = [
  ['color', 'color: Color("${1:#000000}")', 'Tint color'],
  ['opacity', 'opacity: Curve(Key(Ticks(${1:0}), ${2:0.0}), Key(Ticks(${3:20}), ${4:1.0}))', 'Opacity curve'],
  ['layer', 'layer: ${1:BELOW_UI}', 'Screen effect layer']
];
ARGUMENT_SNIPPETS['Screen.Vignette'] = ARGUMENT_SNIPPETS['Screen.Tint'];
ARGUMENT_SNIPPETS['Sound.PlayAt'] = [
  ['time', 'time: Ticks(${1:0})', 'Sound event time'],
  ['sound', 'sound: "${1:minecraft:block.note_block.pling}"', 'Minecraft sound id'],
  ['volume', 'volume: ${1:1.0}', 'Sound volume'],
  ['pitch', 'pitch: ${1:1.0}', 'Sound pitch']
];
ARGUMENT_SNIPPETS['VanillaEffect.Apply'] = [
  ['time', 'time: Ticks(${1:0})', 'Vanilla effect event time'],
  ['effect', 'effect: "${1:minecraft:nausea}"', 'Minecraft MobEffect id'],
  ['duration', 'duration: Ticks(${1:140})', 'Vanilla effect duration'],
  ['amplifier', 'amplifier: ${1:0}', 'Vanilla effect amplifier'],
  ['ambient', 'ambient: ${1:false}', 'Vanilla ambient flag'],
  ['visible', 'visible: ${1:true}', 'Show vanilla particles'],
  ['showIcon', 'showIcon: ${1:true}', 'Show vanilla HUD icon']
];
ARGUMENT_SNIPPETS['Effect.ApplyToPlayer'] = [
  ['intensity', 'intensity: ${1:1.0}', 'Effect intensity']
];
ARGUMENT_SNIPPETS['Effect.RemoveFromPlayer'] = [
  ['fadeOut', 'fadeOut: Ticks(${1:10})', 'Fade-out duration']
];
for (const effectQuery of ['Effect.WaitOnPlayer', 'Effect.IsActiveOnPlayer', 'Effect.HasCompletedOnPlayer']) {
  ARGUMENT_SNIPPETS[effectQuery] = [
    ['effectId', '"${1:jdh:effect_id}"', 'Effect resource id']
  ];
}
ARGUMENT_SNIPPETS.Profile = [
  ['profileId', '"${1:jdh:wardrobe/default}"', 'Wardrobe profile id']
];
ARGUMENT_SNIPPETS.Item = [
  ['itemId', '"${1:minecraft:leather_chestplate}"', 'Minecraft item id']
];
ARGUMENT_SNIPPETS.Slot = [
  ...(ARGUMENT_SNIPPETS.Slot || []),
  ['slot', '${1:TOP}', 'FACE, TOP, BOTTOM, FEET']
];
ARGUMENT_SNIPPETS.EquipmentSlot = [
  ['slot', '${1:CHEST}', 'HEAD, CHEST, LEGS, FEET']
];
ARGUMENT_SNIPPETS.Collection = [
  ['collection', '${1:CASUAL}', 'Collection symbol']
];
ARGUMENT_SNIPPETS.Model = [
  ['modelId', '"${1:jdh:wearable/model}"', 'Wearable model id']
];
for (const transformCall of ['ModelPosition', 'ModelRotation', 'ModelScale']) {
  ARGUMENT_SNIPPETS[transformCall] = [
    ['x', '${1:0}', 'X value'],
    ['y', '${1:0}', 'Y value'],
    ['z', '${1:0}', 'Z value']
  ];
}
ARGUMENT_SNIPPETS.PortraitTexture = [
  ['textureId', '"${1:jdh:textures/portrait}"', 'Portrait texture id']
];
ARGUMENT_SNIPPETS.PortraitStrip = [
  ['srcX', '${1:0}', 'Source X'],
  ['srcY', '${1:0}', 'Source Y'],
  ['srcW', '${1:16}', 'Source width'],
  ['srcH', '${1:16}', 'Source height'],
  ['dstX', '${1:0}', 'Target X'],
  ['dstY', '${1:0}', 'Target Y'],
  ['layer', '${1:BASE}', 'BASE or OUTER']
];
ARGUMENT_SNIPPETS.BaseSkin = [
  ['skinId', '"${1:jdh:base_skin}"', 'Base skin id']
];
ARGUMENT_SNIPPETS.Coverage = [
  ['policy', '${1:REPORT}', 'REPORT or STRICT']
];
ARGUMENT_SNIPPETS.Slots = [
  ['slot', '${1:FACE}', 'Wardrobe slot']
];
ARGUMENT_SNIPPETS.Match = [
  ['face', 'face: "${1:*}"', 'Face selection'],
  ['top', 'top: "${1:*}"', 'Top selection'],
  ['bottom', 'bottom: "${1:*}"', 'Bottom selection'],
  ['feet', 'feet: "${1:*}"', 'Feet selection']
];
ARGUMENT_SNIPPETS.WorldSkin = ARGUMENT_SNIPPETS.BaseSkin;
ARGUMENT_SNIPPETS.FaceSkin = ARGUMENT_SNIPPETS.BaseSkin;
ARGUMENT_SNIPPETS.FaceRig = [
  ['bones', 'bones: [Bone("${1:face_root}")]', 'Face rig bones'],
  ['layers', 'layers: [Layer("${1:eye_left}", bone: "${2:eye_root}", texture: "${3:eyes}")]', 'Face rig layers'],
  ['controls', 'controls: [BlinkControl("${1:blink}")]', 'Face rig controls']
];
ARGUMENT_SNIPPETS.Rig = [
  ['id', '"${1:jdh:face_rig}"', 'Face rig id']
];
ARGUMENT_SNIPPETS.Surface = [
  ['rig', 'rig: "${1:jdh:face_rig}"', 'Surface rig id'],
  ['enter', 'enter: Enter(durationTicks: ${1:4})', 'Enter phase'],
  ['stay', 'stay: Stay(durationTicks: ${1:12})', 'Stay phase'],
  ['exit', 'exit: Exit(durationTicks: ${1:4})', 'Exit phase']
];
const FACIAL_PHASE_ARGUMENTS = [
  ['durationTicks', 'durationTicks: ${1:12}', 'Duration in ticks'],
  ['durationMillis', 'durationMillis: ${1:600}', 'Duration in milliseconds'],
  ['end', 'end: "${1:HOLD}"', 'End behavior'],
  ['tracks', 'tracks: [Track("${1:bone_id}", frames: [Frame(0).Position(0, 0, 0)])]', 'Phase tracks']
];
for (const phaseCall of ['Phase', 'Enter', 'Stay']) {
  ARGUMENT_SNIPPETS[phaseCall] = FACIAL_PHASE_ARGUMENTS;
}
ARGUMENT_SNIPPETS.Exit = [
  ...(ARGUMENT_SNIPPETS.Exit || []),
  ...FACIAL_PHASE_ARGUMENTS
];
ARGUMENT_SNIPPETS.Socket = [
  ['bone', 'bone: "${1:right_forearm}"', 'Parent bone id'],
  ['x', 'x: ${1:0}', 'Local X'],
  ['y', 'y: ${1:0}', 'Local Y'],
  ['z', 'z: ${1:0}', 'Local Z']
];
ARGUMENT_SNIPPETS.BlinkControl = [
  ['upperTravel', 'upperTravel: ${1:1.0}', 'Upper eyelid travel'],
  ['lowerTravel', 'lowerTravel: ${1:0.5}', 'Lower eyelid travel'],
  ['idle', 'idle: ${1:true}', 'Enable idle blink'],
  ['periodTicks', 'periodTicks: ${1:80}', 'Blink period'],
  ['closeTicks', 'closeTicks: ${1:3}', 'Close duration'],
  ['holdTicks', 'holdTicks: ${1:1}', 'Hold duration'],
  ['openTicks', 'openTicks: ${1:4}', 'Open duration']
];
ARGUMENT_SNIPPETS.Textures = [
  ['root', '"${1:jdh:textures/entity/face}"', 'Texture root']
];
ARGUMENT_SNIPPETS.Texture = [
  ['textureId', '"${1:eyes}"', 'Texture id']
];
ARGUMENT_SNIPPETS.TextureArea = [
  ['x', '${1:0}', 'Texture X'],
  ['y', '${1:0}', 'Texture Y'],
  ['width', '${1:16}', 'Width'],
  ['height', '${1:16}', 'Height']
];
ARGUMENT_SNIPPETS.Size = [
  ['width', '${1:16}', 'Width'],
  ['height', '${1:16}', 'Height']
];
ARGUMENT_SNIPPETS.Order = [
  ['value', '${1:0}', 'Draw order']
];
ARGUMENT_SNIPPETS.Role = [
  ['role', '"${1:DECORATION}"', 'Layer role']
];
ARGUMENT_SNIPPETS.Parent = [
  ['parentBoneId', '"${1:face_root}"', 'Parent bone id']
];
ARGUMENT_SNIPPETS.Pivot = [
  ['x', '${1:0}', 'Pivot X'],
  ['y', '${1:0}', 'Pivot Y']
];
ARGUMENT_SNIPPETS.Travel = [
  ['x', '${1:0}', 'Travel X'],
  ['y', '${1:1}', 'Travel Y']
];
ARGUMENT_SNIPPETS.Gaze = [
  ['x', '${1:0}', 'Gaze X'],
  ['y', '${1:0}', 'Gaze Y']
];
ARGUMENT_SNIPPETS.Timing = [
  ['periodTicks', '${1:80}', 'Blink period'],
  ['closeTicks', '${1:3}', 'Close duration'],
  ['holdTicks', '${1:1}', 'Hold duration'],
  ['openTicks', '${1:4}', 'Open duration']
];
for (const blinkTimingCall of ['PeriodTicks', 'CloseTicks', 'HoldTicks', 'OpenTicks']) {
  ARGUMENT_SNIPPETS[blinkTimingCall] = [
    ['value', '${1:4}', 'Tick count']
  ];
}

ARGUMENT_SNIPPETS['Story.Set'] = [
  ['next', 'next: "${1:next_node}"', 'Next local story node id']
];
ARGUMENT_SNIPPETS['Story.Load'] = [
  ['storyId', 'storyId: ${1:Stories.Story}', 'Story resource id'],
  ['executionId', 'executionId: "${1:jdh:execution_id}"', 'Explicit child execution id'],
  ['next', 'next: "${1:next_node}"', 'Next local story node id']
];
ARGUMENT_SNIPPETS['Dialogue.Load'] = [
  ['dialogueId', 'dialogueId: ${1:Dialogues.Dialogue}', 'Dialogue resource id'],
  ['audienceId', 'audienceId: "${1:player}"', 'Dialogue audience id'],
  ...STORY_ACTION_CONTROL_ARGUMENTS
];
ARGUMENT_SNIPPETS['Dialogue.Start'] = [
  ['dialogue', 'dialogue: ${1:Dialogues.Dialogue}', 'Dialogue resource id'],
  ['sessionId', 'sessionId: "${1:trigger_session}"', 'Explicit dialogue session id'],
  ['audienceId', 'audienceId: "${1:player}"', 'Explicit dialogue audience id']
];
ARGUMENT_SNIPPETS['Environment.Set'] = [
  ['environmentId', 'environmentId: ${1:Environments.Environment}', 'Environment resource id'],
  ['stateId', 'stateId: "${1:state_id}"', 'Environment-local state id'],
  ['next', 'next: "${1:next_node}"', 'Next local story node id']
];
ARGUMENT_SNIPPETS['Screen.SetBlack'] = [
  ['canvasGroup', 'canvasGroup: "${1:default}"', 'Screen canvas group id'],
  ['alpha', 'alpha: ${1:1.0}', 'Target black overlay alpha'],
  ['time', 'time: ${1:500}', 'Transition duration'],
  ['durationMillis', 'durationMillis: ${1:500}', 'Transition duration in milliseconds'],
  ...STORY_ACTION_CONTROL_ARGUMENTS
];
ARGUMENT_SNIPPETS['Player.SpawnAt'] = [
  ['yaw', 'yaw: ${1:0}', 'Player yaw'],
  ['pitch', 'pitch: ${1:0}', 'Player pitch'],
  ...STORY_ACTION_CONTROL_ARGUMENTS
];
ARGUMENT_SNIPPETS['Player.SetHotbarItem'] = [
  ['slot', 'slot: ${1:0}', 'Hotbar slot index'],
  ['itemId', 'itemId: "${1:minecraft:stick}"', 'Item id'],
  ['count', 'count: ${1:1}', 'Stack count'],
  ['replace', 'replace: ${1:true}', 'Replace existing stack'],
  ['next', 'next: "${1:next_node}"', 'Next local story node id']
];
ARGUMENT_SNIPPETS['Player.SelectHotbarSlot'] = [
  ['slot', 'slot: ${1:0}', 'Hotbar slot index'],
  ['next', 'next: "${1:next_node}"', 'Next local story node id']
];
ARGUMENT_SNIPPETS['Player.SitAt'] = [
  ['timeoutTicks', 'timeoutTicks: ${1:120}', 'Interaction timeout in ticks'],
  ['next', 'next: "${1:next_node}"', 'Next local story node id']
];
ARGUMENT_SNIPPETS['Player.Teleport'] = [
  ['instanceId', 'instanceId: "${1:chapter_1_blackspace}"', 'Blackspace instance id'],
  ['target', 'target: "${1:entry}"', 'Teleport target id'],
  ['playerName', 'playerName: "${1:player}"', 'Player name'],
  ['playerUuid', 'playerUuid: "${1:uuid}"', 'Player UUID'],
  ...STORY_ACTION_CONTROL_ARGUMENTS
];
ARGUMENT_SNIPPETS['Blackspace.Create'] = [
  ['instanceId', 'instanceId: "${1:chapter_1_blackspace}"', 'Blackspace instance id'],
  ['id', 'id: "${1:chapter_1_blackspace}"', 'Alias for instanceId'],
  ['playerName', 'playerName: "${1:player}"', 'Player name'],
  ['playerUuid', 'playerUuid: "${1:uuid}"', 'Player UUID'],
  ...STORY_ACTION_CONTROL_ARGUMENTS
];
ARGUMENT_SNIPPETS['Npc.SpawnAt'] = [
  ['yaw', 'yaw: ${1:0}', 'NPC yaw'],
  ['pitch', 'pitch: ${1:0}', 'NPC pitch'],
  ['worldId', 'worldId: "minecraft:overworld"', 'Spawn world/dimension id']
];
ARGUMENT_SNIPPETS['Npc.SleepAt'] = [
  ['expectedBlock', 'expectedBlock: "${1:minecraft:red_bed}"', 'Expected bed/block id'],
  ['worldId', 'worldId: "minecraft:overworld"', 'Bed world/dimension id']
];
ARGUMENT_SNIPPETS['Npc.Interact'] = [
  ['npc', 'npc: ${1:Sasha}', 'NPC actor'],
  ['position', 'position: ${1:TargetBlock}', 'BlockPosition field or new BlockPosition(...)'],
  ['block', 'block: ${1:TargetBlock}', 'Alias for position'],
  ['targetBlock', 'targetBlock: ${1:TargetBlock}', 'Alias for position'],
  ['x', 'x: ${1:0}', 'Interaction X'],
  ['y', 'y: ${1:64}', 'Interaction Y'],
  ['z', 'z: ${1:0}', 'Interaction Z'],
  ['dimension', 'dimension: "minecraft:overworld"', 'Interaction dimension'],
  ['face', 'face: "AUTO"', 'Block face'],
  ['hand', 'hand: "MAIN_HAND"', 'Interaction hand'],
  ['approach', 'approach: "AUTO"', 'Approach policy'],
  ...STORY_ACTION_CONTROL_ARGUMENTS
];
ARGUMENT_SNIPPETS['Npc.StopInteraction'] = [
  ['npc', 'npc: ${1:Sasha}', 'NPC actor'],
  ...STORY_ACTION_CONTROL_ARGUMENTS
];
ARGUMENT_SNIPPETS['Npc.StandUp'] = ARGUMENT_SNIPPETS['Npc.StopInteraction'];
ARGUMENT_SNIPPETS['Npc.Stop'] = ARGUMENT_SNIPPETS['Npc.StopInteraction'];
ARGUMENT_SNIPPETS['Debug.Mark'] = [
  ['label', '"${1:label}"', 'Debug marker label']
];

ARGUMENT_SNIPPETS['Sleep.Arm'] = [
  ['scenario', 'scenario: "${1:default}"', 'Sleep scenario id'],
  ...SLEEP_CONTEXT_ARGUMENTS
];
ARGUMENT_SNIPPETS['Sleep.CloseEyes'] = [
  ['duration', 'duration: ${1:20}', 'Close duration'],
  ...SLEEP_CONTEXT_ARGUMENTS
];
ARGUMENT_SNIPPETS['Sleep.EnterDream'] = [
  ['target', 'target: "${1:chapter_1_dream}"', 'Dream target id'],
  ['blackspaceInstanceId', 'blackspaceInstanceId: "${1:chapter_1_blackspace}"', 'Blackspace instance id'],
  ['blackspaceId', 'blackspaceId: "${1:chapter_1_blackspace}"', 'Blackspace definition id'],
  ['dimension', 'dimension: "minecraft:overworld"', 'Target dimension'],
  ['worldId', 'worldId: "minecraft:overworld"', 'Target world/dimension id'],
  ['x', 'x: ${1:0}', 'Target X'],
  ['y', 'y: ${1:64}', 'Target Y'],
  ['z', 'z: ${1:0}', 'Target Z'],
  ['yaw', 'yaw: ${1:0}', 'Target yaw'],
  ['pitch', 'pitch: ${1:0}', 'Target pitch'],
  ...SLEEP_CONTEXT_ARGUMENTS
];
ARGUMENT_SNIPPETS['Sleep.OpenEyes'] = [
  ['duration', 'duration: ${1:20}', 'Open duration'],
  ['sleepyBlink', 'sleepyBlink: ${1:true}', 'Use sleepy blink'],
  ['showHud', 'showHud: ${1:true}', 'Restore HUD'],
  ...SLEEP_CONTEXT_ARGUMENTS
];
ARGUMENT_SNIPPETS['Sleep.Wait'] = [
  ['seconds', 'seconds: ${1:1.0}', 'Wait seconds'],
  ['duration', 'duration: ${1:20}', 'Wait duration'],
  ['ticks', 'ticks: ${1:20}', 'Wait ticks'],
  ...SLEEP_CONTEXT_ARGUMENTS
];
ARGUMENT_SNIPPETS['Sleep.ReturnToBed'] = [
  ['mode', 'mode: "${1:default}"', 'Return mode'],
  ...SLEEP_CONTEXT_ARGUMENTS
];
for (const sleepTarget of ['Disarm', 'WaitUntilClosed', 'WaitUntilOpen', 'CompleteNight', 'Complete', 'Cancel', 'IsActive', 'IsArmed', 'IsEyesClosed']) {
  ARGUMENT_SNIPPETS[`Sleep.${sleepTarget}`] = SLEEP_CONTEXT_ARGUMENTS;
}
ARGUMENT_SNIPPETS['Sleep.WaitSeconds'] = ARGUMENT_SNIPPETS['Sleep.Wait'];

ARGUMENT_SNIPPETS['Npc.MoveTo'] = ARGUMENT_SNIPPETS['Npc.Move'];
ARGUMENT_SNIPPETS['Npc.WalkTo'] = ARGUMENT_SNIPPETS['Npc.Move'];
ARGUMENT_SNIPPETS['Npc.MoveToPlayer'] = ARGUMENT_SNIPPETS['Npc.ApproachPlayer'];
ARGUMENT_SNIPPETS['Npc.WalkToPlayer'] = ARGUMENT_SNIPPETS['Npc.ApproachPlayer'];
ARGUMENT_SNIPPETS['Npc.Turn'] = ARGUMENT_SNIPPETS['Npc.Rotate'];
ARGUMENT_SNIPPETS['This.Turn'] = ARGUMENT_SNIPPETS['This.Rotate'];
ARGUMENT_SNIPPETS['Npc.InteractBlock'] = ARGUMENT_SNIPPETS['Npc.UseBlock'];

function echoScriptCompletionEntries(text, offset, index) {
  const source = String(text || '');
  const before = source.slice(0, Math.max(0, offset));
  const eventEntries = eventCompletions(before, index);
  if (eventEntries !== null) return eventEntries;
  const memberContext = completionMemberTarget(source, before);
  if (memberContext) {
    const namespaceEntries = resourceNamespaceMemberCompletions(index, memberContext.target);
    const staticEntries = staticNamespaceMemberCompletions(memberContext.target);
    return namespaceEntries.length > 0 || staticEntries.length > 0
      ? dedupeEntries([...namespaceEntries, ...staticEntries])
      : memberCompletions(memberContext.target, memberContext.mode);
  }
  const argumentContext = completionArgumentContext(source, offset);
  if (argumentContext) {
    const argumentEntries = argumentCompletions(argumentContext);
    const referenceEntries = argumentReferenceCompletions(index, argumentContext);
    const staticEntries = argumentStaticCompletions(argumentContext);
    if (argumentEntries.length > 0 || referenceEntries.length > 0 || staticEntries.length > 0) {
      const generalResources = referenceEntries.length > 0 || staticEntries.length > 0
        ? []
        : resourceCompletions(index);
      return dedupeEntries([
        ...referenceEntries,
        ...staticEntries,
        ...argumentEntries,
        ...localCompletions(source),
        ...generalResources,
        ...stateRefCompletions(source),
        ...stateKeyCompletions(index)
      ]);
    }
  }
  return dedupeEntries([
    ...GLOBAL_SNIPPETS,
    ...localCompletions(source),
    ...resourceCompletions(index),
    ...stateRefCompletions(source),
    ...stateKeyCompletions(index)
  ]);
}

function completionMemberTarget(source, before) {
  const methodBuilders = collectLocalMethodReturnBuilders(source);
  const builderVariables = collectBuilderVariables(source, methodBuilders);
  const explicitMember = /\b([A-Za-z_][A-Za-z0-9_]*)\.\s*$/.exec(before);
  if (explicitMember) {
    const builder = builderVariables.get(explicitMember[1]);
    return builder
      ? { target: builder, mode: 'builder' }
      : { target: explicitMember[1], mode: 'namespace' };
  }

  const lineStart = before.lastIndexOf('\n') + 1;
  const currentLine = before.slice(lineStart);
  if (!/\.\s*$/.test(currentLine)) {
    return '';
  }

  const dotColumn = currentLine.lastIndexOf('.');
  const sameLineBuilder = lastBuilderCallName(currentLine.slice(0, dotColumn), methodBuilders);
  if (sameLineBuilder) {
    return { target: sameLineBuilder, mode: 'builder' };
  }

  if (/^\s*\.\s*$/.test(currentLine)) {
    const builder = inferFluentBuilder(before, methodBuilders);
    return builder ? { target: builder, mode: 'builder' } : null;
  }

  return null;
}

function completionArgumentContext(source, offset) {
  const masked = maskStringsAndComments(String(source || ''));
  if (isMaskedOffset(source, masked, offset)) {
    return null;
  }
  let depth = 0;
  for (let index = Math.max(0, offset - 1); index >= 0; index -= 1) {
    const char = masked[index];
    if (char === ')' || char === ']' || char === '}') {
      depth += 1;
      continue;
    }
    if (char === '(' || char === '[' || char === '{') {
      if (depth > 0) {
        depth -= 1;
        continue;
      }
      if (char !== '(') {
        return null;
      }
      const name = calleeNameBefore(masked, index);
      if (!name) {
        return null;
      }
      const bodyBeforeCursor = masked.slice(index + 1, Math.max(index + 1, offset));
      const used = new Set([...bodyBeforeCursor.matchAll(/\b([A-Za-z_][A-Za-z0-9_]*)\s*:/g)].map(match => match[1]));
      return {
        target: name,
        used,
        ...argumentCursorState(bodyBeforeCursor)
      };
    }
    if (depth === 0 && /[;{}]/.test(char)) {
      return null;
    }
  }
  return null;
}

function argumentCursorState(bodyBeforeCursor) {
  const body = String(bodyBeforeCursor || '');
  let depth = 0;
  let argumentIndex = 0;
  let segmentStart = 0;
  for (let index = 0; index < body.length; index += 1) {
    const char = body[index];
    if (char === '(' || char === '[' || char === '{') {
      depth += 1;
      continue;
    }
    if (char === ')' || char === ']' || char === '}') {
      depth = Math.max(0, depth - 1);
      continue;
    }
    if (char === ',' && depth === 0) {
      argumentIndex += 1;
      segmentStart = index + 1;
    }
  }
  const currentArgument = body.slice(segmentStart);
  const named = /^\s*([A-Za-z_][A-Za-z0-9_]*)\s*:/.exec(currentArgument);
  return {
    argumentIndex,
    namedArg: named ? named[1] : ''
  };
}

function isMaskedOffset(source, masked, offset) {
  const index = Math.max(0, Math.min(String(source || '').length - 1, offset - 1));
  if (index < 0) {
    return false;
  }
  const original = String(source || '')[index];
  return masked[index] === ' ' && !/\s/.test(original);
}

function calleeNameBefore(masked, openParenIndex) {
  const before = masked.slice(0, openParenIndex).trimEnd();
  const member = /([A-Za-z_][A-Za-z0-9_]*)\s*\.\s*([A-Za-z_][A-Za-z0-9_]*)$/.exec(before);
  if (member) {
    return `${member[1]}.${member[2]}`;
  }
  const plain = /([A-Za-z_][A-Za-z0-9_]*)$/.exec(before);
  return plain ? plain[1] : '';
}

function argumentCompletions(context) {
  const snippets = ARGUMENT_SNIPPETS[context.target] || [];
  return snippets
    .filter(([label]) => !context.used.has(label))
    .map(([label, insertText, detail], index) => ({
      label,
      insertText,
      detail,
      kind: 'snippet',
      sortText: `000_arg_${context.target}_${String(index).padStart(2, '0')}`
    }));
}

function argumentReferenceCompletions(index, context) {
  const kinds = referenceKindsForArgument(context);
  if (kinds.length === 0) {
    return [];
  }
  return resourceCompletions(index, kinds).map((entry, itemIndex) => ({
    ...entry,
    sortText: `010_ref_${String(itemIndex).padStart(3, '0')}_${entry.sortText}`
  }));
}

function referenceKindsForArgument(context) {
  if (context.namedArg && NAMED_REFERENCE_KINDS[context.namedArg]) {
    return NAMED_REFERENCE_KINDS[context.namedArg];
  }
  const rules = POSITIONAL_REFERENCE_KINDS[context.target] || {};
  return rules[context.argumentIndex] || rules['*'] || [];
}

function argumentStaticCompletions(context) {
  const namespaces = (NAMED_STATIC_NAMESPACES[context.target] || {})[context.namedArg];
  if (!context.namedArg || !namespaces) {
    return [];
  }
  return staticNamespaceCompletions(namespaces, '020_static_arg', true);
}

function memberCompletions(target, mode = 'namespace') {
  const snippets = mode === 'builder'
    ? (BUILDER_MEMBER_SNIPPETS[target] || MEMBER_SNIPPETS[target] || [])
    : (MEMBER_SNIPPETS[target] || []);
  return snippets.map(([label, insertText, detail], index) => ({
    label,
    insertText,
    detail,
    kind: 'snippet',
    sortText: `000_${target}_${String(index).padStart(2, '0')}`
  }));
}

function staticNamespaceMemberCompletions(namespace) {
  return staticNamespaceCompletions([namespace], `000_${namespace}`, false);
}

function staticNamespaceCompletions(namespaces, sortPrefix, includeQualified) {
  const result = [];
  for (const namespace of namespaces || []) {
    const members = STATIC_NAMESPACE_MEMBERS[namespace] || [];
    for (let index = 0; index < members.length; index += 1) {
      const [member, value] = members[index];
      result.push({
        label: member,
        insertText: member,
        detail: `${namespace}.${member} -> ${value}`,
        kind: 'value',
        sortText: `${sortPrefix}_${namespace}_${String(index).padStart(2, '0')}`
      });
      if (includeQualified) {
        result.push({
          label: `${namespace}.${member}`,
          insertText: `${namespace}.${member}`,
          detail: `${namespace}.${member} -> ${value}`,
          kind: 'value',
          sortText: `${sortPrefix}_${namespace}_${String(index).padStart(2, '0')}_qualified`
        });
      }
    }
  }
  return result;
}

function collectBuilderVariables(text, methodBuilders = new Map()) {
  const result = new Map();
  for (const rawLine of String(text || '').split(/\r?\n/)) {
    const line = maskStringsAndComments(rawLine);
    const match = /^\s*(?:(?:public|private|static)\s+)*(var|[A-Za-z_][A-Za-z0-9_.]*(?:\[\])?)\s+([A-Za-z_][A-Za-z0-9_]*)\s*(?:=\s*(?:new\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*\(|=|;)/.exec(line);
    if (!match) {
      continue;
    }
    const declaredBuilder = builderFromTypeName(match[1]);
    if (declaredBuilder) {
      result.set(match[2], declaredBuilder);
      continue;
    }
    const initializerName = match[3] || '';
    const initializerBuilder = MEMBER_SNIPPETS[initializerName]
      ? initializerName
      : methodBuilders.get(initializerName);
    if (initializerBuilder) {
      result.set(match[2], initializerBuilder);
    }
  }
  return result;
}

function collectLocalMethodReturnBuilders(text) {
  const result = new Map();
  const pattern = /^\s*(?:(?:public|private|static|override)\s+)*([A-Za-z_][A-Za-z0-9_.]*(?:\[\])?)\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(/gm;
  for (const match of String(text || '').matchAll(pattern)) {
    const builder = builderFromTypeName(match[1]);
    if (builder) {
      result.set(match[2], builder);
    }
  }
  return result;
}

function builderFromTypeName(typeName) {
  const normalized = String(typeName || '');
  if (/\[\]$/.test(normalized)) {
    return '';
  }
  const simple = normalized.includes('.')
    ? normalized.slice(normalized.lastIndexOf('.') + 1)
    : normalized;
  return TYPE_TO_BUILDER.get(simple) || '';
}

function inferFluentBuilder(before, methodBuilders = new Map()) {
  const lines = String(before || '').split(/\r?\n/);
  const currentIndent = leadingWhitespaceLength(lines[lines.length - 1] || '');
  for (let index = lines.length - 1; index >= 0 && lines.length - 1 - index <= 40; index -= 1) {
    const line = maskStringsAndComments(lines[index]);
    const trimmed = line.trim();
    if (!trimmed) {
      continue;
    }
    const indent = leadingWhitespaceLength(line);
    if (index !== lines.length - 1 && indent > currentIndent) {
      continue;
    }
    if (trimmed.startsWith('.') && indent >= currentIndent) {
      continue;
    }
    const builder = lastBuilderCallName(line, methodBuilders);
    if (builder) {
      return builder;
    }
    if (/;\s*$/.test(trimmed) || /[{}]\s*$/.test(trimmed)) {
      return '';
    }
  }
  return '';
}

function lastBuilderCallName(fragment, methodBuilders = new Map()) {
  let result = '';
  for (const match of String(fragment || '').matchAll(/\b(?:new\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*\(/g)) {
    if (MEMBER_SNIPPETS[match[1]]) {
      result = match[1];
    } else if (methodBuilders.has(match[1])) {
      result = methodBuilders.get(match[1]);
    }
  }
  return result;
}

function leadingWhitespaceLength(line) {
  const match = /^(\s*)/.exec(String(line || ''));
  return match ? match[1].length : 0;
}

function localCompletions(text) {
  const result = [];
  for (const match of String(text || '').matchAll(/^\s*(?:(?:public|private|static|override)\s+)*([A-Za-z_][A-Za-z0-9_.]*(?:\[\])?)\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(/gm)) {
    result.push({
      label: match[2],
      insertText: match[2],
      detail: `локальный метод: ${match[1]} ${match[2]}(...)`,
      kind: 'method',
      sortText: `100_${match[2]}`
    });
  }
  for (const match of String(text || '').matchAll(/^\s*(?:(?:public|private|static)\s+)*(?:var|[A-Za-z_][A-Za-z0-9_.]*(?:\[\])?)\s+([A-Za-z_][A-Za-z0-9_]*)\s*(?:=|;)/gm)) {
    result.push({
      label: match[1],
      insertText: match[1],
      detail: 'локальное поле/переменная',
      kind: 'variable',
      sortText: `101_${match[1]}`
    });
  }
  return result;
}

function resourceNamespaceMemberCompletions(index, namespace) {
  if (!index || typeof index.ids !== 'function') {
    return [];
  }
  const pair = RESOURCE_KINDS.find(([, value]) => value === namespace);
  if (!pair) {
    return [];
  }
  const [kind] = pair;
  return (index.ids(kind) || []).map((id, itemIndex) => {
    const symbol = pascalFromId(id);
    return {
      label: symbol,
      insertText: symbol,
      detail: `индекс ${kind}: ${id}`,
      kind: 'reference',
      sortText: `000_${namespace}_${String(itemIndex).padStart(3, '0')}_${symbol}`
    };
  });
}

function resourceCompletions(index, kinds = null) {
  if (!index || typeof index.ids !== 'function') {
    return [];
  }
  const result = [];
  const resourceKinds = Array.isArray(kinds) && kinds.length > 0
    ? kinds.map(kind => [kind, RESOURCE_NAMESPACE_BY_KIND.get(kind)]).filter(([, namespace]) => namespace !== undefined)
    : RESOURCE_KINDS;
  for (const [kind, namespace] of resourceKinds) {
    for (const id of index.ids(kind) || []) {
      const symbol = pascalFromId(id);
      result.push({
        label: namespace ? `${namespace}.${symbol}` : symbol,
        insertText: namespace ? `${namespace}.${symbol}` : symbol,
        detail: `индекс ${kind}: ${id}`,
        kind: 'reference',
        sortText: `200_${kind}_${symbol}`
      });
      if (namespace) {
        result.push({
          label: symbol,
          insertText: symbol,
          detail: `индекс ${kind}: ${id}`,
          kind: 'reference',
          sortText: `201_${kind}_${symbol}`
        });
      }
    }
  }
  return result;
}

function stateRefCompletions(text) {
  const result = [];
  const pattern = /\bStateRef\s+([A-Za-z_][A-Za-z0-9_]*)\s*=/g;
  for (const match of String(text || '').matchAll(pattern)) {
    result.push({
      label: match[1],
      insertText: match[1],
      detail: 'локальный StateRef',
      kind: 'variable',
      sortText: `090_${match[1]}`
    });
  }
  return result;
}

function stateKeyCompletions(index) {
  if (!index || typeof index.stateKeys !== 'function') {
    return [];
  }
  return (index.stateKeys() || []).map(key => ({
    label: `"${key}"`,
    insertText: `"${key}"`,
    detail: 'индексированный story state key',
    kind: 'value',
    sortText: `300_${key}`
  }));
}

function pascalFromId(id) {
  return String(id || '')
    .split(/[^A-Za-z0-9]+/)
    .filter(Boolean)
    .map(part => part.charAt(0).toUpperCase() + part.slice(1))
    .join('');
}

function dedupeEntries(entries) {
  const seen = new Set();
  const result = [];
  for (const entry of entries) {
    const key = `${entry.label}:${entry.insertText}`;
    if (!seen.has(key)) {
      seen.add(key);
      result.push(entry);
    }
  }
  return result;
}

function maskStringsAndComments(line) {
  return String(line || '')
    .replace(/"([^"\\]|\\.)*"|'([^'\\]|\\.)*'/g, value => ' '.repeat(value.length))
    .replace(/\/\/.*$/g, value => ' '.repeat(value.length))
    .replace(/\/\*.*?\*\//g, value => ' '.repeat(value.length));
}

module.exports = {
  echoScriptCompletionEntries,
  pascalFromId
};
