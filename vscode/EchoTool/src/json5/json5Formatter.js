'use strict';

const vscode = require('vscode');
const { CommentPreservingJson5Formatter } = require('./commentPreservingFormatter');

class Json5Formatter {
  constructor(parser, configuration) {
    this.parser = parser;
    this.configuration = configuration;
    this.commentPreservingFormatter = new CommentPreservingJson5Formatter();
  }

  async provideDocumentFormattingEdits(document, formattingOptions) {
    const formatted = await this.format(document.getText(), formattingOptions);
    const range = new vscode.Range(
      document.positionAt(0),
      document.positionAt(document.getText().length)
    );
    return [vscode.TextEdit.replace(range, formatted)];
  }

  async format(text, formattingOptions) {
    const options = this.configuration.formatterOptions(formattingOptions);
    try {
      return await this.commentPreservingFormatter.format(text, options);
    } catch (error) {
      const parsed = this.parser.parse(text, 'JSON5 formatter fallback');
      if (!parsed.ok) {
        throw new Error(`Cannot format invalid JSON5: ${parsed.error.message}`);
      }
      throw error;
    }
  }
}

module.exports = {
  Json5Formatter
};
