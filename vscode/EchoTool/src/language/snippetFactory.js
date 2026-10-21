'use strict';

const vscode = require('vscode');

const PROPERTY_SNIPPETS = {
  id: 'id: "${1:id}",',
  type: 'type: "${1:notify}",',
  key: 'key: "${1:memory.flag}",',
  trigger: [
    'trigger: {',
    '\ttype: "${1:manual}",',
    '\tkey: "${2:story.signal}",',
    '},'
  ].join('\n'),
  condition: 'condition: { type: "${1:always_true}" },',
  if: 'if: { type: "${1:state_exists}", key: "${2:memory.flag}" },',
  effects: [
    'effects: [',
    '\t{ type: "${1:notify}", title: "${2:Title}", text: "${3:Text}" },',
    '],'
  ].join('\n'),
  do: [
    'do: [',
    '\t{ type: "${1:notify}", title: "${2:Title}", text: "${3:Text}" },',
    '],'
  ].join('\n'),
  then: [
    'then: [',
    '\t{ type: "${1:set_state}", key: "${2:memory.flag}", value: true },',
    '],'
  ].join('\n'),
  else: 'else: [],',
  command: [
    'command: {',
    '\ttype: "${1:spawn}",',
    '\tnpcId: "${2:npc_id}",',
    '},'
  ].join('\n'),
  signal: [
    'signal: {',
    '\ttype: "${1:manual}",',
    '\tkey: "${2:story.signal}",',
    '},'
  ].join('\n'),
  min: 'min: { x: ${1:0}, y: ${2:64}, z: ${3:0} },',
  max: 'max: { x: ${1:4}, y: ${2:68}, z: ${3:4} },',
  at: 'at: { x: ${1:0}, y: ${2:64}, z: ${3:0} },',
  pos: 'pos: { x: ${1:0}, y: ${2:64}, z: ${3:0} },',
  position: 'position: { x: ${1:0}, y: ${2:64}, z: ${3:0} },',
  route: [
    'route: [',
    '\t{ x: ${1:0}, y: ${2:64}, z: ${3:0} },',
    '\t{ x: ${4:4}, y: ${5:64}, z: ${6:4} },',
    '],'
  ].join('\n'),
  points: [
    'points: [',
    '\t{ x: ${1:0}, y: ${2:64}, z: ${3:0} },',
    '],'
  ].join('\n'),
  marker: [
    'marker: {',
    '\tdimensionId: "minecraft:overworld",',
    '\tx: ${1:0},',
    '\ty: ${2:64},',
    '\tz: ${3:0},',
    '},'
  ].join('\n'),
  objectives: [
    'objectives: [',
    '\t{ id: "${1:objective_id}", text: "${2:Objective text}", done: false },',
    '],'
  ].join('\n'),
  payload: [
    'payload: {',
    '\t${1:key}: ${2:value},',
    '},'
  ].join('\n'),
  nodes: [
    'nodes: {',
    '\tstart: {',
    '\t\tsay: ["${1:Line text}"],',
    '\t},',
    '},'
  ].join('\n'),
  choices: [
    'choices: [',
    '\t{ id: "${1:choice_id}", text: "${2:Choice text}", next: "${3:next_node}" },',
    '],'
  ].join('\n'),
  steps: [
    'steps: [',
    '\t{ type: "wait", ticks: ${1:10} },',
    '\t{ type: "say_line", speaker: "${2:speaker_id}", text: "${3:Line text}" },',
    '],'
  ].join('\n'),
  speakerAliases: [
    'speakerAliases: {',
    '\t${1:a}: "${2:speaker_id}",',
    '},'
  ].join('\n'),
  interactions: [
    'interactions: [',
    '\t{ id: "${1:talk}", label: "${2:Talk}", dialogue: "${3:dialogue_id}" },',
    '],'
  ].join('\n'),
  interactionActions: [
    'interactionActions: [',
    '\t{',
    '\t\tid: "${1:talk}",',
    '\t\tlabel: "${2:Talk}",',
    '\t\teffects: [{ type: "start_dialogue", dialogueId: "${3:dialogue_id}" }],',
    '\t},',
    '],'
  ].join('\n'),
  spawn: [
    'spawn: {',
    '\tdimensionId: "minecraft:overworld",',
    '\tx: ${1:0},',
    '\ty: ${2:64},',
    '\tz: ${3:0},',
    '},'
  ].join('\n'),
  behavior: [
    'behavior: {',
    '\trecoveryTicks: ${1:60},',
    '\tschedules: [],',
    '},'
  ].join('\n'),
  npcBehavior: [
    'npcBehavior: {',
    '\trecoveryTicks: ${1:60},',
    '\tschedules: [],',
    '},'
  ].join('\n')
};

