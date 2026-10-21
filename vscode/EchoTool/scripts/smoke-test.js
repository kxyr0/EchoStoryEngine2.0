'use strict';

const fs = require('fs');
const os = require('os');
const path = require('path');
const { Json5Parser } = require('../src/json5/json5Parser');
const { CommentPreservingJson5Formatter } = require('../src/json5/commentPreservingFormatter');
const { StoryWorkspaceIndex } = require('../src/workspace/storyWorkspaceIndex');
const { createStoryCatalog } = require('../src/catalog/defaultCatalog');
const { resolveReference, referenceIds, shortcutReference } = require('../src/language/referenceResolver');
const {
  referenceOccurrencesInText,
  referenceRangesInText,
  referenceRenameEditsInText
} = require('../src/language/storyReferenceScanner');
const {
  dialogueNodeRenameRanges,
  dialogueNodeRenameTarget,
  dialogueNodeDeclarationRanges,
  dialogueNodeReferenceRanges
} = require('../src/language/dialogueNodeRename');
const { scalarValuesForField, isBooleanField } = require('../src/language/valueCompletionModel');
const { keyCompletionRole } = require('../src/shared/authoringKeyModel');
const { authoringKeyEntriesFromValue } = require('../src/workspace/authoringKeys');
const { localizationKeyEntriesFromValue } = require('../src/workspace/localizationKeys');
const {
  questVisualsInText,
  normalizeQuestStatus,
  questCodeLensTitle
} = require('../src/language/questVisuals');
const {
  dialogueNodeIdsFromValue,
  dialogueChoiceIdsFromValue,
  speakerAliasTargetsFromValue
} = require('../src/language/localStoryContext');
const {
  analyzeEchoScript,
  formatEchoScript,
  hoverWords,
  hoverSummaryForWord,
  signatureForWord
} = require('../src/language/echoscriptSupport');
const {
  echoScriptCompletionEntries,
  pascalFromId
} = require('../src/language/echoscriptCompletionModel');
const {
  echoScriptDefinitionTarget,
  echoScriptRenameTarget,
  echoScriptReferenceQueryAtOffset,
  echoScriptReferenceOccurrencesInText,
  echoScriptReferenceRangesInText,
  echoScriptReferenceRenameEditsInText,
  echoScriptIdentifierRenameEditsInText
} = require('../src/language/echoscriptNavigation');
const {
  echoScriptSignatureHelp,
  signatureHelpTargets
} = require('../src/language/echoscriptSignatureHelp');

async function main() {
  const workspaceRoot = path.resolve(__dirname, '..');
  const modRoot = process.env.ECHOSTORY_MOD_ROOT || 'D:\\JDHSeason\\mods';
  assertFile(path.join(workspaceRoot, 'package.json'));
  assertFile(path.join(workspaceRoot, 'package.nls.json'));
  assertFile(path.join(workspaceRoot, 'package.nls.ru.json'));
  assertDirectory(path.join(workspaceRoot, 'node_modules', 'json5'));
  assertDirectory(path.join(workspaceRoot, 'node_modules', 'prettier'));

  const manifest = JSON.parse(fs.readFileSync(path.join(workspaceRoot, 'package.json'), 'utf8'));
  assertEqual(manifest.version, '1.11.0', 'Extension version must match the EE1 release.');
  assertEqual(manifest.icon, 'icon.png', 'Manifest should use bundled VS Code icon.');
  assert(manifest.displayName.includes('%'), 'Manifest displayName should use package.nls localization.');
  assert(manifest.description.includes('%'), 'Manifest description should use package.nls localization.');
  assert(
    manifest.contributes.languages.some(language => language.id === 'echoscript'),
    'Manifest should register EchoScript .es language support.'
  );

  verifyLanguageAssets(workspaceRoot);
  verifyEchoScriptTooling();
  verifyEchoScriptSignatureHelp();
  verifyEchoScriptNavigation();
  verifyEchoScriptCompletions();
  verifyCatalog();
  verifyLocalStoryContext();
  verifyValueCompletionModel();
  verifyAuthoringKeyModel();
  verifyLocalizationKeyModel();
  verifyQuestVisuals();
  verifyReferenceScanner();
  verifyDialogueNodeRename();
  await verifyFormatter();
  await verifyIndex(modRoot);
  verifyInstalledCopyIfRequested(manifest);

  console.log('EchoStory smoke test passed.');
}

function verifyLanguageAssets(workspaceRoot) {
  JSON.parse(fs.readFileSync(path.join(workspaceRoot, 'language-configuration.json'), 'utf8'));
  JSON.parse(fs.readFileSync(path.join(workspaceRoot, 'syntaxes', 'echostory-json5.tmLanguage.json'), 'utf8'));
  const echoScriptGrammar = fs.readFileSync(path.join(workspaceRoot, 'syntaxes', 'echoscript.tmLanguage.json'), 'utf8');
  JSON.parse(echoScriptGrammar);
  assert(echoScriptGrammar.includes('Npc\\\\.SpawnNearPlayer'), 'EchoScript grammar should highlight typed NPC helpers.');
  assert(echoScriptGrammar.includes('Vehicle\\\\.Trip'), 'EchoScript grammar should highlight typed vehicle helpers.');
  assert(echoScriptGrammar.includes('Navigation\\\\.SetTarget'), 'EchoScript grammar should highlight navigation helpers.');
  assert(echoScriptGrammar.includes('Player\\\\.Pose'), 'EchoScript grammar should highlight player pose helpers.');
  assert(echoScriptGrammar.includes('Story\\\\.(?:Set|Load)') && echoScriptGrammar.includes('Environment\\\\.Set') && echoScriptGrammar.includes('Dialogue\\\\.Load'), 'EchoScript grammar should highlight story, environment, and dialogue load helpers.');
  assert(echoScriptGrammar.includes('Sleep\\\\.(?:Arm') && echoScriptGrammar.includes('Blackspace\\\\.Create'), 'EchoScript grammar should highlight sleep and blackspace helpers.');
  assert(echoScriptGrammar.includes('Npc\\\\.SpawnAt') && echoScriptGrammar.includes('Player\\\\.SpawnAt') && echoScriptGrammar.includes('Screen\\\\.SetBlack'), 'EchoScript grammar should highlight new spawn and screen helpers.');
  assert(echoScriptGrammar.includes('StartDialogue') && echoScriptGrammar.includes('DialogueStart') && echoScriptGrammar.includes('Npc\\\\.StandUp') && echoScriptGrammar.includes('Debug\\\\.Mark'), 'EchoScript grammar should highlight trigger dialogue aliases and debug callback actions.');
  assert(echoScriptGrammar.includes('InventoryHasItem'), 'EchoScript grammar should highlight inventory trigger helpers.');
  assert(echoScriptGrammar.includes('WaitChunks'), 'EchoScript grammar should highlight scene step helpers.');
  assert(echoScriptGrammar.includes('AbortEffects') && echoScriptGrammar.includes('BuildQuest') && echoScriptGrammar.includes('Money'), 'EchoScript grammar should highlight lifecycle, build, and cost helper calls.');
  assert(echoScriptGrammar.includes('animation_profile') && echoScriptGrammar.includes('animation_clip') && echoScriptGrammar.includes('npc_animation_binding'), 'EchoScript grammar should highlight new modular resource keywords.');
  assert(echoScriptGrammar.includes('environment') && echoScriptGrammar.includes('npc_form'), 'EchoScript grammar should highlight environment and NPC form resource keywords.');
  assert(echoScriptGrammar.includes('player_profile_template') && echoScriptGrammar.includes('weapon') && echoScriptGrammar.includes('trigger'), 'EchoScript grammar should highlight player, weapon, and trigger resource keywords.');
  assert(echoScriptGrammar.includes('AnimationProfiles') && echoScriptGrammar.includes('AnimationClips') && echoScriptGrammar.includes('FaceRigs'), 'EchoScript grammar should highlight new modular resource namespaces.');
  assert(echoScriptGrammar.includes('Environments') && echoScriptGrammar.includes('NpcForms'), 'EchoScript grammar should highlight environment and NPC form namespaces.');
  assert(echoScriptGrammar.includes('PlayerProfileTemplates') && echoScriptGrammar.includes('Weapons') && echoScriptGrammar.includes('Triggers'), 'EchoScript grammar should highlight player, weapon, and trigger namespaces.');
  assert(echoScriptGrammar.includes('PlayerSleep\\\\.(?:Confirmed') && echoScriptGrammar.includes('PlayerInside') && echoScriptGrammar.includes('WeatherChanged'), 'EchoScript grammar should highlight new trigger DSL helpers.');
  assert(echoScriptGrammar.includes('(?:Resource|Def|Ref|Id|Effect'), 'EchoScript grammar should highlight typed resource id declarations.');
  assert(echoScriptGrammar.includes('State\\\\.(?:Memory|Ref|Set|Remove|Clear|Add|Adjust|Increment|Decrement)'), 'EchoScript grammar should highlight state helpers.');
  assert(echoScriptGrammar.includes('face_animation') && echoScriptGrammar.includes('camera_path'), 'EchoScript grammar should highlight resource keywords.');
  assert(echoScriptGrammar.includes('Monologue\\\\.Show'), 'EchoScript grammar should highlight member effect helpers.');
  assert(!echoScriptGrammar.includes('variable.other.state.memory'), 'EchoScript grammar should not highlight unsupported direct memory assignment.');
  assert(!echoScriptGrammar.includes('alias-line'), 'EchoScript grammar should not highlight unsupported alias-line shorthand.');
  JSON.parse(fs.readFileSync(path.join(workspaceRoot, 'snippets', 'echostory-json5.code-snippets'), 'utf8'));
  const echoScriptSnippets = fs.readFileSync(path.join(workspaceRoot, 'snippets', 'echoscript.code-snippets'), 'utf8');
  JSON.parse(echoScriptSnippets);
  assert(!echoScriptSnippets.includes('"prefix": "ese-'), 'EchoScript snippets should use visible method/resource names, not internal ese-* prefixes.');
  assert(echoScriptSnippets.includes('"prefix": "StoryResource"'), 'EchoScript snippets should expose the new StoryResource template.');
  assert(echoScriptSnippets.includes('"prefix": "DialogueResource"'), 'EchoScript snippets should expose the new DialogueResource template.');
  assert(echoScriptSnippets.includes('"prefix": "LineAfterMove"'), 'EchoScript snippets should expose LineAfterMove for arrival-gated dialogue lines.');
  assert(echoScriptSnippets.includes('"prefix": "NpcResource"'), 'EchoScript snippets should expose the new NpcResource template.');
  assert(echoScriptSnippets.includes('"prefix": "NpcFormResource"'), 'EchoScript snippets should expose the new NpcFormResource template.');
  assert(echoScriptSnippets.includes('"prefix": "EnvironmentResource"'), 'EchoScript snippets should expose the new EnvironmentResource template.');
  assert(echoScriptSnippets.includes('"prefix": "AnimationResource"'), 'EchoScript snippets should expose the new AnimationResource template.');
  assert(echoScriptSnippets.includes('"prefix": "AnimationProfileResource"'), 'EchoScript snippets should expose the new AnimationProfileResource template.');
  assert(echoScriptSnippets.includes('"prefix": "AnimationRigResource"'), 'EchoScript snippets should expose the new AnimationRigResource template.');
  assert(echoScriptSnippets.includes('"prefix": "AnimationClipResource"'), 'EchoScript snippets should expose the new AnimationClipResource template.');
  assert(echoScriptSnippets.includes('"prefix": "FaceRigResource"'), 'EchoScript snippets should expose the high-level face DSL template.');
  assert(echoScriptSnippets.includes('"prefix": "FacePoseResource"'), 'EchoScript snippets should expose the new FacePoseResource template.');
  assert(echoScriptSnippets.includes('"prefix": "FacialAnimationResource"'), 'EchoScript snippets should expose the new FacialAnimationResource template.');
  assert(echoScriptSnippets.includes('"prefix": "NpcAnimationBindingResource"'), 'EchoScript snippets should expose the new NPC animation binding template.');
  assert(echoScriptSnippets.includes('"prefix": "TriggerResource"'), 'EchoScript snippets should expose the new TriggerResource template.');
  assert(echoScriptSnippets.includes('"prefix": "PlayerProfileTemplate"'), 'EchoScript snippets should expose the new PlayerProfileTemplate template.');
  assert(echoScriptSnippets.includes('"prefix": "WeaponResource"'), 'EchoScript snippets should expose the new WeaponResource template.');
  assert(echoScriptSnippets.includes('"prefix": "PlayerSleep.Confirmed"'), 'EchoScript snippets should expose PlayerSleep selectors.');
  assert(!echoScriptSnippets.includes('"EchoScript StartEffects Method"'), 'EchoScript snippets should not display internal StartEffects names.');
  assert(!echoScriptSnippets.includes('"EchoScript CompleteEffects Method"'), 'EchoScript snippets should not display internal CompleteEffects names.');
  assert(!echoScriptSnippets.includes('"EchoScript AbortEffects Method"'), 'EchoScript snippets should not display internal AbortEffects names.');
  assert(!echoScriptSnippets.includes('"EchoScript NPC Spawn"'), 'EchoScript snippets should not display internal Npc.Spawn names.');
  assert(echoScriptSnippets.includes('return Story('), 'EchoScript story snippets should use Story(...) graph resources.');
  assert(echoScriptSnippets.includes('type: \\"echo_dialogue:play\\"'), 'EchoScript story snippets should use modular dialogue action types.');
  assert(echoScriptSnippets.includes('type: \\"echo_npc:spawn\\"'), 'EchoScript story snippets should use modular NPC action types.');
  assert(!echoScriptSnippets.includes('BuildEvent()'), 'EchoScript snippets should not guide new content toward old EventResource BuildEvent.');
  assert(!echoScriptSnippets.includes('class ${1:SceneName} : SceneResource'), 'EchoScript snippets should not expose old SceneResource templates.');
  assert(!echoScriptSnippets.includes('.RunIf('), 'EchoScript snippets should not use RunIf.');
  assert(!echoScriptSnippets.includes('.VisibleIf('), 'EchoScript snippets should not use VisibleIf.');
  assert(!echoScriptSnippets.includes('.If('), 'EchoScript snippets should not use member If.');
  assert(!echoScriptSnippets.includes('.Line('), 'EchoScript snippets should not use leading-dot Line.');
  assert(!echoScriptSnippets.includes('.LineAfterMove('), 'EchoScript snippets should not use leading-dot LineAfterMove.');
  assert(!echoScriptSnippets.includes('.Choice('), 'EchoScript snippets should not use leading-dot Choice.');
  assert(!echoScriptSnippets.includes('.NextNode('), 'EchoScript snippets should not use leading-dot NextNode.');
  assert(!echoScriptSnippets.includes('.Alias('), 'EchoScript snippets should not use leading-dot Alias.');
  assert(!echoScriptSnippets.includes('ese-alias-line'), 'EchoScript snippets should not expose legacy alias-line shorthand naming.');
  assert(!echoScriptSnippets.includes('if: "'), 'EchoScript snippets should not emit string-based if: choice conditions.');
  assert(!echoScriptSnippets.includes('Layer.Body') && !echoScriptSnippets.includes('Hand.Mainhand') && !echoScriptSnippets.includes('Items.Item'), 'EchoScript snippets should not emit unsupported static symbols.');
  assert(!echoScriptSnippets.includes('memory.${1:chapter}.${2:key}'), 'EchoScript snippets should not emit unsupported direct memory assignment.');
  assert(echoScriptSnippets.includes('Node ${2:FirstMeeting}()'), 'Default dialogue snippet should use one high-level Node.');
  assert(echoScriptSnippets.includes('start Node ${2:Opening}()'), 'Multi-node dialogue snippet should expose start Node.');
  assert(echoScriptSnippets.includes('GoTo(${5:Continue});'), 'Multi-node dialogue snippet should use symbolic GoTo.');
  assert(!echoScriptSnippets.includes('public override DialogueResourceDef Build()'), 'Default dialogue snippet should not emit legacy Build boilerplate.');
  assert(echoScriptSnippets.includes('Line(\\"${3:john}\\"'), 'EchoScript dialogue snippets should use local Line(...).');
  assert(!echoScriptSnippets.includes('.StartEffects('), 'EchoScript snippets should not include StartEffects chain calls.');
  assert(!echoScriptSnippets.includes('.CompleteEffects('), 'EchoScript snippets should not include CompleteEffects chain calls.');
  assert(echoScriptSnippets.includes('private StateRef ${1:stateName} = State.Memory'), 'EchoScript snippets should include explicit StateRef field declarations.');
  assert(echoScriptSnippets.includes('Set(${1:stateName}'), 'EchoScript snippets should include camelCase Set(...) state writes.');
  assert(echoScriptSnippets.includes('Npc.Spawn(${1:NpcRef}'), 'EchoScript snippets should keep typed NPC spawn helper.');
  assert(echoScriptSnippets.includes('defaultForm: DefaultForm'), 'EchoScript snippets should include NPC default forms.');
  assert(echoScriptSnippets.includes('Environment.Set('), 'EchoScript snippets should include Environment.Set.');
  assert(echoScriptSnippets.includes('Dialogue.Load('), 'EchoScript snippets should include Dialogue.Load.');
  assert(echoScriptSnippets.includes('Story.Load('), 'EchoScript snippets should include Story.Load.');
  assert(echoScriptSnippets.includes('Sleep.EnterDream('), 'EchoScript snippets should include Sleep.EnterDream.');
  assert(echoScriptSnippets.includes('Vehicle.Trip('), 'EchoScript snippets should include Vehicle.Trip.');
  assert(echoScriptSnippets.includes('Navigation.SetTarget(') && echoScriptSnippets.includes('dimensionId: \\"minecraft:overworld\\"'), 'EchoScript snippets should include current Navigation.SetTarget syntax.');
  const inlayHintsProvider = fs.readFileSync(path.join(workspaceRoot, 'src', 'language', 'inlayHintsProvider.js'), 'utf8');
  assert(!inlayHintsProvider.includes('resolved.location.relativePath'), 'Inlay hints should not render long target paths inline.');
  assert(inlayHintsProvider.includes('kindLabel(resolved.kind)'), 'Inlay hints should render compact target-kind labels.');
  const hoverProvider = fs.readFileSync(path.join(workspaceRoot, 'src', 'language', 'hoverProvider.js'), 'utf8');
  assert(!hoverProvider.includes('relativePath'), 'Hover provider should not render indexed file paths.');
  const completionProvider = fs.readFileSync(path.join(workspaceRoot, 'src', 'language', 'completionProvider.js'), 'utf8');
  assert(!completionProvider.includes('relativePath'), 'Completion documentation should not render indexed file paths.');
  const workspaceSymbolProvider = fs.readFileSync(path.join(workspaceRoot, 'src', 'language', 'workspaceSymbolProvider.js'), 'utf8');
  assert(workspaceSymbolProvider.includes('`EchoStory ${entry.kind}`'), 'Workspace symbols should display compact containers instead of file paths.');
}

