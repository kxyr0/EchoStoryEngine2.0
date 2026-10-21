'use strict';

function echoScriptDefinitionTarget(text, offset) {
  const string = stringAtOffset(text, offset);
  if (string) {
    const node = nodeIdRangeForValue(text, string.value);
    if (isLocalNodeReferenceContext(text, string.range.start) && node) {
      return { type: 'local', id: string.value, range: node.range };
    }
    const declaration = resourceDeclarationForStringContext(text, string.range.start - 1, string.value);
    if (declaration) {
      return { type: 'reference', declaration: true, ...declaration };
    }
    const reference = referenceForStringContext(text, string.range.start - 1, string.value);
    return reference ? { type: 'reference', ...reference } : undefined;
  }

  const word = wordAtOffset(text, offset);
  if (!word) {
    return undefined;
  }

  const method = methodDeclarations(text).find(entry => entry.name === word.value);
  if (method) {
    return { type: 'local', id: word.value, range: method.range };
  }

  const state = stateReferenceForIdentifierContext(text, word.range.start, word.value);
  if (state) {
    return { type: 'reference', kind: 'stateKey', ...state };
  }

  const localValue = valueDeclarations(text).find(entry => entry.name === word.value);
  if (localValue && (offset < localValue.range.start || offset > localValue.range.end)) {
    return { type: 'local', id: word.value, range: localValue.range };
  }

  const resource = referenceForIdentifierContext(text, word.range.start, word.value);
  if (resource) {
    return { type: 'reference', ...resource };
  }

  const classDeclaration = classDeclarations(text).find(entry => entry.name === word.value);
  return classDeclaration ? { type: 'local', id: word.value, range: classDeclaration.range } : undefined;
}

function echoScriptRenameTarget(text, offset) {
  const string = stringAtOffset(text, offset);
  if (string && isLocalNodeReferenceContext(text, string.range.start)) {
    return {
      kind: 'node',
      id: string.value,
      range: string.range,
      ranges: nodeIdRanges(text, string.value)
    };
  }
  if (string) {
    const declaration = resourceDeclarationForStringContext(text, string.range.start - 1, string.value);
    if (declaration) {
      return {
        kind: 'reference',
        referenceKind: declaration.kind,
        id: declaration.id,
        range: string.range,
        scope: 'workspaceReference'
      };
    }
    const reference = referenceForStringContext(text, string.range.start - 1, string.value);
    if (reference) {
      return {
        kind: 'reference',
        referenceKind: reference.kind,
        id: reference.id,
        range: string.range,
        scope: 'workspaceReference'
      };
    }
  }

  const word = wordAtOffset(text, offset);
  if (!word || RESERVED_RENAME_WORDS.has(word.value)) {
    return undefined;
  }
  const declarations = [
    ...methodDeclarations(text),
    ...classDeclarations(text),
    ...valueDeclarations(text)
  ];
  const declared = declarations.some(entry => entry.name === word.value);
  if (!declared && typedReferenceContext(text, word.range.start, word.value)) {
    const reference = referenceForIdentifierContext(text, word.range.start, word.value);
    return {
      kind: 'identifier',
      id: word.value,
      range: word.range,
      ranges: identifierRanges(text, word.value),
      scope: 'workspaceIdentifier',
      referenceKind: reference ? reference.kind : undefined,
      referenceId: reference ? reference.id : undefined
    };
  }
  if (!declared && !methodCallLikeContext(text, word.range.start, word.value)) {
    return undefined;
  }
  return {
    kind: 'identifier',
    id: word.value,
    range: word.range,
    ranges: identifierRanges(text, word.value)
  };
}

function isValidEchoScriptIdentifier(value) {
  return /^[A-Za-z_][A-Za-z0-9_]*$/.test(String(value || ''));
}

function isValidEchoScriptNodeId(value) {
  return /^[A-Za-z0-9_.:-]+$/.test(String(value || ''));
}

