'use strict';

const {
  STATIC_NAMESPACE_MEMBERS
} = require('./echoscriptStaticSymbols');

const METHOD_SIGNATURES = new Map([
  ['Action', 'Action(id: string, type: string, ...fields, next?: string) -> StoryNodeDef\nAction() -> ActionDef'],
  ['ActionKey', 'Action().ActionKey(key: string) -> ActionDef\nСтабильный signal key, который потом ловится через ActionPerformed(...).'],
  ['Actions', 'Speaker().Actions(actions: ActionDef...) -> SpeakerDef'],
  ['Alias', 'Alias(alias: string, speaker: SpeakerRef | NpcRef | string) inside Dialogue Init()'],
  ['Add', 'State.Add(state: StateRef, amount: int) -> Effect'],
  ['Animation', 'Animation(id: AnimationId | string, channels?: ChannelDef[], sources?: SourceDef[], entries?: EntryDef[], main?: MainDef, exits?: ExitDef[], target?: string, recovery?: RecoveryDef, replay?: string) -> AnimationDef\nAnimation() -> AnimationDef'],
  ['AnimationClip', 'AnimationClip(id: AnimationClipId | string, rig: AnimationRigId | FaceRigId | string, tracks?: TrackDef[], schemaVersion?: int, loopSeam?: string, authoredSpeedBlocksPerSecond?: number) -> AnimationClipDef'],
  ['AnimationProfile', 'AnimationProfile(id: AnimationProfileId | string, rig: AnimationRigId | string, graph?: AnimationRigId | FaceRigId | string, render?: string, initialStates?: InitialStateDef[], requiredChannels?: ChannelDef[], metadata?: MetaDef[]) -> AnimationProfileDef'],
  ['AnimationRig', 'AnimationRig(id: AnimationRigId | string, kind?: string, skin?: string, bones?: BoneDef[], sockets?: SocketDef[]) -> AnimationRigDef'],
  ['AreaEnter', 'AreaEnter(enteredFlag: BoolStateRef, dimension: DimensionRef, min: Vector3, max: Vector3) -> TriggerDef'],
  ['AreaEnterDelayed', 'AreaEnterDelayed(enteredFlag: BoolStateRef, dimension: DimensionRef, min: Vector3, max: Vector3, delayTicks: int) -> TriggerDef'],
  ['Behavior', 'Behavior() -> BehaviorDef'],
  ['BlackScreen', 'BlackScreen.Show(state?: string, visible?: bool, fadeInTicks?: int, durationTicks?: int, fadeOutTicks?: int, text?: string, textKey?: localizationKey, button?: string, buttonKey?: localizationKey) -> Effect\nBlackScreen.Hide() -> Effect'],
  ['BlendMode', 'Animation().BlendMode(mode: string) -> AnimationDef'],
  ['BlockPosition', 'new BlockPosition(x: int | string, y: int | string, z: int | string) -> BlockPosition'],
  ['Bone', 'Bone(id: string, frames: Frame...) -> BoneTimelineDef'],
  ['Bones', 'Animation().Bones(bones: BoneTimelineDef...) -> AnimationDef'],
  ['BuildEvent', 'public override EventDef BuildEvent()'],
  ['BuildDialogue', 'public override DialogueDef BuildDialogue()'],
  ['BuildAnimation', 'public override AnimationDef BuildAnimation()'],
  ['BuildQuest', 'public override <ResourceDef> BuildQuest()'],
  ['BuildCharacter', 'public override <ResourceDef> BuildCharacter()'],
  ['Camera', 'Camera().Preset(presetId: string) -> CameraDef\nCamera().PresetId(id: string).Position(Vec(...)).Focus(Vec(...)).FadeInTicks(...).FadeOutTicks(...).DurationTicks(...) -> CameraDef'],
  ['Choice', 'Choice(id: string, text: string, next?: NodeDef | string, key?: string, if?: condition) -> ChoiceDef\nВнутри Choices { ... } лучше использовать if (condition) { Choice(...); NextNode(...); }, чтобы условия оставались typed.'],
  ['Choices', 'Choices { Choice(...); NextNode(...); }'],
  ['ClearHeldItem', 'This.ClearHeldItem(hand?: HandRef, fadeOutTicks?: int) -> Effect\nNpc.ClearHeldItem(npc: NpcRef, hand?: HandRef) -> Effect'],
  ['Complete', 'Scene().Complete(effects: Effect...) -> SceneDef\nSleep.Complete(player?: string, scenario?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect'],
  ['CompleteEffects', 'private Effect[] CompleteEffects() { EffectStatements... }\nАвтоматически прикрепляется к Dialogue() или Scene().'],
  ['Cost', 'Action().Cost(cost: CostDef) -> ActionDef'],
  ['CooldownTicks', 'Event().CooldownTicks(ticks: int) -> EventDef'],
  ['Cutscene', 'Dialogue().Cutscene(cutscene: CutsceneDef) -> DialogueDef\nCutscene.Start(cutscene: CutsceneRef | string) -> DialogueCommand\nCutscene.Play(cutscene: CutsceneRef | string) -> DialogueCommand\nCutscene.Wait(cutscene: CutsceneRef | string) -> DialogueCommand\nCutscene.WaitPrepared(cutscene: CutsceneRef | string) -> DialogueCommand\nCutscene.Stop(cutscene: CutsceneRef | string) -> DialogueCommand'],
  ['DimensionEnter', 'DimensionEnter(key: string, dimension?: DimensionRef | string) -> TriggerDef'],
  ['Debug', 'Debug.Mark(label: string) inside TriggerResource callbacks'],
  ['Dialogue', 'Dialogue(id: DialogueId | string) -> DialogueResourceDef\nDialogue() -> DialogueDef\nDialogue.Start(dialogue: DialogueRef | string) -> Effect'],
  ['DialogueStart', 'DialogueStart(dialogue: DialogueId | string, sessionId?: string, audienceId?: string) inside TriggerResource callbacks'],
  ['DisplayName', 'Speaker().DisplayName(text: string) -> SpeakerDef'],
  ['DisplayNameKey', 'Speaker().DisplayNameKey(key: localizationKey) -> SpeakerDef'],
  ['Do', 'Do(effects: Effect...) -> SceneStep\nИспользуй внутри Scene().Steps(...) для apply_effects шагов.'],
  ['DurationTicks', 'Camera().DurationTicks(ticks: int) -> CameraDef'],
  ['Effect', 'Effect(type: string, ...fields) -> Effect'],
  ['Effects', 'storyEvent.Effects(effects: Effect...) -> EventDef\nAction().Effects(effects: Effect...) -> ActionDef\nLine(...).Effects(effects: Effect...) -> LineDef\nChoice(...).Effects(effects: Effect...) -> ChoiceDef\nИспользуй внутри C#-style if blocks, чтобы условные runtime effects генерировались читаемо.'],
  ['Event', 'new Event() -> EventDef'],
  ['Face', 'Line(...).Face(face: FaceAnimationRef | string) -> LineDef'],
  ['FaceAnimation', 'FaceAnimation() -> FaceAnimationDef'],
  ['FacePose', 'FacePose(id: FacePoseId | string, rig: FaceRigId | string, bones?: BoneDeltaDef[], controls?: ControlDeltaDef[]) -> FacePoseDef\nFacePose(partTransforms...) -> FacePoseDef'],
  ['FacialAnimation', 'FacialAnimation(id: FaceAnimationId | string, rig?: FaceRigId | string, surfaces?: FacialAnimationSurfaceDef[], tracks?: TrackDef[], durationTicks?: int, durationMillis?: int, end?: string) -> FacialAnimationDef'],
  ['FacialAnimationSurface3d', 'class Name : FacialAnimationSurface3d { private string Rig() { return "faceRigId"; } private FacialAnimationPhaseDef Enter() { return Enter(...); } private FacialAnimationPhaseDef Stay() { return Stay(...); } private FacialAnimationPhaseDef Exit() { return Exit(...); } }'],
  ['FacialAnimationSurfacePortrait', 'class Name : FacialAnimationSurfacePortrait { private string Rig() { return "faceRigId"; } private FacialAnimationPhaseDef Enter() { return Enter(...); } private FacialAnimationPhaseDef Stay() { return Stay(...); } private FacialAnimationPhaseDef Exit() { return Exit(...); } }'],
  ['Field', 'Field(key: string, value: any) -> object field'],
  ['FireOnce', 'Event().FireOnce(enabled?: bool = true) -> EventDef\nAction().FireOnce(enabled?: bool = true) -> ActionDef'],
  ['Fog', 'Fog.Show(id: string, durationTicks?: int) -> Effect\nFog.Hide(id?: string) -> Effect'],
  ['Frame', 'Frame(tick: int | string, pose?: PoseDef, interpolation?: string) -> KeyframeDef\nFrame(tick).Position(x, y, z).Rotation(z).Scale(x, y, z) -> KeyframeDef'],
  ['Frames', 'Bone(...).Frames(frames: Frame...) -> BoneTimelineDef'],
  ['GiveItem', 'Player.GiveItem(item: ItemRef, count?: int = 1, autoEquip?: bool) -> Effect'],
  ['Goto', 'Node(...).Goto(node: NodeDef | string) -> NodeDef\nChoice(...).Goto(node: NodeDef | string) -> ChoiceDef'],
  ['Gun', 'Gun(fields?: Field...) -> animation item attachment marker'],
  ['Hand', 'Hand.Main -> mainhand'],
  ['HoldItem', 'This.HoldItem(item: ItemRef, count?: int = 1, hand?: HandRef) -> Effect\nNpc.HoldItem(npc: NpcRef, item: ItemRef, count?: int = 1, hand?: HandRef) -> Effect'],
  ['Id', 'Resource().Id(id: string) -> ResourceDef'],
  ['Init', 'private void Init() { Start(...); Alias(...); Speaker(...); Cutscene(...); }\nАвтоматически прикрепляется к Dialogue().'],
  ['Idle', 'Behavior().Idle(state: StateRef | string, animation?: AnimationRef) -> BehaviorDef'],
  ['InteractionDialogues', 'Speaker().InteractionDialogues(dialogues: DialogueRef...) -> SpeakerDef'],
  ['Interpolation', 'Animation().Interpolation(mode: string) -> AnimationDef'],
  ['Increment', 'State.Increment(state: StateRef, amount?: int = 1) -> Effect'],
  ['InventoryHasItem', 'InventoryHasItem(key: string, item: ItemRef | string, count?: int) -> TriggerDef'],
  ['InventoryMissing', 'InventoryMissing(key: string, item: ItemRef | string, count?: int) -> TriggerDef'],
  ['Key', 'Line(...).Key(key: localizationKey) -> LineDef\nChoice(...).Key(key: localizationKey) -> ChoiceDef'],
  ['Kind', 'Node(...).Kind(kind: string) -> NodeDef'],
  ['Label', 'Action().Label(text: string) -> ActionDef'],
  ['LabelKey', 'Action().LabelKey(key: localizationKey) -> ActionDef'],
  ['LengthTicks', 'Animation().LengthTicks(ticks: int) -> AnimationDef'],
  ['Line', 'Line(speaker: alias | SpeakerRef, text: string, key?: localizationKey, thought?: bool, delay?: int, face?: FaceAnimationRef, anim?: AnimationRef, path?: NpcPathDef) -> LineDef\nLine(...).SetAnimationFace(face: FaceAnimationId | string) -> LineDef\nLine(...).SetAnimationFaceClear() / StopFaceAnimation() -> LineDef'],
  ['LineAfterMove', 'LineAfterMove(speaker: alias | SpeakerRef, text: string, key?: localizationKey, npc?: NpcRef) -> LineDef\nIn dialogue nodes, waits for the speaker or npc movement started by Npc.Move(...) before showing this line. Normal Line(...) can be shown while the NPC is still walking.'],
  ['Lines', 'Node(...).Lines(lines: LineDef...) -> NodeDef'],
  ['LockPlayer', 'Scene().LockPlayer(enabled?: bool = true) -> SceneDef'],
  ['LookAtPlayers', 'Behavior().LookAtPlayers(enabled?: bool = true) -> BehaviorDef'],
  ['Loop', 'Animation().Loop(enabled?: bool = true) -> AnimationDef'],
  ['Money', 'Money(amount: int) -> CostDef'],
  ['MobKilled', 'MobKilled(key: string, entityId: string) -> TriggerDef'],
  ['Memory', 'State.Memory(path: string) -> StateRef\nДля чтения используется path, для записи используется memory.<path>.'],
  ['Monologue', 'Monologue.Show(speaker: SpeakerRef | string, text: string, thought?: bool, delay?: int, durationTicks?: int) -> Effect'],
  ['Move', 'Npc.Move(npc: NpcRef, x: number, y: number, z: number, dimension?: string, output?: string, next?: string) -> Effect\nNpc.Move(npc: NpcRef, targetPosition: Vector3, speed?: float, completionDistance?: float, pauseTicks?: int, repathAfterTicks?: int, maxRepathAttempts?: int, teleportToTargetIfStuck?: bool, openDoors?: bool, canFloat?: bool, targetSearchRadius?: int, thenDialogue?: DialogueRef) -> Effect\nPlayer.Move(x: number, y: number, z: number, dimension?: string) -> Effect\nAlias для Npc.MoveTo.'],
  ['MoveTo', 'Npc.MoveTo(npc: NpcRef, targetPosition: Vector3, speed?: float, completionDistance?: float, pauseTicks?: int, repathAfterTicks?: int, maxRepathAttempts?: int, teleportToTargetIfStuck?: bool, openDoors?: bool, canFloat?: bool, targetSearchRadius?: int, thenDialogue?: DialogueRef) -> Effect\nДвигает NPC к одной точке. Обязательны npc и targetPosition.'],
  ['MoveToPlayer', 'Npc.MoveToPlayer(npc: NpcRef, stopDistance?: number, speed?: number, repathTicks?: int, timeoutTicks?: int, lookAtPlayer?: bool, warpWhenUnseen?: bool, warpDistance?: number, warpSearchRadius?: int, thenDialogue?: DialogueRef) -> Effect\nПодводит NPC к игроку. Обязателен только npc.'],
  ['WalkTo', 'Npc.WalkTo(npc: NpcRef, targetPosition: Vector3, speed?: float, completionDistance?: float, pauseTicks?: int, repathAfterTicks?: int, maxRepathAttempts?: int, teleportToTargetIfStuck?: bool, openDoors?: bool, canFloat?: bool, targetSearchRadius?: int, thenDialogue?: DialogueRef) -> Effect\nAlias для Npc.MoveTo.'],
  ['WalkToPlayer', 'Npc.WalkToPlayer(npc: NpcRef, stopDistance?: number, speed?: number, repathTicks?: int, timeoutTicks?: int, lookAtPlayer?: bool, warpWhenUnseen?: bool, warpDistance?: number, warpSearchRadius?: int, thenDialogue?: DialogueRef) -> Effect\nAlias для Npc.MoveToPlayer.'],
  ['NameColor', 'Speaker().NameColor(color: string | int) -> SpeakerDef\nLine(...).NameColor(color: string | int) -> LineDef'],
  ['Next', 'Node(...).Next(node: NodeDef | string) -> NodeDef\nChoice(...).Next(node: NodeDef | string) -> ChoiceDef'],
  ['NextNode', 'NextNode(node: NodeDef | string) -> void'],
  ['Node', 'Node Name() { ... } inside high-level DialogueResource\nnew Node(id?: string) -> NodeDef\nNode(id: string) -> NodeDef'],
  ['Nodes', 'Legacy only: private NodeDef[] Nodes() { return dialogueNodes; }\nHigh-level DialogueResource discovers Node declarations automatically.'],
  ['Npc', 'Npc(id: NpcDefinitionId | string, display: string, skin?: SkinDef, nameColor?: NpcNameColor, defaultForm?: NpcDefaultFormRef) -> NpcResourceDef\nNpc.Spawn(npc: NpcRef, dimension: DimensionRef, x: int, y: int, z: int, yaw?: number, pitch?: number) -> Effect\nNpc.SpawnNearPlayer(npc: NpcRef, forwardOffset?: number, sideOffset?: number, yOffset?: number) -> Effect\nNpc.Move(npc: NpcRef, x: number, y: number, z: number, dimension?: string, output?: string, next?: string) -> Effect\nNpc.MoveTo(npc: NpcRef, targetPosition: Vector3, speed?: float, completionDistance?: float, pauseTicks?: int, repathAfterTicks?: int, maxRepathAttempts?: int, teleportToTargetIfStuck?: bool, openDoors?: bool, canFloat?: bool, targetSearchRadius?: int, thenDialogue?: DialogueRef) -> Effect\nNpc.Move(npc: NpcRef, targetPosition: Vector3, ...) -> Effect alias\nNpc.Path(npc: NpcRef, points: Vector3[], speed?: number, pauseTicks?: int, completionDistance?: number, teleportToTargetIfStuck?: bool, openDoors?: bool, canFloat?: bool, repathAfterTicks?: int, maxRepathAttempts?: int, thenDialogue?: DialogueRef) -> Effect\nNpc.MoveToPlayer(npc: NpcRef, stopDistance?: number, speed?: number, repathTicks?: int, timeoutTicks?: int, lookAtPlayer?: bool, warpWhenUnseen?: bool, warpDistance?: number, warpSearchRadius?: int, thenDialogue?: DialogueRef) -> Effect\nNpc.ApproachPlayer(npc: NpcRef, ...) -> Effect alias\nNpc.Rotate(npc: NpcRef, rotation: Vector3 | yaw, speed?: number, lock?: bool, pitch?: number, roll?: number) -> Effect\nNpc.UseBlock(npc: NpcRef, position: Vector3, action?: string) -> Effect\nNpc.OpenDoor(npc: NpcRef, position: Vector3) -> Effect\nNpc.Despawn(npc: NpcRef) -> Effect\nNpc.HoldItem(npc: NpcRef, item: ItemRef, count?: int = 1, hand?: HandRef) -> Effect\nNpc.ClearHeldItem(npc: NpcRef, hand?: HandRef) -> Effect\nNpc.PlayAnimation(npc: NpcRef, animation: AnimationRef, layer?: LayerRef, fadeInTicks?: int, fadeOutTicks?: int, durationTicks?: int) -> Effect\nNpc.StopAnimation(npc: NpcRef, layer?: LayerRef, fadeOutTicks?: int) -> Effect\nNpc.PlayFaceAnimation(npc: NpcRef, animation: FaceAnimationRef, layer?: FaceLayerRef, fadeInTicks?: int, fadeOutTicks?: int, durationTicks?: int) -> Effect\nNpc.StopFaceAnimation(npc: NpcRef, layer?: FaceLayerRef, fadeOutTicks?: int) -> Effect\nNpc.StandUp(npc: NpcRef, output?: string, next?: string) -> Effect\nNpc.EnterVehicle(npc: NpcRef, vehicleEntityId: string, role?: string, seat?: string) -> Effect\nNpc.EnterVehicleAt(npc: NpcRef, position: Vector3, role?: string, seat?: string, radius?: number, dimension?: DimensionRef) -> Effect\nNpc.EnterNearestVehicle(npc: NpcRef, radius?: number, role?: string, seat?: string) -> Effect\nNpc.LeaveVehicle(npc: NpcRef) -> Effect'],
  ['NpcAnimationBinding', 'NpcAnimationBinding(id: NpcAnimationBindingId | string, npc?: NpcDefinitionId | string, actor?: ActorId | string, profile: AnimationProfileId | string) -> NpcAnimationBindingDef'],
  ['NpcPath', 'NpcPath(npc: NpcRef | string, points: Vector3[], speed?: float, pauseTicks?: int, completionDistance?: float, openDoors?: bool) -> NpcPathDef'],
  ['Obj', 'Obj(fields: Field...) -> object'],
  ['Player', 'Player.GiveItem(item: ItemRef, count?: int = 1, autoEquip?: bool) -> Effect\nPlayer.Pose(pose: string) -> Effect'],
  ['PlayAnimation', 'This.PlayAnimation(animation: AnimationRef, layer?: LayerRef, fadeInTicks?: int, fadeOutTicks?: int, durationTicks?: int) -> Effect\nNpc.PlayAnimation(npc: NpcRef, animation: AnimationRef, layer?: LayerRef, fadeInTicks?: int, fadeOutTicks?: int, durationTicks?: int) -> Effect'],
  ['Point', 'Point(x: number, y: number, z: number) -> Vector3 (deprecated; use new Vector3)'],
  ['Portrait', 'Portrait(skin: SkinId | string) -> VisualDef\nSpeaker().Portrait(portrait: PortraitRef | string) -> SpeakerDef'],
  ['Portraits', 'Portraits.Buyer, Portraits.Sasha, Portraits.Marlo, and other built-in portrait refs'],
  ['Pose', 'Pose(x?: number, y?: number, z?: number, pitch?: number, yaw?: number, roll?: number) -> PoseDef'],
  ['PresetId', 'Camera().PresetId(id: string) -> CameraDef'],
  ['Priority', 'Action().Priority(value: int) -> ActionDef'],
  ['Quest', 'Quest.Update(quest: QuestRef, status?: QuestStatus, title?: string, body?: string, stage?: QuestStageRef, showNotification?: bool) -> Effect'],
  ['RecoveryTicks', 'Behavior().RecoveryTicks(ticks: int) -> BehaviorDef'],
  ['Require', 'Require(id: string, variable: string, value: bool | number | string, next: string) -> StoryNodeDef'],
  ['Replayable', 'Scene().Replayable(enabled?: bool = true) -> SceneDef'],
  ['Route', 'Schedule().Route(points: Vector3...) -> ScheduleDef'],
  ['Rotation', 'Frame(tick).Rotation(z: number) -> KeyframeDef\nFrame(tick).Rotation(x: 0, y: 0, z: number) -> KeyframeDef'],
  ['Scale', 'Frame(tick).Scale(x: number, y: number, z: number) -> KeyframeDef'],
  ['Say', 'Say(speaker: SpeakerRef | alias, text: string, key?: localizationKey) -> SequenceStep'],
  ['Scene', 'Scene() -> SceneDef\nScene.Start(scene: SceneRef | string) -> Effect'],
  ['Schedule', 'Schedule(startTime: int, endTime: int) -> ScheduleDef'],
  ['Schedules', 'Behavior().Schedules(schedules: ScheduleDef...) -> BehaviorDef'],
  ['Ref', 'State.Ref(namespacedKey: string) -> StateRef\nИспользуй для явных namespaces: debug.*, plot.*, world.*, scene.* или memory.*.'],
  ['Recovery', 'Recovery(mode: string, checkpointIntervalTicks?: int) -> RecoveryDef'],
  ['Set', 'Set(id: string, variable: string, value: bool | number | string, next?: string) -> StoryNodeDef\nSet(state: StateRef | string, value: bool | number | string) -> Effect\nState.Set(state: StateRef | string, value: any) -> Effect\nState.Add(state: StateRef, amount: int) -> Effect\nState.Increment(state: StateRef) -> Effect'],
  ['SetAnimation', 'Line(...).SetAnimation(animation: AnimationId | string) -> LineDef'],
  ['SetAnimationFace', 'Line(...).SetAnimationFace(face: FaceAnimationId | string) -> LineDef'],
  ['SetAnimationFaceClear', 'Line(...).SetAnimationFaceClear() -> LineDef'],
  ['SetFaceAnimation', 'Line(...).SetFaceAnimation(face: FaceAnimationId | string) -> LineDef\nSetFaceAnimation(subject: NpcRef | string, animation: FaceAnimationRef | string) -> DialogueCommand'],
  ['Skippable', 'Scene().Skippable(enabled?: bool = true) -> SceneDef'],
  ['Speaker', 'Speaker() -> SpeakerDef'],
  ['SpeakerAliases', 'Scene().SpeakerAliases(Alias...) -> SceneDef'],
  ['Spawn', 'Npc.Spawn(npc: NpcRef, dimension: DimensionRef, x: int, y: int, z: int, yaw?: number, pitch?: number) -> Effect'],
  ['Skin', 'Skin(id: SkinId | string) -> SkinVisualDef'],
  ['SkinHead', 'SkinHead(id: SkinId | string) -> SkinVisualDef'],
  ['Source', 'Source(id: string) -> AnimationSourceDef'],
  ['Start', 'start Node Name() marks the entry of a multi-node high-level DialogueResource\nStart(id: string, next?: string) -> StoryNodeDef\nStart(node: NodeDef | string) inside legacy Dialogue Init()\nStart(method: MethodRef) inside TriggerResource.Init()\nDialogue.Start(dialogue: DialogueRef | string) -> Effect\nScene.Start(scene: SceneRef | string) -> Effect\nCutscene.Start(cutscene: CutsceneRef | string) -> DialogueCommand'],
  ['StartEffects', 'private Effect[] StartEffects() { EffectStatements... }\nАвтоматически прикрепляется к Dialogue() или Scene().'],
  ['State', 'State.Memory(path: string) -> StateRef\nState.Ref(namespacedKey: string) -> StateRef\nState.Set(state: StateRef, value: any) -> Effect\nState.Add(state: StateRef, amount: int) -> Effect\nState.Increment(state: StateRef) -> Effect\nState.Decrement(state: StateRef) -> Effect\nSchedule().State(state: StateRef | string) -> ScheduleDef'],
  ['States', 'States.Idle -> idle\nStates.Patrol -> patrol'],
  ['StateRef', 'StateRef is a runtime story-state key reference. Declare private/local refs in camelCase, e.g. `private StateRef roomDone = State.Memory("chapter_1.room_done");`.'],
  ['Story', 'Story(id: StoryId | string, start: string) -> StoryResourceDef\nStory.Load(storyId: StoryId | string, executionId?: string, next?: string) -> Effect'],
  ['Step', 'Step(type: string, ...fields) -> SequenceStep'],
  ['Steps', 'Scene().Steps(steps: SequenceStep...) -> SceneDef'],
  ['StopAnimation', 'This.StopAnimation(layer?: LayerRef, fadeOutTicks?: int) -> Effect\nNpc.StopAnimation(npc: NpcRef, layer?: LayerRef, fadeOutTicks?: int) -> Effect'],
  ['SuccessLabel', 'Action().SuccessLabel(text: string) -> ActionDef'],
  ['TextKey', 'Line(...).TextKey(key: localizationKey) -> LineDef\nChoice(...).TextKey(key: localizationKey) -> ChoiceDef'],
  ['Track', 'Track(target: string, frames: KeyframeDef[]) -> TrackDef'],
  ['Timer', 'Timer(key: string, delayTicks?: int, repeat?: int, scope?: string) -> TriggerDef\nTimer(durationTicks: int).OnTimeout(choiceId).DefaultChoice(choiceId) -> dialogue timer'],
  ['TimeReached', 'TimeReached(key: string, dimension?: DimensionRef | string, dayTime: int) -> TriggerDef'],
  ['Trigger', 'Event().Trigger(trigger: TriggerDef) -> EventDef'],
  ['UnavailableLabel', 'Action().UnavailableLabel(text: string) -> ActionDef'],
  ['UnavailableLabelKey', 'Action().UnavailableLabelKey(key: localizationKey) -> ActionDef'],
  ['Update', 'Quest.Update(quest: QuestRef, status?: QuestStatus, title?: string, body?: string, stage?: QuestStageRef, showNotification?: bool) -> Effect'],
  ['Vec', 'Vec(x: number, y: number, z: number) -> Vector3'],
  ['Vector3', 'new Vector3(x: number, y: number, z: number) -> Vector3'],
  ['BoneDelta', 'BoneDelta(id: string, rotation?: Vector3, position?: Vector3, scale?: Vector3) -> BoneDeltaDef'],
  ['Channel', 'Channel(id: string) -> ChannelDef'],
  ['ControlDelta', 'ControlDelta(id: string, value: number | string | bool) -> ControlDeltaDef'],
  ['Finish', 'Finish(id: string) -> StoryNodeDef'],
  ['InitialState', 'InitialState(channel: string, state: string) -> InitialStateDef'],
  ['Main', 'Main(clip: AnimationClipId | string, mode?: string, durationTicks?: int, exitPoints?: ExitPointDef[]) -> MainAnimationDef'],
  ['Meta', 'Meta(key: string, value: string) -> MetaDef'],
  ['Video', 'Video.Play(source: string, title?: string, durationTicks?: int, skippable?: bool) -> Effect'],
  ['Vehicle', 'Vehicle.EnsureSpawn(spawnId: string) -> Effect\nVehicle.Trip(npc: NpcRef, spawnId: string, fromSegmentId: string, toSegmentId: string, tripId?: string, category?: string, backend?: string, speed?: number) -> Effect\nVehicle.Sequence(driverNpc: NpcRef, spawnId: string, fromSegmentId: string, destinationId: string, sequenceId?: string, passenger?: string, passengerSeat?: string, cinematic?: CameraPathRef) -> Effect'],
  ['EnsureSpawn', 'Vehicle.EnsureSpawn(spawnId: string) -> Effect'],
  ['Trip', 'Vehicle.Trip(npc: NpcRef, spawnId: string, fromSegmentId: string, toSegmentId: string, tripId?: string, category?: string, backend?: string, speed?: number, seat?: string) -> Effect'],
  ['Sequence', 'Vehicle.Sequence(driverNpc: NpcRef, spawnId: string, fromSegmentId: string, destinationId: string, sequenceId?: string, passenger?: string, passengerSeat?: string, cinematic?: CameraPathRef, completionPolicy?: string) -> Effect'],
  ['EnterVehicle', 'Npc.EnterVehicle(npc: NpcRef, vehicleEntityId: string, role?: string, seat?: string) -> Effect'],
  ['EnterNearestVehicle', 'Npc.EnterNearestVehicle(npc: NpcRef, radius?: number, role?: string, seat?: string) -> Effect'],
  ['EnterVehicleAt', 'Npc.EnterVehicleAt(npc: NpcRef, position: Vector3, role?: string, seat?: string, radius?: number, dimension?: DimensionRef) -> Effect'],
  ['LeaveVehicle', 'Npc.LeaveVehicle(npc: NpcRef) -> Effect'],
  ['Visible', 'action.Visible() inside if block sets action visibleWhen'],
  ['Wait', 'Wait(durationTicks: int) -> SequenceStep'],
  ['WaitChunks', 'WaitChunks(dimensionId: string, centerX: int, centerZ: int, radiusChunks?: int, timeoutTicks?: int) -> SceneStep'],
  ['World', 'World.TeleportPlayer(dimension: DimensionRef, position: Vector3) -> Effect\nWorld.RemovePlayerItem(item: ItemRef, count?: int) -> Effect\nWorld.SetNavigationTarget(targetId: string, x: number, y: number, z: number, label?: string) -> Effect\nWorld.ClearNavigationTarget() -> Effect'],
  ['Abort', 'Scene().Abort(effects: Effect...) -> SceneDef'],
  ['AbortEffects', 'private Effect[] AbortEffects() { EffectStatements... }\nАвтоматически прикрепляется к Scene().\nScene().AbortEffects(effects: Effect...) тоже принимается compiler-ом.'],
  ['ActionPerformed', 'ActionPerformed(actionKey: string) -> TriggerDef'],
  ['Actor', 'Cutscene().Actor(actor: PlayerActor | NpcRef, alias?: string) -> CutsceneDef'],
  ['AllowDialogueInput', 'Cutscene().AllowDialogueInput(enabled?: bool = true) -> CutsceneDef'],
  ['Animations', 'Animations.<Name> -> AnimationRef'],
  ['ApproachPlayer', 'Npc.ApproachPlayer(npc: NpcRef, stopDistance?: number, speed?: number, repathTicks?: int, timeoutTicks?: int, lookAtPlayer?: bool, warpWhenUnseen?: bool, warpDistance?: number, warpSearchRadius?: int, thenDialogue?: DialogueRef) -> Effect\nAlias для Npc.MoveToPlayer.'],
  ['AreaExit', 'AreaExit(exitedFlag: BoolStateRef, dimension: DimensionRef, min: Vector3, max: Vector3) -> TriggerDef'],
  ['BlendTicks', 'Camera().BlendTicks(ticks: int) -> CameraDef\nScene().BlendTicks(ticks: int) -> SceneDef'],
  ['BlinkTravelY', 'FaceAnimation().BlinkTravelY(value: number) -> FaceAnimationDef'],
  ['Build', 'public override <ResourceDef> Build()'],
  ['CameraPath', 'CameraPath() -> CameraPathDef'],
  ['CameraPathCue', 'CameraPathCue(path: CameraPathRef, atTick?: int) -> CameraPathCueDef'],
  ['CameraSample', 'CameraSample(tick: int, position: Vector3, yaw?: number, pitch?: number) -> CameraPathSampleDef'],
  ['Default', 'Localization().Default(enabled?: bool = true) -> LocalizationDef'],
  ['Decrement', 'State.Decrement(state: StateRef, amount?: int = 1) -> Effect'],
  ['Despawn', 'Npc.Despawn(npc: NpcRef) -> Effect'],
  ['Dialogues', 'StoryIndex().Dialogues(paths: string[] | DialogueRef...) -> StoryIndexDef\nDialogues.<Name> -> DialogueRef'],
  ['Editor', 'CameraPath().Editor(fields: Field...) -> CameraPathDef'],
  ['End', 'Animation().End(mode: string) -> AnimationDef\nFaceAnimation().End(mode: string) -> FaceAnimationDef'],
  ['Entries', 'Localization().Entries(entries: Entry...) -> LocalizationDef'],
  ['Entry', 'Entry(source: string, clip: AnimationClipId | string, durationTicks?: int) -> AnimationEntryDef\nEntry(key: localizationKey, value: string) -> LocalizationEntry'],
  ['Exit', 'Exit(id: string, clip: AnimationClipId | string, durationTicks?: int) -> AnimationExitDef'],
  ['ExitPoint', 'ExitPoint(id: string, progress?: string) -> AnimationExitPointDef'],
  ['Events', 'StoryIndex().Events(paths: string[] | EventRef...) -> StoryIndexDef'],
  ['ExitAnimationId', 'Animation().ExitAnimationId(animation: AnimationRef | string) -> AnimationDef'],
  ['FadeIn', 'Camera().FadeIn(ticks: int) -> CameraDef'],
  ['FadeInTicks', 'Camera().FadeInTicks(ticks: int) -> CameraDef'],
  ['FadeOut', 'Camera().FadeOut(ticks: int) -> CameraDef'],
  ['FadeOutTicks', 'Camera().FadeOutTicks(ticks: int) -> CameraDef'],
  ['Focus', 'Camera().Focus(position: Vector3) -> CameraDef'],
  ['Format', 'CameraPath().Format(format: string) -> CameraPathDef'],
  ['Hide', 'Fog.Hide(id?: string) -> Effect\nBlackScreen.Hide(durationTicks?: int) -> Effect'],
  ['Ik', 'Bone(...).Ik(enabled?: bool = true) -> BoneTimelineDef'],
  ['Input', 'Cutscene().Input(mode: string) -> CutsceneDef'],
  ['Items', 'Items.SparkGrass -> echostoryengine:spark_grass'],
  ['FaceLayer', 'FaceLayer.Base -> base\nFaceLayer.Overlay -> overlay\nFaceLayer.Detail -> detail'],
  ['Layer', 'Layer.Base -> base\nLayer.Overlay -> overlay'],
  ['LengthSeconds', 'CameraPath().LengthSeconds(seconds: number) -> CameraPathDef'],
  ['Locale', 'Localization().Locale(locale: string) -> LocalizationDef'],
  ['Localization', 'Localization() -> LocalizationDef'],
  ['LockControls', 'Camera().LockControls(enabled?: bool = true) -> CameraDef'],
  ['LockMouse', 'Camera().LockMouse(enabled?: bool = true) -> CameraDef'],
  ['OnTimeout', 'Timer(durationTicks).OnTimeout(choiceId: string) -> TimerDef'],
  ['DefaultChoice', 'Timer(durationTicks).DefaultChoice(choiceId: string) -> TimerDef'],
  ['OldFriend', 'OldFriend.Started / MoneyGiven / ItemReady / Paid -> StateRef\nOldFriend.Quest -> quest id\nOldFriend.GoToFriendStage -> quest stage id'],
  ['LogTag', 'Cutscene().LogTag(tag: string) -> CutsceneDef'],
  ['Manual', 'Manual(key: string) -> TriggerDef'],
  ['Name', 'Resource().Name(name: string) -> ResourceDef'],
  ['NameKey', 'Resource().NameKey(key: localizationKey) -> ResourceDef'],
  ['Navigation', 'Navigation.SetTarget(targetId: string, x: int, y: int, z: int, label?: string, dimensionId?: DimensionRef | string, icon?: string, profile?: string) -> Effect\nNavigation.ClearTarget() -> Effect'],
  ['SetTarget', 'Navigation.SetTarget(targetId: string, x: int, y: int, z: int, label?: string, dimensionId?: DimensionRef | string, icon?: string, profile?: string) -> Effect'],
  ['ClearTarget', 'Navigation.ClearTarget() -> Effect'],
  ['SetNavigationTarget', 'World.SetNavigationTarget(targetId: string, x: int, y: int, z: int, label?: string, dimensionId?: DimensionRef | string, icon?: string, profile?: string) -> Effect'],
  ['ClearNavigationTarget', 'World.ClearNavigationTarget() -> Effect'],
  ['Part', 'Part(id: string, pose: PoseDef) -> FacePartDef'],
  ['Parts', 'FaceAnimation().Parts(parts: Part...) -> FaceAnimationDef'],
  ['OpenDoor', 'Npc.OpenDoor(npc: NpcRef, position: Vector3) -> Effect\nОткрывает дверь/блок от имени NPC. Обязательны npc и position.'],
  ['CloseDoor', 'Npc.CloseDoor(npc: NpcRef, position: Vector3) -> Effect\nЗакрывает дверь/блок от имени NPC. Обязательны npc и position.'],
  ['ToggleDoor', 'Npc.ToggleDoor(npc: NpcRef, position: Vector3) -> Effect\nПереключает дверь/блок от имени NPC. Обязательны npc и position.'],
  ['UseDoor', 'Npc.UseDoor(npc: NpcRef, position: Vector3) -> Effect\nВыполняет стандартное use-действие двери/блока от имени NPC.'],
  ['UseBlock', 'Npc.UseBlock(npc: NpcRef, position: Vector3, action?: string) -> Effect\nВыполняет block interaction от имени NPC. Обязательны npc и position, action/mode необязателен.'],
  ['InteractBlock', 'Npc.InteractBlock(npc: NpcRef, position: Vector3, action?: string) -> Effect\nAlias для Npc.UseBlock.'],
  ['Path', 'Line(...).Path(path: NpcPathDef) -> LineDef\nNpc.Path(npc: NpcRef, points: Vector3[], speed?: number, pauseTicks?: int, completionDistance?: number, teleportToTargetIfStuck?: bool, openDoors?: bool, canFloat?: bool, repathAfterTicks?: int, maxRepathAttempts?: int, thenDialogue?: DialogueRef) -> Effect'],
  ['Play', 'Video.Play(source: string, title?: string, durationTicks?: int, skippable?: bool) -> Effect\nCutscene.Play(cutscene: CutsceneRef | string) -> DialogueCommand'],
  ['PlayerJoin', 'PlayerJoin(key: string) -> TriggerDef'],
  ['PlayerRespawn', 'PlayerRespawn(key: string) -> TriggerDef'],
  ['PlayFaceAnimation', 'Npc.PlayFaceAnimation(npc: NpcRef, animation: FaceAnimationRef, layer?: FaceLayerRef, fadeInTicks?: int, fadeOutTicks?: int, durationTicks?: int) -> Effect'],
  ['Position', 'Camera().Position(position: Vector3) -> CameraDef'],
  ['PositionSource', 'Camera().PositionSource(source: string) -> CameraDef'],
  ['Prefix', 'Localization().Prefix(prefix: string) -> LocalizationDef'],
  ['Preset', 'Camera().Preset(presetId: string) -> CameraDef'],
  ['QuestMarker', 'QuestMarker(dimension: DimensionRef, position: Vector3, label?: string) -> QuestMarkerDef'],
  ['QuestStatus', 'QuestStatus.Active -> ACTIVE\nQuestStatus.Completed -> COMPLETED'],
  ['Release', 'Cutscene().Release(enabled?: bool = true) -> CutsceneDef'],
  ['RemovePlayerItem', 'World.RemovePlayerItem(item: ItemRef, count?: int = 1) -> Effect'],
  ['Rotate', 'Npc.Rotate(npc: NpcRef, rotation: Vector3 | yaw, speed?: number, lock?: bool, pitch?: number, roll?: number) -> Effect\nThis.Rotate(rotation: Vector3 | yaw, speed?: number, lock?: bool, pitch?: number, roll?: number) -> Effect\nPlayer.Rotate(yaw: number, pitch: number) -> Effect\nVector3 оси: x=pitch, y=yaw, z=roll. speed включает плавный поворот голова -> тело.'],
  ['Samples', 'CameraPath().Samples(samples: CameraSample...) -> CameraPathDef'],
  ['Scenes', 'StoryIndex().Scenes(paths: string[] | SceneRef...) -> StoryIndexDef\nScenes.<Name> -> SceneRef'],
  ['Show', 'Monologue.Show(speaker: SpeakerRef | string, text: string, thought?: bool, delay?: int, durationTicks?: int) -> Effect\nFog.Show(id: string, durationTicks?: int) -> Effect\nBlackScreen.Show(durationTicks?: int) -> Effect'],
  ['SpawnNearPlayer', 'Npc.SpawnNearPlayer(npc: NpcRef, forwardOffset?: number, sideOffset?: number, yOffset?: number) -> Effect'],
  ['Speakers', 'StoryIndex().Speakers(paths: string[] | SpeakerRef...) -> StoryIndexDef\nSpeakers.<Name> -> SpeakerRef'],
  ['Stay', 'Npc.Stay(npc: NpcRef, position?: Vector3 | bool, lookAtPlayer?: bool) -> Effect'],
  ['StopFaceAnimation', 'Line(...).StopFaceAnimation() -> LineDef\nNpc.StopFaceAnimation(npc: NpcRef, layer?: FaceLayerRef, fadeOutTicks?: int) -> Effect'],
  ['StoryIndex', 'StoryIndex() -> StoryIndexDef'],
  ['SuppressHud', 'Cutscene().SuppressHud(enabled?: bool = true) -> CutsceneDef'],
  ['TargetPitch', 'Camera().TargetPitch(pitch: number) -> CameraDef'],
  ['TargetYaw', 'Camera().TargetYaw(yaw: number) -> CameraDef'],
  ['TalkDistance', 'Speaker().TalkDistance(distance: number) -> SpeakerDef'],
  ['TeleportPlayer', 'World.TeleportPlayer(dimension: DimensionRef, position: Vector3) -> Effect'],
  ['Turn', 'Npc.Turn(npc: NpcRef, rotation: Vector3 | yaw, speed?: number, lock?: bool, pitch?: number, roll?: number) -> Effect\nThis.Turn(rotation: Vector3 | yaw, speed?: number, lock?: bool, pitch?: number, roll?: number) -> Effect\nAlias для Rotate.'],
  ['exists', 'exists(state: StateRef | memoryKey) -> bool'],
  ['if', 'if (condition) { statements... }']
]);

