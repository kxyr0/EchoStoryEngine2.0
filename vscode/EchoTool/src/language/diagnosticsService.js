'use strict';

const vscode = require('vscode');
const {
  inferObjectContextFromPath,
  referenceKindForField,
  quotedValueRange,
  typeValueOnLine
} = require('./languageContext');
const { hasReference, shortcutReference } = require('./referenceResolver');
const { isLocalizationKeyField } = require('./valueCompletionModel');
const {
  dialogueNodeIdsFromValue,
  dialogueNodeMapFromValue,
  speakerAliasIdsFromValue,
  speakerAliasTargetsFromValue
} = require('./localStoryContext');
const { analyzeEchoScript } = require('./echoscriptSupport');
const { eventDiagnostics } = require('./eventDiagnostics');

const STATE_KEY_PREFIXES = ['memory.', 'plot.', 'world.', 'scene.', 'debug.'];
const TICK_FIELD_PATTERN = /(?:ticks|delay|duration|fade|timeout|repeat|attempts|count|amount)$/i;

class DiagnosticsService {
  constructor(parser, catalog, index, configuration) {
    this.parser = parser;
    this.catalog = catalog;
    this.index = index;
    this.configuration = configuration;
    this.collection = vscode.languages.createDiagnosticCollection('echostoryEngine');
  }

  dispose() {
    this.collection.dispose();
  }

  refresh(document) {
    if (!this.configuration.diagnosticsEnabled()) {
      this.collection.delete(document.uri);
      return;
    }
    if (document.languageId === 'echoscript') {
      const diagnostics = [...analyzeEchoScript(document.getText()),
        ...eventDiagnostics(document.getText(), this.index)].map(item => {
        const diagnostic = new vscode.Diagnostic(
          rangeFromLineColumns(document, item.line, item.column, item.column + item.length),
          item.message,
          echoScriptSeverity(item.severity)
        );
        diagnostic.source = 'EchoScript linter';
        return diagnostic;
      });
      this.collection.set(document.uri, dedupeDiagnostics(diagnostics));
      return;
    }
    if (document.languageId !== 'echostory-json5') {
      this.collection.delete(document.uri);
      return;
    }

    const diagnostics = [];
    const parsed = this.parser.parse(document.getText(), document.uri.fsPath);
    if (!parsed.ok) {
      diagnostics.push(parseDiagnostic(document, parsed.error));
      this.collection.set(document.uri, diagnostics);
      return;
    }

    const documentKind = this.index.documentKind(document.uri.fsPath);
    validateRoot(parsed.value, documentKind, this.catalog, diagnostics, document);
    validateValue(parsed.value, [], documentKind, this.catalog, this.index, this.configuration, diagnostics, document, parsed.value);
    validateInternalReferences(parsed.value, documentKind, this.index, this.configuration, diagnostics, document);
    validateTypeLines(document, this.catalog, diagnostics);
    this.collection.set(document.uri, dedupeDiagnostics(diagnostics));
  }

  refreshOpenDocuments() {
    for (const document of vscode.workspace.textDocuments) {
      if (document.languageId === 'echostory-json5' || document.languageId === 'echoscript') {
        this.refresh(document);
      }
    }
  }
}

function echoScriptSeverity(severity) {
  if (severity === 'error') {
    return vscode.DiagnosticSeverity.Error;
  }
  if (severity === 'information') {
    return vscode.DiagnosticSeverity.Information;
  }
  return vscode.DiagnosticSeverity.Warning;
}

function validateRoot(value, documentKind, catalog, diagnostics, document) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    diagnostics.push(diagnostic(document, 0, 0, 'EchoStory story files must be JSON5 objects.', vscode.DiagnosticSeverity.Error));
    return;
  }

  const root = catalog.rootKinds[documentKind] || catalog.rootKinds.generic;
  for (const field of root.required || []) {
    if (!hasOwn(value, field)) {
      diagnostics.push(diagnostic(document, 0, 0, `Missing required ${documentKind} field "${field}".`, vscode.DiagnosticSeverity.Error));
    }
  }
}

