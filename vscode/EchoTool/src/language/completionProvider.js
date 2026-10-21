'use strict';

const vscode = require('vscode');
const {
  propertyBeforeCursor,
  isCompletingProperty,
  isCompletingStringValue,
  inferTypeContext,
  pathAtOffset,
  inferObjectContextFromPath,
  referenceKindForField
} = require('./languageContext');
const { referenceIds, resolveReference } = require('./referenceResolver');
const { snippetForProperty, rootSnippetCompletions } = require('./snippetFactory');
const { dialogueNodeIds, dialogueChoiceIds, speakerAliasIds } = require('./localStoryContext');
const { scalarValuesForField, isLocalizationKeyField } = require('./valueCompletionModel');
const { keyCompletionRole } = require('../shared/authoringKeyModel');

class EchoStoryCompletionProvider {
  constructor(catalog, index, parser) {
    this.catalog = catalog;
    this.index = index;
    this.parser = parser;
  }

  provideCompletionItems(document, position) {
    const property = propertyBeforeCursor(document, position);
    const offset = document.offsetAt(position);
    const pathParts = pathAtOffset(document.getText(), offset);
    const documentKind = this.index.documentKind(document.uri.fsPath);
    const objectContext = inferObjectContextFromPath(pathParts, documentKind);
    const completionContext = inferCompletionContext(document, position, pathParts, objectContext);
    const nearestType = nearestTypeInCurrentObject(document, position);

    if (isDocumentEmpty(document)) {
      return rootSnippetCompletions(documentKind);
    }

    if (property === 'type' && isCompletingStringValue(document, position)) {
      const typeContext = isCommandCompletionContext(completionContext)
        ? completionContext
        : inferTypeContext(document, position) || completionContext;
      return this.typeCompletions(typeContext);
    }

    if (isCompletingFreeStringItem(document, position)) {
      return this.shortcutCompletions(completionContext);
    }

    if (property && isCompletingStringValue(document, position)) {
      return this.valueCompletions(document, property, completionContext, documentKind, pathParts, nearestType);
    }

    if (property && isCompletingUnquotedValue(document, position)) {
      return this.unquotedValueCompletions(property);
    }

    if (isCompletingProperty(document, position) || !property) {
      return this.propertyCompletions(document, position, completionContext, documentKind, nearestType);
    }

    return [];
  }

  typeCompletions(context) {
    switch (context) {
      case 'trigger':
        return completionItemsFromDefinitionMap(this.catalog.triggerTypes, vscode.CompletionItemKind.EnumMember);
      case 'condition':
        return completionItemsFromDefinitionMap(this.catalog.conditionTypes, vscode.CompletionItemKind.EnumMember);
      case 'sceneStep':
        return completionItemsFromDefinitionMap(this.catalog.sceneStepTypes, vscode.CompletionItemKind.EnumMember);
      case 'npcCommand':
        return completionItemsFromDefinitionMap(this.catalog.npcCommandTypes, vscode.CompletionItemKind.EnumMember);
      case 'worldCommand':
        return completionItemsFromDefinitionMap(this.catalog.worldCommandTypes, vscode.CompletionItemKind.EnumMember);
      case 'command':
        return completionItemsFromDefinitionMap(this.catalog.commandTypes, vscode.CompletionItemKind.EnumMember);
      case 'effect':
      default:
        return completionItemsFromDefinitionMap(this.catalog.effectTypes, vscode.CompletionItemKind.EnumMember);
    }
  }