function verifyEchoScriptTooling() {
  const diagnostics = analyzeEchoScript([
    'var storyEvent = Event()',
    '    .RunIf(street_done);',
    'elif (home_done) {',
    '    storyEvent.Effects(Dialogue.Start(chapter_1_home));',
    '}'
  ].join('\n'));
  assert(
    diagnostics.some(item => /RunIf/.test(item.message))
      && diagnostics.some(item => /elif/.test(item.message))
      && diagnostics.some(item => /ESL0304/.test(item.message)),
    'EchoScript diagnostics should report forbidden condition spelling and incomplete fragments.'
  );
  assert(
    !diagnostics.some(item => /Variable declarations must end/.test(item.message)),
    'EchoScript diagnostics should not report multiline fluent variable declarations as unterminated.'
  );
  const scopedDiagnostics = analyzeEchoScript([
    'class Sample : EventResource',
    '{',
    '    public override StoryResourceDef Build()',
    '    {',
    '        var storyEvent = new Event()',
    '            .Trigger(AreaEnterDelayed(HomeRelaxExit, Overworld, minArea, maxArea, delayTicks: 40))',
    '            .FireOnce(true);',
    '        var minArea = new Vector3(0, 0, 0);',
    '        var maxArea = new Vector3(1, 1, 1);',
    '        if (RoomDone)',
    '        {',
    '            var minArea = new Vector3(2, 2, 2);',
    '            storyEvent.Effects(Npc.Move(JohnFather, minArea));',
    '        }',
    '        return storyEvent;',
    '    }',
    '}'
  ].join('\n'));
  assert(scopedDiagnostics.some(item => /ESL0301/.test(item.message) && /minArea/.test(item.message)), 'EchoScript diagnostics should report variable use before declaration.');
  assert(scopedDiagnostics.some(item => /ESL0302/.test(item.message) && /minArea/.test(item.message)), 'EchoScript diagnostics should report variable shadowing.');
  assert(!scopedDiagnostics.some(item => /ESL0303/.test(item.message)), 'EchoScript diagnostics should not warn about compiler-supported Build() fallback.');
  assert(!scopedDiagnostics.some(item => /Variable declarations must end/.test(item.message)), 'EchoScript diagnostics should allow multiline fluent var declarations.');
  assert(!scopedDiagnostics.some(item => /Return statements must end/.test(item.message)), 'EchoScript diagnostics should allow terminated return statements.');
  const buildFallbackDiagnostics = analyzeEchoScript([
    'class EventSample : EventResource',
    '{',
    '    public override StoryResourceDef Build()',
    '    {',
    '        return new Event();',
    '    }',
    '}',
    'class QuestSample : QuestResource',
    '{',
    '    public override StoryResourceDef Build()',
    '    {',
    '        return Obj();',
    '    }',
    '}',
    'class DialogueSample : DialogueResource',
    '{',
    '    public override DialogueDef Build()',
    '    {',
    '        return Dialogue();',
    '    }',
    '}',
    'class AnimationSample : AnimationResource',
    '{',
    '    public override AnimationDef Build()',
    '    {',
    '        return Animation();',
    '    }',
    '}',
    'class SceneSample : SceneResource',
    '{',
    '    public override StoryResourceDef Build()',
    '    {',
    '        return Scene();',
    '    }',
    '}'
  ].join('\n'));
  assert(!buildFallbackDiagnostics.some(item => /ESL0303/.test(item.message)), 'EchoScript diagnostics should not warn about Build() fallback for resource classes by default.');
  const returnChainDiagnostics = analyzeEchoScript([
    'class Intro : DialogueResource',
    '{',
    '    public override DialogueDef BuildDialogue()',
    '    {',
    '        return Dialogue()',
    '            .Start("start")',
    '            .Alias("j", Jdh)',
    '            .Nodes(Start());',
    '    }',
    '}'
  ].join('\n'));
  assert(!returnChainDiagnostics.some(item => /Return statements must end/.test(item.message)), 'EchoScript diagnostics should allow multiline fluent return chains.');
  assert(returnChainDiagnostics.some(item => /ESL0430/.test(item.message)), 'EchoScript diagnostics should require Init()/Nodes() methods instead of dialogue config chains.');
  const nodeReturnDiagnostics = analyzeEchoScript([
    'private NodeDef Room()',
    '{',
    '    return Node("room")',
    '        .Line(',
    '            "s",',
    '            "Можно?",',
    '            key: "chapter_1.room.line_1"',
    '        );',
    '}'
  ].join('\n'));
  assert(!nodeReturnDiagnostics.some(item => /Return statements must end/.test(item.message)), 'EchoScript diagnostics should allow multiline fluent Node return chains.');
  const namedIfDiagnostics = analyzeEchoScript([
    'Choice(',
    '    "john_ask_no_problem",',
    '    "Да, без проблем",',
    '    "room_after_choices",',
    '    key: "chapter_1.room.choice.no_problem",',
    '    if: "!exists(chapter_1.john_ask_no_problem)"',
    ');'
  ].join('\n'));
  assert(!namedIfDiagnostics.some(item => /if.*parentheses/i.test(item.message)), 'EchoScript diagnostics should not treat named argument if: as an if statement.');
  const startEffectsDiagnostics = analyzeEchoScript([
    'return Dialogue()',
    '    .StartEffects(Set(RoomDone, true));'
  ].join('\n'));
  assert(startEffectsDiagnostics.some(item => /ESL0431/.test(item.message)), 'EchoScript diagnostics should require StartEffects methods instead of chain calls.');
  const badReturnDiagnostics = analyzeEchoScript([
    'class Intro : DialogueResource',
    '{',
    '    public override DialogueDef BuildDialogue()',
    '    {',
    '        return Dialogue()',
    '    }',
    '}'
  ].join('\n'));
  assert(badReturnDiagnostics.some(item => /Return statements must end/.test(item.message)), 'EchoScript diagnostics should still report genuinely unterminated return statements.');
  const localCallDiagnostics = analyzeEchoScript([
    '    .Line("s", "Legacy line");',
    '    .Choice("ask", "Ask");',
    '    .NextNode(RoomAfterChoices);'
  ].join('\n'));
  assert(localCallDiagnostics.some(item => /ESL0420/.test(item.message)), 'EchoScript diagnostics should report leading-dot Line.');
  assert(analyzeEchoScript('    .LineAfterMove("s", "Arrived");').some(item => /ESL0420/.test(item.message)), 'EchoScript diagnostics should report leading-dot LineAfterMove.');
  assert(localCallDiagnostics.some(item => /ESL0421/.test(item.message)), 'EchoScript diagnostics should report leading-dot Choice.');
  assert(localCallDiagnostics.some(item => /ESL0422/.test(item.message)), 'EchoScript diagnostics should report leading-dot NextNode.');
  const namingDiagnostics = analyzeEchoScript([
    'private StateRef RoomDone = State.Memory("chapter_1.room_done");',
    'public StateRef roomDone = State.Memory("chapter_1.room_done");',
    'Vector3 TargetPos = new Vector3(1, 2, 3);'
  ].join('\n'));
  assert(namingDiagnostics.some(item => /ESL0440/.test(item.message) && /RoomDone/.test(item.message)), 'EchoScript diagnostics should require camelCase private fields.');
  assert(namingDiagnostics.some(item => /ESL0441/.test(item.message) && /roomDone/.test(item.message)), 'EchoScript diagnostics should require PascalCase public fields.');
  assert(namingDiagnostics.some(item => /ESL0440/.test(item.message) && /TargetPos/.test(item.message)), 'EchoScript diagnostics should require camelCase local variables.');
  const formatted = formatEchoScript('if (street_done)\n{\nstoryEvent.Effects(Set(home_done, true));\n}', {
    insertSpaces: true,
    tabSize: 4
  });
  assert(formatted.includes('    storyEvent.Effects'), 'EchoScript formatter should indent block contents.');
  const chainFormatted = formatEchoScript([
    'class Sasha : SpeakerResource',
    '{',
    '    public override SpeakerDef Build()',
    '    {',
    '        return Speaker()',
    '        .DisplayName(displayName)',
    '        .DisplayNameKey(displayNameKey)',
    '        .Behavior(SashaBehavior());',
    '    }',
    '',
    '    private BehaviorDef SashaBehavior()',
    '    {',
    '        return Behavior()',
    '        .LookAtPlayers()',
    '        .RecoveryTicks(40)',
    '        .Idle(States.Idle, Animations.LocomotionIdle);',
    '    }',
    '}'
  ].join('\n'), {
    insertSpaces: true,
    tabSize: 4
  });
  assert(chainFormatted.includes('        return Speaker()\n            .DisplayName(displayName)'), 'EchoScript formatter should indent fluent member chains under return values.');
  assert(chainFormatted.includes('        return Behavior()\n            .LookAtPlayers()'), 'EchoScript formatter should indent secondary fluent chains under return values.');
  const nestedChainFormatted = formatEchoScript([
    'return Scene()',
    '.Camera(',
    'Camera()',
    '.PresetId("first_person")',
    '.BlendTicks(8)',
    ')',
    '.Start(',
    'Effect("black_screen")',
    ');'
  ].join('\n'), {
    insertSpaces: true,
    tabSize: 4
  });
  assert(nestedChainFormatted.includes('    .Camera(\n        Camera()\n            .PresetId("first_person")'), 'EchoScript formatter should keep nested fluent chains visually nested.');
  const localCallFormatted = formatEchoScript('    .Line("s", "Legacy line");\n    .Choice("ask", "Ask");\n    .NextNode(RoomAfterChoices);', {
    insertSpaces: true,
    tabSize: 4
  });
  assert(localCallFormatted.includes('Line("s", "Legacy line");'), 'EchoScript formatter should remove leading-dot Line.');
  assert(formatEchoScript('    .LineAfterMove("s", "Arrived");', {
    insertSpaces: true,
    tabSize: 4
  }).includes('LineAfterMove("s", "Arrived");'), 'EchoScript formatter should remove leading-dot LineAfterMove.');
  assert(localCallFormatted.includes('Choice("ask", "Ask");'), 'EchoScript formatter should remove leading-dot Choice.');
  assert(localCallFormatted.includes('NextNode(RoomAfterChoices);'), 'EchoScript formatter should remove leading-dot NextNode.');
  const lineFormatted = formatEchoScript([
    'Line(',
    '    "s",',
    '    "Можно?",',
    '    key: "chapter_1.room.line_1"',
    ');'
  ].join('\n'), {
    insertSpaces: true,
    tabSize: 4
  });
  assert(lineFormatted.trim() === 'Line("s", "Можно?", key: "chapter_1.room.line_1");', 'EchoScript formatter should collapse Line(...) calls to one line.');
  const lineAfterMoveFormatted = formatEchoScript([
    'LineAfterMove(',
    '    "s",',
    '    "Я на месте."',
    ');'
  ].join('\n'), {
    insertSpaces: true,
    tabSize: 4
  });
  assert(lineAfterMoveFormatted.trim() === 'LineAfterMove("s", "Я на месте.");', 'EchoScript formatter should collapse LineAfterMove(...) calls to one line.');
  const npcPathDiagnostics = analyzeEchoScript([
    'Npc.Path(',
    '    Sasha,',
    '    [new Vector3(101, 64, 100)],',
    '    speed: 1.25,',
    '    pauseTicks: 10,',
    '    openDoors: true,',
    '    canFloat: true,',
    '    repathAfterTicks: 30,',
    '    maxRepathAttempts: 4',
    ');'
  ].join('\n'));
  assert(!npcPathDiagnostics.some(item => /Npc\.Path/.test(item.message)), 'EchoScript diagnostics should accept current Npc.Path helpers.');
  const currentHelperDiagnostics = analyzeEchoScript([
    'return Event()',
    '    .Trigger(InventoryHasItem("chapter_1.radio_part.found", Items.SparkGrass, count: 1))',
    '    .Effects(',
    '        Do(BlackScreen.Show(state: "wake_prompt", fadeInTicks: 20, durationTicks: 40, fadeOutTicks: 20)),',
    '        Navigation.ClearTarget(),',
    '        Player.Pose("none")',
    '    )',
    '    .CooldownTicks(1200);',
    'var action = Action()',
    '    .Label("Pay")',
    '    .LabelKey("buyer.pay.label")',
    '    .ActionKey("buyer.pay")',
    '    .UnavailableLabelKey("buyer.pay.unavailable")',
    '    .SuccessLabelKey("buyer.pay.success")',
    '    .FireOnce(true);',
    'if (canPay)',
    '{',
    '    action.Visible();',
    '}',
    'Camera().Preset("template_close").FadeInTicks(20).DurationTicks(80);'
  ].join('\n'));
  assert(!currentHelperDiagnostics.some(item => /ESL060/.test(item.message)), 'EchoScript diagnostics should accept current action, trigger, navigation, player, and camera helpers.');
  const modularResourceDiagnostics = analyzeEchoScript([
    'face JohnFather("jdh:john_father")',
    '{',
    '    Bone Glasses;',
    '}',
    'class HomeArrivalDialogue : DialogueResource',
    '{',
    '    private FaceAnimationId FatherAngryBrows = "jdh:john_father/angry_brows";',
    '    public override DialogueResourceDef Build()',
    '    {',
    '        return Dialogue("jdh:chapter_1_home_arrival");',
    '    }',
    '    private NodeDef Greeting()',
    '    {',
    '        Line("father", "Hi").SetAnimationFace(FatherAngryBrows);',
    '        Line("father", "Next").StopFaceAnimation();',
    '        Line("john", "Done").SetAnimationFaceClear();',
    '    }',
    '}'
  ].join('\n'));
  assert(!modularResourceDiagnostics.some(item => /ESL0440|ESL0600/.test(item.message)), 'EchoScript diagnostics should accept current modular face id fields, face DSL declarations, and line face-animation methods.');
  const newResourceDiagnostics = analyzeEchoScript([
    'class DefaultPlayerProfile : PlayerProfileTemplate',
    '{',
    '    public override PlayerProfile Build()',
    '    {',
    '        return PlayerProfile(id: "jdh:player/default", nameMode: ACCOUNT, spark: 100, maxSpark: 100, skinMode: ACCOUNT);',
    '    }',
    '}',
    'class SparkSword : WeaponResource',
    '{',
    '    public override WeaponDefinition Build()',
    '    {',
    '        return Weapon(id: "jdh:spark_sword", name: "Spark Sword", texture: "jdh:item/spark_sword", damage: 7, attacksPerSecond: 1.6, rarity: COMMON);',
    '    }',
    '}',
    'class HomeDoorTrigger : TriggerResource',
    '{',
    '    private Area HomeDoor = Area("minecraft:overworld", 0, 64, 0, 2, 3, 2);',
    '    private void Init()',
    '    {',
    '        Id("jdh:home/door");',
    '        Scope(PLAYER);',
    '        Mode(REPEAT);',
    '        Subscribe(PlayerSleep.Confirmed("chapter_1", sleepCountAtLeast: 1));',
    '        Start(Sequence);',
    '    }',
    '    private void Sequence()',
    '    {',
    '        Wait(All(PlayerInside(HomeDoor), Not(IsSneaking())));',
    '        StartStory("jdh:chapter_1_home_arrival");',
    '    }',
    '}'
  ].join('\n'));
  assert(!newResourceDiagnostics.some(item => /ESL060|ESL061|ESL0301|ESL0303/.test(item.message)), 'EchoScript diagnostics should accept player profile, weapon, and trigger resource DSL.');
  const unknownMemberDiagnostics = analyzeEchoScript([
    'return Action()',
    '    .Lable("Pay");',
    'Npc.Fly(Sasha);'
  ].join('\n'));
  assert(unknownMemberDiagnostics.some(item => /ESL0600/.test(item.message) && /Lable/.test(item.message)), 'EchoScript diagnostics should report unknown fluent builder methods.');
  assert(unknownMemberDiagnostics.some(item => /ESL0601/.test(item.message) && /Npc\.Fly/.test(item.message)), 'EchoScript diagnostics should report unknown namespace helpers.');
  const unknownStaticDiagnostics = analyzeEchoScript([
    'This.PlayAnimation(Animations.NpcOfferItemHold, layer: Layer.Body);',
    'This.HoldItem(Items.Item, hand: Hand.Mainhand);',
    'Quest.Update(OldFriend.Quest, status: QuestStatus.Failed);',
    'Speaker().Portrait(Portraits.Default);',
    'Behavior().Idle(States.Walk, Animations.LocomotionIdle);'
  ].join('\n'));
  assert(unknownStaticDiagnostics.some(item => /ESL0610/.test(item.message) && /Layer\.Body/.test(item.message)), 'EchoScript diagnostics should report unsupported Layer static symbols.');
  assert(unknownStaticDiagnostics.some(item => /ESL0610/.test(item.message) && /Items\.Item/.test(item.message)), 'EchoScript diagnostics should report unsupported Items static symbols.');
  assert(unknownStaticDiagnostics.some(item => /ESL0610/.test(item.message) && /Hand\.Mainhand/.test(item.message)), 'EchoScript diagnostics should report unsupported Hand static symbols.');
  assert(unknownStaticDiagnostics.some(item => /ESL0610/.test(item.message) && /QuestStatus\.Failed/.test(item.message)), 'EchoScript diagnostics should report unsupported QuestStatus static symbols.');
  assert(unknownStaticDiagnostics.some(item => /ESL0610/.test(item.message) && /Portraits\.Default/.test(item.message)), 'EchoScript diagnostics should report unsupported Portraits static symbols.');
  assert(unknownStaticDiagnostics.some(item => /ESL0610/.test(item.message) && /States\.Walk/.test(item.message)), 'EchoScript diagnostics should report unsupported States static symbols.');
  const validStaticDiagnostics = analyzeEchoScript([
    'This.PlayAnimation(Animations.NpcOfferItemHold, layer: Layer.Overlay);',
    'This.HoldItem(Items.SparkGrass, hand: Hand.Main);',
    'Quest.Update(OldFriend.Quest, status: QuestStatus.Active);',
    'Speaker().Portrait(Portraits.Buyer);',
    'Behavior().Idle(States.Idle, Animations.LocomotionIdle);'
  ].join('\n'));
  assert(!validStaticDiagnostics.some(item => /ESL0610/.test(item.message)), 'EchoScript diagnostics should accept supported static symbols.');
  const inferredUnknownMemberDiagnostics = analyzeEchoScript([
    'private ActionDef PayMoney()',
    '{',
    '    return Action().Label("Pay");',
    '}',
    'private void Configure()',
    '{',
    '    var action = PayMoney();',
    '    action.Lable("Pay");',
    '    PayMoney().Lable("Pay");',
    '}'
  ].join('\n'));
  assert(inferredUnknownMemberDiagnostics.some(item => /ESL0602/.test(item.message) && /Lable/.test(item.message)), 'EchoScript diagnostics should report unknown members on local variables initialized from typed local methods.');
  assert(inferredUnknownMemberDiagnostics.some(item => /ESL0600/.test(item.message) && /Lable/.test(item.message)), 'EchoScript diagnostics should report unknown fluent methods on typed local method calls.');
  const namespaceFormatted = formatEchoScript('namespace story.chapter\n{\nclass Sample : EventResource\n{\n}\n}', {
    insertSpaces: true,
    tabSize: 4
  });
  assert(namespaceFormatted.startsWith('namespace story.chapter;'), 'EchoScript formatter should prefer file-scoped namespaces.');
  assert(!namespaceFormatted.includes('namespace story.chapter\n{'), 'EchoScript formatter should remove block-scoped namespace braces.');
  assert(signatureForWord('AreaEnter').includes('BoolStateRef'), 'EchoScript hover signatures should expose method arguments.');
  assert(signatureForWord('Move').includes('Npc.Move'), 'EchoScript hover signatures should include Npc.Move.');
  assert(signatureForWord('Move').includes('Npc.Move(npc: NpcRef, x: number, y: number, z: number'), 'EchoScript hover signatures should include canonical Npc.Move coordinate overload.');
  assert(signatureForWord('LineAfterMove').includes('LineAfterMove'), 'EchoScript hover signatures should include LineAfterMove.');
  assert(signatureForWord('Start').includes('Scene.Start'), 'EchoScript hover signatures should include overloaded Start methods.');
  assert(signatureForWord('Start').includes('TriggerResource.Init'), 'EchoScript hover signatures should include TriggerResource Start overloads.');
  assert(signatureForWord('Effects').includes('Choice(...).Effects'), 'EchoScript hover signatures should include rich Effects overloads.');
  assert(signatureForWord('StartEffects').includes('private Effect[] StartEffects'), 'EchoScript hover signatures should include StartEffects method.');
  assert(signatureForWord('Nodes').includes('private NodeDef[] Nodes'), 'EchoScript hover signatures should include Nodes method.');
  assert(signatureForWord('Nodes').includes('dialogueNodes'), 'EchoScript hover signatures should describe class-level dialogueNodes arrays.');
  assert(signatureForWord('AbortEffects').includes('private Effect[] AbortEffects'), 'EchoScript hover signatures should include Scene AbortEffects method.');
  assert(signatureForWord('BuildQuest').includes('BuildQuest'), 'EchoScript hover signatures should include BuildQuest.');
  assert(signatureForWord('BuildAnimation').includes('BuildAnimation'), 'EchoScript hover signatures should include BuildAnimation.');
  assert(signatureForWord('QuestStatus').includes('QuestStatus.Active'), 'EchoScript hover signatures should include QuestStatus static refs.');
  assert(signatureForWord('Layer').includes('Layer.Overlay'), 'EchoScript hover signatures should include Layer static refs.');
  assert(signatureForWord('Hand').includes('Hand.Main'), 'EchoScript hover signatures should include Hand static refs.');
  assert(signatureForWord('Items').includes('Items.SparkGrass'), 'EchoScript hover signatures should include item static refs.');
  assert(signatureForWord('Portraits').includes('Portraits.Buyer'), 'EchoScript hover signatures should include portrait static refs.');
  assert(signatureForWord('States').includes('States.Idle'), 'EchoScript hover signatures should include state-id static refs.');
  assert(signatureForWord('Spawn').includes('Npc.Spawn'), 'EchoScript hover signatures should include typed NPC spawn.');
  assert(signatureForWord('SpawnNearPlayer').includes('forwardOffset'), 'EchoScript hover signatures should include current SpawnNearPlayer offsets.');
  assert(signatureForWord('WalkTo').includes('Npc.WalkTo') && signatureForWord('WalkToPlayer').includes('Npc.WalkToPlayer'), 'EchoScript hover signatures should include NPC walk aliases.');
  assert(signatureForWord('Npc').includes('Npc.PlayAnimation') && signatureForWord('Npc').includes('Npc.HoldItem') && signatureForWord('Npc').includes('Npc.StandUp'), 'EchoScript hover signatures should include current NPC animation, item, and interaction helpers.');
  assert(signatureForWord('Path').includes('canFloat'), 'EchoScript hover signatures should include current Npc.Path options.');
  assert(signatureForWord('Vehicle').includes('Vehicle.Trip'), 'EchoScript hover signatures should include typed vehicle helpers.');
  assert(signatureForWord('Navigation').includes('SetTarget'), 'EchoScript hover signatures should include navigation helpers.');
  assert(signatureForWord('SetNavigationTarget').includes('World.SetNavigationTarget'), 'EchoScript hover signatures should include World navigation helpers.');
  assert(hoverSummaryForWord('Navigation').includes('GPS'), 'EchoScript hover summaries should describe navigation helpers.');
  assert(hoverSummaryForWord('SetTarget').includes('targetId'), 'EchoScript hover summaries should explain navigation target ids.');
  assert(hoverSummaryForWord('LineAfterMove').includes('Npc.Move') && hoverSummaryForWord('LineAfterMove').includes('завершения'), 'EchoScript hover summaries should explain LineAfterMove arrival gating in Russian.');
  assert(hoverSummaryForWord('FadeInTicks').length > 0, 'EchoScript hover summaries should provide fallback descriptions for existing hover methods.');
  assert(hoverSummaryForWord('Spawn').includes('Спавнит NPC'), 'EchoScript hover summaries should prefer Russian NPC Spawn docs.');
  assert(!hoverSummaryForWord('Spawn').startsWith('Spawns an NPC'), 'EchoScript hover summaries should not show the old English Spawn fallback first.');
  assert(hoverSummaryForWord('SpawnAt').includes('Спавнит NPC'), 'EchoScript hover summaries should describe SpawnAt in Russian.');
  assert(hoverSummaryForWord('Interact').includes('Npc.Interact(npc, x, y, z)') && hoverSummaryForWord('Interact').includes('BlockPosition'), 'EchoScript hover summaries should describe current NPC interaction overloads in Russian.');
  assert(hoverSummaryForWord('OnStart').includes('StoryResource'), 'EchoScript hover summaries should describe StoryResource OnStart.');
  assert(hoverSummaryForWord('Require').includes('проверяет переменную'), 'EchoScript hover summaries should describe story Require nodes in Russian.');
  assert(hoverSummaryForWord('Blink').includes('Sleep.Blink'), 'EchoScript hover summaries should describe Sleep.Blink.');
  assert(hoverSummaryForWord('OnTriggerEnter').includes('TriggerResource'), 'EchoScript hover summaries should describe trigger callbacks.');
  assert(hoverSummaryForWord('PlayerEnter').includes('игрок вошел'), 'EchoScript hover summaries should describe trigger events in Russian.');
  assert(hoverSummaryForWord('HasItem').includes('есть ли у игрока'), 'EchoScript hover summaries should describe trigger predicates in Russian.');
  assert(hoverSummaryForWord('SetBlack').includes('черным overlay'), 'EchoScript hover summaries should describe Screen.SetBlack in Russian.');
  assert(hoverSummaryForWord('SetPlayerName').includes('отображаемое имя'), 'EchoScript hover summaries should describe SetPlayerName in Russian.');
  assert(hoverSummaryForWord('Start').includes('TriggerResource.Init'), 'EchoScript hover summaries should describe TriggerResource Start overloads.');
  assert(hoverSummaryForWord('StartDialogue').includes('DialogueResource') && hoverSummaryForWord('DialogueStart').includes('Dialogue.Start'), 'EchoScript hover summaries should describe trigger dialogue aliases in Russian.');
  assert(hoverSummaryForWord('Debug').includes('Debug.Mark') && hoverSummaryForWord('Mark').includes('debug marker'), 'EchoScript hover summaries should describe Debug.Mark callback actions.');
  assert(hoverSummaryForWord('LookAt').includes('player actor') && hoverSummaryForWord('SetFatigue').includes('Player.SetFatigue'), 'EchoScript hover summaries should describe player actor and vitals helpers in Russian.');
  assert(hoverSummaryForWord('WakeUp').includes('SleepAt'), 'EchoScript hover summaries should describe Npc.WakeUp in Russian.');
  assert(hoverSummaryForWord('GoTo').includes('Dialogue.GoTo') && hoverSummaryForWord('SetForm').includes('NpcFormRef'), 'EchoScript hover summaries should describe dialogue transition and NPC form helpers.');
  assert(hoverSummaryForWord('SetBlinkTiming').includes('periodTicks') && hoverSummaryForWord('WaitPrepared').includes('Cutscene.WaitPrepared'), 'EchoScript hover summaries should describe blink timing and cutscene helpers.');
  assert(hoverSummaryForWord('Environment').includes('EnvironmentResource') && hoverSummaryForWord('SetBlock').includes('blockId'), 'EchoScript hover summaries should describe environment resources and block changes in Russian.');
  assert(hoverSummaryForWord('DefaultForm').includes('NpcFormResource') && hoverSummaryForWord('Form').includes('NpcFormResource'), 'EchoScript hover summaries should describe NPC form resources in Russian.');
  assert(hoverSummaryForWord('Skin').includes('NPC skin') && hoverSummaryForWord('NameColor').includes('Npc(') && hoverSummaryForWord('nameColor').includes('hex'), 'EchoScript hover summaries should describe skin and NPC name-color helpers in Russian.');
  assert(hoverSummaryForWord('ApplyToPlayer').includes('intensity') && hoverSummaryForWord('Tint').includes('opacity'), 'EchoScript hover summaries should describe Echo Effects player and screen methods.');
  assert(hoverSummaryForWord('WearableResource').includes('wardrobe') && hoverSummaryForWord('PortraitStrip').includes('BASE'), 'EchoScript hover summaries should describe wardrobe resources and portrait strips.');
  assert(hoverSummaryForWord('TextureArea').includes('face DSL') && hoverSummaryForWord('Timing').includes('Blink'), 'EchoScript hover summaries should describe high-level face DSL methods.');
  assert(hoverSummaryForWord('FacialAnimationSurface3d').includes('3D') && hoverSummaryForWord('FacialAnimationSurfacePortrait').includes('portrait'), 'EchoScript hover summaries should describe facial animation surface base classes.');
  assert(hoverSummaryForWord('framing').includes('CameraTrack') && hoverSummaryForWord('key').includes('timeMillis'), 'EchoScript hover summaries should describe camera-track text DSL keywords.');
  assert(hoverSummaryForWord('WalkTo').includes('Npc.MoveTo') && hoverSummaryForWord('WalkToPlayer').includes('Npc.MoveToPlayer'), 'EchoScript hover summaries should describe NPC walk aliases in Russian.');
  assert(hoverSummaryForWord('BlockPosition').includes('Npc.Interact') && hoverSummaryForWord('Rotation').includes('Frame') && hoverSummaryForWord('Scale').includes('Frame'), 'EchoScript hover summaries should describe block positions and frame transform modifiers.');
  assert(signatureForWord('Player').includes('Player.Pose'), 'EchoScript hover signatures should include player pose helpers.');
  assert(signatureForWord('Player').includes('Player.Move') && signatureForWord('Player').includes('Player.SetFatigue'), 'EchoScript hover signatures should include player actor and vitals helpers.');
  assert(signatureForWord('LookAt').includes('Player.LookAt') && signatureForWord('SetFatigue').includes('Player.SetFatigue'), 'EchoScript hover signatures should include individual player command helpers.');
  assert(signatureForWord('WakeUp').includes('Npc.WakeUp'), 'EchoScript hover signatures should include Npc.WakeUp.');
  assert(signatureForWord('StandUp').includes('Player.StandUp') && signatureForWord('StandUp').includes('Npc.StandUp'), 'EchoScript hover signatures should include player and NPC StandUp helpers.');
  assert(signatureForWord('GoTo').includes('Dialogue.GoTo') && signatureForWord('SetForm').includes('Npc.SetForm'), 'EchoScript hover signatures should include dialogue transition and NPC form helpers.');
  assert(signatureForWord('ClearFaceAnimation').includes('DialogueCommand') && signatureForWord('SetBlinkTiming').includes('periodTicks'), 'EchoScript hover signatures should include dialogue face-animation command helpers.');
  assert(signatureForWord('Cutscene').includes('Cutscene.WaitPrepared') && signatureForWord('WaitPrepared').includes('Cutscene.WaitPrepared'), 'EchoScript hover signatures should include cutscene command helpers.');
  assert(signatureForWord('ActionKey').includes('Action().ActionKey'), 'EchoScript hover signatures should include interaction action keys.');
  assert(signatureForWord('CooldownTicks').includes('Event().CooldownTicks'), 'EchoScript hover signatures should include event cooldown helpers.');
  assert(signatureForWord('InventoryHasItem').includes('TriggerDef'), 'EchoScript hover signatures should include inventory triggers.');
  assert(signatureForWord('DurationTicks').includes('Camera().DurationTicks'), 'EchoScript hover signatures should include camera duration helpers.');
  assert(signatureForWord('TalkDistance').includes('Speaker().TalkDistance'), 'EchoScript hover signatures should include speaker talk distance.');
  assert(signatureForWord('EnterVehicleAt').includes('Npc.EnterVehicleAt'), 'EchoScript hover signatures should include NPC vehicle helpers.');
  assert(signatureForWord('FadeIn').includes('Camera().FadeIn'), 'EchoScript hover signatures should include camera methods.');
  assert(signatureForWord('Story').includes('StoryResourceDef') && signatureForWord('Story').includes('Story.Load'), 'EchoScript hover signatures should include Story(...) modular resources and loaders.');
  assert(signatureForWord('Environment').includes('EnvironmentResourceDef') && signatureForWord('Environment').includes('Environment.Set'), 'EchoScript hover signatures should include Environment resources and setters.');
  assert(signatureForWord('Npc').includes('defaultForm') && signatureForWord('Npc').includes('nameColor'), 'EchoScript hover signatures should include current NPC definition arguments.');
  assert(signatureForWord('NameColor').includes('NpcNameColor') && signatureForWord('nameColor').includes('NpcNameColor'), 'EchoScript hover signatures should include NPC name-color helpers.');
  assert(signatureForWord('Form').includes('NpcFormResourceDef') && signatureForWord('DefaultForm').includes('NpcForm'), 'EchoScript hover signatures should include NPC form resources.');
  assert(signatureForWord('Sleep').includes('Sleep.EnterDream') && signatureForWord('Blackspace').includes('Blackspace.Create'), 'EchoScript hover signatures should include sleep and blackspace helpers.');
  assert(signatureForWord('Sleep').includes('Sleep.Blink') && signatureForWord('Blink').includes('inside OnStart'), 'EchoScript hover signatures should include Sleep.Blink.');
  assert(signatureForWord('Complete').includes('Sleep.Complete'), 'EchoScript hover signatures should include Sleep.Complete overloads.');
  assert(signatureForWord('SpawnAt').includes('Npc.SpawnAt') && signatureForWord('SetBlack').includes('Screen.SetBlack'), 'EchoScript hover signatures should include new spawn and screen helpers.');
  assert(signatureForWord('Action').includes('StoryNodeDef') && signatureForWord('Action').includes('ActionDef'), 'EchoScript hover signatures should cover modular graph actions and legacy action defs.');
  assert(signatureForWord('AnimationProfile').includes('AnimationProfileDef'), 'EchoScript hover signatures should include AnimationProfile resources.');
  assert(signatureForWord('AnimationClip').includes('AnimationClipDef'), 'EchoScript hover signatures should include AnimationClip resources.');
  assert(signatureForWord('FacialAnimation').includes('FacialAnimationDef'), 'EchoScript hover signatures should include FacialAnimation resources.');
  assert(signatureForWord('NpcAnimationBinding').includes('NpcAnimationBindingDef'), 'EchoScript hover signatures should include NPC animation bindings.');
  assert(signatureForWord('SetAnimationFace').includes('Line(...).SetAnimationFace'), 'EchoScript hover signatures should include current line face-animation helpers.');
  assert(signatureForWord('PlayerProfile').includes('PlayerProfileTemplateId'), 'EchoScript hover signatures should include player profile templates.');
  assert(signatureForWord('Weapon').includes('WeaponDefinition'), 'EchoScript hover signatures should include weapon resources.');
  assert(signatureForWord('TriggerResource').includes('private void Init'), 'EchoScript hover signatures should include trigger resources.');
  assert(signatureForWord('FireOnlyOncePerPlayer').includes('TriggerResource.Init'), 'EchoScript hover signatures should include trigger FireOnlyOncePerPlayer alias.');
  assert(signatureForWord('StoryResource').includes('StoryResourceDef') && signatureForWord('DialogueResource').includes('DialogueResourceDef') && signatureForWord('AnimationResource').includes('AnimationDef'), 'EchoScript hover signatures should include base resource class types.');
  assert(signatureForWord('NpcResource').includes('NpcResourceDef') && signatureForWord('FaceRigResource').includes('FaceRigDef') && signatureForWord('PlayerProfileTemplate').includes('PlayerProfile') && signatureForWord('WeaponResource').includes('WeaponDefinition'), 'EchoScript hover signatures should include NPC, face, player, and weapon resource base types.');
  assert(signatureForWord('NodeDef').includes('dialogueNodes') && signatureForWord('EnvironmentStateDef').includes('EnvironmentChangeDef') && signatureForWord('EnvironmentChangeDef').includes('SetBlock'), 'EchoScript hover signatures should include common EchoScript authoring return types.');
  assert(signatureForWord('PlayerRef').includes('OnTriggerEnter') && signatureForWord('TriggerEvent').includes('OnTriggerEnter') && signatureForWord('NpcAnimationBindingId').includes('NpcAnimationBinding'), 'EchoScript hover signatures should include common EchoScript authoring reference types.');
  assert(signatureForWord('Upper').includes('Face DSL') && signatureForWord('Left').includes('Face DSL'), 'EchoScript hover signatures should include face DSL side tokens.');
  assert(signatureForWord('PlayerSleep').includes('PlayerSleep.Confirmed'), 'EchoScript hover signatures should include PlayerSleep selectors.');
  assert(signatureForWord('StartStory').includes('StoryId'), 'EchoScript hover signatures should include trigger StartStory.');
  assert(signatureForWord('StartDialogue').includes('DialogueId') && signatureForWord('DialogueStart').includes('TriggerResource callbacks'), 'EchoScript hover signatures should include trigger dialogue start aliases.');
  assert(signatureForWord('Debug').includes('Debug.Mark') && signatureForWord('Mark').includes('TriggerResource callbacks'), 'EchoScript hover signatures should include Debug.Mark callback actions.');
  assert(signatureForWord('OnStart').includes('OnStart'), 'EchoScript hover signatures should include StoryResource OnStart.');
  assert(signatureForWord('Require').includes('StoryNodeDef'), 'EchoScript hover signatures should include story Require nodes.');
  assert(signatureForWord('SetPlayerName').includes('SetPlayerName'), 'EchoScript hover signatures should include player dialogue-side SetPlayerName.');
  assert(signatureForWord('OnTriggerEnter').includes('TriggerEvent'), 'EchoScript hover signatures should include trigger callback overloads.');
  assert(signatureForWord('PlayerEnter').includes('TriggerExpression'), 'EchoScript hover signatures should include trigger events.');
  assert(signatureForWord('WeatherChanged').includes('TriggerExpression'), 'EchoScript hover signatures should include trigger event coverage.');
  assert(signatureForWord('Effect').includes('Effect.ApplyToPlayer') && signatureForWord('Duration').includes('InfiniteDuration'), 'EchoScript hover signatures should include Echo Effects resources and commands.');
  assert(signatureForWord('Movement').includes('SpeedMultiplier') && signatureForWord('Screen').includes('Screen.Tint'), 'EchoScript hover signatures should include Echo Effects channel methods.');
  assert(signatureForWord('VanillaEffect').includes('VanillaEffect.Apply'), 'EchoScript hover signatures should include Echo Effects vanilla effect methods.');
  assert(signatureForWord('WearableResource').includes('WearableResource') && signatureForWord('Match').includes('face'), 'EchoScript hover signatures should include wardrobe resource methods.');
  assert(signatureForWord('FaceRig').includes('BlinkControl') && signatureForWord('TextureArea').includes('width'), 'EchoScript hover signatures should include face rig DSL methods.');
  assert(signatureForWord('FacialAnimationSurface3d').includes('FacialAnimationSurface3d') && signatureForWord('FacialAnimationSurfacePortrait').includes('FacialAnimationSurfacePortrait'), 'EchoScript hover signatures should include facial animation surface base classes.');
  assert(signatureForWord('schema').includes('CameraTrack') && signatureForWord('target').includes('BETWEEN'), 'EchoScript hover signatures should include camera-track text DSL keywords.');
  assert(signatureForWord('BlockPosition').includes('BlockPosition') && signatureForWord('Rotation').includes('Frame') && signatureForWord('Scale').includes('Frame'), 'EchoScript hover signatures should include block positions and frame transform modifiers.');
  assert(signatureForWord('SeatZoneResource').includes('SeatZoneResource') && signatureForWord('Box').includes('SeatZoneBox'), 'EchoScript hover signatures should include Echo Seating resources.');
  assert(signatureForWord('Slot').includes('SeatSlotId') && signatureForWord('MatchRadius').includes('SeatZoneResource'), 'EchoScript hover signatures should include Echo Seating slot helpers.');
  assert(signatureForWord('SeatedAt').includes('Player.SeatedAt') && signatureForWord('ChangedSeatIn').includes('SeatZoneId'), 'EchoScript hover signatures should include Echo Seating trigger selectors.');
  assert(signatureForWord('Presentation').includes('EffectResource') && signatureForWord('PlayerHud').includes('referenceWidth'), 'EchoScript hover signatures should include Effect presentation and Player HUD methods.');
  assert(signatureForWord('Bar').includes('gradientStart') && signatureForWord('Description').includes('WeaponResource') && signatureForWord('Glint').includes('WeaponResource'), 'EchoScript hover signatures should include HUD bars and WeaponResource helper methods.');
  assert(signatureForWord('DialogueOverlayStyle').includes('typewriterMsPerCharacter') && signatureForWord('PlayerFatigue').includes('exhaustedNausea'), 'EchoScript hover signatures should include dialogue overlay and player vitals config methods.');
  assert(signatureForWord('DialoguePanelBackgroundOverlay').includes('textureWidth') && signatureForWord('DialogueChoiceBackgroundOverlay').includes('fillDirection'), 'EchoScript hover signatures should include dialogue background overlay config calls.');
  assert(signatureForWord('PortraitOverlay').includes('facePixels') && signatureForWord('Layer').includes('placeX'), 'EchoScript hover signatures should include portrait overlay config methods.');
  assert(hoverSummaryForWord('Story').includes('StoryResource') && hoverSummaryForWord('Story').includes('стартовый node'), 'EchoScript hover summaries should describe modular story resources in Russian.');
  assert(hoverSummaryForWord('SeatZoneResource').includes('Echo Seating') && hoverSummaryForWord('Slot').includes('SeatZoneResource'), 'EchoScript hover summaries should describe Echo Seating methods in Russian.');
  assert(hoverSummaryForWord('UnknownSeats').includes('разрешить') && hoverSummaryForWord('MatchRadius').includes('радиус'), 'EchoScript hover summaries should describe Echo Seating policies in Russian.');
  assert(hoverSummaryForWord('SeatedAt').includes('конкретный Slot') && hoverSummaryForWord('ChangedSeatIn').includes('сменил место'), 'EchoScript hover summaries should describe Echo Seating trigger selectors in Russian.');
  assert(hoverSummaryForWord('FireOnlyOncePerPlayer').includes('FireOnlyOnce'), 'EchoScript hover summaries should describe the FireOnlyOncePerPlayer alias.');
  assert(hoverSummaryForWord('StoryResource').includes('Echo Story') && hoverSummaryForWord('DialogueResource').includes('Echo Dialogue') && hoverSummaryForWord('AnimationClipResource').includes('AnimationClip'), 'EchoScript hover summaries should describe base resource classes in Russian.');
  assert(hoverSummaryForWord('NpcResource').includes('Echo NPC') && hoverSummaryForWord('FaceRigResource').includes('face rig') && hoverSummaryForWord('WeaponResource').includes('оружия'), 'EchoScript hover summaries should describe NPC, face, and weapon base classes in Russian.');
  assert(hoverSummaryForWord('NodeDef').includes('dialogue node') && hoverSummaryForWord('EnvironmentChangeDef').includes('SetBlock') && hoverSummaryForWord('NpcAnimationBindingId').includes('NpcAnimationBinding'), 'EchoScript hover summaries should describe common EchoScript authoring types.');
  assert(hoverSummaryForWord('PlayerRef').includes('OnTriggerEnter') && hoverSummaryForWord('TriggerEvent').includes('payload') && hoverSummaryForWord('Left').includes('Face DSL') && hoverSummaryForWord('Upper').includes('веко'), 'EchoScript hover summaries should describe trigger and face DSL authoring tokens.');
  assert(hoverSummaryForWord('Presentation').includes('nameKey') && hoverSummaryForWord('VanillaEffect').includes('MobEffect'), 'EchoScript hover summaries should describe effect presentation and vanilla effects.');
  assert(hoverSummaryForWord('PlayerHudConfigResource').includes('Echo Player HUD'), 'EchoScript hover summaries should describe player HUD resources in Russian.');
  assert(hoverSummaryForWord('Bar').includes('HUD bar') && hoverSummaryForWord('Description').includes('WeaponResource'), 'EchoScript hover summaries should describe HUD bars and weapon helpers in Russian.');
  assert(hoverSummaryForWord('DialogueOverlayStyle').includes('typewriter') && hoverSummaryForWord('PlayerFatigue').includes('fatigue'), 'EchoScript hover summaries should describe overlay style and vitals config in Russian.');
  assert(hoverSummaryForWord('DialoguePanelBackgroundOverlay').includes('dialogue panel') && hoverSummaryForWord('DialogueChoiceBackgroundOverlay').includes('choice panel'), 'EchoScript hover summaries should describe dialogue background overlay config calls.');
  assert(hoverSummaryForWord('PortraitOverlay').includes('portrait overlay') && hoverSummaryForWord('Layer').includes('PortraitOverlay'), 'EchoScript hover summaries should describe portrait overlay methods in Russian.');
  assert(signatureForWord('exists').includes('StateRef'), 'EchoScript hover signatures should include condition helpers.');
  const wordsWithHover = hoverWords();
  assert(wordsWithHover.length > 200, 'EchoScript hover registry should expose all known method words.');
  const missingSummaries = wordsWithHover.filter(word => !hoverSummaryForWord(word));
  assertEqual(missingSummaries.join(', '), '', 'Every EchoScript hover method should have a short purpose summary.');
}

