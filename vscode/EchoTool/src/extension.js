'use strict';

const fs = require('fs');
const path = require('path');
const vscode = require('vscode');
const { createStoryCatalog } = require('./catalog/defaultCatalog');
const { ConfigurationService } = require('./config/configurationService');
const { Json5Parser } = require('./json5/json5Parser');
const { Json5Formatter } = require('./json5/json5Formatter');
const { EchoStoryCompletionProvider } = require('./language/completionProvider');
const { EchoScriptCompletionProvider } = require('./language/echoscriptCompletionProvider');
const { EchoScriptSignatureHelpProvider } = require('./language/echoscriptSignatureHelpProvider');
const { EchoStoryHoverProvider } = require('./language/hoverProvider');
const { DiagnosticsService } = require('./language/diagnosticsService');
const { EchoScriptFormatter } = require('./language/echoscriptFormatter');
const { EchoStoryDocumentSymbolProvider } = require('./language/documentSymbolProvider');
const { EchoStoryDefinitionProvider } = require('./language/definitionProvider');
const { EchoStoryDocumentLinkProvider } = require('./language/documentLinkProvider');
const { EchoStoryCodeActionProvider } = require('./language/codeActionProvider');
const { EchoStorySemanticTokensProvider } = require('./language/semanticTokensProvider');
const { EchoStoryWorkspaceSymbolProvider } = require('./language/workspaceSymbolProvider');
const { EchoStoryReferencesProvider } = require('./language/referencesProvider');
const { EchoStoryInlayHintsProvider } = require('./language/inlayHintsProvider');
const { EchoStoryRenameProvider } = require('./language/renameProvider');
const { QuestDecorationsService } = require('./language/questDecorationsService');
const { EchoStoryQuestCodeLensProvider } = require('./language/questCodeLensProvider');
const { StoryWorkspaceIndex } = require('./workspace/storyWorkspaceIndex');

const LANGUAGE_ID = 'echostory-json5';
const ECHOSCRIPT_LANGUAGE_ID = 'echoscript';

let refreshTimer;
let fsWatcher;

