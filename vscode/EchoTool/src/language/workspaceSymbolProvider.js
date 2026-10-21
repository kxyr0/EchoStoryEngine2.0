'use strict';

const vscode = require('vscode');

class EchoStoryWorkspaceSymbolProvider {
  constructor(index) {
    this.index = index;
  }

  provideWorkspaceSymbols(query) {
    const normalizedQuery = String(query || '').trim().toLowerCase();
    return this.index.entries()
      .filter(entry => matchesQuery(entry, normalizedQuery))
      .slice(0, 500)
      .map(entry => symbolForEntry(entry));
  }
}

function matchesQuery(entry, query) {
  if (!query) {
    return true;
  }
  return entry.id.toLowerCase().includes(query)
    || entry.kind.toLowerCase().includes(query)
    || entry.relativePath.toLowerCase().includes(query);
}

function symbolForEntry(entry) {
  const uri = vscode.Uri.file(entry.path);
  const range = new vscode.Range(0, 0, 0, 0);
  return new vscode.SymbolInformation(
    `${entry.kind}: ${entry.id}`,
    symbolKind(entry.kind),
    `EchoStory ${entry.kind}`,
    new vscode.Location(uri, range)
  );
}

function symbolKind(kind) {
  switch (kind) {
    case 'story':
      return vscode.SymbolKind.Module;
    case 'event':
      return vscode.SymbolKind.Event;
    case 'dialogue':
      return vscode.SymbolKind.Interface;
    case 'scene':
      return vscode.SymbolKind.Module;
    case 'speaker':
    case 'npc':
      return vscode.SymbolKind.Class;
    case 'animation':
    case 'faceAnimation':
    case 'animationClip':
      return vscode.SymbolKind.Method;
    case 'animationProfile':
    case 'animationRig':
    case 'faceRig':
    case 'facePose':
    case 'npcAnimationBinding':
      return vscode.SymbolKind.Object;
    case 'cameraPath':
      return vscode.SymbolKind.Object;
    case 'trigger':
      return vscode.SymbolKind.Event;
    case 'localization':
    case 'localizationKey':
      return vscode.SymbolKind.String;
    case 'signalKey':
      return vscode.SymbolKind.Event;
    case 'stateKey':
      return vscode.SymbolKind.Key;
    default:
      return vscode.SymbolKind.Object;
  }
}

module.exports = {
  EchoStoryWorkspaceSymbolProvider
};