function verifyEchoScriptSignatureHelp() {
  assert(signatureHelpTargets().includes('Npc.Spawn'), 'EchoScript signature help should include Npc.Spawn.');
  assert(signatureHelpTargets().includes('LineAfterMove'), 'EchoScript signature help should include LineAfterMove.');
  assert(signatureHelpTargets().includes('Navigation.SetTarget'), 'EchoScript signature help should include navigation targets.');
  assert(signatureHelpTargets().includes('Story'), 'EchoScript signature help should include Story(...) resources.');
  assert(signatureHelpTargets().includes('Action'), 'EchoScript signature help should include story graph Action(...) nodes.');
  assert(signatureHelpTargets().includes('AnimationClip'), 'EchoScript signature help should include AnimationClip(...) resources.');
  assert(signatureHelpTargets().includes('NpcAnimationBinding'), 'EchoScript signature help should include NpcAnimationBinding(...) resources.');
  assert(signatureHelpTargets().includes('Environment.Set'), 'EchoScript signature help should include Environment.Set.');
  assert(signatureHelpTargets().includes('Story.Load'), 'EchoScript signature help should include Story.Load.');
  assert(signatureHelpTargets().includes('Form'), 'EchoScript signature help should include Form(...) NPC form resources.');
  assert(signatureHelpTargets().includes('BlockPosition') && signatureHelpTargets().includes('Rotation') && signatureHelpTargets().includes('Scale'), 'EchoScript signature help should include block position and frame transform helpers.');
  assert(signatureHelpTargets().includes('Npc.SpawnAt'), 'EchoScript signature help should include Npc.SpawnAt.');
  assert(signatureHelpTargets().includes('Npc.Move'), 'EchoScript signature help should include Npc.Move.');
  assert(signatureHelpTargets().includes('Npc.WakeUp'), 'EchoScript signature help should include Npc.WakeUp.');
  assert(signatureHelpTargets().includes('Player.Move') && signatureHelpTargets().includes('Player.SetFatigue'), 'EchoScript signature help should include player actor and vitals helpers.');
  assert(signatureHelpTargets().includes('GoTo') && signatureHelpTargets().includes('Formerly') && signatureHelpTargets().includes('Dialogue.GoTo') && signatureHelpTargets().includes('Npc.SetForm'), 'EchoScript signature help should include high-level/legacy dialogue transition and migration helpers.');
  assert(signatureHelpTargets().includes('Cutscene.WaitPrepared') && signatureHelpTargets().includes('SetBlinkTiming'), 'EchoScript signature help should include cutscene and blink timing helpers.');
  assert(signatureHelpTargets().includes('Sleep.EnterDream'), 'EchoScript signature help should include Sleep.EnterDream.');
  assert(signatureHelpTargets().includes('Sleep.Blink'), 'EchoScript signature help should include Sleep.Blink.');
  assert(signatureHelpTargets().includes('PlayerProfile'), 'EchoScript signature help should include PlayerProfile.');
  assert(signatureHelpTargets().includes('NameColor') && signatureHelpTargets().includes('nameColor'), 'EchoScript signature help should include NPC name-color helpers.');
  assert(signatureHelpTargets().includes('Weapon'), 'EchoScript signature help should include Weapon.');
  assert(signatureHelpTargets().includes('PlayerSleep.Confirmed'), 'EchoScript signature help should include PlayerSleep selectors.');
  assert(signatureHelpTargets().includes('PlayerEnter'), 'EchoScript signature help should include trigger events.');
  assert(signatureHelpTargets().includes('StartStory') && signatureHelpTargets().includes('StartDialogue') && signatureHelpTargets().includes('DialogueStart'), 'EchoScript signature help should include trigger StartStory and dialogue aliases.');
  assert(signatureHelpTargets().includes('Dialogue.Start') && signatureHelpTargets().includes('Npc.StandUp') && signatureHelpTargets().includes('Debug.Mark'), 'EchoScript signature help should include callback action methods.');
  assert(signatureHelpTargets().includes('Effect.ApplyToPlayer') && signatureHelpTargets().includes('Screen.Tint'), 'EchoScript signature help should include Echo Effects methods.');
  assert(signatureHelpTargets().includes('VanillaEffect.Apply'), 'EchoScript signature help should include Echo Effects vanilla effect methods.');
  assert(signatureHelpTargets().includes('Movement.SpeedMultiplier') && signatureHelpTargets().includes('Sound.PlayAt'), 'EchoScript signature help should include Echo Effects channel methods.');
  assert(signatureHelpTargets().includes('PortraitStrip') && signatureHelpTargets().includes('TextureArea') && signatureHelpTargets().includes('BlinkControl'), 'EchoScript signature help should include wardrobe and face DSL methods.');
  assert(signatureHelpTargets().includes('FacialAnimationSurface3d') && signatureHelpTargets().includes('FacialAnimationSurfacePortrait'), 'EchoScript signature help should include facial animation surface base classes.');
  assert(signatureHelpTargets().includes('SeatZoneResource') && signatureHelpTargets().includes('Box') && signatureHelpTargets().includes('MatchRadius'), 'EchoScript signature help should include Echo Seating methods.');
  assert(signatureHelpTargets().includes('Detect') && signatureHelpTargets().includes('UnknownSeats'), 'EchoScript signature help should include Echo Seating policy methods.');
  assert(signatureHelpTargets().includes('Player.SeatedAt') && signatureHelpTargets().includes('Player.ChangedSeatIn'), 'EchoScript signature help should include Echo Seating trigger selectors.');
  assert(signatureHelpTargets().includes('FireOnlyOncePerPlayer'), 'EchoScript signature help should include TriggerResource FireOnlyOncePerPlayer alias.');
  assert(signatureHelpTargets().includes('Presentation') && signatureHelpTargets().includes('PlayerHud') && signatureHelpTargets().includes('Bar'), 'EchoScript signature help should include effect presentation and Player HUD methods.');
  assert(signatureHelpTargets().includes('Description') && signatureHelpTargets().includes('Glint') && signatureHelpTargets().includes('CreativeOrder'), 'EchoScript signature help should include WeaponResource helper methods.');
  assert(signatureHelpTargets().includes('DialogueOverlayStyle') && signatureHelpTargets().includes('PlayerFatigue') && signatureHelpTargets().includes('PortraitOverlay'), 'EchoScript signature help should include overlay, vitals, and portrait config methods.');
  assert(signatureHelpTargets().includes('DialoguePanelBackgroundOverlay') && signatureHelpTargets().includes('DialogueChoiceBackgroundOverlay'), 'EchoScript signature help should include dialogue background overlay config calls.');
  const spawnSecondArg = echoScriptSignatureHelp('private Effect[] StartEffects() { Npc.Spawn(Ben, ); }', 'private Effect[] StartEffects() { Npc.Spawn(Ben, '.length);
  assert(spawnSecondArg && spawnSecondArg.target === 'Npc.Spawn', 'Signature help should detect Npc.Spawn call target.');
  assertEqual(spawnSecondArg.activeParameter, 1, 'Npc.Spawn signature help should focus the dimension argument after the first comma.');
  assert(spawnSecondArg.signatures.some(item => item.label.includes('x: int') && item.label.includes('yaw?: number')), 'Npc.Spawn signature help should expose compiler-supported coordinate arguments.');
  assert(spawnSecondArg.signatures[0].documentation.includes('Спавнит NPC'), 'Npc.Spawn signature help should use Russian documentation.');

  const spawnCoordinateArg = echoScriptSignatureHelp('Npc.Spawn(Ben, Overworld, 10, ', 'Npc.Spawn(Ben, Overworld, 10, '.length);
  assert(spawnCoordinateArg && spawnCoordinateArg.activeSignature === 0, 'Npc.Spawn signature help should use the compiler-supported coordinate signature.');
  assertEqual(spawnCoordinateArg.activeParameter, 3, 'Npc.Spawn coordinate overload should focus y after x comma.');

  const navigationProfileArg = echoScriptSignatureHelp('Navigation.SetTarget("quest.main_gate", x: 1, y: 2, z: 3, profile: ', 'Navigation.SetTarget("quest.main_gate", x: 1, y: 2, z: 3, profile: '.length);
  assert(navigationProfileArg && navigationProfileArg.target === 'Navigation.SetTarget', 'Signature help should detect Navigation.SetTarget.');
  assert(navigationProfileArg.signatures[0].documentation.includes('GPS'), 'Navigation signature help should explain GPS target usage.');

  const actionAnimationArg = echoScriptSignatureHelp('Action("play_wave", type: "echo_animation:play_animation", animation: ', 'Action("play_wave", type: "echo_animation:play_animation", animation: '.length);
  assert(actionAnimationArg && actionAnimationArg.target === 'Action', 'Signature help should detect modular story Action nodes.');
  assert(actionAnimationArg.signatures[0].documentation.includes('echo_animation:play_animation'), 'Action signature help should mention modular engine adapters.');

  const bindingProfileArg = echoScriptSignatureHelp('NpcAnimationBinding("jdh:binding", npc: "jdh:john_father", profile: ', 'NpcAnimationBinding("jdh:binding", npc: "jdh:john_father", profile: '.length);
  assert(bindingProfileArg && bindingProfileArg.target === 'NpcAnimationBinding', 'Signature help should detect NpcAnimationBinding.');
  assertEqual(bindingProfileArg.activeParameter, 3, 'NpcAnimationBinding signature help should focus profile named argument.');

  const clipRigArg = echoScriptSignatureHelp('AnimationClip("jdh:wave_loop", rig: ', 'AnimationClip("jdh:wave_loop", rig: '.length);
  assert(clipRigArg && clipRigArg.target === 'AnimationClip', 'Signature help should detect AnimationClip.');
  assertEqual(clipRigArg.activeParameter, 1, 'AnimationClip signature help should focus rig named argument.');

  const lineFaceArg = echoScriptSignatureHelp('Line("father", "Hi").SetAnimationFace(', 'Line("father", "Hi").SetAnimationFace('.length);
  assert(lineFaceArg && lineFaceArg.target === 'SetAnimationFace', 'Signature help should detect Line().SetAnimationFace.');
  const lineAfterMoveArg = echoScriptSignatureHelp('LineAfterMove("father", ', 'LineAfterMove("father", '.length);
  assert(lineAfterMoveArg && lineAfterMoveArg.target === 'LineAfterMove', 'Signature help should detect LineAfterMove call target.');
  assertEqual(lineAfterMoveArg.activeParameter, 1, 'LineAfterMove signature help should focus the text argument after the first comma.');

  const environmentSetArg = echoScriptSignatureHelp('Environment.Set(Environments.KitchenTable, ', 'Environment.Set(Environments.KitchenTable, '.length);
  assert(environmentSetArg && environmentSetArg.target === 'Environment.Set', 'Signature help should detect Environment.Set.');
  assertEqual(environmentSetArg.activeParameter, 1, 'Environment.Set signature help should focus stateId after the first comma.');

  const formArg = echoScriptSignatureHelp('Form(id: ', 'Form(id: '.length);
  assert(formArg && formArg.target === 'Form', 'Signature help should detect Form resources.');
  assertEqual(formArg.activeParameter, 0, 'Form signature help should focus the id named argument.');

  const nameColorArg = echoScriptSignatureHelp('Npc(id: "jdh:father", display: "Father", nameColor: NameColor(', 'Npc(id: "jdh:father", display: "Father", nameColor: NameColor('.length);
  assert(nameColorArg && nameColorArg.target === 'NameColor', 'Signature help should detect NPC NameColor helper.');
  assertEqual(nameColorArg.activeParameter, 0, 'NameColor signature help should focus the color argument.');

  const sleepDreamArg = echoScriptSignatureHelp('Sleep.EnterDream(target: "dream", x: 1, y: ', 'Sleep.EnterDream(target: "dream", x: 1, y: '.length);
  assert(sleepDreamArg && sleepDreamArg.target === 'Sleep.EnterDream', 'Signature help should detect Sleep.EnterDream.');
  assertEqual(sleepDreamArg.activeParameter, 6, 'Sleep.EnterDream signature help should focus named y coordinate.');

  const profileArg = echoScriptSignatureHelp('PlayerProfile(id: "jdh:player/default", skinMode: ', 'PlayerProfile(id: "jdh:player/default", skinMode: '.length);
  assert(profileArg && profileArg.target === 'PlayerProfile', 'Signature help should detect PlayerProfile resources.');
  assertEqual(profileArg.activeParameter, 5, 'PlayerProfile signature help should focus skinMode named argument.');

  const weaponArg = echoScriptSignatureHelp('Weapon(id: "jdh:spark_sword", name: "Spark Sword", texture: ', 'Weapon(id: "jdh:spark_sword", name: "Spark Sword", texture: '.length);
  assert(weaponArg && weaponArg.target === 'Weapon', 'Signature help should detect Weapon resources.');
  assertEqual(weaponArg.activeParameter, 2, 'Weapon signature help should focus texture named argument.');
  const seatSlotArg = echoScriptSignatureHelp('Slot(Chair, ', 'Slot(Chair, '.length);
  assert(seatSlotArg && seatSlotArg.target === 'Slot', 'Signature help should detect SeatZone Slot calls.');
  assertEqual(seatSlotArg.activeParameter, 1, 'SeatZone Slot signature help should focus anchor argument.');
  const matchRadiusArg = echoScriptSignatureHelp('MatchRadius(', 'MatchRadius('.length);
  assert(matchRadiusArg && matchRadiusArg.target === 'MatchRadius', 'Signature help should detect MatchRadius.');

  const seatedAtArg = echoScriptSignatureHelp('Subscribe(Player.SeatedAt(BreakfastTable, ', 'Subscribe(Player.SeatedAt(BreakfastTable, '.length);
  assert(seatedAtArg && seatedAtArg.target === 'Player.SeatedAt', 'Signature help should detect Player.SeatedAt seating selectors.');
  assertEqual(seatedAtArg.activeParameter, 1, 'Player.SeatedAt signature help should focus slot after the zone comma.');

  const changedSeatArg = echoScriptSignatureHelp('Subscribe(Player.ChangedSeatIn(', 'Subscribe(Player.ChangedSeatIn('.length);
  assert(changedSeatArg && changedSeatArg.target === 'Player.ChangedSeatIn', 'Signature help should detect Player.ChangedSeatIn seating selectors.');

  const presentationArg = echoScriptSignatureHelp('Presentation(nameKey: ', 'Presentation(nameKey: '.length);
  assert(presentationArg && presentationArg.target === 'Presentation', 'Signature help should detect Presentation.');
  assertEqual(presentationArg.activeParameter, 0, 'Presentation signature help should focus nameKey.');

  const playerHudArg = echoScriptSignatureHelp('PlayerHud(id: "jdh:hud", referenceWidth: ', 'PlayerHud(id: "jdh:hud", referenceWidth: '.length);
  assert(playerHudArg && playerHudArg.target === 'PlayerHud', 'Signature help should detect PlayerHud.');
  assertEqual(playerHudArg.activeParameter, 1, 'PlayerHud signature help should focus referenceWidth.');

  const barArg = echoScriptSignatureHelp('Bar(id: "bar", source: "health", width: ', 'Bar(id: "bar", source: "health", width: '.length);
  assert(barArg && barArg.target === 'Bar', 'Signature help should detect Bar.');
  assertEqual(barArg.activeParameter, 2, 'Bar signature help should focus width named argument.');

  const weaponHelperArg = echoScriptSignatureHelp('Description(', 'Description('.length);
  assert(weaponHelperArg && weaponHelperArg.target === 'Description', 'Signature help should detect WeaponResource local helpers.');

  const overlayStyleArg = echoScriptSignatureHelp('DialogueOverlayStyle(textScale: "1.0", typewriterMsPerCharacter: ', 'DialogueOverlayStyle(textScale: "1.0", typewriterMsPerCharacter: '.length);
  assert(overlayStyleArg && overlayStyleArg.target === 'DialogueOverlayStyle', 'Signature help should detect DialogueOverlayStyle.');
  assertEqual(overlayStyleArg.activeParameter, 3, 'DialogueOverlayStyle signature help should focus typewriterMsPerCharacter.');

  const panelBackgroundArg = echoScriptSignatureHelp('DialoguePanelBackgroundOverlay(mode: "sliced", textureWidth: ', 'DialoguePanelBackgroundOverlay(mode: "sliced", textureWidth: '.length);
  assert(panelBackgroundArg && panelBackgroundArg.target === 'DialoguePanelBackgroundOverlay', 'Signature help should detect DialoguePanelBackgroundOverlay.');
  assertEqual(panelBackgroundArg.activeParameter, 2, 'DialoguePanelBackgroundOverlay signature help should focus textureWidth named argument.');

  const fatigueConfigArg = echoScriptSignatureHelp('PlayerFatigue(id: "jdh:player_fatigue", maximum: 100, exhaustedNausea: ', 'PlayerFatigue(id: "jdh:player_fatigue", maximum: 100, exhaustedNausea: '.length);
  assert(fatigueConfigArg && fatigueConfigArg.target === 'PlayerFatigue', 'Signature help should detect PlayerFatigue.');
  assertEqual(fatigueConfigArg.activeParameter, 8, 'PlayerFatigue signature help should focus exhaustedNausea.');

  const portraitOverlayArg = echoScriptSignatureHelp('PortraitOverlay("jdh:john_father", facePixels: ', 'PortraitOverlay("jdh:john_father", facePixels: '.length);
  assert(portraitOverlayArg && portraitOverlayArg.target === 'PortraitOverlay', 'Signature help should detect PortraitOverlay.');
  assertEqual(portraitOverlayArg.activeParameter, 5, 'PortraitOverlay signature help should focus facePixels.');

  const portraitLayerArg = echoScriptSignatureHelp('Layer("pupil_left", placeX: ', 'Layer("pupil_left", placeX: '.length);
  assert(portraitLayerArg && portraitLayerArg.target === 'Layer', 'Signature help should detect portrait Layer.');
  assert(portraitLayerArg.signatures.some(item => item.label.includes('placeX')), 'Layer signature help should include portrait overlay overload.');

  const playerSleepArg = echoScriptSignatureHelp('Subscribe(PlayerSleep.Confirmed("chapter_1", sleepCountAtLeast: ', 'Subscribe(PlayerSleep.Confirmed("chapter_1", sleepCountAtLeast: '.length);
  assert(playerSleepArg && playerSleepArg.target === 'PlayerSleep.Confirmed', 'Signature help should detect PlayerSleep member selectors.');
  assertEqual(playerSleepArg.activeParameter, 2, 'PlayerSleep selector signature help should focus sleepCountAtLeast.');

  const waitArg = echoScriptSignatureHelp('Wait(PlayerEnter(', 'Wait(PlayerEnter('.length);
  assert(waitArg && waitArg.target === 'PlayerEnter', 'Signature help should detect trigger event expressions.');

  const startStoryArg = echoScriptSignatureHelp('StartStory(', 'StartStory('.length);
  assert(startStoryArg && startStoryArg.target === 'StartStory', 'Signature help should detect StartStory.');

  const startDialogueArg = echoScriptSignatureHelp('StartDialogue(', 'StartDialogue('.length);
  assert(startDialogueArg && startDialogueArg.target === 'StartDialogue', 'Signature help should detect StartDialogue.');
  const dialogueStartArg = echoScriptSignatureHelp('DialogueStart("jdh:dialogue", sessionId: ', 'DialogueStart("jdh:dialogue", sessionId: '.length);
  assert(dialogueStartArg && dialogueStartArg.target === 'DialogueStart', 'Signature help should detect DialogueStart.');
  assertEqual(dialogueStartArg.activeParameter, 1, 'DialogueStart signature help should focus sessionId named argument.');
  const debugMarkArg = echoScriptSignatureHelp('Debug.Mark(', 'Debug.Mark('.length);
  assert(debugMarkArg && debugMarkArg.target === 'Debug.Mark', 'Signature help should detect Debug.Mark.');
  const npcStandUpArg = echoScriptSignatureHelp('Npc.StandUp(Sasha, output: ', 'Npc.StandUp(Sasha, output: '.length);
  assert(npcStandUpArg && npcStandUpArg.target === 'Npc.StandUp', 'Signature help should detect Npc.StandUp.');
  const npcMoveArg = echoScriptSignatureHelp('Npc.Move(Sasha, 10, 64, ', 'Npc.Move(Sasha, 10, 64, '.length);
  assert(npcMoveArg && npcMoveArg.target === 'Npc.Move', 'Signature help should detect Npc.Move.');
  assert(npcMoveArg.signatures.some(item => item.label.includes('x: number, y: number, z: number')), 'Npc.Move signature help should include coordinate overload.');
  const npcInteractArg = echoScriptSignatureHelp('Npc.Interact(Sasha, TargetBlock, face: ', 'Npc.Interact(Sasha, TargetBlock, face: '.length);
  assert(npcInteractArg && npcInteractArg.target === 'Npc.Interact', 'Signature help should detect Npc.Interact.');
  assert(npcInteractArg.signatures.some(item => item.label.includes('position: BlockPosition')), 'Npc.Interact signature help should include BlockPosition overload.');

  const fireOnlyOncePerPlayerArg = echoScriptSignatureHelp('FireOnlyOncePerPlayer(', 'FireOnlyOncePerPlayer('.length);
  assert(fireOnlyOncePerPlayerArg && fireOnlyOncePerPlayerArg.target === 'FireOnlyOncePerPlayer', 'Signature help should detect FireOnlyOncePerPlayer.');

  const storyLoadArg = echoScriptSignatureHelp('Story.Load(', 'Story.Load('.length);
  assert(storyLoadArg && storyLoadArg.target === 'Story.Load', 'Signature help should detect Story.Load.');

  const effectApplyArg = echoScriptSignatureHelp('Effect.ApplyToPlayer("jdh:slow", intensity: ', 'Effect.ApplyToPlayer("jdh:slow", intensity: '.length);
  assert(effectApplyArg && effectApplyArg.target === 'Effect.ApplyToPlayer', 'Signature help should detect Effect.ApplyToPlayer.');
  assertEqual(effectApplyArg.activeParameter, 1, 'Effect.ApplyToPlayer signature help should focus intensity named argument.');

  const screenTintArg = echoScriptSignatureHelp('Screen.Tint(color: Color("#000000"), opacity: ', 'Screen.Tint(color: Color("#000000"), opacity: '.length);
  assert(screenTintArg && screenTintArg.target === 'Screen.Tint', 'Signature help should detect Screen.Tint.');
  assertEqual(screenTintArg.activeParameter, 1, 'Screen.Tint signature help should focus opacity named argument.');

  const cameraShakeArg = echoScriptSignatureHelp('Camera.Shake(amplitude: Curve(Key(Ticks(0), 0.0)), frequency: ', 'Camera.Shake(amplitude: Curve(Key(Ticks(0), 0.0)), frequency: '.length);
  assert(cameraShakeArg && cameraShakeArg.target === 'Camera.Shake', 'Signature help should detect Camera.Shake.');
  assertEqual(cameraShakeArg.activeParameter, 1, 'Camera.Shake signature help should focus frequency named argument.');

  const screenTintLayerArg = echoScriptSignatureHelp('Screen.Tint(color: Color("#000000"), opacity: Curve(Key(Ticks(0), 0.0)), layer: ', 'Screen.Tint(color: Color("#000000"), opacity: Curve(Key(Ticks(0), 0.0)), layer: '.length);
  assert(screenTintLayerArg && screenTintLayerArg.target === 'Screen.Tint', 'Signature help should detect Screen.Tint layer.');
  assertEqual(screenTintLayerArg.activeParameter, 2, 'Screen.Tint signature help should focus layer named argument.');

  const portraitStripArg = echoScriptSignatureHelp('PortraitStrip(0, 0, 16, 16, 0, 0, ', 'PortraitStrip(0, 0, 16, 16, 0, 0, '.length);
  assert(portraitStripArg && portraitStripArg.target === 'PortraitStrip', 'Signature help should detect PortraitStrip.');
  assertEqual(portraitStripArg.activeParameter, 6, 'PortraitStrip signature help should focus layer after six coordinates.');
}

