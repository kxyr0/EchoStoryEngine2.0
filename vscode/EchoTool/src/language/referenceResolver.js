'use strict';

const SHORTCUT_REFERENCE_KIND = new Map([
  ['dialogue', 'dialogue'],
  ['start_dialogue', 'dialogue'],
  ['startdialogue', 'dialogue'],
  ['scene', 'scene'],
  ['start_scene', 'scene'],
  ['startscene', 'scene']
]);

const RELATED_KINDS = new Map([
  ['story', ['story']],
  ['environment', ['environment']],
  ['dialogue', ['dialogue']],
  ['scene', ['scene']],
  ['speaker', ['speaker', 'npc']],
  ['npc', ['npc', 'speaker']],
  ['npcForm', ['npcForm']],
  ['playerProfileTemplate', ['playerProfileTemplate']],
  ['playerProfile', ['playerProfileTemplate']],
  ['weapon', ['weapon']],
  ['animation', ['animation', 'faceAnimation']],
  ['faceAnimation', ['faceAnimation', 'animation']],
  ['animationProfile', ['animationProfile']],
  ['animationRig', ['animationRig', 'faceRig']],
  ['animationClip', ['animationClip']],
  ['faceRig', ['faceRig', 'animationRig']],
  ['facePose', ['facePose']],
  ['npcAnimationBinding', ['npcAnimationBinding']],
  ['trigger', ['trigger']],
  ['cameraPath', ['cameraPath']],
  ['skin', ['skin']],
  ['event', ['event']],
  ['localization', ['localization']]
]);

function resolveReference(index, kind, rawId) {
  const id = cleanId(rawId);
  if (!index || !kind || !id) {
    return undefined;
  }

  for (const candidateKind of relatedKinds(kind)) {
    const location = index.location(candidateKind, id);
    if (location) {
      return {
        requestedKind: kind,
        kind: candidateKind,
        id,
        location
      };
    }
  }
  return undefined;
}

function hasReference(index, kind, rawId) {
  return Boolean(resolveReference(index, kind, rawId));
}

function referenceIds(index, kind) {
  if (!index || !kind) {
    return [];
  }
  if (kind === 'npc' || kind === 'speaker') {
    return index.allNpcLikeIds();
  }
  if (kind === 'animation' || kind === 'faceAnimation') {
    return index.allAnimationIds();
  }
  return index.ids(kind);
}

function shortcutReference(rawValue) {
  const value = String(rawValue || '').trim();
  const separator = value.indexOf(':');
  if (separator <= 0) {
    return undefined;
  }
  const prefix = value.slice(0, separator).trim().toLowerCase();
  const id = value.slice(separator + 1).trim();
  const kind = SHORTCUT_REFERENCE_KIND.get(prefix);
  return kind && id ? { kind, id, prefix } : undefined;
}

function isReferenceShortcutPrefix(prefix) {
  return SHORTCUT_REFERENCE_KIND.has(String(prefix || '').trim().toLowerCase());
}

function relatedKinds(kind) {
  return RELATED_KINDS.get(kind) || [kind];
}

function cleanId(rawId) {
  const id = String(rawId || '').trim();
  return id && !id.includes('${') ? id : '';
}

module.exports = {
  resolveReference,
  hasReference,
  referenceIds,
  shortcutReference,
  isReferenceShortcutPrefix,
  relatedKinds
};