function validateValue(value, path, documentKind, catalog, index, configuration, diagnostics, document, rootValue) {
  if (Array.isArray(value)) {
    for (let arrayIndex = 0; arrayIndex < value.length; arrayIndex += 1) {
      validateValue(value[arrayIndex], path.concat(arrayIndex), documentKind, catalog, index, configuration, diagnostics, document, rootValue);
    }
    return;
  }
  if (!value || typeof value !== 'object') {
    validatePrimitive(value, path, documentKind, index, configuration, diagnostics, document);
    return;
  }

  validateObject(value, path, documentKind, catalog, index, configuration, diagnostics, document);
  for (const [key, child] of Object.entries(value)) {
    validateFieldValue(key, child, path, documentKind, index, configuration, diagnostics, document, rootValue);
    validateValue(child, path.concat(key), documentKind, catalog, index, configuration, diagnostics, document, rootValue);
  }
}

function validateObject(value, path, documentKind, catalog, index, configuration, diagnostics, document) {
  if (path.length === 0 && typeof value.id === 'string') {
    const duplicates = index.duplicateIds(documentKind, value.id);
    if (duplicates.length > 1) {
      diagnostics.push(fieldDiagnostic(document, 'id', value.id, `Duplicate ${documentKind} id "${value.id}" found in ${duplicates.length} files.`, vscode.DiagnosticSeverity.Warning));
    }
  }

  if (!hasOwn(value, 'type') || typeof value.type !== 'string') {
    return;
  }

  const context = inferObjectContextFromPath(path.map(String), documentKind);
  if (shouldSkipTypeValidation(path, context)) {
    return;
  }

  const definitions = definitionsForContext(catalog, context);
  const definition = definitions.get(value.type);
  if (!definition) {
    diagnostics.push(fieldDiagnostic(document, 'type', value.type, `Unsupported ${contextLabel(context)} type "${value.type}".`, vscode.DiagnosticSeverity.Error));
    return;
  }

  validateRequiredFields(value, definition, document, diagnostics);
  if (configuration.strictUnknownFields()) {
    validateUnknownFields(value, definition, document, diagnostics);
  }
}

function validateRequiredFields(value, definition, document, diagnostics) {
  for (const field of definition.required || []) {
    if (!hasOwn(value, field)) {
      diagnostics.push(fieldDiagnostic(document, 'type', value.type, `Type "${value.type}" requires field "${field}".`, vscode.DiagnosticSeverity.Error));
    }
  }
  for (const group of definition.anyRequired || []) {
    if (!group.some(field => hasOwn(value, field))) {
      diagnostics.push(fieldDiagnostic(document, 'type', value.type, `Type "${value.type}" requires one of: ${group.join(', ')}.`, vscode.DiagnosticSeverity.Error));
    }
  }
}

function validateUnknownFields(value, definition, document, diagnostics) {
  const allowed = new Set(definition.fields || []);
  for (const field of Object.keys(value)) {
    if (!allowed.has(field)) {
      diagnostics.push(fieldDiagnostic(document, field, undefined, `Field "${field}" is not listed for type "${value.type}".`, vscode.DiagnosticSeverity.Information));
    }
  }
}

function validateFieldValue(field, value, path, documentKind, index, configuration, diagnostics, document, rootValue) {
  if (typeof value === 'number' && TICK_FIELD_PATTERN.test(field) && value < 0) {
    diagnostics.push(fieldDiagnostic(document, field, undefined, `Field "${field}" should not be negative.`, vscode.DiagnosticSeverity.Warning));
  }

  if (typeof value !== 'string') {
    return;
  }

  if (isStateKeyField(field, path)) {
    validateStateKey(field, value, diagnostics, document);
  }

  if (isLocalizationKeyField(field) && configuration.validateReferences()) {
    validateLocalizationKey(field, value, index, diagnostics, document);
  }

  const referenceKind = referenceKindForField(field);
  if (referenceKind && configuration.validateReferences()) {
    if (isLocalSpeakerAliasReference(field, value, rootValue)) {
      return;
    }
    validateReference(field, value, referenceKind, index, diagnostics, document);
  }
}

function validatePrimitive(value, path, documentKind, index, configuration, diagnostics, document) {
  if (typeof value !== 'string' || !configuration.validateReferences()) {
    return;
  }
  const shortcut = shortcutReference(value);
  if (!shortcut) {
    return;
  }
  validateReference(shortcut.kind, shortcut.id, shortcut.kind, index, diagnostics, document);
}