function verifyEchoScriptNavigation() {
  const source = [
    'class Chapter1Room : DialogueResource',
    '{',
    '    private StateRef roomDone = State.Memory("chapter_1.room_done");',
    '    private StateRef relationshipSasha = State.Memory("chapter_1.relationship.sasha");',
    '    private NodeDef[] dialogueNodes = [',
    '        Room(),',
    '        RoomAfterChoices()',
    '    ];',
    '',
    '    public override StoryResourceDef Build()',
    '    {',
    '        return Dialogue();',
    '    }',
    '',
    '    private void Init()',
    '    {',
    '        Start("room");',
    '        Alias("s", Sasha);',
    '    }',
    '',
    '    private NodeDef[] Nodes()',
    '    {',
    '        return dialogueNodes;',
    '    }',
    '',
    '    private Effect[] StartEffects()',
    '    {',
    '        Npc.Spawn(Sasha, Overworld, 1, 2, 3);',
    '        Set(roomDone, true);',
    '    }',
    '',
    '    private Effect[] CompleteEffects()',
    '    {',
    '        Scene.Start(Chapter1RoomToHomeRelax);',
    '        Npc.EnterVehicleAt(Sasha, new Vector3(100207, 80, 100016), role: "driver", seat: "driver", radius: 3);',
    '        Vehicle.Trip(Sasha, "sasha_car", "chapter1_street_a", "chapter1_parking_b", tripId: "sasha_home", backend: "native_preferred");',
    '        Vehicle.Sequence(Sasha, "sasha_car", "chapter1_street_a", "home.front", sequenceId: "sasha_arrival", cinematic: Chapter1DriveCam);',
    '        relationshipSasha++;',
    '    }',
    '',
    '    private NodeDef Room()',
    '    {',
    '        var node = new Node("room");',
    '        Line("s", "Можно?");',
    '        if (!exists(roomDone))',
    '        {',
    '            Line("s", "Still not done.");',
    '        }',
    '        NextNode(RoomAfterChoices);',
    '        return node;',
    '    }',
    '',
    '    private NodeDef RoomAfterChoices()',
    '    {',
    '        var node = new Node("room_after_choices");',
    '        Line("s", "Ладно.");',
    '        return node;',
    '    }',
    '}'
  ].join('\n');

  const methodTarget = echoScriptDefinitionTarget(source, source.indexOf('RoomAfterChoices);') + 2);
  assert(methodTarget && methodTarget.type === 'local', 'EchoScript definition should resolve local method references.');
  assert(source.slice(methodTarget.range.start, methodTarget.range.end) === 'RoomAfterChoices', 'EchoScript definition should target method declaration range.');

  const nodeTarget = echoScriptDefinitionTarget(source, source.indexOf('"room"') + 2);
  assert(nodeTarget && nodeTarget.type === 'local', 'EchoScript definition should resolve local node id strings.');
  assert(source.slice(nodeTarget.range.start, nodeTarget.range.end) === 'room', 'EchoScript node definition should target new Node string.');

  const sceneTarget = echoScriptDefinitionTarget(source, source.indexOf('Chapter1RoomToHomeRelax') + 3);
  assert(sceneTarget && sceneTarget.kind === 'scene' && sceneTarget.id === 'chapter_1_room_to_home_relax', 'EchoScript definition should map typed scene refs to indexed ids.');

  const writeStateTarget = echoScriptDefinitionTarget(source, source.indexOf('roomDone', source.indexOf('Set(')) + 3);
  assert(writeStateTarget && writeStateTarget.kind === 'stateKey' && writeStateTarget.id === 'memory.chapter_1.room_done', 'EchoScript definition should map Set(...) state refs to write keys.');

  const readStateIndex = source.indexOf('roomDone', source.indexOf('exists('));
  const readStateTarget = echoScriptDefinitionTarget(source, readStateIndex + 3);
  assert(readStateTarget && readStateTarget.kind === 'stateKey' && readStateTarget.id === 'chapter_1.room_done', 'EchoScript definition should map exists(...) state refs to read keys.');

  const incrementStateIndex = source.indexOf('relationshipSasha++');
  const incrementStateTarget = echoScriptDefinitionTarget(source, incrementStateIndex + 3);
  assert(incrementStateTarget && incrementStateTarget.kind === 'stateKey' && incrementStateTarget.id === 'memory.chapter_1.relationship.sasha', 'EchoScript definition should map stateRef++ to write keys.');

  const renameTarget = echoScriptRenameTarget(source, source.indexOf('RoomAfterChoices()') + 2);
  assert(renameTarget && renameTarget.ranges.length >= 3, 'EchoScript rename should include local method declaration and references.');

  const highLevel = 'class Sample : DialogueResource {\nstart Node Opening() { GoTo(Continue); }\nNode Continue() { Line("john", "Done"); }\n}';
  const highLevelReference = highLevel.indexOf('Continue');
  const highLevelDefinition = echoScriptDefinitionTarget(highLevel, highLevelReference + 2);
  assert(highLevelDefinition && highLevel.slice(highLevelDefinition.range.start, highLevelDefinition.range.end) === 'Continue', 'Go to Definition should resolve symbolic high-level GoTo targets.');
  const highLevelRename = echoScriptRenameTarget(highLevel, highLevelReference + 2);
  assert(highLevelRename && highLevelRename.ranges.length === 2, 'Rename should update a high-level Node declaration and its GoTo references.');

  const nodeRenameTarget = echoScriptRenameTarget(source, source.indexOf('"room"') + 2);
  assert(nodeRenameTarget && nodeRenameTarget.kind === 'node' && nodeRenameTarget.ranges.length >= 2, 'EchoScript rename should include local node declaration and start reference.');

  const stateRenameTarget = echoScriptRenameTarget(source, source.indexOf('roomDone', source.indexOf('Set(')) + 2);
  assert(stateRenameTarget && stateRenameTarget.ranges.length >= 2, 'EchoScript rename should include typed state refs in the current file.');

  const npcRenameTarget = echoScriptRenameTarget(source, source.indexOf('Sasha', source.indexOf('Npc.Spawn(')) + 2);
  assert(npcRenameTarget && npcRenameTarget.ranges.length >= 1, 'EchoScript rename should accept typed NPC refs in helper calls.');

  const vehicleEnterNpcTarget = echoScriptDefinitionTarget(source, source.indexOf('Sasha', source.indexOf('Npc.EnterVehicleAt(')) + 2);
  assert(vehicleEnterNpcTarget && vehicleEnterNpcTarget.kind === 'npc' && vehicleEnterNpcTarget.id === 'sasha', 'EchoScript definition should map Npc.EnterVehicleAt first argument to indexed NPC ids.');

  const vehicleTripNpcTarget = echoScriptDefinitionTarget(source, source.indexOf('Sasha', source.indexOf('Vehicle.Trip(')) + 2);
  assert(vehicleTripNpcTarget && vehicleTripNpcTarget.kind === 'npc' && vehicleTripNpcTarget.id === 'sasha', 'EchoScript definition should map Vehicle.Trip first argument to indexed NPC ids.');

  const vehicleCameraTarget = echoScriptDefinitionTarget(source, source.indexOf('Chapter1DriveCam') + 3);
  assert(vehicleCameraTarget && vehicleCameraTarget.kind === 'cameraPath' && vehicleCameraTarget.id === 'chapter_1_drive_cam', 'EchoScript definition should map Vehicle.Sequence cinematic refs to indexed camera paths.');

  const sceneQuery = echoScriptReferenceQueryAtOffset(source, source.indexOf('Chapter1RoomToHomeRelax') + 3);
  assert(sceneQuery && sceneQuery.kind === 'scene' && sceneQuery.id === 'chapter_1_room_to_home_relax', 'EchoScript reference queries should resolve typed scene refs.');
  const occurrences = echoScriptReferenceOccurrencesInText(source);
  assert(occurrences.some(item => item.kind === 'scene' && item.id === 'chapter_1_room_to_home_relax'), 'EchoScript reference scanner should find typed scene refs.');
  assert(occurrences.some(item => item.kind === 'npc' && item.id === 'sasha'), 'EchoScript reference scanner should find typed NPC refs.');
  assert(occurrences.some(item => item.kind === 'stateKey' && item.id === 'memory.chapter_1.room_done'), 'EchoScript reference scanner should find write state refs.');
  const sceneRanges = echoScriptReferenceRangesInText(source, {
    kind: 'scene',
    id: 'chapter_1_room_to_home_relax'
  }, 'dialogue', false);
  assert(sceneRanges.length >= 1, 'EchoScript reference ranges should include typed scene refs.');

  const identifierEdits = echoScriptIdentifierRenameEditsInText(source, 'roomDone', 'roomComplete');
  assert(identifierEdits.length >= 2 && identifierEdits.every(edit => edit.newText === 'roomComplete'), 'EchoScript workspace rename should find typed identifier edits.');

  const stringReferenceSource = 'private Effect[] CompleteEffects() { Scene.Start("chapter_1_room_to_home_relax"); }';
  const stringReferenceEdits = echoScriptReferenceRenameEditsInText(stringReferenceSource, {
    kind: 'scene',
    id: 'chapter_1_room_to_home_relax'
  }, 'chapter_1_room_to_home_sleep');
  assertEqual(stringReferenceEdits.length, 1, 'EchoScript workspace rename should find string resource refs.');
  assert(stringReferenceSource.slice(stringReferenceEdits[0].range.start, stringReferenceEdits[0].range.end) === 'chapter_1_room_to_home_relax', 'EchoScript string ref edit should target only string contents.');

  const newResourceSource = [
    'class HomeArrivalStory : StoryResource',
    '{',
    '    public override StoryResourceDef Build()',
    '    {',
    '        return Story("jdh:chapter_1_home_arrival", start: "start");',
    '    }',
    '}',
    'class HomeArrivalDialogue : DialogueResource',
    '{',
    '    private FaceAnimationId FatherAngryBrows = "jdh:john_father/angry_brows";',
    '    public override DialogueResourceDef Build()',
    '    {',
    '        return Dialogue("jdh:chapter_1_home_arrival");',
    '    }',
    '    private void Init()',
    '    {',
    '        Alias("father", "jdh:john_father", visual: Portrait("jdh:john_father"));',
    '    }',
    '    private NodeDef Greeting()',
    '    {',
    '        Line("father", "Hi").SetAnimationFace(FatherAngryBrows);',
    '    }',
    '}',
    'class FatherNpc : NpcResource',
    '{',
    '    public override NpcResourceDef Build()',
    '    {',
    '        return Npc(id: "jdh:john_father", display: "Father", skin: Skin("jdh:john_father"), defaultForm: DefaultForm("jdh:sasha/child"));',
    '    }',
    '}',
    'class SashaChildForm : NpcFormResource',
    '{',
    '    public override NpcFormResourceDef Build()',
    '    {',
    '        return Form(id: "jdh:sasha/child", skin: Skin("jdh:sasha"), dimensions: Dimensions(0.6, 1.4, 1.2));',
    '    }',
    '}',
    'class KitchenTableEnvironment : EnvironmentResource',
    '{',
    '    public override EnvironmentResourceDef Build()',
    '    {',
    '        return Environment("jdh:home/kitchen_table", "minecraft:overworld", [State("morning_breakfast", [])]);',
    '    }',
    '}',
    'class WaveLoopClip : AnimationClipResource',
    '{',
    '    public override AnimationClipDef Build()',
    '    {',
    '        return AnimationClip("jdh:wave_loop", rig: "jdh:humanoid");',
    '    }',
    '}',
    'face JohnFather("jdh:john_father")',
    '{',
    '}',
    'class DefaultPlayerProfile : PlayerProfileTemplate',
    '{',
    '    public override PlayerProfile Build()',
    '    {',
    '        return PlayerProfile(id: "jdh:player/default", nameMode: ACCOUNT, spark: 100, maxSpark: 100, skinMode: ACCOUNT);',
    '    }',
    '}',
    'class SparkSword : WeaponResource',
    '{',
    '    public override WeaponDefinition Build()',
    '    {',
    '        return Weapon(id: "jdh:spark_sword", name: "Spark Sword", texture: "jdh:item/spark_sword", rarity: COMMON);',
    '    }',
    '}',
    'class HomeDoorTrigger : TriggerResource',
    '{',
    '    private Area HomeDoor = Area("minecraft:overworld", 0, 64, 0, 2, 3, 2);',
    '    private void Init()',
    '    {',
    '        Id("jdh:home/door");',
    '        StartStory("jdh:chapter_1_home_arrival");',
    '    }',
    '}'
  ].join('\n');
  const storyDeclaration = echoScriptDefinitionTarget(newResourceSource, newResourceSource.indexOf('jdh:chapter_1_home_arrival') + 5);
  assert(storyDeclaration && storyDeclaration.kind === 'story', 'EchoScript definition should recognize Story(...) resource declarations.');
  const npcDeclaration = echoScriptDefinitionTarget(newResourceSource, newResourceSource.indexOf('jdh:john_father", display') + 5);
  assert(npcDeclaration && npcDeclaration.kind === 'npc', 'EchoScript definition should recognize Npc(id: ...) resource declarations.');
  const skinReference = echoScriptDefinitionTarget(newResourceSource, newResourceSource.indexOf('jdh:john_father"), defaultForm', newResourceSource.indexOf('Skin(')) + 5);
  assert(skinReference && skinReference.kind === 'skin', 'EchoScript definition should recognize Skin(...) references.');
  const defaultFormReference = echoScriptDefinitionTarget(newResourceSource, newResourceSource.indexOf('jdh:sasha/child"));', newResourceSource.indexOf('DefaultForm(')) + 5);
  assert(defaultFormReference && defaultFormReference.kind === 'npcForm', 'EchoScript definition should recognize DefaultForm(...) references.');
  const formDeclaration = echoScriptDefinitionTarget(newResourceSource, newResourceSource.indexOf('jdh:sasha/child", skin') + 5);
  assert(formDeclaration && formDeclaration.kind === 'npcForm', 'EchoScript definition should recognize Form(id: ...) resource declarations.');
  const environmentDeclaration = echoScriptDefinitionTarget(newResourceSource, newResourceSource.indexOf('jdh:home/kitchen_table') + 5);
  assert(environmentDeclaration && environmentDeclaration.kind === 'environment', 'EchoScript definition should recognize Environment(...) resource declarations.');
  const playerProfileDeclaration = echoScriptDefinitionTarget(newResourceSource, newResourceSource.indexOf('jdh:player/default') + 5);
  assert(playerProfileDeclaration && playerProfileDeclaration.kind === 'playerProfileTemplate', 'EchoScript definition should recognize PlayerProfile(id: ...) resource declarations.');
  const weaponDeclaration = echoScriptDefinitionTarget(newResourceSource, newResourceSource.indexOf('jdh:spark_sword') + 5);
  assert(weaponDeclaration && weaponDeclaration.kind === 'weapon', 'EchoScript definition should recognize Weapon(id: ...) resource declarations.');
  const triggerDeclaration = echoScriptDefinitionTarget(newResourceSource, newResourceSource.indexOf('jdh:home/door') + 5);
  assert(triggerDeclaration && triggerDeclaration.kind === 'trigger', 'EchoScript definition should recognize TriggerResource Id(...) declarations.');
  const triggerStoryReference = echoScriptDefinitionTarget(newResourceSource, newResourceSource.indexOf('jdh:chapter_1_home_arrival', newResourceSource.indexOf('StartStory(')) + 5);
  assert(triggerStoryReference && triggerStoryReference.kind === 'story', 'EchoScript definition should recognize StartStory(...) story references.');
  const environmentSetSource = 'private void OnStart() { Environment.Set("jdh:home/kitchen_table", "morning_breakfast"); Dialogue.Load("jdh:chapter_1_home_arrival"); Story.Load("jdh:chapter_1_home_arrival"); }';
  const environmentSetReference = echoScriptDefinitionTarget(environmentSetSource, environmentSetSource.indexOf('jdh:home/kitchen_table') + 5);
  assert(environmentSetReference && environmentSetReference.kind === 'environment', 'EchoScript definition should recognize Environment.Set(...) environment references.');
  const dialogueLoadReference = echoScriptDefinitionTarget(environmentSetSource, environmentSetSource.indexOf('jdh:chapter_1_home_arrival') + 5);
  assert(dialogueLoadReference && dialogueLoadReference.kind === 'dialogue', 'EchoScript definition should recognize Dialogue.Load(...) dialogue references.');
  const storyLoadReference = echoScriptDefinitionTarget(environmentSetSource, environmentSetSource.lastIndexOf('jdh:chapter_1_home_arrival') + 5);
  assert(storyLoadReference && storyLoadReference.kind === 'story', 'EchoScript definition should recognize Story.Load(...) story references.');
  const localFaceAnimation = echoScriptDefinitionTarget(newResourceSource, newResourceSource.indexOf('FatherAngryBrows);') + 5);
  assert(localFaceAnimation && localFaceAnimation.type === 'local', 'EchoScript definition should prefer local FaceAnimationId fields in SetAnimationFace(...).');
  const clipRenameEdits = echoScriptReferenceRenameEditsInText(newResourceSource, {
    kind: 'animationClip',
    id: 'jdh:wave_loop'
  }, 'jdh:wave_loop_v2');
  assert(clipRenameEdits.some(edit => newResourceSource.slice(edit.range.start, edit.range.end) === 'jdh:wave_loop'), 'EchoScript rename should include AnimationClip(...) declarations.');
  const weaponRenameEdits = echoScriptReferenceRenameEditsInText(newResourceSource, {
    kind: 'weapon',
    id: 'jdh:spark_sword'
  }, 'jdh:spark_sword_v2');
  assert(weaponRenameEdits.some(edit => newResourceSource.slice(edit.range.start, edit.range.end) === 'jdh:spark_sword'), 'EchoScript rename should include Weapon(...) declarations.');
  const triggerRenameEdits = echoScriptReferenceRenameEditsInText(newResourceSource, {
    kind: 'trigger',
    id: 'jdh:home/door'
  }, 'jdh:home/door_v2');
  assert(triggerRenameEdits.some(edit => newResourceSource.slice(edit.range.start, edit.range.end) === 'jdh:home/door'), 'EchoScript rename should include TriggerResource Id(...) declarations.');
  const faceRigRenameEdits = echoScriptReferenceRenameEditsInText(newResourceSource, {
    kind: 'faceRig',
    id: 'jdh:john_father'
  }, 'jdh:john_father_v2');
  assert(faceRigRenameEdits.some(edit => edit.source === 'echoscript-face-rig-declaration'), 'EchoScript rename should include high-level face DSL declarations.');
}