  valueCompletions(document, fieldName, objectContext, documentKind, pathParts, nearestType) {
    if (documentKind === 'dialogue' && isDialogueNodeReferenceField(fieldName)) {
      return localValueCompletions(dialogueNodeIds(this.parser, document), 'Dialogue node id');
    }

    if (documentKind === 'dialogue' && fieldName === 'defaultChoiceId') {
      const nodeId = currentDialogueNodeId(pathParts);
      return localValueCompletions(dialogueChoiceIds(this.parser, document, nodeId), 'Dialogue choice id');
    }

    if (isSpeakerField(fieldName)) {
      const aliases = localValueCompletions(speakerAliasIds(this.parser, document), 'Local speaker alias');
      return [...aliases, ...this.referenceCompletions(referenceKindForField(fieldName))];
    }

    const keyRole = keyCompletionRole({
      fieldName,
      objectContext,
      documentKind,
      pathParts,
      typeValue: nearestType
    });
    if (keyRole === 'signal') {
      return this.signalKeyCompletions();
    }
    if (keyRole === 'state') {
      return this.stateKeyCompletions();
    }
    if (keyRole === 'localization') {
      return this.localizationKeyCompletions();
    }

    if (isLocalizationKeyField(fieldName)) {
      return this.localizationKeyCompletions();
    }

    const referenceKind = referenceKindForField(fieldName);
    if (referenceKind) {
      return this.referenceCompletions(referenceKind);
    }

    if (isLikelyStateKeyField(fieldName)) {
      return this.stateKeyCompletions();
    }

    switch (fieldName) {
      case 'kind':
        return enumItems(this.catalog.enums.notifyKind);
      case 'status':
        return enumItems([...this.catalog.enums.questStatus, ...this.catalog.enums.puzzleStatus]);
      case 'scope':
        return enumItems(this.catalog.enums.timerScope);
      case 'operator':
        return enumItems(this.catalog.enums.conditionOperator);
      case 'target':
        return enumItems(objectContext === 'npcCommand'
          ? [...this.catalog.enums.npcPlayerTarget, ...this.catalog.enums.animationTarget]
          : this.catalog.enums.animationTarget);
      case 'layer':
        return enumItems([...this.catalog.enums.animationLayer, ...this.catalog.enums.faceAnimationLayer]);
      case 'hand':
      case 'slot':
        return enumItems([...this.catalog.enums.hand, ...this.catalog.enums.equipmentSlot]);
      case 'dimensionId':
      case 'dimension':
      case 'dim':
        return enumItems(this.catalog.enums.dimensions);
      case 'weatherType':
        return enumItems(this.catalog.enums.weatherType);
      case 'role':
      case 'as':
      case 'seatRole':
      case 'seatType':
      case 'vehicleRole':
        return enumItems(this.catalog.enums.vehicleRole);
      case 'action':
        return enumItems(this.catalog.enums.blockAction);
      case 'yawMode':
      case 'yaw_mode':
      case 'facing':
        return enumItems(this.catalog.enums.seatYawMode);
      default:
        return [];
    }
  }

  referenceCompletions(kind) {
    return referenceIds(this.index, kind).map(id => {
      const item = new vscode.CompletionItem(id, vscode.CompletionItemKind.Reference);
      item.detail = `EchoStory ${kind} id`;
      const resolved = resolveReference(this.index, kind, id);
      if (resolved) {
        item.documentation = new vscode.MarkdownString(`Resolved ${resolved.kind} reference. Use Go to Definition to open the source.`);
        item.detail = `EchoStory ${resolved.kind} id`;
      }
      item.insertText = id;
      return item;
    });
  }

  unquotedValueCompletions(fieldName) {
    return literalValueCompletions(scalarValuesForField(this.catalog, fieldName));
  }

  shortcutCompletions(objectContext) {
    if (!['effect', 'sceneStep', 'eventRoot', 'dialogueRoot', 'sceneRoot'].includes(objectContext)) {
      return [];
    }
    return [
      ...this.shortcutReferenceCompletions('dialogue', 'dialogue'),
      ...this.shortcutReferenceCompletions('scene', 'scene')
    ];
  }

  shortcutReferenceCompletions(kind, prefix) {
    return referenceIds(this.index, kind).map(id => {
      const label = `${prefix}:${id}`;
      const item = new vscode.CompletionItem(label, vscode.CompletionItemKind.Reference);
      item.detail = `EchoStory ${kind} string shortcut`;
      const resolved = resolveReference(this.index, kind, id);
      if (resolved) {
        item.documentation = new vscode.MarkdownString(`Resolved ${resolved.kind} shortcut. Use Go to Definition to open the source.`);
      }
      item.insertText = label;
      return item;
    });
  }

  localizationKeyCompletions() {
    if (!this.index.localizationKeys) {
      return [];
    }
    return this.index.localizationKeys().map(key => {
      const item = new vscode.CompletionItem(key, vscode.CompletionItemKind.Value);
      const location = this.index.localizationKeyLocation(key);
      item.detail = 'EchoStory localization key';
      item.insertText = key;
      if (location) {
        item.documentation = new vscode.MarkdownString([
          location.preview ? location.preview.slice(0, 200) : '',
          'Resolved localization key. Use Go to Definition to open the source.'
        ].filter(Boolean).join('\n\n'));
      }
      return item;
    });
  }

  stateKeyCompletions() {
    const knownKeys = this.index.stateKeys ? this.index.stateKeys() : [];
    return [
      ...knownKeys.map(key => {
        const item = new vscode.CompletionItem(key, vscode.CompletionItemKind.Value);
        const location = this.index.stateKeyLocation(key);
        item.detail = 'EchoStory state key';
        item.insertText = key;
        item.sortText = `0_${key}`;
        if (location) {
          item.documentation = new vscode.MarkdownString('Resolved story state key. Use Go to Definition to open the source.');
        }
        return item;
      }),
      ...stateKeyNamespaceCompletions()
    ];
  }