const ENGINE_1_8_METHOD_SIGNATURES = [
  ['Story', 'Story(id: StoryId | string, start: string) -> StoryResourceDef\nStory.Set(stateKey: string, value: any, next?: string) -> Effect\nStory.Load(storyId: StoryId | string, executionId?: string, next?: string) -> Effect'],
  ['OnStart', 'private void OnStart() inside StoryResource\nLinear story entry method used when the resource does not provide Nodes().'],
  ['Environment', 'Environment(id: EnvironmentId | string, dimension: DimensionRef | string, states: EnvironmentStateDef[]) -> EnvironmentResourceDef\nEnvironment.Set(environmentId: EnvironmentId | string, stateId: string, next?: string) -> Effect'],
  ['DefaultForm', 'DefaultForm(form: NpcFormId | string) -> NpcDefaultFormRef'],
  ['nameColor', 'nameColor(color: string) -> NpcNameColor\nNameColor(color: string) -> NpcNameColor'],
  ['NameColor', `${METHOD_SIGNATURES.get('NameColor')}\nnameColor(color: string) -> NpcNameColor\nNameColor(color: string) -> NpcNameColor`],
  ['Form', 'Form(id: NpcFormId | string, skin?: SkinDef, model?: string, dimensions?: DimensionsDef, bodyScale?: number, parts?: PartDef[], anchors?: AnchorDef[], animationGraph?: string, faceProfile?: string, portrait?: VisualDef, tags?: string[]) -> NpcFormResourceDef'],
  ['Dimensions', 'Dimensions(width: number, height: number, eyeHeight: number) -> DimensionsDef'],
  ['Part', 'Part(id: string, scale?: number, scaleX?: number, scaleY?: number, scaleZ?: number, pivotX?: number, pivotY?: number, pivotZ?: number, rotationX?: number, rotationY?: number, rotationZ?: number) -> NpcFormPartDef\nPart(id: string, pose: PoseDef) -> FacePartDef'],
  ['Anchor', 'Anchor(id: string, x: number, y: number, z: number) -> NpcFormAnchorDef'],
  ['SetBlock', 'SetBlock(position: PositionDef, blockId: string, properties?: PropertiesDef, nbt?: string) -> EnvironmentChangeDef'],
  ['ClearBlock', 'ClearBlock(position: PositionDef) -> EnvironmentChangeDef'],
  ['Properties', 'Properties(properties: PropertyDef...) -> PropertiesDef'],
  ['Property', 'Property(name: string, value: string) -> PropertyDef'],
  ['Position', 'Position(x: int, y: int, z: int) -> PositionDef\nCamera().Position(position: Vector3) -> CameraDef'],
  ['Dialogue', 'Dialogue(id: DialogueId | string) -> DialogueResourceDef\nDialogue() -> DialogueDef\nDialogue.Start(dialogue: DialogueRef | string) -> Effect\nDialogue.Load(dialogueId: DialogueId | string, audienceId?: string, output?: string, next?: string) -> Effect'],
  ['Load', 'Dialogue.Load(dialogueId: DialogueId | string, audienceId?: string, output?: string, next?: string) -> Effect\nStory.Load(storyId: StoryId | string, executionId?: string, next?: string) -> Effect'],
  ['Screen', 'Screen.SetBlack(canvasGroup: string, alpha?: number, time?: int, durationMillis?: int, output?: string, next?: string) -> Effect'],
  ['SetBlack', 'Screen.SetBlack(canvasGroup: string, alpha?: number, time?: int, durationMillis?: int, output?: string, next?: string) -> Effect'],
  ['Player', 'Player.SpawnAt(x: number, y: number, z: number, yaw?: number, pitch?: number, output?: string, next?: string) -> Effect\nPlayer.SetHotbarItem(slot: int, itemId: string, count?: int, replace?: bool, next?: string) -> Effect\nPlayer.SelectHotbarSlot(slot: int, next?: string) -> Effect\nPlayer.SitAt(x: number, y: number, z: number, timeoutTicks?: int, next?: string) -> Effect\nPlayer.Rotate(yaw: number, pitch: number) -> Effect\nPlayer.LookAt(x: number, y: number, z: number) -> Effect\nPlayer.Move(x: number, y: number, z: number, dimension?: string) -> Effect\nPlayer.Stop() -> Effect\nPlayer.Interact(x: number, y: number, z: number, face?: string, hand?: string, maxDistance?: number) -> Effect\nPlayer.StandUp() -> Effect\nPlayer.StopUsingItem() -> Effect\nPlayer.IsMoving() -> Predicate\nPlayer.IsSitting() -> Predicate\nPlayer.SelectedHotbarSlot() -> Predicate\nPlayer.IsUsingItem() -> Predicate\nPlayer.SetFatigue(value: int) -> Effect\nPlayer.AddFatigue(value: int) -> Effect\nPlayer.ReduceFatigue(value: int) -> Effect\nPlayer.RestoreFatigue() -> Effect\nPlayer.Teleport(instanceId?: string, target?: string, playerName?: string, playerUuid?: string, output?: string, next?: string) -> Effect\nPlayer.GiveItem(item: ItemRef, count?: int = 1, autoEquip?: bool) -> Effect\nPlayer.Pose(pose: string) -> Effect'],
  ['SpawnAt', 'Npc.SpawnAt(npc: NpcRef, x: number, y: number, z: number, yaw?: number, pitch?: number, worldId?: string) -> Effect\nPlayer.SpawnAt(x: number, y: number, z: number, yaw?: number, pitch?: number, output?: string, next?: string) -> Effect'],
  ['SetHotbarItem', 'Player.SetHotbarItem(slot: int, itemId: string, count?: int, replace?: bool, next?: string) -> Effect'],
  ['SelectHotbarSlot', 'Player.SelectHotbarSlot(slot: int, next?: string) -> Effect'],
  ['SitAt', 'Player.SitAt(x: number, y: number, z: number, timeoutTicks?: int, next?: string) -> Effect'],
  ['Blackspace', 'Blackspace.Create(instanceId?: string, id?: string, playerName?: string, playerUuid?: string, output?: string, next?: string) -> Effect'],
  ['Create', 'Blackspace.Create(instanceId?: string, id?: string, playerName?: string, playerUuid?: string, output?: string, next?: string) -> Effect'],
  ['Teleport', 'Player.Teleport(instanceId?: string, target?: string, playerName?: string, playerUuid?: string, output?: string, next?: string) -> Effect'],
  ['Npc', `${METHOD_SIGNATURES.get('Npc')}\nNpc.SpawnAt(npc: NpcRef, x: number, y: number, z: number, yaw?: number, pitch?: number, worldId?: string) -> Effect\nNpc.SleepAt(npc: NpcRef, bedX: number, bedY: number, bedZ: number, expectedBlock?: string, worldId?: string) -> Effect\nNpc.WakeUp(npc: NpcRef) -> Effect\nNpc.Interact(npc: NpcRef, x: number, y: number, z: number, face?: string, hand?: string, approach?: string, dimension?: string, output?: string, next?: string) -> Effect\nNpc.Interact(npc: NpcRef, position: BlockPosition, face?: string, hand?: string, approach?: string, dimension?: string, output?: string, next?: string) -> Effect\nNpc.StopInteraction(npc: NpcRef, output?: string, next?: string) -> Effect\nNpc.StandUp(npc: NpcRef, output?: string, next?: string) -> Effect\nNpc.Stop(npc: NpcRef, output?: string, next?: string) -> Effect`],
  ['SleepAt', 'Npc.SleepAt(npc: NpcRef, bedX: number, bedY: number, bedZ: number, expectedBlock?: string, worldId?: string) -> Effect'],
  ['WakeUp', 'Npc.WakeUp(npc: NpcRef) -> Effect'],
  ['SetForm', 'Npc.SetForm(npc: NpcRef, form: NpcFormRef | string) -> DialogueCommand'],
  ['LookAt', 'Player.LookAt(x: number, y: number, z: number) -> Effect'],
  ['Interact', 'Npc.Interact(npc: NpcRef, x: number, y: number, z: number, face?: string, hand?: string, approach?: string, dimension?: string, output?: string, next?: string) -> Effect\nNpc.Interact(npc: NpcRef, position: BlockPosition, face?: string, hand?: string, approach?: string, dimension?: string, output?: string, next?: string) -> Effect\nPlayer.Interact(x: number, y: number, z: number, face?: string, hand?: string, maxDistance?: number) -> Effect'],
  ['StopInteraction', 'Npc.StopInteraction(npc: NpcRef, output?: string, next?: string) -> Effect'],
  ['StandUp', 'Player.StandUp() -> Effect\nNpc.StandUp(npc: NpcRef, output?: string, next?: string) -> Effect'],
  ['StopUsingItem', 'Player.StopUsingItem() -> Effect'],
  ['IsMoving', 'Player.IsMoving() -> Predicate'],
  ['IsSitting', 'Player.IsSitting() -> Predicate'],
  ['SelectedHotbarSlot', 'Player.SelectedHotbarSlot() -> Predicate'],
  ['IsUsingItem', 'Player.IsUsingItem() -> Predicate'],
  ['SetFatigue', 'Player.SetFatigue(value: int) -> Effect\nSetFatigue(value: int) -> Effect'],
  ['AddFatigue', 'Player.AddFatigue(value: int) -> Effect\nAddFatigue(value: int) -> Effect'],
  ['ReduceFatigue', 'Player.ReduceFatigue(value: int) -> Effect\nReduceFatigue(value: int) -> Effect'],
  ['RestoreFatigue', 'Player.RestoreFatigue() -> Effect\nRestoreFatigue() -> Effect'],
  ['Stop', 'Npc.Stop(npc: NpcRef, output?: string, next?: string) -> Effect\nPlayer.Stop() -> Effect\nCutscene.Stop(cutscene: CutsceneRef | string) -> DialogueCommand'],
  ['GoTo', 'Dialogue.GoTo(node: MethodRef) -> DialogueTransition'],
  ['ClearFaceAnimation', 'ClearFaceAnimation(subject: NpcRef | string) -> DialogueCommand'],
  ['ClearFaceAnimations', 'ClearFaceAnimations() -> DialogueCommand'],
  ['SetBlinkTiming', 'SetBlinkTiming(subject: NpcRef | string, periodTicks: int, closeTicks: int, holdTicks: int, openTicks: int) -> DialogueCommand'],
  ['SetIdleBlinkTiming', 'SetIdleBlinkTiming(subject: NpcRef | string, periodTicks: int, closeTicks: int, holdTicks: int, openTicks: int) -> DialogueCommand'],
  ['ClearBlinkTiming', 'ClearBlinkTiming(subject: NpcRef | string) -> DialogueCommand'],
  ['ClearIdleBlinkTiming', 'ClearIdleBlinkTiming(subject: NpcRef | string) -> DialogueCommand'],
  ['NodeId', 'NodeId(id: string) marker inside DialogueResource node methods'],
  ['Sleep', 'Sleep.Arm(scenario?: string) -> Effect\nSleep.Disarm() -> Effect\nSleep.CloseEyes(duration?: int) -> Effect\nSleep.WaitUntilClosed() -> Effect\nSleep.EnterDream(target?: string, blackspaceInstanceId?: string, dimension?: string, x?: number, y?: number, z?: number, yaw?: number, pitch?: number) -> Effect\nSleep.OpenEyes(duration?: int, sleepyBlink?: bool, showHud?: bool) -> Effect\nSleep.WaitUntilOpen() -> Effect\nSleep.Wait(seconds?: number, duration?: int, ticks?: int) -> Effect\nSleep.WaitSeconds(seconds?: number, duration?: int, ticks?: int) -> Effect\nSleep.ReturnToBed(mode?: string) -> Effect\nSleep.CompleteNight() -> Effect\nSleep.Complete() -> Effect\nSleep.Cancel() -> Effect\nSleep.IsActive(output?: string) -> Effect\nSleep.IsArmed(output?: string) -> Effect\nSleep.IsEyesClosed(output?: string) -> Effect\nSleep.Blink(count: int, duration?: int, closedWait?: number, openWait?: number, sleepyBlink?: bool, showHud?: bool) inside OnStart'],
  ['Arm', 'Sleep.Arm(scenario?: string, player?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect'],
  ['Disarm', 'Sleep.Disarm(player?: string, scenario?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect'],
  ['CloseEyes', 'Sleep.CloseEyes(duration?: int, player?: string, scenario?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect'],
  ['WaitUntilClosed', 'Sleep.WaitUntilClosed(player?: string, scenario?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect'],
  ['EnterDream', 'Sleep.EnterDream(target?: string, blackspaceInstanceId?: string, blackspaceId?: string, dimension?: string, worldId?: string, x?: number, y?: number, z?: number, yaw?: number, pitch?: number, output?: string, next?: string) -> Effect'],
  ['OpenEyes', 'Sleep.OpenEyes(duration?: int, sleepyBlink?: bool, showHud?: bool, player?: string, scenario?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect'],
  ['WaitUntilOpen', 'Sleep.WaitUntilOpen(player?: string, scenario?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect'],
  ['Wait', 'Wait(durationTicks: int) -> SequenceStep\nSleep.Wait(seconds?: number, duration?: int, ticks?: int, player?: string, scenario?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect\nCutscene.Wait(cutscene: CutsceneRef | string) -> DialogueCommand'],
  ['Blink', 'Sleep.Blink(count: int, duration?: int, closedWait?: number, openWait?: number, sleepyBlink?: bool, showHud?: bool, player?: string, scenario?: string, owner?: string, command?: string) inside OnStart -> generated sleep actions'],
  ['WaitPrepared', 'Cutscene.WaitPrepared(cutscene: CutsceneRef | string) -> DialogueCommand'],
  ['WaitSeconds', 'Sleep.WaitSeconds(seconds?: number, duration?: int, ticks?: int, player?: string, scenario?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect'],
  ['ReturnToBed', 'Sleep.ReturnToBed(mode?: string, player?: string, scenario?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect'],
  ['CompleteNight', 'Sleep.CompleteNight(player?: string, scenario?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect'],
  ['Cancel', 'Sleep.Cancel(player?: string, scenario?: string, owner?: string, command?: string, output?: string, next?: string) -> Effect'],
  ['IsActive', 'Sleep.IsActive(output?: string, player?: string, scenario?: string, owner?: string, command?: string, next?: string) -> Effect'],
  ['IsArmed', 'Sleep.IsArmed(output?: string, player?: string, scenario?: string, owner?: string, command?: string, next?: string) -> Effect'],
  ['IsEyesClosed', 'Sleep.IsEyesClosed(output?: string, player?: string, scenario?: string, owner?: string, command?: string, next?: string) -> Effect'],
  ['Set', `${METHOD_SIGNATURES.get('Set')}\nStory.Set(stateKey: string, value: any, next?: string) -> Effect\nEnvironment.Set(environmentId: EnvironmentId | string, stateId: string, next?: string) -> Effect`],
  ['State', `${METHOD_SIGNATURES.get('State')}\nState(id: string, changes: EnvironmentChangeDef[]) -> EnvironmentStateDef`]
];

