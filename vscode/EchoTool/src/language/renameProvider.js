'use strict';

const fs = require('fs/promises');
const vscode = require('vscode');
const {
  propertyOnLine,
  quotedValueRange,
  wordRangeAtPosition,
  referenceKindForField
} = require('./languageContext');
const {
  dialogueNodeRenameTarget,
  dialogueNodeRenameRanges,
  isValidDialogueNodeId
} = require('./dialogueNodeRename');
const { parseRootValue, dialogueNodeIdsFromValue } = require('./localStoryContext');
const {
  echoScriptRenameTarget,
  echoScriptReferenceRenameEditsInText,
  echoScriptIdentifierRenameEditsInText,
  isValidEchoScriptIdentifier,
  isValidEchoScriptNodeId,
  pascalToSnake
} = require('./echoscriptNavigation');
const {
  referenceQueryAtPosition,
  referenceRenameEditsInText
} = require('./storyReferenceScanner');
const { shortcutReference } = require('./referenceResolver');

class EchoStoryRenameProvider {
  constructor(index, parser) {
    this.index = index;
    this.parser = parser;
  }

  prepareRename(document, position) {
    if (document.languageId === 'echoscript') {
      const target = echoScriptRenameTarget(document.getText(), document.offsetAt(position));
      return target ? {
        range: toVscodeRange(document, target.range),
        placeholder: target.id
      } : undefined;
    }

    if (!this.canRenameInDocument(document)) {
      const referenceTarget = jsonReferenceRenameTarget(this.index, document, position);
      return referenceTarget ? {
        range: referenceTarget.range,
        placeholder: referenceTarget.id
      } : undefined;
    }

    const target = dialogueNodeRenameTarget(document.getText(), document.offsetAt(position));
    if (target && isValidDialogueNodeId(target.id)) {
      return {
        range: toVscodeRange(document, target.range),
        placeholder: target.id
      };
    }

    const referenceTarget = jsonReferenceRenameTarget(this.index, document, position);
    return referenceTarget ? {
      range: referenceTarget.range,
      placeholder: referenceTarget.id
    } : undefined;
  }

  provideRenameEdits(document, position, newName) {
    if (document.languageId === 'echoscript') {
      const target = echoScriptRenameTarget(document.getText(), document.offsetAt(position));
      if (!target) {
        return undefined;
      }
      if (target.kind === 'node' && !isValidEchoScriptNodeId(newName)) {
        throw new Error('EchoScript node id can contain only letters, numbers, "_", "-", ".", and ":".');
      }
      if (target.kind === 'identifier' && !isValidEchoScriptIdentifier(newName)) {
        throw new Error('EchoScript identifier can contain only letters, numbers, and "_", and cannot start with a number.');
      }
      if (target.kind === 'reference' && !isValidEchoScriptNodeId(newName)) {
        throw new Error('EchoStory reference id can contain only letters, numbers, "_", "-", ".", and ":".');
      }
      if (target.scope === 'workspaceIdentifier') {
        return this.provideEchoScriptWorkspaceIdentifierRename(document, target, newName);
      }
      if (target.scope === 'workspaceReference') {
        return this.provideEchoStoryReferenceRename(document, target, newName);
      }
      const edit = new vscode.WorkspaceEdit();
      for (const range of target.ranges) {
        edit.replace(document.uri, toVscodeRange(document, range), newName);
      }
      return edit;
    }

    const target = this.canRenameInDocument(document)
      ? dialogueNodeRenameTarget(document.getText(), document.offsetAt(position))
      : undefined;
    if (target) {
      if (!isValidDialogueNodeId(newName)) {
        throw new Error('EchoStory dialogue node id can contain only letters, numbers, "_", "-", ".", and ":".');
      }
      const edit = new vscode.WorkspaceEdit();
      for (const range of dialogueNodeRenameRanges(document.getText(), target.id)) {
        edit.replace(document.uri, toVscodeRange(document, range), newName);
      }
      return edit;
    }

    const referenceTarget = jsonReferenceRenameTarget(this.index, document, position);
    if (referenceTarget) {
      if (!isValidEchoScriptNodeId(newName)) {
        throw new Error('EchoStory reference id can contain only letters, numbers, "_", "-", ".", and ":".');
      }
      return this.provideIndexedReferenceRename(document, referenceTarget.query, newName);
    }
    return undefined;
  }

  async provideEchoScriptWorkspaceIdentifierRename(document, target, newName) {
    const edit = new vscode.WorkspaceEdit();
    const resourceNewId = target.referenceKind && target.referenceId ? pascalToSnake(newName) : undefined;
    for (const entry of uniqueStoryFileEntries(this.index)) {
      if (!entry.path.toLowerCase().endsWith('.es')) {
        continue;
      }
      const text = await textForFile(entry.path, document);
      const uri = vscode.Uri.file(entry.path);
      for (const renameEdit of echoScriptIdentifierRenameEditsInText(text, target.id, newName)) {
        edit.replace(uri, rangeFromOffsetsInText(text, renameEdit.range), renameEdit.newText);
      }
      if (resourceNewId) {
        for (const renameEdit of echoScriptReferenceRenameEditsInText(text, {
          kind: target.referenceKind,
          id: target.referenceId
        }, resourceNewId)) {
          edit.replace(uri, rangeFromOffsetsInText(text, renameEdit.range), renameEdit.newText);
        }
      }
    }
    if (resourceNewId) {
      await this.addJsonReferenceRenameEdits(edit, document, {
        kind: target.referenceKind,
        id: target.referenceId
      }, resourceNewId);
    }
    return edit;
  }