async function activate(context) {
  const configuration = new ConfigurationService();
  const parser = new Json5Parser();
  const catalog = createStoryCatalog();
  const index = new StoryWorkspaceIndex(configuration, parser);
  const formatter = new Json5Formatter(parser, configuration);
  const echoScriptFormatter = new EchoScriptFormatter(configuration);
  const diagnostics = new DiagnosticsService(parser, catalog, index, configuration);
  const questDecorations = new QuestDecorationsService(configuration);

  context.subscriptions.push(diagnostics);
  context.subscriptions.push(questDecorations);
  context.subscriptions.push(vscode.languages.registerCompletionItemProvider(
    { language: LANGUAGE_ID, scheme: 'file' },
    new EchoStoryCompletionProvider(catalog, index, parser),
    '"',
    "'",
    ':',
    ' '
  ));
  context.subscriptions.push(vscode.languages.registerCompletionItemProvider(
    { language: ECHOSCRIPT_LANGUAGE_ID, scheme: 'file' },
    new EchoScriptCompletionProvider(index),
    '.',
    '(',
    '"',
    "'",
    ' '
  ));
  context.subscriptions.push(vscode.languages.registerSignatureHelpProvider(
    { language: ECHOSCRIPT_LANGUAGE_ID, scheme: 'file' },
    new EchoScriptSignatureHelpProvider(),
    '(',
    ',',
    ':'
  ));
  context.subscriptions.push(vscode.languages.registerHoverProvider(
    { language: LANGUAGE_ID, scheme: 'file' },
    new EchoStoryHoverProvider(catalog, index)
  ));
  context.subscriptions.push(vscode.languages.registerHoverProvider(
    { language: ECHOSCRIPT_LANGUAGE_ID, scheme: 'file' },
    new EchoStoryHoverProvider(catalog, index)
  ));
  context.subscriptions.push(vscode.languages.registerDocumentSymbolProvider(
    { language: LANGUAGE_ID, scheme: 'file' },
    new EchoStoryDocumentSymbolProvider(parser, index)
  ));
  context.subscriptions.push(vscode.languages.registerDocumentSemanticTokensProvider(
    { language: LANGUAGE_ID, scheme: 'file' },
    new EchoStorySemanticTokensProvider(catalog),
    EchoStorySemanticTokensProvider.legend
  ));
  context.subscriptions.push(vscode.languages.registerDocumentSemanticTokensProvider(
    { language: ECHOSCRIPT_LANGUAGE_ID, scheme: 'file' },
    new EchoStorySemanticTokensProvider(catalog),
    EchoStorySemanticTokensProvider.legend
  ));
  context.subscriptions.push(vscode.languages.registerDefinitionProvider(
    { language: LANGUAGE_ID, scheme: 'file' },
    new EchoStoryDefinitionProvider(index)
  ));
  context.subscriptions.push(vscode.languages.registerDefinitionProvider(
    { language: ECHOSCRIPT_LANGUAGE_ID, scheme: 'file' },
    new EchoStoryDefinitionProvider(index)
  ));
  context.subscriptions.push(vscode.languages.registerReferenceProvider(
    { language: LANGUAGE_ID, scheme: 'file' },
    new EchoStoryReferencesProvider(index)
  ));
  context.subscriptions.push(vscode.languages.registerReferenceProvider(
    { language: ECHOSCRIPT_LANGUAGE_ID, scheme: 'file' },
    new EchoStoryReferencesProvider(index)
  ));
  context.subscriptions.push(vscode.languages.registerRenameProvider(
    { language: LANGUAGE_ID, scheme: 'file' },
    new EchoStoryRenameProvider(index, parser)
  ));
  context.subscriptions.push(vscode.languages.registerRenameProvider(
    { language: ECHOSCRIPT_LANGUAGE_ID, scheme: 'file' },
    new EchoStoryRenameProvider(index, parser)
  ));
  context.subscriptions.push(vscode.languages.registerDocumentLinkProvider(
    { language: LANGUAGE_ID, scheme: 'file' },
    new EchoStoryDocumentLinkProvider(index)
  ));
  context.subscriptions.push(vscode.languages.registerDocumentLinkProvider(
    { language: ECHOSCRIPT_LANGUAGE_ID, scheme: 'file' },
    new EchoStoryDocumentLinkProvider(index)
  ));
  context.subscriptions.push(vscode.languages.registerInlayHintsProvider(
    { language: LANGUAGE_ID, scheme: 'file' },
    new EchoStoryInlayHintsProvider(index, configuration)
  ));
  context.subscriptions.push(vscode.languages.registerInlayHintsProvider(
    { language: ECHOSCRIPT_LANGUAGE_ID, scheme: 'file' },
    new EchoStoryInlayHintsProvider(index, configuration)
  ));
  context.subscriptions.push(vscode.languages.registerCodeLensProvider(
    { language: LANGUAGE_ID, scheme: 'file' },
    new EchoStoryQuestCodeLensProvider(configuration)
  ));
  context.subscriptions.push(vscode.languages.registerCodeActionsProvider(
    { language: LANGUAGE_ID, scheme: 'file' },
    new EchoStoryCodeActionProvider(),
    { providedCodeActionKinds: EchoStoryCodeActionProvider.providedCodeActionKinds }
  ));
  context.subscriptions.push(vscode.languages.registerCodeActionsProvider(
    { language: ECHOSCRIPT_LANGUAGE_ID, scheme: 'file' },
    new EchoStoryCodeActionProvider(),
    { providedCodeActionKinds: EchoStoryCodeActionProvider.providedCodeActionKinds }
  ));
  context.subscriptions.push(vscode.languages.registerWorkspaceSymbolProvider(
    new EchoStoryWorkspaceSymbolProvider(index)
  ));
  context.subscriptions.push(vscode.languages.registerDocumentFormattingEditProvider(
    { language: LANGUAGE_ID, scheme: 'file' },
    formatter
  ));
  context.subscriptions.push(vscode.languages.registerDocumentFormattingEditProvider(
    { language: ECHOSCRIPT_LANGUAGE_ID, scheme: 'file' },
    echoScriptFormatter
  ));

  context.subscriptions.push(vscode.workspace.onDidOpenTextDocument(document => {
    diagnostics.refresh(document);
    questDecorations.refreshDocument(document);
  }));
  context.subscriptions.push(vscode.workspace.onDidChangeTextDocument(event => {
    diagnostics.refresh(event.document);
    questDecorations.refreshDocument(event.document);
  }));
  context.subscriptions.push(vscode.workspace.onDidSaveTextDocument(async document => {
    if ((document.languageId === LANGUAGE_ID || document.languageId === ECHOSCRIPT_LANGUAGE_ID) && configuration.autoRefreshIndex()) {
      await refreshIndex(index, diagnostics, false);
    }
    diagnostics.refresh(document);
    questDecorations.refreshDocument(document);
  }));
  context.subscriptions.push(vscode.workspace.onDidChangeConfiguration(async event => {
    if (configuration.affectsAny(event)) {
      restartAutoRefresh(configuration, index, diagnostics, context);
      await refreshIndex(index, diagnostics, true);
      questDecorations.refreshVisibleEditors();
    }
  }));
  context.subscriptions.push(vscode.window.onDidChangeActiveTextEditor(editor => questDecorations.refresh(editor)));
  context.subscriptions.push(vscode.window.onDidChangeVisibleTextEditors(() => questDecorations.refreshVisibleEditors()));

  context.subscriptions.push(vscode.commands.registerCommand('echostoryEngine.refreshIndex', async () => {
    await refreshIndex(index, diagnostics, true);
  }));
  context.subscriptions.push(vscode.commands.registerCommand('echostoryEngine.noop', () => undefined));
  context.subscriptions.push(vscode.commands.registerCommand('echostoryEngine.showIndex', async () => {
    await showIndex(index);
  }));
  context.subscriptions.push(vscode.commands.registerCommand('echostoryEngine.formatCurrent', async () => {
    await vscode.commands.executeCommand('editor.action.formatDocument');
  }));
  context.subscriptions.push(vscode.commands.registerCommand('echostoryEngine.openDocs', async () => {
    await openAuthoringDocs(configuration, index);
  }));

  await refreshIndex(index, diagnostics, false);
  questDecorations.refreshVisibleEditors();
  restartAutoRefresh(configuration, index, diagnostics, context);
}

