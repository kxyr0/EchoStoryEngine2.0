'use strict';

const vscode = require('vscode');

class EchoStoryCodeActionProvider {
  provideCodeActions(_document, _range, context) {
    const actions = [];
    for (const diagnostic of context.diagnostics || []) {
      if (/Unknown .* reference/.test(diagnostic.message)) {
        actions.push(commandAction(
          'EchoStory: Refresh story index',
          'echostoryEngine.refreshIndex',
          diagnostic
        ));
      }
      if (/JSON5 parse error/.test(diagnostic.message)) {
        actions.push(commandAction(
          'EchoStory: Format current JSON5 file',
          'echostoryEngine.formatCurrent',
          diagnostic
        ));
      }
      if (/ESL04\d\d|Incomplete fluent|Return statements must end|Variable declarations must end/.test(diagnostic.message)) {
        actions.push(commandAction(
          'EchoStory: Format current EchoScript file',
          'echostoryEngine.formatCurrent',
          diagnostic
        ));
      }
    }
    return actions;
  }
}

function commandAction(title, command, diagnostic) {
  const action = new vscode.CodeAction(title, vscode.CodeActionKind.QuickFix);
  action.command = { command, title };
  action.diagnostics = [diagnostic];
  action.isPreferred = /Refresh/.test(title);
  return action;
}

EchoStoryCodeActionProvider.providedCodeActionKinds = [
  vscode.CodeActionKind.QuickFix
];

module.exports = {
  EchoStoryCodeActionProvider
};
