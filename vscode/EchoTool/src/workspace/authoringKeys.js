'use strict';

const {
  isSignalKeyField,
  isStateKeyField
} = require('../shared/authoringKeyModel');

function authoringKeyEntriesFromValue(value, source = {}) {
  const stateKeys = [];
  const signalKeys = [];
  visit(value, [], (field, fieldValue, owner, pathParts) => {
    if (typeof fieldValue !== 'string' || !fieldValue.trim() || fieldValue.includes('${')) {
      return;
    }
    const typeValue = owner && typeof owner.type === 'string' ? owner.type : '';
    const objectContext = pathParts.includes('trigger') ? 'trigger' : '';
    const base = {
      id: fieldValue.trim(),
      field,
      path: source.path || '',
      relativePath: source.relativePath || '',
      ownerId: source.id || '',
      ownerKind: source.kind || ''
    };
    if (isSignalKeyField(field, objectContext)) {
      signalKeys.push({ ...base, kind: 'signalKey' });
      return;
    }
    if (isStateKeyField(field, typeValue)) {
      stateKeys.push({ ...base, kind: 'stateKey' });
    }
  });
  return {
    stateKeys: dedupeById(stateKeys),
    signalKeys: dedupeById(signalKeys)
  };
}

function visit(value, pathParts, onField) {
  if (Array.isArray(value)) {
    for (let index = 0; index < value.length; index += 1) {
      visit(value[index], pathParts.concat(index), onField);
    }
    return;
  }
  if (!value || typeof value !== 'object') {
    return;
  }
  for (const [field, child] of Object.entries(value)) {
    onField(field, child, value, pathParts);
    visit(child, pathParts.concat(field), onField);
  }
}

function dedupeById(entries) {
  const seen = new Map();
  for (const entry of entries) {
    seen.set(entry.id, entry);
  }
  return Array.from(seen.values()).sort((left, right) => left.id.localeCompare(right.id));
}

module.exports = {
  authoringKeyEntriesFromValue
};