  signalKeyCompletions() {
    if (!this.index.signalKeys) {
      return [];
    }
    return this.index.signalKeys().map(key => {
      const item = new vscode.CompletionItem(key, vscode.CompletionItemKind.Event);
      const location = this.index.signalKeyLocation(key);
      item.detail = 'EchoStory signal key';
      item.insertText = key;
      if (location) {
        item.documentation = new vscode.MarkdownString('Resolved story signal key. Use Go to Definition to open the source.');
      }
      return item;
    });
  }

  propertyCompletions(document, position, objectContext, documentKind, nearestType) {
    const fields = fieldsForContext(this.catalog, objectContext, documentKind, nearestType);
    const existing = existingPropertiesInCurrentObject(document, position);
    return fields.filter(field => !existing.has(field)).map(field => {
      const item = new vscode.CompletionItem(field, vscode.CompletionItemKind.Property);
      item.detail = this.catalog.fieldDocs[field] || 'EchoStory JSON5 field';
      item.documentation = new vscode.MarkdownString(this.catalog.fieldDocs[field] || '');
      item.insertText = snippetForProperty(field);
      return item;
    });
  }
}

function localValueCompletions(values, detail) {
  return values.map(value => {
    const item = new vscode.CompletionItem(value, vscode.CompletionItemKind.Value);
    item.detail = detail;
    item.insertText = value;
    item.sortText = `0_${value}`;
    return item;
  });
}

function isDialogueNodeReferenceField(fieldName) {
  return ['start', 'startNodeId', 'next', 'goto', 'to'].includes(fieldName);
}

function isSpeakerField(fieldName) {
  return ['speaker', 'speakerId'].includes(fieldName);
}

function currentDialogueNodeId(pathParts) {
  const parts = pathParts.map(String);
  const nodesIndex = parts.lastIndexOf('nodes');
  if (nodesIndex >= 0 && parts[nodesIndex + 1]) {
    return parts[nodesIndex + 1];
  }
  return undefined;
}

function completionItemsFromDefinitionMap(map, kind) {
  const result = [];
  const seen = new Set();
  for (const [id, definition] of map.entries()) {
    if (seen.has(id)) {
      continue;
    }
    seen.add(id);
    const item = new vscode.CompletionItem(id, kind);
    item.detail = definition.alias ? `Alias of ${definition.canonical}` : 'EchoStory type';
    item.documentation = new vscode.MarkdownString([
      definition.description || '',
      definition.required && definition.required.length > 0 ? `Required: \`${definition.required.join('`, `')}\`` : ''
    ].filter(Boolean).join('\n\n'));
    item.insertText = id;
    result.push(item);
  }
  return result;
}

function enumItems(values) {
  return Array.from(new Set(values)).map(value => {
    const item = new vscode.CompletionItem(value, vscode.CompletionItemKind.Value);
    item.insertText = value;
    return item;
  });
}

function literalValueCompletions(values) {
  return Array.from(new Set(values)).map(value => {
    const item = new vscode.CompletionItem(value, vscode.CompletionItemKind.Value);
    item.detail = 'EchoStory JSON5 literal';
    item.insertText = value;
    return item;
  });
}

function fieldsForContext(catalog, objectContext, documentKind, nearestType) {
  const typeDefinition = definitionForNearestType(catalog, objectContext, nearestType);
  if (typeDefinition) {
    return typeDefinition.fields || [];
  }

  if (objectContext === 'npcCommand') {
    return ['type', 'npcId', 'npc', 'dimensionId', 'x', 'y', 'z', 'at', 'route', 'animationId', 'itemId', 'enabled'];
  }
  if (objectContext === 'worldCommand') {
    return ['type', 'dimensionId', 'x', 'y', 'z', 'blockId', 'itemId', 'count', 'weatherType', 'durationTicks'];
  }
  if (objectContext === 'command') {
    return ['type', 'npcId', 'npc', 'dimensionId', 'x', 'y', 'z', 'itemId', 'blockId'];
  }
  if (objectContext === 'trigger') {
    return ['type', 'key', 'dimensionId', 'min', 'max', 'delayTicks', 'itemId', 'entityId', 'dayTime', 'scope'];
  }
  if (objectContext === 'condition') {
    return ['type', 'key', 'value', 'field', 'conditions', 'condition', 'amount', 'questId', 'puzzleId', 'status'];
  }
  if (objectContext === 'effect') {
    return ['type', 'key', 'value', 'dialogueId', 'sceneId', 'npcId', 'animationId', 'effects', 'condition', 'then', 'else'];
  }
  if (objectContext === 'sceneStep') {
    return ['type', 'ticks', 'speaker', 'text', 'say', 'effects', 'do'];
  }

  const root = catalog.rootKinds[documentKind] || catalog.rootKinds.generic;
  return root.fields;
}

