'use strict';

function parseRootValue(parser, document) {
  if (!parser || !document) {
    return undefined;
  }
  const parsed = parser.parse(document.getText(), document.uri.fsPath);
  return parsed.ok && parsed.value && typeof parsed.value === 'object' ? parsed.value : undefined;
}

function dialogueNodeIds(parser, document) {
  return dialogueNodeIdsFromValue(parseRootValue(parser, document));
}

function dialogueNodeIdsFromValue(root) {
  if (!root || typeof root !== 'object') {
    return [];
  }
  const nodes = root.nodes;
  if (!nodes || typeof nodes !== 'object') {
    return [];
  }
  if (Array.isArray(nodes)) {
    return unique(nodes
      .filter(node => node && typeof node === 'object' && typeof node.id === 'string')
      .map(node => node.id.trim()));
  }
  return unique(Object.keys(nodes));
}

function dialogueNodeMapFromValue(root) {
  const nodes = root && root.nodes;
  if (!nodes || typeof nodes !== 'object') {
    return new Map();
  }
  if (Array.isArray(nodes)) {
    return new Map(nodes
      .filter(node => node && typeof node === 'object' && typeof node.id === 'string' && node.id.trim())
      .map(node => [node.id.trim(), node]));
  }
  return new Map(Object.entries(nodes).filter(([, node]) => node && typeof node === 'object'));
}

function dialogueChoiceIds(parser, document, nodeId) {
  return dialogueChoiceIdsFromValue(parseRootValue(parser, document), nodeId);
}

function dialogueChoiceIdsFromValue(root, nodeId) {
  const node = dialogueNodeMapFromValue(root).get(String(nodeId || ''));
  if (!node || typeof node !== 'object') {
    return [];
  }
  const choices = node.choices || node.choose;
  if (!choices) {
    return [];
  }
  if (Array.isArray(choices)) {
    return unique(choices.map((choice, index) => choiceIdFromValue(choice, undefined, index)));
  }
  if (typeof choices === 'object') {
    return unique(Object.entries(choices).map(([id, choice], index) => choiceIdFromValue(choice, id, index)));
  }
  return [];
}

function speakerAliasIds(parser, document) {
  return speakerAliasIdsFromValue(parseRootValue(parser, document));
}

function speakerAliasIdsFromValue(root) {
  const aliases = root && root.speakerAliases;
  return aliases && typeof aliases === 'object' && !Array.isArray(aliases)
    ? Object.keys(aliases).sort()
    : [];
}

function speakerAliasTargetsFromValue(root) {
  const aliases = root && root.speakerAliases;
  if (!aliases || typeof aliases !== 'object' || Array.isArray(aliases)) {
    return [];
  }
  return Object.entries(aliases)
    .filter(([, value]) => typeof value === 'string' && value.trim())
    .map(([alias, target]) => ({ alias, target: target.trim() }));
}

function unique(values) {
  return Array.from(new Set(values.filter(Boolean))).sort();
}

function choiceIdFromValue(choice, idFromMap, index) {
  if (idFromMap) {
    return idFromMap;
  }
  if (Array.isArray(choice) && typeof choice[0] === 'string') {
    return choice[0].trim();
  }
  if (choice && typeof choice === 'object' && typeof choice.id === 'string') {
    return choice.id.trim();
  }
  return `choice_${index + 1}`;
}

module.exports = {
  parseRootValue,
  dialogueNodeIds,
  dialogueNodeIdsFromValue,
  dialogueNodeMapFromValue,
  dialogueChoiceIds,
  dialogueChoiceIdsFromValue,
  speakerAliasIds,
  speakerAliasIdsFromValue,
  speakerAliasTargetsFromValue
};