  async provideEchoStoryReferenceRename(document, target, newName) {
    return this.provideIndexedReferenceRename(document, {
      kind: target.referenceKind,
      id: target.id
    }, newName);
  }

  async provideIndexedReferenceRename(document, query, newName) {
    const edit = new vscode.WorkspaceEdit();
    for (const entry of uniqueStoryFileEntries(this.index)) {
      const text = await textForFile(entry.path, document);
      const uri = vscode.Uri.file(entry.path);
      if (entry.path.toLowerCase().endsWith('.es')) {
        for (const renameEdit of echoScriptReferenceRenameEditsInText(text, query, newName)) {
          edit.replace(uri, rangeFromOffsetsInText(text, renameEdit.range), renameEdit.newText);
        }
      } else {
        for (const renameEdit of referenceRenameEditsInText(text, query, entry.kind, newName, true)) {
          edit.replace(uri, new vscode.Range(renameEdit.line, renameEdit.start, renameEdit.line, renameEdit.end), renameEdit.newText);
        }
      }
    }
    return edit;
  }

  async addJsonReferenceRenameEdits(edit, document, query, newId) {
    for (const entry of uniqueStoryFileEntries(this.index)) {
      if (entry.path.toLowerCase().endsWith('.es')) {
        continue;
      }
      const text = await textForFile(entry.path, document);
      const uri = vscode.Uri.file(entry.path);
      for (const renameEdit of referenceRenameEditsInText(text, query, entry.kind, newId, true)) {
        edit.replace(uri, new vscode.Range(renameEdit.line, renameEdit.start, renameEdit.line, renameEdit.end), renameEdit.newText);
      }
    }
  }

  canRenameInDocument(document) {
    const kind = this.index && this.index.documentKind(document.uri.fsPath);
    if (kind === 'dialogue') {
      return true;
    }
    const root = parseRootValue(this.parser, document);
    return dialogueNodeIdsFromValue(root).length > 0;
  }
}

function toVscodeRange(document, range) {
  return new vscode.Range(document.positionAt(range.start), document.positionAt(range.end));
}

function jsonReferenceRenameTarget(index, document, position) {
  const query = referenceQueryAtPosition(index, document, position);
  if (!query || !query.id || !query.kind) {
    return undefined;
  }
  const range = jsonReferenceRangeAtPosition(index, document, position);
  return range ? {
    id: query.id,
    query,
    range
  } : undefined;
}

function jsonReferenceRangeAtPosition(index, document, position) {
  const line = document.lineAt(position.line).text;
  const property = propertyOnLine(line);
  if (property) {
    const value = quotedValueRange(document, position.line, property);
    if (value && position.character >= value.start && position.character <= value.end) {
      const kind = referenceKindForField(property);
      const documentKind = index && index.documentKind ? index.documentKind(document.uri.fsPath) : 'generic';
      if (kind || (property === 'id' && documentKind && documentKind !== 'generic')) {
        return new vscode.Range(position.line, value.start, position.line, value.end);
      }
    }
  }
  const wordRange = wordRangeAtPosition(document, position);
  if (wordRange && shortcutReference(document.getText(wordRange))) {
    return wordRange;
  }
  return undefined;
}

function uniqueStoryFileEntries(index) {
  const result = [];
  const seen = new Set();
  for (const entry of index && index.entries ? index.entries() : []) {
    if (!entry.path || seen.has(entry.path)) {
      continue;
    }
    const lower = entry.path.toLowerCase();
    if (!lower.endsWith('.es') && !lower.endsWith('.json5')) {
      continue;
    }
    seen.add(entry.path);
    result.push(entry);
  }
  return result;
}

async function textForFile(filePath, activeDocument) {
  const openDocument = vscode.workspace.textDocuments.find(document => samePath(document.uri.fsPath, filePath));
  if (openDocument) {
    return openDocument.getText();
  }
  if (activeDocument && samePath(activeDocument.uri.fsPath, filePath)) {
    return activeDocument.getText();
  }
  try {
    return await fs.readFile(filePath, 'utf8');
  } catch {
    return '';
  }
}

function rangeFromOffsetsInText(text, range) {
  return new vscode.Range(positionAtOffset(text, range.start), positionAtOffset(text, range.end));
}

function positionAtOffset(text, offset) {
  const source = String(text || '');
  let line = 0;
  let character = 0;
  for (let index = 0; index < Math.min(offset, source.length); index += 1) {
    if (source[index] === '\r' && source[index + 1] === '\n') {
      line += 1;
      character = 0;
      index += 1;
    } else if (source[index] === '\n' || source[index] === '\r') {
      line += 1;
      character = 0;
    } else {
      character += 1;
    }
  }
  return new vscode.Position(line, character);
}

function samePath(left, right) {
  return String(left || '').toLowerCase() === String(right || '').toLowerCase();
}

module.exports = {
  EchoStoryRenameProvider
};
