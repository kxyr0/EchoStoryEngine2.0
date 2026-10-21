'use strict';

const vscode = require('vscode');
const { formatEchoScript } = require('./echoscriptSupport');

class EchoScriptFormatter {
  constructor(configuration) {
    this.configuration = configuration;
  }

  provideDocumentFormattingEdits(document, formattingOptions) {
    const options = this.configuration && this.configuration.formatterOptions
      ? this.configuration.formatterOptions(formattingOptions)
      : formattingOptions;
    const formatted = formatEchoScript(document.getText(), {
      insertSpaces: Boolean(options.insertSpaces),
      tabSize: options.tabSize || 4
    });
    const lastLine = Math.max(0, document.lineCount - 1);
    const range = new vscode.Range(0, 0, lastLine, document.lineAt(lastLine).text.length);
    return [vscode.TextEdit.replace(range, formatted)];
  }
}

module.exports = {
  EchoScriptFormatter
};
