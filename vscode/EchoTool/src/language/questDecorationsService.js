'use strict';

const vscode = require('vscode');
const {
  QUEST_STATUS_DETAILS,
  questVisualsInText,
  questHoverLines
} = require('./questVisuals');

const LANGUAGE_ID = 'echostory-json5';

class QuestDecorationsService {
  constructor(configuration) {
    this.configuration = configuration;
    this.decorations = new Map(Object.entries(QUEST_STATUS_DETAILS).map(([status, detail]) => [
      status,
      vscode.window.createTextEditorDecorationType({
        backgroundColor: detail.backgroundColor,
        border: `1px solid ${detail.borderColor}`,
        borderRadius: '3px',
        overviewRulerColor: detail.borderColor,
        overviewRulerLane: vscode.OverviewRulerLane.Right
      })
    ]));
  }

  refreshVisibleEditors() {
    for (const editor of vscode.window.visibleTextEditors) {
      this.refresh(editor);
    }
  }

  refreshDocument(document) {
    for (const editor of vscode.window.visibleTextEditors) {
      if (editor.document.uri.toString() === document.uri.toString()) {
        this.refresh(editor);
      }
    }
  }

  refresh(editor) {
    if (!editor || editor.document.languageId !== LANGUAGE_ID) {
      return;
    }

    if (this.configuration.questVisualsEnabled && !this.configuration.questVisualsEnabled()) {
      this.clear(editor);
      return;
    }

    const grouped = new Map(Object.keys(QUEST_STATUS_DETAILS).map(status => [status, []]));
    for (const quest of questVisualsInText(editor.document.getText())) {
      const detail = QUEST_STATUS_DETAILS[quest.status];
      const range = toVscodeRange(editor.document, quest.statusRange || quest.typeRange);
      grouped.get(quest.status).push({
        range,
        hoverMessage: markdown(questHoverLines(quest)),
        renderOptions: {
          after: {
            contentText: ` [${detail.label}]`,
            color: detail.color,
            fontWeight: '700'
          }
        }
      });
    }

    for (const [status, decoration] of this.decorations.entries()) {
      editor.setDecorations(decoration, grouped.get(status) || []);
    }
  }

  clear(editor) {
    for (const decoration of this.decorations.values()) {
      editor.setDecorations(decoration, []);
    }
  }

  dispose() {
    for (const decoration of this.decorations.values()) {
      decoration.dispose();
    }
  }
}

function toVscodeRange(document, range) {
  return new vscode.Range(document.positionAt(range.start), document.positionAt(range.end));
}

function markdown(lines) {
  const md = new vscode.MarkdownString(lines.join('\n\n'));
  md.isTrusted = false;
  return md;
}

module.exports = {
  QuestDecorationsService
};
