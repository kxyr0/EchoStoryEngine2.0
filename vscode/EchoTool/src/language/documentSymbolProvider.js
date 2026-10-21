'use strict';

const vscode = require('vscode');

class EchoStoryDocumentSymbolProvider {
  constructor(parser, index) {
    this.parser = parser;
    this.index = index;
  }

  provideDocumentSymbols(document) {
    const parsed = this.parser.parse(document.getText(), document.uri.fsPath);
    if (!parsed.ok || !parsed.value || typeof parsed.value !== 'object') {
      return [];
    }
    const kind = this.index.documentKind(document.uri.fsPath);
    const rootName = rootSymbolName(kind, parsed.value, document.uri);
    const fullRange = new vscode.Range(
      document.positionAt(0),
      document.positionAt(document.getText().length)
    );
    const root = new vscode.DocumentSymbol(rootName, kind, vscode.SymbolKind.Object, fullRange, fullRange);
    addChildren(root, parsed.value, document, []);
    return [root];
  }
}

function addChildren(parent, value, document, path) {
  if (!value || typeof value !== 'object') {
    return;
  }

  if (Array.isArray(value)) {
    for (let index = 0; index < value.length; index += 1) {
      const entry = value[index];
      if (entry && typeof entry === 'object') {
        const name = symbolNameForValue(entry, `[${index}]`);
        const symbol = new vscode.DocumentSymbol(name, path.concat(index).join('.'), vscode.SymbolKind.Object, parent.range, parent.selectionRange);
        parent.children.push(symbol);
        addChildren(symbol, entry, document, path.concat(index));
      }
    }
    return;
  }

  for (const [key, entry] of Object.entries(value)) {
    if (!entry || typeof entry !== 'object') {
      continue;
    }
    if (!shouldExposeKey(key, entry)) {
      continue;
    }
    const name = key === 'nodes' && !Array.isArray(entry)
      ? 'nodes'
      : symbolNameForValue(entry, key);
    const symbol = new vscode.DocumentSymbol(name, key, symbolKindForKey(key), parent.range, parent.selectionRange);
    parent.children.push(symbol);
    addChildren(symbol, entry, document, path.concat(key));
  }
}

function shouldExposeKey(key, value) {
  return [
    'trigger',
    'condition',
    'if',
    'effects',
    'do',
    'then',
    'else',
    'nodes',
    'choices',
    'steps',
    'interactionActions',
    'startEffects',
    'completeEffects',
    'abortEffects'
  ].includes(key) || (value && typeof value === 'object' && (value.id || value.type));
}

function symbolNameForValue(value, fallback) {
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    if (typeof value.id === 'string') {
      return value.id;
    }
    if (typeof value.type === 'string') {
      return `type: ${value.type}`;
    }
    if (typeof value.text === 'string') {
      return value.text.slice(0, 48);
    }
    if (typeof value.say === 'string') {
      return value.say.slice(0, 48);
    }
  }
  return fallback;
}

function symbolKindForKey(key) {
  switch (key) {
    case 'nodes':
    case 'steps':
    case 'choices':
    case 'effects':
    case 'do':
      return vscode.SymbolKind.Array;
    case 'trigger':
    case 'condition':
    case 'if':
      return vscode.SymbolKind.Event;
    default:
      return vscode.SymbolKind.Object;
  }
}

function rootSymbolName(kind, value, uri) {
  if (value && typeof value.id === 'string') {
    return `${kind}: ${value.id}`;
  }
  return `${kind}: ${uri.path.split('/').pop()}`;
}

module.exports = {
  EchoStoryDocumentSymbolProvider
};