function definitionForNearestType(catalog, objectContext, nearestType) {
  if (!nearestType) {
    return undefined;
  }
  if (objectContext === 'trigger') {
    return catalog.triggerTypes.get(nearestType);
  }
  if (objectContext === 'condition') {
    return catalog.conditionTypes.get(nearestType);
  }
  if (objectContext === 'sceneStep') {
    return catalog.sceneStepTypes.get(nearestType) || catalog.effectTypes.get(nearestType);
  }
  if (objectContext === 'npcCommand') {
    return catalog.npcCommandTypes.get(nearestType);
  }
  if (objectContext === 'worldCommand') {
    return catalog.worldCommandTypes.get(nearestType);
  }
  if (objectContext === 'command') {
    return catalog.commandTypes.get(nearestType);
  }
  return catalog.effectTypes.get(nearestType);
}

function nearestTypeInCurrentObject(document, position) {
  const text = document.getText();
  const offset = document.offsetAt(position);
  const prefix = text.slice(Math.max(0, offset - 2500), offset);
  const lastOpen = Math.max(prefix.lastIndexOf('{'), prefix.lastIndexOf('['));
  const scope = lastOpen >= 0 ? prefix.slice(lastOpen) : prefix;
  const matches = Array.from(scope.matchAll(/\btype\s*:\s*(['"])(.*?)\1/g));
  if (matches.length === 0) {
    return undefined;
  }
  return matches[matches.length - 1][2];
}

function inferCompletionContext(document, position, pathParts, objectContext) {
  const commandContext = inferCommandContext(document, position, pathParts);
  return commandContext || objectContext;
}

function inferCommandContext(document, position, pathParts) {
  const parts = pathParts.map(String);
  if (parts.some(part => ['npcCommand', 'npc_command'].includes(part))) {
    return 'npcCommand';
  }
  if (parts.some(part => ['worldCommand', 'world_command'].includes(part))) {
    return 'worldCommand';
  }
  if (!parts.includes('command')) {
    return undefined;
  }

  const prefix = textBeforePosition(document, position, 90);
  if (/\b(npcCommand|npc_command)\s*:/.test(prefix)) {
    return 'npcCommand';
  }
  if (/\b(worldCommand|world_command)\s*:/.test(prefix)) {
    return 'worldCommand';
  }
  const typeMatches = Array.from(prefix.matchAll(/\btype\s*:\s*(['"])(npc_command|npccommand|npc|world_command|worldcommand|world)\1/g));
  const lastType = typeMatches.length ? typeMatches[typeMatches.length - 1][2] : '';
  if (lastType.startsWith('npc')) {
    return 'npcCommand';
  }
  if (lastType.startsWith('world')) {
    return 'worldCommand';
  }
  return 'command';
}

function textBeforePosition(document, position, maxLines) {
  const from = Math.max(0, position.line - maxLines);
  const lines = [];
  for (let line = from; line <= position.line; line += 1) {
    const text = document.lineAt(line).text;
    lines.push(line === position.line ? text.slice(0, position.character) : text);
  }
  return lines.join('\n');
}

function existingPropertiesInCurrentObject(document, position) {
  const text = document.getText();
  const offset = document.offsetAt(position);
  const prefix = text.slice(Math.max(0, offset - 3000), offset);
  const lastOpen = prefix.lastIndexOf('{');
  const scope = lastOpen >= 0 ? prefix.slice(lastOpen) : prefix;
  const result = new Set();
  for (const match of scope.matchAll(/\b([A-Za-z_$][\w$-]*)\s*:/g)) {
    result.add(match[1]);
  }
  return result;
}

function isCompletingFreeStringItem(document, position) {
  const text = document.lineAt(position.line).text.slice(0, position.character);
  return /(?:^|[\[,]\s*)(["'])[^"']*$/.test(text) && !/:\s*(["'])[^"']*$/.test(text);
}

function isCompletingUnquotedValue(document, position) {
  const text = document.lineAt(position.line).text.slice(0, position.character);
  return /:\s*[A-Za-z0-9_.:-]*$/.test(text) && !/:\s*(["'])[^"']*$/.test(text);
}

function isDocumentEmpty(document) {
  return document.getText().trim().length === 0;
}

function isLikelyStateKeyField(fieldName) {
  return ['key', 'stateKey', 'sourceKey', 'targetKey', 'answerKey', 'fromKey', 'toKey'].includes(fieldName);
}

function isCommandCompletionContext(context) {
  return ['command', 'npcCommand', 'worldCommand'].includes(context);
}

function stateKeyNamespaceCompletions() {
  return ['memory.', 'plot.', 'world.', 'scene.', 'debug.'].map(prefix => {
    const item = new vscode.CompletionItem(prefix, vscode.CompletionItemKind.Value);
    item.detail = 'EchoStory state key namespace';
    item.insertText = prefix;
    return item;
  });
}

module.exports = {
  EchoStoryCompletionProvider
};
