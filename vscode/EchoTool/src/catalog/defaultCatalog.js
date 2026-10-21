'use strict';

function createStoryCatalog() {
  const triggerTypes = entries({
    manual: {
      description: 'Manual signal trigger. Fires when an effect emits the same key or debug command sends it.',
      required: ['type', 'key'],
      fields: ['type', 'key']
    },
    action_performed: {
      description: 'Fires when an interaction action with the same key is performed.',
      required: ['type', 'key'],
      fields: ['type', 'key']
    },
    area_enter: {
      description: 'Fires when the player crosses from outside into a cuboid area.',
      required: ['type', 'key', 'min', 'max'],
      fields: ['type', 'key', 'dimensionId', 'min', 'max']
    },
    area_enter_delayed: {
      description: 'Fires after the player stays in a cuboid area for delayTicks.',
      aliases: ['delayed_area_enter', 'area_stay'],
      required: ['type', 'key', 'min', 'max'],
      fields: ['type', 'key', 'dimensionId', 'min', 'max', 'delayTicks', 'delay']
    },
    area_exit: {
      description: 'Fires when the player leaves a cuboid area.',
      required: ['type', 'key', 'min', 'max'],
      fields: ['type', 'key', 'dimensionId', 'min', 'max']
    },
    inventory_has_item: {
      description: 'Fires when player inventory contains itemId with count or more.',
      required: ['type', 'key', 'itemId'],
      fields: ['type', 'key', 'itemId', 'count']
    },
    inventory_missing: {
      description: 'Fires when player inventory does not contain itemId with count.',
      required: ['type', 'key', 'itemId'],
      fields: ['type', 'key', 'itemId', 'count']
    },
    mob_killed: {
      description: 'Fires when the player kills the configured entity id.',
      required: ['type', 'key', 'entityId'],
      fields: ['type', 'key', 'entityId']
    },
    timer: {
      description: 'Fires after delayTicks. Can repeat with repeatIntervalTicks.',
      required: ['type', 'key'],
      fields: ['type', 'key', 'delayTicks', 'repeatIntervalTicks', 'scope']
    },
    time_reached: {
      description: 'Fires when dayTime is reached in the dimension.',
      required: ['type', 'key', 'dayTime'],
      fields: ['type', 'key', 'dimensionId', 'dayTime']
    },
    player_join: {
      description: 'Fires when a player joins.',
      required: ['type', 'key'],
      fields: ['type', 'key']
    },
    player_respawn: {
      description: 'Fires when a player respawns.',
      required: ['type', 'key'],
      fields: ['type', 'key']
    },
    dimension_enter: {
      description: 'Fires when player enters dimensionId.',
      required: ['type', 'key'],
      fields: ['type', 'key', 'dimensionId']
    }
  });

  const conditionTypes = entries({
    always_true: {
      description: 'Always passes.',
      required: ['type'],
      fields: ['type']
    },
    state_equals: {
      description: 'Checks that a story state key equals a JSON5 value.',
      required: ['type', 'key', 'value'],
      fields: ['type', 'key', 'value']
    },
    state_exists: {
      description: 'Checks that a story state key exists.',
      required: ['type', 'key'],
      fields: ['type', 'key']
    },
    state_compare: {
      description: 'Compares a state value with ==, !=, >=, <=, >, or <.',
      required: ['type', 'key', 'operator', 'value'],
      fields: ['type', 'key', 'operator', 'value']
    },
    payload_equals: {
      description: 'Checks dialogue/event payload field equality.',
      required: ['type', 'field', 'value'],
      fields: ['type', 'field', 'value']
    },
    quest_completed: {
      description: 'Alias of mission_completed. Checks a completed quest.',
      aliases: ['mission_completed', 'is_completed_mission', 'iscompletedmission'],
      required: ['type'],
      anyRequired: [['questId', 'missionId', 'id']],
      fields: ['type', 'questId', 'missionId', 'id']
    },
    quest_status: {
      description: 'Checks quest/mission status.',
      aliases: ['mission_status'],
      required: ['type', 'status'],
      anyRequired: [['questId', 'missionId', 'id']],
      fields: ['type', 'questId', 'missionId', 'id', 'status']
    },
    puzzle_status: {
      description: 'Checks puzzle status.',
      required: ['type', 'status'],
      anyRequired: [['puzzleId', 'id']],
      fields: ['type', 'puzzleId', 'id', 'status']
    },
    puzzle_stage: {
      description: 'Checks current puzzle stage.',
      required: ['type'],
      anyRequired: [['puzzleId', 'id'], ['stageId', 'stage']],
      fields: ['type', 'puzzleId', 'id', 'stageId', 'stage']
    },
    puzzle_solved: {
      description: 'Checks solved flag for a puzzle.',
      required: ['type'],
      anyRequired: [['puzzleId', 'id']],
      fields: ['type', 'puzzleId', 'id', 'solved']
    },
    puzzle_attempts_at_least: {
      description: 'Checks that puzzle attempts are at least amount.',
      required: ['type'],
      anyRequired: [['puzzleId', 'id'], ['amount', 'min', 'atLeast']],
      fields: ['type', 'puzzleId', 'id', 'amount', 'min', 'atLeast']
    },
    puzzle_attempts_below: {
      description: 'Checks that puzzle attempts are below amount.',
      required: ['type'],
      anyRequired: [['puzzleId', 'id'], ['amount', 'below', 'max']],
      fields: ['type', 'puzzleId', 'id', 'amount', 'below', 'max']
    },
    cipher_answer_matches: {
      description: 'Checks a cipher/plain answer against expected or expectedEncoded.',
      aliases: ['puzzle_answer_matches'],
      required: ['type'],
      anyRequired: [['expected', 'expectedEncoded']],
      fields: ['type', 'answer', 'answerField', 'answerKey', 'expected', 'expectedEncoded', 'cipher', 'answerRules', 'matching', 'match']
    },
    player_money_at_least: {
      description: 'Checks player money >= amount.',
      required: ['type', 'amount'],
      fields: ['type', 'amount']
    },
    player_money_below: {
      description: 'Checks player money < amount.',
      required: ['type', 'amount'],
      fields: ['type', 'amount']
    },
    mafia_relation_at_least: {
      description: 'Checks mafia faction relation >= amount.',
      aliases: ['mafia_at_least'],
      required: ['type'],
      anyRequired: [['mafia', 'factionId', 'faction', 'id', 'name'], ['min', 'atLeast', 'amount', 'value']],
      fields: ['type', 'mafia', 'factionId', 'faction', 'id', 'name', 'min', 'atLeast', 'amount', 'value']
    },
    mafia_relation_below: {
      description: 'Checks mafia faction relation < amount.',
      aliases: ['mafia_below'],
      required: ['type'],
      anyRequired: [['mafia', 'factionId', 'faction', 'id', 'name'], ['below', 'lt', 'max', 'amount', 'value']],
      fields: ['type', 'mafia', 'factionId', 'faction', 'id', 'name', 'below', 'lt', 'max', 'amount', 'value']
    },
    mafia: {
      description: 'Compact mafia relation condition.',
      aliases: ['mafia_relation'],
      required: ['type'],
      fields: ['type', 'mafia', 'factionId', 'faction', 'id', 'name', 'operator', 'value', 'amount']
    },
    player_equipped_item: {
      description: 'Checks equipped item in a player slot.',
      required: ['type', 'slot', 'itemId'],
      fields: ['type', 'slot', 'itemId']
    },
    all_of: {
      description: 'All nested conditions must pass.',
      required: ['type', 'conditions'],
      fields: ['type', 'conditions']
    },
    any_of: {
      description: 'At least one nested condition must pass.',
      required: ['type', 'conditions'],
      fields: ['type', 'conditions']
    },
    not: {
      description: 'Negates a nested condition.',
      required: ['type', 'condition'],
      fields: ['type', 'condition']
    }
  });

  const effectTypes = entries({
    if: {
      description: 'Conditional effect. Aliases: if_else, conditional, branch.',
      aliases: ['if_else', 'if-else', 'conditional', 'branch'],
      required: ['type'],
      anyRequired: [['if', 'condition'], ['then', 'thenEffects', 'thenDo', 'effects', 'do']],
      fields: ['type', 'if', 'condition', 'then', 'thenEffects', 'thenDo', 'effects', 'do', 'else', 'elseEffects', 'otherwise']
    },
    set_state: {
      description: 'Set story state key to a JSON5 value.',
      required: ['type', 'key', 'value'],
      fields: ['type', 'key', 'value']
    },
    remove_state: {
      description: 'Remove story state key.',
      required: ['type', 'key'],
      fields: ['type', 'key']
    },
    black_screen: {
      description: 'Show or hide black screen/cinematic transition.',
      required: ['type'],
      fields: ['type', 'visible', 'fadeInTicks', 'durationTicks', 'fadeOutTicks', 'text', 'textKey', 'state', 'button', 'buttonKey', 'playerPoseOnWake', 'onWakeStartedSignal', 'onCoveredSignal', 'onClearedSignal']
    },
    fog: {
      description: 'Control client fog overlay.',
      required: ['type'],
      fields: ['type', 'visible', 'start', 'end', 'color', 'skyEnd', 'cloudEnd']
    },
    player_pose: {
      description: 'Set player render pose.',
      required: ['type'],
      fields: ['type', 'pose']
    },
    notify: {
      description: 'Show a story notification.',
      required: ['type', 'title', 'text'],
      fields: ['type', 'title', 'titleKey', 'text', 'textKey', 'kind', 'durationTicks', 'showNotification']
    },
    quest_update: {
      description: 'Create/update a quest journal entry.',
      required: ['type', 'questId', 'title', 'stageId', 'stageTitle'],
      fields: ['type', 'questId', 'title', 'titleKey', 'body', 'bodyKey', 'stageId', 'stageTitle', 'stageTitleKey', 'objectives', 'marker', 'status', 'showNotification']
    },
    journal_note: {
      description: 'Add/update a journal note.',
      required: ['type', 'entryId', 'title', 'text'],
      fields: ['type', 'entryId', 'title', 'titleKey', 'text', 'textKey', 'category', 'showNotification']
    },
    puzzle_state: {
      description: 'Update puzzle state. Alias: puzzle_update.',
      aliases: ['puzzle_update'],
      required: ['type'],
      anyRequired: [['puzzleId', 'id']],
      fields: ['type', 'puzzleId', 'id', 'stageId', 'stage', 'branchId', 'branch', 'stepId', 'step', 'status', 'solved', 'solvedSteps', 'stepsSolved', 'variables', 'vars', 'resetAttempts', 'reset_attempts']
    },
    puzzle_attempt: {
      description: 'Check and record a puzzle answer. Aliases: puzzle_answer, check_puzzle_answer.',
      aliases: ['puzzle_answer', 'check_puzzle_answer'],
      required: ['type'],
      anyRequired: [['puzzleId', 'id'], ['expected', 'expectedEncoded']],
      fields: ['type', 'puzzleId', 'id', 'answer', 'value', 'answerField', 'field', 'payloadField', 'answerKey', 'stateKey', 'sourceKey', 'expected', 'plain', 'expectedEncoded', 'encoded', 'cipherText', 'ciphertext', 'cipher', 'answerRules', 'successStageId', 'failureStageId', 'successSignal', 'failureSignal', 'maxAttempts']
    },
    cipher_decode: {
      description: 'Decode text into a target state key. Alias: decode_cipher.',
      aliases: ['decode_cipher'],
      required: ['type', 'targetKey'],
      fields: ['type', 'text', 'encoded', 'cipherText', 'ciphertext', 'sourceKey', 'fromKey', 'targetKey', 'toKey', 'cipher']
    },
    play_sound: {
      description: 'Play a sound once.',
      required: ['type', 'soundId'],
      fields: ['type', 'soundId', 'volume', 'pitch']
    },
    play_weapon_sound: {
      description: 'Play weapon sound with hand/voice tuning.',
      required: ['type', 'soundType'],
      fields: ['type', 'soundType', 'hand', 'volume', 'pitch', 'voicePitchMultiplier', 'distance']
    },
    play_music: {
      description: 'Play looping or one-shot music.',
      required: ['type', 'soundId'],
      fields: ['type', 'soundId', 'volume', 'pitch', 'repeatTicks']
    },
    stop_music: {
      description: 'Stop music.',
      required: ['type'],
      fields: ['type']
    },
    play_ambient: {
      description: 'Play ambient sound.',
      required: ['type', 'soundId'],
      fields: ['type', 'soundId', 'volume', 'pitch', 'repeatTicks']
    },
    stop_ambient: {
      description: 'Stop ambient sound.',
      required: ['type'],
      fields: ['type']
    },
    play_animation: {
      description: 'Play humanoid body animation.',
      required: ['type'],
      anyRequired: [['animationId', 'clipId', 'clip', 'id']],
      fields: ['type', 'target', 'npcId', 'npc', 'animationId', 'clipId', 'clip', 'id', 'layer', 'fadeInTicks', 'fadeIn', 'fadeOutTicks', 'fadeOut', 'durationTicks', 'durationSeconds', 'duration']
    },
    animation_enter: {
      description: 'Enter humanoid animation state.',
      aliases: ['enter_animation', 'play_animation_enter'],
      required: ['type'],
      anyRequired: [['animationId', 'clipId', 'clip', 'id']],
      fields: ['type', 'target', 'npcId', 'npc', 'animationId', 'clipId', 'clip', 'id', 'layer', 'fadeInTicks', 'fadeIn', 'fadeOutTicks', 'fadeOut']
    },
    animation_exit: {
      description: 'Exit humanoid animation state.',
      aliases: ['exit_animation', 'play_animation_exit'],
      required: ['type'],
      fields: ['type', 'target', 'npcId', 'npc', 'animationId', 'clipId', 'clip', 'id', 'layer', 'fadeInTicks', 'fadeIn', 'fadeOutTicks', 'fadeOut']
    },
    play_face_animation: {
      description: 'Play face animation.',
      required: ['type'],
      anyRequired: [['animationId', 'clipId', 'clip', 'id']],
      fields: ['type', 'target', 'npcId', 'npc', 'animationId', 'clipId', 'clip', 'id', 'layer', 'fadeInTicks', 'fadeIn', 'fadeOutTicks', 'fadeOut', 'durationTicks', 'durationSeconds', 'duration']
    },
    stop_animation: {
      description: 'Stop humanoid animation.',
      required: ['type'],
      fields: ['type', 'target', 'npcId', 'npc', 'layer', 'fadeOutTicks', 'fadeOut']
    },
    stop_face_animation: {
      description: 'Stop face animation.',
      required: ['type'],
      fields: ['type', 'target', 'npcId', 'npc', 'layer', 'fadeOutTicks', 'fadeOut']
    },
    first_person_prop: {
      description: 'Animate a first-person prop. Alias: show_first_person_prop.',
      aliases: ['show_first_person_prop'],
      required: ['type', 'itemId', 'from', 'to'],
      fields: ['type', 'itemId', 'hand', 'from', 'to', 'durationTicks', 'easing', 'attachToHand', 'attachTick', 'debug', 'frames']
    },
    camera_cue: {
      description: 'Apply a camera directive.',
      required: ['type', 'directive'],
      fields: ['type', 'directive', 'durationTicks']
    },
    play_camera_path: {
      description: 'Play authored camera path. Alias: camera_path.',
      aliases: ['camera_path'],
      required: ['type'],
      anyRequired: [['pathId', 'cameraPathId', 'id']],
      fields: ['type', 'pathId', 'cameraPathId', 'id', 'startTick', 'durationTicks', 'fadeInTicks', 'fadeOutTicks', 'lockControls', 'lockMouse']
    },
    stop_camera_path: {
      description: 'Stop active camera path.',
      required: ['type'],
      fields: ['type', 'fadeOutTicks', 'fadeOut']
    },
    release_camera: {
      description: 'Release story camera. Aliases: clear_camera, camera_release.',
      aliases: ['clear_camera', 'camera_release'],
      required: ['type'],
      fields: ['type', 'fadeOutTicks', 'fadeOut']
    },
    lock_player_actor: {
      description: 'Lock player as an actor for cutscenes.',
      aliases: ['player_actor_lock'],
      required: ['type'],
      fields: ['type', 'ownerToken', 'owner', 'logTag', 'tag', 'inputPolicy', 'facing', 'anchor', 'allowCamera', 'allowMovement', 'allowInteraction']
    },
    release_player_actor: {
      description: 'Release player actor lock.',
      aliases: ['player_actor_release'],
      required: ['type'],
      fields: ['type', 'ownerToken', 'owner', 'reason']
    },
    play_video: {
      description: 'Play a cinematic video.',
      aliases: ['video', 'cinematic_video'],
      required: ['type', 'source'],
      fields: ['type', 'source', 'title', 'durationTicks', 'duration', 'skippable', 'openExternal', 'external', 'onCompleteSignal', 'onFinishedSignal']
    },
    show_monologue: {
      description: 'Show monologue/thought text.',
      aliases: ['monologue'],
      required: ['type'],
      anyRequired: [['speakerId', 'speaker'], ['text', 'say']],
      fields: ['type', 'speakerId', 'speaker', 'text', 'say', 'textKey', 'key', 'portraitId', 'portrait', 'thought', 'delayTicks', 'delay', 'durationTicks', 'duration']
    },
    magic: {
      description: 'Trigger story magic. Alias: cast_magic.',
      aliases: ['cast_magic'],
      required: ['type'],
      fields: ['type', 'mode', 'giveItem', 'respectCooldown']
    },
    magic_status: {
      description: 'Apply magic/combat status.',
      aliases: ['apply_magic_status', 'story_magic_status'],
      required: ['type'],
      anyRequired: [['effectId', 'spellId']],
      fields: ['type', 'effectId', 'spellId', 'durationTicks', 'duration', 'strength', 'source']
    },
    clear_magic_status: {
      description: 'Clear magic status.',
      aliases: ['remove_magic_status', 'magic_status_clear'],
      required: ['type'],
      fields: ['type', 'effectId']
    },
    give_money: {
      description: 'Give player money.',
      required: ['type', 'amount'],
      fields: ['type', 'amount']
    },
    take_money: {
      description: 'Take player money.',
      required: ['type', 'amount'],
      fields: ['type', 'amount']
    },
    set_mafia_relation: {
      description: 'Set mafia relation value.',
      required: ['type'],
      anyRequired: [['mafia', 'factionId', 'faction', 'id', 'name'], ['value', 'amount']],
      fields: ['type', 'mafia', 'factionId', 'faction', 'id', 'name', 'value', 'amount']
    },
    change_mafia_relation: {
      description: 'Adjust mafia relation value.',
      aliases: ['add_mafia_relation', 'increase_mafia_relation'],
      required: ['type'],
      anyRequired: [['mafia', 'factionId', 'faction', 'id', 'name'], ['amount', 'delta', 'value']],
      fields: ['type', 'mafia', 'factionId', 'faction', 'id', 'name', 'amount', 'delta', 'value']
    },
    mafia: {
      description: 'Compact mafia relation effect. Alias: mafia_relation.',
      aliases: ['mafia_relation'],
      required: ['type'],
      fields: ['type', 'mafia', 'factionId', 'faction', 'id', 'name', 'add', 'delta', 'amount', 'increase', 'set', 'to', 'value']
    },
    sit: {
      description: 'NPC sit command. Aliases: npc_sit, seat.',
      aliases: ['npc_sit', 'seat'],
      required: ['type'],
      fields: ['type', 'npcId', 'npc', 'seatId', 'x', 'y', 'z', 'yaw']
    },
    stand: {
      description: 'NPC stand command. Aliases: npc_stand, unseat.',
      aliases: ['npc_stand', 'unseat'],
      required: ['type'],
      fields: ['type', 'npcId', 'npc']
    },
    use_block: {
      description: 'NPC uses a block. Aliases: useblock, block_use, useblock_action.',
      aliases: ['useblock', 'block_use', 'useblock_action'],
      required: ['type'],
      anyRequired: [['npcId', 'npc'], ['at', 'block', 'pos', 'position', 'target', 'x']],
      fields: ['type', 'npcId', 'npc', 'at', 'block', 'pos', 'position', 'target', 'x', 'y', 'z', 'action', 'mode', 'hand']
    },
    npc_path: {
      description: 'Move NPC along a route. Aliases: move_path, move_route, walk_path, walk_route.',
      aliases: ['move_path', 'move_route', 'walk_path', 'walk_route'],
      required: ['type'],
      anyRequired: [['npcId', 'npc'], ['route', 'points', 'waypoints', 'to']],
      fields: ['type', 'npcId', 'npc', 'route', 'points', 'waypoints', 'to', 'speed', 'completionDistance', 'pauseTicks', 'repathAfterTicks', 'maxRepathAttempts', 'teleportToTargetIfStuck', 'openDoors', 'canFloat', 'usePathfinding', 'targetSearchRadius', 'thenDialogueId', 'thenDialogue', 'payload']
    },
    npc_approach_player: {
      description: 'Move NPC toward player. Aliases: approach_player, walk_to_player, move_to_player.',
      aliases: ['approach_player', 'walk_to_player', 'move_to_player'],
      required: ['type'],
      anyRequired: [['npcId', 'npc']],
      fields: ['type', 'npcId', 'npc', 'stopDistance', 'distance', 'nearDistance', 'completionDistance', 'speed', 'repathTicks', 'timeoutTicks', 'lookAtPlayer', 'warpWhenUnseen', 'warpDistance', 'warpSearchRadius', 'thenDialogueId', 'thenDialogue', 'payload']
    },
    npc_rotate: {
      description: 'Rotate NPC by Vector3 axes or yaw. Aliases: rotate_npc, rotate, turn_npc, turn.',
      aliases: ['rotate_npc', 'rotate', 'turn_npc', 'turn'],
      required: ['type'],
      anyRequired: [['npcId', 'npc'], ['rotation', 'rot', 'axes', 'yaw', 'targetYaw', 'rotationYaw', 'facingYaw', 'y']],
      fields: ['type', 'npcId', 'npc', 'rotation', 'rot', 'axes', 'vector', 'x', 'y', 'z', 'yaw', 'targetYaw', 'rotationYaw', 'facingYaw', 'pitch', 'targetPitch', 'rotationPitch', 'roll', 'targetRoll', 'rotationRoll', 'speed', 'turnSpeed', 'rotationSpeed', 'speedDegreesPerTick', 'degreesPerTick', 'lock', 'locked', 'keepLocked']
    },
    npc_enter_vehicle: {
      description: 'Put NPC into a specific vehicle. Aliases: enter_vehicle, vehicle_mount, npc_vehicle.',
      aliases: ['enter_vehicle', 'vehicle_mount', 'npc_vehicle'],
      required: ['type'],
      anyRequired: [['npcId', 'npc']],
      fields: ['type', 'npcId', 'npc', 'vehicleId', 'seat', 'role']
    },
    npc_enter_nearest_vehicle: {
      description: 'Put NPC into nearest matching vehicle.',
      aliases: ['enter_nearest_vehicle', 'vehicle_mount_nearest', 'npc_vehicle_nearest'],
      required: ['type'],
      anyRequired: [['npcId', 'npc']],
      fields: ['type', 'npcId', 'npc', 'radius', 'seat', 'role']
    },
    npc_enter_vehicle_at: {
      description: 'Put NPC into vehicle at coordinates.',
      aliases: ['enter_vehicle_at', 'vehicle_mount_at', 'npc_vehicle_at'],
      required: ['type'],
      anyRequired: [['npcId', 'npc']],
      fields: ['type', 'npcId', 'npc', 'x', 'y', 'z', 'radius', 'seat', 'role']
    },
    npc_leave_vehicle: {
      description: 'Make NPC leave vehicle.',
      aliases: ['leave_vehicle', 'vehicle_dismount', 'npc_vehicle_leave'],
      required: ['type'],
      anyRequired: [['npcId', 'npc']],
      fields: ['type', 'npcId', 'npc']
    },
    start_scene: {
      description: 'Start a scene by sceneId.',
      required: ['type', 'sceneId'],
      fields: ['type', 'sceneId']
    },
    start_dialogue: {
      description: 'Start a dialogue by dialogueId.',
      required: ['type', 'dialogueId'],
      fields: ['type', 'dialogueId', 'payload']
    },
    emit_signal: {
      description: 'Emit a story signal/trigger.',
      required: ['type', 'signal'],
      fields: ['type', 'signal']
    },
    world_command: {
      description: 'Execute world command object.',
      required: ['type', 'command'],
      fields: ['type', 'command']
    },
    npc_command: {
      description: 'Execute NPC command object.',
      required: ['type', 'command'],
      fields: ['type', 'command']
    }
  });

  const sceneStepTypes = entries({
    wait: {
      description: 'Wait ticks.',
      required: ['type', 'ticks'],
      fields: ['type', 'ticks']
    },
    say_line: {
      description: 'Scene line object.',
      required: ['type'],
      anyRequired: [['text', 'say']],
      fields: ['type', 'speakerId', 'speaker', 'text', 'say', 'textKey', 'key', 'voiceSoundId', 'voice', 'portraitId', 'portrait', 'fxCueId', 'fx', 'nameColor', 'thought', 'anim', 'animation', 'a', 'face', 'faceAnimation', 'effects', 'do']
    },
    wait_chunks: {
      description: 'Wait until chunks are loaded.',
      required: ['type', 'dimensionId', 'centerX', 'centerZ'],
      fields: ['type', 'dimensionId', 'centerX', 'centerZ', 'radiusChunks', 'timeoutTicks']
    },
    apply_effects: {
      description: 'Apply effects from a scene step.',
      required: ['type'],
      anyRequired: [['effects', 'do']],
      fields: ['type', 'effects', 'do']
    }
  });

  const npcCommandTypes = entries({
    spawn: {
      description: 'Spawn an authored NPC at explicit block coordinates.',
      required: ['type', 'npcId', 'dimensionId', 'x', 'y', 'z'],
      fields: ['type', 'npcId', 'dimensionId', 'x', 'y', 'z', 'yaw', 'pitch']
    },
    spawn_near_player: {
      description: 'Spawn an authored NPC near the initiating player.',
      required: ['type', 'npcId'],
      fields: ['type', 'npcId', 'forwardOffset', 'sideOffset', 'yOffset']
    },
    move_route: {
      description: 'Move an NPC along an authored route/path.',
      aliases: ['move_path', 'walk_route', 'walk_path', 'npc_path'],
      required: ['type'],
      anyRequired: [['npcId', 'npc'], ['route', 'points', 'waypoints', 'to', 'path']],
      fields: ['type', 'npcId', 'npc', 'route', 'path', 'points', 'waypoints', 'to', 'speed', 'completionDistance', 'pauseTicks', 'repathAfterTicks', 'maxRepathAttempts', 'thenDialogueId', 'thenDialogue', 'thenDialoguePayload', 'payload']
    },
    approach_player: {
      description: 'Move an NPC toward the initiating or nearest player.',
      aliases: ['npc_approach_player', 'walk_to_player', 'move_to_player'],
      required: ['type'],
      anyRequired: [['npcId', 'npc']],
      fields: ['type', 'npcId', 'npc', 'target', 'player', 'targetPlayer', 'stopDistance', 'distance', 'nearDistance', 'completionDistance', 'speed', 'walkSpeed', 'repathTicks', 'repathAfterTicks', 'timeoutTicks', 'timeout', 'lookAtPlayer', 'look', 'facePlayer', 'face', 'thenDialogueId', 'thenDialogue', 'payload', 'warpWhenUnseen', 'warpDistance', 'warpSearchRadius']
    },
    sit: {
      description: 'Seat an NPC at coordinates or a seat rule.',
      aliases: ['npc_sit', 'seat'],
      required: ['type'],
      anyRequired: [['npcId', 'npc'], ['at', 'block', 'pos', 'position', 'target', 'x']],
      fields: ['type', 'npcId', 'npc', 'at', 'block', 'pos', 'position', 'target', 'x', 'y', 'z', 'seatId', 'seat', 'rule', 'yawMode', 'yaw_mode', 'facing', 'yaw', 'fixedYaw', 'fadeInTicks', 'fadeIn', 'fadeOutTicks', 'fadeOut', 'enterAnimationId', 'enterAnimation', 'enter', 'loopAnimationId', 'loopAnimation', 'loop']
    },
    stand: {
      description: 'Make a seated NPC stand up.',
      aliases: ['npc_stand', 'unseat'],
      required: ['type'],
      anyRequired: [['npcId', 'npc']],
      fields: ['type', 'npcId', 'npc', 'fadeInTicks', 'fadeIn', 'fadeOutTicks', 'fadeOut', 'exitAnimationId', 'exitAnimation', 'exit']
    },
    use_block: {
      description: 'Make an NPC use, open, close, or toggle a block.',
      aliases: ['useblock', 'block_use', 'useblock_action'],
      required: ['type'],
      anyRequired: [['npcId', 'npc'], ['at', 'block', 'pos', 'position', 'target', 'x']],
      fields: ['type', 'npcId', 'npc', 'at', 'block', 'pos', 'position', 'target', 'x', 'y', 'z', 'action']
    },
    set_state: {
      description: 'Set an NPC visual/behavior state, not story memory.',
      required: ['type'],
      anyRequired: [['npcId', 'npc'], ['stateId', 'state']],
      fields: ['type', 'npcId', 'npc', 'stateId', 'state', 'fadeOutTicks', 'fadeOut']
    },
    play_animation: {
      description: 'Play a humanoid animation on an NPC.',
      required: ['type'],
      anyRequired: [['npcId', 'npc'], ['animationId', 'clipId', 'clip', 'id']],
      fields: ['type', 'npcId', 'npc', 'animationId', 'clipId', 'clip', 'id', 'layer', 'fadeInTicks', 'fadeIn', 'fadeOutTicks', 'fadeOut', 'durationTicks', 'durationSeconds', 'duration']
    },
    animation_exit: {
      description: 'Exit an NPC humanoid animation state.',
      aliases: ['exit_animation', 'play_animation_exit'],
      required: ['type'],
      anyRequired: [['npcId', 'npc']],
      fields: ['type', 'npcId', 'npc', 'animationId', 'clipId', 'clip', 'id', 'layer', 'fadeInTicks', 'fadeIn', 'fadeOutTicks', 'fadeOut']
    },
    play_face_animation: {
      description: 'Play a face animation on an NPC.',
      required: ['type'],
      anyRequired: [['npcId', 'npc'], ['animationId', 'clipId', 'clip', 'id']],
      fields: ['type', 'npcId', 'npc', 'animationId', 'clipId', 'clip', 'id', 'layer', 'fadeInTicks', 'fadeIn', 'fadeOutTicks', 'fadeOut', 'durationTicks', 'durationSeconds', 'duration']
    },
    stop_animation: {
      description: 'Stop a humanoid animation on an NPC.',
      required: ['type'],
      anyRequired: [['npcId', 'npc']],
      fields: ['type', 'npcId', 'npc', 'layer', 'fadeOutTicks', 'fadeOut']
    },
    stop_face_animation: {
      description: 'Stop a face animation on an NPC.',
      required: ['type'],
      anyRequired: [['npcId', 'npc']],
      fields: ['type', 'npcId', 'npc', 'layer', 'fadeOutTicks', 'fadeOut']
    },
    set_look_at_players: {
      description: 'Override whether an NPC looks at nearby players.',
      aliases: ['look_at_players', 'look_at_player', 'npc_look_at_players'],
      required: ['type'],
      anyRequired: [['npcId', 'npc']],
      fields: ['type', 'npcId', 'npc', 'enabled']
    },
    reset_look_at_players: {
      description: 'Clear the look-at-players override for an NPC.',
      aliases: ['clear_look_at_players', 'default_look_at_players'],
      required: ['type'],
      anyRequired: [['npcId', 'npc']],
      fields: ['type', 'npcId', 'npc']
    },
    stay: {
      description: 'Set whether an NPC should stay still.',
      aliases: ['set_stay_still'],
      required: ['type', 'npcId'],
      fields: ['type', 'npcId', 'enabled']
    },
    dont_rotate: {
      description: 'Set whether an NPC rotation is locked.',
      aliases: ['set_rotation_locked'],
      required: ['type', 'npcId'],
      fields: ['type', 'npcId', 'enabled']
    },
    rotate: {
      description: 'Rotate an NPC by Vector3 axes or yaw. Vector3 fields are x=pitch, y=yaw, z=roll.',
      aliases: ['npc_rotate', 'rotate_npc', 'turn', 'turn_npc', 'face_yaw', 'set_rotation'],
      required: ['type'],
      anyRequired: [['npcId', 'npc'], ['rotation', 'rot', 'axes', 'yaw', 'targetYaw', 'rotationYaw', 'facingYaw', 'y']],
      fields: ['type', 'npcId', 'npc', 'rotation', 'rot', 'axes', 'vector', 'x', 'y', 'z', 'yaw', 'targetYaw', 'rotationYaw', 'facingYaw', 'pitch', 'targetPitch', 'rotationPitch', 'roll', 'targetRoll', 'rotationRoll', 'speed', 'turnSpeed', 'rotationSpeed', 'speedDegreesPerTick', 'degreesPerTick', 'lock', 'locked', 'keepLocked']
    },
    set_held_item: {
      description: 'Put an item into an NPC hand.',
      aliases: ['hold_item', 'show_item'],
      required: ['type'],
      anyRequired: [['npcId', 'npc'], ['itemId', 'item']],
      fields: ['type', 'npcId', 'npc', 'itemId', 'item', 'count', 'hand', 'slot', 'dropPrevious', 'drop_previous', 'drop']
    },
    clear_held_item: {
      description: 'Clear an item from an NPC hand.',
      aliases: ['clear_item', 'hide_item'],
      required: ['type'],
      anyRequired: [['npcId', 'npc']],
      fields: ['type', 'npcId', 'npc', 'hand', 'slot', 'dropPrevious', 'drop_previous', 'drop']
    },
    pick_up_item: {
      description: 'Make an NPC pick up an item nearby.',
      required: ['type', 'npcId', 'itemId'],
      fields: ['type', 'npcId', 'itemId', 'count', 'searchRadius']
    },
    throw_item_to_player: {
      description: 'Make an NPC throw an item to the player.',
      required: ['type', 'npcId'],
      fields: ['type', 'npcId', 'itemId', 'count', 'target', 'searchRadius', 'throwSpeed', 'pickupDelayTicks']
    },
    enter_vehicle: {
      description: 'Put an NPC into a specific vehicle entity.',
      aliases: ['npc_enter_vehicle', 'vehicle_mount', 'npc_vehicle'],
      required: ['type'],
      anyRequired: [['npcId', 'npc'], ['vehicleEntityId', 'entityId', 'vehicle_entity_id']],
      fields: ['type', 'npcId', 'npc', 'vehicleEntityId', 'entityId', 'vehicle_entity_id', 'role', 'as', 'seatRole', 'seatType', 'vehicleRole', 'seatId', 'slot', 'seat', 'seat_id']
    },
    enter_nearest_vehicle: {
      description: 'Put an NPC into the nearest matching vehicle.',
      aliases: ['npc_enter_nearest_vehicle', 'vehicle_mount_nearest', 'npc_vehicle_nearest'],
      required: ['type'],
      anyRequired: [['npcId', 'npc']],
      fields: ['type', 'npcId', 'npc', 'radius', 'r', 'searchRadius', 'vehicleSearchRadius', 'role', 'as', 'seatRole', 'seatType', 'vehicleRole', 'seatId', 'slot', 'seat', 'seat_id']
    },
    enter_vehicle_at: {
      description: 'Put an NPC into a vehicle near explicit coordinates.',
      aliases: ['npc_enter_vehicle_at', 'vehicle_mount_at', 'npc_vehicle_at'],
      required: ['type'],
      anyRequired: [['npcId', 'npc']],
      fields: ['type', 'npcId', 'npc', 'vehicleAt', 'vehiclePosition', 'vehiclePos', 'vehicle', 'at', 'block', 'pos', 'position', 'target', 'x', 'y', 'z', 'radius', 'r', 'searchRadius', 'vehicleSearchRadius', 'role', 'as', 'seatRole', 'seatType', 'vehicleRole', 'seatId', 'slot', 'seat', 'seat_id', 'dimensionId', 'dimension', 'dim']
    },
    leave_vehicle: {
      description: 'Make an NPC leave its current vehicle.',
      aliases: ['npc_leave_vehicle', 'vehicle_dismount', 'npc_vehicle_leave'],
      required: ['type'],
      anyRequired: [['npcId', 'npc']],
      fields: ['type', 'npcId', 'npc']
    },
    despawn: {
      description: 'Despawn an authored NPC.',
      required: ['type', 'npcId'],
      fields: ['type', 'npcId']
    }
  });

  const worldCommandTypes = entries({
    set_block: {
      description: 'Set a block at coordinates.',
      required: ['type', 'dimensionId', 'x', 'y', 'z', 'blockId'],
      fields: ['type', 'dimensionId', 'x', 'y', 'z', 'blockId']
    },
    teleport_player: {
      description: 'Teleport the initiating player.',
      required: ['type', 'x', 'y', 'z'],
      fields: ['type', 'dimensionId', 'x', 'y', 'z', 'yaw', 'pitch']
    },
    set_world_time: {
      description: 'Set world time in a dimension.',
      required: ['type', 'dimensionId', 'time'],
      fields: ['type', 'dimensionId', 'time']
    },
    set_weather: {
      description: 'Set weather in a dimension.',
      required: ['type', 'dimensionId', 'weatherType', 'durationTicks'],
      fields: ['type', 'dimensionId', 'weatherType', 'durationTicks']
    },
    give_player_item: {
      description: 'Give an item to the player.',
      required: ['type', 'itemId'],
      fields: ['type', 'itemId', 'count', 'preferredSlot', 'autoEquip', 'replacePreferredSlot']
    },
    remove_player_item: {
      description: 'Remove an item from the player inventory.',
      required: ['type', 'itemId'],
      fields: ['type', 'itemId', 'count']
    },
    unequip_player_item: {
      description: 'Unequip a player item.',
      aliases: ['remove_player_equipment'],
      required: ['type', 'slot'],
      fields: ['type', 'slot', 'itemId', 'keepItem', 'preferredSlot', 'autoEquip', 'replacePreferredSlot']
    },
    set_player_appearance_layer: {
      description: 'Set a player appearance layer.',
      aliases: ['set_appearance_layer', 'player_appearance_layer'],
      required: ['type'],
      anyRequired: [['layer', 'slot', 'part'], ['style', 'value', 'variant']],
      fields: ['type', 'layer', 'slot', 'part', 'style', 'value', 'variant', 'keepReplacedItem', 'keepItem', 'keepPreviousItem', 'preferredSlot', 'autoEquip', 'replacePreferredSlot']
    },
    lock_door: {
      description: 'Lock a door block with an optional message.',
      required: ['type', 'dimensionId', 'x', 'y', 'z'],
      fields: ['type', 'dimensionId', 'x', 'y', 'z', 'message']
    },
    unlock_door: {
      description: 'Unlock a door block.',
      required: ['type', 'dimensionId', 'x', 'y', 'z'],
      fields: ['type', 'dimensionId', 'x', 'y', 'z']
    },
    ensure_vehicle_spawn: {
      description: 'Ensure an authored vehicle spawn exists.',
      aliases: ['vehicle_spawn_ensure', 'spawn_vehicle'],
      required: ['type'],
      anyRequired: [['spawnId', 'vehicleSpawnId', 'id']],
      fields: ['type', 'spawnId', 'vehicleSpawnId', 'id']
    },
    vehicle_trip: {
      description: 'Start a road vehicle trip.',
      aliases: ['start_vehicle_trip', 'drive_vehicle_trip', 'door_to_door_trip'],
      required: ['type'],
      anyRequired: [['npcId', 'npc'], ['spawnId', 'vehicleSpawnId', 'vehicle', 'car'], ['fromSegmentId', 'fromSegment', 'fromRoad', 'from'], ['toSegmentId', 'destinationSegmentId', 'toSegment', 'toRoad', 'to', 'destination']],
      fields: ['type', 'tripId', 'id', 'npcId', 'npc', 'spawnId', 'vehicleSpawnId', 'vehicle', 'car', 'fromSegmentId', 'fromSegment', 'fromRoad', 'from', 'toSegmentId', 'destinationSegmentId', 'toSegment', 'toRoad', 'to', 'destination', 'category', 'vehicleCategory', 'speed', 'speedBlocksPerSecond', 'driveSpeed', 'cruise', 'cruiseSpeed', 'backend', 'motionBackend', 'driveBackend', 'control', 'mountRadius', 'vehicleSearchRadius', 'parkingRadius', 'arrivalRadius', 'exitOnArrival', 'exit', 'leaveVehicle', 'walkToBuilding', 'walk', 'enterBuilding', 'seatId', 'slot', 'seat']
    },
    story_vehicle_sequence: {
      description: 'Start a cinematic vehicle sequence.',
      aliases: ['vehicle_sequence', 'transport_sequence', 'start_vehicle_sequence'],
      required: ['type'],
      anyRequired: [['driverNpcId', 'driverNpc', 'driver', 'npcId', 'npc'], ['spawnId', 'vehicleSpawnId', 'vehicle', 'car'], ['fromSegmentId', 'fromSegment', 'fromRoad', 'from'], ['destinationId', 'destination', 'toDestination', 'building']],
      fields: ['type', 'sequenceId', 'id', 'driverNpcId', 'driverNpc', 'driver', 'npcId', 'npc', 'spawnId', 'vehicleSpawnId', 'vehicle', 'car', 'fromSegmentId', 'fromSegment', 'fromRoad', 'from', 'destinationId', 'destination', 'toDestination', 'building', 'passengerActor', 'passenger', 'playerPassenger', 'player', 'passengerSeatId', 'passengerSeat', 'seatId', 'seat', 'category', 'vehicleCategory', 'backend', 'motionBackend', 'driveBackend', 'control', 'speed', 'speedBlocksPerSecond', 'driveSpeed', 'cruise', 'cruiseSpeed', 'exitOnArrival', 'exit', 'leaveVehicle', 'walkToBuilding', 'walk', 'enterBuilding', 'cinematicPathId', 'cameraPathId', 'cameraPath', 'cinematic', 'cinematicDurationTicks', 'cameraDurationTicks', 'durationTicks', 'keepCameraAfterTrip', 'keepCamera', 'holdCamera', 'completionPolicy', 'collisionPolicy', 'cinematicCompletionSignal', 'completionSignal', 'endSignal', 'onStartedSignal', 'startedSignal', 'onCompletedSignal', 'completedSignal', 'onCompleteSignal', 'onFailedSignal', 'failedSignal', 'onFailureSignal']
    }
  });

  const commandTypes = mergeDefinitionMaps(npcCommandTypes, worldCommandTypes);

  const rootKinds = {
    event: {
      description: 'Story event file under story/events.',
      required: ['id', 'trigger'],
      fields: ['id', 'trigger', 'condition', 'if', 'effects', 'do', 'dialogue', 'scene', 'fireOnce', 'enabled']
    },
    dialogue: {
      description: 'Story dialogue file under story/dialogues.',
      required: ['id', 'start', 'nodes'],
      fields: ['id', 'speaker', 'speakerId', 'speakerAliases', 'start', 'startNodeId', 'nodes', 'onStartEffects', 'startEffects', 'onCompleteEffects', 'completeEffects', 'cutscene']
    },
    scene: {
      description: 'Story scene file under story/scenes.',
      required: ['id', 'steps'],
      fields: ['id', 'sequenceId', 'speaker', 'speakerId', 'speakerAliases', 'steps', 'lockPlayer', 'skippable', 'replayable', 'openingCamera', 'onStartEffects', 'startEffects', 'start', 'onCompleteEffects', 'completeEffects', 'complete', 'onAbortEffects', 'abortEffects', 'abort', 'cutscene']
    },
    speaker: {
      description: 'Speaker definition under story/speakers.',
      required: ['id'],
      fields: ['id', 'displayName', 'name', 'displayNameKey', 'portraitId', 'skinId', 'dialogue', 'interactionDialogue', 'dialogues', 'interactions', 'interactionActions', 'npcBehavior']
    },
    npc: {
      description: 'Compact NPC definition under story/npcs.',
      required: ['id'],
      fields: ['id', 'displayName', 'name', 'speaker', 'skin', 'skinId', 'portraitId', 'dialogue', 'dialogues', 'interactions', 'spawn', 'behavior']
    },
    animation: {
      description: 'Humanoid animation clip.',
      required: ['id'],
      fields: ['id', 'durationTicks', 'duration', 'loop', 'blendMode', 'endBehavior', 'bones', 'tracks', 'events', 'items']
    },
    faceAnimation: {
      description: 'Face animation clip.',
      required: ['id'],
      fields: ['id', 'durationTicks', 'duration', 'loop', 'parts', 'tracks', 'events']
    },
    cameraPath: {
      description: 'Camera path file.',
      required: ['id'],
      fields: ['id', 'durationTicks', 'duration', 'keyframes', 'frames', 'collisionPolicy', 'completionPolicy']
    },
    localization: {
      description: 'Localization catalog.',
      required: [],
      fields: ['locale', 'language', 'lang', 'default', 'defaultLocale', 'fallbackLocale', 'fallbackLocales', 'extends', 'prefix', 'keyPrefix', 'namespace', 'entries', 'translations', 'values']
    },
    generic: {
      description: 'Generic EchoStory JSON5 file.',
      required: [],
      fields: ['id', 'type', 'key', 'value', 'effects', 'condition']
    }
  };

  const fieldDocs = {
    id: 'Stable identifier. For story content this usually matches the file name.',
    type: 'Discriminator used by the EchoStory parser. Completion depends on context: trigger, condition, effect, or scene step.',
    key: 'Story signal key, state key, text key, or trigger key depending on context.',
    trigger: 'Event trigger object. Must include type and key.',
    condition: 'Story condition object or supported condition shortcut.',
    effects: 'List of story effects. Effects can be objects, strings such as dialogue:id, or compact tuples.',
    do: 'Alias for effects.',
    startEffects: 'Effects executed before dialogue/scene body starts.',
    completeEffects: 'Effects executed when dialogue/scene completes.',
    speakerAliases: 'Map of short aliases used by scene/dialogue line tuples.',
    dialogueId: 'Reference to a dialogue id under story/dialogues.',
    sceneId: 'Reference to a scene id under story/scenes.',
    speakerId: 'Reference to a speaker id under story/speakers or story/npcs.',
    npcId: 'Reference to an NPC/speaker id controlled by NPC commands.',
    npc: 'Alias for npcId in many NPC commands and effects.',
    animationId: 'Reference to a humanoid or face animation clip id.',
    clipId: 'Alias for animationId.',
    pathId: 'Reference to a camera path id.',
    cameraPathId: 'Reference to a camera path id.',
    targetId: 'Stable target identifier. For navigation targets prefer readable ids such as quest.main_gate.',
    x: 'World X coordinate. For navigation targets this is the destination marker position.',
    y: 'World Y coordinate. For navigation targets this is the destination marker height.',
    z: 'World Z coordinate. For navigation targets this is the destination marker position.',
    label: 'Visible UI label. For navigation targets it falls back to the target id when omitted.',
    dimensionId: 'Minecraft dimension id. Defaults to minecraft:overworld in many parsers.',
    iconId: 'Optional icon id for UI markers such as navigation targets.',
    icon: 'Alias for iconId in typed helpers and authoring templates.',
    profile: 'Navigation or behavior profile. For GPS targets use values such as prefer_roads or quest_safe.',
    navigationProfile: 'Alias for profile in navigation target helpers.',
    itemId: 'Minecraft item id, for example minecraft:tripwire_hook.',
    blockId: 'Minecraft block id, for example minecraft:stone.',
    soundId: 'Minecraft sound id or custom sound event id.',
    command: 'Nested NPC/world command object. Completion for command.type depends on the owner effect.',
    signal: 'Story signal object with type and key.',
    route: 'NPC route points. Accepts point objects/tuples depending on command parser.',
    at: 'Coordinate object or tuple for NPC/block commands.',
    position: 'Coordinate object, tuple, or Vector3-like value depending on parser context.',
    rotation: 'NPC rotation vector. For EchoScript/JSON5 rotate commands, x=pitch, y=yaw, z=roll.',
    rot: 'Alias for rotation.',
    axes: 'Alias for rotation.',
    vector: 'Alias for rotation vector in rotate commands.',
    yaw: 'Yaw rotation in degrees.',
    targetYaw: 'Alias for yaw in rotate commands.',
    rotationYaw: 'Alias for yaw in rotate commands.',
    facingYaw: 'Alias for yaw in rotate commands.',
    pitch: 'Pitch rotation in degrees.',
    targetPitch: 'Alias for pitch in rotate commands.',
    rotationPitch: 'Alias for pitch in rotate commands.',
    roll: 'Roll rotation in degrees. Currently stored in IR; vanilla entity rendering applies pitch/yaw.',
    targetRoll: 'Alias for roll in rotate commands.',
    rotationRoll: 'Alias for roll in rotate commands.',
    speed: 'Movement, rotation, or vehicle speed depending on context. For NPC rotate this is degrees per tick.',
    turnSpeed: 'Alias for rotate speed.',
    rotationSpeed: 'Alias for rotate speed.',
    speedDegreesPerTick: 'Alias for rotate speed.',
    degreesPerTick: 'Alias for rotate speed.',
    lock: 'For rotate commands, keep NPC rotation locked after the turn.',
    locked: 'Alias for lock.',
    keepLocked: 'Alias for lock.',
    action: 'Block action/mode such as open, close, toggle, or use.',
    mode: 'Alias for action in block interaction helpers.',
    stopDistance: 'Distance where an NPC approach/move-to-player command is considered complete.',
    repathTicks: 'Approach-player path recalculation interval in ticks.',
    lookAtPlayer: 'When true, NPC keeps looking at the player during approach.',
    warpWhenUnseen: 'When true, runtime may warp the NPC while unseen as a fallback.',
    warpDistance: 'Distance threshold for unseen warp fallback.',
    warpSearchRadius: 'Search radius for a safe unseen warp position.',
    amount: 'Integer amount. Some conditions/effects require non-negative values.',
    durationTicks: 'Duration in Minecraft ticks. 20 ticks is roughly one second.',
    fadeInTicks: 'Fade-in duration in ticks.',
    fadeOutTicks: 'Fade-out duration in ticks.',
    timeoutTicks: 'Timeout in Minecraft ticks.',
    repeatTicks: 'Repeat interval in Minecraft ticks.',
    fireOnce: 'When true, the event should only fire once per relevant scope.',
    status: 'Quest or puzzle status. Common values: ACTIVE, COMPLETED, FAILED.',
    scope: 'Trigger timer scope: PLAYER or GLOBAL.',
    weatherType: 'World weather command value.',
    role: 'Vehicle role for NPC vehicle commands.',
    enabled: 'Boolean toggle. Use true/false, not a quoted string.',
    visible: 'Boolean visibility toggle. Use true/false, not a quoted string.'
  };

  const enums = {
    notifyKind: ['info', 'warning', 'error', 'success'],
    questStatus: ['ACTIVE', 'COMPLETED', 'FAILED'],
    puzzleStatus: ['ACTIVE', 'COMPLETED', 'FAILED', 'LOCKED'],
    timerScope: ['PLAYER', 'GLOBAL'],
    conditionOperator: ['==', '!=', '>=', '<=', '>', '<'],
    animationTarget: ['PLAYER', 'NPC', 'player', 'npc'],
    animationLayer: ['BASE', 'OVERLAY', 'base', 'overlay'],
    faceAnimationLayer: ['BASE', 'OVERLAY', 'DETAIL', 'base', 'overlay', 'detail', 'EYES', 'BROWS', 'MOUTH', 'eyes', 'brows', 'mouth'],
    hand: ['mainhand', 'offhand', 'right', 'left'],
    equipmentSlot: ['mainhand', 'offhand', 'head', 'chest', 'legs', 'feet'],
    dimensions: ['minecraft:overworld', 'minecraft:the_nether', 'minecraft:the_end'],
    weatherType: ['CLEAR', 'RAIN', 'THUNDER', 'clear', 'rain', 'thunder'],
    vehicleRole: ['ANY', 'DRIVER', 'PASSENGER', 'any', 'driver', 'passenger'],
    vehicleCategory: ['car', 'bus', 'emergency', 'service'],
    vehicleBackend: ['native_preferred', 'native_required', 'kinematic_only'],
    cinematicCompletionPolicy: [
      'end_when_trip_complete',
      'end_when_actors_enter_building',
      'end_on_signal',
      'end_after_timeout',
      'explicit_timeline_end',
      'manual_only'
    ],
    cameraCollisionPolicy: ['respect_collision', 'ignore_collision', 'soft_avoidance', 'cut_or_fade_on_occlusion'],
    blockAction: ['open', 'close', 'toggle', 'use'],
    seatYawMode: ['block_facing', 'opposite_facing', 'npc_current', 'fixed'],
    npcPlayerTarget: ['initiator', 'nearest'],
    boolean: ['true', 'false']
  };

  return {
    rootKinds,
    triggerTypes,
    conditionTypes,
    effectTypes,
    sceneStepTypes,
    npcCommandTypes,
    worldCommandTypes,
    commandTypes,
    fieldDocs,
    enums,
    allTriggerTypeIds: ids(triggerTypes),
    allConditionTypeIds: ids(conditionTypes),
    allEffectTypeIds: ids(effectTypes),
    allSceneStepTypeIds: ids(sceneStepTypes),
    allNpcCommandTypeIds: ids(npcCommandTypes),
    allWorldCommandTypeIds: ids(worldCommandTypes),
    allCommandTypeIds: ids(commandTypes)
  };
}

function entries(definitions) {
  const result = new Map();
  for (const [id, definition] of Object.entries(definitions)) {
    const normalized = normalizeDefinition(id, definition);
    result.set(id, normalized);
    for (const alias of normalized.aliases) {
      result.set(alias, { ...normalized, canonical: id, alias: true });
    }
  }
  return result;
}

function normalizeDefinition(id, definition) {
  return {
    canonical: id,
    aliases: definition.aliases || [],
    description: definition.description || '',
    required: definition.required || [],
    anyRequired: definition.anyRequired || [],
    fields: unique(['type', ...(definition.fields || [])])
  };
}

function ids(map) {
  return unique(Array.from(map.keys()));
}

function mergeDefinitionMaps(...maps) {
  const result = new Map();
  for (const map of maps) {
    for (const [id, definition] of map.entries()) {
      if (!result.has(id)) {
        result.set(id, definition);
      }
    }
  }
  return result;
}

function unique(values) {
  return Array.from(new Set(values.filter(Boolean)));
}

module.exports = {
  createStoryCatalog
};
