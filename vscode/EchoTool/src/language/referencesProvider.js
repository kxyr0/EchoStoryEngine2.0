'use strict';

const fs = require('fs/promises');
const vscode = require('vscode');
const {
  referenceQueryAtPosition,
  referenceRangesInText
} = require('./storyReferenceScanner');
const {
  echoScriptReferenceQueryAtOffset,
  echoScriptReferenceRangesInText
} = require('./echoscriptNavigation');

class EchoStoryReferencesProvider {
  constructor(index) {
    this.index = index;
  }

  async provideReferences(document, position, context) {
    const query = document.languageId === 'echoscript'
      ? echoScriptReferenceQueryAtOffset(document.getText(), document.offsetAt(position))
      : referenceQueryAtPosition(this.index, document, position);
    if (!query || !query.id) {
      return [];
    }

    const locations = [];
    for (const entry of this.index.entries()) {
      const text = await safeRead(entry.path);
      if (!text) {
        continue;
      }
      const ranges = entry.path.toLowerCase().endsWith('.es')
        ? echoScriptReferenceRangesInText(text, query, entry.kind, Boolean(context && context.includeDeclaration))
        : referenceRangesInText(text, query, entry.kind, Boolean(context && context.includeDeclaration));
      const uri = vscode.Uri.file(entry.path);
      for (const range of ranges) {
        locations.push(new vscode.Location(
          uri,
          new vscode.Range(range.line, range.start, range.line, range.end)
        ));
      }
    }
    return locations;
  }
}

async function safeRead(filePath) {
  try {
    return await fs.readFile(filePath, 'utf8');
  } catch {
    return '';
  }
}

module.exports = {
  EchoStoryReferencesProvider
};