function deactivate() {
  stopAutoRefresh();
}

async function refreshIndex(index, diagnostics, showMessage) {
  const snapshot = await index.refresh();
  diagnostics.refreshOpenDocuments();
  if (showMessage) {
    const summary = index.summary();
    const detail = [
      `${summary.stories} stories`,
      `${summary.dialogues} dialogues`,
      `${summary.speakers + summary.npcs} NPC/speakers`,
      `${summary.playerProfileTemplates || 0} player templates`,
      `${summary.weapons || 0} weapons`,
      `${summary.animations + summary.faceAnimations + summary.animationClips} animations`,
      `${summary.animationProfiles} animation profiles`,
      `${summary.npcAnimationBindings} NPC animation bindings`,
      `${summary.localizationKeys} localization keys`,
      `${summary.signalKeys} signals`,
      `${summary.stateKeys} state keys`
    ].join(', ');
    if (snapshot.errors.length > 0) {
      vscode.window.showWarningMessage(`EchoStory index refreshed with ${snapshot.errors.length} parse/index warnings: ${detail}.`);
    } else {
      vscode.window.showInformationMessage(`EchoStory index refreshed: ${detail}.`);
    }
  }
}

function restartAutoRefresh(configuration, index, diagnostics, context) {
  stopAutoRefresh();
  if (!configuration.autoRefreshIndex()) {
    return;
  }

  refreshTimer = setInterval(() => {
    refreshIndex(index, diagnostics, false).catch(error => {
      console.error('[EchoStory] index refresh failed', error);
    });
  }, configuration.indexRefreshIntervalSeconds() * 1000);

  const storyDataDirectory = configuration.storyDataDirectory();
  if (storyDataDirectory && fs.existsSync(storyDataDirectory)) {
    try {
      fsWatcher = fs.watch(storyDataDirectory, { recursive: true }, debounce(() => {
        refreshIndex(index, diagnostics, false).catch(error => {
          console.error('[EchoStory] watched refresh failed', error);
        });
      }, 700));
    } catch (error) {
      console.warn('[EchoStory] fs.watch unavailable for story data directory', error);
    }
  }

  context.subscriptions.push({ dispose: stopAutoRefresh });
}

function stopAutoRefresh() {
  if (refreshTimer) {
    clearInterval(refreshTimer);
    refreshTimer = undefined;
  }
  if (fsWatcher) {
    fsWatcher.close();
    fsWatcher = undefined;
  }
}