function validateStateKey(field, value, diagnostics, document) {
  if (!STATE_KEY_PREFIXES.some(prefix => value.startsWith(prefix))) {
    diagnostics.push(fieldDiagnostic(
      document,
      field,
      value,
      `State key "${value}" should start with ${STATE_KEY_PREFIXES.map(prefix => `"${prefix}"`).join(', ')}.`,
      vscode.DiagnosticSeverity.Warning
    ));
  }
}

function validateReference(field, value, referenceKind, index, diagnostics, document) {
  if (!value || value.includes('${')) {
    return;
  }

  if (hasReference(index, referenceKind, value)) {
    return;
  }

  diagnostics.push(fieldDiagnostic(
    document,
    field,
    value,
    `Unknown ${referenceKind} reference "${value}". Refresh index if the file was just created.`,
    vscode.DiagnosticSeverity.Warning
  ));
}

function validateLocalizationKey(field, value, index, diagnostics, document) {
  if (!value || value.includes('${') || !index.hasLocalizationKey || index.hasLocalizationKey(value)) {
    return;
  }

  diagnostics.push(fieldDiagnostic(
    document,
    field,
    value,
    `Unknown localization key "${value}". Add it under story/localization entries or refresh the index if it was just created.`,
    vscode.DiagnosticSeverity.Warning
  ));
}

function validateInternalReferences(root, documentKind, index, configuration, diagnostics, document) {
  if (!configuration.validateReferences() || !root || typeof root !== 'object') {
    return;
  }
  validateSpeakerAliases(root, index, diagnostics, document);
  if (documentKind === 'dialogue') {
    validateDialogueNodes(root, diagnostics, document);
  }
}

function validateSpeakerAliases(root, index, diagnostics, document) {
  for (const { alias, target } of speakerAliasTargetsFromValue(root)) {
    if (!target.includes('${') && !hasReference(index, 'speaker', target)) {
      diagnostics.push(fieldDiagnostic(
        document,
        alias,
        target,
        `Speaker alias "${alias}" points to unknown speaker/NPC "${target}".`,
        vscode.DiagnosticSeverity.Warning
      ));
    }
  }
}

function validateDialogueNodes(root, diagnostics, document) {
  const nodeIds = new Set(dialogueNodeIdsFromValue(root));
  const nodes = dialogueNodeMapFromValue(root);
  if (nodeIds.size === 0) {
    return;
  }

  const startNodeId = typeof root.startNodeId === 'string'
    ? root.startNodeId
    : typeof root.start === 'string'
    ? root.start
    : '';
  if (startNodeId && !nodeIds.has(startNodeId)) {
    diagnostics.push(fieldDiagnostic(
      document,
      hasOwn(root, 'startNodeId') ? 'startNodeId' : 'start',
      startNodeId,
      `Dialogue start node "${startNodeId}" does not exist.`,
      vscode.DiagnosticSeverity.Error
    ));
  }

  const edges = new Map();
  for (const [nodeId, node] of nodes.entries()) {
    const references = dialogueNodeReferences(node);
    edges.set(nodeId, references.map(reference => reference.value).filter(value => nodeIds.has(value)));
    for (const reference of references) {
      if (!nodeIds.has(reference.value)) {
        diagnostics.push(fieldDiagnostic(
          document,
          reference.field,
          reference.value,
          `Dialogue node "${nodeId}" points to missing node "${reference.value}".`,
          vscode.DiagnosticSeverity.Error
        ));
      }
    }
    validateTimerDefaultChoice(nodeId, node, diagnostics, document);
  }

  if (startNodeId && nodeIds.has(startNodeId)) {
    validateReachability(startNodeId, nodes, edges, diagnostics, document);
  }
}

function dialogueNodeReferences(node) {
  if (!node || typeof node !== 'object') {
    return [];
  }
  const references = [];
  const nodeNext = firstString(node, 'next', 'goto', 'to');
  if (nodeNext) {
    references.push({ field: nodeNext.field, value: nodeNext.value });
  }
  const choices = firstPresent(node, 'choices', 'choose');
  for (const choice of normalizeChoices(choices)) {
    const choiceNext = firstString(choice, 'next', 'goto', 'to');
    if (choiceNext) {
      references.push({ field: choiceNext.field, value: choiceNext.value });
    }
  }
  return references;
}

