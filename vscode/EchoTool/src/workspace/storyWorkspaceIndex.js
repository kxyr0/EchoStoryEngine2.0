'use strict';

const fs = require('fs/promises');
const path = require('path');
const { authoringKeyEntriesFromValue } = require('./authoringKeys');
const { localizationKeyEntriesFromValue } = require('./localizationKeys');

const CATEGORY_BY_DIRECTORY = new Map([
  ['events', 'events'],
  ['dialogues', 'dialogues'],
  ['scenes', 'scenes'],
  ['speakers', 'speakers'],
  ['npcs', 'npcs'],
  ['triggers', 'triggers'],
  ['animations', 'animations'],
  ['face_animations', 'faceAnimations'],
  ['camera_paths', 'cameraPaths'],
  ['localization', 'localizations'],
  ['localizations', 'localizations'],
  ['lang', 'localizations']
]);

const RESOURCE_SPECS = [
  { root: 'echo_scene/scenes', category: 'scenes', kind: 'scene', suffixes: ['.es'], idCalls: ['Scene', 'Id'] },
  { root: 'echo_scene/locations', category: 'locations', kind: 'location', suffixes: ['.json'], idCalls: ['Id'] },
  { root: 'echo_observation/observations', category: 'observations', kind: 'observation', suffixes: ['.es'], idCalls: ['Id'] },
  {
    root: 'echo_story/stories',
    category: 'stories',
    kind: 'story',
    suffixes: ['.es'],
    idCalls: ['Story', 'Id']
  },
  {
    root: 'echo_story/environments',
    category: 'environments',
    kind: 'environment',
    suffixes: ['.es'],
    idCalls: ['Environment', 'Id']
  },
  {
    root: 'echo_dialogue/dialogues',
    category: 'dialogues',
    kind: 'dialogue',
    suffixes: ['.dialogue.es'],
    idCalls: ['Id', 'Dialogue']
  },
  {
    root: 'echo_npc/npcs',
    category: 'npcs',
    kind: 'npc',
    suffixes: ['.npc.es'],
    idCalls: [{ call: 'Npc', named: 'id' }, 'Npc', 'Id']
  },
  {
    root: 'echo_npc/forms',
    category: 'npcForms',
    kind: 'npcForm',
    suffixes: ['.form.es', '.es'],
    idCalls: [{ call: 'Form', named: 'id' }, 'Form', 'Id']
  },
  {
    root: 'echo_npc_animation/bindings',
    category: 'npcAnimationBindings',
    kind: 'npcAnimationBinding',
    suffixes: ['.npc-animation-binding.es'],
    idCalls: ['NpcAnimationBinding', 'Id']
  },
  {
    root: 'echo_player/profile_templates',
    category: 'playerProfileTemplates',
    kind: 'playerProfileTemplate',
    suffixes: ['.player.es'],
    idCalls: [{ call: 'PlayerProfile', named: 'id' }, 'PlayerProfile', 'Id']
  },
  {
    root: 'echo_item/items',
    category: 'weapons',
    kind: 'weapon',
    suffixes: ['.weapon.es'],
    idCalls: [{ call: 'Weapon', named: 'id' }, 'Weapon', 'Id']
  },
  {
    root: 'echo_animation/animations',
    category: 'animations',
    kind: 'animation',
    suffixes: ['.animation.es'],
    idCalls: ['Animation', 'Id']
  },
  {
    root: 'echo_animation/face_animations',
    category: 'faceAnimations',
    kind: 'faceAnimation',
    suffixes: ['.face-animation.es'],
    idCalls: ['FacialAnimation', 'FaceAnimation', 'Id']
  },
  {
    root: 'echo_animation/animation_profiles',
    category: 'animationProfiles',
    kind: 'animationProfile',
    suffixes: ['.animation-profile.es'],
    idCalls: ['AnimationProfile', 'Id']
  },
  {
    root: 'echo_animation/animation_rigs',
    category: 'animationRigs',
    kind: 'animationRig',
    suffixes: ['.animation-rig.es'],
    idCalls: ['AnimationRig', 'Id']
  },
  {
    root: 'echo_animation/animation_clips',
    category: 'animationClips',
    kind: 'animationClip',
    suffixes: ['.animation-clip.es'],
    idCalls: ['AnimationClip', 'Id']
  },
  {
    root: 'echo_animation/face_rigs',
    category: 'faceRigs',
    kind: 'faceRig',
    suffixes: ['.face-rig.es'],
    idCalls: [{ pattern: /\bface\s+[A-Za-z_][A-Za-z0-9_]*\s*\(\s*(["'])((?:\\.|(?!\1)[\s\S])*)\1/g, group: 2 }, 'FaceRig', 'Id']
  },
  {
    root: 'echo_animation/face_poses',
    category: 'facePoses',
    kind: 'facePose',
    suffixes: ['.face-pose.es'],
    idCalls: ['FacePose', 'Id']
  },
  {
    root: 'echo_trigger/triggers',
    category: 'triggers',
    kind: 'trigger',
    suffixes: ['.trigger.es'],
    idCalls: ['Trigger', 'Id']
  }
];

const RESOURCE_SPEC_BY_CATEGORY = new Map(RESOURCE_SPECS.map(spec => [spec.category, spec]));

const KIND_BY_CATEGORY = {
  stories: 'story',
  environments: 'environment',
  events: 'event',
  dialogues: 'dialogue',
  scenes: 'scene',
  observations: 'observation',
  locations: 'location',
  speakers: 'speaker',
  npcs: 'npc',
  npcForms: 'npcForm',
  playerProfileTemplates: 'playerProfileTemplate',
  weapons: 'weapon',
  animations: 'animation',
  faceAnimations: 'faceAnimation',
  animationProfiles: 'animationProfile',
  animationRigs: 'animationRig',
  animationClips: 'animationClip',
  faceRigs: 'faceRig',
  facePoses: 'facePose',
  npcAnimationBindings: 'npcAnimationBinding',
  triggers: 'trigger',
  cameraPaths: 'cameraPath',
  localizations: 'localization'
};

class StoryWorkspaceIndex {
  constructor(configuration, parser) {
    this.configuration = configuration;
    this.parser = parser;
    this.index = createEmptyIndex();
    this.lastRefresh = 0;
    this.refreshing = false;
  }

  async refresh() {
    if (this.refreshing) {
      return this.index;
    }
    this.refreshing = true;
    try {
      const next = createEmptyIndex();
      next.modRoot = this.configuration.modRoot();
      next.storyDataDirectories = storyDataDirectories(this.configuration);
      next.storyDataDirectory = next.storyDataDirectories[0] || this.configuration.storyDataDirectory();
      await this.scanStoryData(next);
      await this.scanAssets(next);
      await this.scanTemplates(next);
      this.index = next;
      this.lastRefresh = Date.now();
      return this.index;
    } finally {
      this.refreshing = false;
    }
  }

  snapshot() {
    return this.index;
  }

  documentKind(filePath) {
    const normalized = normalizePath(filePath);
    for (const root of storyDataDirectories(this.configuration)) {
      const storyRoot = normalizePath(root);
      if (storyRoot && normalized.startsWith(`${storyRoot}/`)) {
        const category = categoryForStoryFile(storyRoot, normalized);
        return KIND_BY_CATEGORY[category] || 'generic';
      }
    }
    const resourceSpec = resourceSpecForNormalizedPath(normalized);
    if (resourceSpec) {
      return resourceSpec.kind;
    }
    if (normalized.includes('/echo_story/stories/')) {
      return 'story';
    }
    if (normalized.includes('/echo_story/environments/')) {
      return 'environment';
    }
    if (normalized.includes('/echo_dialogue/dialogues/')) {
      return 'dialogue';
    }
    if (normalized.includes('/echo_npc/npcs/')) {
      return 'npc';
    }
    if (normalized.includes('/echo_npc/forms/')) {
      return 'npcForm';
    }
    if (normalized.includes('/echo_npc_animation/bindings/')) {
      return 'npcAnimationBinding';
    }
    if (normalized.includes('/echo_player/profile_templates/')) {
      return 'playerProfileTemplate';
    }
    if (normalized.includes('/echo_item/items/')) {
      return 'weapon';
    }
    if (normalized.includes('/echo_animation/animations/')) {
      return 'animation';
    }
    if (normalized.includes('/echo_animation/face_animations/')) {
      return 'faceAnimation';
    }
    if (normalized.includes('/story/events/')) {
      return 'event';
    }
    if (normalized.includes('/story/dialogues/')) {
      return 'dialogue';
    }
    if (normalized.includes('/story/scenes/')) {
      return 'scene';
    }
    if (normalized.includes('/story/speakers/')) {
      return 'speaker';
    }
    if (normalized.includes('/story/npcs/')) {
      return 'npc';
    }
    if (normalized.includes('/story/animations/')) {
      return 'animation';
    }
    if (normalized.includes('/story/face_animations/')) {
      return 'faceAnimation';
    }
    if (normalized.includes('/story/camera_paths/')) {
      return 'cameraPath';
    }
    if (normalized.includes('/story/localization/')) {
      return 'localization';
    }
    if (normalized.includes('/story/localizations/') || normalized.includes('/story/lang/')) {
      return 'localization';
    }
    return 'generic';
  }

  ids(kind) {
    const key = collectionKey(kind);
    return key && this.index[key] ? Array.from(this.index[key].keys()).sort() : [];
  }

  has(kind, id) {
    const key = collectionKey(kind);
    if (!key || !id) {
      return false;
    }
    return Boolean(this.index[key] && this.index[key].has(String(id)));
  }

  location(kind, id) {
    const key = collectionKey(kind);
    return key && this.index[key] ? this.index[key].get(String(id)) : undefined;
  }

  entries(kind) {
    if (kind === 'localizationKey' || kind === 'localizationKeys') {
      return Array.from(this.index.localizationKeys.values()).sort(compareEntries);
    }
    if (kind === 'stateKey' || kind === 'stateKeys') {
      return Array.from(this.index.stateKeys.values()).sort(compareEntries);
    }
    if (kind === 'signalKey' || kind === 'signalKeys') {
      return Array.from(this.index.signalKeys.values()).sort(compareEntries);
    }
    if (kind) {
      const key = collectionKey(kind);
      return key && this.index[key] ? Array.from(this.index[key].values()).sort(compareEntries) : [];
    }
    return [
      ...this.index.events.values(),
      ...this.index.stories.values(),
      ...this.index.environments.values(),
      ...this.index.dialogues.values(),
      ...this.index.scenes.values(),
      ...this.index.speakers.values(),
      ...this.index.npcs.values(),
      ...this.index.npcForms.values(),
      ...this.index.playerProfileTemplates.values(),
      ...this.index.weapons.values(),
      ...this.index.animations.values(),
      ...this.index.faceAnimations.values(),
      ...this.index.animationProfiles.values(),
      ...this.index.animationRigs.values(),
      ...this.index.animationClips.values(),
      ...this.index.faceRigs.values(),
      ...this.index.facePoses.values(),
      ...this.index.npcAnimationBindings.values(),
      ...this.index.triggers.values(),
      ...this.index.cameraPaths.values(),
      ...this.index.localizations.values(),
      ...this.index.skins.values(),
      ...this.index.localizationKeys.values(),
      ...this.index.stateKeys.values(),
      ...this.index.signalKeys.values()
    ].sort(compareEntries);
  }

  duplicateIds(kind, id) {
    const key = collectionKey(kind);
    if (!key || !id) {
      return [];
    }
    return this.index.duplicates.get(`${key}:${id}`) || [];
  }

  allNpcLikeIds() {
    return unique([...this.ids('speaker'), ...this.ids('npc')]);
  }

  allAnimationIds() {
    return unique([...this.ids('animation'), ...this.ids('faceAnimation')]);
  }

  localizationKeys() {
    return Array.from(this.index.localizationKeys.keys()).sort();
  }

  hasLocalizationKey(key) {
    return Boolean(key && this.index.localizationKeys.has(String(key)));
  }

  localizationKeyLocation(key) {
    return this.index.localizationKeys.get(String(key));
  }

  stateKeys() {
    return Array.from(this.index.stateKeys.keys()).sort();
  }

  stateKeyLocation(key) {
    return this.index.stateKeys.get(String(key));
  }

  signalKeys() {
    return Array.from(this.index.signalKeys.keys()).sort();
  }

  signalKeyLocation(key) {
    return this.index.signalKeys.get(String(key));
  }

  summary() {
    const snapshot = this.index;
    return {
      modRoot: snapshot.modRoot,
      storyDataDirectory: snapshot.storyDataDirectory,
      storyDataDirectories: snapshot.storyDataDirectories,
      stories: snapshot.stories.size,
      environments: snapshot.environments.size,
      events: snapshot.events.size,
      dialogues: snapshot.dialogues.size,
      scenes: snapshot.scenes.size,
      speakers: snapshot.speakers.size,
      npcs: snapshot.npcs.size,
      npcForms: snapshot.npcForms.size,
      playerProfileTemplates: snapshot.playerProfileTemplates.size,
      weapons: snapshot.weapons.size,
      animations: snapshot.animations.size,
      faceAnimations: snapshot.faceAnimations.size,
      animationProfiles: snapshot.animationProfiles.size,
      animationRigs: snapshot.animationRigs.size,
      animationClips: snapshot.animationClips.size,
      faceRigs: snapshot.faceRigs.size,
      facePoses: snapshot.facePoses.size,
      npcAnimationBindings: snapshot.npcAnimationBindings.size,
      triggers: snapshot.triggers.size,
      cameraPaths: snapshot.cameraPaths.size,
      localizationKeys: snapshot.localizationKeys.size,
      stateKeys: snapshot.stateKeys.size,
      signalKeys: snapshot.signalKeys.size,
      templates: snapshot.templates.length,
      duplicateGroups: snapshot.duplicates.size,
      lastRefresh: this.lastRefresh
    };
  }

  async scanStoryData(next) {
    const roots = next.storyDataDirectories.length > 0 ? next.storyDataDirectories : [next.storyDataDirectory].filter(Boolean);
    if (roots.length === 0) {
      next.errors.push('Story data directory is not configured.');
      return;
    }

    for (const root of roots) {
      if (!root || !(await exists(root))) {
        next.errors.push(`Story data directory not found: ${root}`);
        continue;
      }

      const files = preferEchoScriptSiblings(await collectFiles(root, isStoryAuthoringFile));
      for (const file of files) {
        const category = categoryForStoryFile(root, file);
        if (!category) {
          continue;
        }
        if (file.toLowerCase().endsWith('.es')) {
          await this.addEchoScriptFile(next, category, file, root);
        } else {
          await this.addJson5File(next, category, file, root);
        }
      }
    }
  }

  async scanAssets(next) {
    const modRoot = next.modRoot;
    const roots = [];
    if (modRoot) {
      roots.push(path.join(modRoot, 'src', 'main', 'resources', 'assets', 'echostoryengine', 'story'));
    }
    for (const dataRoot of next.storyDataDirectories || []) {
      const resourcesRoot = path.dirname(dataRoot);
      roots.push(path.join(resourcesRoot, 'assets'));
    }

    const seen = new Set();
    for (const assetsRoot of roots.filter(Boolean)) {
      const normalizedRoot = path.normalize(assetsRoot);
      if (seen.has(normalizedRoot) || !(await exists(normalizedRoot))) {
        continue;
      }
      seen.add(normalizedRoot);
      await this.scanAssetsRoot(next, normalizedRoot);
    }
  }

  async scanAssetsRoot(next, assetsRoot) {
    if (!assetsRoot.endsWith(path.join('echostoryengine', 'story'))) {
      const files = await collectFiles(assetsRoot, file => isSkinOrPortraitTexture(assetsRoot, file));
      for (const file of files) {
        this.addTextureAsset(next, assetsRoot, file);
      }
      return;
    }
    const files = await collectFiles(assetsRoot, file => file.toLowerCase().endsWith('.json5'));
    for (const file of files) {
      const normalized = normalizePath(file);
      if (normalized.includes('/skins/')) {
        await this.addJson5File(next, 'skins', file);
      }
    }
  }

  addTextureAsset(next, assetsRoot, file) {
    const id = textureId(assetsRoot, file);
    if (!id || next.skins.has(id)) {
      return;
    }
    next.skins.set(id, {
      id,
      path: file,
      relativePath: relativeToMod(next.modRoot, file),
      kind: 'skin',
      value: undefined
    });
  }

  async scanTemplates(next) {
    const templatesRoot = this.configuration.templatesDirectory();
    if (!templatesRoot || !(await exists(templatesRoot))) {
      return;
    }
    const files = await collectFiles(templatesRoot, isTemplateFile);
    next.templates = files.map(file => ({
      label: path.basename(file, path.extname(file)),
      path: file,
      relativePath: normalizePath(path.relative(templatesRoot, file))
    })).sort((a, b) => a.relativePath.localeCompare(b.relativePath));
  }

  async addJson5File(next, category, file, dataRoot = next.storyDataDirectory) {
    const collection = collectionForCategory(next, category);
    if (!collection) {
      return;
    }

    let id = path.basename(file, path.extname(file));
    if (category === 'localizations') {
      id = localizationFileId(dataRoot, file);
    }
    let value;
    try {
      const text = await fs.readFile(file, 'utf8');
      const parsed = this.parser.parse(text, file);
      if (parsed.ok && parsed.value && typeof parsed.value === 'object' && !Array.isArray(parsed.value)) {
        value = parsed.value;
        if (category !== 'localizations' && typeof value.id === 'string' && value.id.trim()) {
          id = value.id.trim();
        }
      } else if (!parsed.ok) {
        next.errors.push(`${file}: ${parsed.error.message}`);
      }
    } catch (error) {
      next.errors.push(`${file}: ${error.message}`);
    }

    const entry = {
      id,
      path: file,
      relativePath: relativeToMod(next.modRoot, file),
      kind: KIND_BY_CATEGORY[category] || category,
      value
    };
    if (category === 'localizations' && value) {
      addLocalizationKeys(next, entry, value);
    }
    if (value) {
      addAuthoringKeys(next, entry, value);
    }
    if (collection.has(id)) {
      const duplicateKey = `${category}:${id}`;
      const existing = next.duplicates.get(duplicateKey) || [collection.get(id)];
      existing.push(entry);
      next.duplicates.set(duplicateKey, existing);
    }
    collection.set(id, entry);
  }

  async addEchoScriptFile(next, category, file, dataRoot = next.storyDataDirectory) {
    const collection = collectionForCategory(next, category);
    if (!collection) {
      return;
    }

    let id = defaultEchoScriptId(dataRoot, file, category);
    let text = '';
    try {
      text = await fs.readFile(file, 'utf8');
    } catch (error) {
      next.errors.push(`${file}: ${error.message}`);
    }

    const explicitId = echoScriptResourceId(text, category);
    if (category === 'localizations') {
      id = localizationFileId(dataRoot, file);
    } else if (explicitId) {
      id = explicitId;
    }

    const entry = {
      id,
      path: file,
      relativePath: relativeToMod(next.modRoot, file),
      kind: KIND_BY_CATEGORY[category] || category,
      value: undefined,
      echoscript: true
    };

    if (category === 'localizations') {
      const value = echoScriptLocalizationValue(text);
      entry.value = value;
      addLocalizationKeys(next, entry, value);
    }
    addEchoScriptAuthoringKeys(next, entry, text);

    if (collection.has(id)) {
      const duplicateKey = `${category}:${id}`;
      const existing = next.duplicates.get(duplicateKey) || [collection.get(id)];
      existing.push(entry);
      next.duplicates.set(duplicateKey, existing);
    }
    collection.set(id, entry);
    if (category === 'observations') {
      for (const match of text.matchAll(/\bObservation\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(/g)) {
        const local = match[1].replace(/([a-z0-9])([A-Z])/g, '$1_$2').toLowerCase();
        const pointId = `${id}#${local}`;
        collection.set(pointId, { ...entry, id: pointId });
      }
    }
  }
}

function createEmptyIndex() {
  return {
    modRoot: '',
    storyDataDirectory: '',
    storyDataDirectories: [],
    stories: new Map(),
    environments: new Map(),
    events: new Map(),
    dialogues: new Map(),
    scenes: new Map(),
    observations: new Map(),
    locations: new Map(),
    speakers: new Map(),
    npcs: new Map(),
    npcForms: new Map(),
    playerProfileTemplates: new Map(),
    weapons: new Map(),
    animations: new Map(),
    faceAnimations: new Map(),
    animationProfiles: new Map(),
    animationRigs: new Map(),
    animationClips: new Map(),
    faceRigs: new Map(),
    facePoses: new Map(),
    npcAnimationBindings: new Map(),
    triggers: new Map(),
    cameraPaths: new Map(),
    localizations: new Map(),
    localizationKeys: new Map(),
    stateKeys: new Map(),
    signalKeys: new Map(),
    skins: new Map(),
    templates: [],
    duplicates: new Map(),
    errors: []
  };
}

function categoryForStoryFile(root, file) {
  const relative = normalizePath(path.relative(root, file));
  const parts = relative.split('/').filter(Boolean);
  const first = parts[0];
  const legacy = CATEGORY_BY_DIRECTORY.get(first);
  if (legacy) {
    return legacy;
  }
  if (parts.length < 3) {
    return undefined;
  }
  const resourcePath = parts.slice(1).join('/');
  const spec = resourceSpecForRelativePath(resourcePath);
  return spec ? spec.category : undefined;
}

function resourceSpecForRelativePath(relativePath) {
  const normalized = normalizePath(relativePath);
  for (const spec of RESOURCE_SPECS) {
    if (!normalized.startsWith(`${spec.root}/`)) {
      continue;
    }
    const lower = normalized.toLowerCase();
    if (spec.suffixes.some(suffix => lower.endsWith(suffix))) {
      return spec;
    }
  }
  return undefined;
}

function resourceSpecForNormalizedPath(normalizedPath) {
  for (const spec of RESOURCE_SPECS) {
    if (normalizedPath.includes(`/${spec.root}/`)) {
      return spec;
    }
  }
  return undefined;
}

function collectionForCategory(index, category) {
  return index[category] instanceof Map ? index[category] : undefined;
}

function collectionKey(kind) {
  switch (kind) {
    case 'story':
    case 'stories':
      return 'stories';
    case 'environment':
    case 'environments':
      return 'environments';
    case 'event':
    case 'events':
      return 'events';
    case 'dialogue':
    case 'dialogues':
      return 'dialogues';
    case 'observation':
    case 'observations':
      return 'observations';
    case 'location':
    case 'locations':
      return 'locations';
    case 'scene':
    case 'scenes':
      return 'scenes';
    case 'speaker':
    case 'speakers':
      return 'speakers';
    case 'npc':
    case 'npcs':
      return 'npcs';
    case 'npcForm':
    case 'npcForms':
      return 'npcForms';
    case 'playerProfileTemplate':
    case 'playerProfileTemplates':
    case 'playerProfile':
    case 'playerProfiles':
      return 'playerProfileTemplates';
    case 'weapon':
    case 'weapons':
      return 'weapons';
    case 'animation':
    case 'animations':
      return 'animations';
    case 'faceAnimation':
    case 'faceAnimations':
      return 'faceAnimations';
    case 'animationProfile':
    case 'animationProfiles':
      return 'animationProfiles';
    case 'animationRig':
    case 'animationRigs':
      return 'animationRigs';
    case 'animationClip':
    case 'animationClips':
      return 'animationClips';
    case 'faceRig':
    case 'faceRigs':
      return 'faceRigs';
    case 'facePose':
    case 'facePoses':
      return 'facePoses';
    case 'npcAnimationBinding':
    case 'npcAnimationBindings':
      return 'npcAnimationBindings';
    case 'trigger':
    case 'triggers':
      return 'triggers';
    case 'cameraPath':
    case 'cameraPaths':
      return 'cameraPaths';
    case 'localization':
    case 'localizations':
      return 'localizations';
    case 'localizationKey':
    case 'localizationKeys':
      return 'localizationKeys';
    case 'stateKey':
    case 'stateKeys':
      return 'stateKeys';
    case 'signalKey':
    case 'signalKeys':
      return 'signalKeys';
    case 'skin':
    case 'skins':
      return 'skins';
    default:
      return undefined;
  }
}

async function collectFiles(root, predicate) {
  const result = [];
  const stack = [root];
  while (stack.length > 0) {
    const current = stack.pop();
    let entries;
    try {
      entries = await fs.readdir(current, { withFileTypes: true });
    } catch {
      continue;
    }
    for (const entry of entries) {
      const fullPath = path.join(current, entry.name);
      if (entry.isDirectory()) {
        stack.push(fullPath);
      } else if (entry.isFile() && predicate(fullPath)) {
        result.push(fullPath);
      }
    }
  }
  return result;
}

function storyDataDirectories(configuration) {
  if (configuration && typeof configuration.storyDataDirectories === 'function') {
    const roots = configuration.storyDataDirectories();
    if (Array.isArray(roots)) {
      return roots.map(root => path.normalize(root)).filter(Boolean);
    }
  }
  if (configuration && typeof configuration.storyDataDirectory === 'function') {
    const root = configuration.storyDataDirectory();
    return root ? [path.normalize(root)] : [];
  }
  return [];
}

function isStoryAuthoringFile(file) {
  const lower = file.toLowerCase();
  return lower.endsWith('.json5') || (lower.endsWith('.es') && !lower.endsWith('.backup.es'));
}

function isTemplateFile(file) {
  const lower = file.toLowerCase();
  return isStoryAuthoringFile(file)
    || lower.endsWith('.es.template')
    || lower.endsWith('.json5.template')
    || lower.endsWith('.template');
}

function isSkinOrPortraitTexture(assetsRoot, file) {
  const lower = normalizePath(path.relative(assetsRoot, file)).toLowerCase();
  return lower.endsWith('.png')
    && (lower.includes('/textures/echo_npc/skins/')
      || lower.includes('/textures/echo_dialogue/skins/')
      || lower.includes('/textures/echo_dialogue/portraits/'));
}

function textureId(assetsRoot, file) {
  const relative = normalizePath(path.relative(assetsRoot, file));
  const parts = relative.split('/').filter(Boolean);
  if (parts.length < 3 || parts[1] !== 'textures') {
    return '';
  }
  const namespace = parts[0];
  let texturePath = stripExtension(parts.slice(2).join('/'));
  for (const prefix of ['echo_npc/skins/', 'echo_dialogue/skins/', 'echo_dialogue/portraits/']) {
    if (texturePath.startsWith(prefix)) {
      texturePath = texturePath.slice(prefix.length);
      break;
    }
  }
  return namespace && texturePath ? `${namespace}:${texturePath}` : '';
}

function preferEchoScriptSiblings(files) {
  const echoScriptBases = new Set(
    files
      .filter(file => file.toLowerCase().endsWith('.es'))
      .map(file => normalizePath(stripExtension(file)).toLowerCase())
  );
  return files
    .filter(file => {
      const lower = file.toLowerCase();
      if (!lower.endsWith('.json5')) {
        return true;
      }
      return !echoScriptBases.has(normalizePath(stripExtension(file)).toLowerCase());
    })
    .sort((left, right) => normalizePath(left).localeCompare(normalizePath(right)));
}

async function exists(target) {
  try {
    await fs.stat(target);
    return true;
  } catch {
    return false;
  }
}

function relativeToMod(modRoot, file) {
  return modRoot ? normalizePath(path.relative(modRoot, file)) : normalizePath(file);
}

function normalizePath(value) {
  return String(value || '').replace(/\\/g, '/');
}

function stripExtension(value) {
  return value.replace(/\.[^.]+$/, '');
}

function defaultEchoScriptId(dataRoot, file, category) {
  const relative = normalizePath(path.relative(dataRoot, file));
  const parts = relative.split('/').filter(Boolean);
  const spec = RESOURCE_SPEC_BY_CATEGORY.get(category);
  if (spec && parts.length >= 3) {
    const namespace = parts[0];
    const resourcePath = parts.slice(1).join('/');
    const withoutRoot = resourcePath.startsWith(`${spec.root}/`)
      ? resourcePath.slice(spec.root.length + 1)
      : resourcePath;
    return `${namespace}:${stripKnownSuffix(withoutRoot, spec.suffixes)}`;
  }
  return stripKnownSuffix(path.basename(file), spec ? spec.suffixes : ['.es']);
}

function stripKnownSuffix(value, suffixes) {
  const text = normalizePath(value);
  const lower = text.toLowerCase();
  const suffix = (suffixes || [])
    .slice()
    .sort((left, right) => right.length - left.length)
    .find(candidate => lower.endsWith(candidate.toLowerCase()));
  if (suffix) {
    return text.slice(0, -suffix.length);
  }
  return stripExtension(text);
}

function localizationFileId(storyDataDirectory, file) {
  const relative = normalizePath(path.relative(storyDataDirectory, file));
  const parts = relative.split('/');
  return stripExtension(parts.slice(1).join('/')) || stripExtension(path.basename(file));
}

function addLocalizationKeys(index, localizationEntry, value) {
  for (const entry of localizationKeyEntriesFromValue(value, localizationEntry)) {
    if (!entry.key) {
      continue;
    }
    index.localizationKeys.set(entry.key, {
      id: entry.key,
      key: entry.key,
      fullKey: entry.fullKey,
      sourceKey: entry.sourceKey,
      locale: entry.locale,
      path: entry.path,
      relativePath: entry.relativePath,
      kind: 'localizationKey',
      preview: entry.preview
    });
  }
}

function addAuthoringKeys(index, sourceEntry, value) {
  const entries = authoringKeyEntriesFromValue(value, sourceEntry);
  for (const entry of entries.stateKeys) {
    index.stateKeys.set(entry.id, {
      ...entry,
      relativePath: sourceEntry.relativePath,
      path: sourceEntry.path
    });
  }
  for (const entry of entries.signalKeys) {
    index.signalKeys.set(entry.id, {
      ...entry,
      relativePath: sourceEntry.relativePath,
      path: sourceEntry.path
    });
  }
}

function addEchoScriptAuthoringKeys(index, sourceEntry, text) {
  for (const key of unique([
    ...stateKeysFromStateCalls(text),
    ...regexStringMatches(text, /\b(?:variable|stateKey|sourceKey|targetKey)\s*:\s*(["'])((?:\\.|(?!\1)[\s\S])*)\1/g, 2)
  ])) {
    addStateKey(index, sourceEntry, key);
  }
  for (const key of unique(regexStringMatches(text, /\bexists\s*\(\s*(["'])((?:\\.|(?!\1)[\s\S])*)\1/g, 2))) {
    addStateKey(index, sourceEntry, key);
  }
  const localStateRefs = echoScriptLocalStateRefs(text);
  for (const entry of localStateRefs.values()) {
    for (const key of unique([entry.readKey, entry.writeKey])) {
      addStateKey(index, sourceEntry, key);
    }
  }
  for (const symbol of unique([
    ...regexIdentifierMatches(text, /\b(?:Set|State\.(?:Set|Add|Adjust|Increment|Decrement|Remove|Clear)|exists|AreaEnter|AreaEnterDelayed|AreaExit)\s*\(\s*([A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)?)/g, 1),
    ...regexIdentifierMatches(text, /\b([A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)?)\b/g, 1)
  ])) {
    const localRef = localStateRefs.get(symbol);
    const keys = localRef ? unique([localRef.readKey, localRef.writeKey]) : echoScriptStateKeysForSymbol(symbol);
    for (const key of keys) {
      addStateKey(index, sourceEntry, key);
    }
  }

  const signalKeys = [
    ...regexStringMatches(text, /\b(?:Manual|ActionPerformed|PlayerJoin|AreaEnter|AreaEnterDelayed|AreaExit)\s*\(\s*(["'])((?:\\.|(?!\1)[\s\S])*)\1/g, 2),
    ...regexStringMatches(text, /\bon[A-Za-z0-9_]*Signal\s*:\s*(["'])((?:\\.|(?!\1)[\s\S])*)\1/g, 2)
  ];
  for (const key of unique(signalKeys)) {
    index.signalKeys.set(key, {
      id: key,
      key,
      kind: 'signalKey',
      path: sourceEntry.path,
      relativePath: sourceEntry.relativePath
    });
  }
}

function stateKeysFromStateCalls(text) {
  const result = [];
  const source = String(text || '');
  const pattern = /\b(Set|State\.(?:Set|Add|Adjust|Increment|Decrement|Remove|Clear))\s*\(/g;
  let match;
  while ((match = pattern.exec(source))) {
    const open = source.indexOf('(', match.index);
    const close = matchingParen(source, open);
    if (close <= open) {
      continue;
    }
    const body = source.slice(open + 1, close);
    const variable = firstNamedStringInFragment(body, 'variable');
    if (variable) {
      result.push(variable);
    } else {
      const first = firstStringInFragment(body);
      if (first) {
        result.push(first);
      }
    }
    pattern.lastIndex = close + 1;
  }
  return result;
}

function addStateKey(index, sourceEntry, key) {
  if (!key) {
    return;
  }
  index.stateKeys.set(key, {
    id: key,
    key,
    kind: 'stateKey',
    path: sourceEntry.path,
    relativePath: sourceEntry.relativePath
  });
}

function echoScriptStateKeysForSymbol(symbol) {
  const entry = ECHOSCRIPT_STATE_REFS.get(String(symbol || ''));
  if (!entry) {
    return [];
  }
  return unique([entry.readKey, entry.writeKey]);
}

function echoScriptLocalStateRefs(text) {
  const result = new Map();
  const pattern = /\bStateRef\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*State\.(Memory|Plot|World|Scene|Ref|Key)\s*\(\s*(["'])((?:\\.|(?!\3)[\s\S])*)\3\s*\)/g;
  for (const match of String(text || '').matchAll(pattern)) {
    result.set(match[1], echoScriptStateRefKeys(match[2], match[4] || ''));
  }
  return result;
}

function echoScriptStateRefKeys(kind, raw) {
  const normalizedKind = String(kind || '').toLowerCase();
  if (normalizedKind === 'ref' || normalizedKind === 'key') {
    return { readKey: raw, writeKey: raw };
  }
  const prefix = `${normalizedKind}.`;
  const path = raw.startsWith(prefix) ? raw.slice(prefix.length) : raw;
  if (normalizedKind === 'memory') {
    return { readKey: path, writeKey: `memory.${path}` };
  }
  return { readKey: `${normalizedKind}.${path}`, writeKey: `${normalizedKind}.${path}` };
}

const ECHOSCRIPT_STATE_REFS = new Map([
  ['OldFriend.Started', { readKey: 'chapter_1.j_old_friend_started', writeKey: 'memory.chapter_1.j_old_friend_started' }],
  ['OldFriend.MoneyGiven', { readKey: 'chapter_1.j_old_friend_money_given', writeKey: 'memory.chapter_1.j_old_friend_money_given' }],
  ['OldFriend.ItemReady', { readKey: 'chapter_1.j_old_friend_item_ready', writeKey: 'memory.chapter_1.j_old_friend_item_ready' }],
  ['OldFriend.Paid', { readKey: 'chapter_1.j_old_friend_paid', writeKey: 'memory.chapter_1.j_old_friend_paid' }],
  ['HomeEnter', { readKey: 'chapter_1.home.enter', writeKey: 'memory.chapter_1.home.enter' }],
  ['HomeRoom', { readKey: 'chapter_1.home.room', writeKey: 'memory.chapter_1.home.room' }],
  ['HomeRelaxExit', { readKey: 'chapter_1.home.relax.exit', writeKey: 'memory.chapter_1.home.relax.exit' }],
  ['StreetDone', { readKey: 'chapter_1.street_done', writeKey: 'memory.chapter_1.street_done' }],
  ['StreetGoToFriends', { readKey: 'chapter_1.street_go_to_friends', writeKey: 'memory.chapter_1.street_go_to_friends' }],
  ['HomeDone', { readKey: 'chapter_1.home_done', writeKey: 'memory.chapter_1.home_done' }],
  ['RoomDone', { readKey: 'chapter_1.room_done', writeKey: 'memory.chapter_1.room_done' }],
  ['JohnAskNoProblem', { readKey: 'chapter_1.john_ask_no_problem', writeKey: 'memory.chapter_1.john_ask_no_problem' }],
  ['JohnAskSheHaveProblems', { readKey: 'chapter_1.john_ask_she_have_problems', writeKey: 'memory.chapter_1.john_ask_she_have_problems' }],
  ['IntroAreaFogEnter', { readKey: 'intro.area.fog.enter', writeKey: 'memory.intro.area.fog.enter' }],
  ['IntroAreaFogExit', { readKey: 'intro.area.fog.exit', writeKey: 'memory.intro.area.fog.exit' }],
  ['IntroVideoDone', { readKey: 'intro_video.done', writeKey: 'memory.intro_video.done' }],
  ['DebugTestChoiceStarted', { readKey: 'debug.test_choice.started', writeKey: 'debug.test_choice.started' }],
  ['DebugTestChoiceCompleted', { readKey: 'debug.test_choice.completed', writeKey: 'debug.test_choice.completed' }],
  ['DebugTestChoiceFollow', { readKey: 'debug.test_choice.follow', writeKey: 'debug.test_choice.follow' }],
  ['DebugTestChoiceStay', { readKey: 'debug.test_choice.stay', writeKey: 'debug.test_choice.stay' }],
  ['DebugTestChoiceSashaLocked', { readKey: 'debug.test_choice.sasha_locked', writeKey: 'debug.test_choice.sasha_locked' }],
  ['chapter_1_home_enter', { readKey: 'chapter_1.home.enter', writeKey: 'memory.chapter_1.home.enter' }],
  ['chapter_1_home_room', { readKey: 'chapter_1.home.room', writeKey: 'memory.chapter_1.home.room' }],
  ['chapter_1_home_relax_exit', { readKey: 'chapter_1.home.relax.exit', writeKey: 'memory.chapter_1.home.relax.exit' }],
  ['street_done', { readKey: 'chapter_1.street_done', writeKey: 'memory.chapter_1.street_done' }],
  ['street_go_to_friends', { readKey: 'chapter_1.street_go_to_friends', writeKey: 'memory.chapter_1.street_go_to_friends' }],
  ['home_done', { readKey: 'chapter_1.home_done', writeKey: 'memory.chapter_1.home_done' }],
  ['room_done', { readKey: 'chapter_1.room_done', writeKey: 'memory.chapter_1.room_done' }],
  ['john_ask_no_problem', { readKey: 'chapter_1.john_ask_no_problem', writeKey: 'memory.chapter_1.john_ask_no_problem' }],
  ['john_ask_she_have_problems', { readKey: 'chapter_1.john_ask_she_have_problems', writeKey: 'memory.chapter_1.john_ask_she_have_problems' }],
  ['intro_area_fog_enter', { readKey: 'intro.area.fog.enter', writeKey: 'memory.intro.area.fog.enter' }],
  ['intro_area_fog_exit', { readKey: 'intro.area.fog.exit', writeKey: 'memory.intro.area.fog.exit' }],
  ['intro_video_done', { readKey: 'intro_video.done', writeKey: 'memory.intro_video.done' }],
  ['debug_test_choice_started', { readKey: 'debug.test_choice.started', writeKey: 'debug.test_choice.started' }],
  ['debug_test_choice_completed', { readKey: 'debug.test_choice.completed', writeKey: 'debug.test_choice.completed' }],
  ['debug_test_choice_follow', { readKey: 'debug.test_choice.follow', writeKey: 'debug.test_choice.follow' }],
  ['debug_test_choice_stay', { readKey: 'debug.test_choice.stay', writeKey: 'debug.test_choice.stay' }],
  ['debug_test_choice_sasha_locked', { readKey: 'debug.test_choice.sasha_locked', writeKey: 'debug.test_choice.sasha_locked' }]
]);

function echoScriptLocalizationValue(text) {
  const locale = firstEchoScriptStringCall(text, 'Locale') || '';
  const prefix = firstEchoScriptStringCall(text, 'Prefix') || '';
  const entries = {};
  const entryPattern = /\bEntry\s*\(\s*(["'])((?:\\.|(?!\1)[\s\S])*)\1\s*,\s*(["'])((?:\\.|(?!\3)[\s\S])*)\3/g;
  let match;
  while ((match = entryPattern.exec(text))) {
    entries[decodeEchoScriptString(match[2])] = decodeEchoScriptString(match[4]);
  }
  return {
    locale,
    default: /\.Default\s*\(/.test(text),
    prefix,
    entries
  };
}

function echoScriptResourceId(text, category) {
  const spec = RESOURCE_SPEC_BY_CATEGORY.get(category);
  const idCalls = spec ? spec.idCalls : ['Id'];
  for (const rule of idCalls) {
    if (typeof rule === 'string') {
      const value = firstEchoScriptStringCall(text, rule);
      if (value) {
        return value;
      }
      continue;
    }
    if (rule && rule.call && rule.named) {
      const value = firstEchoScriptNamedStringValueCall(text, rule.call, rule.named);
      if (value) {
        return value;
      }
      continue;
    }
    if (rule && rule.pattern) {
      const match = rule.pattern.exec(String(text || ''));
      rule.pattern.lastIndex = 0;
      if (match && match[rule.group]) {
        return decodeEchoScriptString(match[rule.group]);
      }
    }
  }
  return '';
}

function firstEchoScriptStringCall(text, callName) {
  const pattern = new RegExp(`\\b${escapeRegExp(callName)}\\s*\\(\\s*(["'])((?:\\\\.|(?!\\1)[\\s\\S])*)\\1`);
  const match = pattern.exec(text || '');
  return match ? decodeEchoScriptString(match[2]) : '';
}

function firstEchoScriptNamedStringCall(text, callName, argumentName) {
  const pattern = new RegExp(`\\b${escapeRegExp(callName)}\\s*\\([\\s\\S]*?\\b${escapeRegExp(argumentName)}\\s*:\\s*(["'])((?:\\\\.|(?!\\1)[\\s\\S])*)\\1`);
  const match = pattern.exec(text || '');
  return match ? decodeEchoScriptString(match[2]) : '';
}

function firstEchoScriptNamedStringValueCall(text, callName, argumentName) {
  const direct = firstEchoScriptNamedStringCall(text, callName, argumentName);
  if (direct) {
    return direct;
  }
  const source = String(text || '');
  const pattern = new RegExp(`\\b${escapeRegExp(callName)}\\s*\\(`, 'g');
  let match;
  while ((match = pattern.exec(source))) {
    const open = source.indexOf('(', match.index);
    const close = matchingParen(source, open);
    if (close <= open) {
      continue;
    }
    const body = source.slice(open + 1, close);
    const argument = namedArgumentValueFragment(body, argumentName);
    const resolved = resolveEchoScriptStringExpression(source, argument);
    if (resolved) {
      return resolved;
    }
    pattern.lastIndex = close + 1;
  }
  return '';
}

function namedArgumentValueFragment(body, argumentName) {
  const source = String(body || '');
  const pattern = new RegExp(`\\b${escapeRegExp(argumentName)}\\s*:`, 'g');
  let match;
  while ((match = pattern.exec(source))) {
    let start = match.index + match[0].length;
    start = skipEchoScriptTrivia(source, start, source.length);
    const end = topLevelArgumentEnd(source, start);
    return source.slice(start, end).trim();
  }
  return '';
}

function resolveEchoScriptStringExpression(source, expression, seen = new Set()) {
  const value = String(expression || '').trim();
  if (!value) {
    return '';
  }
  const literal = readEchoScriptStringLiteral(value, 0, value.length);
  if (literal && literal.next === value.length) {
    return literal.value;
  }
  const call = /^([A-Za-z_][A-Za-z0-9_]*)\s*\(\s*\)$/.exec(value);
  if (call) {
    return resolveEchoScriptZeroArgStringMethod(source, call[1], seen);
  }
  const identifier = /^([A-Za-z_][A-Za-z0-9_]*)$/.exec(value);
  if (identifier) {
    return resolveEchoScriptStringField(source, identifier[1], seen);
  }
  return '';
}

function resolveEchoScriptZeroArgStringMethod(source, methodName, seen = new Set()) {
  const key = `method:${methodName}`;
  if (!methodName || seen.has(key)) {
    return '';
  }
  seen.add(key);
  const pattern = new RegExp(`\\b${escapeRegExp(methodName)}\\s*\\(\\s*\\)\\s*\\{`, 'g');
  let match;
  while ((match = pattern.exec(source))) {
    const open = source.indexOf('{', match.index);
    const close = matchingBrace(source, open);
    if (close <= open) {
      continue;
    }
    const body = source.slice(open + 1, close);
    const returnMatch = /\breturn\s+([^;]+)\s*;/.exec(body);
    if (!returnMatch) {
      continue;
    }
    return resolveEchoScriptStringExpression(source, returnMatch[1], seen);
  }
  return '';
}

function resolveEchoScriptStringField(source, fieldName, seen = new Set()) {
  const key = `field:${fieldName}`;
  if (!fieldName || seen.has(key)) {
    return '';
  }
  seen.add(key);
  const pattern = new RegExp(`^\\s*(?:(?:private|public|static|readonly|const|override)\\s+)*(?:var|string|[A-Za-z_][A-Za-z0-9_.]*(?:\\[\\])?)\\s+${escapeRegExp(fieldName)}\\s*=\\s*([^;]+)\\s*;`, 'gm');
  let match;
  while ((match = pattern.exec(source))) {
    const resolved = resolveEchoScriptStringExpression(source, match[1], seen);
    if (resolved) {
      return resolved;
    }
  }
  return '';
}

function readEchoScriptStringLiteral(text, index, limit) {
  const quote = text[index];
  if (quote !== '"' && quote !== "'") {
    return undefined;
  }
  let cursor = index + 1;
  let value = '';
  while (cursor < limit) {
    const char = text[cursor];
    if (char === '\\') {
      if (cursor + 1 < limit) {
        value += decodeEchoScriptString(`\\${text[cursor + 1]}`);
        cursor += 2;
        continue;
      }
      break;
    }
    if (char === quote) {
      return {
        value,
        next: cursor + 1
      };
    }
    value += char;
    cursor += 1;
  }
  return undefined;
}

function topLevelArgumentEnd(text, start) {
  const source = String(text || '');
  let depth = 0;
  let quote = '';
  let escaped = false;
  for (let index = start; index < source.length; index += 1) {
    const char = source[index];
    if (quote) {
      if (escaped) {
        escaped = false;
      } else if (char === '\\') {
        escaped = true;
      } else if (char === quote) {
        quote = '';
      }
      continue;
    }
    if (char === '"' || char === "'") {
      quote = char;
      continue;
    }
    if (char === '(' || char === '[' || char === '{') {
      depth += 1;
      continue;
    }
    if (char === ')' || char === ']' || char === '}') {
      depth = Math.max(0, depth - 1);
      continue;
    }
    if (char === ',' && depth === 0) {
      return index;
    }
  }
  return source.length;
}

function skipEchoScriptTrivia(text, index, limit) {
  let cursor = index;
  while (cursor < limit && /\s/.test(text[cursor])) {
    cursor += 1;
  }
  return cursor;
}

function matchingBrace(text, open) {
  if (open < 0) {
    return -1;
  }
  const source = String(text || '');
  let depth = 0;
  let quote = '';
  let escaped = false;
  let lineComment = false;
  let blockComment = false;
  for (let index = open; index < source.length; index += 1) {
    const char = source[index];
    const next = source[index + 1];
    if (lineComment) {
      if (char === '\n') {
        lineComment = false;
      }
      continue;
    }
    if (blockComment) {
      if (char === '*' && next === '/') {
        blockComment = false;
        index += 1;
      }
      continue;
    }
    if (quote) {
      if (escaped) {
        escaped = false;
      } else if (char === '\\') {
        escaped = true;
      } else if (char === quote) {
        quote = '';
      }
      continue;
    }
    if (char === '/' && next === '/') {
      lineComment = true;
      index += 1;
      continue;
    }
    if (char === '/' && next === '*') {
      blockComment = true;
      index += 1;
      continue;
    }
    if (char === '"' || char === "'") {
      quote = char;
      continue;
    }
    if (char === '{') {
      depth += 1;
      continue;
    }
    if (char === '}') {
      depth -= 1;
      if (depth === 0) {
        return index;
      }
    }
  }
  return -1;
}

function regexStringMatches(text, pattern, groupIndex) {
  const result = [];
  let match;
  while ((match = pattern.exec(text || ''))) {
    result.push(decodeEchoScriptString(match[groupIndex]));
  }
  return result;
}

function regexIdentifierMatches(text, pattern, groupIndex) {
  const result = [];
  let match;
  while ((match = pattern.exec(text || ''))) {
    result.push(String(match[groupIndex] || ''));
  }
  return result;
}

function firstNamedStringInFragment(fragment, argumentName) {
  const pattern = new RegExp(`\\b${escapeRegExp(argumentName)}\\s*:\\s*(["'])((?:\\\\.|(?!\\1)[\\s\\S])*)\\1`);
  const match = pattern.exec(String(fragment || ''));
  return match ? decodeEchoScriptString(match[2]) : '';
}

function firstStringInFragment(fragment) {
  const match = /(["'])((?:\\.|(?!\1)[\s\S])*)\1/.exec(String(fragment || ''));
  return match ? decodeEchoScriptString(match[2]) : '';
}

function matchingParen(text, open) {
  if (open < 0) {
    return -1;
  }
  const source = String(text || '');
  let depth = 0;
  let quote = '';
  let escaped = false;
  let lineComment = false;
  let blockComment = false;
  for (let index = open; index < source.length; index += 1) {
    const char = source[index];
    const next = source[index + 1];
    if (lineComment) {
      if (char === '\n') {
        lineComment = false;
      }
      continue;
    }
    if (blockComment) {
      if (char === '*' && next === '/') {
        blockComment = false;
        index += 1;
      }
      continue;
    }
    if (quote) {
      if (escaped) {
        escaped = false;
      } else if (char === '\\') {
        escaped = true;
      } else if (char === quote) {
        quote = '';
      }
      continue;
    }
    if (char === '/' && next === '/') {
      lineComment = true;
      index += 1;
      continue;
    }
    if (char === '/' && next === '*') {
      blockComment = true;
      index += 1;
      continue;
    }
    if (char === '"' || char === "'") {
      quote = char;
      continue;
    }
    if (char === '(') {
      depth += 1;
      continue;
    }
    if (char === ')') {
      depth -= 1;
      if (depth === 0) {
        return index;
      }
    }
  }
  return -1;
}

function decodeEchoScriptString(value) {
  return String(value || '').replace(/\\(["'\\nrt])/g, (full, escaped) => {
    switch (escaped) {
      case 'n':
        return '\n';
      case 'r':
        return '\r';
      case 't':
        return '\t';
      case '"':
      case "'":
      case '\\':
        return escaped;
      default:
        return full;
    }
  });
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function unique(values) {
  return Array.from(new Set(values.filter(Boolean))).sort();
}

function compareEntries(left, right) {
  return `${left.kind}:${left.id}`.localeCompare(`${right.kind}:${right.id}`);
}

module.exports = {
  StoryWorkspaceIndex
};
