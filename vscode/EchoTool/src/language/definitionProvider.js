'use strict';

const fs = require('fs/promises');
const vscode = require('vscode');
const {
  propertyOnLine,
  quotedValueRange,
  wordRangeAtPosition,
  referenceKindForField
} = require('./languageContext');
const { resolveReference, shortcutReference } = require('./referenceResolver');
const { isLocalizationKeyField } = require('./valueCompletionModel');
const { echoScriptDefinitionTarget } = require('./echoscriptNavigation');

class EchoStoryDefinitionProvider {
  constructor(index) {
    this.index = index;
  }

  async provideDefinition(document, position) {
    if (document.languageId === 'echoscript') {
      const target = echoScriptDefinitionTarget(document.getText(), document.offsetAt(position));
      if (!target) {
        return undefined;
      }
      if (target.type === 'local') {
        return new vscode.Location(document.uri, rangeFromOffsets(document, target.range));
      }
      if (target.kind === 'stateKey') {
        return locationForStateKey(this.index, target);
      }
      const resolved = resolveReference(this.index, target.kind, target.id);
      return resolved ? locationForResolvedReference(resolved) : undefined;
    }

    const candidate = referenceAtPosition(document, position);
    if (!candidate) {
      return undefined;
    }
    if (candidate.kind === 'localizationKey') {
      const location = this.index.localizationKeyLocation && this.index.localizationKeyLocation(candidate.id);
      return location ? locationForLocalizationKey(location) : undefined;
    }
    const resolved = resolveReference(this.index, candidate.kind, candidate.id);
    if (!resolved) {
      return undefined;
    }
    return locationForResolvedReference(resolved);
  }
}

function rangeFromOffsets(document, range) {
  return new vscode.Range(document.positionAt(range.start), document.positionAt(range.end));
}

function referenceAtPosition(document, position) {
  const line = document.lineAt(position.line).text;
  const property = propertyOnLine(line);
  if (property) {
    const kind = referenceKindForField(property);
    const value = quotedValueRange(document, position.line, property);
    if (isLocalizationKeyField(property) && value && position.character >= value.start && position.character <= value.end) {
      return { kind: 'localizationKey', id: value.value };
    }
    if (kind && value && position.character >= value.start && position.character <= value.end) {
      return { kind, id: value.value };
    }
  }

  const wordRange = wordRangeAtPosition(document, position);
  if (!wordRange) {
    return undefined;
  }
  const shortcut = shortcutReference(document.getText(wordRange));
  return shortcut ? { kind: shortcut.kind, id: shortcut.id } : undefined;
}

async function locationForResolvedReference(resolved) {
  const uri = vscode.Uri.file(resolved.location.path);
  const range = await identifierRange(uri, resolved.id);
  return new vscode.Location(uri, range);
}

async function locationForLocalizationKey(location) {
  const uri = vscode.Uri.file(location.path);
  const range = await localizationKeyRange(uri, location);
  return new vscode.Location(uri, range);
}

async function locationForStateKey(index, target) {
  const candidates = Array.isArray(target.ids) && target.ids.length > 0 ? target.ids : [target.id];
  for (const key of candidates) {
    const location = index.stateKeyLocation && index.stateKeyLocation(key);
    if (!location) {
      continue;
    }
    const uri = vscode.Uri.file(location.path);
    const range = await stateKeyRange(uri, target, candidates);
    return new vscode.Location(uri, range);
  }
  return undefined;
}

async function stateKeyRange(uri, target, keys) {
  const candidates = [
    target.symbol,
    ...(Array.isArray(keys) ? keys : []),
    target.id
  ].filter(Boolean);
  try {
    const text = await fs.readFile(uri.fsPath, 'utf8');
    const lines = text.split(/\r?\n/);
    for (let lineNumber = 0; lineNumber < lines.length; lineNumber += 1) {
      for (const candidate of candidates) {
        const start = lines[lineNumber].indexOf(candidate);
        if (start >= 0) {
          return new vscode.Range(lineNumber, start, lineNumber, start + candidate.length);
        }
      }
    }
  } catch {
    // Fall back to file start when the file is temporarily unreadable.
  }
  return new vscode.Range(0, 0, 0, 0);
}

async function localizationKeyRange(uri, location) {
  try {
    const text = await fs.readFile(uri.fsPath, 'utf8');
    const lines = text.split(/\r?\n/);
    const fullKey = String(location.fullKey || location.key || '');
    const sourceKey = String(location.sourceKey || location.key || '');
    const lastSegment = sourceKey.split('.').filter(Boolean).pop() || sourceKey;
    const preview = String(location.preview || '');
    const candidates = [fullKey, sourceKey, lastSegment].filter(Boolean);

    if (preview) {
      for (let lineNumber = 0; lineNumber < lines.length; lineNumber += 1) {
        if (!lines[lineNumber].includes(preview)) {
          continue;
        }
        const range = keyRangeOnLine(lines[lineNumber], lineNumber, candidates);
        if (range) {
          return range;
        }
      }
    }

    for (let lineNumber = 0; lineNumber < lines.length; lineNumber += 1) {
      const range = keyRangeOnLine(lines[lineNumber], lineNumber, candidates);
      if (range) {
        return range;
      }
    }
  } catch {
    // Fall back to file start when the file is temporarily unreadable.
  }
  return new vscode.Range(0, 0, 0, 0);
}

function keyRangeOnLine(line, lineNumber, candidates) {
  for (const key of candidates) {
    const escaped = escapeRegExp(key);
    const patterns = [
      new RegExp(`(['"])${escaped}\\1\\s*:`),
      new RegExp(`\\b${escaped}\\b\\s*:`)
    ];
    for (const pattern of patterns) {
      const match = pattern.exec(line);
      if (match) {
        const start = match.index + match[0].indexOf(key);
        return new vscode.Range(lineNumber, start, lineNumber, start + key.length);
      }
    }
  }
  return undefined;
}

async function identifierRange(uri, id) {
  try {
    const text = await fs.readFile(uri.fsPath, 'utf8');
    const lines = text.split(/\r?\n/);
    const escaped = escapeRegExp(id);
    const patterns = [
      new RegExp(`\\bid\\s*:\\s*(['"])${escaped}\\1`),
      new RegExp(`(['"])${escaped}\\1`)
    ];
    for (let lineNumber = 0; lineNumber < lines.length; lineNumber += 1) {
      for (const pattern of patterns) {
        const match = pattern.exec(lines[lineNumber]);
        if (match) {
          const start = match.index + match[0].indexOf(id);
          return new vscode.Range(lineNumber, start, lineNumber, start + id.length);
        }
      }
    }
  } catch {
    // Fall back to file start when the file is temporarily unreadable.
  }
  return new vscode.Range(0, 0, 0, 0);
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

module.exports = {
  EchoStoryDefinitionProvider,
  referenceAtPosition
};
