'use strict';

const vscode = require('vscode');
const { referenceFieldNames, referenceKindForField } = require('./languageContext');
const { resolveReference, shortcutReference, isReferenceShortcutPrefix } = require('./referenceResolver');
const { isLocalizationKeyField } = require('./valueCompletionModel');
const { echoScriptReferenceOccurrencesInText } = require('./echoscriptNavigation');

class EchoStoryDocumentLinkProvider {
  constructor(index) {
    this.index = index;
  }

  provideDocumentLinks(document) {
    const links = [];
    const seen = new Set();
    if (document.languageId === 'echoscript') {
      addEchoScriptLinks(this.index, document, links, seen);
      return links;
    }
    for (let lineNumber = 0; lineNumber < document.lineCount; lineNumber += 1) {
      const line = document.lineAt(lineNumber).text;
      addFieldLinks(this.index, document, line, lineNumber, links, seen);
      addShortcutLinks(this.index, document, line, lineNumber, links, seen);
    }
    return links;
  }
}

function addEchoScriptLinks(index, document, links, seen) {
  for (const occurrence of echoScriptReferenceOccurrencesInText(document.getText())) {
    if (occurrence.kind === 'stateKey') {
      const location = index.stateKeyLocation && index.stateKeyLocation(occurrence.id);
      if (location) {
        addFileLink(document, occurrence.line, occurrence.start, occurrence.end, location.path, `Open state key: ${occurrence.id}`, links, seen);
      }
      continue;
    }
    const resolved = resolveReference(index, occurrence.kind, occurrence.id);
    if (resolved) {
      addLink(document, occurrence.line, occurrence.start, occurrence.end, resolved, links, seen);
    }
  }
}

function addFieldLinks(index, document, line, lineNumber, links, seen) {
  const pattern = /\b([A-Za-z_$][\w$-]*)\s*:\s*(['"])(.*?)\2/g;
  for (const match of line.matchAll(pattern)) {
    const fieldName = match[1];
    const id = match[3];
    if (isLocalizationKeyField(fieldName)) {
      const location = index.localizationKeyLocation && index.localizationKeyLocation(id);
      if (location) {
        const start = match.index + match[0].indexOf(id);
        addFileLink(document, lineNumber, start, start + id.length, location.path, `Open localization key: ${id}`, links, seen);
      }
      continue;
    }
    if (!referenceFieldNames().includes(fieldName)) {
      continue;
    }
    const kind = referenceKindForField(fieldName);
    const resolved = resolveReference(index, kind, id);
    if (!resolved) {
      continue;
    }
    const start = match.index + match[0].indexOf(id);
    addLink(document, lineNumber, start, start + id.length, resolved, links, seen);
  }
}

function addShortcutLinks(index, document, line, lineNumber, links, seen) {
  const pattern = /(['"])([A-Za-z_][\w-]*):([^'"]+)\1/g;
  for (const match of line.matchAll(pattern)) {
    if (!isReferenceShortcutPrefix(match[2])) {
      continue;
    }
    const shortcut = shortcutReference(`${match[2]}:${match[3]}`);
    const resolved = shortcut && resolveReference(index, shortcut.kind, shortcut.id);
    if (!resolved) {
      continue;
    }
    const start = match.index + 1;
    const end = start + match[2].length + 1 + match[3].length;
    addLink(document, lineNumber, start, end, resolved, links, seen);
  }
}

function addLink(document, lineNumber, start, end, resolved, links, seen) {
  addFileLink(document, lineNumber, start, end, resolved.location.path, `Open ${resolved.kind}: ${resolved.id}`, links, seen);
}

function addFileLink(document, lineNumber, start, end, filePath, tooltip, links, seen) {
  const range = new vscode.Range(lineNumber, start, lineNumber, end);
  const key = `${document.uri.toString()}:${lineNumber}:${start}:${end}`;
  if (seen.has(key)) {
    return;
  }
  seen.add(key);
  const link = new vscode.DocumentLink(range, vscode.Uri.file(filePath));
  link.tooltip = tooltip;
  links.push(link);
}

module.exports = {
  EchoStoryDocumentLinkProvider
};