function validateTimerDefaultChoice(nodeId, node, diagnostics, document) {
  if (!node || typeof node !== 'object' || !node.timer || typeof node.timer !== 'object') {
    return;
  }
  const defaultChoiceId = typeof node.timer.defaultChoiceId === 'string' ? node.timer.defaultChoiceId : '';
  if (!defaultChoiceId) {
    return;
  }
  const choiceIds = new Set(normalizeChoices(firstPresent(node, 'choices', 'choose'))
    .map(choice => typeof choice.id === 'string' ? choice.id : '')
    .filter(Boolean));
  if (!choiceIds.has(defaultChoiceId)) {
    diagnostics.push(fieldDiagnostic(
      document,
      'defaultChoiceId',
      defaultChoiceId,
      `Dialogue node "${nodeId}" timer references missing choice "${defaultChoiceId}".`,
      vscode.DiagnosticSeverity.Error
    ));
  }
}

function validateReachability(startNodeId, nodes, edges, diagnostics, document) {
  const visited = new Set();
  const queue = [startNodeId];
  while (queue.length > 0) {
    const current = queue.shift();
    if (visited.has(current) || !nodes.has(current)) {
      continue;
    }
    visited.add(current);
    for (const next of edges.get(current) || []) {
      queue.push(next);
    }
  }
  for (const nodeId of nodes.keys()) {
    if (!visited.has(nodeId)) {
      diagnostics.push(new vscode.Diagnostic(
        findKeyRange(document, nodeId),
        `Dialogue node "${nodeId}" is unreachable from start node "${startNodeId}".`,
        vscode.DiagnosticSeverity.Warning
      ));
    }
  }
}

function validateTypeLines(document, catalog, diagnostics) {
  for (let lineNumber = 0; lineNumber < document.lineCount; lineNumber += 1) {
    const text = document.lineAt(lineNumber).text;
    const value = typeValueOnLine(text);
    if (!value) {
      continue;
    }
    if (catalog.effectTypes.has(value)
      || catalog.conditionTypes.has(value)
      || catalog.triggerTypes.has(value)
      || catalog.sceneStepTypes.has(value)
      || catalog.commandTypes.has(value)) {
      continue;
    }
    if (/\bcommand\s*:/.test(document.getText(new vscode.Range(Math.max(0, lineNumber - 3), 0, lineNumber, text.length)))) {
      continue;
    }
    const range = quotedValueRange(document, lineNumber, 'type');
    diagnostics.push(new vscode.Diagnostic(
      rangeFromLineColumns(document, lineNumber, range ? range.start : 0, range ? range.end : text.length),
      `Unknown EchoStory type "${value}".`,
      vscode.DiagnosticSeverity.Error
    ));
  }
}

function definitionsForContext(catalog, context) {
  if (context === 'trigger') {
    return catalog.triggerTypes;
  }
  if (context === 'condition') {
    return catalog.conditionTypes;
  }
  if (context === 'sceneStep') {
    return catalog.sceneStepTypes;
  }
  if (context === 'command') {
    return catalog.commandTypes;
  }
  return catalog.effectTypes;
}

function shouldSkipTypeValidation(path, context) {
  const parts = path.map(String);
  if (context === 'command') {
    return false;
  }
  return parts.includes('command')
    || parts.includes('directive')
    || parts.includes('cipher')
    || parts.includes('marker')
    || parts.includes('spawn')
    || parts.includes('npcBehavior')
    || parts.includes('behavior');
}

function isStateKeyField(field, path) {
  if (!['key', 'stateKey', 'sourceKey', 'targetKey', 'answerKey', 'fromKey', 'toKey'].includes(field)) {
    return false;
  }
  const parts = path.map(String);
  return parts.includes('condition')
    || parts.includes('effects')
    || parts.includes('do')
    || parts.includes('then')
    || parts.includes('else')
    || parts.includes('startEffects')
    || parts.includes('completeEffects')
    || parts.includes('abortEffects');
}

function parseDiagnostic(document, error) {
  const line = clamp(error.line || 0, 0, Math.max(0, document.lineCount - 1));
  const column = clamp(error.column || 0, 0, document.lineAt(line).text.length);
  return new vscode.Diagnostic(
    rangeFromLineColumns(document, line, column, Math.min(column + 1, document.lineAt(line).text.length + 1)),
    `JSON5 parse error: ${error.message}`,
    vscode.DiagnosticSeverity.Error
  );
}