function methodDeclarations(text) {
  const result = [];
  const pattern = /^\s*(?:start\s+)?(?:(?:public|private|static|override)\s+)*[A-Za-z_][A-Za-z0-9_.]*(?:\[\])?\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(/gm;
  for (const match of String(text || '').matchAll(pattern)) {
    const start = match.index + match[0].lastIndexOf(match[1]);
    result.push({ name: match[1], range: { start, end: start + match[1].length } });
  }
  return result;
}

function classDeclarations(text) {
  const result = [];
  const pattern = /\bclass\s+([A-Za-z_][A-Za-z0-9_]*)\b/g;
  for (const match of String(text || '').matchAll(pattern)) {
    const start = match.index + match[0].lastIndexOf(match[1]);
    result.push({ name: match[1], range: { start, end: start + match[1].length } });
  }
  return result;
}

function valueDeclarations(text) {
  const result = [];
  const pattern = /^\s*(?:(?:public|private|static)\s+)*(?:var|[A-Za-z_][A-Za-z0-9_.]*(?:\[\])?)\s+([A-Za-z_][A-Za-z0-9_]*)\s*(?:=|;)/gm;
  for (const match of String(text || '').matchAll(pattern)) {
    const start = match.index + match[0].lastIndexOf(match[1]);
    result.push({ name: match[1], range: { start, end: start + match[1].length } });
  }
  return result;
}

function nodeIdRanges(text, id) {
  const result = [];
  for (const range of callStringArgumentRanges(text, ['Node'], 0)) {
    if (range.value === id) {
      result.push(range.range);
    }
  }
  for (const range of callStringArgumentRanges(text, ['Start', 'NextNode', 'Next', 'Goto'], 0)) {
    if (range.value === id) {
      result.push(range.range);
    }
  }
  for (const range of callStringArgumentRanges(text, ['Choice'], 2)) {
    if (range.value === id) {
      result.push(range.range);
    }
  }
  return dedupeRanges(result);
}

function nodeIdRangeForValue(text, id) {
  return callStringArgumentRanges(text, ['Node'], 0).find(entry => entry.value === id);
}

function callStringArgumentRanges(text, names, argumentIndex) {
  const source = String(text || '');
  const result = [];
  const namePattern = names.map(escapeRegExp).join('|');
  const pattern = new RegExp(`(?:\\.|\\b)(${namePattern})\\s*\\(`, 'g');
  let match;
  while ((match = pattern.exec(source))) {
    const open = source.indexOf('(', match.index);
    const close = matchingParen(source, open);
    if (close <= open) {
      continue;
    }
    const args = topLevelStringArguments(source, open + 1, close);
    if (args[argumentIndex]) {
      result.push(args[argumentIndex]);
    }
  }
  return result;
}

function topLevelStringArguments(text, start, end) {
  const result = [];
  let index = start;
  let argument = 0;
  while (index < end) {
    index = skipWhitespaceAndComments(text, index, end);
    const named = /^[A-Za-z_][A-Za-z0-9_]*\s*:/.exec(text.slice(index, end));
    if (named) {
      index += named[0].length;
      index = skipWhitespaceAndComments(text, index, end);
    }
    const string = readString(text, index, end);
    if (string) {
      result[argument] = {
        value: string.value,
        range: string.range
      };
      index = string.next;
    } else {
      index = skipValue(text, index, end);
    }
    index = skipWhitespaceAndComments(text, index, end);
    if (text[index] === ',') {
      argument += 1;
      index += 1;
    }
  }
  return result;
}

function referenceForStringContext(text, offset, value) {
  const before = contextBefore(text, offset, 320);
  const typedAssignmentKind = referenceKindForTypedStringAssignment(before);
  if (typedAssignmentKind) {
    return { kind: typedAssignmentKind, id: value };
  }
  const namedArgumentKind = referenceKindForNamedStringArgument(before);
  if (namedArgumentKind) {
    return { kind: namedArgumentKind, id: value };
  }
  if (/(?:Skin|SkinHead|Portrait)\s*\(\s*$/.test(before)) {
    return { kind: 'skin', id: value };
  }
  if (/DefaultForm\s*\(\s*$/.test(before)) {
    return { kind: 'npcForm', id: value };
  }
  if (/\.Alias\s*\(\s*(['"])[^'"]+\1\s*,\s*$/.test(before)) {
    return { kind: 'speaker', id: value };
  }
  if (/(?:\.|\b)(?:SetAnimationFace|SetFaceAnimation|Face)\s*\(\s*$/.test(before)) {
    return { kind: 'faceAnimation', id: value };
  }
  if (/(?:\.|\b)(?:SetAnimation|Anim|Animation)\s*\(\s*$/.test(before)) {
    return { kind: 'animation', id: value };
  }
  if (/(?:NpcPath|Monologue\.Show|Vehicle\.(?:Trip|Sequence)|Npc\.(?:Spawn|SpawnAt|SleepAt|Despawn|SpawnNearPlayer|Stay|Stop|Interact|StopInteraction|Path|Move|MoveTo|WalkTo|ApproachPlayer|MoveToPlayer|WalkToPlayer|Rotate|Turn|UseBlock|InteractBlock|OpenDoor|CloseDoor|ToggleDoor|UseDoor|LookAtPlayers|ClearLookAtPlayers|HoldItem|ClearHeldItem|PlayAnimation|StopAnimation|PlayFaceAnimation|StopFaceAnimation|EnterVehicle|EnterNearestVehicle|EnterVehicleAt|LeaveVehicle))\s*\(\s*$/.test(before)) {
    return { kind: 'npc', id: value };
  }
  if (/Environment\.Set\s*\(\s*$/.test(before)) {
    return { kind: 'environment', id: value };
  }
  if (/Scene\.Start\s*\(\s*$/.test(before)) {
    return { kind: 'scene', id: value };
  }
  if (/(?:StartStory|Story\.Load)\s*\(\s*$/.test(before)) {
    return { kind: 'story', id: value };
  }
  if (/(?:Dialogue\.(?:Start|Load))\s*\(\s*$/.test(before) || /thenDialogue\s*:\s*$/.test(before)) {
    return { kind: 'dialogue', id: value };
  }
  if (/(?:cinematic|cameraPath|cameraPathId|cinematicPathId)\s*:\s*$/.test(before)) {
    return { kind: 'cameraPath', id: value };
  }
  return undefined;
}

function referenceForIdentifierContext(text, offset, value) {
  const before = contextBefore(text, offset, 320);
  const namedArgumentKind = referenceKindForNamedIdentifierArgument(before);
  if (namedArgumentKind) {
    return { kind: namedArgumentKind, id: pascalToSnake(value) };
  }
  if (/(?:\.|\b)(?:SetAnimationFace|SetFaceAnimation|Face)\s*\(\s*(?:FaceAnimations\.)?$/.test(before)) {
    return { kind: 'faceAnimation', id: pascalToSnake(value) };
  }
  if (/(?:\.|\b)(?:SetAnimation|Anim|Animation)\s*\(\s*(?:Animations\.)?$/.test(before)) {
    return { kind: 'animation', id: pascalToSnake(value) };
  }
  if (/(?:Skin|SkinHead|Portrait)\s*\(\s*(?:Skins\.)?$/.test(before)) {
    return { kind: 'skin', id: pascalToSnake(value) };
  }
  if (/DefaultForm\s*\(\s*(?:NpcForms\.)?$/.test(before)) {
    return { kind: 'npcForm', id: pascalToSnake(value) };
  }
  if (/Scene\.Start\s*\(\s*(?:Scenes\.)?$/.test(before)) {
    return { kind: 'scene', id: pascalToSnake(value) };
  }
  if (/(?:StartStory|Story\.Load)\s*\(\s*(?:Stories\.)?$/.test(before)) {
    return { kind: 'story', id: pascalToSnake(value) };
  }
  if (/(?:Dialogue\.(?:Start|Load))\s*\(\s*(?:Dialogues\.)?$/.test(before) || /thenDialogue\s*:\s*(?:Dialogues\.)?$/.test(before)) {
    return { kind: 'dialogue', id: pascalToSnake(value) };
  }
  if (/(?:NpcPath|Monologue\.Show|Vehicle\.(?:Trip|Sequence)|Npc\.(?:Spawn|SpawnAt|SleepAt|Despawn|SpawnNearPlayer|Stay|Stop|Interact|StopInteraction|Path|Move|MoveTo|WalkTo|ApproachPlayer|MoveToPlayer|WalkToPlayer|Rotate|Turn|UseBlock|InteractBlock|OpenDoor|CloseDoor|ToggleDoor|UseDoor|LookAtPlayers|ClearLookAtPlayers|HoldItem|ClearHeldItem|PlayAnimation|StopAnimation|PlayFaceAnimation|StopFaceAnimation|EnterVehicle|EnterNearestVehicle|EnterVehicleAt|LeaveVehicle))\s*\(\s*$/.test(before)) {
    return { kind: 'npc', id: pascalToSnake(value) };
  }
  if (/Environment\.Set\s*\(\s*(?:Environments\.)?$/.test(before)) {
    return { kind: 'environment', id: pascalToSnake(value) };
  }
  if (/(?:cinematic|cameraPath|cameraPathId|cinematicPathId)\s*:\s*(?:CameraPaths\.)?$/.test(before)) {
    return { kind: 'cameraPath', id: pascalToSnake(value) };
  }
  return undefined;
}

function referenceKindForNamedStringArgument(before) {
  const match = /\b([A-Za-z_][A-Za-z0-9_]*)\s*:\s*$/.exec(before);
  return match ? NAMED_REFERENCE_KINDS[match[1]] : undefined;
}

function referenceKindForNamedIdentifierArgument(before) {
  const match = /\b([A-Za-z_][A-Za-z0-9_]*)\s*:\s*(?:[A-Za-z_][A-Za-z0-9_]*\.)?$/.exec(before);
  return match ? NAMED_REFERENCE_KINDS[match[1]] : undefined;
}

function referenceKindForTypedStringAssignment(before) {
  const match = /\b([A-Za-z_][A-Za-z0-9_]*)\s+[A-Za-z_][A-Za-z0-9_]*\s*=\s*$/.exec(before);
  return match ? TYPE_REFERENCE_KINDS[match[1]] : undefined;
}

function stateReferenceForIdentifierContext(text, offset, value) {
  const entry = localStateRefs(text).get(String(value || '')) || ECHOSCRIPT_STATE_REFS.get(String(value || ''));
  if (!entry) {
    return undefined;
  }
  const before = contextBefore(text, offset, 160);
  const after = contextAfter(text, offset + String(value || '').length, 20);
  if (/(?:Set|State\.(?:Set|Add|Adjust|Increment|Decrement|Remove|Clear))\s*\(\s*$/.test(before)
      || /^\s*(?:\+\+|--|\+=|-=)/.test(after)) {
    return {
      id: entry.writeKey,
      ids: [entry.writeKey, entry.readKey],
      symbol: value
    };
  }
  if (/(?:exists|AreaEnter|AreaEnterDelayed|AreaExit)\s*\(\s*$/.test(before)
      || /(?:if|else\s+if)\s*\([^()]*$/.test(before)
      || /(?:&&|\|\||!)\s*$/.test(before)) {
    return {
      id: entry.readKey,
      ids: [entry.readKey, entry.writeKey],
      symbol: value
    };
  }
  return undefined;
}

function localStateRefs(text) {
  const source = String(text || '');
  const result = new Map();
  const pattern = /\bStateRef\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*State\.(Memory|Plot|World|Scene|Ref|Key)\s*\(\s*(['"])(.*?)\3\s*\)/g;
  for (const match of source.matchAll(pattern)) {
    const name = match[1];
    const kind = match[2];
    const raw = match[4] || '';
    result.set(name, stateRefKeys(kind, raw));
  }
  return result;
}

function stateRefKeys(kind, raw) {
  const normalizedKind = String(kind || '').toLowerCase();
  if (normalizedKind === 'ref' || normalizedKind === 'key') {
    return { readKey: raw, writeKey: raw };
  }
  const namespace = normalizedKind;
  const prefix = `${namespace}.`;
  const path = raw.startsWith(prefix) ? raw.slice(prefix.length) : raw;
  if (namespace === 'memory') {
    return { readKey: path, writeKey: `memory.${path}` };
  }
  return { readKey: `${namespace}.${path}`, writeKey: `${namespace}.${path}` };
}

function echoScriptReferenceRenameEditsInText(text, query, newId) {
  const result = [];
  const source = String(text || '');
  for (const reference of echoScriptStringReferences(source)) {
    if (reference.id === query.id && referenceKindsOverlap(reference.kind, query.kind)) {
      result.push({
        range: reference.range,
        newText: newId,
        source: 'echoscript-string-reference'
      });
    }
  }
  for (const declaration of echoScriptDeclarationRangesInText(source, query)) {
    result.push({
      range: { start: declaration.offsetStart, end: declaration.offsetEnd },
      newText: newId,
      source: declaration.source
    });
  }
  return dedupeRangeEdits(result);
}

function echoScriptIdentifierRenameEditsInText(text, name, newName) {
  return identifierRanges(text, name).map(range => ({
    range,
    newText: newName,
    source: 'echoscript-identifier'
  }));
}

function echoScriptStringReferences(text) {
  const result = [];
  const source = String(text || '');
  for (let index = 0; index < source.length; index += 1) {
    const char = source[index];
    if (char !== '"' && char !== "'") {
      continue;
    }
    if (isOffsetInTrivia(source, index)) {
      continue;
    }
    const string = readString(source, index, source.length);
    if (!string) {
      continue;
    }
    const reference = referenceForStringContext(source, string.range.start - 1, string.value);
    if (reference) {
      result.push({
        ...reference,
        range: string.range
      });
    }
    index = string.next - 1;
  }
  return result;
}

function echoScriptReferenceQueryAtOffset(text, offset) {
  const target = echoScriptDefinitionTarget(text, offset);
  if (!target || target.type !== 'reference' || !target.kind || !target.id) {
    return undefined;
  }
  return {
    kind: target.kind,
    id: target.id,
    ids: target.ids,
    sourceKind: target.kind
  };
}

function echoScriptReferenceOccurrencesInText(text) {
  const source = String(text || '');
  const occurrences = [
    ...echoScriptStringReferences(source).map(reference => ({
      ...reference,
      source: 'echoscript-string-reference'
    })),
    ...echoScriptIdentifierReferences(source)
  ];
  return dedupeOffsetOccurrences(occurrences)
    .map(reference => ({
      ...reference,
      ...lineRangeFromOffsets(source, reference.range)
    }));
}

function echoScriptReferenceRangesInText(text, query, documentKind, includeDeclaration) {
  const source = String(text || '');
  const ranges = echoScriptReferenceOccurrencesInText(source)
    .filter(occurrence => occurrenceMatchesQuery(occurrence, query))
    .map(occurrence => ({
      kind: occurrence.kind,
      id: occurrence.id,
      line: occurrence.line,
      start: occurrence.start,
      end: occurrence.end,
      source: occurrence.source
    }));
  if (includeDeclaration && referenceKindsOverlap(documentKind, query.kind)) {
    ranges.push(...echoScriptDeclarationRangesInText(source, query.id));
  }
  return dedupeLineRanges(ranges);
}

function echoScriptIdentifierReferences(text) {
  const result = [];
  const source = String(text || '');
  for (const match of source.matchAll(/\b[A-Za-z_][A-Za-z0-9_]*\b/g)) {
    const start = match.index || 0;
    const value = match[0];
    if (isOffsetInTrivia(source, start)) {
      continue;
    }
    const resource = referenceForIdentifierContext(source, start, value);
    if (resource) {
      result.push({
        ...resource,
        range: { start, end: start + value.length },
        source: 'echoscript-identifier-reference'
      });
    }
    const state = stateReferenceForIdentifierContext(source, start, value);
    if (state) {
      result.push({
        kind: 'stateKey',
        ...state,
        range: { start, end: start + value.length },
        source: 'echoscript-state-reference'
      });
    }
  }
  return result;
}

function echoScriptDeclarationRangesInText(text, id) {
  const query = typeof id === 'string' ? { id, kind: '' } : id;
  return resourceDeclarationRangesInText(text)
    .filter(entry => entry.id === query.id && (!query.kind || referenceKindsOverlap(entry.kind, query.kind)))
    .map(entry => ({
      kind: '',
      id: entry.id,
      offsetStart: entry.range.start,
      offsetEnd: entry.range.end,
      ...lineRangeFromOffsets(text, entry.range),
      source: entry.source
    }));
}

function resourceDeclarationForStringContext(text, offset, value) {
  const before = contextBefore(text, offset, 320);
  for (const [call, kind] of RESOURCE_DECLARATION_CALL_KINDS) {
    if (new RegExp(`(?:\\.|\\b)${escapeRegExp(call)}\\s*\\(\\s*$`).test(before)) {
      return { kind, id: value };
    }
  }
  if (/\bNpc\s*\([^()]*\bid\s*:\s*$/.test(before)) {
    return { kind: 'npc', id: value };
  }
  if (/\bForm\s*\([^()]*\bid\s*:\s*$/.test(before)) {
    return { kind: 'npcForm', id: value };
  }
  if (/\bPlayerProfile\s*\([^()]*\bid\s*:\s*$/.test(before)) {
    return { kind: 'playerProfileTemplate', id: value };
  }
  if (/\bWeapon\s*\([^()]*\bid\s*:\s*$/.test(before)) {
    return { kind: 'weapon', id: value };
  }
  if (/\bId\s*\(\s*$/.test(before)) {
    const kind = resourceKindForText(text, offset);
    return kind ? { kind, id: value } : undefined;
  }
  if (/\bface\s+[A-Za-z_][A-Za-z0-9_]*\s*\(\s*$/.test(before)) {
    return { kind: 'faceRig', id: value };
  }
  return undefined;
}

function resourceDeclarationRangesInText(text) {
  const source = String(text || '');
  const result = [];
  for (const [call, kind] of RESOURCE_DECLARATION_CALL_KINDS) {
    for (const entry of callStringArgumentRanges(source, [call], 0)) {
      result.push({
        id: entry.value,
        kind,
        range: entry.range,
        source: `echoscript-${kind}-declaration`
      });
    }
  }
  for (const entry of callNamedStringArgumentRanges(source, ['Npc'], 'id')) {
    result.push({
      id: entry.value,
      kind: 'npc',
      range: entry.range,
      source: 'echoscript-npc-declaration'
    });
  }
  for (const entry of callNamedStringArgumentRanges(source, ['Form'], 'id')) {
    result.push({
      id: entry.value,
      kind: 'npcForm',
      range: entry.range,
      source: 'echoscript-npc-form-declaration'
    });
  }
  for (const entry of callNamedStringArgumentRanges(source, ['PlayerProfile'], 'id')) {
    result.push({
      id: entry.value,
      kind: 'playerProfileTemplate',
      range: entry.range,
      source: 'echoscript-player-profile-template-declaration'
    });
  }
  for (const entry of callNamedStringArgumentRanges(source, ['Weapon'], 'id')) {
    result.push({
      id: entry.value,
      kind: 'weapon',
      range: entry.range,
      source: 'echoscript-weapon-declaration'
    });
  }
  for (const entry of callStringArgumentRanges(source, ['Id'], 0)) {
    const idKind = resourceKindForText(source, entry.range.start);
    if (idKind) {
      result.push({
        id: entry.value,
        kind: idKind,
        range: entry.range,
        source: 'echoscript-id-declaration'
      });
    }
  }
  const facePattern = /\bface\s+[A-Za-z_][A-Za-z0-9_]*\s*\(\s*(['"])((?:\\.|(?!\1)[\s\S])*)\1/g;
  for (const match of source.matchAll(facePattern)) {
    const start = (match.index || 0) + match[0].lastIndexOf(match[2]);
    result.push({
      id: readString(source, start - 1, source.length)?.value || match[2],
      kind: 'faceRig',
      range: { start, end: start + match[2].length },
      source: 'echoscript-face-rig-declaration'
    });
  }
  return dedupeDeclarationRanges(result);
}

function callNamedStringArgumentRanges(text, names, argumentName) {
  const source = String(text || '');
  const result = [];
  const namePattern = names.map(escapeRegExp).join('|');
  const pattern = new RegExp(`(?:\\.|\\b)(${namePattern})\\s*\\(`, 'g');
  let match;
  while ((match = pattern.exec(source))) {
    const open = source.indexOf('(', match.index);
    const close = matchingParen(source, open);
    if (close <= open) {
      continue;
    }
    result.push(...topLevelNamedStringArguments(source, open + 1, close, argumentName));
  }
  return result;
}

function topLevelNamedStringArguments(text, start, end, argumentName) {
  const result = [];
  let index = start;
  while (index < end) {
    index = skipWhitespaceAndComments(text, index, end);
    const named = /^([A-Za-z_][A-Za-z0-9_]*)\s*:/.exec(text.slice(index, end));
    if (named) {
      index += named[0].length;
      index = skipWhitespaceAndComments(text, index, end);
      const string = readString(text, index, end);
      if (string && named[1] === argumentName) {
        result.push({
          value: string.value,
          range: string.range
        });
      }
      index = string ? string.next : skipValue(text, index, end);
    } else {
      index = skipValue(text, index, end);
    }
    index = skipWhitespaceAndComments(text, index, end);
    if (text[index] === ',') {
      index += 1;
    }
  }
  return result;
}

function resourceKindForText(text, offset = 0) {
  const source = String(text || '');
  const safeOffset = Math.max(0, Math.min(offset || 0, source.length));
  let nearest = '';
  for (const match of source.matchAll(/\bclass\s+[A-Za-z_][A-Za-z0-9_]*\s*:\s*([A-Za-z_][A-Za-z0-9_]*)\b/g)) {
    if ((match.index || 0) > safeOffset) {
      break;
    }
    nearest = match[1];
  }
  return RESOURCE_KIND_BY_BASE_TYPE[nearest] || '';
}

function occurrenceMatchesQuery(occurrence, query) {
  if (!occurrence || !query || !referenceKindsOverlap(occurrence.kind, query.kind)) {
    return false;
  }
  const occurrenceIds = [occurrence.id, ...(Array.isArray(occurrence.ids) ? occurrence.ids : [])].filter(Boolean);
  const queryIds = [query.id, ...(Array.isArray(query.ids) ? query.ids : [])].filter(Boolean);
  return occurrenceIds.some(id => queryIds.includes(id));
}

function lineRangeFromOffsets(text, range) {
  const start = offsetPosition(text, range.start);
  const end = offsetPosition(text, range.end);
  return {
    line: start.line,
    start: start.character,
    end: start.line === end.line ? end.character : start.character + Math.max(1, range.end - range.start)
  };
}

function offsetPosition(text, offset) {
  const source = String(text || '');
  let line = 0;
  let character = 0;
  for (let index = 0; index < Math.min(offset, source.length); index += 1) {
    if (source[index] === '\r' && source[index + 1] === '\n') {
      line += 1;
      character = 0;
      index += 1;
    } else if (source[index] === '\n' || source[index] === '\r') {
      line += 1;
      character = 0;
    } else {
      character += 1;
    }
  }
  return { line, character };
}

function dedupeOffsetOccurrences(occurrences) {
  const seen = new Set();
  const result = [];
  for (const occurrence of occurrences) {
    const key = `${occurrence.kind}:${occurrence.id}:${occurrence.range.start}:${occurrence.range.end}:${occurrence.source}`;
    if (!seen.has(key)) {
      seen.add(key);
      result.push(occurrence);
    }
  }
  return result.sort((left, right) => left.range.start - right.range.start);
}

function dedupeLineRanges(ranges) {
  const seen = new Set();
  const result = [];
  for (const range of ranges) {
    const key = `${range.line}:${range.start}:${range.end}:${range.source}`;
    if (!seen.has(key)) {
      seen.add(key);
      result.push(range);
    }
  }
  return result.sort((left, right) => left.line - right.line || left.start - right.start);
}

function dedupeDeclarationRanges(ranges) {
  const seen = new Set();
  const result = [];
  for (const range of ranges) {
    const key = `${range.kind}:${range.id}:${range.range.start}:${range.range.end}:${range.source}`;
    if (!seen.has(key)) {
      seen.add(key);
      result.push(range);
    }
  }
  return result.sort((left, right) => left.range.start - right.range.start);
}

function isLocalNodeReferenceContext(text, offset) {
  const before = contextBefore(text, offset, 160);
  return /(?:\.|\b)(?:Node|Start|Next|Goto|NextNode|Choice)\s*\([^()]*$/.test(before);
}

function methodCallLikeContext(text, offset, value) {
  const after = text.slice(offset + value.length, offset + value.length + 8);
  const before = contextBefore(text, offset, 24);
  return /^\s*\(/.test(after) || /(?:NextNode|Next|Goto|GoTo)\s*\(\s*$/.test(before);
}

function typedReferenceContext(text, offset) {
  const before = contextBefore(text, offset, 160);
  const after = contextAfter(text, offset + (wordAtOffset(text, offset)?.value.length || 0), 20);
  return /(?:StartStory|Story\.Load|Scene\.Start|Dialogue\.(?:Start|Load)|Environment\.Set|DefaultForm|Set|State\.(?:Set|Add|Adjust|Increment|Decrement|Remove|Clear)|exists|AreaEnter|AreaEnterDelayed|AreaExit|NpcPath|Monologue\.Show|Vehicle\.(?:Trip|Sequence)|Npc\.(?:Spawn|SpawnAt|SleepAt|Despawn|SpawnNearPlayer|Stay|Stop|Interact|StopInteraction|Path|Move|MoveTo|WalkTo|ApproachPlayer|MoveToPlayer|WalkToPlayer|Rotate|Turn|UseBlock|InteractBlock|OpenDoor|CloseDoor|ToggleDoor|UseDoor|LookAtPlayers|ClearLookAtPlayers|HoldItem|ClearHeldItem|PlayAnimation|StopAnimation|PlayFaceAnimation|StopFaceAnimation|EnterVehicle|EnterNearestVehicle|EnterVehicleAt|LeaveVehicle))\s*\(\s*$/.test(before)
    || /(?:cinematic|cameraPath|cameraPathId|cinematicPathId|environment|environmentId|defaultForm|defaultFormId|form|formId|playerProfile|playerProfileId|playerProfileTemplate|playerProfileTemplateId|template|templateId|weapon|weaponId)\s*:\s*(?:CameraPaths\.|Environments\.|NpcForms\.|PlayerProfileTemplates\.|Weapons\.)?$/.test(before)
    || /(?:if|else\s+if)\s*\([^()]*$/.test(before)
    || /(?:&&|\|\||!)\s*$/.test(before)
    || /^\s*(?:\+\+|--|\+=|-=)/.test(after);
}

function identifierRanges(text, name) {
  const result = [];
  const source = String(text || '');
  const pattern = new RegExp(`\\b${escapeRegExp(name)}\\b`, 'g');
  for (const match of source.matchAll(pattern)) {
    const start = match.index;
    if (!isOffsetInTrivia(source, start)) {
      result.push({ start, end: start + name.length });
    }
  }
  return dedupeRanges(result);
}

function wordAtOffset(text, offset) {
  const source = String(text || '');
  let start = offset;
  while (start > 0 && /[A-Za-z0-9_]/.test(source[start - 1])) {
    start -= 1;
  }
  let end = offset;
  while (end < source.length && /[A-Za-z0-9_]/.test(source[end])) {
    end += 1;
  }
  if (end <= start || !/[A-Za-z_]/.test(source[start])) {
    return undefined;
  }
  return { value: source.slice(start, end), range: { start, end } };
}

function stringAtOffset(text, offset) {
  const source = String(text || '');
  const safeOffset = Math.max(0, Math.min(offset, source.length));
  const lineStart = source.lastIndexOf('\n', Math.max(0, safeOffset - 1)) + 1;
  const nextLineBreak = source.indexOf('\n', safeOffset);
  const lineEnd = nextLineBreak === -1 ? source.length : nextLineBreak;
  for (let index = lineStart; index < lineEnd && index <= safeOffset; index += 1) {
    if (source[index] !== '"' && source[index] !== "'") {
      continue;
    }
    const string = readString(source, index, lineEnd);
    if (string && safeOffset >= string.range.start && safeOffset <= string.range.end) {
      return { value: string.value, range: string.range };
    }
    if (string) {
      index = string.next - 1;
    }
  }
  return undefined;
}

function readString(text, index, limit) {
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
        value += text[cursor + 1];
        cursor += 2;
        continue;
      }
      break;
    }
    if (char === quote) {
      return {
        value,
        range: { start: index + 1, end: cursor },
        next: cursor + 1
      };
    }
    value += char;
    cursor += 1;
  }
  return undefined;
}

function matchingParen(text, open) {
  let depth = 0;
  for (let index = open; index < text.length; index += 1) {
    const char = text[index];
    if (char === '"' || char === "'") {
      const string = readString(text, index, text.length);
      index = string ? string.next - 1 : index;
      continue;
    }
    if (char === '(') {
      depth += 1;
    } else if (char === ')') {
      depth -= 1;
      if (depth === 0) {
        return index;
      }
    }
  }
  return -1;
}

function skipWhitespaceAndComments(text, index, limit) {
  let cursor = index;
  while (cursor < limit) {
    if (/\s/.test(text[cursor])) {
      cursor += 1;
      continue;
    }
    if (text[cursor] === '/' && text[cursor + 1] === '/') {
      cursor += 2;
      while (cursor < limit && text[cursor] !== '\n') {
        cursor += 1;
      }
      continue;
    }
    if (text[cursor] === '/' && text[cursor + 1] === '*') {
      cursor += 2;
      while (cursor + 1 < limit && !(text[cursor] === '*' && text[cursor + 1] === '/')) {
        cursor += 1;
      }
      cursor = Math.min(limit, cursor + 2);
      continue;
    }
    break;
  }
  return cursor;
}

function skipValue(text, index, limit) {
  let cursor = index;
  let depth = 0;
  while (cursor < limit) {
    const char = text[cursor];
    if (char === '"' || char === "'") {
      const string = readString(text, cursor, limit);
      cursor = string ? string.next : cursor + 1;
      continue;
    }
    if (char === '(' || char === '[' || char === '{') {
      depth += 1;
    } else if (char === ')' || char === ']' || char === '}') {
      if (depth === 0) {
        return cursor;
      }
      depth -= 1;
    } else if (char === ',' && depth === 0) {
      return cursor;
    }
    cursor += 1;
  }
  return cursor;
}

function isOffsetInTrivia(text, offset) {
  let inString = '';
  let lineComment = false;
  let blockComment = false;
  let escaped = false;
  for (let index = 0; index < offset; index += 1) {
    const char = text[index];
    const next = text[index + 1];
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
    if (inString) {
      if (escaped) {
        escaped = false;
      } else if (char === '\\') {
        escaped = true;
      } else if (char === inString) {
        inString = '';
      }
      continue;
    }
    if (char === '/' && next === '/') {
      lineComment = true;
      index += 1;
    } else if (char === '/' && next === '*') {
      blockComment = true;
      index += 1;
    } else if (char === '"' || char === "'") {
      inString = char;
    }
  }
  return Boolean(inString || lineComment || blockComment);
}

function contextBefore(text, offset, length) {
  return String(text || '').slice(Math.max(0, offset - length), offset);
}

function contextAfter(text, offset, length) {
  return String(text || '').slice(offset, offset + length);
}

function pascalToSnake(value) {
  return String(value || '')
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/([A-Za-z])([0-9])/g, '$1_$2')
    .replace(/([0-9])([A-Za-z])/g, '$1_$2')
    .toLowerCase();
}

function dedupeRanges(ranges) {
  const seen = new Set();
  const result = [];
  for (const range of ranges) {
    const key = `${range.start}:${range.end}`;
    if (!seen.has(key)) {
      seen.add(key);
      result.push(range);
    }
  }
  return result.sort((left, right) => left.start - right.start);
}

function dedupeRangeEdits(edits) {
  const seen = new Set();
  const result = [];
  for (const edit of edits) {
    const key = `${edit.range.start}:${edit.range.end}:${edit.newText}`;
    if (!seen.has(key)) {
      seen.add(key);
      result.push(edit);
    }
  }
  return result.sort((left, right) => left.range.start - right.range.start);
}

function referenceKindsOverlap(left, right) {
  if (!left || !right) {
    return false;
  }
  if (left === right) {
    return true;
  }
  return (left === 'npc' && right === 'speaker')
    || (left === 'speaker' && right === 'npc')
    || (left === 'animation' && right === 'faceAnimation')
    || (left === 'faceAnimation' && right === 'animation')
    || (left === 'animationRig' && right === 'faceRig')
    || (left === 'faceRig' && right === 'animationRig');
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

const RESERVED_RENAME_WORDS = new Set([
  'class',
  'public',
  'private',
  'static',
  'override',
  'start',
  'return',
  'if',
  'else',
  'new',
  'var',
  'true',
  'false'
]);

const NAMED_REFERENCE_KINDS = {
  actor: 'npc',
  actorId: 'npc',
  animation: 'animation',
  animationGraph: 'animationProfile',
  animationId: 'animation',
  animationClip: 'animationClip',
  animationClipId: 'animationClip',
  animationProfileId: 'animationProfile',
  animationRigId: 'animationRig',
  cameraPath: 'cameraPath',
  cameraPathId: 'cameraPath',
  cinematic: 'cameraPath',
  cinematicPathId: 'cameraPath',
  clip: 'animationClip',
  clipId: 'animationClip',
  defaultForm: 'npcForm',
  defaultFormId: 'npcForm',
  definitionId: 'npc',
  dialogue: 'dialogue',
  dialogueId: 'dialogue',
  environment: 'environment',
  environmentId: 'environment',
  face: 'faceAnimation',
  faceAnimation: 'faceAnimation',
  faceAnimationId: 'faceAnimation',
  facePoseId: 'facePose',
  faceRigId: 'faceRig',
  faceProfile: 'faceRig',
  form: 'npcForm',
  formId: 'npcForm',
  graph: 'animationRig',
  profile: 'animationProfile',
  rig: 'animationRig',
  skin: 'skin',
  npc: 'npc',
  npcId: 'npc',
  playerProfile: 'playerProfileTemplate',
  playerProfileId: 'playerProfileTemplate',
  playerProfileTemplate: 'playerProfileTemplate',
  playerProfileTemplateId: 'playerProfileTemplate',
  scene: 'scene',
  sceneId: 'scene',
  speaker: 'speaker',
  speakerId: 'speaker',
  story: 'story',
  storyId: 'story',
  template: 'playerProfileTemplate',
  templateId: 'playerProfileTemplate',
  thenDialogue: 'dialogue',
  thenDialogueId: 'dialogue',
  weapon: 'weapon',
  weaponId: 'weapon'
};

const TYPE_REFERENCE_KINDS = {
  ActorId: 'npc',
  AnimationId: 'animation',
  AnimationClipId: 'animationClip',
  AnimationProfileId: 'animationProfile',
  AnimationRigId: 'animationRig',
  CameraPathId: 'cameraPath',
  DialogueId: 'dialogue',
  EnvironmentId: 'environment',
  FormId: 'npcForm',
  FaceAnimationId: 'faceAnimation',
  FacialAnimationId: 'faceAnimation',
  FacePoseId: 'facePose',
  FaceRigId: 'faceRig',
  NpcFormId: 'npcForm',
  NpcDefinitionId: 'npc',
  PlayerProfileId: 'playerProfileTemplate',
  PlayerProfileTemplateId: 'playerProfileTemplate',
  SkinId: 'skin',
  StoryId: 'story',
  WeaponId: 'weapon',
  EchoItemId: 'weapon'
};

const RESOURCE_DECLARATION_CALL_KINDS = new Map([
  ['Story', 'story'],
  ['Environment', 'environment'],
  ['Dialogue', 'dialogue'],
  ['Form', 'npcForm'],
  ['PlayerProfile', 'playerProfileTemplate'],
  ['Weapon', 'weapon'],
  ['NpcAnimationBinding', 'npcAnimationBinding'],
  ['AnimationProfile', 'animationProfile'],
  ['AnimationRig', 'animationRig'],
  ['AnimationClip', 'animationClip'],
  ['Animation', 'animation'],
  ['FacialAnimation', 'faceAnimation'],
  ['FaceAnimation', 'faceAnimation'],
  ['FacePose', 'facePose']
]);

const RESOURCE_KIND_BY_BASE_TYPE = {
  StoryResource: 'story',
  EnvironmentResource: 'environment',
  DialogueResource: 'dialogue',
  NpcFormResource: 'npcForm',
  NpcResource: 'npc',
  AnimationResource: 'animation',
  FacialAnimationResource: 'faceAnimation',
  FaceAnimationResource: 'faceAnimation',
  AnimationProfileResource: 'animationProfile',
  AnimationRigResource: 'animationRig',
  AnimationClipResource: 'animationClip',
  FaceRigResource: 'faceRig',
  FacePoseResource: 'facePose',
  NpcAnimationBindingResource: 'npcAnimationBinding',
  PlayerProfileTemplate: 'playerProfileTemplate',
  WeaponResource: 'weapon',
  TriggerResource: 'trigger'
};

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

module.exports = {
  echoScriptDefinitionTarget,
  echoScriptRenameTarget,
  isValidEchoScriptIdentifier,
  isValidEchoScriptNodeId,
  methodDeclarations,
  nodeIdRanges,
  identifierRanges,
  echoScriptReferenceQueryAtOffset,
  echoScriptReferenceOccurrencesInText,
  echoScriptReferenceRangesInText,
  echoScriptReferenceRenameEditsInText,
  echoScriptIdentifierRenameEditsInText,
  pascalToSnake
};
