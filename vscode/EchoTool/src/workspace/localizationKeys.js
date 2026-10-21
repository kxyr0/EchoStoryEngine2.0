'use strict';

const STORY_PREFIX = 'story';

function localizationKeyEntriesFromValue(value, source = {}) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return [];
  }

  const entriesRoot = firstObject(value.entries, value.translations, value.values);
  if (!entriesRoot) {
    return [];
  }

  const prefix = normalizeKeyPrefix(firstString(value.prefix, value.keyPrefix, value.namespace));
  const locale = firstString(value.locale, source.locale);
  const flattened = flattenEntries(entriesRoot);
  const result = [];
  const seen = new Set();

  for (const item of flattened) {
    const fullKey = withPrefix(prefix, item.key);
    for (const key of localizationKeyAliases(fullKey)) {
      if (seen.has(key)) {
        continue;
      }
      seen.add(key);
      result.push({
        key,
        fullKey,
        sourceKey: item.key,
        locale,
        path: source.path || '',
        relativePath: source.relativePath || '',
        preview: item.preview
      });
    }
  }
  return result.sort((left, right) => left.key.localeCompare(right.key));
}

function localizationKeyAliases(fullKey) {
  const normalized = normalizeKeyPrefix(fullKey);
  if (!normalized) {
    return [];
  }
  const aliases = [normalized];
  const storyPrefix = `${STORY_PREFIX}.`;
  if (normalized.startsWith(storyPrefix)) {
    aliases.push(normalized.slice(storyPrefix.length));
  }
  return Array.from(new Set(aliases));
}

function normalizeKeyPrefix(value) {
  return String(value || '')
    .trim()
    .replace(/^\.+|\.+$/g, '')
    .replace(/\.\.+/g, '.');
}

function flattenEntries(value, pathParts = []) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return [];
  }

  const result = [];
  for (const [rawKey, child] of Object.entries(value)) {
    const key = normalizeKeyPrefix(rawKey);
    if (!key) {
      continue;
    }
    const nextPath = pathParts.concat(key);
    if (child && typeof child === 'object' && !Array.isArray(child)) {
      result.push(...flattenEntries(child, nextPath));
    } else {
      result.push({
        key: nextPath.join('.'),
        preview: typeof child === 'string' ? child : String(child)
      });
    }
  }
  return result;
}

function withPrefix(prefix, key) {
  const normalizedKey = normalizeKeyPrefix(key);
  if (!prefix) {
    return normalizedKey;
  }
  if (normalizedKey === prefix || normalizedKey.startsWith(`${prefix}.`)) {
    return normalizedKey;
  }
  return `${prefix}.${normalizedKey}`;
}

function firstObject(...values) {
  return values.find(value => value && typeof value === 'object' && !Array.isArray(value));
}

function firstString(...values) {
  const value = values.find(item => typeof item === 'string' && item.trim());
  return value ? value.trim() : '';
}

module.exports = {
  STORY_PREFIX,
  localizationKeyEntriesFromValue,
  localizationKeyAliases,
  normalizeKeyPrefix
};
