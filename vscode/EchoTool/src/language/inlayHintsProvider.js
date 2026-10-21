'use strict';

const vscode = require('vscode');
const { resolveReference } = require('./referenceResolver');
const { referenceOccurrencesInText } = require('./storyReferenceScanner');
const { echoScriptReferenceOccurrencesInText } = require('./echoscriptNavigation');

class EchoStoryInlayHintsProvider {
  constructor(index, configuration) {
    this.index = index;
    this.configuration = configuration;
  }

  provideInlayHints(document, range) {
    if (this.configuration.inlayHintsEnabled && !this.configuration.inlayHintsEnabled()) {
      return [];
    }

    const hints = [];
    const occurrences = document.languageId === 'echoscript'
      ? echoScriptReferenceOccurrencesInText(document.getText())
      : referenceOccurrencesInText(document.getText());
    for (const occurrence of occurrences) {
      if (occurrence.line < range.start.line || occurrence.line > range.end.line) {
        continue;
      }
      const resolved = resolveOccurrence(this.index, occurrence);
      if (!resolved || !resolved.location) {
        continue;
      }
      const hint = new vscode.InlayHint(
        new vscode.Position(occurrence.line, occurrence.end),
        ` -> ${kindLabel(resolved.kind)}`,
        vscode.InlayHintKind.Parameter
      );
      hint.paddingLeft = true;
      hint.tooltip = `EchoStory ${resolved.kind}: ${resolved.id}`;
      hints.push(hint);
    }
    return hints;
  }
}

function resolveOccurrence(index, occurrence) {
  if (occurrence.kind === 'stateKey') {
    const location = index.stateKeyLocation && index.stateKeyLocation(occurrence.id);
    return location ? { kind: 'stateKey', id: occurrence.id, location } : undefined;
  }
  return resolveReference(index, occurrence.kind, occurrence.id);
}

function kindLabel(kind) {
  switch (kind) {
    case 'stateKey':
      return 'state';
    case 'localizationKey':
      return 'text';
    case 'faceAnimation':
      return 'face';
    case 'cameraPath':
      return 'camera';
    default:
      return String(kind || 'ref');
  }
}

module.exports = {
  EchoStoryInlayHintsProvider,
  kindLabel
};