function verifyEchoScriptCompletions() {
  const fakeIndex = {
    ids(kind) {
      return {
        story: ['jdh:chapter_1_home_arrival'],
        dialogue: ['chapter_1_room'],
        environment: ['jdh:home/kitchen_table'],
        scene: ['chapter_1_room_to_home_relax'],
        npc: ['sasha'],
        npcForm: ['jdh:sasha/child'],
        speaker: ['buyer'],
        animation: ['npc_offer_item_hold'],
        faceAnimation: ['sasha_smile'],
        animationProfile: ['standard_humanoid'],
        animationRig: ['humanoid'],
        animationClip: ['wave_loop'],
        faceRig: ['john_father'],
        facePose: ['john_father_neutral'],
        playerProfileTemplate: ['jdh:player/default'],
        weapon: ['jdh:spark_sword'],
        trigger: ['jdh:home/door'],
        skin: ['john_father'],
        cameraPath: ['chapter_1_drive_cam']
      }[kind] || [];
    },
    stateKeys() {
      return ['memory.chapter_1.room_done', 'chapter_1.room_done'];
    }
  };
  const source = [
    'class Chapter1Room : DialogueResource',
    '{',
    '    private StateRef roomDone = State.Memory("chapter_1.room_done");',
    '    private NodeDef[] dialogueNodes = [',
    '        RoomAfterChoices()',
    '    ];',
    '',
    '    private NodeDef RoomAfterChoices()',
    '    {',
    '        return Node("room_after_choices");',
    '    }',
    '}'
  ].join('\n');
  const entries = echoScriptCompletionEntries(source, source.length, fakeIndex);
  const labels = entries.map(entry => entry.label);
  assert(labels.includes('Node declaration') && labels.includes('start Node declaration'), 'EchoScript completions should offer high-level Node snippets.');
  assert(labels.includes('dialogueNodes'), 'EchoScript completions should include local dialogueNodes field.');
  assert(labels.includes('RoomAfterChoices'), 'EchoScript completions should include local node methods.');
  assert(labels.includes('Dialogues.Chapter1Room'), 'EchoScript completions should include indexed dialogue refs.');
  assert(labels.includes('Environments.JdhHomeKitchenTable'), 'EchoScript completions should include indexed environment refs.');
  assert(labels.includes('NpcForms.JdhSashaChild'), 'EchoScript completions should include indexed NPC form refs.');
  assert(labels.includes('Scenes.Chapter1RoomToHomeRelax'), 'EchoScript completions should include indexed scene refs.');
  assert(labels.includes('AnimationProfiles.StandardHumanoid'), 'EchoScript completions should include indexed animation profile refs.');
  assert(labels.includes('AnimationClips.WaveLoop'), 'EchoScript completions should include indexed animation clip refs.');
  assert(labels.includes('Skins.JohnFather'), 'EchoScript completions should include indexed skin refs.');
  assert(labels.includes('CameraPaths.Chapter1DriveCam'), 'EchoScript completions should include indexed camera path refs.');
  assert(labels.includes('PlayerProfileTemplates.JdhPlayerDefault'), 'EchoScript completions should include indexed player profile template refs.');
  assert(labels.includes('Weapons.JdhSparkSword'), 'EchoScript completions should include indexed weapon refs.');
  assert(labels.includes('Triggers.JdhHomeDoor'), 'EchoScript completions should include indexed trigger refs.');
  assert(labels.includes('"memory.chapter_1.room_done"'), 'EchoScript completions should include indexed state keys.');
  assert(labels.includes('Vehicle.Trip'), 'EchoScript completions should include vehicle global snippets.');
  assert(labels.includes('Npc.Path'), 'EchoScript completions should include Npc.Path global snippets.');
  assert(labels.includes('Navigation.SetTarget'), 'EchoScript completions should include navigation global snippets.');
  assert(labels.includes('Player.Pose'), 'EchoScript completions should include player pose global snippets.');
  assert(labels.includes('Environment.Set'), 'EchoScript completions should include Environment.Set global snippets.');
  assert(labels.includes('Dialogue.Load'), 'EchoScript completions should include Dialogue.Load global snippets.');
  assert(labels.includes('Story.Load'), 'EchoScript completions should include Story.Load global snippets.');
  assert(labels.includes('Sleep.EnterDream'), 'EchoScript completions should include Sleep.EnterDream global snippets.');
  assert(labels.includes('Effect.ApplyToPlayer') && labels.includes('Movement.SpeedMultiplier') && labels.includes('Sound.PlayAt') && labels.includes('VanillaEffect.Apply'), 'EchoScript completions should include Echo Effects global snippets.');
  assert(labels.includes('NameColor') && labels.includes('nameColor'), 'EchoScript completions should include NPC name-color helper snippets.');
  assert(labels.includes('TriggerResource') && labels.includes('PlayerProfileTemplate') && labels.includes('WeaponResource') && labels.includes('SeatZoneResource') && labels.includes('PlayerHudConfigResource'), 'EchoScript completions should include new resource global snippets.');
  assert(labels.includes('PlayerVitalsConfigResource') && labels.includes('DialogueOverlayStyleConfig') && labels.includes('PortraitOverlayConfig'), 'EchoScript completions should include config resource global snippets.');
  assert(labels.includes('DialoguePanelBackgroundOverlay') && labels.includes('DialogueChoiceBackgroundOverlay'), 'EchoScript completions should include dialogue background overlay call snippets.');
  const navigationTargetSnippet = entries.find(entry => entry.label === 'Navigation.SetTarget');
  assert(navigationTargetSnippet.insertText.includes('x:') && !navigationTargetSnippet.insertText.includes('new Vector3'), 'EchoScript Navigation.SetTarget snippet should use compiler-supported named coordinates.');

  const vehicleLabels = echoScriptCompletionEntries('Vehicle.', 'Vehicle.'.length, fakeIndex).map(entry => entry.label);
  assert(vehicleLabels.includes('EnsureSpawn') && vehicleLabels.includes('Trip') && vehicleLabels.includes('Sequence'), 'EchoScript member completions should include Vehicle helpers.');

  const npcLabels = echoScriptCompletionEntries('Npc.', 'Npc.'.length, fakeIndex).map(entry => entry.label);
  assert(npcLabels.includes('SpawnAt') && npcLabels.includes('SleepAt') && npcLabels.includes('WakeUp') && npcLabels.includes('SetForm') && npcLabels.includes('StopInteraction') && npcLabels.includes('StandUp'), 'EchoScript member completions should include new NPC external helpers.');
  assert(npcLabels.includes('EnterVehicleAt') && npcLabels.includes('LeaveVehicle'), 'EchoScript member completions should include NPC vehicle helpers.');
  assert(npcLabels.includes('SpawnNearPlayer') && npcLabels.includes('Path') && npcLabels.includes('Stay') && npcLabels.includes('WalkTo') && npcLabels.includes('WalkToPlayer'), 'EchoScript member completions should include current NPC movement helpers.');
  assert(npcLabels.includes('PlayAnimation') && npcLabels.includes('HoldItem') && npcLabels.includes('ClearHeldItem'), 'EchoScript member completions should include current NPC item and animation helpers.');

  const dialogueNamespaceLabels = echoScriptCompletionEntries('Dialogues.', 'Dialogues.'.length, fakeIndex).map(entry => entry.label);
  assert(dialogueNamespaceLabels.includes('Chapter1Room') && !dialogueNamespaceLabels.includes('Chapter1RoomToHomeRelax'), 'EchoScript resource namespace completions should narrow Dialogues. to dialogue ids.');

  const dialogueMemberLabels = echoScriptCompletionEntries('Dialogue.', 'Dialogue.'.length, fakeIndex).map(entry => entry.label);
  assert(dialogueMemberLabels.includes('Start') && dialogueMemberLabels.includes('Load') && dialogueMemberLabels.includes('GoTo'), 'EchoScript member completions should include dialogue namespace helpers.');

  const cutsceneMemberLabels = echoScriptCompletionEntries('Cutscene.', 'Cutscene.'.length, fakeIndex).map(entry => entry.label);
  assert(cutsceneMemberLabels.includes('Start') && cutsceneMemberLabels.includes('Play') && cutsceneMemberLabels.includes('WaitPrepared') && cutsceneMemberLabels.includes('Stop'), 'EchoScript member completions should include cutscene command helpers.');

  const effectMemberLabels = echoScriptCompletionEntries('Effect.', 'Effect.'.length, fakeIndex).map(entry => entry.label);
  assert(effectMemberLabels.includes('ApplyToPlayer') && effectMemberLabels.includes('RemoveFromPlayer') && effectMemberLabels.includes('HasCompletedOnPlayer'), 'EchoScript member completions should include Echo Effects player commands.');

  const movementMemberLabels = echoScriptCompletionEntries('Movement.', 'Movement.'.length, fakeIndex).map(entry => entry.label);
  assert(movementMemberLabels.includes('SpeedMultiplier'), 'EchoScript member completions should include Echo Effects movement channels.');

  const soundMemberLabels = echoScriptCompletionEntries('Sound.', 'Sound.'.length, fakeIndex).map(entry => entry.label);
  assert(soundMemberLabels.includes('PlayAt'), 'EchoScript member completions should include Echo Effects sound events.');

  const vanillaEffectMemberLabels = echoScriptCompletionEntries('VanillaEffect.', 'VanillaEffect.'.length, fakeIndex).map(entry => entry.label);
  assert(vanillaEffectMemberLabels.includes('Apply'), 'EchoScript member completions should include Echo Effects vanilla effect events.');

  const screenMemberLabels = echoScriptCompletionEntries('Screen.', 'Screen.'.length, fakeIndex).map(entry => entry.label);
  assert(screenMemberLabels.includes('SetBlack') && screenMemberLabels.includes('Tint') && screenMemberLabels.includes('Vignette'), 'EchoScript member completions should include Screen story and effect helpers.');

  const environmentNamespaceLabels = echoScriptCompletionEntries('Environments.', 'Environments.'.length, fakeIndex).map(entry => entry.label);
  assert(environmentNamespaceLabels.includes('JdhHomeKitchenTable'), 'EchoScript resource namespace completions should narrow Environments. to environment ids.');

  const npcFormNamespaceLabels = echoScriptCompletionEntries('NpcForms.', 'NpcForms.'.length, fakeIndex).map(entry => entry.label);
  assert(npcFormNamespaceLabels.includes('JdhSashaChild'), 'EchoScript resource namespace completions should narrow NpcForms. to NPC form ids.');

  const cameraPathNamespaceLabels = echoScriptCompletionEntries('CameraPaths.', 'CameraPaths.'.length, fakeIndex).map(entry => entry.label);
  assert(cameraPathNamespaceLabels.includes('Chapter1DriveCam'), 'EchoScript resource namespace completions should narrow CameraPaths. to camera path ids.');

  const playerProfileNamespaceLabels = echoScriptCompletionEntries('PlayerProfileTemplates.', 'PlayerProfileTemplates.'.length, fakeIndex).map(entry => entry.label);
  assert(playerProfileNamespaceLabels.includes('JdhPlayerDefault'), 'EchoScript resource namespace completions should narrow PlayerProfileTemplates. to player profile template ids.');

  const weaponNamespaceLabels = echoScriptCompletionEntries('Weapons.', 'Weapons.'.length, fakeIndex).map(entry => entry.label);
  assert(weaponNamespaceLabels.includes('JdhSparkSword'), 'EchoScript resource namespace completions should narrow Weapons. to weapon ids.');

  const triggerNamespaceLabels = echoScriptCompletionEntries('Triggers.', 'Triggers.'.length, fakeIndex).map(entry => entry.label);
  assert(triggerNamespaceLabels.includes('JdhHomeDoor'), 'EchoScript resource namespace completions should narrow Triggers. to trigger ids.');

  const environmentArgLabels = echoScriptCompletionEntries('Environment.Set(', 'Environment.Set('.length, fakeIndex).map(entry => entry.label);
  assert(environmentArgLabels.includes('Environments.JdhHomeKitchenTable') && !environmentArgLabels.includes('Dialogues.Chapter1Room'), 'EchoScript Environment.Set completions should suggest environment ids.');

  const startStoryLabels = echoScriptCompletionEntries('StartStory(', 'StartStory('.length, fakeIndex).map(entry => entry.label);
  assert(startStoryLabels.includes('Stories.JdhChapter1HomeArrival') && !startStoryLabels.includes('Dialogues.Chapter1Room'), 'EchoScript StartStory completions should suggest story ids.');

  const startDialogueLabels = echoScriptCompletionEntries('StartDialogue(', 'StartDialogue('.length, fakeIndex).map(entry => entry.label);
  assert(startDialogueLabels.includes('Dialogues.Chapter1Room') && !startDialogueLabels.includes('Stories.JdhChapter1HomeArrival'), 'EchoScript StartDialogue completions should suggest dialogue ids.');

  const storyLoadLabels = echoScriptCompletionEntries('Story.Load(', 'Story.Load('.length, fakeIndex).map(entry => entry.label);
  assert(storyLoadLabels.includes('Stories.JdhChapter1HomeArrival') && !storyLoadLabels.includes('Dialogues.Chapter1Room'), 'EchoScript Story.Load completions should suggest story ids.');

  const playerProfileArgLabels = echoScriptCompletionEntries('UseProfile(template: ', 'UseProfile(template: '.length, fakeIndex).map(entry => entry.label);
  assert(playerProfileArgLabels.includes('PlayerProfileTemplates.JdhPlayerDefault') && !playerProfileArgLabels.includes('Weapons.JdhSparkSword'), 'EchoScript template named argument completions should suggest player profile templates.');

  const weaponArgLabels = echoScriptCompletionEntries('UseWeapon(weapon: ', 'UseWeapon(weapon: '.length, fakeIndex).map(entry => entry.label);
  assert(weaponArgLabels.includes('Weapons.JdhSparkSword') && !weaponArgLabels.includes('PlayerProfileTemplates.JdhPlayerDefault'), 'EchoScript weapon named argument completions should suggest weapons.');

  const formArgLabels = echoScriptCompletionEntries('Npc(defaultForm: ', 'Npc(defaultForm: '.length, fakeIndex).map(entry => entry.label);
  assert(formArgLabels.includes('NpcForms.JdhSashaChild') && !formArgLabels.includes('Environments.JdhHomeKitchenTable'), 'EchoScript defaultForm completions should suggest NPC form ids.');

  const npcNameColorEntries = echoScriptCompletionEntries('Npc(', 'Npc('.length, fakeIndex);
  const npcNameColorSnippet = npcNameColorEntries.find(entry => entry.label === 'nameColor');
  assert(npcNameColorSnippet && npcNameColorSnippet.insertText.includes('NameColor('), 'EchoScript Npc(...) argument completions should use NameColor(...) for nameColor.');

  const effectApplyArgLabels = echoScriptCompletionEntries('Effect.ApplyToPlayer("jdh:slow", ', 'Effect.ApplyToPlayer("jdh:slow", '.length, fakeIndex).map(entry => entry.label);
  assert(effectApplyArgLabels.includes('intensity'), 'EchoScript Effect.ApplyToPlayer argument completions should include intensity.');

  const vanillaEffectArgLabels = echoScriptCompletionEntries('VanillaEffect.Apply(', 'VanillaEffect.Apply('.length, fakeIndex).map(entry => entry.label);
  assert(vanillaEffectArgLabels.includes('effect') && vanillaEffectArgLabels.includes('duration') && vanillaEffectArgLabels.includes('showIcon'), 'EchoScript VanillaEffect.Apply argument completions should include vanilla effect fields.');

  const screenTintArgLabels = echoScriptCompletionEntries('Screen.Tint(', 'Screen.Tint('.length, fakeIndex).map(entry => entry.label);
  assert(screenTintArgLabels.includes('color') && screenTintArgLabels.includes('opacity') && screenTintArgLabels.includes('layer'), 'EchoScript Screen.Tint argument completions should include color, opacity, and layer.');

  const cameraShakeArgLabels = echoScriptCompletionEntries('Camera.Shake(', 'Camera.Shake('.length, fakeIndex).map(entry => entry.label);
  assert(cameraShakeArgLabels.includes('amplitude') && cameraShakeArgLabels.includes('frequency') && cameraShakeArgLabels.includes('seed'), 'EchoScript Camera.Shake argument completions should include amplitude, frequency, and seed.');

  const presentationArgLabels = echoScriptCompletionEntries('Presentation(', 'Presentation('.length, fakeIndex).map(entry => entry.label);
  assert(presentationArgLabels.includes('nameKey') && presentationArgLabels.includes('descriptionKey') && presentationArgLabels.includes('icon'), 'EchoScript Presentation argument completions should include display fields.');

  const playerHudArgLabels = echoScriptCompletionEntries('PlayerHud(', 'PlayerHud('.length, fakeIndex).map(entry => entry.label);
  assert(playerHudArgLabels.includes('id') && playerHudArgLabels.includes('referenceWidth') && playerHudArgLabels.includes('scaleWithResolution'), 'EchoScript PlayerHud argument completions should include HUD layout fields.');

  const barArgLabels = echoScriptCompletionEntries('Bar(', 'Bar('.length, fakeIndex).map(entry => entry.label);
  assert(barArgLabels.includes('source') && barArgLabels.includes('gradientStart') && barArgLabels.includes('iconWidth') && barArgLabels.includes('iconOffsetX'), 'EchoScript Bar argument completions should include HUD bar fields.');

  const playerFatigueArgLabels = echoScriptCompletionEntries('PlayerFatigue(', 'PlayerFatigue('.length, fakeIndex).map(entry => entry.label);
  assert(playerFatigueArgLabels.includes('maximum') && playerFatigueArgLabels.includes('tiredMovementMultiplier') && playerFatigueArgLabels.includes('exhaustedNausea'), 'EchoScript PlayerFatigue argument completions should include vitals config fields.');

  const dialogueOverlayArgLabels = echoScriptCompletionEntries('DialogueOverlayStyle(', 'DialogueOverlayStyle('.length, fakeIndex).map(entry => entry.label);
  assert(dialogueOverlayArgLabels.includes('typewriterMsPerCharacter') && dialogueOverlayArgLabels.includes('choiceTextColor') && dialogueOverlayArgLabels.includes('visualOffsetY'), 'EchoScript DialogueOverlayStyle argument completions should include overlay style fields.');

  const panelBackgroundArgLabels = echoScriptCompletionEntries('DialoguePanelBackgroundOverlay(', 'DialoguePanelBackgroundOverlay('.length, fakeIndex).map(entry => entry.label);
  assert(panelBackgroundArgLabels.includes('mode') && panelBackgroundArgLabels.includes('textureWidth') && panelBackgroundArgLabels.includes('sliceLeft') && panelBackgroundArgLabels.includes('fillDirection'), 'EchoScript DialoguePanelBackgroundOverlay argument completions should include panel image fields.');

  const choiceBackgroundArgLabels = echoScriptCompletionEntries('DialogueChoiceBackgroundOverlay(', 'DialogueChoiceBackgroundOverlay('.length, fakeIndex).map(entry => entry.label);
  assert(choiceBackgroundArgLabels.includes('mode') && choiceBackgroundArgLabels.includes('texture') && choiceBackgroundArgLabels.includes('sliceBottom'), 'EchoScript DialogueChoiceBackgroundOverlay argument completions should include choice image fields.');

  const portraitOverlayArgLabels = echoScriptCompletionEntries('PortraitOverlay(', 'PortraitOverlay('.length, fakeIndex).map(entry => entry.label);
  assert(portraitOverlayArgLabels.includes('visualId') && portraitOverlayArgLabels.includes('facePixels') && portraitOverlayArgLabels.includes('layers') && portraitOverlayArgLabels.includes('idle'), 'EchoScript PortraitOverlay argument completions should include portrait overlay fields.');

  const portraitLayerArgLabels = echoScriptCompletionEntries('Layer(', 'Layer('.length, fakeIndex).map(entry => entry.label);
  assert(portraitLayerArgLabels.includes('bone') && portraitLayerArgLabels.includes('texture') && portraitLayerArgLabels.includes('placeX') && portraitLayerArgLabels.includes('scaleY'), 'EchoScript Layer argument completions should include FaceRig and portrait overlay fields.');

  const matchArgLabels = echoScriptCompletionEntries('Match(', 'Match('.length, fakeIndex).map(entry => entry.label);
  assert(matchArgLabels.includes('face') && matchArgLabels.includes('top') && matchArgLabels.includes('feet'), 'EchoScript wardrobe Match argument completions should include outfit slots.');

  const detectArgLabels = echoScriptCompletionEntries('Detect(', 'Detect('.length, fakeIndex).map(entry => entry.label);
  assert(detectArgLabels.includes('policy'), 'EchoScript Detect argument completions should include seating policy.');

  const unknownSeatsArgLabels = echoScriptCompletionEntries('UnknownSeats(', 'UnknownSeats('.length, fakeIndex).map(entry => entry.label);
  assert(unknownSeatsArgLabels.includes('policy'), 'EchoScript UnknownSeats argument completions should include seating policy.');

  const boxArgLabels = echoScriptCompletionEntries('Box(', 'Box('.length, fakeIndex).map(entry => entry.label);
  assert(boxArgLabels.includes('min') && boxArgLabels.includes('max'), 'EchoScript Box argument completions should include SeatZone bounds.');

  const slotArgLabels = echoScriptCompletionEntries('Slot(', 'Slot('.length, fakeIndex).map(entry => entry.label);
  assert(slotArgLabels.includes('slot') && slotArgLabels.includes('id') && slotArgLabels.includes('anchor'), 'EchoScript Slot argument completions should include wardrobe and SeatZone variants.');

  const profileLabels = echoScriptCompletionEntries('NpcAnimationBinding("jdh:binding", npc: "jdh:john_father", profile: ', 'NpcAnimationBinding("jdh:binding", npc: "jdh:john_father", profile: '.length, fakeIndex).map(entry => entry.label);
  assert(profileLabels.includes('AnimationProfiles.StandardHumanoid') && !profileLabels.includes('AnimationClips.WaveLoop'), 'EchoScript profile argument completions should suggest animation profiles.');

  const rigLabels = echoScriptCompletionEntries('AnimationClip("jdh:wave_loop", rig: ', 'AnimationClip("jdh:wave_loop", rig: '.length, fakeIndex).map(entry => entry.label);
  assert(rigLabels.includes('AnimationRigs.Humanoid') && rigLabels.includes('FaceRigs.JohnFather'), 'EchoScript rig argument completions should suggest body and face rigs.');

  const clipLabels = echoScriptCompletionEntries('Entry("body/idle", clip: ', 'Entry("body/idle", clip: '.length, fakeIndex).map(entry => entry.label);
  assert(clipLabels.includes('AnimationClips.WaveLoop') && !clipLabels.includes('AnimationProfiles.StandardHumanoid'), 'EchoScript clip argument completions should suggest animation clips.');

  const questStatusLabels = echoScriptCompletionEntries('QuestStatus.', 'QuestStatus.'.length, fakeIndex).map(entry => entry.label);
  assert(questStatusLabels.includes('Active') && questStatusLabels.includes('Completed') && !questStatusLabels.includes('Failed'), 'EchoScript QuestStatus. completions should expose compiler-supported quest statuses.');

  const layerLabels = echoScriptCompletionEntries('Layer.', 'Layer.'.length, fakeIndex).map(entry => entry.label);
  assert(layerLabels.includes('Overlay') && !layerLabels.includes('Body'), 'EchoScript Layer. completions should expose supported animation layers.');

  const handLabels = echoScriptCompletionEntries('Hand.', 'Hand.'.length, fakeIndex).map(entry => entry.label);
  assert(handLabels.includes('Main') && !handLabels.includes('Mainhand'), 'EchoScript Hand. completions should expose supported hand refs.');

  const itemLabels = echoScriptCompletionEntries('Items.', 'Items.'.length, fakeIndex).map(entry => entry.label);
  assert(itemLabels.includes('SparkGrass'), 'EchoScript Items. completions should expose built-in item refs.');

  const portraitLabels = echoScriptCompletionEntries('Portraits.', 'Portraits.'.length, fakeIndex).map(entry => entry.label);
  assert(portraitLabels.includes('Buyer') && portraitLabels.includes('Sasha'), 'EchoScript Portraits. completions should expose built-in portrait refs.');

  const oldFriendLabels = echoScriptCompletionEntries('OldFriend.', 'OldFriend.'.length, fakeIndex).map(entry => entry.label);
  assert(oldFriendLabels.includes('Quest') && oldFriendLabels.includes('Paid'), 'EchoScript OldFriend. completions should expose built-in quest and state refs.');

  const navigationLabels = echoScriptCompletionEntries('Navigation.', 'Navigation.'.length, fakeIndex).map(entry => entry.label);
  assert(navigationLabels.includes('SetTarget') && navigationLabels.includes('ClearTarget'), 'EchoScript member completions should include navigation helpers.');

  const sleepLabels = echoScriptCompletionEntries('Sleep.', 'Sleep.'.length, fakeIndex).map(entry => entry.label);
  assert(sleepLabels.includes('EnterDream') && sleepLabels.includes('Blink') && sleepLabels.includes('WaitSeconds') && sleepLabels.includes('IsEyesClosed'), 'EchoScript member completions should include sleep helpers.');

  const playerSleepLabels = echoScriptCompletionEntries('PlayerSleep.', 'PlayerSleep.'.length, fakeIndex).map(entry => entry.label);
  assert(playerSleepLabels.includes('Confirmed') && playerSleepLabels.includes('EnteredDream') && playerSleepLabels.includes('Failed'), 'EchoScript member completions should include PlayerSleep selectors.');

  const playerSeatedAtArgLabels = echoScriptCompletionEntries('Player.SeatedAt(', 'Player.SeatedAt('.length, fakeIndex).map(entry => entry.label);
  assert(playerSeatedAtArgLabels.includes('zone') && playerSeatedAtArgLabels.includes('slot'), 'EchoScript Player.SeatedAt argument completions should include zone and slot.');

  const environmentLabels = echoScriptCompletionEntries('Environment.', 'Environment.'.length, fakeIndex).map(entry => entry.label);
  assert(environmentLabels.includes('Set'), 'EchoScript member completions should include Environment.Set.');

  const playerLabels = echoScriptCompletionEntries('Player.', 'Player.'.length, fakeIndex).map(entry => entry.label);
  assert(playerLabels.includes('Pose') && playerLabels.includes('GiveItem') && playerLabels.includes('SpawnAt') && playerLabels.includes('Move') && playerLabels.includes('SetFatigue') && playerLabels.includes('RestoreFatigue') && playerLabels.includes('Teleport'), 'EchoScript member completions should include player helpers.');
  assert(playerLabels.includes('SeatedIn') && playerLabels.includes('SeatedAt') && playerLabels.includes('ChangedSeatIn'), 'EchoScript member completions should include player seating trigger selectors.');

  const actionChainSource = 'var action = Action()\n    .';
  const actionChainLabels = echoScriptCompletionEntries(actionChainSource, actionChainSource.length, fakeIndex).map(entry => entry.label);
  assert(actionChainLabels.includes('ActionKey') && actionChainLabels.includes('LabelKey') && actionChainLabels.includes('FireOnce'), 'EchoScript fluent completions should infer Action() chain members.');

  const actionVariableSource = 'var action = Action();\naction.';
  const actionVariableLabels = echoScriptCompletionEntries(actionVariableSource, actionVariableSource.length, fakeIndex).map(entry => entry.label);
  assert(actionVariableLabels.includes('Visible') && actionVariableLabels.includes('ActionKey'), 'EchoScript member completions should infer local Action() variables.');

  const localActionMethodSource = [
    'private ActionDef PayMoney()',
    '{',
    '    return Action().Label("Pay");',
    '}',
    'PayMoney().'
  ].join('\n');
  const localActionMethodLabels = echoScriptCompletionEntries(localActionMethodSource, localActionMethodSource.length, fakeIndex).map(entry => entry.label);
  assert(localActionMethodLabels.includes('ActionKey') && localActionMethodLabels.includes('FireOnce'), 'EchoScript member completions should infer builder members from local method return types.');
  const localLifecycleEntries = echoScriptCompletionEntries('private Effect[] CompleteEffects() {}\nComple', 'private Effect[] CompleteEffects() {}\nComple'.length, fakeIndex);
  const localLifecycleEntry = localLifecycleEntries.find(entry => entry.label === 'CompleteEffects');
  assert(localLifecycleEntry && localLifecycleEntry.detail.includes('локальный метод'), 'EchoScript local method completions should use Russian detail text.');

  const localActionVariableSource = [
    'private ActionDef PayMoney()',
    '{',
    '    return Action().Label("Pay");',
    '}',
    'var action = PayMoney();',
    'action.'
  ].join('\n');
  const localActionVariableLabels = echoScriptCompletionEntries(localActionVariableSource, localActionVariableSource.length, fakeIndex).map(entry => entry.label);
  assert(localActionVariableLabels.includes('Visible') && localActionVariableLabels.includes('SuccessLabel'), 'EchoScript member completions should infer variables initialized from typed local methods.');

  const typedActionVariableSource = 'private ActionDef cachedAction;\ncachedAction.';
  const typedActionVariableLabels = echoScriptCompletionEntries(typedActionVariableSource, typedActionVariableSource.length, fakeIndex).map(entry => entry.label);
  assert(typedActionVariableLabels.includes('ActionKey') && typedActionVariableLabels.includes('Effects'), 'EchoScript member completions should infer explicitly typed builder variables.');

  const behaviorMethodSource = [
    'private BehaviorDef SashaBehavior()',
    '{',
    '    return Behavior().LookAtPlayers();',
    '}',
    'SashaBehavior().'
  ].join('\n');
  const behaviorMethodLabels = echoScriptCompletionEntries(behaviorMethodSource, behaviorMethodSource.length, fakeIndex).map(entry => entry.label);
  assert(behaviorMethodLabels.includes('RecoveryTicks') && behaviorMethodLabels.includes('Schedules'), 'EchoScript member completions should infer non-Action local method return types.');

  const nodesArraySource = 'private NodeDef[] Nodes() { return dialogueNodes; }\nNodes().';
  const nodesArrayLabels = echoScriptCompletionEntries(nodesArraySource, nodesArraySource.length, fakeIndex).map(entry => entry.label);
  assert(!nodesArrayLabels.includes('Line') && !nodesArrayLabels.includes('Choice'), 'EchoScript member completions should not treat NodeDef[] methods as Node() builders.');

  const speakerChainSource = 'return Speaker()\n    .';
  const speakerChainLabels = echoScriptCompletionEntries(speakerChainSource, speakerChainSource.length, fakeIndex).map(entry => entry.label);
  assert(speakerChainLabels.includes('DisplayName') && speakerChainLabels.includes('TalkDistance'), 'EchoScript fluent completions should infer Speaker() return chains.');

  const cameraChainLabels = echoScriptCompletionEntries('Camera().', 'Camera().'.length, fakeIndex).map(entry => entry.label);
  assert(cameraChainLabels.includes('DurationTicks') && cameraChainLabels.includes('FadeInTicks'), 'EchoScript member completions should infer Camera() builder chains.');
  const frameChainLabels = echoScriptCompletionEntries('Frame(0).', 'Frame(0).'.length, fakeIndex).map(entry => entry.label);
  assert(frameChainLabels.includes('Position') && frameChainLabels.includes('Rotation') && frameChainLabels.includes('Scale'), 'EchoScript member completions should infer Frame() transform modifiers.');

  const sceneNamespaceLabels = echoScriptCompletionEntries('Scene.', 'Scene.'.length, fakeIndex).map(entry => entry.label);
  assert(sceneNamespaceLabels.includes('Start') && !sceneNamespaceLabels.includes('LockPlayer'), 'EchoScript Scene. completions should keep namespace helpers separate from Scene() builder helpers.');

  const sceneBuilderLabels = echoScriptCompletionEntries('return Scene()\n    .', 'return Scene()\n    .'.length, fakeIndex).map(entry => entry.label);
  assert(sceneBuilderLabels.includes('LockPlayer') && sceneBuilderLabels.includes('Steps'), 'EchoScript fluent completions should infer Scene() builder members.');
  assert(sceneBuilderLabels.includes('OpeningCamera') && sceneBuilderLabels.includes('SpeakerAlias') && sceneBuilderLabels.includes('AbortEffects'), 'EchoScript Scene() completions should include compiler-supported alias and lifecycle members.');

  const animationLabels = echoScriptCompletionEntries('return Animation()\n    .', 'return Animation()\n    .'.length, fakeIndex).map(entry => entry.label);
  assert(animationLabels.includes('LengthTicks') && animationLabels.includes('Gun') && animationLabels.includes('Items'), 'EchoScript fluent completions should infer Animation() builder members.');

  const faceAnimationLabels = echoScriptCompletionEntries('FaceAnimation().', 'FaceAnimation().'.length, fakeIndex).map(entry => entry.label);
  assert(faceAnimationLabels.includes('BlinkTravelY') && faceAnimationLabels.includes('Parts'), 'EchoScript fluent completions should infer FaceAnimation() builder members.');

  const cameraPathLabels = echoScriptCompletionEntries('CameraPath().', 'CameraPath().'.length, fakeIndex).map(entry => entry.label);
  assert(cameraPathLabels.includes('Sample') && cameraPathLabels.includes('Samples'), 'EchoScript fluent completions should infer CameraPath() builder members.');

  const cutsceneLabels = echoScriptCompletionEntries('Cutscene().', 'Cutscene().'.length, fakeIndex).map(entry => entry.label);
  assert(cutsceneLabels.includes('Camera') && cutsceneLabels.includes('CameraPath'), 'EchoScript fluent completions should infer Cutscene() builder members.');

  const localizationLabels = echoScriptCompletionEntries('Localization().', 'Localization().'.length, fakeIndex).map(entry => entry.label);
  assert(localizationLabels.includes('Locale') && localizationLabels.includes('Entry'), 'EchoScript fluent completions should infer Localization() builder members.');

  const behaviorLabels = echoScriptCompletionEntries('Behavior().', 'Behavior().'.length, fakeIndex).map(entry => entry.label);
  assert(behaviorLabels.includes('LookAtPlayers') && behaviorLabels.includes('Schedules'), 'EchoScript fluent completions should infer Behavior() builder members.');

  const lineLabels = echoScriptCompletionEntries('Line(Sasha, "Hi").', 'Line(Sasha, "Hi").'.length, fakeIndex).map(entry => entry.label);
  assert(lineLabels.includes('TextKey') && lineLabels.includes('Face') && lineLabels.includes('Effects'), 'EchoScript fluent completions should infer Line() builder members.');
  assert(lineLabels.includes('SetAnimationFace') && lineLabels.includes('StopFaceAnimation') && lineLabels.includes('SetAnimationFaceClear'), 'EchoScript fluent completions should include current line face-animation helpers.');
  const lineAfterMoveLabels = echoScriptCompletionEntries('LineAfterMove(Sasha, "Hi").', 'LineAfterMove(Sasha, "Hi").'.length, fakeIndex).map(entry => entry.label);
  assert(lineAfterMoveLabels.includes('TextKey') && lineAfterMoveLabels.includes('Face') && lineAfterMoveLabels.includes('Effects'), 'EchoScript fluent completions should infer LineAfterMove() builder members.');

  const choiceLabels = echoScriptCompletionEntries('Choice("ask").', 'Choice("ask").'.length, fakeIndex).map(entry => entry.label);
  assert(choiceLabels.includes('Text') && choiceLabels.includes('Next') && choiceLabels.includes('Effects'), 'EchoScript fluent completions should infer Choice() builder members.');

  const nodeLabels = echoScriptCompletionEntries('Node("start").', 'Node("start").'.length, fakeIndex).map(entry => entry.label);
  assert(nodeLabels.includes('Line') && nodeLabels.includes('LineAfterMove') && nodeLabels.includes('Choice') && nodeLabels.includes('EnterEffects'), 'EchoScript fluent completions should infer Node() builder members.');

  const dialogueLabels = echoScriptCompletionEntries('Dialogue().', 'Dialogue().'.length, fakeIndex).map(entry => entry.label);
  assert(dialogueLabels.includes('Cutscene') && !dialogueLabels.includes('Start'), 'EchoScript Dialogue() completions should prefer non-legacy chain helpers.');

  const storyIndexLabels = echoScriptCompletionEntries('StoryIndex().', 'StoryIndex().'.length, fakeIndex).map(entry => entry.label);
  assert(storyIndexLabels.includes('Speakers') && storyIndexLabels.includes('FaceAnimations'), 'EchoScript fluent completions should infer StoryIndex() builder members.');

  const boneLabels = echoScriptCompletionEntries('Bone("head").', 'Bone("head").'.length, fakeIndex).map(entry => entry.label);
  assert(boneLabels.includes('Frames') && boneLabels.includes('Ik'), 'EchoScript fluent completions should infer Bone() builder members.');

  const partLabels = echoScriptCompletionEntries('Part("upperEyelids").', 'Part("upperEyelids").'.length, fakeIndex).map(entry => entry.label);
  assert(partLabels.includes('Frames'), 'EchoScript fluent completions should infer Part() builder members.');

  const sceneStartReferenceLabels = echoScriptCompletionEntries('Scene.Start(', 'Scene.Start('.length, fakeIndex).map(entry => entry.label);
  assert(sceneStartReferenceLabels.includes('Scenes.Chapter1RoomToHomeRelax') && sceneStartReferenceLabels.includes('Chapter1RoomToHomeRelax'), 'EchoScript Scene.Start argument completions should suggest indexed scenes.');
  assert(!sceneStartReferenceLabels.includes('Dialogues.Chapter1Room'), 'EchoScript Scene.Start argument completions should not suggest dialogue refs.');

  const dialogueStartReferenceLabels = echoScriptCompletionEntries('Dialogue.Start(', 'Dialogue.Start('.length, fakeIndex).map(entry => entry.label);
  assert(dialogueStartReferenceLabels.includes('Dialogues.Chapter1Room') && !dialogueStartReferenceLabels.includes('Scenes.Chapter1RoomToHomeRelax'), 'EchoScript Dialogue.Start argument completions should narrow refs to dialogues.');

  const dialogueStartAliasLabels = echoScriptCompletionEntries('DialogueStart(', 'DialogueStart('.length, fakeIndex).map(entry => entry.label);
  assert(dialogueStartAliasLabels.includes('Dialogues.Chapter1Room') && !dialogueStartAliasLabels.includes('Scenes.Chapter1RoomToHomeRelax'), 'EchoScript DialogueStart argument completions should narrow refs to dialogues.');

  const debugLabels = echoScriptCompletionEntries('Debug.', 'Debug.'.length, fakeIndex).map(entry => entry.label);
  assert(debugLabels.includes('Mark'), 'EchoScript member completions should include Debug.Mark.');
  const debugMarkArgLabels = echoScriptCompletionEntries('Debug.Mark(', 'Debug.Mark('.length, fakeIndex).map(entry => entry.label);
  assert(debugMarkArgLabels.includes('label'), 'EchoScript Debug.Mark argument completions should suggest label.');

  const npcPathReferenceLabels = echoScriptCompletionEntries('Npc.Path(', 'Npc.Path('.length, fakeIndex).map(entry => entry.label);
  assert(npcPathReferenceLabels.includes('Sasha') && !npcPathReferenceLabels.includes('Scenes.Chapter1RoomToHomeRelax'), 'EchoScript Npc.Path first argument completions should suggest NPC refs.');
  const npcStandUpReferenceLabels = echoScriptCompletionEntries('Npc.StandUp(', 'Npc.StandUp('.length, fakeIndex).map(entry => entry.label);
  assert(npcStandUpReferenceLabels.includes('Sasha') && !npcStandUpReferenceLabels.includes('Dialogues.Chapter1Room'), 'EchoScript Npc.StandUp first argument completions should suggest NPC refs.');
  const npcMoveArgumentLabels = echoScriptCompletionEntries('Npc.Move(Sasha, ', 'Npc.Move(Sasha, '.length, fakeIndex).map(entry => entry.label);
  assert(npcMoveArgumentLabels.includes('x') && npcMoveArgumentLabels.includes('dimension') && npcMoveArgumentLabels.includes('next'), 'EchoScript Npc.Move argument completions should include coordinate overload arguments.');
  const npcInteractArgumentLabels = echoScriptCompletionEntries('Npc.Interact(Sasha, ', 'Npc.Interact(Sasha, '.length, fakeIndex).map(entry => entry.label);
  assert(npcInteractArgumentLabels.includes('position') && npcInteractArgumentLabels.includes('targetBlock') && npcInteractArgumentLabels.includes('approach'), 'EchoScript Npc.Interact argument completions should include BlockPosition aliases and approach policy.');

  const npcAnimationReferenceLabels = echoScriptCompletionEntries('Npc.PlayAnimation(Sasha, ', 'Npc.PlayAnimation(Sasha, '.length, fakeIndex).map(entry => entry.label);
  assert(npcAnimationReferenceLabels.includes('Animations.NpcOfferItemHold') && !npcAnimationReferenceLabels.includes('Sasha'), 'EchoScript Npc.PlayAnimation second argument completions should suggest animation refs.');

  const thisAnimationReferenceLabels = echoScriptCompletionEntries('This.PlayAnimation(', 'This.PlayAnimation('.length, fakeIndex).map(entry => entry.label);
  assert(thisAnimationReferenceLabels.includes('Animations.NpcOfferItemHold') && thisAnimationReferenceLabels.includes('layer'), 'EchoScript This.PlayAnimation argument completions should suggest animation refs and named args.');

  const cameraPathCueReferenceLabels = echoScriptCompletionEntries('CameraPathCue(', 'CameraPathCue('.length, fakeIndex).map(entry => entry.label);
  assert(cameraPathCueReferenceLabels.includes('CameraPaths.Chapter1DriveCam') && !cameraPathCueReferenceLabels.includes('Animations.NpcOfferItemHold'), 'EchoScript CameraPathCue argument completions should suggest camera paths.');

  const vehicleCinematicReferenceLabels = echoScriptCompletionEntries('Vehicle.Sequence(Sasha, "car", "from", "dest", cinematic: ', 'Vehicle.Sequence(Sasha, "car", "from", "dest", cinematic: '.length, fakeIndex).map(entry => entry.label);
  assert(vehicleCinematicReferenceLabels.includes('CameraPaths.Chapter1DriveCam') && !vehicleCinematicReferenceLabels.includes('Dialogues.Chapter1Room'), 'EchoScript named cinematic argument completions should suggest camera paths.');

  const npcThenDialogueReferenceLabels = echoScriptCompletionEntries('Npc.Move(Sasha, Vec(1, 2, 3), thenDialogue: ', 'Npc.Move(Sasha, Vec(1, 2, 3), thenDialogue: '.length, fakeIndex).map(entry => entry.label);
  assert(npcThenDialogueReferenceLabels.includes('Dialogues.Chapter1Room') && !npcThenDialogueReferenceLabels.includes('Scenes.Chapter1RoomToHomeRelax'), 'EchoScript thenDialogue argument completions should suggest dialogue refs.');

  const lineSpeakerReferenceLabels = echoScriptCompletionEntries('Line(', 'Line('.length, fakeIndex).map(entry => entry.label);
  assert(lineSpeakerReferenceLabels.includes('Sasha') && lineSpeakerReferenceLabels.includes('Speakers.Buyer'), 'EchoScript Line first argument completions should suggest speakers and NPC refs.');
  const lineAfterMoveSpeakerReferenceLabels = echoScriptCompletionEntries('LineAfterMove(', 'LineAfterMove('.length, fakeIndex).map(entry => entry.label);
  assert(lineAfterMoveSpeakerReferenceLabels.includes('Sasha') && lineAfterMoveSpeakerReferenceLabels.includes('Speakers.Buyer'), 'EchoScript LineAfterMove first argument completions should suggest speakers and NPC refs.');

  const lineFaceReferenceLabels = echoScriptCompletionEntries('Line(Sasha, "Hi", face: ', 'Line(Sasha, "Hi", face: '.length, fakeIndex).map(entry => entry.label);
  assert(lineFaceReferenceLabels.includes('FaceAnimations.SashaSmile') && !lineFaceReferenceLabels.includes('Animations.NpcOfferItemHold'), 'EchoScript face argument completions should suggest face animation refs.');

  const questStatusArgLabels = echoScriptCompletionEntries('Quest.Update(OldFriend.Quest, status: ', 'Quest.Update(OldFriend.Quest, status: '.length, fakeIndex).map(entry => entry.label);
  assert(questStatusArgLabels.includes('QuestStatus.Active') && questStatusArgLabels.includes('QuestStatus.Completed') && !questStatusArgLabels.includes('Scenes.Chapter1RoomToHomeRelax'), 'EchoScript status argument completions should suggest quest statuses.');

  const layerArgLabels = echoScriptCompletionEntries('This.PlayAnimation(Animations.NpcOfferItemHold, layer: ', 'This.PlayAnimation(Animations.NpcOfferItemHold, layer: '.length, fakeIndex).map(entry => entry.label);
  assert(layerArgLabels.includes('Layer.Overlay') && !layerArgLabels.includes('Layer.Body'), 'EchoScript layer argument completions should suggest supported layers.');

  const handArgLabels = echoScriptCompletionEntries('This.HoldItem(Items.SparkGrass, hand: ', 'This.HoldItem(Items.SparkGrass, hand: '.length, fakeIndex).map(entry => entry.label);
  assert(handArgLabels.includes('Hand.Main') && !handArgLabels.includes('Hand.Mainhand'), 'EchoScript hand argument completions should suggest supported hands.');

  const questArgLabels = echoScriptCompletionEntries('Quest.Update(Chapter1Radio.Quest, ', 'Quest.Update(Chapter1Radio.Quest, '.length, fakeIndex).map(entry => entry.label);
  assert(questArgLabels.includes('title') && questArgLabels.includes('stage') && questArgLabels.includes('showNotification'), 'EchoScript argument completions should suggest Quest.Update named args.');

  const usedQuestArgLabels = echoScriptCompletionEntries('Quest.Update(Chapter1Radio.Quest, title: "Old radio", ', 'Quest.Update(Chapter1Radio.Quest, title: "Old radio", '.length, fakeIndex).map(entry => entry.label);
  assert(!usedQuestArgLabels.includes('title') && usedQuestArgLabels.includes('body'), 'EchoScript argument completions should hide already-used named args.');

  const npcPathArgLabels = echoScriptCompletionEntries('Npc.Path(Sasha, [Vec(1, 2, 3)], ', 'Npc.Path(Sasha, [Vec(1, 2, 3)], '.length, fakeIndex).map(entry => entry.label);
  assert(npcPathArgLabels.includes('speed') && npcPathArgLabels.includes('openDoors') && npcPathArgLabels.includes('thenDialogue'), 'EchoScript argument completions should suggest Npc.Path named args.');

  const blackScreenArgLabels = echoScriptCompletionEntries('BlackScreen.Show(', 'BlackScreen.Show('.length, fakeIndex).map(entry => entry.label);
  assert(blackScreenArgLabels.includes('fadeInTicks') && blackScreenArgLabels.includes('onCoveredSignal'), 'EchoScript argument completions should suggest BlackScreen.Show named args.');

  const profileNameModeLabels = echoScriptCompletionEntries('PlayerProfile(nameMode: ', 'PlayerProfile(nameMode: '.length, fakeIndex).map(entry => entry.label);
  assert(profileNameModeLabels.includes('NameMode.Account') && profileNameModeLabels.includes('NameMode.Custom'), 'EchoScript PlayerProfile nameMode completions should suggest enum values.');

  const weaponRarityLabels = echoScriptCompletionEntries('Weapon(rarity: ', 'Weapon(rarity: '.length, fakeIndex).map(entry => entry.label);
  assert(weaponRarityLabels.includes('Rarity.Common') && weaponRarityLabels.includes('Rarity.Epic'), 'EchoScript Weapon rarity completions should suggest enum values.');

  const triggerArgLabels = echoScriptCompletionEntries('PlayerEnter(', 'PlayerEnter('.length, fakeIndex).map(entry => entry.label);
  assert(triggerArgLabels.includes('area'), 'EchoScript trigger event completions should suggest named args.');

  const subscribeLabels = echoScriptCompletionEntries('Subscribe(', 'Subscribe('.length, fakeIndex).map(entry => entry.label);
  assert(subscribeLabels.includes('selector') && subscribeLabels.includes('seatedAt') && subscribeLabels.includes('changedSeatIn'), 'EchoScript Subscribe completions should suggest sleep and seating selector arguments.');

  assertEqual(pascalFromId('chapter_1_room_to_home_relax'), 'Chapter1RoomToHomeRelax', 'EchoScript completion ids should convert to PascalCase symbols.');
}

