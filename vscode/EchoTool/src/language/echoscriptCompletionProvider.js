'use strict';

const vscode = require('vscode');
const { echoScriptCompletionEntries } = require('./echoscriptCompletionModel');

const KIND = {
  snippet: vscode.CompletionItemKind.Snippet,
  method: vscode.CompletionItemKind.Method,
  variable: vscode.CompletionItemKind.Variable,
  reference: vscode.CompletionItemKind.Reference,
  value: vscode.CompletionItemKind.Value
};

class EchoScriptCompletionProvider {
  constructor(index) {
    this.index = index;
  }

  provideCompletionItems(document, position) {
    const text = document.getText();
    const offset = document.offsetAt(position);
    return echoScriptCompletionEntries(text, offset, this.index).map(entry => toCompletionItem(entry, document, position));
  }
}

function toCompletionItem(entry, document, position) {
  const item = new vscode.CompletionItem(entry.label, KIND[entry.kind] || vscode.CompletionItemKind.Text);
  item.detail = entry.detail || 'EchoScript подсказка';
  item.sortText = entry.sortText;
  if (Number.isInteger(entry.replaceLength)) {
    item.range = new vscode.Range(document.positionAt(document.offsetAt(position) - entry.replaceLength), position);
  }
  item.insertText = entry.kind === 'snippet'
    ? new vscode.SnippetString(entry.insertText)
    : entry.insertText;
  return item;
}

module.exports = {
  EchoScriptCompletionProvider
};
