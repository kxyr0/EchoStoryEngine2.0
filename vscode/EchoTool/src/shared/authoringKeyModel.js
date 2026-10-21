'use strict';

const STATE_KEY_FIELDS = new Set([
  'answerKey',
  'fromKey',
  'sourceKey',
  'stateKey',
  'targetKey',
  'toKey'
]);

const STATE_KEY_TYPES = new Set([
  'cipher_decode',
  'decode_cipher',
  'puzzle_answer',
  'puzzle_attempt',
  'remove_state',
  'set_state',
  'state_compare',
  'state_equals',
  'state_exists'
]);

function keyCompletionRole(context) {
  const fieldName = String(context && context.fieldName || '');
  const objectContext = String(context && context.objectContext || '');
  const documentKind = String(context && context.documentKind || '');
  const typeValue = String(context && context.typeValue || '');
  const pathParts = Array.isArray(context && context.pathParts) ? context.pathParts.map(String) : [];

  if (isSignalKeyField(fieldName, objectContext)) {
    return 'signal';
  }
  if (isStateKeyField(fieldName, typeValue)) {
    return 'state';
  }
  if (isCompactLocalizationKeyField(fieldName, objectContext, documentKind, pathParts, typeValue)) {
    return 'localization';
  }
  if (fieldName === 'key' && ['condition', 'effect'].includes(objectContext)) {
    return 'state';
  }
  return undefined;
}

function isSignalKeyField(fieldName, objectContext) {
  const field = String(fieldName || '');
  return field === 'signal'
    || field.endsWith('Signal')
    || (field === 'key' && objectContext === 'trigger');
}

function isStateKeyField(fieldName, typeValue) {
  const field = String(fieldName || '');
  return STATE_KEY_FIELDS.has(field)
    || (field === 'key' && STATE_KEY_TYPES.has(String(typeValue || '')));
}

function isStateKeyType(typeValue) {
  return STATE_KEY_TYPES.has(String(typeValue || ''));
}

function isCompactLocalizationKeyField(fieldName, objectContext, documentKind, pathParts, typeValue) {
  if (String(fieldName || '') !== 'key' || objectContext === 'trigger' || isStateKeyType(typeValue)) {
    return false;
  }
  if (['show_monologue', 'monologue', 'say_line'].includes(String(typeValue || ''))) {
    return true;
  }
  if (!['dialogue', 'scene'].includes(String(documentKind || ''))) {
    return false;
  }
  return pathParts.some(part => ['nodes', 'choices', 'choose', 'steps', 'lines', 'say'].includes(String(part)));
}

module.exports = {
  STATE_KEY_FIELDS,
  STATE_KEY_TYPES,
  keyCompletionRole,
  isSignalKeyField,
  isStateKeyField,
  isStateKeyType,
  isCompactLocalizationKeyField
};
