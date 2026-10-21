'use strict';

const path = require('path');
const vscode = require('vscode');

const CONFIG_NAMESPACE = 'echostoryEngine';

class ConfigurationService {
  constructor() {
    this.namespace = CONFIG_NAMESPACE;
  }

  get configuration() {
    return vscode.workspace.getConfiguration(this.namespace);
  }

  modRoot() {
    const configured = String(this.configuration.get('modRoot', '') || '').trim();
    if (configured) {
      return path.normalize(configured);
    }
    const folder = vscode.workspace.workspaceFolders && vscode.workspace.workspaceFolders[0];
    return folder ? folder.uri.fsPath : '';
  }

  storyDataDirectory() {
    return this.storyDataDirectories()[0] || '';
  }

  storyDataDirectories() {
    const root = this.modRoot();
    if (!root) {
      return [];
    }
    const defaultGlob = 'echo-story-content-pack/src/main/resources/data';
    const configured = String(this.configuration.get('storyDataGlob', defaultGlob) || '').trim();
    const includeMissingConfiguredRoots = configured !== defaultGlob;
    const configuredRoots = configured
      .split(/[;\r\n]+/)
      .map(value => value.trim())
      .filter(Boolean)
      .map(value => path.resolve(root, value))
      .filter(value => includeMissingConfiguredRoots || directoryExists(value));
    const fallbackRoots = [
      path.resolve(root, 'src/main/resources/data'),
      path.resolve(root, 'echo-story-content-pack/src/main/resources/data'),
      ...discoveredDataDirectories(root)
    ];
    return uniquePaths([...configuredRoots, ...existingDirectories(fallbackRoots)]);
  }

  docsDirectory() {
    const root = this.modRoot();
    if (!root) {
      return '';
    }
    return firstExistingDirectory([
      path.resolve(root, 'docs'),
      path.resolve(root, 'echo-story-content-pack/docs'),
      path.resolve(root, '..', 'docs', 'echo-platform-docs')
    ]) || path.resolve(root, 'docs');
  }

  templatesDirectory() {
    const root = this.modRoot();
    if (!root) {
      return '';
    }
    return firstExistingDirectory([
      path.resolve(root, 'authoring/templates'),
      path.resolve(root, 'echo-story-content-pack/authoring/templates'),
      path.resolve(root, 'docs/templates')
    ]) || path.resolve(root, 'authoring/templates');
  }

  diagnosticsEnabled() {
    return Boolean(this.configuration.get('enableDiagnostics', true));
  }

  validateReferences() {
    return Boolean(this.configuration.get('validateReferences', true));
  }

  strictUnknownFields() {
    return Boolean(this.configuration.get('strictUnknownFields', false));
  }

  autoRefreshIndex() {
    return Boolean(this.configuration.get('autoRefreshIndex', true));
  }

  inlayHintsEnabled() {
    return Boolean(this.configuration.get('inlayHints.enabled', true));
  }

  questVisualsEnabled() {
    return Boolean(this.configuration.get('questVisuals.enabled', true));
  }

  questCodeLensEnabled() {
    return Boolean(this.configuration.get('questVisuals.codeLens.enabled', true));
  }

  indexRefreshIntervalSeconds() {
    return clampNumber(this.configuration.get('indexRefreshIntervalSeconds', 30), 5, 600);
  }

  formatterOptions(formattingOptions = {}) {
    const useTabs = Boolean(this.configuration.get('formatter.useTabs', true));
    const tabSize = clampNumber(this.configuration.get('formatter.tabSize', formattingOptions.tabSize || 2), 1, 8);
    return {
      useTabs,
      tabSize,
      insertSpaces: useTabs ? false : Boolean(formattingOptions.insertSpaces),
      trailingComma: Boolean(this.configuration.get('formatter.trailingComma', true)),
      quote: enumValue(this.configuration.get('formatter.quote', 'double'), ['double', 'single'], 'double')
    };
  }

  affectsAny(event) {
    return event.affectsConfiguration(this.namespace);
  }
}

function clampNumber(value, min, max) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) {
    return min;
  }
  return Math.max(min, Math.min(max, numeric));
}

function enumValue(value, allowed, fallback) {
  const text = String(value || '');
  return allowed.includes(text) ? text : fallback;
}

function uniquePaths(values) {
  const seen = new Set();
  const result = [];
  for (const value of values) {
    const normalized = path.normalize(value);
    const key = normalized.toLowerCase();
    if (!seen.has(key)) {
      seen.add(key);
      result.push(normalized);
    }
  }
  return result;
}

function firstExistingDirectory(values) {
  for (const value of values) {
    if (directoryExists(value)) {
      return value;
    }
  }
  return '';
}

function existingDirectories(values) {
  return values.filter(directoryExists);
}

function discoveredDataDirectories(root) {
  const fs = require('fs');
  const result = [];
  for (const first of safeDirectories(fs, root)) {
    result.push(path.join(first, 'src', 'main', 'resources', 'data'));
    for (const second of safeDirectories(fs, first)) {
      result.push(path.join(second, 'src', 'main', 'resources', 'data'));
    }
  }
  return result;
}

function safeDirectories(fs, root) {
  try {
    return fs.readdirSync(root, { withFileTypes: true })
      .filter(entry => entry.isDirectory())
      .map(entry => path.join(root, entry.name));
  } catch {
    return [];
  }
}

function directoryExists(value) {
  const fs = require('fs');
  try {
    return fs.statSync(value).isDirectory();
  } catch {
    return false;
  }
}

module.exports = {
  CONFIG_NAMESPACE,
  ConfigurationService
};
