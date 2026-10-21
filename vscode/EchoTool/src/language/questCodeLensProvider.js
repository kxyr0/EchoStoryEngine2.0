'use strict';

const vscode = require('vscode');
const {
  questVisualsInText,
  questCodeLensTitle
} = require('./questVisuals');

class EchoStoryQuestCodeLensProvider {
  constructor(configuration) {
    this.configuration = configuration;
  }

  provideCodeLenses(document) {
    if (this.configuration.questCodeLensEnabled && !this.configuration.questCodeLensEnabled()) {
      return [];
    }

    return questVisualsInText(document.getText()).map(quest => {
      const position = document.positionAt(quest.objectRange.start);
      return new vscode.CodeLens(
        new vscode.Range(position.line, 0, position.line, 0),
        {
          title: questCodeLensTitle(quest),
          command: 'echostoryEngine.noop'
        }
      );
    });
  }
}

module.exports = {
  EchoStoryQuestCodeLensProvider
};