function fieldDiagnostic(document, field, value, message, severity) {
  const range = value !== undefined
    ? findFieldValueRange(document, field, value)
    : findFieldNameRange(document, field);
  return new vscode.Diagnostic(range, message, severity);
}

function diagnostic(document, line, column, message, severity) {
  return new vscode.Diagnostic(
    rangeFromLineColumns(document, line, column, column + 1),
    message,
    severity
  );
}

function findFieldValueRange(document, field, value) {
  for (let lineNumber = 0; lineNumber < document.lineCount; lineNumber += 1) {
    const range = quotedValueRange(document, lineNumber, field);
    if (range && range.value === value) {
      return rangeFromLineColumns(document, lineNumber, range.start, range.end);
    }
  }
  return findFieldNameRange(document, field);
}

function findFieldNameRange(document, field) {
  const pattern = new RegExp(`\\b${escapeRegExp(field)}\\b`);
  for (let lineNumber = 0; lineNumber < document.lineCount; lineNumber += 1) {
    const text = document.lineAt(lineNumber).text;
    const match = pattern.exec(text);
    if (match) {
      return rangeFromLineColumns(document, lineNumber, match.index, match.index + field.length);
    }
  }
  return rangeFromLineColumns(document, 0, 0, 1);
}

function rangeFromLineColumns(document, line, startColumn, endColumn) {
  const max = document.lineAt(line).text.length;
  return new vscode.Range(line, clamp(startColumn, 0, max), line, clamp(Math.max(endColumn, startColumn + 1), 0, Math.max(max, 1)));
}

function contextLabel(context) {
  switch (context) {
    case 'trigger':
      return 'trigger';
    case 'condition':
      return 'condition';
    case 'sceneStep':
      return 'scene step';
    case 'command':
      return 'command';
    default:
      return 'effect';
  }
}

function isLocalSpeakerAliasReference(field, value, rootValue) {
  return ['speaker', 'speakerId'].includes(field)
    && typeof value === 'string'
    && speakerAliasIdsFromValue(rootValue).includes(value);
}

function firstPresent(object, ...fields) {
  if (!object || typeof object !== 'object') {
    return undefined;
  }
  for (const field of fields) {
    if (hasOwn(object, field)) {
      return object[field];
    }
  }
  return undefined;
}

function firstString(object, ...fields) {
  if (!object || typeof object !== 'object') {
    return undefined;
  }
  for (const field of fields) {
    if (typeof object[field] === 'string' && object[field].trim()) {
      return { field, value: object[field].trim() };
    }
  }
  return undefined;
}

function normalizeChoices(value) {
  if (!value) {
    return [];
  }
  if (Array.isArray(value)) {
    return value.map(choiceFromValue).filter(Boolean);
  }
  if (typeof value === 'object') {
    return Object.entries(value).map(([id, choice]) => {
      const normalized = choiceFromValue(choice);
      return normalized ? { id, ...normalized } : undefined;
    }).filter(Boolean);
  }
  return [];
}

function choiceFromValue(value) {
  if (!value) {
    return undefined;
  }
  if (Array.isArray(value)) {
    if (value.length >= 3 && typeof value[0] === 'string') {
      return { id: value[0], text: value[1], next: value[2] };
    }
    if (value.length >= 2) {
      return { text: value[0], next: value[1] };
    }
    return undefined;
  }
  return typeof value === 'object' ? value : undefined;
}

function findKeyRange(document, key) {
  const pattern = new RegExp(`(^|[,{]\\s*)(${escapeRegExp(key)})(?=\\s*:)`);
  for (let lineNumber = 0; lineNumber < document.lineCount; lineNumber += 1) {
    const text = document.lineAt(lineNumber).text;
    const match = pattern.exec(text);
    if (match) {
      const start = match.index + match[0].lastIndexOf(match[2]);
      return rangeFromLineColumns(document, lineNumber, start, start + key.length);
    }
  }
  return findFieldNameRange(document, key);
}

function hasOwn(value, field) {
  return Object.prototype.hasOwnProperty.call(value, field);
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value));
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function dedupeDiagnostics(diagnostics) {
  const seen = new Set();
  const result = [];
  for (const item of diagnostics) {
    const key = `${item.range.start.line}:${item.range.start.character}:${item.message}`;
    if (!seen.has(key)) {
      seen.add(key);
      result.push(item);
    }
  }
  return result;
}

module.exports = {
  DiagnosticsService
};