const TRIGGER_PREDICATE_NAMES = [
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

const TRIGGER_EVENT_NAMES = [
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

const PLAYER_SLEEP_SELECTOR_NAMES = ['Confirmed', 'EnteredDream', 'Returned', 'Completed', 'Failed'];
const PLAYER_SEATING_SELECTOR_NAMES = ['SeatedIn', 'SeatedAt', 'StoodUpIn', 'StoodUpFrom', 'ChangedSeatIn'];

const ENGINE_1_8_4_METHOD_SIGNATURES = [
  ['PlayerProfile', 'PlayerProfile(id: PlayerProfileTemplateId | string, nameMode?: ACCOUNT | CUSTOM, displayName?: string, spark?: int, maxSpark?: int, skinMode?: ACCOUNT | RESOURCE, skinId?: string, skinModel?: CLASSIC | SLIM) -> PlayerProfile'],
  ['SetPlayerName', 'SetPlayerName(name: string) inside player dialogue side calls'],
  ['Weapon', 'Weapon(id: EchoItemId | string, name: string, texture: string, description?: string, tooltip?: string[], model?: string, damage?: number, attackDamage?: number, attacksPerSecond?: number, attackSpeedModifier?: number, knockback?: number, durability?: int, unbreakable?: bool, enchantability?: int, repairIngredient?: string, rarity?: COMMON | UNCOMMON | RARE | EPIC, glint?: bool, fireResistant?: bool, equipmentSlot?: HEAD | CHEST | LEGS | FEET, hiddenFromCreative?: bool, creativeOrder?: int) -> WeaponDefinition'],
  ['TriggerResource', 'class Name : TriggerResource { private void Init() { Id("namespace:path"); Start(Sequence); } }'],
  ['OnTriggerEnter', 'private void OnTriggerEnter()\nprivate void OnTriggerEnter(PlayerRef player)\nprivate void OnTriggerEnter(TriggerEvent event)'],
  ['OnTriggerExit', 'private void OnTriggerExit()\nprivate void OnTriggerExit(PlayerRef player)\nprivate void OnTriggerExit(TriggerEvent event)'],
  ['OnTriggerStay', 'private void OnTriggerStay()\nprivate void OnTriggerStay(PlayerRef player)\nprivate void OnTriggerStay(TriggerEvent event)'],
  ['PlayerSleep', 'PlayerSleep.Confirmed(scenario: string, sleepCount?: int, sleepCountAtLeast?: int) -> TriggerEventSelector\nPlayerSleep.EnteredDream/Returned/Completed/Failed(scenario: string, sleepCount?: int, sleepCountAtLeast?: int) -> TriggerEventSelector'],
  ['Subscribe', 'Subscribe(PlayerSleep.Confirmed(scenario, sleepCount?: int, sleepCountAtLeast?: int)) inside TriggerResource.Init()\nSubscribe(Player.SeatedIn(zone) | Player.SeatedAt(zone, slot) | Player.StoodUpIn(zone) | Player.StoodUpFrom(zone, slot) | Player.ChangedSeatIn(zone)) inside TriggerResource.Init()'],
  ['StartStory', 'StartStory(story: StoryId | string) inside TriggerResource.Init()'],
  ['StartDialogue', 'StartDialogue(dialogue: DialogueId | string) inside TriggerResource.Init() after Subscribe(Player.Seated*)\nStartDialogue(dialogue: DialogueId | string, sessionId?: string, audienceId?: string) inside TriggerResource callbacks'],
  ['DialogueStart', 'DialogueStart(dialogue: DialogueId | string, sessionId?: string, audienceId?: string) inside TriggerResource callbacks'],
  ['Debug', 'Debug.Mark(label: string) inside TriggerResource callbacks'],
  ['Mark', 'Debug.Mark(label: string) inside TriggerResource callbacks'],
  ['Scope', 'Scope(GLOBAL | SERVER | PLAYER | DIALOGUE_SESSION) inside TriggerResource.Init()'],
  ['Mode', 'Mode(ONCE | REPEAT) inside TriggerResource.Init()'],
  ['AllowManualFire', 'AllowManualFire() inside TriggerResource.Init()'],
  ['Cooldown', 'Cooldown(ticks: int | Ticks(int)) inside TriggerResource.Init()'],
  ['Dimension', 'Dimension(dimensionId: string) inside TriggerResource.Init()'],
  ['Bind', 'Bind(areaField: Area | BoxArea | SphereArea | CylinderArea | CircleArea | PolygonArea) inside TriggerResource.Init()'],
  ['FireOnlyOnce', 'FireOnlyOnce() inside TriggerResource.Init()'],
  ['EnterOnlyOnce', 'EnterOnlyOnce() inside TriggerResource.Init()'],
  ['ExitOnlyOnce', 'ExitOnlyOnce() inside TriggerResource.Init()'],
  ['StayOnlyOnce', 'StayOnlyOnce() inside TriggerResource.Init()'],
  ['FireCount', 'FireCount(count: int) inside TriggerResource.Init()'],
  ['StayInterval', 'StayInterval(ticks: int | Ticks(int)) inside TriggerResource.Init()'],
  ['StayPolicy', 'StayPolicy(SKIP_WHILE_RUNNING | QUEUE_ONE) inside TriggerResource.Init()'],
  ['Ticks', 'Ticks(value: int) -> trigger tick duration'],
  ['Area', 'Area(dimension: string, x: int, y: int, z: int, sx?: int, sy?: int, sz?: int) -> Area'],
  ['BoxArea', 'BoxArea(min: Vector3, max: Vector3) -> AreaShape'],
  ['SphereArea', 'SphereArea(center: Vector3, radius: number) -> AreaShape'],
  ['CylinderArea', 'CylinderArea(center: Vector3, radius: number, minY?: number, maxY?: number) -> AreaShape'],
  ['CircleArea', 'CircleArea(center: Vector3, radius: number, minY?: number, maxY?: number) -> AreaShape'],
  ['PolygonArea', 'PolygonArea(points: Vector3[], minY?: number, maxY?: number) -> AreaShape'],
  ['Any', 'Any(expressions: TriggerExpression...) -> TriggerExpression'],
  ['All', 'All(expressions: TriggerExpression...) -> TriggerExpression'],
  ['Not', 'Not(expression: TriggerExpression) -> TriggerExpression'],
  ['Wait', `${METHOD_SIGNATURES.get('Wait')}\nWait(expression: TriggerExpression) inside TriggerResource sequence methods`],
  ['Id', `${METHOD_SIGNATURES.get('Id')}\nId(id: TriggerId | string) inside TriggerResource.Init()`],
  ['Build', `${METHOD_SIGNATURES.get('Build')}\npublic override DialogueResourceDef Build() inside DialogueResource\npublic override NpcResourceDef Build() inside NpcResource\npublic override NpcFormResourceDef Build() inside NpcFormResource\npublic override EnvironmentResourceDef Build() inside EnvironmentResource\npublic override NpcAnimationBindingDef Build() inside NpcAnimationBindingResource\npublic override AnimationDef Build() inside AnimationResource\npublic override AnimationProfileDef Build() inside AnimationProfileResource\npublic override AnimationRigDef Build() inside AnimationRigResource\npublic override AnimationClipDef Build() inside AnimationClipResource\npublic override FaceRigDef Build() inside FaceRigResource\npublic override FacePoseDef Build() inside FacePoseResource\npublic override FacialAnimationDef Build() inside FacialAnimationResource\npublic override PlayerProfile Build() inside PlayerProfileTemplate\npublic override WeaponDefinition Build() inside WeaponResource`],
  ['FireOnlyOncePerPlayer', 'FireOnlyOncePerPlayer() inside TriggerResource.Init() alias for FireOnlyOnce()']
];

for (const name of TRIGGER_PREDICATE_NAMES) {
  ENGINE_1_8_4_METHOD_SIGNATURES.push([name, `${name}(area?: AreaRef, target?: string, count?: int, value?: number, level?: int, day?: int, start?: int, end?: int) -> TriggerExpression`]);
}
for (const name of TRIGGER_EVENT_NAMES) {
  ENGINE_1_8_4_METHOD_SIGNATURES.push([name, `${name}(area?: AreaRef, target?: string, count?: int, value?: number, to?: string) -> TriggerExpression`]);
}
for (const name of PLAYER_SLEEP_SELECTOR_NAMES) {
  ENGINE_1_8_4_METHOD_SIGNATURES.push([name, `PlayerSleep.${name}(scenario: string, sleepCount?: int, sleepCountAtLeast?: int) -> TriggerEventSelector`]);
}
ENGINE_1_8_4_METHOD_SIGNATURES.push([
  'Seating',
  'Player.SeatedIn(zone: SeatZoneId | string) -> TriggerEventSelector\nPlayer.SeatedAt(zone: SeatZoneId | string, slot: SeatSlotId | string) -> TriggerEventSelector\nPlayer.StoodUpIn(zone: SeatZoneId | string) -> TriggerEventSelector\nPlayer.StoodUpFrom(zone: SeatZoneId | string, slot: SeatSlotId | string) -> TriggerEventSelector\nPlayer.ChangedSeatIn(zone: SeatZoneId | string) -> TriggerEventSelector'
]);
for (const name of PLAYER_SEATING_SELECTOR_NAMES) {
  const slotRequired = name === 'SeatedAt' || name === 'StoodUpFrom';
  ENGINE_1_8_4_METHOD_SIGNATURES.push([
    name,
    slotRequired
      ? `Player.${name}(zone: SeatZoneId | string, slot: SeatSlotId | string) -> TriggerEventSelector`
      : `Player.${name}(zone: SeatZoneId | string) -> TriggerEventSelector`
  ]);
}

const ENGINE_1_8_5_METHOD_SIGNATURES = [
  ['Effect', `${METHOD_SIGNATURES.get('Effect')}\nEffect(id: EffectId | string) -> EffectDefinition inside EffectResource.Build()\nEffect.ApplyToPlayer(effectId: EffectId | string, intensity?: number) -> EffectCommand\nEffect.RemoveFromPlayer(effectId: EffectId | string, fadeOut?: Ticks | Seconds) -> EffectCommand\nEffect.WaitOnPlayer(effectId: EffectId | string) -> EffectCommand\nEffect.IsActiveOnPlayer(effectId: EffectId | string) -> EffectCommand\nEffect.HasCompletedOnPlayer(effectId: EffectId | string) -> EffectCommand`],
  ['EffectResource', 'class Name : EffectResource { public override EffectDefinition Build() { return Effect("namespace:path"); } private void Init() { Duration(...); } }'],
  ['Duration', 'Duration(Ticks(value) | Seconds(value) | InfiniteDuration()) inside EffectResource.Init()'],
  ['Stacking', 'Stacking(IGNORE_NEW | REFRESH_DURATION | RESTART | STACK_INTENSITY | ALLOW_MULTIPLE | REPLACE_OLD) inside EffectResource.Init()'],
  ['Lifetime', 'Lifetime(INDEPENDENT | OWNER_BOUND | SESSION_BOUND | UNTIL_REMOVED) inside EffectResource.Init()'],
  ['EndOn', 'EndOn(PLAYER_DEATH, WORLD_EXIT, DISCONNECT, DIMENSION_CHANGE, OWNER_CANCELLED, OWNER_COMPLETED) inside EffectResource.Init()'],
  ['Ticks', 'Ticks(value: int) -> trigger tick duration\nTicks(value: number) -> EffectTime inside EffectResource curves/durations'],
  ['Seconds', 'Seconds(value: number) -> EffectTime inside EffectResource curves/durations'],
  ['InfiniteDuration', 'InfiniteDuration() -> EffectDuration'],
  ['Curve', 'Curve(keys: EffectCurveKey...) -> EffectFloatCurve'],
  ['Color', 'Color(hex: string) -> EffectColor'],
  ['Key', `${METHOD_SIGNATURES.get('Key')}\nKey(time: Ticks | Seconds, value: number, interpolation?: STEP | LINEAR | SMOOTH | CUBIC) -> EffectCurveKey`],
  ['schema', 'schema version:int inside CameraTrack text DSL'],
  ['key', 'key timeMillis x y z yaw pitch fov inside CameraTrack text DSL'],
  ['mode', 'mode timeMillis CameraControlMode inside CameraTrack text DSL'],
  ['target', 'target timeMillis NONE | FIXED_POINT x y z | PLAYER anchor binding | NPC id anchor binding | BETWEEN first second weight inside CameraTrack text DSL'],
  ['framing', 'framing timeMillis horizontal vertical inside CameraTrack text DSL'],
  ['Movement', 'Movement.SpeedMultiplier(Curve(Key(...))) inside EffectResource.Init()'],
  ['SpeedMultiplier', 'Movement.SpeedMultiplier(curve: EffectFloatCurve) inside EffectResource.Init()'],
  ['Camera', `${METHOD_SIGNATURES.get('Camera')}\nCamera.FovOffset(curve: EffectFloatCurve) inside EffectResource.Init()\nCamera.Roll(curve: EffectFloatCurve) inside EffectResource.Init()\nCamera.Shake(amplitude: EffectFloatCurve) inside EffectResource.Init()`],
  ['FovOffset', 'Camera.FovOffset(curve: EffectFloatCurve) inside EffectResource.Init()'],
  ['Roll', 'Camera.Roll(curve: EffectFloatCurve) inside EffectResource.Init()'],
  ['Shake', 'Camera.Shake(amplitude: EffectFloatCurve) inside EffectResource.Init()'],
  ['Screen', 'Screen.SetBlack(canvasGroup: string, alpha?: number, time?: int, durationMillis?: int, output?: string, next?: string) -> Effect\nScreen.Tint(color: Color, opacity: EffectFloatCurve) inside EffectResource.Init()\nScreen.Vignette(color: Color, opacity: EffectFloatCurve) inside EffectResource.Init()'],
  ['Tint', 'Screen.Tint(color: Color, opacity: EffectFloatCurve) inside EffectResource.Init()'],
  ['Vignette', 'Screen.Vignette(color: Color, opacity: EffectFloatCurve) inside EffectResource.Init()'],
  ['Sound', 'Sound.PlayAt(time?: Ticks | Seconds, sound: string, volume?: number, pitch?: number) inside EffectResource.Init()'],
  ['PlayAt', 'Sound.PlayAt(time?: Ticks | Seconds, sound: string, volume?: number, pitch?: number) inside EffectResource.Init()'],
  ['VanillaEffect', 'VanillaEffect.Apply(time?: Ticks | Seconds, effect: string, duration: Ticks | Seconds, amplifier?: number, ambient?: bool, visible?: bool, showIcon?: bool) inside EffectResource.Init()'],
  ['Apply', 'VanillaEffect.Apply(...) applies a vanilla Minecraft MobEffect such as minecraft:nausea from an EffectResource.'],
  ['ApplyToPlayer', 'Effect.ApplyToPlayer(effectId: EffectId | string, intensity?: number) -> EffectCommand'],
  ['RemoveFromPlayer', 'Effect.RemoveFromPlayer(effectId: EffectId | string, fadeOut?: Ticks | Seconds) -> EffectCommand'],
  ['WaitOnPlayer', 'Effect.WaitOnPlayer(effectId: EffectId | string) -> EffectCommand'],
  ['IsActiveOnPlayer', 'Effect.IsActiveOnPlayer(effectId: EffectId | string) -> EffectCommand'],
  ['HasCompletedOnPlayer', 'Effect.HasCompletedOnPlayer(effectId: EffectId | string) -> EffectCommand'],
  ['WearableResource', 'class Name : WearableResource { private void Init() { Id(...); Profile(...); Item(...); Slot(...); EquipmentSlot(...); } }'],
  ['WardrobeProfileResource', 'class Name : WardrobeProfileResource { private void Init() { Id(...); BaseSkin(...); Slots(...); } }'],
  ['OutfitVariantResource', 'class Name : OutfitVariantResource { private void Init() { Id(...); Profile(...); Match(...); Skin(...); } }'],
  ['Id', 'Resource().Id(id: string) -> ResourceDef\nId(id: TriggerId | string) inside TriggerResource.Init()\nId(id: string) inside WearableResource/WardrobeProfileResource/OutfitVariantResource.Init()'],
  ['Profile', 'Profile(profileId: string) inside WearableResource or OutfitVariantResource.Init()'],
  ['Item', 'Item(itemId: string) inside WearableResource.Init()'],
  ['Slot', 'Slot(FACE | TOP | BOTTOM | FEET) inside WearableResource.Init()'],
  ['EquipmentSlot', 'EquipmentSlot(HEAD | CHEST | LEGS | FEET) inside WearableResource.Init()'],
  ['Collection', 'Collection(symbol) inside WearableResource.Init()'],
  ['Model', 'Model(modelId: string) inside WearableResource.Init()'],
  ['ModelPosition', 'ModelPosition(x: number, y: number, z: number) inside WearableResource.Init()'],
  ['ModelRotation', 'ModelRotation(x: number, y: number, z: number) inside WearableResource.Init()'],
  ['ModelScale', 'ModelScale(x: number, y: number, z: number) inside WearableResource.Init()'],
  ['PortraitTexture', 'PortraitTexture(textureId: string) inside WearableResource.Init()'],
  ['PortraitStrip', 'PortraitStrip(srcX: int, srcY: int, srcW: int, srcH: int, dstX: int, dstY: int, BASE | OUTER, pixelXOffset?: number, pixelYOffset?: number, pixelLeftInset?: number, pixelRightInset?: number, targetHeightScale?: number) inside WearableResource.Init()'],
  ['BaseSkin', 'BaseSkin(skinId: string) inside WardrobeProfileResource.Init()'],
  ['Coverage', 'Coverage(REPORT | STRICT) inside WardrobeProfileResource.Init()'],
  ['Slots', 'Slots(FACE, TOP, BOTTOM, FEET) inside WardrobeProfileResource.Init()'],
  ['Match', 'Match(face?: string, top?: string, bottom?: string, feet?: string) inside OutfitVariantResource.Init()'],
  ['Skin', `${METHOD_SIGNATURES.get('Skin')}\nSkin(skinId: string) inside OutfitVariantResource.Init() -> sets WorldSkin and FaceSkin`],
  ['WorldSkin', 'WorldSkin(skinId: string) inside OutfitVariantResource.Init()'],
  ['FaceSkin', 'FaceSkin(skinId: string) inside OutfitVariantResource.Init()'],
  ['FaceRig', 'FaceRig(id: FaceRigId | string, bones?: BoneDef[], layers?: LayerDef[], controls?: BlinkControlDef[]) -> FaceRigDef'],
  ['Rig', 'Rig(id: FaceRigId | string) helper inside FacialAnimationSurface classes'],
  ['Surface', 'Surface(name: string, rig?: FaceRigId | string, enter?: PhaseDef, stay?: PhaseDef, exit?: PhaseDef) -> FacialAnimationSurfaceDef'],
  ['Phase', 'Phase(durationTicks?: int, durationMillis?: int, end?: HOLD | RESET, tracks?: TrackDef[]) -> FacialAnimationPhaseDef'],
  ['Enter', 'Enter(durationTicks?: int, durationMillis?: int, end?: HOLD | RESET, tracks?: TrackDef[]) -> FacialAnimationPhaseDef'],
  ['Stay', `${METHOD_SIGNATURES.get('Stay')}\nStay(durationTicks?: int, durationMillis?: int, end?: HOLD | RESET, tracks?: TrackDef[]) -> FacialAnimationPhaseDef`],
  ['Exit', `${METHOD_SIGNATURES.get('Exit')}\nExit(durationTicks?: int, durationMillis?: int, end?: HOLD | RESET, tracks?: TrackDef[]) -> FacialAnimationPhaseDef`],
  ['Layer', `${METHOD_SIGNATURES.get('Layer')}\nLayer(id: string, bone: string, texture: string, role?: string, order?: int, visible?: bool, x?: int, y?: int, w?: int, h?: int) -> FaceLayerDef`],
  ['Socket', 'Socket(id: string, bone: string, x?: number, y?: number, z?: number, rotX?: number, rotY?: number, rotZ?: number, scaleX?: number, scaleY?: number, scaleZ?: number, pivotX?: number, pivotY?: number, pivotZ?: number) -> AnimationSocketDef'],
  ['BlinkControl', 'BlinkControl(id: string, upperTravel?: number, lowerTravel?: number, gazeMinX?: number, gazeMinY?: number, gazeMaxX?: number, gazeMaxY?: number, upperBone?: string, lowerBone?: string, idle?: bool, periodTicks?: int, closeTicks?: int, holdTicks?: int, openTicks?: int) -> FaceControlDef'],
  ['BoneDelta', `${METHOD_SIGNATURES.get('BoneDelta') || 'BoneDelta(id: string, x?: number, y?: number, z?: number, rotationZ?: number, scaleX?: number, scaleY?: number, scaleZ?: number) -> BoneDeltaDef'}`],
  ['ControlDelta', `${METHOD_SIGNATURES.get('ControlDelta') || 'ControlDelta(id: string, value: number) -> ControlDeltaDef'}`],
  ['Textures', 'Textures(root: string) inside high-level face DSL'],
  ['Eyes', 'Eyes { Sclera Left/Right; Pupil Left/Right; Eyelid Upper/Lower; } inside high-level face DSL'],
  ['Brows', 'Brows { Pivot(x, y); Brow Left/Right; } inside high-level face DSL'],
  ['Brow', 'Brow Left/Right inside Brows block of high-level face DSL'],
  ['Sclera', 'Sclera Left/Right inside Eyes block of high-level face DSL'],
  ['Pupil', 'Pupil Left/Right inside Eyes block of high-level face DSL'],
  ['Eyelid', 'Eyelid Upper/Lower inside Eyes block of high-level face DSL'],
  ['Texture', 'Texture(textureId: string) inside high-level face DSL visual settings'],
  ['TextureArea', 'TextureArea(x: int, y: int, width: int, height: int) inside high-level face DSL'],
  ['Size', 'Size(width: int, height: int) inside high-level face DSL'],
  ['Order', 'Order(value: int) inside high-level face DSL'],
  ['Role', 'Role(role: string) inside high-level face DSL'],
  ['Parent', 'Parent(parentBoneId: string) inside high-level face DSL Bone block'],
  ['Pivot', 'Pivot(x: number, y: number) inside high-level face DSL'],
  ['Travel', 'Travel(x: number, y: number) inside high-level face DSL Blink block'],
  ['Gaze', 'Gaze(x: number, y: number) inside high-level face DSL Blink block'],
  ['Idle', `${METHOD_SIGNATURES.get('Idle')}\nIdle(enabled: bool) inside high-level face DSL Blink block`],
  ['Timing', 'Timing(periodTicks: int, closeTicks: int, holdTicks: int, openTicks: int) inside high-level face DSL Blink block'],
  ['PeriodTicks', 'PeriodTicks(value: int) inside high-level face DSL Blink block'],
  ['CloseTicks', 'CloseTicks(value: int) inside high-level face DSL Blink block'],
  ['HoldTicks', 'HoldTicks(value: int) inside high-level face DSL Blink block'],
  ['OpenTicks', 'OpenTicks(value: int) inside high-level face DSL Blink block'],
  ['Blink', 'Sleep.Blink(count: int, duration?: int, closedWait?: number, openWait?: number, sleepyBlink?: bool, showHud?: bool, player?: string, scenario?: string, owner?: string, command?: string) inside OnStart -> generated sleep actions\nBlink(id: string) { Travel(...); Gaze(...); Timing(...); } inside high-level face DSL']
];

for (const [word, signature] of [...ENGINE_1_8_METHOD_SIGNATURES, ...ENGINE_1_8_4_METHOD_SIGNATURES, ...ENGINE_1_8_5_METHOD_SIGNATURES]) {
  METHOD_SIGNATURES.set(word, signature);
}

for (const [word, signature] of Object.entries({
  StoryResource: 'class Name : StoryResource { public override StoryResourceDef Build() { return Story("namespace:path", "start"); } private void OnStart() { ... } }',
  StoryResourceDef: 'Story(id: StoryId | string, start: string) -> StoryResourceDef',
  DialogueResource: 'class Name : DialogueResource { Node FirstMeeting() { Line("john", "..."); } } -> DialogueResourceDef\nID defaults to data namespace + .dialogue.es filename; namespace/Build/Init/Nodes are optional.',
  NpcResource: 'class Name : NpcResource { public override NpcResourceDef Build() { return Npc(...); } }',
  NpcFormResource: 'class Name : NpcFormResource { public override NpcFormResourceDef Build() { return Form(...); } }',
  EnvironmentResource: 'class Name : EnvironmentResource { public override EnvironmentResourceDef Build() { return Environment(...); } }',
  NpcAnimationBindingResource: 'class Name : NpcAnimationBindingResource { public override NpcAnimationBindingDef Build() { return NpcAnimationBinding(...); } }',
  AnimationResource: 'class Name : AnimationResource { public override AnimationDef Build() { return Animation(...); } }',
  AnimationProfileResource: 'class Name : AnimationProfileResource { public override AnimationProfileDef Build() { return AnimationProfile(...); } }',
  AnimationRigResource: 'class Name : AnimationRigResource { public override AnimationRigDef Build() { return AnimationRig(...); } }',
  AnimationClipResource: 'class Name : AnimationClipResource { public override AnimationClipDef Build() { return AnimationClip(...); } }',
  FaceRigResource: 'class Name : FaceRigResource { public override FaceRigDef Build() { return FaceRig(...); } }\nHigh-level face DSL may also use face Name("id") { ... } syntax.',
  FacePoseResource: 'class Name : FacePoseResource { public override FacePoseDef Build() { return FacePose(...); } }',
  FacialAnimationResource: 'class Name : FacialAnimationResource { public override FacialAnimationDef Build() { return FacialAnimation(...); } }',
  PlayerProfileTemplate: 'class Name : PlayerProfileTemplate { public override PlayerProfile Build() { return PlayerProfile(...); } }',
  WeaponResource: 'class Name : WeaponResource { public override WeaponDefinition Build() { return Weapon(...); } }',
  NodeDef: 'Node MethodName() { Line("john", "..."); }\nLegacy: private NodeDef MethodName() { return Node("id").Lines(...); } with dialogueNodes.',
  EnvironmentResourceDef: 'Environment(id: EnvironmentId | string, states?: EnvironmentStateDef[]) -> EnvironmentResourceDef',
  EnvironmentStateDef: 'State(id: string, changes: EnvironmentChangeDef[]) -> EnvironmentStateDef',
  EnvironmentChangeDef: 'SetBlock(x: int, y: int, z: int, block: string) -> EnvironmentChangeDef\nClearBlock(x: int, y: int, z: int) -> EnvironmentChangeDef',
  BodyAnimationId: 'private BodyAnimationId Anim = "namespace:path";\nLine(...).SetAnimation(Anim) or Npc.PlayAnimation(npc, Anim)',
  FaceAnimationId: 'private FaceAnimationId Face = "namespace:path";\nLine(...).SetAnimationFace(Face) or Npc.PlayFaceAnimation(npc, Face)',
  NpcAnimationBindingId: 'private NpcAnimationBindingId Binding = "namespace:path";\nNpcAnimationBinding(Binding, npc: ..., profile: ...)',
  PlayerRef: 'Trigger callback parameter type: private void OnTriggerEnter(PlayerRef player) { ... }',
  TriggerEvent: 'Trigger callback parameter type: private void OnTriggerEnter(TriggerEvent event) { ... }',
  Left: 'Face DSL side token: Left eye, brow, sclera, pupil or similar left-side face part.',
  Right: 'Face DSL side token: Right eye, brow, sclera, pupil or similar right-side face part.',
  Upper: 'Face DSL eyelid token: Upper eyelid part inside Eyes/Eyelid blocks.',
  Lower: 'Face DSL eyelid token: Lower eyelid part inside Eyes/Eyelid blocks.',
  SeatZoneResource: 'class Name : SeatZoneResource { private void Init() { Id("namespace:path"); Dimension("minecraft:overworld"); Area(Box(Position(minX, minY, minZ), Position(maxX, maxY, maxZ))); Slot("seat_id", Position(x, y, z)); } }',
  SeatZoneId: 'private SeatZoneId Zone = "namespace:path";\nId(Zone) inside SeatZoneResource.Init()',
  SeatSlotId: 'private SeatSlotId Chair = "chair";\nSlot(Chair, Position(x, y, z)) inside SeatZoneResource.Init()',
  Id: `${METHOD_SIGNATURES.get('Id')}\nId(id: SeatZoneId | string) inside SeatZoneResource.Init()`,
  Init: `${METHOD_SIGNATURES.get('Init')}\nprivate void Init() inside SeatZoneResource { Id(...); Dimension(...); Area(Box(...)); Slot(...); }`,
  Dimension: `${METHOD_SIGNATURES.get('Dimension') || 'Dimension(dimensionId: string) -> void'}\nDimension(dimensionId: string) inside SeatZoneResource.Init()`,
  Area: `${METHOD_SIGNATURES.get('Area')}\nArea(Box(Position(minX, minY, minZ), Position(maxX, maxY, maxZ))) inside SeatZoneResource.Init()`,
  Box: 'Box(min: Position, max: Position) -> SeatZoneBox inside SeatZoneResource.Area(...)',
  Detect: 'Detect(RIDING_ANCHOR | EXACT_SLOT_ONLY) inside SeatZoneResource.Init()',
  UnknownSeats: 'UnknownSeats(ALLOW | REJECT) inside SeatZoneResource.Init()',
  Slot: `${METHOD_SIGNATURES.get('Slot')}\nSlot(id: SeatSlotId | string, anchor: Position) inside SeatZoneResource.Init()`,
  MatchRadius: 'MatchRadius(radius: number) inside SeatZoneResource.Init()',
  Position: `${METHOD_SIGNATURES.get('Position')}\nPosition(x: number, y: number, z: number) inside SeatZoneResource Area/Slot definitions`,
  RIDING_ANCHOR: 'SeatDetectionPolicy value for Detect(...): resolve seats from the player riding anchor.',
  EXACT_SLOT_ONLY: 'SeatDetectionPolicy value for Detect(...): accept only exact authored slot anchors.',
  ALLOW: 'UnknownSeatPolicy value for UnknownSeats(...): allow unnamed seats in the zone.',
  REJECT: 'UnknownSeatPolicy value for UnknownSeats(...): reject seats that do not match authored slots.'
})) {
  METHOD_SIGNATURES.set(word, signature);
}

for (const [word, signature] of Object.entries({
  Presentation: 'Presentation(nameKey: string, descriptionKey: string, icon: string) inside EffectResource.Init()',
  PlayerHudConfigResource: 'class Name : PlayerHudConfigResource { private void Build() { PlayerHud(...); Bar(...); } }',
  PlayerHud: 'PlayerHud(id: string, referenceWidth?: int, referenceHeight?: int, scaleWithResolution?: bool) inside PlayerHudConfigResource.Build()',
  Bar: 'Bar(id: string, source: string, width: int, height: int, enabled?: bool, drawWhenUnavailable?: bool, icon?: string, iconWidth?: int, iconHeight?: int, iconSpacing?: int, iconOffsetX?: number, iconOffsetY?: number, anchor?: string, x?: int, y?: int, orientation?: HORIZONTAL | VERTICAL, fill?: LEFT_TO_RIGHT | RIGHT_TO_LEFT | TOP_TO_BOTTOM | BOTTOM_TO_TOP, background?: string, borderWidth?: int, borderColor?: string, gradientStart?: string, gradientEnd?: string, gradientSteps?: int) inside PlayerHudConfigResource.Build()',
  Description: 'private string Description() inside WeaponResource; pass as Weapon(description: Description())',
  Tooltip: 'private string[] Tooltip() inside WeaponResource; pass as Weapon(tooltip: Tooltip())',
  Damage: 'private int Damage() inside WeaponResource; pass as Weapon(damage: Damage())',
  AttackDamage: 'private number AttackDamage() inside WeaponResource; pass as Weapon(attackDamage: AttackDamage())',
  AttacksPerSecond: 'private number|string AttacksPerSecond() inside WeaponResource; pass as Weapon(attacksPerSecond: AttacksPerSecond())',
  AttackSpeedModifier: 'private number AttackSpeedModifier() inside WeaponResource; pass as Weapon(attackSpeedModifier: AttackSpeedModifier())',
  Knockback: 'private number Knockback() inside WeaponResource; pass as Weapon(knockback: Knockback())',
  Durability: 'private int Durability() inside WeaponResource; pass as Weapon(durability: Durability())',
  Unbreakable: 'private bool Unbreakable() inside WeaponResource; pass as Weapon(unbreakable: Unbreakable())',
  Enchantability: 'private int Enchantability() inside WeaponResource; pass as Weapon(enchantability: Enchantability())',
  RepairIngredient: 'private string RepairIngredient() inside WeaponResource; pass as Weapon(repairIngredient: RepairIngredient())',
  Rarity: 'private WeaponRarity Rarity() inside WeaponResource; pass as Weapon(rarity: Rarity())\nRarity.Common/Rarity.Uncommon/Rarity.Rare/Rarity.Epic static refs are also supported in completions.',
  Glint: 'private bool Glint() inside WeaponResource; pass as Weapon(glint: Glint())',
  FireResistant: 'private bool FireResistant() inside WeaponResource; pass as Weapon(fireResistant: FireResistant())',
  HiddenFromCreative: 'private bool HiddenFromCreative() inside WeaponResource; pass as Weapon(hiddenFromCreative: HiddenFromCreative())',
  CreativeOrder: 'private int CreativeOrder() inside WeaponResource; pass as Weapon(creativeOrder: CreativeOrder())'
})) {
  METHOD_SIGNATURES.set(word, signature);
}

for (const [word, signature] of Object.entries({
  DialogueOverlayStyleConfig: 'class Name : DialogueOverlayStyleConfig { public override DialogueOverlayStyleDef Build() { return DialogueOverlayStyle(...); } }',
  DialogueOverlayStyle: 'DialogueOverlayStyle(textScale?: number, textShadow?: bool, textAppearDelayMs?: int, typewriterMsPerCharacter?: int, typewriterMinDurationMs?: int, typewriterMaxDurationMs?: int, anchorX?: number, anchorY?: number, anchorOffsetX?: int, anchorOffsetY?: int, crosshairPaddingY?: int, screenMarginX?: int, screenMarginY?: int, minTextWidth?: int, maxTextWidth?: int, paddingX?: int, paddingY?: int, lineGap?: int, choiceSectionGap?: int, choicePaddingX?: int, choicePaddingY?: int, choiceGap?: int, visualSize?: int, visualGap?: int, visualTextOffsetY?: int, visualOffsetX?: int, visualOffsetY?: int, backgroundColor?: string, choiceBackgroundColor?: string, choiceBorderColor?: string, speakerColor?: string, textColor?: string, choiceNumberColor?: string, choiceTextColor?: string) -> DialogueOverlayStyleDef',
  DialoguePanelBackgroundOverlay: 'DialoguePanelBackgroundOverlay(mode?: SOLID | SLICED | TILED | string, texture?: string, textureWidth?: int, textureHeight?: int, sliceLeft?: int, sliceRight?: int, sliceTop?: int, sliceBottom?: int, tileScaleX?: number, tileScaleY?: number, tileOriginX?: int, tileOriginY?: int, tileFullOnly?: bool, fillDirection?: LEFT_TO_RIGHT | RIGHT_TO_LEFT | TOP_TO_BOTTOM | BOTTOM_TO_TOP | string, fillAmount?: number) -> DialoguePanelBackgroundConfig',
  DialogueChoiceBackgroundOverlay: 'DialogueChoiceBackgroundOverlay(mode?: SOLID | SLICED | TILED | string, texture?: string, textureWidth?: int, textureHeight?: int, sliceLeft?: int, sliceRight?: int, sliceTop?: int, sliceBottom?: int, tileScaleX?: number, tileScaleY?: number, tileOriginX?: int, tileOriginY?: int, tileFullOnly?: bool, fillDirection?: LEFT_TO_RIGHT | RIGHT_TO_LEFT | TOP_TO_BOTTOM | BOTTOM_TO_TOP | string, fillAmount?: number) -> DialoguePanelBackgroundConfig',
  PlayerVitalsConfigResource: 'class Name : PlayerVitalsConfigResource { private void Build() { PlayerFatigue(...); } }',
  PlayerFatigue: 'PlayerFatigue(id: string, maximum: int, restedReserveAbove?: number, tiredReserveAtMost?: number, tiredReserveAbove?: number, exhaustedReserveAtMost?: number, tiredMovementMultiplier?: number, exhaustedMovementMultiplier?: number, exhaustedNausea?: number) inside PlayerVitalsConfigResource.Build()',
  PortraitOverlayConfig: 'class Name : PortraitOverlayConfig { public override PortraitOverlayDef Build() { return PortraitOverlay(...); } }',
  PortraitOverlay: 'PortraitOverlay(visualId: string, centerX?: number, centerY?: number, offsetX?: number, offsetY?: number, facePixels?: number, layers?: Layer[], idle?: Blink[]) -> PortraitOverlayDef',
  Layer: `${METHOD_SIGNATURES.get('Layer')}\nLayer(id: string, placeX?: number, placeY?: number, offsetX?: number, offsetY?: number, scaleX?: number, scaleY?: number, w?: number, h?: number, visible?: bool) inside PortraitOverlay(...)`,
  Blink: `${METHOD_SIGNATURES.get('Blink')}\nBlink "id" { Travel(...); Gaze(...); Idle(...); PeriodTicks(...); CloseTicks(...); HoldTicks(...); OpenTicks(...); } inside PortraitOverlay idle block`
})) {
  METHOD_SIGNATURES.set(word, signature);
}

const METHOD_HOVER_SUMMARIES = new Map([
  ['Action', 'Builds a modular story graph action node, or an older interaction action definition.'],
  ['ActionKey', 'Assigns a stable action signal key so an ActionPerformed trigger can react to this action later.'],
  ['Actions', 'Attaches one or more interaction actions to a speaker.'],
  ['Alias', 'Binds a short local speaker alias to a speaker or NPC for dialogue and scene lines.'],
  ['Animation', 'Builds a modular animation lifecycle resource from entry clips, a main clip, exits, recovery, and replay policy.'],
  ['AnimationClip', 'Builds a normalized animation clip for a body or face rig.'],
  ['AnimationProfile', 'Builds an animation subject profile that binds a rig, graph, render mode, and required channels.'],
  ['AnimationRig', 'Builds a reusable body animation rig definition.'],
  ['AreaEnter', 'Starts an event when the player enters a configured area.'],
  ['AreaEnterDelayed', 'Starts an area-enter event only after the player stays in the area for the delay.'],
  ['Behavior', 'Builds NPC behavior defaults such as idle animation, schedules, and player look-at behavior.'],
  ['BlackScreen', 'Controls the black-screen overlay used for fades, wake prompts, and covered transitions.'],
  ['Bone', 'Defines animation frames for a single model bone.'],
  ['BoneDelta', 'Builds a face-pose bone delta.'],
  ['Build', 'Generic resource build entry point used when the compiler accepts the resource type from context.'],
  ['BuildAnimation', 'Animation resource build entry point. Return the Animation() definition from here.'],
  ['BuildCharacter', 'Character resource build entry point for character-style authored resources.'],
  ['BuildDialogue', 'Dialogue resource build entry point. Return the Dialogue() definition from here.'],
  ['BuildEvent', 'Event resource build entry point. Return the Event() definition from here.'],
  ['BuildQuest', 'Quest-related resource build entry point for quest authored resources.'],
  ['Camera', 'Builds a scene/dialogue camera setup: preset, position, focus, locks, and timing.'],
  ['CameraPath', 'Creates a sampled camera path resource for cinematics and camera cues.'],
  ['CameraPathCue', 'References a camera path at a timeline tick inside a cutscene or scene flow.'],
  ['Choice', 'Creates a dialogue choice, optionally with localization, conditions, next node, and effects.'],
  ['Channel', 'Declares an animation lifecycle channel.'],
  ['ClearHeldItem', 'Removes the current held item from an NPC or the current scene actor.'],
  ['ClearNavigationTarget', 'Clears the active GPS/navigation target through the shorter World command helper.'],
  ['ClearTarget', 'Clears the active GPS/navigation target from the player HUD.'],
  ['Complete', 'Завершает scene/dialogue section или закрывает активный Sleep flow через Sleep.Complete.'],
  ['CompleteEffects', 'Defines a reusable completion-effect method auto-attached by resource compilation.'],
  ['ControlDelta', 'Builds a face-pose control value delta.'],
  ['Cost', 'Adds an interaction cost such as money to an action.'],
  ['CooldownTicks', 'Prevents an event from firing again until the cooldown expires.'],
  ['Cutscene', 'Configures cutscene actors, camera, input locks, HUD suppression, and release behavior.'],
  ['Dialogue', 'Builds a dialogue resource and can start another dialogue as an effect.'],
  ['DisplayName', 'Sets the visible speaker name directly.'],
  ['DisplayNameKey', 'Sets the visible speaker name through a localization key.'],
  ['Do', 'Wraps effect statements for scene steps and conditional effect blocks.'],
  ['DurationTicks', 'Sets how long a camera, overlay, video, or timed element lasts.'],
  ['Effect', 'Creates a raw effect object when a typed helper is not available.'],
  ['Effects', 'Attaches runtime effects to events, actions, lines, choices, scenes, or dialogues.'],
  ['Event', 'Creates an event definition with trigger, conditions, effects, fire-once, and cooldown settings.'],
  ['Face', 'Sets or starts the face animation used by a dialogue line.'],
  ['FaceAnimation', 'Creates a face animation resource with part timelines and blink settings.'],
  ['FacePose', 'Builds a face pose resource for a face rig.'],
  ['FacialAnimation', 'Builds a facial animation resource with optional rig, surfaces, tracks, and end behavior.'],
  ['Field', 'Creates a key/value field for flexible object payloads.'],
  ['FireOnce', 'Marks an event or action as single-use in its relevant scope.'],
  ['Fog', 'Shows or hides story fog as a visual atmosphere effect.'],
  ['Frame', 'Creates a keyframe at a specific animation tick.'],
  ['Frames', 'Attaches keyframes to a bone or face part timeline.'],
  ['GiveItem', 'Gives an item stack to the player from a story effect.'],
  ['Goto', 'Branches dialogue flow to a node without implying linear next-node order.'],
  ['HoldItem', 'Makes an NPC or current actor hold a specific item in a specific hand.'],
  ['Id', 'Sets the stable resource id for the authored definition.'],
  ['Init', 'Defines dialogue initialization: start node, aliases, speakers, and cutscene setup.'],
  ['Idle', 'Sets the idle state/animation pair used by an NPC behavior.'],
  ['InteractionDialogues', 'Connects a speaker to one or more dialogues available on interaction.'],
  ['InventoryHasItem', 'Creates a trigger that fires when the player has the required item count.'],
  ['InventoryMissing', 'Creates a trigger that fires when the player lacks or spent the required item.'],
  ['Key', 'Assigns a localization key to a line or choice.'],
  ['Label', 'Sets direct visible UI text for an action.'],
  ['LabelKey', 'Sets localized visible UI text for an action.'],
  ['Line', 'Creates a dialogue line with speaker, text/localization, portrait, face, animation, and effects.'],
  ['LineAfterMove', 'Creates a dialogue line that is shown only after the matching dialogue Npc.Move(...) has arrived.'],
  ['LockPlayer', 'Locks or unlocks player movement during a scene.'],
  ['LookAtPlayers', 'Makes NPC behavior orient toward nearby players.'],
  ['Memory', 'Creates a memory StateRef; reads use the path and writes use memory.<path>.'],
  ['Monologue', 'Shows a single speaker line outside the normal dialogue node flow.'],
  ['Move', 'Moves an NPC toward a fixed point and can continue into a dialogue on arrival.'],
  ['MoveTo', 'Preferred NPC fixed-point movement helper; Move remains an alias.'],
  ['MoveToPlayer', 'Moves an NPC toward the player with pathing, timeout, and optional follow-up dialogue.'],
  ['Navigation', 'GPS/navigation effects for setting or clearing the active player target.'],
  ['SetTarget', 'Sets the active GPS target: stable targetId, coordinates, optional label, dimension, icon, and route profile.'],
  ['NameColor', 'Sets speaker or line name color.'],
  ['Next', 'Continues dialogue flow to the next node after a node or choice.'],
  ['NextNode', 'Sets the next node from compact local dialogue node code.'],
  ['Node', 'Creates a dialogue node containing lines and branch flow.'],
  ['Nodes', 'Returns the node list for a dialogue; use class-level arrays when several methods reference nodes.'],
  ['Npc', 'Builds an NPC definition resource, and exposes NPC runtime helper methods through Npc.*.'],
  ['NpcAnimationBinding', 'Binds an NPC definition or actor to an animation profile.'],
  ['NpcPath', 'Builds a reusable NPC path payload for line/path-driven staging.'],
  ['Player', 'Player runtime effects such as give item and presentation pose.'],
  ['PlayAnimation', 'Starts an animation on the current actor or a selected NPC, usually on an overlay layer.'],
  ['PlayFaceAnimation', 'Starts a face animation on a selected NPC.'],
  ['Portrait', 'Sets a speaker portrait id for speaker definitions or dialogue presentation.'],
  ['Quest', 'Quest helpers for updating quest text, stage, status, and notifications.'],
  ['Update', 'Updates quest UI/state with status, title/body, stage, and optional notification.'],
  ['Ref', 'Creates an exact StateRef for explicit namespaces such as debug.*, plot.*, world.*, scene.*, or memory.*.'],
  ['Set', 'Builds a story graph state-write node, or writes story state in runtime effect contexts.'],
  ['SetAnimation', 'Sets the body animation used by a dialogue line.'],
  ['SetAnimationFace', 'Sets the face animation used by a dialogue line.'],
  ['SetAnimationFaceClear', 'Clears the active face animation for a dialogue line.'],
  ['SetFaceAnimation', 'Sets the face animation used by a dialogue line.'],
  ['SetNavigationTarget', 'Sets a GPS/navigation target through the shorter World command helper.'],
  ['Scene', 'Builds a scene resource and can start another scene as an effect.'],
  ['Schedule', 'Creates a time-window schedule for NPC behavior.'],
  ['Speaker', 'Builds a speaker definition with display name, portrait, behavior, actions, and interaction distance.'],
  ['Spawn', 'Spawns an NPC in a dimension at a position.'],
  ['SpawnNearPlayer', 'Spawns an NPC around the player using forward/side/y offsets.'],
  ['Rotate', 'Rotates an NPC by Vector3 axes or yaw; smooth speed turns head first, then body.'],
  ['Start', 'Builds a story graph start node, starts a dialogue node locally, or starts a referenced dialogue/scene as an effect.'],
  ['StartEffects', 'Defines effects that run before the dialogue or scene body starts.'],
  ['State', 'State helpers create refs and runtime effects for story memory/state values.'],
  ['Step', 'Creates a raw scene sequence step when a typed helper is not enough.'],
  ['Steps', 'Attaches ordered sequence steps to a scene.'],
  ['StopAnimation', 'Stops an animation layer on the current actor or selected NPC.'],
  ['StopFaceAnimation', 'Stops a face animation layer on a selected NPC.'],
  ['Story', 'Builds the root modular story graph resource and names its start node.'],
  ['TextKey', 'Uses a localization key for line or choice text.'],
  ['Timer', 'Creates event timers or dialogue timers with timeout/default choice behavior.'],
  ['Trigger', 'Attaches the trigger that starts an event.'],
  ['Vec', 'Short vector helper for coordinates.'],
  ['Vector3', 'Coordinate vector for world positions.'],
  ['Finish', 'Builds a terminal story graph node.'],
  ['Skin', 'References an NPC skin resource for NPC definitions and visuals.'],
  ['SkinHead', 'References the head portrait visual derived from a skin.'],
  ['FaceLayer', 'Built-in face animation layer refs accepted by face animation helpers.'],
  ['Vehicle', 'Vehicle effects for spawn, trips, sequences, passengers, and cinematic routing.'],
  ['Wait', 'Pauses a sequence for a number of ticks.'],
  ['WaitChunks', 'Waits until chunks around a position are loaded before continuing.'],
  ['World', 'World command effects for teleport, inventory removal, and short-form navigation target commands.'],
  ['AbortEffects', 'Defines effects that run when a scene is aborted.'],
  ['ActionPerformed', 'Creates a trigger fired by an interaction action key.'],
  ['Actor', 'Adds an actor binding to a cutscene.'],
  ['ApproachPlayer', 'Moves an NPC toward the player with pathing, timeout, and optional follow-up dialogue.'],
  ['UseBlock', 'Runs a block interaction from an NPC at a target position.'],
  ['OpenDoor', 'Opens a door/block from an NPC at a target position.'],
  ['CloseDoor', 'Closes a door/block from an NPC at a target position.'],
  ['ToggleDoor', 'Toggles a door/block from an NPC at a target position.'],
  ['UseDoor', 'Uses a door/block from an NPC at a target position.'],
  ['InteractBlock', 'Короткая форма Npc.UseBlock: выполняет block interaction от имени NPC.'],
  ['Default', 'Marks localization resources or timer choices as default.'],
  ['Despawn', 'Removes an NPC from the world.'],
  ['Entries', 'Adds localization entries to a localization resource.'],
  ['Entry', 'Builds an animation entry phase, or one localization key/value pair.'],
  ['Exit', 'Builds a named animation exit phase.'],
  ['ExitPoint', 'Names a point where the main animation can leave into an exit phase.'],
  ['FadeIn', 'Sets fade-in timing for camera or visual transitions.'],
  ['FadeOut', 'Sets fade-out timing for camera or visual transitions.'],
  ['Focus', 'Sets the camera focus point.'],
  ['Hide', 'Hides a visual overlay/effect such as fog or black screen.'],
  ['Items', 'Built-in item refs; use these instead of unsupported raw placeholder symbols.'],
  ['InitialState', 'Declares the initial graph state for an animation channel.'],
  ['Layer', 'Built-in animation layer refs accepted by current animation helpers.'],
  ['Main', 'Builds the main phase of a modular animation lifecycle.'],
  ['Meta', 'Adds metadata to a resource builder payload.'],
  ['OldFriend', 'Built-in refs for the old-friend quest/state ids used by current story scripts.'],
  ['Play', 'Plays video or media content as a story effect.'],
  ['QuestStatus', 'Built-in quest status refs accepted by Quest.Update.'],
  ['Recovery', 'Configures animation lifecycle recovery behavior.'],
  ['RemovePlayerItem', 'Removes an item stack from the player inventory.'],
  ['Show', 'Shows a visual or dialogue-style effect such as monologue, fog, or black screen.'],
  ['Source', 'Declares an animation graph source state.'],
  ['Stay', 'Toggles or sets an NPC stay-still behavior at a position.'],
  ['Turn', 'Короткая форма Rotate(...): поворачивает NPC или player actor.'],
  ['TalkDistance', 'Sets the distance at which a speaker can be talked to.'],
  ['Track', 'Builds an animation track with keyframes for one target.'],
  ['TeleportPlayer', 'Teleports the player to a dimension and position.'],
  ['exists', 'Checks whether a StateRef or memory key exists for conditions.'],
  ['if', 'Creates a C#-style conditional block so generated effects stay readable and typed.']
]);

const METHOD_HOVER_SUMMARIES_RU = new Map([
  ['StoryResource', 'Базовый ресурс Echo Story: Build() возвращает Story(...), а OnStart() задает линейный стартовый сценарий, если graph не описан через Nodes().'],
  ['StoryResourceDef', 'Результат Story(...): корневое определение story graph со стартовым node id.'],
  ['DialogueResource', 'Базовый ресурс Echo Dialogue. В high-level форме достаточно Node-блоков: ID выводится из namespace ресурса и имени файла, один Node становится entry автоматически, а несколько требуют ровно один start Node. Legacy Build/Init/Nodes остается совместимым.'],
  ['NpcResource', 'Базовый ресурс Echo NPC: Build() возвращает Npc(...) с id, display, skin/defaultForm и другими параметрами персонажа.'],
  ['NpcFormResource', 'Базовый ресурс формы NPC: Build() возвращает Form(...) с skin/model, dimensions, parts, anchors, animationGraph, faceProfile и portrait.'],
  ['EnvironmentResource', 'Базовый ресурс окружения: Build() возвращает Environment(...) со states, где State(...) содержит SetBlock/ClearBlock изменения.'],
  ['NpcAnimationBindingResource', 'Ресурс привязки NPC к animation profile: Build() возвращает NpcAnimationBinding(...) для npc/actor и profile.'],
  ['AnimationResource', 'Базовый lifecycle-ресурс Echo Animation: Build() возвращает Animation(...) с channels/sources/entries/main/exits/recovery.'],
  ['AnimationProfileResource', 'Базовый ресурс animation profile: Build() возвращает AnimationProfile(...) с rig/graph/render/initialStates/requiredChannels.'],
  ['AnimationRigResource', 'Базовый ресурс body animation rig: Build() возвращает AnimationRig(...) с kind/skin/bones/sockets.'],
  ['AnimationClipResource', 'Базовый ресурс normalized body clip: Build() возвращает AnimationClip(...) с rig, tracks, frames и loop/speed параметрами.'],
  ['FaceRigResource', 'Базовый ресурс face rig: Build() возвращает FaceRig(...) или используется high-level face DSL с Eyes/Blink/Bone/Layer блоками.'],
  ['FacePoseResource', 'Базовый ресурс face pose: Build() возвращает FacePose(...) с rig, BoneDelta(...) и ControlDelta(...).'],
  ['FacialAnimationResource', 'Базовый ресурс facial animation: Build() возвращает FacialAnimation(...) с rig, surfaces, tracks, duration и end behavior.'],
  ['PlayerProfileTemplate', 'Базовый ресурс профиля игрока: Build() возвращает PlayerProfile(...) с id, nameMode, displayName, spark/maxSpark и skinMode.'],
  ['WeaponResource', 'Базовый ресурс оружия Echo Item Pack: Build() возвращает Weapon(...), а локальные helpers могут заполнять description, stats, durability и rarity.'],
  ['NodeDef', 'Строгая runtime-модель dialogue node. High-level `Node Name()` понижается в NodeDef автоматически; legacy методы по-прежнему поддерживаются.'],
  ['EnvironmentResourceDef', 'Результат Environment(...): описание ресурса окружения со списком states.'],
  ['EnvironmentStateDef', 'Результат State(...): именованное состояние окружения с набором EnvironmentChangeDef изменений.'],
  ['EnvironmentChangeDef', 'Изменение состояния окружения: SetBlock(...) ставит блок, ClearBlock(...) очищает позицию.'],
  ['BodyAnimationId', 'Тип ссылки на body animation resource: используется в SetAnimation(...) и Npc.PlayAnimation(...).'],
  ['FaceAnimationId', 'Тип ссылки на facial animation resource: используется в SetAnimationFace(...) и Npc.PlayFaceAnimation(...).'],
  ['NpcAnimationBindingId', 'Тип id для NpcAnimationBinding resource: связывает NPC definition/actor с animation profile.'],
  ['PlayerRef', 'Тип параметра reactive trigger callback: дает ссылку на игрока в OnTriggerEnter/Exit/Stay(PlayerRef player).'],
  ['TriggerEvent', 'Тип параметра reactive trigger callback: дает payload события в OnTriggerEnter/Exit/Stay(TriggerEvent event).'],
  ['Left', 'Face DSL side token: выбирает левую часть глаза, брови, зрачка или похожего face element.'],
  ['Right', 'Face DSL side token: выбирает правую часть глаза, брови, зрачка или похожего face element.'],
  ['Upper', 'Face DSL eyelid token: верхнее веко внутри Eyes/Eyelid блока.'],
  ['Lower', 'Face DSL eyelid token: нижнее веко внутри Eyes/Eyelid блока.'],
  ['DialoguePanelBackgroundOverlay', 'Config-call для фона dialogue panel: задает режим solid/sliced/tiled, texture, slice borders, tile settings и fill progress.'],
  ['DialogueChoiceBackgroundOverlay', 'Config-call для фона dialogue choice panel: использует те же параметры, что DialoguePanelBackgroundOverlay, но применяется к вариантам выбора.'],
  ['Action', 'Создает действие для взаимодействия со speaker/NPC: текст кнопки, стоимость, видимость и эффекты при нажатии.'],
  ['ActionKey', 'Задает стабильный ключ действия, чтобы триггер ActionPerformed мог отреагировать на выбор игрока.'],
  ['Actions', 'Прикрепляет список действий к speaker, чтобы игрок видел варианты взаимодействия.'],
  ['Alias', 'Создает короткий локальный alias speaker/NPC для строк диалога и сцен.'],
  ['Animation', 'Создает ресурс анимации персонажа: длительность, кости, loop, blend и вложенные треки.'],
  ['AreaEnter', 'Создает триггер события при входе игрока в область.'],
  ['AreaEnterDelayed', 'Создает триггер входа в область с задержкой, чтобы событие не срабатывало мгновенно.'],
  ['Behavior', 'Описывает поведение NPC: idle state, анимации, расписания и реакцию на игрока.'],
  ['BlackScreen', 'Показывает или скрывает черный экран для fade-переходов, wake prompt и скрытых телепортов.'],
  ['Build', 'Главный метод сборки ресурса, когда компилятор сам понимает тип результата из контекста.'],
  ['BuildAnimation', 'Метод сборки animation resource. Верни из него Animation().'],
  ['BuildDialogue', 'Метод сборки dialogue resource. Верни из него Dialogue().'],
  ['BuildEvent', 'Метод сборки event resource. Верни из него Event().'],
  ['BuildQuest', 'Метод сборки quest-related resource. Используй для ресурсов квестов и стадий.'],
  ['Story', 'Собирает корневой StoryResource: задает story id и стартовый node, откуда начинается graph.'],
  ['OnStart', 'Линейный стартовый метод StoryResource. Компилятор берет statements отсюда, если ресурс не описывает graph через Nodes().'],
  ['Environment', 'Собирает EnvironmentResource: набор именованных состояний, которые меняют блоки в указанном dimension/world.'],
  ['Form', 'Собирает NpcFormResource: skin/model, размеры тела, части, anchors, animationGraph, faceProfile, portrait и tags.'],
  ['DefaultForm', 'Ссылается на NpcFormResource из Npc(...), чтобы NPC по умолчанию использовал указанную форму.'],
  ['Dimensions', 'Задает collision width/height и eyeHeight для NPC form.'],
  ['Part', 'Описывает одну часть NPC form: scale, pivot и rotation по осям. Также используется как face-animation part track.'],
  ['Anchor', 'Создает именованную точку привязки NPC form, например для головы, глаз, диалога или предметов.'],
  ['BlockPosition', 'Создает block position x/y/z для dialogue-side Npc.Interact и других команд, где нужен именно блок, а не Vector3.'],
  ['Position', 'Создает координаты блока x/y/z для EnvironmentResource или задает позицию камеры в Camera().Position(...).'],
  ['Rotation', 'Модификатор Frame(...): задает поворот keyframe. Для face frames поддерживается Z-rotation: Rotation(z) или Rotation(0, 0, z).'],
  ['Scale', 'Модификатор Frame(...): задает scale-вектор keyframe по x/y/z.'],
  ['SetBlock', 'Добавляет изменение Environment state: поставить blockId в указанную Position с optional properties и NBT.'],
  ['ClearBlock', 'Добавляет изменение Environment state: очистить блок в указанной Position.'],
  ['Properties', 'Собирает список block-state свойств для SetBlock(...).'],
  ['Property', 'Создает одно block-state свойство name/value для Properties(...).'],
  ['Skin', 'Ссылается на NPC skin resource в Npc(...), Form(...) и visual helpers.'],
  ['SkinHead', 'Ссылается на портрет головы, производный от NPC skin resource.'],
  ['NameColor', 'Задает цвет имени speaker/line или NPC definition. В Npc(...) можно использовать NameColor(...) или nameColor(...).'],
  ['nameColor', 'Задает цвет имени NPC definition как positional helper внутри Npc(...). Принимает hex вроде "#RRGGBB" или "#RRGGBBAA".'],
  ['Camera', 'Настраивает камеру сцены/диалога: preset, позицию, фокус, блокировки и длительность.'],
  ['Choice', 'Создает вариант выбора в диалоге: текст, ключ локализации, условие, next/goto и эффекты.'],
  ['Cutscene', 'Cutscene helpers настраивают cutscene resource или запускают dialogue commands: Start, Play, Wait, WaitPrepared и Stop.'],
  ['CompleteEffects', 'Метод эффектов завершения. Компилятор автоматически прикрепляет его к Dialogue/Scene.'],
  ['StartEffects', 'Метод стартовых эффектов. Используй для действий перед началом Dialogue/Scene.'],
  ['AbortEffects', 'Метод эффектов отмены сцены. Используй для отката камеры, экрана или временных состояний.'],
  ['Effect', 'Создает raw effect, если для нужного действия еще нет typed helper-а.'],
  ['Effects', 'Прикрепляет runtime effects к событию, действию, строке, выбору, сцене или диалогу.'],
  ['Event', 'Создает story event: trigger, условия, effects, fireOnce и cooldown.'],
  ['FireOnce', 'Помечает событие или action как одноразовое в нужном scope.'],
  ['Line', 'Создает строку диалога: speaker, текст/локализацию, portrait, face/body animation и effects.'],
  ['Memory', 'Создает StateRef в namespace memory. Для чтения используется путь, для записи memory.<path>.'],
  ['Navigation', 'Navigation helpers ставят или очищают GPS/маршрутную цель игрока.'],
  ['SetTarget', 'Ставит активную GPS цель: стабильный targetId, координаты, label, dimension, icon и profile маршрута.'],
  ['ClearTarget', 'Очищает активную GPS/navigation цель игрока.'],
  ['SetNavigationTarget', 'Короткая World.* форма для установки GPS/navigation цели.'],
  ['ClearNavigationTarget', 'Короткая World.* форма для очистки GPS/navigation цели.'],
  ['Npc', 'NPC helpers управляют спавном, движением, предметами в руках, анимациями и транспортом.'],
  ['Spawn', 'Спавнит NPC в указанном измерении по координатам x/y/z. Используй для появления персонажа в мире; yaw/pitch задают поворот после спавна.'],
  ['SpawnAt', 'Спавнит NPC или переносит spawn/action позицию игрока по координатам x/y/z. Для NPC можно указать yaw, pitch и worldId.'],
  ['SpawnNearPlayer', 'Спавнит NPC рядом с игроком по forward/side/y offset.'],
  ['SleepAt', 'Укладывает NPC спать в кровать/блок по координатам bedX/bedY/bedZ. Можно проверить expectedBlock и worldId.'],
  ['WakeUp', 'Будит NPC после SleepAt и снимает sleep action state.'],
  ['SetForm', 'Меняет форму NPC на указанный NpcFormRef/form id внутри dialogue command flow.'],
  ['Despawn', 'Удаляет NPC из мира.'],
  ['Stop', 'Останавливает текущее действие NPC, scripted player actor command или Cutscene.Stop command.'],
  ['Interact', 'Заставляет NPC или игрока выполнить взаимодействие с блоком/точкой. Для NPC поддержаны формы Npc.Interact(npc, x, y, z) и Npc.Interact(npc, BlockPosition); face, hand и approach по умолчанию AUTO/MAIN_HAND/AUTO.'],
  ['StopInteraction', 'Останавливает активное NPC interaction действие и продолжает story node через output/next.'],
  ['Move', 'Двигает NPC к точке: в Dialogue/Trigger можно писать Npc.Move(npc, x, y, z), в Story graph доступна форма с node id и next. Также запускает Player.Move к координатам x/y/z в scripted actor flow.'],
  ['MoveTo', 'Предпочтительный helper для движения NPC к точке; Move остается alias.'],
  ['MoveToPlayer', 'Подводит NPC к игроку с pathing, timeout и необязательным диалогом после подхода.'],
  ['WalkTo', 'Короткая форма Npc.MoveTo: ведет NPC к указанной точке и может запустить thenDialogue после прибытия.'],
  ['WalkToPlayer', 'Короткая форма Npc.MoveToPlayer: подводит NPC к игроку с pathing, timeout и optional warp/follow-up dialogue.'],
  ['Path', 'Задает маршрут NPC по нескольким точкам или прикрепляет NpcPath к строке.'],
  ['Rotate', 'Поворачивает NPC по Vector3/yaw или игрока через Player.Rotate(yaw, pitch).'],
  ['Turn', 'Короткая форма Rotate(...): поворачивает NPC или player actor.'],
  ['UseBlock', 'Выполняет взаимодействие с блоком от имени NPC.'],
  ['InteractBlock', 'Короткая форма Npc.UseBlock: выполняет block interaction от имени NPC.'],
  ['OpenDoor', 'Открывает дверь или совместимый блок от имени NPC.'],
  ['CloseDoor', 'Закрывает дверь или совместимый блок от имени NPC.'],
  ['ToggleDoor', 'Переключает дверь или совместимый блок от имени NPC.'],
  ['UseDoor', 'Выполняет use-действие двери или блока от имени NPC.'],
  ['HoldItem', 'Кладет предмет в руку NPC или текущему actor-у сцены.'],
  ['ClearHeldItem', 'Очищает предмет в руке NPC или текущего actor-а.'],
  ['LookAtPlayers', 'Включает или выключает поворот NPC к ближайшим игрокам. Полезно для живых сцен и idle behavior.'],
  ['ClearLookAtPlayers', 'Сбрасывает forced look-at состояние NPC, чтобы он больше не следил взглядом за игроками.'],
  ['PlayAnimation', 'Запускает анимацию на NPC/current actor, обычно на overlay layer.'],
  ['StopAnimation', 'Останавливает анимационный layer у NPC/current actor.'],
  ['PlayFaceAnimation', 'Запускает face animation на конкретном NPC.'],
  ['StopFaceAnimation', 'Останавливает face animation layer у конкретного NPC.'],
  ['NpcPath', 'Создает payload маршрута NPC для постановочных линий и сцен.'],
  ['Quest', 'Quest helpers обновляют статус, текст, stage, marker и уведомление квеста.'],
  ['Update', 'Обновляет квест: status, title/body, stage, marker и showNotification.'],
  ['Scene', 'Создает scene resource или запускает scene как effect.'],
  ['Scene.Start', 'Запускает сцену по SceneRef/id.'],
  ['Dialogue', 'Создает dialogue resource или запускает dialogue как effect.'],
  ['Start', '`start Node Name()` отмечает entry в high-level диалоге; Start(...) сохраняется для legacy Dialogue, Story, TriggerResource.Init и runtime actions.'],
  ['Node', 'Объявляет high-level dialogue node как `Node Name() { ... }`; стабильный id нормализуется из имени символа. Legacy Node(...) builder также поддерживается.'],
  ['Nodes', 'Legacy-метод со списком nodes. В high-level DialogueResource Node-блоки находятся автоматически.'],
  ['Next', 'Продолжает flow диалога в следующий node.'],
  ['NextNode', 'Указывает следующий node в compact local dialogue коде.'],
  ['Goto', 'Переходит к node явно, без линейного next flow.'],
  ['GoTo', 'GoTo(NodeSymbol) переводит high-level dialogue к другому Node без строкового id; Go to Definition и rename работают по символу. Dialogue.GoTo остается legacy-алиасом.'],
  ['Formerly', 'Formerly("old_node_id", CurrentNode) объявляет явную миграцию сохраненного node id после переименования Node.'],
  ['NodeId', 'Marker/placeholder для явного id dialogue node, который compiler пропускает как служебный вызов.'],
  ['ClearFaceAnimation', 'Очищает face animation у указанного NPC/subject в dialogue command flow.'],
  ['ClearFaceAnimations', 'Очищает все активные face animations в текущем dialogue command flow.'],
  ['SetBlinkTiming', 'Задает blink timing для NPC/subject: periodTicks, closeTicks, holdTicks и openTicks.'],
  ['SetIdleBlinkTiming', 'Задает idle blink timing для NPC/subject теми же period/close/hold/open параметрами.'],
  ['ClearBlinkTiming', 'Сбрасывает blink timing у NPC/subject.'],
  ['ClearIdleBlinkTiming', 'Сбрасывает idle blink timing у NPC/subject.'],
  ['WaitPrepared', 'Cutscene.WaitPrepared ждет заранее подготовленную cutscene command stage.'],
  ['Play', 'Запускает Video.Play effect или Cutscene.Play dialogue command.'],
  ['Wait', 'Ждет sequence ticks, sleep flow, TriggerExpression или Cutscene.Wait command в зависимости от контекста.'],
  ['Speaker', 'Создает speaker definition: имя, portrait, поведение, actions и дистанции.'],
  ['DisplayName', 'Задает видимое имя speaker прямым текстом.'],
  ['DisplayNameKey', 'Задает видимое имя speaker через localization key.'],
  ['Portrait', 'Задает portrait id для speaker или line presentation.'],
  ['State', 'State helpers создают StateRef и effects для чтения/записи story state.'],
  ['Ref', 'Создает точный StateRef для debug.*, plot.*, world.*, scene.* или memory.* namespaces.'],
  ['Set', 'Записывает story state или создает Set(...) effect.'],
  ['Remove', 'Удаляет story state key или runtime StateRef значение.'],
  ['Clear', 'Очищает story state значение или группу state данных, если helper это поддерживает.'],
  ['Add', 'Добавляет числовое значение к StateRef. Используй для счетчиков и отношений.'],
  ['Adjust', 'Alias/форма изменения числового StateRef на заданную величину.'],
  ['Increment', 'Увеличивает числовой StateRef на 1 или указанное значение.'],
  ['Decrement', 'Уменьшает числовой StateRef на 1 или указанное значение.'],
  ['Timer', 'Создает event timer или dialogue timer с timeout/default choice.'],
  ['Trigger', 'Прикрепляет trigger, который запускает Event.'],
  ['TriggerResource', 'Ресурс нового trigger engine. В Init() объявляет id, scope/mode, area bindings, callbacks, sequence или story subscription.'],
  ['OnTriggerEnter', 'Callback TriggerResource при входе игрока в bound area. Может принимать no args, PlayerRef или TriggerEvent.'],
  ['OnTriggerExit', 'Callback TriggerResource при выходе игрока из bound area. Может принимать no args, PlayerRef или TriggerEvent.'],
  ['OnTriggerStay', 'Callback TriggerResource пока игрок остается в bound area. Интервал задается StayInterval(...).'],
  ['Id', 'Задает стабильный id ресурса. В TriggerResource используется внутри Init().'],
  ['Require', 'Story graph node: проверяет переменную перед переходом дальше. Если условие не выполнено, node не проходит дальше по next.'],
  ['Scope', 'Задает scope триггера: GLOBAL, SERVER, PLAYER или DIALOGUE_SESSION.'],
  ['Mode', 'Задает режим триггера: ONCE для одноразового запуска или REPEAT для повторных срабатываний.'],
  ['Subscribe', 'Подписывает TriggerResource на внешний routed event, например PlayerSleep.Confirmed(...).'],
  ['StartStory', 'Запускает StoryResource по story id после срабатывания subscription trigger.'],
  ['StartDialogue', 'В Init() TriggerResource запускает DialogueResource после subscription-события, сейчас только для Player seating selectors. В callback может быть alias для Dialogue.Start(...).'],
  ['DialogueStart', 'Callback alias для Dialogue.Start(...): запускает DialogueResource из OnTriggerEnter/Exit/Stay через Echo Dialogue service.'],
  ['Debug', 'Namespace для debug callback actions в TriggerResource, например Debug.Mark("label").'],
  ['Mark', 'Создает debug marker action в TriggerResource callback; полезно для тестов и диагностики callback-потока.'],
  ['AllowManualFire', 'Разрешает ручной запуск trigger через runtime/debug tooling.'],
  ['Cooldown', 'Задает cooldown между срабатываниями триггера в ticks.'],
  ['Dimension', 'Привязывает reactive trigger к Minecraft dimension. Нужен для area-based callbacks.'],
  ['Bind', 'Привязывает primary area/shape field к reactive trigger callbacks OnTriggerEnter/Exit/Stay.'],
  ['FireOnlyOnce', 'Ограничивает trigger одним успешным fire.'],
  ['FireOnlyOncePerPlayer', 'Политика TriggerResource поверх FireOnlyOnce(): ограничивает один успешный fire в соответствующем scope/player-контексте.'],
  ['EnterOnlyOnce', 'Ограничивает OnTriggerEnter одним запуском.'],
  ['ExitOnlyOnce', 'Ограничивает OnTriggerExit одним запуском.'],
  ['StayOnlyOnce', 'Ограничивает OnTriggerStay одним запуском.'],
  ['FireCount', 'Ограничивает общее число срабатываний trigger.'],
  ['StayInterval', 'Задает интервал проверки OnTriggerStay в ticks.'],
  ['StayPolicy', 'Задает поведение OnTriggerStay, если предыдущий запуск еще выполняется: skip или queue one.'],
  ['Ticks', 'Оборачивает числовое значение ticks для trigger policy методов.'],
  ['Area', 'Создает простую trigger area по dimension и координатам x/y/z с optional размером sx/sy/sz.'],
  ['BoxArea', 'Создает box shape для trigger area по двум углам Vector3.'],
  ['SphereArea', 'Создает sphere shape для trigger area по center и radius.'],
  ['CylinderArea', 'Создает cylinder shape для trigger area по center, radius и optional minY/maxY.'],
  ['CircleArea', 'Короткая форма cylinder/circle для trigger area по radius вокруг center.'],
  ['PolygonArea', 'Создает polygon trigger shape по списку Vector3 точек и optional minY/maxY.'],
  ['Any', 'Trigger expression: true, если сработало любое вложенное условие.'],
  ['All', 'Trigger expression: true, если сработали все вложенные условия.'],
  ['Not', 'Trigger expression: инвертирует вложенное условие.'],
  ['PlayerSleep', 'Namespace селекторов сна игрока для TriggerResource Subscribe(...).'],
  ['Confirmed', 'PlayerSleep selector: сон игрока подтвержден для указанного scenario.'],
  ['EnteredDream', 'PlayerSleep selector: игрок вошел в dream/blackspace фазу сна.'],
  ['Returned', 'PlayerSleep selector: игрок вернулся из dream/blackspace фазы.'],
  ['Completed', 'PlayerSleep selector: sleep flow завершился успешно.'],
  ['Failed', 'PlayerSleep selector: sleep flow завершился ошибкой или отменой.'],
  ['Seating', 'Группа Player seating selectors для TriggerResource Subscribe(...): реагирует на посадку, вставание и смену места в SeatZoneResource.'],
  ['SeatedIn', 'Player seating selector: TriggerResource сработает, когда игрок сел в любую точку указанной SeatZoneResource.'],
  ['SeatedAt', 'Player seating selector: TriggerResource сработает, когда игрок сел в конкретный Slot указанной SeatZoneResource.'],
  ['StoodUpIn', 'Player seating selector: TriggerResource сработает, когда игрок встал из любого места указанной SeatZoneResource.'],
  ['StoodUpFrom', 'Player seating selector: TriggerResource сработает, когда игрок встал из конкретного Slot указанной SeatZoneResource.'],
  ['ChangedSeatIn', 'Player seating selector: TriggerResource сработает, когда игрок сменил место внутри указанной SeatZoneResource.'],
  ['SetPlayerName', 'Меняет отображаемое имя игрока из player dialogue-side вызова. Принимает ровно одну строку.'],
  ['PlayerProfile', 'Собирает шаблон профиля игрока: id, режим имени, displayName, spark/maxSpark и режим skin.'],
  ['Weapon', 'Собирает weapon definition: id, имя, texture, combat stats, durability, rarity и визуальные флаги.'],
  ['Vehicle', 'Vehicle helpers управляют spawn, trip, sequence, пассажирами и cinematic маршрутом.'],
  ['EnsureSpawn', 'Гарантирует наличие vehicle spawn/id перед поездкой или cinematic sequence.'],
  ['Trip', 'Запускает поездку vehicle по road segments для NPC/driver.'],
  ['Sequence', 'Запускает vehicle cinematic sequence с destination, passenger/seat и optional camera path.'],
  ['World', 'World helpers делают teleport, удаление предметов и короткие navigation команды.'],
  ['Player', 'Player helpers меняют состояние игрока: actor-позицию, взгляд, движение, hotbar, fatigue/vitals, teleport, give item и pose.'],
  ['GiveItem', 'Выдает предмет игроку из story effect.'],
  ['SetHotbarItem', 'Кладет item stack в указанный hotbar slot игрока, optionally replacing текущий предмет.'],
  ['SelectHotbarSlot', 'Переключает выбранный hotbar slot игрока.'],
  ['SitAt', 'Сажает/фиксирует игрока в точке x/y/z, обычно для постановочной сцены.'],
  ['LookAt', 'Поворачивает взгляд игрока к точке x/y/z через player actor command.'],
  ['StandUp', 'Поднимает игрока из scripted sitting/actor состояния или снимает NPC с interaction seat anchor через Npc.StandUp.'],
  ['StopUsingItem', 'Прерывает использование предмета игроком.'],
  ['IsMoving', 'Predicate player actor: проверяет, выполняет ли игрок сейчас scripted movement.'],
  ['IsSitting', 'Predicate player actor: проверяет, сидит ли игрок в scripted seat.'],
  ['SelectedHotbarSlot', 'Predicate player actor: читает/проверяет выбранный hotbar slot.'],
  ['IsUsingItem', 'Predicate player actor: проверяет, использует ли игрок предмет.'],
  ['SetFatigue', 'Устанавливает fatigue игрока в неотрицательное значение. Доступно как Player.SetFatigue(...) и SetFatigue(...).'],
  ['AddFatigue', 'Добавляет fatigue игроку на неотрицательное значение. Доступно как Player.AddFatigue(...) и AddFatigue(...).'],
  ['ReduceFatigue', 'Уменьшает fatigue игрока на неотрицательное значение. Доступно как Player.ReduceFatigue(...) и ReduceFatigue(...).'],
  ['RestoreFatigue', 'Восстанавливает fatigue игрока через vitals adapter. Доступно как Player.RestoreFatigue() и RestoreFatigue().'],
  ['Teleport', 'Телепортирует игрока через blackspace routing по instanceId/target или player identity.'],
  ['Pose', 'Создает pose payload или меняет presentation pose игрока.'],
  ['Screen', 'Screen helpers управляют overlay/canvas эффектами интерфейса.'],
  ['SetBlack', 'Управляет черным overlay: canvasGroup/alpha/time или durationMillis, затем optional output/next.'],
  ['Blackspace', 'Blackspace helpers создают и используют отдельные blackspace instances для сна/телепорта.'],
  ['Create', 'Создает blackspace instance для игрока по instanceId/id и optional playerName/playerUuid.'],
  ['Sleep', 'Sleep helpers управляют story sleep flow: armed state, closing/opening eyes, blink, dream enter, return и completion.'],
  ['Arm', 'Подготавливает sleep scenario к перехвату сна игрока.'],
  ['Disarm', 'Отключает подготовленный sleep scenario/flow.'],
  ['CloseEyes', 'Закрывает sleep eye overlay за duration ticks.'],
  ['WaitUntilClosed', 'Ждет, пока sleep eye overlay полностью закроется.'],
  ['EnterDream', 'Переводит игрока в dream/blackspace target с dimension/координатами и yaw/pitch.'],
  ['OpenEyes', 'Открывает sleep eye overlay и optionally возвращает HUD.'],
  ['WaitUntilOpen', 'Ждет, пока sleep eye overlay полностью откроется.'],
  ['Blink', 'Linear-only helper Sleep.Blink для OnStart: собирает серию CloseEyes/Wait/OpenEyes шагов, чтобы игрок моргнул заданное число раз.'],
  ['WaitSeconds', 'Пауза sleep flow в секундах; alias/форма Sleep.Wait.'],
  ['ReturnToBed', 'Возвращает игрока из dream/blackspace обратно к кровати по выбранному mode.'],
  ['CompleteNight', 'Завершает ночной sleep flow как успешный сон.'],
  ['Cancel', 'Отменяет активный sleep flow.'],
  ['IsActive', 'Пишет в output, активен ли сейчас sleep flow.'],
  ['IsArmed', 'Пишет в output, вооружен ли sleep scenario для перехвата сна.'],
  ['IsEyesClosed', 'Пишет в output, закрыт ли sleep eye overlay.'],
  ['InventoryHasItem', 'Триггер срабатывает, когда у игрока есть нужный предмет/count.'],
  ['InventoryMissing', 'Триггер срабатывает, когда нужного предмета нет или он потрачен.'],
  ['Vec', 'Короткий helper для координат Vector3.'],
  ['Vector3', 'Вектор координат мира для NPC, камеры, маршрутов и world effects.'],
  ['QuestStatus', 'Встроенные quest status символы для Quest.Update.'],
  ['Layer', 'Встроенные animation layer символы, которые принимает текущий compiler.'],
  ['FaceLayer', 'Встроенные face animation layer символы: Base, Overlay, Detail.'],
  ['Hand', 'Встроенная ссылка на руку для item helpers.'],
  ['Items', 'Встроенные item refs. Используй их вместо неподдерживаемых placeholder symbols.'],
  ['Portraits', 'Встроенные portrait refs для speaker/line presentation.'],
  ['States', 'Встроенные state ids для Behavior/Schedule.'],
  ['exists', 'Проверяет, существует ли StateRef или memory key, для условий if/trigger.'],
  ['if', 'C#-style условный блок, чтобы typed generated effects оставались читаемыми.']
]);

for (const [name, summary] of Object.entries({
  PlayerInside: 'Trigger predicate: проверяет, находится ли игрок внутри указанной area/shape.',
  HasItem: 'Trigger predicate: проверяет, есть ли у игрока itemId/target в нужном количестве.',
  ItemCountAtLeast: 'Trigger predicate: item count у игрока не меньше заданного count.',
  ItemCountAtMost: 'Trigger predicate: item count у игрока не больше заданного count.',
  ItemCountExactly: 'Trigger predicate: item count у игрока ровно равен заданному count.',
  HasEmptyInventorySlot: 'Trigger predicate: проверяет, есть ли у игрока свободный слот инвентаря.',
  Holding: 'Trigger predicate: проверяет, держит ли игрок нужный item в любой руке.',
  HoldingMainHand: 'Trigger predicate: проверяет item в main hand.',
  HoldingOffHand: 'Trigger predicate: проверяет item в off hand.',
  Wearing: 'Trigger predicate: проверяет item/equipment в указанном armor slot.',
  HasAnyArmor: 'Trigger predicate: проверяет, надета ли у игрока хоть какая-то броня.',
  HasFullArmorSet: 'Trigger predicate: проверяет полный комплект брони.',
  HealthAtMost: 'Trigger predicate: здоровье игрока не выше заданного value.',
  HealthAtLeast: 'Trigger predicate: здоровье игрока не ниже заданного value.',
  FoodAtMost: 'Trigger predicate: hunger/food игрока не выше заданного value.',
  FoodAtLeast: 'Trigger predicate: hunger/food игрока не ниже заданного value.',
  IsSneaking: 'Trigger predicate: игрок сейчас crouch/sneak.',
  IsSprinting: 'Trigger predicate: игрок сейчас sprint.',
  IsSwimming: 'Trigger predicate: игрок сейчас swimming.',
  IsSleeping: 'Trigger predicate: игрок сейчас sleeping.',
  IsOnGround: 'Trigger predicate: игрок стоит на земле.',
  IsBurning: 'Trigger predicate: игрок горит.',
  HasEffect: 'Trigger predicate: у игрока есть potion/effect target.',
  EffectAmplifierAtLeast: 'Trigger predicate: amplifier эффекта не ниже заданного level/value.',
  EffectDurationAtLeast: 'Trigger predicate: длительность эффекта не ниже заданного value.',
  AdvancementCompleted: 'Trigger predicate: advancement target завершен.',
  ExperienceLevelAtLeast: 'Trigger predicate: уровень опыта игрока не ниже заданного level.',
  InDimension: 'Trigger predicate: игрок находится в указанном dimension.',
  GameModeIs: 'Trigger predicate: game mode игрока совпадает с target.',
  TimeBetween: 'Trigger predicate: world day time находится между start и end.',
  DayAtLeast: 'Trigger predicate: номер дня не ниже заданного day.',
  WeatherIs: 'Trigger predicate: текущая погода совпадает с target.'
})) {
  METHOD_HOVER_SUMMARIES_RU.set(name, summary);
}

for (const [name, summary] of Object.entries({
  PlayerEnter: 'Trigger event: игрок вошел в указанную area/shape.',
  PlayerExit: 'Trigger event: игрок вышел из указанной area/shape.',
  PlayerStay: 'Trigger event: игрок остается внутри area/shape; частоту задает StayInterval(...).',
  ItemAdded: 'Trigger event: предмет добавлен в инвентарь игрока.',
  ItemRemoved: 'Trigger event: предмет удален из инвентаря игрока.',
  ItemPickedUp: 'Trigger event: игрок поднял item из мира.',
  ItemDropped: 'Trigger event: игрок выбросил item.',
  ItemUsed: 'Trigger event: игрок использовал item.',
  ItemConsumed: 'Trigger event: игрок consumed/съел item.',
  InventoryBecameFull: 'Trigger event: инвентарь игрока стал полным.',
  InventorySlotFreed: 'Trigger event: в инвентаре игрока освободился слот.',
  HeldItemChanged: 'Trigger event: изменился предмет в руках игрока.',
  MainHandChanged: 'Trigger event: изменился предмет в main hand.',
  OffHandChanged: 'Trigger event: изменился предмет в off hand.',
  EquipmentChanged: 'Trigger event: изменилось equipment/armor состояние.',
  EquipmentEquipped: 'Trigger event: игрок экипировал предмет.',
  EquipmentUnequipped: 'Trigger event: игрок снял предмет экипировки.',
  PlayerDamaged: 'Trigger event: игрок получил урон.',
  PlayerHealed: 'Trigger event: игрок восстановил здоровье.',
  PlayerDied: 'Trigger event: игрок умер.',
  PlayerRespawned: 'Trigger event: игрок respawned.',
  PlayerJumped: 'Trigger event: игрок прыгнул.',
  SneakStarted: 'Trigger event: игрок начал sneak.',
  SneakStopped: 'Trigger event: игрок перестал sneak.',
  SprintStarted: 'Trigger event: игрок начал sprint.',
  SprintStopped: 'Trigger event: игрок перестал sprint.',
  SwimmingStarted: 'Trigger event: игрок начал swimming.',
  SwimmingStopped: 'Trigger event: игрок перестал swimming.',
  SleepStarted: 'Trigger event: игрок начал sleep.',
  SleepFinished: 'Trigger event: игрок завершил sleep.',
  PlayerStartedBurning: 'Trigger event: игрок начал гореть.',
  PlayerStoppedBurning: 'Trigger event: игрок перестал гореть.',
  EffectAdded: 'Trigger event: на игрока добавлен effect.',
  EffectRemoved: 'Trigger event: с игрока снят effect.',
  EffectAmplifierChanged: 'Trigger event: amplifier эффекта изменился.',
  ExperienceLevelReached: 'Trigger event: игрок достиг уровня опыта.',
  AdvancementGranted: 'Trigger event: игрок получил advancement.',
  DimensionChanged: 'Trigger event: игрок сменил dimension.',
  DimensionEntered: 'Trigger event: игрок вошел в dimension.',
  DimensionLeft: 'Trigger event: игрок вышел из dimension.',
  GameModeChanged: 'Trigger event: game mode игрока изменился.',
  BlockInteracted: 'Trigger event: игрок взаимодействовал с блоком.',
  BlockBroken: 'Trigger event: игрок сломал блок.',
  BlockPlaced: 'Trigger event: игрок поставил блок.',
  ContainerOpened: 'Trigger event: игрок открыл container.',
  BedEntered: 'Trigger event: игрок лег в bed.',
  BedLeft: 'Trigger event: игрок покинул bed.',
  TimeReached: 'Trigger event: world time достиг заданного значения.',
  DayReached: 'Trigger event: world day достиг заданного значения.',
  WeatherChanged: 'Trigger event: погода изменилась.'
})) {
  METHOD_HOVER_SUMMARIES_RU.set(name, summary);
}

for (const [name, summary] of Object.entries({
  EffectResource: 'Ресурс Echo Effects: Build() возвращает Effect("id"), а Init() задает длительность, stacking/lifetime и каналы движения, камеры, экрана или звука.',
  Effect: 'Создает raw story effect или id ресурса Echo Effects. В namespace Effect.* также есть команды применить/снять/подождать эффект на игроке.',
  Duration: 'Задает длительность EffectResource через Ticks(...), Seconds(...) или InfiniteDuration().',
  Stacking: 'Задает, как повторное применение эффекта ведет себя относительно уже активного эффекта: ignore, refresh, restart, stack, allow multiple или replace old.',
  Lifetime: 'Задает lifetime policy эффекта: независимый, привязанный к owner/session или до явного удаления.',
  EndOn: 'Добавляет условия завершения эффекта: смерть игрока, выход из мира, disconnect, смена dimension или завершение/отмена owner.',
  Ticks: 'Оборачивает число ticks для trigger cooldown/wait или для времени/duration в Echo Effects.',
  Seconds: 'Оборачивает секунды для времени/duration в Echo Effects; compiler переводит значение в ticks.',
  InfiniteDuration: 'Делает EffectResource бесконечным по длительности, пока его не снимут или не завершит lifetime/end condition.',
  Curve: 'Собирает кривую значений эффекта из Key(...): используется каналами Movement, Camera и Screen.',
  Color: 'Оборачивает hex цвет для Screen.Tint/Screen.Vignette в EffectResource.',
  Key: 'Задает точку кривой эффекта: время, значение и interpolation STEP/LINEAR/SMOOTH/CUBIC.',
  Movement: 'Effect DSL namespace для каналов движения игрока, сейчас поддерживает SpeedMultiplier(...).',
  SpeedMultiplier: 'Меняет множитель скорости движения игрока по кривой EffectResource.',
  Camera: 'Настраивает камеру сцены/диалога или effect-каналы камеры: FOV offset, roll и shake.',
  FovOffset: 'EffectResource канал камеры: изменяет offset поля зрения по кривой.',
  Roll: 'EffectResource канал камеры: наклоняет камеру по roll-кривой.',
  Shake: 'EffectResource канал камеры: задает амплитуду тряски по кривой.',
  Screen: 'Screen helpers управляют black overlay story UI и effect-каналами tint/vignette.',
  Tint: 'EffectResource канал экрана: накладывает цветовой tint с opacity-кривой.',
  Vignette: 'EffectResource канал экрана: накладывает vignette с opacity-кривой.',
  Sound: 'Effect DSL namespace для timeline sound events.',
  PlayAt: 'Добавляет sound event в EffectResource на указанном времени с sound id, volume и pitch.',
  VanillaEffect: 'Effect DSL namespace для vanilla Minecraft MobEffect events.',
  Apply: 'Внутри VanillaEffect применяет vanilla MobEffect к игроку, например minecraft:nausea.',
  ApplyToPlayer: 'Применяет Echo Effect resource к игроку; intensity по умолчанию 1.0.',
  RemoveFromPlayer: 'Снимает Echo Effect resource с игрока, optional fadeOut задается через Ticks(...) или Seconds(...).',
  WaitOnPlayer: 'Ждет завершения/состояния Echo Effect resource на игроке.',
  IsActiveOnPlayer: 'Проверяет, активен ли Echo Effect resource на игроке.',
  HasCompletedOnPlayer: 'Проверяет, завершился ли Echo Effect resource на игроке.',
  WearableResource: 'Ресурс wardrobe wearable: связывает item, wardrobe profile, слот, equipment slot, модель и portrait strips.',
  WardrobeProfileResource: 'Ресурс wardrobe profile: задает base skin, coverage policy и допустимые wardrobe slots.',
  OutfitVariantResource: 'Ресурс outfit variant: выбирает skin/worldSkin/faceSkin для комбинации face/top/bottom/feet.',
  Profile: 'В wardrobe ресурсах ссылается на profile id, к которому относится wearable или outfit variant.',
  Item: 'В WearableResource задает Minecraft item id, который представляет wearable.',
  Slot: 'В WearableResource задает wardrobe slot: FACE, TOP, BOTTOM или FEET.',
  EquipmentSlot: 'В WearableResource задает Minecraft equipment slot: HEAD, CHEST, LEGS или FEET.',
  Collection: 'В WearableResource помечает wearable коллекцией/группой для авторинга и runtime-фильтрации.',
  Model: 'В WearableResource задает model id для визуала wearable.',
  ModelPosition: 'В WearableResource задает смещение модели wearable по x/y/z.',
  ModelRotation: 'В WearableResource задает поворот модели wearable по x/y/z.',
  ModelScale: 'В WearableResource задает масштаб модели wearable по x/y/z.',
  PortraitTexture: 'В WearableResource задает texture id для portrait contribution.',
  PortraitStrip: 'Добавляет strip из texture в портрет wearable: source rect, target position, BASE/OUTER layer и optional offsets/insets.',
  BaseSkin: 'В WardrobeProfileResource задает базовый skin id профиля.',
  Coverage: 'В WardrobeProfileResource задает политику покрытия REPORT или STRICT.',
  Slots: 'В WardrobeProfileResource перечисляет разрешенные wardrobe slots.',
  Match: 'В OutfitVariantResource описывает, для какой комбинации face/top/bottom/feet подходит вариант.',
  WorldSkin: 'В OutfitVariantResource задает skin id для world model.',
  FaceSkin: 'В OutfitVariantResource задает skin id для face/portrait model.',
  FaceRig: 'Собирает FaceRig resource: bones, layers и blink/control definitions для лицевой анимации.',
  Rig: 'Helper в surface-классах facial animation: возвращает FaceRig id для поверхности.',
  Surface: 'Описывает поверхность facial animation: 3D/portrait target, rig и фазы enter/stay/exit.',
  Phase: 'Описывает фазу facial animation с длительностью, end behavior и tracks.',
  Enter: 'Фаза входа facial animation surface.',
  Stay: 'В зависимости от контекста: удерживает NPC на месте или задает stay-фазу facial animation surface.',
  Exit: 'В зависимости от контекста: animation exit variant или exit-фаза facial animation surface.',
  Layer: 'Встроенные animation layer символы или FaceRig layer definition с bone/texture/role/order.',
  Socket: 'Описывает socket attachment на кости body animation rig.',
  BlinkControl: 'Описывает blink/gaze control для FaceRig: travel костей, gaze диапазон и тайминги моргания.',
  BoneDelta: 'Задает delta transform одной face bone внутри FacePose.',
  ControlDelta: 'Задает значение одного face control внутри FacePose.',
  Textures: 'В high-level face DSL задает root path текстур лица.',
  Eyes: 'В high-level face DSL группирует Sclera/Pupil/Eyelid части глаз.',
  Brows: 'В high-level face DSL группирует брови и общий Pivot для них.',
  Brow: 'Часть брови в high-level face DSL: Left или Right.',
  Sclera: 'Часть глаза в high-level face DSL: белок Left или Right.',
  Pupil: 'Часть глаза в high-level face DSL: зрачок Left или Right.',
  Eyelid: 'Часть века в high-level face DSL: Upper или Lower.',
  Texture: 'В face DSL задает texture id для visual part.',
  TextureArea: 'В face DSL вырезает прямоугольник из texture: x, y, width, height.',
  Size: 'В face DSL задает размер visual part в пикселях.',
  Order: 'В face DSL задает порядок слоя/части.',
  Role: 'В face DSL задает LayerRole для visual part.',
  Parent: 'В face DSL задает parent bone для дополнительной кости.',
  Pivot: 'В face DSL задает pivot точки в 2D координатах.',
  Travel: 'В Blink block задает движение век при моргании.',
  Gaze: 'В Blink block задает gaze offset/диапазон.',
  Idle: 'Задает idle behavior NPC или включает idle blink в high-level face DSL.',
  Timing: 'В Blink block задает period/close/hold/open ticks для моргания.',
  PeriodTicks: 'В Blink block задает общий период моргания.',
  CloseTicks: 'В Blink block задает длительность закрытия век.',
  HoldTicks: 'В Blink block задает, сколько ticks веки остаются закрытыми.',
  OpenTicks: 'В Blink block задает длительность открытия век.',
  FacialAnimationSurface3d: 'Базовый класс surface для 3D facial animation. Используй его в class World : FacialAnimationSurface3d, а Rig/Enter/Stay/Exit задают rig и фазы поверхности.',
  FacialAnimationSurfacePortrait: 'Базовый класс surface для portrait facial animation. Используй его в class Portrait : FacialAnimationSurfacePortrait, а Rig/Enter/Stay/Exit задают rig и фазы портретной поверхности.',
  schema: 'CameraTrack text DSL: задает версию схемы трека камеры, например schema 2 для semantic mode/target/framing строк.',
  key: 'CameraTrack text DSL: добавляет keyframe камеры в формате key timeMillis x y z yaw pitch fov.',
  mode: 'CameraTrack text DSL: переключает режим управления камерой в момент времени, например mode 0 LOOK_AT_WITH_OFFSET.',
  target: 'CameraTrack text DSL: задает semantic target камеры: NONE, FIXED_POINT, PLAYER, NPC или BETWEEN с нужными параметрами.',
  framing: 'CameraTrack text DSL: задает framing offset камеры в формате framing timeMillis horizontal vertical.'
})) {
  METHOD_HOVER_SUMMARIES_RU.set(name, summary);
}

for (const [name, summary] of Object.entries({
  AnimationClip: 'Собирает нормализованный animation clip для body/face rig: tracks, schemaVersion, loop seam и authored speed.',
  AnimationProfile: 'Собирает профиль субъекта анимации: rig, graph, render mode, initial states, required channels и metadata.',
  AnimationRig: 'Собирает переиспользуемый body animation rig: kind, skin, bones и sockets.',
  Bone: 'Описывает timeline одной кости или части модели и принимает Frames(...).',
  Cost: 'Добавляет стоимость interaction action, например списание Money(...).',
  DurationTicks: 'Задает длительность камеры, overlay, video, sequence step или другого timed element в ticks.',
  Do: 'Оборачивает effect statements для scene steps и условных effect blocks.',
  Face: 'Задает face animation для dialogue line или speaker presentation.',
  FaceAnimation: 'Собирает face animation resource старого part-based формата: parts, frames и blink-настройки.',
  FacePose: 'Собирает FacePose resource для face rig из BoneDelta(...) и ControlDelta(...).',
  FacialAnimation: 'Собирает facial animation resource с rig, surfaces, tracks, duration и end behavior.',
  Field: 'Создает key/value поле для Obj(...) или flexible raw payload.',
  Fog: 'Показывает или скрывает fog/atmosphere effect в story flow.',
  Frame: 'Создает keyframe на tick/time и может цепочкой менять Position(...), Rotation(...) или Scale(...).',
  Frames: 'Прикрепляет keyframes к Bone/Part timeline.',
  Init: 'Метод инициализации ресурса: Id(...), Start(...), Alias(...), Speaker(...), Cutscene(...) и другие настройки, которые compiler подхватывает автоматически.',
  InteractionDialogues: 'Привязывает к speaker список диалогов, доступных при interaction.',
  Label: 'Задает прямой текст кнопки или action prompt.',
  LabelKey: 'Задает localization key для текста кнопки или action prompt.',
  LineAfterMove: 'Создает строку диалога, которая показывается только после завершения подходящего Npc.Move(...)/Npc.MoveTo(...). Обычный Line(...) может появиться, пока NPC еще идет.',
  Monologue: 'Показывает одиночную реплику вне обычного dialogue node flow.',
  NpcAnimationBinding: 'Связывает NPC definition или actor с AnimationProfile.',
  Schedule: 'Создает time-window расписание для Behavior/Schedules NPC.',
  Recovery: 'Настраивает recovery policy для animation lifecycle.',
  SetAnimation: 'Задает body animation для dialogue line.',
  SetAnimationFace: 'Задает face animation для dialogue line.',
  SetAnimationFaceClear: 'Очищает активную face animation у dialogue line.',
  SetFaceAnimation: 'Задает face animation для dialogue line; совместимая форма SetAnimationFace(...).',
  Source: 'Объявляет исходное state в animation graph.',
  Step: 'Создает raw scene sequence step, когда typed helper недостаточно.',
  Steps: 'Прикрепляет ordered sequence steps к Scene().',
  TextKey: 'Задает localization key для текста Line(...) или Choice(...).',
  TeleportPlayer: 'Телепортирует игрока в указанное dimension и позицию.',
  Track: 'Собирает animation track/keyframes для target или bone.',
  Channel: 'Объявляет animation lifecycle channel.',
  Finish: 'Создает terminal story graph node, где ветка завершается.',
  InitialState: 'Задает initial graph state для animation channel.',
  Main: 'Собирает main phase модульного animation lifecycle.',
  Meta: 'Добавляет metadata в resource builder payload.',
  WaitChunks: 'Ждет, пока chunks вокруг позиции будут загружены, прежде чем продолжить flow.',
  World: 'World command helpers: teleport, inventory removal и короткие navigation target команды.',
  ActionPerformed: 'Создает trigger, который срабатывает по stable ActionKey interaction action.',
  Actor: 'Добавляет actor binding в cutscene.',
  ApproachPlayer: 'Подводит NPC к игроку с pathing, timeout и optional follow-up dialogue.',
  CameraPath: 'Создает sampled camera path resource для cinematics и camera cues.',
  CameraPathCue: 'Ссылается на camera path на конкретном timeline tick внутри cutscene или scene flow.',
  Default: 'Помечает localization resource entry или timer choice как default.',
  Entries: 'Добавляет localization entries в localization resource.',
  Entry: 'Создает animation entry phase или одну localization key/value запись.',
  FadeIn: 'Задает fade-in timing для камеры или visual transition.',
  FadeOut: 'Задает fade-out timing для камеры или visual transition.',
  Focus: 'Задает точку фокуса камеры.',
  Hide: 'Скрывает visual overlay/effect, например fog или black screen.',
  RemovePlayerItem: 'Удаляет item stack из инвентаря игрока.',
  Show: 'Показывает visual/dialogue-style effect, например monologue, fog или black screen.',
  TalkDistance: 'Задает дистанцию, с которой игрок может начать разговор со speaker.',
  Vehicle: 'Vehicle helpers управляют spawn, trips, sequences, passengers и cinematic routing транспорта.',
  Quest: 'Quest helpers обновляют текст, stage, status, marker и уведомления квеста.',
  State: 'State helpers создают ссылки и runtime effects для story memory/state values.',
  Turn: 'Короткая форма Rotate(...): поворачивает NPC или player actor.',
  CircleArea: 'Короткая форма cylinder/circle для trigger area по center/radius и optional высоте.',
  Rig: 'В зависимости от контекста: задает animation rig id для clip/profile или возвращает FaceRig id в surface-классах.'
})) {
  METHOD_HOVER_SUMMARIES_RU.set(name, summary);
}

for (const [name, summary] of Object.entries({
  SeatZoneResource: 'Ресурс Echo Seating: описывает зону сидений в измерении, ее границы, именованные места и правила сопоставления игрока с сиденьем.',
  SeatZoneId: 'Типизированное поле для id зоны сидений. Можно объявить private SeatZoneId Zone = "namespace:path" и передать Zone в Id(...).',
  SeatSlotId: 'Типизированное поле для id конкретного места в SeatZone. Можно объявить private SeatSlotId Chair = "chair" и использовать Chair в Slot(...).',
  Id: 'Задает стабильный id ресурса. В TriggerResource, wardrobe/effects и SeatZoneResource используется внутри Init(); для SeatZone можно передать строку namespace:path или SeatZoneId field.',
  Init: 'Метод инициализации ресурса. В Dialogue/Trigger/Effect/Wardrobe/SeatZone ресурсах compiler читает отсюда настройки вроде Id(...), Start(...), Dimension(...), Area(...), Slot(...) и других helper-вызовов.',
  Dimension: 'Задает Minecraft dimension для TriggerResource или SeatZoneResource, например "minecraft:overworld".',
  Area: 'В TriggerResource создает область триггера; в SeatZoneResource задает границы зоны через Area(Box(Position(min), Position(max))).',
  Box: 'Создает прямоугольные границы SeatZoneResource из двух Position(...): минимального и максимального угла зоны.',
  Detect: 'Настраивает, как Seating определяет место игрока: по точке посадки или только по точно описанным Slot-точкам.',
  UnknownSeats: 'Настраивает, что делать с сиденьем внутри зоны, которое не совпало с именованным Slot: разрешить или отклонить.',
  Slot: 'В wardrobe задает слот носимой вещи; в SeatZoneResource добавляет именованное место с id и точкой Position(...).',
  MatchRadius: 'Задает радиус сопоставления точки посадки с ближайшим именованным Slot внутри SeatZoneResource.',
  Position: 'Задает координаты блока/точки. В SeatZoneResource используется для Box bounds и Slot anchor.',
  RIDING_ANCHOR: 'Значение Detect(...): Seating определяет сиденье по riding anchor игрока.',
  EXACT_SLOT_ONLY: 'Значение Detect(...): Seating принимает только точно описанные Slot anchors.',
  ALLOW: 'Значение UnknownSeats(...): разрешает неназванные сиденья внутри зоны.',
  REJECT: 'Значение UnknownSeats(...): отклоняет сиденья, которые не совпали с описанными Slot.'
})) {
  METHOD_HOVER_SUMMARIES_RU.set(name, summary);
}

for (const [name, summary] of Object.entries({
  Presentation: 'Задает отображаемые данные Echo Effect resource: nameKey, descriptionKey и icon для UI, команд и диагностики эффекта.',
  PlayerHudConfigResource: 'Ресурс Echo Player HUD: через Build() задает общий PlayerHud(...) и один или несколько Bar(...) для HUD игрока.',
  PlayerHud: 'Задает базовую конфигурацию HUD layout: id, referenceWidth/referenceHeight и нужно ли масштабировать координаты под текущее разрешение.',
  Bar: 'Добавляет HUD bar для метрики health, spark, fatigue или своего source: задает размер, позицию, anchor, fill, цвета, border и icon.',
  Description: 'Локальный helper в WeaponResource для текста description; обычно передается в Weapon(description: Description()).',
  Tooltip: 'Локальный helper в WeaponResource для массива tooltip-строк; обычно передается в Weapon(tooltip: Tooltip()).',
  Damage: 'Локальный helper в WeaponResource для базового урона оружия; обычно передается в Weapon(damage: Damage()).',
  AttackDamage: 'Локальный helper для альтернативного поля attackDamage в Weapon(...). Не используй вместе с damage в одном Weapon-вызове.',
  AttacksPerSecond: 'Локальный helper в WeaponResource для скорости атак оружия в ударах в секунду; передается в attacksPerSecond.',
  AttackSpeedModifier: 'Локальный helper для Minecraft attack speed modifier; compiler переводит его в attacksPerSecond.',
  Knockback: 'Локальный helper в WeaponResource для силы отбрасывания атаки; передается в Weapon(knockback: Knockback()).',
  Durability: 'Локальный helper в WeaponResource для прочности item; передается в Weapon(durability: Durability()).',
  Unbreakable: 'Локальный helper в WeaponResource, который делает weapon неразрушаемым и отключает расход durability.',
  Enchantability: 'Локальный helper в WeaponResource для enchantability item; влияет на зачарование оружия.',
  RepairIngredient: 'Локальный helper в WeaponResource для item id ингредиента ремонта; передается в repairIngredient.',
  Rarity: 'Локальный helper в WeaponResource для rarity оружия: COMMON, UNCOMMON, RARE или EPIC.',
  Glint: 'Локальный helper в WeaponResource, который включает постоянный enchantment glint на item.',
  FireResistant: 'Локальный helper в WeaponResource, который делает item устойчивым к огню и лаве.',
  HiddenFromCreative: 'Локальный helper в WeaponResource, который скрывает item из creative tab.',
  CreativeOrder: 'Локальный helper в WeaponResource для порядка item в creative tab.'
})) {
  METHOD_HOVER_SUMMARIES_RU.set(name, summary);
}

for (const [name, summary] of Object.entries({
  DialogueOverlayStyleConfig: 'Клиентский config-resource для компактного dialogue overlay; Build() возвращает DialogueOverlayStyle(...) с размерами, отступами, typewriter timing и цветами.',
  DialogueOverlayStyle: 'Задает стиль dialogue overlay: масштаб текста, задержки typewriter, anchor/offset, padding, размеры choice/visual блоков и цвета панели.',
  PlayerVitalsConfigResource: 'Ресурс конфигурации Echo Player Vitals; через Build() задает PlayerFatigue(...) для максимума усталости и порогов стадий.',
  PlayerFatigue: 'Настраивает fatigue систему игрока: maximum, пороги rested/tired/exhausted, movement multiplier и nausea для exhausted.',
  PortraitOverlayConfig: 'Клиентский config-resource для portrait overlay; Build() возвращает PortraitOverlay(...) для конкретного visual id.',
  PortraitOverlay: 'Настраивает portrait overlay: центр лица, offsets, facePixels, слои Layer(...) и idle Blink-блоки для портретной анимации.',
  Layer: 'В зависимости от контекста: animation layer символ, FaceRig layer definition или слой PortraitOverlay с place/offset/scale/visibility.',
  Blink: 'В зависимости от контекста: Sleep.Blink action, face DSL blink control или idle Blink-блок в PortraitOverlay.'
})) {
  METHOD_HOVER_SUMMARIES_RU.set(name, summary);
}

const RESERVED_DECLARATION_PREFIXES = new Set([
  'public',
  'private',
  'static',
  'override',
  'return',
  'if',
  'else',
  'namespace',
  'using',
  'class',
  'new'
]);

const FACE_DSL_DECLARATION_TYPES = new Set([
  'Bone',
  'Brow',
  'Brows',
  'Eyelid',
  'Pupil',
  'Sclera'
]);

const IGNORED_IDENTIFIERS = new Set([
  'true',
  'false',
  'null',
  'var',
  'new',
  'if',
  'else',
  'return',
  'namespace',
  'using',
  'class',
  'public',
  'private',
  'static',
  'override',
  'Event',
  'Vector3',
  'Point',
  'AreaEnter',
  'AreaEnterDelayed',
  'Manual',
  'ActionPerformed',
  'PlayerJoin',
  'Npc',
  'Dialogue',
  'Set',
  'This',
  'Player',
  'World',
  'Navigation',
  'Quest',
  'Scene',
  'Monologue',
  'Fog',
  'BlackScreen',
  'Video',
  'Effect',
  'If',
  'Elif',
  'Else',
  'Trigger',
  'Manual',
  'ActionPerformed',
  'InventoryHasItem',
  'InventoryMissing',
  'MobKilled',
  'Timer',
  'TimeReached',
  'PlayerRespawn',
  'DimensionEnter',
  'Do',
  'Say',
  'WaitChunks',
  'CameraPathCue',
  'FaceLayer',
  'Vec',
  'PlayerSleep',
  'PlayerProfile',
  'Weapon',
  'TriggerResource',
  'ACCOUNT',
  'CUSTOM',
  'RESOURCE',
  'CLASSIC',
  'SLIM',
  'GLOBAL',
  'SERVER',
  'PLAYER',
  'DIALOGUE_SESSION',
  'ONCE',
  'REPEAT',
  'SKIP_WHILE_RUNNING',
  'QUEUE_ONE',
  'COMMON',
  'UNCOMMON',
  'RARE',
  'EPIC',
  'HEAD',
  'CHEST',
  'LEGS',
  'FEET'
]);

const FLUENT_MEMBER_METHODS = new Map([
  ['Action', methods('Label', 'LabelKey', 'ActionKey', 'Priority', 'VisibleIf', 'If', 'VisibleWhen', 'Cost', 'UnavailableLabel', 'UnavailableLabelKey', 'SuccessLabel', 'SuccessLabelKey', 'Effects', 'FireOnce', 'Visible')],
  ['Animation', methods('Id', 'LengthTicks', 'Loop', 'End', 'ExitAnimationId', 'Interpolation', 'BlendMode', 'Editor', 'Bone', 'Bones', 'Gun', 'Items', 'Ik')],
  ['Behavior', methods('LookAtPlayers', 'RecoveryTicks', 'Idle', 'Schedules')],
  ['Bone', methods('Frames', 'Ik')],
  ['Camera', methods('Preset', 'PresetId', 'PositionSource', 'Position', 'Focus', 'LockMouse', 'LockControls', 'FadeIn', 'FadeOut', 'FadeInTicks', 'FadeOutTicks', 'BlendTicks', 'DurationTicks', 'TargetYaw', 'TargetPitch')],
  ['CameraPath', methods('Id', 'Format', 'LengthTicks', 'Sample', 'Samples')],
  ['Choice', methods('Text', 'Label', 'Key', 'TextKey', 'If', 'When', 'Next', 'Goto', 'Effects')],
  ['Cutscene', methods('Actor', 'Input', 'Release', 'SuppressHud', 'AllowDialogueInput', 'LogTag', 'Camera', 'CameraPath')],
  ['Dialogue', methods('Start', 'Alias', 'Aliases', 'Speaker', 'Nodes', 'StartEffects', 'CompleteEffects', 'Cutscene')],
  ['Event', methods('Id', 'Trigger', 'RunIf', 'If', 'When', 'Effects', 'FireOnce', 'CooldownTicks')],
  ['FaceAnimation', methods('Id', 'LengthTicks', 'LengthSeconds', 'Loop', 'End', 'Interpolation', 'BlinkTravelY', 'Part', 'Parts')],
  ['Line', methods('Id', 'Key', 'TextKey', 'Delay', 'DelayTicks', 'Thought', 'Voice', 'Portrait', 'Fx', 'NameColor', 'Anim', 'Animation', 'SetAnimation', 'Face', 'SetAnimationFace', 'SetFaceAnimation', 'SetAnimationFaceClear', 'FaceStop', 'StopFace', 'StopFaceAnimation', 'Path', 'Effects')],
  ['LineAfterMove', methods('Id', 'Key', 'TextKey', 'Delay', 'DelayTicks', 'Thought', 'Voice', 'Portrait', 'Fx', 'NameColor', 'Anim', 'Animation', 'SetAnimation', 'Face', 'SetAnimationFace', 'SetFaceAnimation', 'SetAnimationFaceClear', 'FaceStop', 'StopFace', 'StopFaceAnimation', 'Path', 'Effects')],
  ['Localization', methods('Locale', 'Default', 'Prefix', 'Entry', 'Entries')],
  ['Node', methods('Kind', 'Lines', 'Next', 'Goto')],
  ['Part', methods('Frame', 'Frames')],
  ['Scene', methods('Id', 'LockPlayer', 'Skippable', 'Replayable', 'Camera', 'OpeningCamera', 'Cutscene', 'Alias', 'SpeakerAlias', 'Aliases', 'SpeakerAliases', 'Start', 'StartEffects', 'Complete', 'CompleteEffects', 'Abort', 'AbortEffects', 'Step', 'Steps')],
  ['Schedule', methods('State', 'Animation', 'Route')],
  ['Speaker', methods('DisplayName', 'DisplayNameKey', 'NameColor', 'Portrait', 'InteractionDialogues', 'TalkDistance', 'InteractionDistance', 'DialogueDistance', 'Behavior', 'Actions')],
  ['StoryIndex', methods('Speakers', 'Npcs', 'Dialogues', 'Scenes', 'Events', 'Animations', 'FaceAnimations', 'CameraPaths', 'Localization')],
  ['Timer', methods('OnTimeout', 'DefaultChoice')]
]);

const NAMESPACE_MEMBER_METHODS = new Map([
  ['Blackspace', methods('Create')],
  ['BlackScreen', methods('Show', 'Hide')],
  ['Cutscene', methods('Start', 'Play', 'Wait', 'WaitPrepared', 'Stop')],
  ['Dialogue', methods('Start', 'Load', 'GoTo')],
  ['Debug', methods('Mark')],
  ['Environment', methods('Set')],
  ['Fog', methods('Show', 'Hide')],
  ['Monologue', methods('Show')],
  ['Navigation', methods('SetTarget', 'ClearTarget')],
  ['Npc', methods('Spawn', 'SpawnAt', 'SleepAt', 'WakeUp', 'SetForm', 'SpawnNearPlayer', 'Despawn', 'Stop', 'Interact', 'StopInteraction', 'StandUp', 'MoveTo', 'Move', 'WalkTo', 'Path', 'MoveToPlayer', 'ApproachPlayer', 'WalkToPlayer', 'Stay', 'Rotate', 'Turn', 'UseBlock', 'InteractBlock', 'OpenDoor', 'CloseDoor', 'ToggleDoor', 'UseDoor', 'LookAtPlayers', 'ClearLookAtPlayers', 'HoldItem', 'ClearHeldItem', 'PlayAnimation', 'StopAnimation', 'PlayFaceAnimation', 'StopFaceAnimation', 'EnterVehicle', 'EnterNearestVehicle', 'EnterVehicleAt', 'LeaveVehicle')],
  ['Player', methods('GiveItem', 'Pose', 'SpawnAt', 'SetHotbarItem', 'SelectHotbarSlot', 'SitAt', 'Rotate', 'LookAt', 'Move', 'Stop', 'Interact', 'StandUp', 'StopUsingItem', 'IsMoving', 'IsSitting', 'SelectedHotbarSlot', 'IsUsingItem', 'SetFatigue', 'AddFatigue', 'ReduceFatigue', 'RestoreFatigue', 'Teleport')],
  ['PlayerSleep', methods('Confirmed', 'EnteredDream', 'Returned', 'Completed', 'Failed')],
  ['Quest', methods('Update')],
  ['Scene', methods('Start')],
  ['Screen', methods('SetBlack')],
  ['Sleep', methods('Arm', 'Disarm', 'CloseEyes', 'WaitUntilClosed', 'EnterDream', 'OpenEyes', 'WaitUntilOpen', 'Wait', 'Blink', 'WaitSeconds', 'ReturnToBed', 'CompleteNight', 'Complete', 'Cancel', 'IsActive', 'IsArmed', 'IsEyesClosed')],
  ['State', methods('Memory', 'Ref', 'Set', 'Remove', 'Clear', 'Add', 'Adjust', 'Increment', 'Decrement')],
    ['Story', methods('Set', 'Load')],
  ['This', methods('HoldItem', 'ClearHeldItem', 'PlayAnimation', 'StopAnimation', 'Rotate', 'Turn')],
  ['Vehicle', methods('EnsureSpawn', 'Trip', 'Sequence')],
  ['Video', methods('Play')],
  ['World', methods('TeleportPlayer', 'RemovePlayerItem', 'SetNavigationTarget', 'ClearNavigationTarget')]
]);

const STATIC_NAMESPACE_MEMBER_NAMES = new Map(
  Object.entries(STATIC_NAMESPACE_MEMBERS)
    .map(([namespace, members]) => [
      namespace,
      new Set(members.map(([member]) => member))
    ])
);

const ECHOSCRIPT_TYPE_TO_BUILDER = new Map([
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
  ['DialogueResourceDef', 'Dialogue'],
  ['EventDef', 'Event'],
  ['FaceAnimationDef', 'FaceAnimation'],
  ['FacePoseDef', 'FacePose'],
  ['FacialAnimationDef', 'FacialAnimation'],
  ['LineDef', 'Line'],
  ['LocalizationDef', 'Localization'],
  ['NodeDef', 'Node'],
  ['NpcAnimationBindingDef', 'NpcAnimationBinding'],
  ['NpcResourceDef', 'Npc'],
  ['ScheduleDef', 'Schedule'],
  ['SceneDef', 'Scene'],
  ['SpeakerDef', 'Speaker'],
  ['StoryResourceDef', 'Story'],
  ['StoryIndexDef', 'StoryIndex'],
  ['TimerDef', 'Timer']
]);

function methods(...names) {
  return new Set(names);
}

function analyzeEchoScript(text) {
  const diagnostics = [];
  const stack = [];
  const lines = text.split(/\r?\n/);
  const state = {
    inBlockComment: false,
    inString: '',
    escaped: false
  };

  for (let lineIndex = 0; lineIndex < lines.length; lineIndex += 1) {
    const line = lines[lineIndex];
    scanLine(line, lineIndex, state, stack, diagnostics);
    lintLine(line, lineIndex, diagnostics);
  }
  lintDocument(text, lines, diagnostics);
  lintUnknownMemberCalls(lines, diagnostics);
  lintVariables(lines, diagnostics);

  if (state.inString) {
    diagnostics.push(issue(lines.length - 1, Math.max(0, lines[lines.length - 1].length - 1), 1, 'Unterminated string literal.', 'error'));
  }
  if (state.inBlockComment) {
    diagnostics.push(issue(lines.length - 1, 0, 1, 'Unterminated block comment.', 'error'));
  }
  while (stack.length > 0) {
    const open = stack.pop();
    diagnostics.push(issue(open.line, open.column, 1, `Unclosed \`${open.char}\`.`, 'error'));
  }
  return diagnostics;
}

function lintLine(line, lineIndex, diagnostics) {
  lintNamingConvention(line, lineIndex, diagnostics);
  addPattern(line, lineIndex, diagnostics, /\.RunIf\s*\(/g, 'EchoScript 0.4 uses story-runtime `if (...) { ... }` blocks instead of `.RunIf(...)`.', 'error');
  addPattern(line, lineIndex, diagnostics, /\.VisibleIf\s*\(/g, 'EchoScript 0.4 uses story-runtime `if (...) { ... }` blocks instead of `.VisibleIf(...)`.', 'error');
  addPattern(line, lineIndex, diagnostics, /\.If\s*\(/g, 'Do not use member `.If(...)`; use `if (...) { ... }` blocks or choice `if:`.', 'error');
  addPattern(line, lineIndex, diagnostics, /\.When\s*\(/g, 'Do not use `.When(...)`; use `if (...) { ... }` blocks.', 'error');
  addPattern(line, lineIndex, diagnostics, /\.VisibleWhen\s*\(/g, 'Do not use `.VisibleWhen(...)`; use `if (...) { ... }` blocks.', 'error');
  addPattern(line, lineIndex, diagnostics, /\.(?:Line|LineAfterMove)\s*\(/g, '[ESL0420] Do not use leading-dot syntax for context-local dialogue line methods. Use `Line(...)` or `LineAfterMove(...)`.', 'warning');
  addPattern(line, lineIndex, diagnostics, /\.Choice\s*\(/g, '[ESL0421] Do not use leading-dot syntax for context-local method `Choice`. Use `Choice(...)`.', 'warning');
  addPattern(line, lineIndex, diagnostics, /\.NextNode\s*\(/g, '[ESL0422] Do not use leading-dot syntax for context-local method `NextNode`. Use `NextNode(...)`.', 'warning');
  addPattern(line, lineIndex, diagnostics, /(?:^|\s)\.(?:Alias|Speaker|Nodes)\s*\(/g, '[ESL0430] Dialogue config belongs in `Init()` and `Nodes()` methods, not in the Dialogue() chain.', 'warning');
  addPattern(line, lineIndex, diagnostics, /(?:^|\s)\.(?:StartEffects|CompleteEffects)\s*\(/g, '[ESL0431] Lifecycle effects must be separate `private Effect[] StartEffects()` / `CompleteEffects()` methods.', 'warning');
  addPattern(line, lineIndex, diagnostics, /\bwhen\b/g, 'Do not use `when` in EchoScript source; use `if`.', 'error');
  addPattern(line, lineIndex, diagnostics, /\belif\b/g, 'Do not use `elif`; EchoScript 0.4 uses C#-style `else if`.', 'error');
  addPattern(line, lineIndex, diagnostics, /\bPoint\s*\(/g, '[ESL0306] `Point(...)` is deprecated. Use `new Vector3(...)`.', 'warning');
  addPattern(line, lineIndex, diagnostics, /\bthenDialogueId\s*:/g, '[ESL0308] Use `thenDialogue` with a typed DialogueRef instead of `thenDialogueId`.', 'warning');
  addPattern(line, lineIndex, diagnostics, /\bdimensionId\s*:/g, '[ESL0305] Area triggers should pass a typed DimensionRef argument instead of `dimensionId:`.', 'warning');
  addPattern(line, lineIndex, diagnostics, /\b(?:AreaEnter|AreaEnterDelayed|Dialogue\.Start|Npc\.(?:ApproachPlayer|MoveToPlayer|WalkToPlayer|Move|MoveTo|WalkTo)|Vehicle\.Trip|Vehicle\.Sequence)\s*\(\s*["']/g, '[ESL0307] Raw string id in story logic. Use a typed symbol.', 'warning');
  addPattern(line, lineIndex, diagnostics, /\bScene\.Start\s*\(\s*["']/g, '[ESL0503] Raw scene id string in Scene.Start. Use a SceneRef symbol.', 'warning');
  addPattern(line, lineIndex, diagnostics, /\bNpc\.(?:Spawn|SpawnAt|SleepAt|Despawn|Stop|Interact|StopInteraction|StandUp|Stay|Path|Rotate|Turn|UseBlock|InteractBlock|OpenDoor|CloseDoor|ToggleDoor|UseDoor|LookAtPlayers|ClearLookAtPlayers|HoldItem|ClearHeldItem|PlayAnimation|StopAnimation|PlayFaceAnimation|StopFaceAnimation|EnterVehicle|EnterNearestVehicle|EnterVehicleAt|LeaveVehicle)\s*\(\s*["']/g, '[ESL0504] Raw NPC id string in Npc helpers. Use a typed symbol.', 'warning');
  addPattern(line, lineIndex, diagnostics, /\bNpc\.Spawn\s*\(\s*[^,\n]+,\s*["']/g, '[ESL0505] Raw dimension id string in Npc.Spawn. Use a DimensionRef symbol.', 'warning');

  for (const match of line.matchAll(/\bEvent\s*\(/g)) {
    const before = line.slice(0, match.index || 0);
    if (!/\bnew\s+$/.test(before)) {
      diagnostics.push(issue(lineIndex, match.index || 0, Math.max(1, match[0].length), '[ESL0304] Prefer `new Event()` instead of `Event()`.', 'warning'));
    }
  }

  if (/\.\s*(\/\/.*)?$/.test(line)) {
    diagnostics.push(issue(lineIndex, line.indexOf('.'), 1, 'Incomplete fluent/member access; add a method or property after `.`.', 'warning'));
  }
  const ifMatch = /^\s*if\b\s*(?!\s*(?:\(|:))/.exec(line);
  if (ifMatch) {
    diagnostics.push(issue(lineIndex, ifMatch.index, 2, '`if` conditions must use parentheses: `if (condition)`.', 'error'));
  }
  const elseIfMatch = /^\s*else\s+if\b\s*(?!\s*\()/.exec(line);
  if (elseIfMatch) {
    diagnostics.push(issue(lineIndex, elseIfMatch.index, 7, '`else if` conditions must use parentheses: `else if (condition)`.', 'error'));
  }
}

function lintNamingConvention(line, lineIndex, diagnostics) {
  const stripped = stripStringsAndComments(line);
  const match = /^(\s*)(?:(public|private)\s+)?(?:static\s+)?(?:(var)|((?:[A-Za-z_][A-Za-z0-9_]*\.)*[A-Za-z_][A-Za-z0-9_]*(?:\[\])?))\s+([A-Za-z_][A-Za-z0-9_]*)\s*(?==|;)/.exec(stripped);
  if (!match) {
    return;
  }
  const visibility = match[2] || '';
  const typeName = match[3] || match[4] || '';
  const name = match[5] || '';
  if (!typeName || RESERVED_DECLARATION_PREFIXES.has(typeName)) {
    return;
  }
  if (isFaceDslDeclarationType(typeName)) {
    return;
  }
  const column = match[0].lastIndexOf(name);
  if (visibility === 'public') {
    if (!/^[A-Z][A-Za-z0-9]*$/.test(name)) {
      diagnostics.push(issue(lineIndex, column, name.length, `[ESL0441] Public fields must use PascalCase: \`${name}\`.`, 'warning'));
    }
    return;
  }
  if (isResourceIdConstantDeclaration(typeName, name)) {
    return;
  }
  if (!/^[a-z][A-Za-z0-9]*$/.test(name)) {
    diagnostics.push(issue(lineIndex, column, name.length, `[ESL0440] Private fields and local variables must use camelCase: \`${name}\`.`, 'warning'));
  }
}

function isFaceDslDeclarationType(typeName) {
  const simple = simpleTypeName(typeName);
  return FACE_DSL_DECLARATION_TYPES.has(simple);
}

function isResourceIdConstantDeclaration(typeName, name) {
  const simple = simpleTypeName(typeName);
  return /Id$/.test(simple) && /^[A-Z][A-Za-z0-9]*$/.test(name);
}

function simpleTypeName(typeName) {
  const normalized = String(typeName || '').replace(/\[\]$/, '');
  return normalized.includes('.')
    ? normalized.slice(normalized.lastIndexOf('.') + 1)
    : normalized;
}

function lintDocument(text, lines, diagnostics) {
  lintTerminatedStatements(lines, diagnostics);
  lintAliasUsage(lines, diagnostics);
  lintDialogueConfigChains(text, lines, diagnostics);

  for (const match of text.matchAll(/\bAreaEnterDelayed\s*\(([\s\S]*?)\)/g)) {
    const body = match[1] || '';
    if (/dimensionId\s*:/.test(body) || /\bPoint\s*\(/.test(body) || /^\s*["']/.test(body)) {
      const position = positionFromOffset(lines, match.index || 0);
      diagnostics.push(issue(position.line, position.column, 'AreaEnterDelayed'.length, '[ESL0305] `AreaEnterDelayed` should use typed flag, DimensionRef, and Vector3 min/max arguments.', 'warning'));
    }
  }

  for (const match of text.matchAll(/\bNpc\.(?:ApproachPlayer|MoveToPlayer|WalkToPlayer)\s*\(([\s\S]*?)\)/g)) {
    const body = match[1] || '';
    const explicitDefaults = ['speed', 'repathTicks', 'timeoutTicks', 'lookAtPlayer', 'warpWhenUnseen', 'warpDistance', 'warpSearchRadius']
      .filter(name => new RegExp(`\\b${name}\\s*:`).test(body));
    if (explicitDefaults.length >= 4) {
      const position = positionFromOffset(lines, match.index || 0);
      diagnostics.push(issue(position.line, position.column, match[0].indexOf('('), '[ESL0309] NPC approach helper repeats default movement arguments. Prefer defaults or a movement profile.', 'information'));
    }
  }
}

function lintDialogueConfigChains(text, lines, diagnostics) {
  for (const match of text.matchAll(/\bDialogue\s*\(\s*\)(?:(?!;)[\s\S])*?\.(Start|Alias|Speaker|Nodes)\s*\(/g)) {
    const method = match[1];
    const offset = (match.index || 0) + match[0].lastIndexOf(method);
    const position = positionFromOffset(lines, offset);
    diagnostics.push(issue(position.line, position.column, method.length, '[ESL0430] Dialogue config belongs in `Init()` and `Nodes()` methods, not in the Dialogue() chain.', 'warning'));
  }
}

function lintUnknownMemberCalls(lines, diagnostics) {
  const methodBuilders = collectLocalMethodReturnBuilders(lines.join('\n'));
  const builderVariables = collectBuilderVariables(lines, methodBuilders);
  for (let lineIndex = 0; lineIndex < lines.length; lineIndex += 1) {
    const line = maskStringsAndComments(lines[lineIndex]);

    for (const match of line.matchAll(/\b([A-Za-z_][A-Za-z0-9_]*)\s*\.\s*([A-Za-z_][A-Za-z0-9_]*)\s*\(/g)) {
      const target = match[1];
      const member = match[2];
      const staticMembers = STATIC_NAMESPACE_MEMBER_NAMES.get(target);
      if (staticMembers) {
        if (!staticMembers.has(member)) {
          const column = (match.index || 0) + match[0].lastIndexOf(member);
          diagnostics.push(issue(lineIndex, column, member.length, `[ESL0610] Unknown EchoScript static symbol \`${target}.${member}\`.`, 'error'));
        }
        continue;
      }
      const namespaceMethods = NAMESPACE_MEMBER_METHODS.get(target);
      if (namespaceMethods) {
        if (!namespaceMethods.has(member)) {
          const column = (match.index || 0) + match[0].lastIndexOf(member);
          diagnostics.push(issue(lineIndex, column, member.length, `[ESL0601] Unknown EchoScript helper \`${target}.${member}(...)\`.`, 'error'));
        }
        continue;
      }
      const builder = builderVariables.get(target);
      const builderMethods = builder ? FLUENT_MEMBER_METHODS.get(builder) : null;
      if (builderMethods && !builderMethods.has(member)) {
        const column = (match.index || 0) + match[0].lastIndexOf(member);
        diagnostics.push(issue(lineIndex, column, member.length, `[ESL0602] Unknown \`${builder}()\` member \`.${member}(...)\`.`, 'error'));
      }
    }

    for (const match of line.matchAll(/\b([A-Za-z_][A-Za-z0-9_]*)\s*\.\s*([A-Za-z_][A-Za-z0-9_]*)\b/g)) {
      const target = match[1];
      const member = match[2];
      const staticMembers = STATIC_NAMESPACE_MEMBER_NAMES.get(target);
      if (!staticMembers || staticMembers.has(member)) {
        continue;
      }
      const nextChar = line[(match.index || 0) + match[0].length] || '';
      if (nextChar === '(') {
        continue;
      }
      const column = (match.index || 0) + match[0].lastIndexOf(member);
      diagnostics.push(issue(lineIndex, column, member.length, `[ESL0610] Unknown EchoScript static symbol \`${target}.${member}\`.`, 'error'));
    }

    for (const match of line.matchAll(/(^|[^\w.])\.([A-Za-z_][A-Za-z0-9_]*)\s*\(/g)) {
      const member = match[2];
      const column = (match.index || 0) + match[1].length;
      const builder = builderFromSameLineBefore(line, column, methodBuilders) || inferFluentBuilder(lines, lineIndex, methodBuilders);
      const builderMethods = builder ? FLUENT_MEMBER_METHODS.get(builder) : null;
      if (builderMethods && !builderMethods.has(member)) {
        diagnostics.push(issue(lineIndex, column + 1, member.length, `[ESL0600] Unknown \`${builder}()\` fluent method \`.${member}(...)\`.`, 'error'));
      }
    }
  }
}

function collectBuilderVariables(lines, methodBuilders = new Map()) {
  const result = new Map();
  for (const rawLine of lines) {
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
    const initializerBuilder = FLUENT_MEMBER_METHODS.has(initializerName)
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
  return ECHOSCRIPT_TYPE_TO_BUILDER.get(simple) || '';
}

function inferFluentBuilder(lines, lineIndex, methodBuilders = new Map()) {
  const currentIndent = leadingWhitespaceLength(lines[lineIndex] || '');
  for (let index = lineIndex; index >= 0 && lineIndex - index <= 40; index -= 1) {
    const line = maskStringsAndComments(lines[index]);
    const trimmed = line.trim();
    if (!trimmed) {
      continue;
    }
    const indent = leadingWhitespaceLength(line);
    if (index !== lineIndex && indent > currentIndent) {
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
      return null;
    }
  }
  return null;
}

function builderFromSameLineBefore(line, column, methodBuilders = new Map()) {
  return lastBuilderCallName(line.slice(0, Math.max(0, column)), methodBuilders);
}

function lastBuilderCallName(fragment, methodBuilders = new Map()) {
  let result = null;
  for (const match of String(fragment || '').matchAll(/\b(?:new\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*\(/g)) {
    if (FLUENT_MEMBER_METHODS.has(match[1])) {
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

function lintTerminatedStatements(lines, diagnostics) {
  let active = null;
  for (let lineIndex = 0; lineIndex < lines.length; lineIndex += 1) {
    const rawLine = lines[lineIndex];
    const stripped = stripStringsAndComments(rawLine);
    const trimmed = stripped.trim();
    if (!active) {
      const start = statementStart(stripped);
      if (!start) {
        continue;
      }
      active = {
        ...start,
        line: lineIndex,
        depth: 0
      };
    } else if (/^}/.test(trimmed)) {
      diagnostics.push(issue(active.line, active.column, active.length, active.message, 'error'));
      active = null;
      continue;
    }

    if (!active) {
      continue;
    }

    const fragment = lineIndex === active.line ? stripped.slice(active.column) : stripped;
    if (statementTerminates(fragment, active)) {
      active = null;
    }
  }

  if (active) {
    diagnostics.push(issue(active.line, active.column, active.length, active.message, 'error'));
  }
}

function statementStart(line) {
  const returnMatch = /^(\s*)return\b/.exec(line);
  if (returnMatch) {
    return {
      type: 'return',
      line: -1,
      column: returnMatch[1].length,
      length: 'return'.length,
      message: 'Return statements must end with `;`.'
    };
  }
  const variableMatch = /^(\s*)(?:var|(?:[A-Za-z_][A-Za-z0-9_]*\.)*[A-Za-z_][A-Za-z0-9_]*(?:\[\])?)\s+[A-Za-z_][A-Za-z0-9_]*\s*=/.exec(line);
  if (variableMatch && !/^\s*(?:public|private|static|override)\b/.test(line)) {
    return {
      type: 'variable',
      line: -1,
      column: variableMatch[1].length,
      length: Math.max(1, variableMatch[0].trimEnd().length - variableMatch[1].length),
      message: 'Variable declarations must end with `;`.'
    };
  }
  return null;
}

function statementTerminates(fragment, active) {
  for (const char of fragment) {
    if (char === '(' || char === '[') {
      active.depth += 1;
    } else if (char === ')' || char === ']') {
      active.depth = Math.max(0, active.depth - 1);
    } else if (char === ';' && active.depth === 0) {
      return true;
    }
  }
  return false;
}

function lintAliasUsage(lines, diagnostics) {
  const aliases = new Map();
  const used = new Set();
  for (let lineIndex = 0; lineIndex < lines.length; lineIndex += 1) {
    const line = stripLineComment(lines[lineIndex]);
    for (const match of line.matchAll(/(?:^|[^\w.])Alias\s*\(\s*["']([^"']+)["']\s*,/g)) {
      const alias = match[1];
      if (aliases.has(alias)) {
        diagnostics.push(issue(lineIndex, (match.index || 0) + match[0].indexOf(alias), alias.length, `[ESL0508] Duplicate dialogue alias \`${alias}\`.`, 'warning'));
      } else {
        aliases.set(alias, { line: lineIndex, column: (match.index || 0) + match[0].indexOf(alias) });
      }
    }
    const explicitLine = /\bLine\s*\(\s*["']([^"']+)["']\s*,/.exec(lines[lineIndex]);
    if (explicitLine && aliases.has(explicitLine[1])) {
      used.add(explicitLine[1]);
    }
  }
}

function stripLineComment(line) {
  let inString = '';
  let escaped = false;
  for (let index = 0; index < line.length; index += 1) {
    const char = line[index];
    const next = index + 1 < line.length ? line[index + 1] : '';
    if (inString) {
      if (escaped) {
        escaped = false;
      } else if (char === '\\') {
        escaped = true;
      } else if (char === inString) {
        inString = '';
      }
      continue;
    }
    if (char === '"' || char === "'") {
      inString = char;
      continue;
    }
    if (char === '/' && next === '/') {
      return line.slice(0, index);
    }
  }
  return line;
}

function lintVariables(lines, diagnostics) {
  const root = createScope(null);
  const stack = [root];
  const lineScopes = [];
  const declarationsByLine = new Map();

  for (let lineIndex = 0; lineIndex < lines.length; lineIndex += 1) {
    const stripped = stripStringsAndComments(lines[lineIndex]);
    const scope = stack[stack.length - 1];
    lineScopes[lineIndex] = scope;
    const declaration = declarationOnLine(stripped);
    if (declaration) {
      declarationsByLine.set(lineIndex, declaration);
      if (scope.declarations.has(declaration.name)) {
        diagnostics.push(issue(lineIndex, declaration.column, declaration.name.length, `[ESL0302] Variable \`${declaration.name}\` shadows another variable in the same scope.`, 'warning'));
      } else if (findDeclaredScope(scope.parent, declaration.name)) {
        diagnostics.push(issue(lineIndex, declaration.column, declaration.name.length, `[ESL0302] Variable \`${declaration.name}\` shadows another variable.`, 'warning'));
      }
      scope.declarations.set(declaration.name, declaration);
    }
    updateScopeStack(stripped, stack);
  }

  for (let lineIndex = 0; lineIndex < lines.length; lineIndex += 1) {
    const stripped = stripStringsAndComments(lines[lineIndex]);
    const scope = lineScopes[lineIndex] || root;
    const declaration = declarationsByLine.get(lineIndex);
    const scanStart = declaration ? Math.max(0, stripped.indexOf('=', declaration.column) + 1) : 0;
    const fragment = stripped.slice(scanStart);
    for (const match of fragment.matchAll(/\b[A-Za-z_][A-Za-z0-9_]*\b/g)) {
      const column = scanStart + (match.index || 0);
      const name = match[0];
      if (isIgnoredIdentifier(name) || (declaration && column === declaration.column)) {
        continue;
      }
      const declaredScope = findDeclaredScope(scope, name);
      if (declaredScope && !findDefinedScope(scope, name)) {
        diagnostics.push(issue(lineIndex, column, name.length, `[ESL0301] Variable \`${name}\` is used before declaration.`, 'error'));
      }
    }
    if (declaration) {
      scope.defined.set(declaration.name, declaration);
    }
  }
}

function createScope(parent) {
  return {
    parent,
    declarations: new Map(),
    defined: new Map()
  };
}

function updateScopeStack(line, stack) {
  for (const char of line) {
    if (char === '{') {
      stack.push(createScope(stack[stack.length - 1]));
    } else if (char === '}' && stack.length > 1) {
      stack.pop();
    }
  }
}

function declarationOnLine(line) {
  const match = /^(\s*)(?:(var)|((?:[A-Za-z_][A-Za-z0-9_]*\.)*[A-Za-z_][A-Za-z0-9_]*(?:\[\])?))\s+([A-Za-z_][A-Za-z0-9_]*)\s*=/.exec(line);
  if (!match) {
    return null;
  }
  const typeName = match[2] || match[3] || '';
  const name = match[4];
  if (!typeName || RESERVED_DECLARATION_PREFIXES.has(typeName)) {
    return null;
  }
  return {
    typeName,
    name,
    column: match[0].lastIndexOf(name)
  };
}

function findDeclaredScope(scope, name) {
  let current = scope;
  while (current) {
    if (current.declarations.has(name)) {
      return current;
    }
    current = current.parent;
  }
  return null;
}

function findDefinedScope(scope, name) {
  let current = scope;
  while (current) {
    if (current.defined.has(name)) {
      return current;
    }
    current = current.parent;
  }
  return null;
}

function isIgnoredIdentifier(name) {
  return IGNORED_IDENTIFIERS.has(name) || /^[A-Z][A-Za-z0-9_]*(?:Resource|Def|Ref|Builder)?$/.test(name);
}

function addDocumentPattern(text, lines, pattern, factory, diagnostics) {
  for (const match of text.matchAll(pattern)) {
    const detail = factory(match);
    const position = positionFromOffset(lines, detail.offset);
    diagnostics.push(issue(position.line, position.column, detail.length, detail.message, detail.severity));
  }
}

function positionFromOffset(lines, offset) {
  let remaining = Math.max(0, offset);
  for (let line = 0; line < lines.length; line += 1) {
    const lineLength = lines[line].length;
    if (remaining <= lineLength) {
      return { line, column: remaining };
    }
    remaining -= lineLength + 1;
  }
  return { line: Math.max(0, lines.length - 1), column: Math.max(0, lines[lines.length - 1]?.length || 0) };
}

function scanLine(line, lineIndex, state, stack, diagnostics) {
  for (let index = 0; index < line.length; index += 1) {
    const char = line[index];
    const next = index + 1 < line.length ? line[index + 1] : '';
    if (state.inString) {
      if (state.escaped) {
        state.escaped = false;
      } else if (char === '\\') {
        state.escaped = true;
      } else if (char === state.inString) {
        state.inString = '';
      }
      continue;
    }
    if (state.inBlockComment) {
      if (char === '*' && next === '/') {
        state.inBlockComment = false;
        index += 1;
      }
      continue;
    }
    if (char === '/' && next === '/') {
      break;
    }
    if (char === '/' && next === '*') {
      state.inBlockComment = true;
      index += 1;
      continue;
    }
    if (char === '"' || char === "'") {
      state.inString = char;
      continue;
    }
    if ('({['.includes(char)) {
      stack.push({ char, line: lineIndex, column: index });
      continue;
    }
    if (')}]'.includes(char)) {
      const expected = matchingOpen(char);
      const open = stack.pop();
      if (!open || open.char !== expected) {
        diagnostics.push(issue(lineIndex, index, 1, `Unmatched \`${char}\`.`, 'error'));
      }
    }
  }
}

function matchingOpen(close) {
  return close === ')' ? '(' : close === '}' ? '{' : '[';
}

function addPattern(line, lineIndex, diagnostics, pattern, message, severity) {
  for (const match of line.matchAll(pattern)) {
    diagnostics.push(issue(lineIndex, match.index || 0, Math.max(1, match[0].length), message, severity));
  }
}

function issue(line, column, length, message, severity) {
  return { line, column, length, message, severity };
}

function formatEchoScript(text, options = {}) {
  text = collapseLineCalls(removeContextLocalLeadingDots(preferFileScopedNamespace(text)));
  const indentText = options.insertSpaces ? ' '.repeat(options.tabSize || 4) : '\t';
  const lines = text.split(/\r?\n/);
  const delimiterStack = [];
  const chains = [];
  const formatted = [];
  for (const rawLine of lines) {
    const trimmed = rawLine.trim();
    if (!trimmed) {
      formatted.push('');
      continue;
    }
    pruneChains(chains, delimiterStack.length);
    const leadingClose = startsWithClosingDelimiter(trimmed);
    const activeChain = !leadingClose && startsWithFluentMember(trimmed)
      ? chainAtDepth(chains, delimiterStack.length)
      : undefined;
    const lineDepth = leadingClose && delimiterStack.length > 0
      ? delimiterStack[delimiterStack.length - 1].closeIndent
      : activeChain
      ? activeChain.indent
      : currentChildIndent(delimiterStack);
    formatted.push(indentText.repeat(lineDepth) + trimmed);

    const startingDepth = delimiterStack.length;
    updateDelimiterStack(trimmed, delimiterStack, lineDepth);
    pruneChains(chains, delimiterStack.length);
    updateFluentChainState(chains, trimmed, startingDepth, delimiterStack.length, lineDepth);
  }
  return formatted.join('\n');
}

function currentChildIndent(delimiterStack) {
  return delimiterStack.length > 0
    ? delimiterStack[delimiterStack.length - 1].childIndent
    : 0;
}

function startsWithClosingDelimiter(line) {
  return /^[}\])]/.test(line);
}

function startsWithFluentMember(line) {
  return /^\.[A-Za-z_][A-Za-z0-9_]*/.test(line);
}

function updateDelimiterStack(line, delimiterStack, lineDepth) {
  const stripped = stripStringsAndComments(line);
  for (const char of stripped) {
    if ('({['.includes(char)) {
      delimiterStack.push({
        char,
        childIndent: lineDepth + 1,
        closeIndent: lineDepth
      });
      continue;
    }
    if (')}]'.includes(char)) {
      const expected = matchingOpen(char);
      const index = findLastDelimiter(delimiterStack, expected);
      if (index >= 0) {
        delimiterStack.splice(index);
      }
    }
  }
}

function findLastDelimiter(delimiterStack, expected) {
  for (let index = delimiterStack.length - 1; index >= 0; index -= 1) {
    if (delimiterStack[index].char === expected) {
      return index;
    }
  }
  return -1;
}

function updateFluentChainState(chains, line, startingDepth, endingDepth, lineDepth) {
  if (startsWithFluentMember(line)) {
    if (hasTopLevelSemicolon(line)) {
      clearChainAtDepth(chains, endingDepth);
      clearChainAtDepth(chains, startingDepth);
    }
    return;
  }

  if (startsWithClosingDelimiter(line)) {
    if (hasTopLevelSemicolon(line)) {
      clearChainAtDepth(chains, endingDepth);
    }
    return;
  }

  clearChainAtDepth(chains, startingDepth);
  if (canStartFluentChain(line)) {
    setChainAtDepth(chains, endingDepth, lineDepth + 1);
  }
}

function canStartFluentChain(line) {
  const trimmed = stripLineComment(line).trim();
  if (!trimmed
      || startsWithFluentMember(trimmed)
      || startsWithClosingDelimiter(trimmed)
      || hasTopLevelSemicolon(trimmed)
      || /[,{\[]\s*$/.test(trimmed)
      || /^(?:if|else|for|while|switch|using|namespace|class|public|private|static|override)\b/.test(trimmed)) {
    return false;
  }
  return /(?:\)|\])\s*$/.test(trimmed);
}

function hasTopLevelSemicolon(line) {
  const stripped = stripStringsAndComments(line);
  let depth = 0;
  for (const char of stripped) {
    if ('({['.includes(char)) {
      depth += 1;
    } else if (')}]'.includes(char)) {
      depth = Math.max(0, depth - 1);
    } else if (char === ';' && depth === 0) {
      return true;
    }
  }
  return false;
}

function chainAtDepth(chains, depth) {
  for (let index = chains.length - 1; index >= 0; index -= 1) {
    if (chains[index].depth === depth) {
      return chains[index];
    }
  }
  return undefined;
}

function setChainAtDepth(chains, depth, indent) {
  clearChainAtDepth(chains, depth);
  chains.push({ depth, indent });
}

function clearChainAtDepth(chains, depth) {
  for (let index = chains.length - 1; index >= 0; index -= 1) {
    if (chains[index].depth === depth) {
      chains.splice(index, 1);
    }
  }
}

function pruneChains(chains, maxDepth) {
  for (let index = chains.length - 1; index >= 0; index -= 1) {
    if (chains[index].depth > maxDepth) {
      chains.splice(index, 1);
    }
  }
}

function removeContextLocalLeadingDots(text) {
  return text.replace(/^(\s*)\.(Line|LineAfterMove|Choice|NextNode)(\s*\()/gm, '$1$2$3');
}

function collapseLineCalls(text) {
  const pattern = /^([ \t]*)(?:Line|LineAfterMove)\s*\(/gm;
  let result = '';
  let cursor = 0;
  let match;
  while ((match = pattern.exec(text)) !== null) {
    const open = text.indexOf('(', match.index);
    const close = findMatchingDelimiter(text, open, '(', ')');
    if (close < 0 || text[close + 1] !== ';') {
      continue;
    }
    const expression = collapseWhitespaceOutsideStrings(text.slice(match.index + match[1].length, close + 1));
    result += text.slice(cursor, match.index);
    result += match[1] + expression + ';';
    cursor = close + 2;
    pattern.lastIndex = close + 2;
  }
  return result + text.slice(cursor);
}

function findMatchingDelimiter(text, openIndex, openChar, closeChar) {
  let depth = 0;
  let quote = '';
  let escaped = false;
  let lineComment = false;
  let blockComment = false;
  for (let index = openIndex; index < text.length; index += 1) {
    const char = text[index];
    const next = index + 1 < text.length ? text[index + 1] : '';
    if (lineComment) {
      if (char === '\n') {
        lineComment = false;
      }
      continue;
    }
    if (blockComment) {
      if (char === '*' && next === '/') {
        blockComment = false;
        index += 1;
      }
      continue;
    }
    if (quote) {
      if (escaped) {
        escaped = false;
      } else if (char === '\\') {
        escaped = true;
      } else if (char === quote) {
        quote = '';
      }
      continue;
    }
    if (char === '/' && next === '/') {
      lineComment = true;
      index += 1;
      continue;
    }
    if (char === '/' && next === '*') {
      blockComment = true;
      index += 1;
      continue;
    }
    if (char === '"' || char === "'") {
      quote = char;
      continue;
    }
    if (char === openChar) {
      depth += 1;
      continue;
    }
    if (char === closeChar) {
      depth -= 1;
      if (depth === 0) {
        return index;
      }
    }
  }
  return -1;
}

function collapseWhitespaceOutsideStrings(text) {
  let output = '';
  let quote = '';
  let escaped = false;
  let pendingSpace = false;
  for (const char of text) {
    if (quote) {
      output += char;
      if (escaped) {
        escaped = false;
      } else if (char === '\\') {
        escaped = true;
      } else if (char === quote) {
        quote = '';
      }
      continue;
    }
    if (char === '"' || char === "'") {
      if (pendingSpace && output && !/[\s([{,:]$/.test(output)) {
        output += ' ';
      }
      pendingSpace = false;
      quote = char;
      output += char;
      continue;
    }
    if (/\s/.test(char)) {
      pendingSpace = true;
      continue;
    }
    if (pendingSpace && output && !/[\s([{,:]$/.test(output) && !/[)\]}.,;]/.test(char)) {
      output += ' ';
    }
    pendingSpace = false;
    output += char;
  }
  return output
    .replace(/\s+,/g, ',')
    .replace(/,\s*/g, ', ')
    .replace(/\(\s+/g, '(')
    .replace(/\s+\)/g, ')')
    .replace(/\[\s+/g, '[')
    .replace(/\s+\]/g, ']')
    .replace(/\s*:\s*/g, ': ')
    .trim();
}

function preferFileScopedNamespace(text) {
  const lines = text.split(/\r?\n/);
  const namespaceLine = lines.findIndex(line => line.trim().length > 0);
  if (namespaceLine < 0) {
    return text;
  }
  const match = /^(\s*)namespace\s+([A-Za-z_][\w.]*)\s*(\{\s*)?$/.exec(lines[namespaceLine]);
  if (!match) {
    return text;
  }
  let openLine = namespaceLine;
  if (!match[3]) {
    openLine = lines.findIndex((line, index) => index > namespaceLine && line.trim().length > 0);
    if (openLine < 0 || !/^\s*\{\s*$/.test(lines[openLine])) {
      return text;
    }
  }

  let depth = 0;
  let closeLine = -1;
  for (let index = openLine; index < lines.length; index += 1) {
    const stripped = stripStringsAndComments(lines[index]);
    depth += countStructural(stripped, /[{]/g) - countStructural(stripped, /[}]/g);
    if (index > openLine && depth === 0) {
      closeLine = index;
      break;
    }
  }
  if (closeLine < 0 || lines.slice(closeLine + 1).some(line => line.trim().length > 0)) {
    return text;
  }

  return [
    ...lines.slice(0, namespaceLine),
    `${match[1]}namespace ${match[2]};`,
    ...lines.slice(openLine + 1, closeLine)
  ].join('\n');
}

function countStructural(line, pattern) {
  const stripped = line.replace(/"([^"\\]|\\.)*"|'([^'\\]|\\.)*'/g, '');
  return [...stripped.matchAll(pattern)].length;
}

function stripStringsAndComments(line) {
  return line
    .replace(/"([^"\\]|\\.)*"|'([^'\\]|\\.)*'/g, '')
    .replace(/\/\/.*$/g, '')
    .replace(/\/\*.*?\*\//g, '');
}

function maskStringsAndComments(line) {
  return String(line || '')
    .replace(/"([^"\\]|\\.)*"|'([^'\\]|\\.)*'/g, value => ' '.repeat(value.length))
    .replace(/\/\/.*$/g, value => ' '.repeat(value.length))
    .replace(/\/\*.*?\*\//g, value => ' '.repeat(value.length));
}

function signatureForWord(word) {
  return METHOD_SIGNATURES.get(word);
}

function hoverWords() {
  return [...METHOD_SIGNATURES.keys()];
}

function hoverSummaryForWord(word) {
  const summary = METHOD_HOVER_SUMMARIES_RU.get(word) || METHOD_HOVER_SUMMARIES.get(word);
  if (summary) {
    return summary;
  }
  return fallbackHoverSummary(word, METHOD_SIGNATURES.get(word));
}

function fallbackHoverSummary(word, signature) {
  if (!signature) {
    return '';
  }
  if (STATIC_NAMESPACE_MEMBERS[word]) {
    return 'Встроенные EchoScript константы. Используй эти символы вместо сырых строк и неподдерживаемых placeholder names.';
  }
  if (/->\s*TriggerDef\b/.test(signature)) {
    return 'Создает trigger, который решает, когда должен стартовать Event().';
  }
  if (/->\s*Effect\b/.test(signature)) {
    return 'Создает runtime story effect для Effects(), Do(), scene steps или action callbacks.';
  }
  if (/->\s*SequenceStep\b/.test(signature) || /->\s*SceneStep\b/.test(signature)) {
    return 'Создает шаг последовательности для Scene().Steps(...).';
  }
  if (/->\s*StateRef\b/.test(signature)) {
    return 'Создает typed reference на story state, чтобы чтение и запись были согласованы.';
  }
  if (/->\s*CostDef\b/.test(signature)) {
    return 'Создает payload стоимости для доступности action и оплаты.';
  }
  if (/->\s*Vector3\b/.test(signature)) {
    return 'Создает координатный Vector3 для движения, камеры и world effects.';
  }
  if (/->\s*[A-Za-z0-9_]*Def\b/.test(signature)) {
    return 'Создает или настраивает EchoScript definition object для текущего ресурса.';
  }
  if (/^private\s+/m.test(signature) || /^public\s+/m.test(signature)) {
    return 'Метод ресурса, который compiler распознает и прикрепляет к generated story definition.';
  }
  if (/StoryIndex\(\)/.test(signature)) {
    return 'Добавляет indexed resource references, чтобы другие story files могли их resolve/autocomplete.';
  }
  return 'EchoScript helper. Сигнатура ниже показывает, где он валиден и какие аргументы принимает.';
}

module.exports = {
  analyzeEchoScript,
  formatEchoScript,
  hoverWords,
  hoverSummaryForWord,
  signatureForWord
};
