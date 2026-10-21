'use strict';

const vscode = require('vscode');
const { referenceKindForField } = require('./languageContext');
const { shortcutReference } = require('./referenceResolver');

const TOKEN_TYPES = [
  'property',
  'enumMember',
  'variable',
  'namespace',
  'keyword',
  'string',
  'number'
];

const TOKEN_MODIFIERS = [
  'declaration',
  'readonly',
  'deprecated'
];

const TOKEN_INDEX = new Map(TOKEN_TYPES.map((type, index) => [type, index]));
const MODIFIER_INDEX = new Map(TOKEN_MODIFIERS.map((modifier, index) => [modifier, index]));

class EchoStorySemanticTokensProvider {
  constructor(catalog) {
    this.catalog = catalog;
  }

  provideDocumentSemanticTokens(document) {
    const builder = new vscode.SemanticTokensBuilder(EchoStorySemanticTokensProvider.legend);
    for (const token of collectSemanticTokens(document.getText(), this.catalog)) {
      builder.push(token.line, token.start, token.length, tokenType(token.type), tokenModifiers(token.modifiers));
    }
    return builder.build();
  }
}

EchoStorySemanticTokensProvider.legend = new vscode.SemanticTokensLegend(TOKEN_TYPES, TOKEN_MODIFIERS);

function collectSemanticTokens(text, catalog) {
  const tokens = [];
  const lines = String(text || '').split(/\r?\n/);
  for (let lineNumber = 0; lineNumber < lines.length; lineNumber += 1) {
    const line = lines[lineNumber];
    const lineTokens = [
      ...propertyTokens(line, lineNumber),
      ...typedValueTokens(line, lineNumber, catalog),
      ...referenceValueTokens(line, lineNumber),
      ...shortcutTokens(line, lineNumber),
      ...stateKeyTokens(line, lineNumber),
      ...echoScriptReferenceTokens(line, lineNumber)
    ];
    tokens.push(...dedupeAndSort(lineTokens));
  }
  return tokens;
}

function propertyTokens(line, lineNumber) {
  return Array.from(line.matchAll(/\b([A-Za-z_$][\w$-]*)\s*:/g)).map(match => ({
    line: lineNumber,
    start: match.index,
    length: match[1].length,
    type: 'property',
    modifiers: []
  }));
}