function verifyCatalog() {
  const catalog = createStoryCatalog();
  assert(catalog.npcCommandTypes.has('spawn'), 'Catalog should include NPC command type spawn.');
  assert(catalog.npcCommandTypes.has('clear_held_item'), 'Catalog should include NPC held item aliases.');
  assert(catalog.worldCommandTypes.has('set_block'), 'Catalog should include world command type set_block.');
  assert(catalog.commandTypes.has('vehicle_trip'), 'Catalog should merge command types.');
  assert(!catalog.effectTypes.get('if').fields.includes('when'), 'Conditional effect catalog should not suggest when.');
  assert(catalog.fieldDocs.targetId.includes('navigation targets'), 'Catalog should document navigation target ids.');
  assert(catalog.fieldDocs.profile.includes('GPS'), 'Catalog should document navigation profiles.');
}

function verifyLocalStoryContext() {
  const root = {
    speakerAliases: { a: 'speaker_a' },
    start: 'start',
    nodes: {
      start: {
        choices: [
          { id: 'go', text: 'Go', next: 'end' }
        ],
        timer: { defaultChoiceId: 'go' }
      },
      end: { say: ['Done'] }
    }
  };
  assertEqual(dialogueNodeIdsFromValue(root).join(','), 'end,start', 'Dialogue node ids should be read from nodes object.');
  assertEqual(dialogueChoiceIdsFromValue(root, 'start').join(','), 'go', 'Dialogue choice ids should be read from current node.');
  assertEqual(speakerAliasTargetsFromValue(root)[0].target, 'speaker_a', 'Speaker aliases should expose alias targets.');
}