async function showIndex(index) {
  const summary = index.summary();
  const lines = [
    '# EchoStory Index',
    '',
    `Mod root: \`${summary.modRoot || '(not configured)'}\``,
    `Story data: \`${(summary.storyDataDirectories || [summary.storyDataDirectory]).filter(Boolean).join('; ') || '(not configured)'}\``,
    '',
    `- Stories: ${summary.stories}`,
    `- Events: ${summary.events}`,
    `- Dialogues: ${summary.dialogues}`,
    `- Scenes: ${summary.scenes}`,
    `- Speakers: ${summary.speakers}`,
    `- NPCs: ${summary.npcs}`,
    `- Player profile templates: ${summary.playerProfileTemplates || 0}`,
    `- Weapons: ${summary.weapons || 0}`,
    `- Humanoid animations: ${summary.animations}`,
    `- Face animations: ${summary.faceAnimations}`,
    `- Animation profiles: ${summary.animationProfiles}`,
    `- Animation rigs: ${summary.animationRigs}`,
    `- Animation clips: ${summary.animationClips}`,
    `- Face rigs: ${summary.faceRigs}`,
    `- Face poses: ${summary.facePoses}`,
    `- NPC animation bindings: ${summary.npcAnimationBindings}`,
    `- Triggers: ${summary.triggers}`,
    `- Camera paths: ${summary.cameraPaths}`,
    `- Localization keys: ${summary.localizationKeys}`,
    `- Signal keys: ${summary.signalKeys}`,
    `- State keys: ${summary.stateKeys}`,
    `- Templates: ${summary.templates}`,
    `- Duplicate id groups: ${summary.duplicateGroups}`
  ];
  const document = await vscode.workspace.openTextDocument({
    content: lines.join('\n'),
    language: 'markdown'
  });
  await vscode.window.showTextDocument(document, { preview: true });
}

async function openAuthoringDocs(configuration, index) {
  const editor = vscode.window.activeTextEditor;
  const kind = editor ? index.documentKind(editor.document.uri.fsPath) : 'generic';
  const docsFile = docsFileForKind(configuration.docsDirectory(), kind);
  if (!docsFile || !fs.existsSync(docsFile)) {
    vscode.window.showWarningMessage(`EchoStory docs file not found for ${kind}.`);
    return;
  }
  const document = await vscode.workspace.openTextDocument(vscode.Uri.file(docsFile));
  await vscode.window.showTextDocument(document, { preview: true, viewColumn: vscode.ViewColumn.Beside });
}

function docsFileForKind(docsDirectory, kind) {
  const relative = {
    story: ['authoring', 'WRITING_STORIES.md'],
    event: ['events', 'events.md'],
    dialogue: ['authoring', 'WRITING_DIALOGUES.md'],
    scene: ['scenes', 'scenes.md'],
    speaker: ['authoring', 'WRITING_NPCS.md'],
    npc: ['authoring', 'WRITING_NPCS.md'],
    npcForm: ['authoring', 'WRITING_NPCS.md'],
    playerProfileTemplate: ['authoring', 'PLAYER_PROFILE_ES.md'],
    weapon: ['authoring', 'WEAPON_ES_REFERENCE.md'],
    trigger: ['authoring', 'TRIGGER_RESOURCE.md'],
    animation: ['animation', 'animation-system-current.md'],
    faceAnimation: ['animation', 'animation-system-current.md'],
    animationProfile: ['architecture', 'RUNTIME_DEPENDENCIES.md'],
    animationRig: ['architecture', 'RUNTIME_DEPENDENCIES.md'],
    animationClip: ['architecture', 'RUNTIME_DEPENDENCIES.md'],
    faceRig: ['architecture', 'RUNTIME_DEPENDENCIES.md'],
    facePose: ['architecture', 'RUNTIME_DEPENDENCIES.md'],
    npcAnimationBinding: ['architecture', 'RUNTIME_DEPENDENCIES.md'],
    cameraPath: ['camera', 'player-actor-cutscenes.md'],
    localization: ['localization', 'story-localization.md']
  }[kind] || ['authoring', 'START_HERE.md'];
  return path.join(docsDirectory, ...relative);
}

function debounce(fn, delayMs) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), delayMs);
  };
}

module.exports = {
  activate,
  deactivate
};