function typedValueTokens(line, lineNumber, catalog) {
  const tokens = [];
  for (const match of line.matchAll(/\btype\s*:\s*(['"])([A-Za-z_][\w-]*)\1/g)) {
    const start = match.index + match[0].indexOf(match[2]);
    const known = isKnownType(catalog, match[2]);
    tokens.push({
      line: lineNumber,
      start,
      length: match[2].length,
      type: 'enumMember',
      modifiers: known ? ['readonly'] : ['deprecated']
    });
  }
  return tokens;
}

function referenceValueTokens(line, lineNumber) {
  const tokens = [];
  for (const match of line.matchAll(/\b([A-Za-z_$][\w$-]*)\s*:\s*(['"])(.*?)\2/g)) {
    const field = match[1];
    if (field === 'type' || !referenceKindForField(field)) {
      continue;
    }
    const value = match[3];
    if (!value) {
      continue;
    }
    const start = match.index + match[0].indexOf(value);
    tokens.push({
      line: lineNumber,
      start,
      length: value.length,
      type: 'variable',
      modifiers: ['readonly']
    });
  }
  return tokens;
}

function shortcutTokens(line, lineNumber) {
  const tokens = [];
  for (const match of line.matchAll(/(['"])([A-Za-z_][\w-]*):([^'"]+)\1/g)) {
    const shortcut = shortcutReference(`${match[2]}:${match[3]}`);
    if (!shortcut) {
      continue;
    }
    const prefixStart = match.index + 1;
    tokens.push({
      line: lineNumber,
      start: prefixStart,
      length: match[2].length,
      type: 'keyword',
      modifiers: ['readonly']
    });
    tokens.push({
      line: lineNumber,
      start: prefixStart + match[2].length + 1,
      length: match[3].length,
      type: 'variable',
      modifiers: ['readonly']
    });
  }
  return tokens;
}

function stateKeyTokens(line, lineNumber) {
  const tokens = [];
  for (const match of line.matchAll(/(['"])((?:memory|plot|world|scene|debug)\.)([^'"]*)\1/g)) {
    const namespaceStart = match.index + 1;
    tokens.push({
      line: lineNumber,
      start: namespaceStart,
      length: match[2].length,
      type: 'namespace',
      modifiers: ['readonly']
    });
    if (match[3]) {
      tokens.push({
        line: lineNumber,
        start: namespaceStart + match[2].length,
        length: match[3].length,
        type: 'variable',
        modifiers: []
      });
    }
  }
  return tokens;
}

function echoScriptReferenceTokens(line, lineNumber) {
  const tokens = [];
  const source = String(line || '');
  addCallReferenceTokens(tokens, source, lineNumber, [
    'Scene\\.Start',
    'Dialogue\\.Start',
    'Set',
    'State\\.(?:Set|Remove|Clear|Add|Adjust|Increment|Decrement|Memory|Ref)',
    'exists',
    'Manual',
    'ActionPerformed',
    'AreaEnter',
    'AreaEnterDelayed',
    'AreaExit',
    'InventoryHasItem',
    'InventoryMissing',
    'MobKilled',
    'Timer',
    'TimeReached',
    'PlayerJoin',
    'PlayerRespawn',
    'DimensionEnter',
    'NpcPath',
    'QuestMarker',
    'Monologue\\.Show',
    'Navigation\\.(?:SetTarget|ClearTarget)',
    'Vehicle\\.(?:EnsureSpawn|Trip|Sequence)',
    'World\\.(?:SetNavigationTarget|ClearNavigationTarget)',
    'Player\\.Pose',
    'Npc\\.(?:Spawn|Despawn|SpawnNearPlayer|Stay|Path|Move|MoveTo|WalkTo|ApproachPlayer|MoveToPlayer|WalkToPlayer|Rotate|Turn|UseBlock|InteractBlock|OpenDoor|CloseDoor|ToggleDoor|UseDoor|LookAtPlayers|ClearLookAtPlayers|HoldItem|ClearHeldItem|PlayAnimation|StopAnimation|PlayFaceAnimation|StopFaceAnimation|EnterVehicle|EnterNearestVehicle|EnterVehicleAt|LeaveVehicle)'
  ]);
  for (const match of source.matchAll(/\b(Overworld|Nether|End|Items|Speakers|Dialogues|Animations|FaceAnimations|Scenes|CameraPaths|States|OldFriend|QuestStatus|Layer|FaceLayer|Hand|Portraits|CommonStates|Vehicle|Navigation)\b/g)) {
    tokens.push({
      line: lineNumber,
      start: match.index,
      length: match[1].length,
      type: 'namespace',
      modifiers: ['readonly']
    });
  }
  return tokens;
}

function addCallReferenceTokens(tokens, source, lineNumber, callNames) {
  const pattern = new RegExp(`\\b(?:${callNames.join('|')})\\s*\\(\\s*([A-Za-z_][A-Za-z0-9_]*)`, 'g');
  for (const match of source.matchAll(pattern)) {
    const value = match[1];
    if (RESERVED_REFERENCE_WORDS.has(value)) {
      continue;
    }
    const start = match.index + match[0].lastIndexOf(value);
    tokens.push({
      line: lineNumber,
      start,
      length: value.length,
      type: 'variable',
      modifiers: ['readonly']
    });
  }
}

function isKnownType(catalog, value) {
  return Boolean(catalog.effectTypes.has(value)
    || catalog.conditionTypes.has(value)
    || catalog.triggerTypes.has(value)
    || catalog.sceneStepTypes.has(value)
    || catalog.commandTypes.has(value));
}

const RESERVED_REFERENCE_WORDS = new Set([
  'true',
  'false',
  'null',
  'new'
]);

function dedupeAndSort(tokens) {
  const result = [];
  const seen = new Set();
  for (const token of tokens.sort((left, right) => left.start - right.start || right.length - left.length)) {
    const key = `${token.start}:${token.length}:${token.type}`;
    if (seen.has(key) || overlapsAny(token, result)) {
      continue;
    }
    seen.add(key);
    result.push(token);
  }
  return result;
}

function overlapsAny(token, accepted) {
  return accepted.some(existing => token.start < existing.start + existing.length && existing.start < token.start + token.length);
}

function tokenType(type) {
  return TOKEN_INDEX.get(type) || 0;
}

function tokenModifiers(modifiers) {
  let result = 0;
  for (const modifier of modifiers || []) {
    const index = MODIFIER_INDEX.get(modifier);
    if (index !== undefined) {
      result |= 1 << index;
    }
  }
  return result;
}

module.exports = {
  EchoStorySemanticTokensProvider,
  collectSemanticTokens
};