const ROOT_SNIPPETS = {
  event: {
    label: 'EchoStory event root',
    body: [
      '{',
      '\tid: "${1:event_id}",',
      '\ttrigger: { type: "manual", key: "${2:story.signal}" },',
      '\tcondition: { type: "always_true" },',
      '\teffects: [',
      '\t\t{ type: "start_dialogue", dialogueId: "${3:dialogue_id}" },',
      '\t],',
      '\tfireOnce: true,',
      '}'
    ]
  },
  dialogue: {
    label: 'EchoStory dialogue root',
    body: [
      '{',
      '\tid: "${1:dialogue_id}",',
      '\tspeaker: "${2:speaker_id}",',
      '\tstart: "start",',
      '\tnodes: {',
      '\t\tstart: {',
      '\t\t\tsay: ["${3:Line text}"],',
      '\t\t},',
      '\t},',
      '}'
    ]
  },
  scene: {
    label: 'EchoStory scene root',
    body: [
      '{',
      '\tid: "${1:scene_id}",',
      '\tlockPlayer: true,',
      '\tskippable: true,',
      '\tsteps: [',
      '\t\t{ type: "wait", ticks: ${2:10} },',
      '\t\t{ type: "say_line", speaker: "${3:speaker_id}", text: "${4:Line text}" },',
      '\t],',
      '}'
    ]
  },
  speaker: {
    label: 'EchoStory speaker root',
    body: [
      '{',
      '\tid: "${1:speaker_id}",',
      '\tdisplayName: "${2:Display Name}",',
      '\tinteractionDialogue: "${3:dialogue_id}",',
      '\tinteractionActions: [],',
      '}'
    ]
  },
  npc: {
    label: 'EchoStory NPC root',
    body: [
      '{',
      '\tid: "${1:npc_id}",',
      '\tdisplayName: "${2:Display Name}",',
      '\tspeaker: "${1:npc_id}",',
      '\tspawn: {',
      '\t\tdimensionId: "minecraft:overworld",',
      '\t\tx: ${3:0},',
      '\t\ty: ${4:64},',
      '\t\tz: ${5:0},',
      '\t},',
      '}'
    ]
  },
  animation: {
    label: 'EchoStory animation root',
    body: [
      '{',
      '\tid: "${1:animation_id}",',
      '\tdurationTicks: ${2:20},',
      '\tloop: false,',
      '\ttracks: {},',
      '}'
    ]
  },
  faceAnimation: {
    label: 'EchoStory face animation root',
    body: [
      '{',
      '\tid: "${1:face_animation_id}",',
      '\tdurationTicks: ${2:20},',
      '\tloop: false,',
      '\ttracks: {},',
      '}'
    ]
  },
  cameraPath: {
    label: 'EchoStory camera path root',
    body: [
      '{',
      '\tid: "${1:camera_path_id}",',
      '\tdurationTicks: ${2:80},',
      '\tkeyframes: [',
      '\t\t{ tick: 0, x: ${3:0}, y: ${4:64}, z: ${5:0}, yaw: ${6:0}, pitch: ${7:0} },',
      '\t],',
      '}'
    ]
  },
  localization: {
    label: 'EchoStory localization root',
    body: [
      '{',
      '\tlocale: "${1:ru_ru}",',
      '\tentries: {',
      '\t\t"${2:key}": "${3:Text}",',
      '\t},',
      '}'
    ]
  }
};

function snippetForProperty(field) {
  return new vscode.SnippetString(PROPERTY_SNIPPETS[field] || `${field}: $0`);
}

function rootSnippetCompletions(documentKind) {
  const snippet = ROOT_SNIPPETS[documentKind] || ROOT_SNIPPETS.event;
  const item = new vscode.CompletionItem(snippet.label, vscode.CompletionItemKind.Snippet);
  item.detail = 'EchoStory root file template';
  item.insertText = new vscode.SnippetString(snippet.body.join('\n'));
  item.sortText = '000_root_template';
  return [item];
}

module.exports = {
  snippetForProperty,
  rootSnippetCompletions
};