function verifyValueCompletionModel() {
  const catalog = createStoryCatalog();
  assert(isBooleanField('enabled'), 'Value completion model should recognize enabled as a boolean field.');
  assert(isBooleanField('fireOnce'), 'Value completion model should recognize fireOnce as a boolean field.');
  assertEqual(scalarValuesForField(catalog, 'visible').join(','), 'true,false', 'Boolean fields should complete JSON5 true/false literals.');
  assertEqual(scalarValuesForField(catalog, 'dialogueId').length, 0, 'Reference string fields should not complete unquoted boolean literals.');
  assert(scalarValuesForField(catalog, 'backend').includes('native_preferred'), 'Vehicle backend fields should complete runtime backend ids.');
  assert(scalarValuesForField(catalog, 'category').includes('car'), 'Vehicle category fields should complete runtime vehicle categories.');
  assert(scalarValuesForField(catalog, 'completionPolicy').includes('explicit_timeline_end'), 'Cinematic completion policy fields should complete runtime policy ids.');
  assert(scalarValuesForField(catalog, 'collisionPolicy').includes('ignore_collision'), 'Camera collision policy fields should complete runtime policy ids.');
}

function verifyAuthoringKeyModel() {
  assertEqual(keyCompletionRole({
    fieldName: 'key',
    objectContext: 'trigger',
    documentKind: 'event',
    pathParts: ['trigger'],
    typeValue: 'manual'
  }), 'signal', 'Trigger key should complete signal keys.');
  assertEqual(keyCompletionRole({
    fieldName: 'key',
    objectContext: 'effect',
    documentKind: 'dialogue',
    pathParts: ['nodes', 'start', 'effects', 0],
    typeValue: 'set_state'
  }), 'state', 'set_state key should complete state keys.');
  assertEqual(keyCompletionRole({
    fieldName: 'key',
    objectContext: 'dialogueRoot',
    documentKind: 'dialogue',
    pathParts: ['nodes', 'start', 'say', 0, 2],
    typeValue: ''
  }), 'localization', 'Compact dialogue line key should complete localization keys.');

  const keys = authoringKeyEntriesFromValue({
    trigger: { type: 'manual', key: 'intro.video.finished' },
    effects: [
      { type: 'set_state', key: 'memory.chapter.started', value: true },
      { type: 'black_screen', onClearedSignal: 'intro.video.cleared' }
    ]
  }, {
    id: 'smoke_event',
    kind: 'event',
    path: 'events/smoke_event.json5',
    relativePath: 'story/events/smoke_event.json5'
  });
  assert(keys.signalKeys.some(entry => entry.id === 'intro.video.finished'), 'Authoring key extractor should find trigger signal keys.');
  assert(keys.signalKeys.some(entry => entry.id === 'intro.video.cleared'), 'Authoring key extractor should find on*Signal keys.');
  assert(keys.stateKeys.some(entry => entry.id === 'memory.chapter.started'), 'Authoring key extractor should find set_state keys.');
}

function verifyLocalizationKeyModel() {
  const entries = localizationKeyEntriesFromValue({
    locale: 'en_us',
    prefix: 'story',
    entries: {
      intro: { wake: { button: 'Wake up' } },
      'story.flat.key': 'Flat'
    }
  }, {
    path: 'localization/en_us/chapter_1.json5',
    relativePath: 'story/localization/en_us/chapter_1.json5'
  });
  const keys = entries.map(entry => entry.key);
  assert(keys.includes('story.intro.wake.button'), 'Localization model should include prefixed full keys.');
  assert(keys.includes('intro.wake.button'), 'Localization model should include story-prefix authoring aliases.');
  assert(keys.includes('flat.key'), 'Localization model should avoid double story prefixes for flat keys.');
}

function verifyQuestVisuals() {
  assertEqual(normalizeQuestStatus('ACTIVE'), 'active', 'Quest status ACTIVE should normalize.');
  assertEqual(normalizeQuestStatus('completed'), 'completed', 'Quest status completed should normalize.');
  assertEqual(normalizeQuestStatus('failed'), 'failed', 'Quest status failed should normalize.');
  assertEqual(normalizeQuestStatus(''), 'incomplete', 'Missing quest status should normalize to incomplete.');

  const source = [
    '{',
    '\teffects: [',
    '\t\t{ type: "quest_update", questId: "quest_active", stageId: "start", status: "ACTIVE" },',
    '\t\t{ type: "quest_update", questId: "quest_done", status: "COMPLETED" },',
    '\t\t{ type: "quest_update", questId: "quest_missing_status", stageId: "todo" },',
    '\t],',
    '}'
  ].join('\n');
  const quests = questVisualsInText(source);
  assertEqual(quests.length, 3, 'Quest visuals should find quest_update objects.');
  assert(quests.some(quest => quest.questId === 'quest_active' && quest.status === 'active'), 'Quest visuals should mark active quests.');
  assert(quests.some(quest => quest.questId === 'quest_done' && quest.status === 'completed'), 'Quest visuals should mark completed quests.');
  assert(quests.some(quest => quest.questId === 'quest_missing_status' && quest.status === 'incomplete'), 'Quest visuals should mark missing status as incomplete.');
  assert(questCodeLensTitle(quests[0]).includes('Quest ACTIVE'), 'Quest CodeLens title should include normalized status.');
}

function verifyReferenceScanner() {
  const source = [
    '{',
    '\teffects: [',
    '\t\t{ type: "start_dialogue", dialogueId: "intro" },',
    '\t\t"scene:opening",',
    '\t],',
    '}'
  ].join('\n');
  const occurrences = referenceOccurrencesInText(source);
  assert(occurrences.some(entry => entry.kind === 'dialogue' && entry.id === 'intro'), 'Reference scanner should find dialogueId references.');
  assert(occurrences.some(entry => entry.kind === 'scene' && entry.id === 'opening'), 'Reference scanner should find scene shortcuts.');
  assertEqual(referenceRangesInText(source, { kind: 'scene', id: 'opening' }, 'event', false).length, 1, 'Reference scanner should return scene shortcut ranges.');
  const renameEdits = referenceRenameEditsInText(source, { kind: 'scene', id: 'opening' }, 'event', 'opening_v2', false);
  assert(renameEdits.some(edit => edit.newText === 'scene:opening_v2'), 'Reference rename should preserve shortcut prefixes.');
}

function verifyDialogueNodeRename() {
  const objectNodes = [
    '{',
    '\tstart: "start",',
    '\tnodes: {',
    '\t\tstart: { next: "end" },',
    '\t\tend: { say: ["Done"] },',
    '\t},',
    '}'
  ].join('\n');
  const objectRanges = dialogueNodeRenameRanges(objectNodes, 'start');
  assert(objectRanges.length >= 2, 'Dialogue node rename should include object declaration and references.');
  assert(dialogueNodeRenameTarget(objectNodes, objectNodes.indexOf('start: {') + 1).id === 'start', 'Dialogue node rename target should resolve object node keys.');

  const arrayNodes = [
    '{',
    '\tstartNodeId: "start",',
    '\tnodes: [',
    '\t\t{ id: "start", next: "end" },',
    '\t\t{ id: "end", say: ["Done"] },',
    '\t],',
    '}'
  ].join('\n');
  assert(dialogueNodeDeclarationRanges(arrayNodes).some(entry => entry.id === 'start'), 'Dialogue node rename should find array node id declarations.');
  assert(dialogueNodeReferenceRanges(arrayNodes).some(entry => entry.id === 'start'), 'Dialogue node rename should find local node references.');
  assert(dialogueNodeRenameRanges(arrayNodes, 'start').length >= 2, 'Dialogue node rename should include array declarations and references.');
}

async function verifyFormatter() {
  const source = [
    '// top',
    '{ id: "smoke", // keep inline',
    ' effects:[{type:"notify",title:"Title",text:"Text"}], /* keep block */',
    '}'
  ].join('\n');
  const formatter = new CommentPreservingJson5Formatter();
  const output = await formatter.format(source, {
    useTabs: true,
    tabSize: 2,
    trailingComma: true,
    quote: 'double'
  });

  assert(output.includes('// top'), 'Formatter must preserve leading line comments.');
  assert(output.includes('// keep inline'), 'Formatter must preserve inline comments.');
  assert(output.includes('/* keep block */'), 'Formatter must preserve block comments.');
  assert(output.includes('\n\t'), 'Formatter must use tabs when configured.');
  assert(output.trimEnd().endsWith('}'), 'Formatter output should end with the JSON5 object.');
}

async function verifyIndex(modRoot) {
  assertDirectory(modRoot);
  const parser = new Json5Parser();
  const config = {
    modRoot: () => modRoot,
    storyDataDirectory: () => path.join(modRoot, 'echo-story-content-pack', 'src', 'main', 'resources', 'data'),
    storyDataDirectories: () => [path.join(modRoot, 'echo-story-content-pack', 'src', 'main', 'resources', 'data')],
    templatesDirectory: () => path.join(modRoot, 'echo-story-content-pack', 'authoring', 'templates')
  };
  const index = new StoryWorkspaceIndex(config, parser);
  await index.refresh();
  const summary = index.summary();
  assert(summary.stories > 0, 'Story index should find StoryResource files.');
  assert(summary.dialogues > 0, 'Story index should find dialogues.');
  assert(summary.npcs > 0, 'Story index should find NPC resources.');
  assert(summary.animations > 0, 'Story index should find authored animations.');
  assert(summary.faceAnimations > 0, 'Story index should find face animations.');
  assert(summary.animationProfiles > 0, 'Story index should find animation profiles.');
  assert(summary.animationRigs > 0, 'Story index should find animation rigs.');
  assert(summary.animationClips > 0, 'Story index should find animation clips.');
  assert(summary.faceRigs > 0, 'Story index should find face rigs.');
  assert(summary.facePoses > 0, 'Story index should find face poses.');
  assert(summary.npcAnimationBindings > 0, 'Story index should find NPC animation bindings.');
  assert(summary.stateKeys > 0, 'Story index should find state keys.');
  assert(summary.templates > 0, 'Story index should find templates.');
  assert(index.snapshot().templates.some(entry => entry.path.endsWith('.es.template')), 'Story index should include EchoScript templates.');
  assert(index.entries().length > 0, 'Story index should expose workspace symbol entries.');
  assert(index.location('story', 'jdh:chapter_1_home_arrival')?.path.endsWith('home_arrival.es'), 'Story index should index StoryResource ids from Story(...).');
  assert(index.location('dialogue', 'jdh:chapter_1_home_arrival')?.path.endsWith('home_arrival.dialogue.es'), 'Story index should index DialogueResource ids from Id(...)/Dialogue(...).');
  assert(index.location('npc', 'jdh:john_father')?.path.endsWith('john_father.npc.es'), 'Story index should index NpcResource ids from Npc(id: ...).');
  assert(index.location('animation', 'jdh:wave')?.path.endsWith('wave.animation.es'), 'Story index should index AnimationResource ids.');
  assert(index.location('faceAnimation', 'jdh:john_father/look_left')?.path.endsWith('look_left.face-animation.es'), 'Story index should index FacialAnimationResource ids.');
  assert(index.location('animationProfile', 'jdh:standard_humanoid')?.path.endsWith('standard_humanoid.animation-profile.es'), 'Story index should index AnimationProfileResource ids.');
  assert(index.location('animationRig', 'jdh:humanoid')?.path.endsWith('humanoid.animation-rig.es'), 'Story index should index AnimationRigResource ids.');
  assert(index.location('animationClip', 'jdh:wave_loop')?.path.endsWith('wave_loop.animation-clip.es'), 'Story index should index AnimationClipResource ids.');
  assert(index.location('faceRig', 'jdh:john_father')?.path.endsWith('john_father.face-rig.es'), 'Story index should index high-level face rig DSL ids.');
  assert(index.location('facePose', 'jdh:john_father/neutral')?.path.endsWith('john_father_neutral.face-pose.es'), 'Story index should index FacePoseResource ids.');
  assert(index.location('npcAnimationBinding', 'jdh:john_father_standard_humanoid')?.path.endsWith('john_father.npc-animation-binding.es'), 'Story index should index NPC animation bindings.');
  assert(index.stateKeys().some(key => key === 'jdh.chapter_1.home_arrival_dialogue_seen'), 'Story index should expose state keys from StoryResource Set(... variable: ...).');

  const firstStory = referenceIds(index, 'story')[0];
  const firstDialogue = referenceIds(index, 'dialogue')[0];
  const firstNpc = referenceIds(index, 'npc')[0];
  const firstAnimation = referenceIds(index, 'animation')[0];
  assert(resolveReference(index, 'story', firstStory), 'Resolver should resolve a story id.');
  assert(resolveReference(index, 'dialogue', firstDialogue), 'Resolver should resolve a dialogue id.');
  assert(resolveReference(index, 'npc', firstNpc), 'Resolver should resolve an NPC or speaker id.');
  assert(resolveReference(index, 'animation', firstAnimation), 'Resolver should resolve humanoid or face animation ids.');
  assertEqual(shortcutReference(`dialogue:${firstDialogue}`).kind, 'dialogue', 'Resolver should parse dialogue string shortcuts.');

  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'echotool-index-'));
  try {
    const dataRoot = path.join(tempRoot, 'data');
    const environmentDir = path.join(dataRoot, 'jdh', 'echo_story', 'environments', 'home');
    const formDir = path.join(dataRoot, 'jdh', 'echo_npc', 'forms', 'sasha');
    const playerProfileDir = path.join(dataRoot, 'jdh', 'echo_player', 'profile_templates');
    const weaponDir = path.join(dataRoot, 'jdh', 'echo_item', 'items', 'weapons');
    const triggerDir = path.join(dataRoot, 'jdh', 'echo_trigger', 'triggers', 'home');
    fs.mkdirSync(environmentDir, { recursive: true });
    fs.mkdirSync(formDir, { recursive: true });
    fs.mkdirSync(playerProfileDir, { recursive: true });
    fs.mkdirSync(weaponDir, { recursive: true });
    fs.mkdirSync(triggerDir, { recursive: true });
    fs.writeFileSync(path.join(environmentDir, 'kitchen_table.es'), [
      'class KitchenTableEnvironment : EnvironmentResource',
      '{',
      '    public override EnvironmentResourceDef Build()',
      '    {',
      '        return Environment("jdh:home/kitchen_table", "minecraft:overworld", [State("morning_breakfast", [])]);',
      '    }',
      '}'
    ].join('\n'));
    fs.writeFileSync(path.join(formDir, 'child.form.es'), [
      'class SashaChildForm : NpcFormResource',
      '{',
      '    public override NpcFormResourceDef Build()',
      '    {',
      '        return Form(id: "jdh:sasha/child", skin: Skin("jdh:sasha"), dimensions: Dimensions(0.6, 1.4, 1.2));',
      '    }',
      '}'
    ].join('\n'));
    fs.writeFileSync(path.join(playerProfileDir, 'default.player.es'), [
      'class DefaultPlayerProfile : PlayerProfileTemplate',
      '{',
      '    private string TemplateId = "jdh:player/default";',
      '    public override PlayerProfile Build()',
      '    {',
      '        return PlayerProfile(id: TemplateId, nameMode: ACCOUNT, spark: 100, maxSpark: 100, skinMode: ACCOUNT);',
      '    }',
      '}'
    ].join('\n'));
    fs.writeFileSync(path.join(weaponDir, 'spark_sword.weapon.es'), [
      'class SparkSword : WeaponResource',
      '{',
      '    private string ItemId = "jdh:spark_sword";',
      '    public override WeaponDefinition Build()',
      '    {',
      '        return Weapon(id: ItemId, name: "Spark Sword", texture: "jdh:item/spark_sword", damage: 7, attacksPerSecond: 1.6, rarity: COMMON);',
      '    }',
      '}'
    ].join('\n'));
    fs.writeFileSync(path.join(triggerDir, 'door.trigger.es'), [
      'class DoorTrigger : TriggerResource',
      '{',
      '    private Area DoorArea = Area("minecraft:overworld", 0, 64, 0, 2, 3, 2);',
      '    private void Init()',
      '    {',
      '        Id("jdh:home/door");',
      '        Scope(PLAYER);',
      '        Mode(REPEAT);',
      '        Start(Sequence);',
      '    }',
      '    private void Sequence()',
      '    {',
      '        Wait(PlayerEnter(DoorArea));',
      '    }',
      '}'
    ].join('\n'));
    const tempIndex = new StoryWorkspaceIndex({
      modRoot: () => tempRoot,
      storyDataDirectory: () => dataRoot,
      storyDataDirectories: () => [dataRoot],
      templatesDirectory: () => path.join(tempRoot, 'templates')
    }, parser);
    await tempIndex.refresh();
    const tempSummary = tempIndex.summary();
    assertEqual(tempSummary.environments, 1, 'Story index should find EnvironmentResource files.');
    assertEqual(tempSummary.npcForms, 1, 'Story index should find NpcFormResource files.');
    assertEqual(tempSummary.playerProfileTemplates, 1, 'Story index should find PlayerProfileTemplate files.');
    assertEqual(tempSummary.weapons, 1, 'Story index should find WeaponResource files.');
    assertEqual(tempSummary.triggers, 1, 'Story index should find TriggerResource files.');
    assert(tempIndex.location('environment', 'jdh:home/kitchen_table')?.path.endsWith('kitchen_table.es'), 'Story index should index EnvironmentResource ids.');
    assert(tempIndex.location('npcForm', 'jdh:sasha/child')?.path.endsWith('child.form.es'), 'Story index should index NpcFormResource ids.');
    assert(tempIndex.location('playerProfileTemplate', 'jdh:player/default')?.path.endsWith('default.player.es'), 'Story index should index PlayerProfileTemplate ids.');
    assert(tempIndex.location('weapon', 'jdh:spark_sword')?.path.endsWith('spark_sword.weapon.es'), 'Story index should index WeaponResource ids.');
    assert(tempIndex.location('trigger', 'jdh:home/door')?.path.endsWith('door.trigger.es'), 'Story index should index TriggerResource ids.');
    assert(resolveReference(tempIndex, 'environment', 'jdh:home/kitchen_table'), 'Resolver should resolve environment ids.');
    assert(resolveReference(tempIndex, 'npcForm', 'jdh:sasha/child'), 'Resolver should resolve NPC form ids.');
    assert(resolveReference(tempIndex, 'playerProfileTemplate', 'jdh:player/default'), 'Resolver should resolve player profile template ids.');
    assert(resolveReference(tempIndex, 'weapon', 'jdh:spark_sword'), 'Resolver should resolve weapon ids.');
    assert(resolveReference(tempIndex, 'trigger', 'jdh:home/door'), 'Resolver should resolve trigger ids.');
  } finally {
    fs.rmSync(tempRoot, { recursive: true, force: true });
  }
}

function verifyInstalledCopyIfRequested(manifest) {
  if (process.env.ECHOSTORY_REQUIRE_INSTALLED !== '1') {
    return;
  }

  const folder = `${manifest.publisher}.${manifest.name}-${manifest.version}`;
  const installRoot = path.join(process.env.USERPROFILE || '', '.vscode', 'extensions', folder);
  assertDirectory(installRoot);
  assertFile(path.join(installRoot, 'package.json'));
  assertFile(path.join(installRoot, 'icon.png'));
  assertFile(path.join(installRoot, 'src', 'extension.js'));
  assertFile(path.join(installRoot, 'src', 'language', 'dialogueNodeRename.js'));
  assertFile(path.join(installRoot, 'src', 'language', 'echoscriptCompletionModel.js'));
  assertFile(path.join(installRoot, 'src', 'language', 'echoscriptCompletionProvider.js'));
  assertFile(path.join(installRoot, 'src', 'language', 'echoscriptNavigation.js'));
  assertFile(path.join(installRoot, 'src', 'language', 'echoscriptSignatureHelp.js'));
  assertFile(path.join(installRoot, 'src', 'language', 'echoscriptSignatureHelpProvider.js'));
  assertFile(path.join(installRoot, 'src', 'language', 'echoscriptStaticSymbols.js'));
  assertFile(path.join(installRoot, 'src', 'language', 'inlayHintsProvider.js'));
  assertFile(path.join(installRoot, 'src', 'language', 'questCodeLensProvider.js'));
  assertFile(path.join(installRoot, 'src', 'language', 'questDecorationsService.js'));
  assertFile(path.join(installRoot, 'src', 'language', 'questVisuals.js'));
  assertFile(path.join(installRoot, 'src', 'language', 'referencesProvider.js'));
  assertFile(path.join(installRoot, 'src', 'language', 'renameProvider.js'));
  assertFile(path.join(installRoot, 'src', 'language', 'storyReferenceScanner.js'));
  assertFile(path.join(installRoot, 'src', 'language', 'valueCompletionModel.js'));
  assertFile(path.join(installRoot, 'src', 'shared', 'authoringKeyModel.js'));
  assertFile(path.join(installRoot, 'src', 'workspace', 'authoringKeys.js'));
  assertFile(path.join(installRoot, 'src', 'workspace', 'localizationKeys.js'));
  assertFile(path.join(installRoot, 'syntaxes', 'echoscript.tmLanguage.json'));
  assertFile(path.join(installRoot, 'snippets', 'echoscript.code-snippets'));
  assertFile(path.join(installRoot, 'package.nls.ru.json'));
  assertDirectory(path.join(installRoot, 'node_modules', 'json5'));
  assertDirectory(path.join(installRoot, 'node_modules', 'prettier'));
}

function assertFile(filePath) {
  assert(fs.existsSync(filePath) && fs.statSync(filePath).isFile(), `Missing file: ${filePath}`);
}

function assertDirectory(directoryPath) {
  assert(fs.existsSync(directoryPath) && fs.statSync(directoryPath).isDirectory(), `Missing directory: ${directoryPath}`);
}

function assertEqual(actual, expected, message) {
  assert(actual === expected, `${message} Expected "${expected}", got "${actual}".`);
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

main().catch(error => {
  console.error(error.stack || error.message || String(error));
  process.exit(1);
});
