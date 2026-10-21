'use strict';

const REFERENCE_FIELD_KIND = new Map([
  ['dialogueId', 'dialogue'],
  ['dialogue', 'dialogue'],
  ['interactionDialogue', 'dialogue'],
  ['thenDialogueId', 'dialogue'],
  ['thenDialogue', 'dialogue'],
  ['onArriveDialogueId', 'dialogue'],
  ['onArrivalDialogueId', 'dialogue'],
  ['startDialogue', 'dialogue'],
  ['start_dialogue', 'dialogue'],
  ['sceneId', 'scene'],
  ['scene', 'scene'],
  ['startScene', 'scene'],
  ['start_scene', 'scene'],
  ['storyId', 'story'],
  ['story', 'story'],
  ['speakerId', 'speaker'],
  ['speaker', 'speaker'],
  ['actorId', 'npc'],
  ['definitionId', 'npc'],
  ['driverNpcId', 'npc'],
  ['driverNpc', 'npc'],
  ['driver', 'npc'],
  ['npcId', 'npc'],
  ['npc', 'npc'],
  ['animationId', 'animation'],
  ['animationProfileId', 'animationProfile'],
  ['animationRigId', 'animationRig'],
  ['animationClipId', 'animationClip'],
  ['clipId', 'animation'],
  ['clip', 'animation'],
  ['anim', 'animation'],
  ['enterAnimationId', 'animation'],
  ['enterAnimation', 'animation'],
  ['loopAnimationId', 'animation'],
  ['loopAnimation', 'animation'],
  ['exitAnimationId', 'animation'],
  ['exitAnimation', 'animation'],
  ['faceRigId', 'faceRig'],
  ['facePoseId', 'facePose'],
  ['pathId', 'cameraPath'],
  ['cameraPathId', 'cameraPath'],
  ['cameraPath', 'cameraPath'],
  ['cinematicPathId', 'cameraPath'],
  ['cinematic', 'cameraPath'],
  ['portraitId', 'speaker'],
  ['portrait', 'speaker'],
  ['skin', 'skin'],
  ['skinId', 'skin']
]);

function lineText(document, position) {
  return document.lineAt(position.line).text;
}

function propertyBeforeCursor(document, position) {
  const text = lineText(document, position).slice(0, position.character);
  const match = /([A-Za-z_$][\w$-]*)\s*:\s*(?:(["'])[^"']*|[A-Za-z0-9_.:-]*)?$/.exec(text);
  return match ? match[1] : undefined;
}

function propertyOnLine(text) {
  const match = /^\s*([A-Za-z_$][\w$-]*)\s*:/.exec(text);
  return match ? match[1] : undefined;
}

function isCompletingProperty(document, position) {
  const text = lineText(document, position).slice(0, position.character);
  return /(?:^|[,{]\s*|^\s*)([A-Za-z_$][\w$-]*)?$/.test(text) && !text.includes(':');
}

function isCompletingStringValue(document, position) {
  const text = lineText(document, position).slice(0, position.character);
  return /:\s*(["'])[^"']*$/.test(text);
}

function wordRangeAtPosition(document, position) {
  return document.getWordRangeAtPosition(position, /[A-Za-z0-9_.$:-]+/);
}

function quotedValueRange(document, lineNumber, fieldName) {
  const text = document.lineAt(lineNumber).text;
  const escaped = escapeRegExp(fieldName);
  const match = new RegExp(`\\b${escaped}\\s*:\\s*(['"])(.*?)\\1`).exec(text);
  if (!match) {
    return undefined;
  }
  const start = match.index + match[0].indexOf(match[2]);
  const end = start + match[2].length;
  return {
    start,
    end,
    value: match[2]
  };
}

function typeValueOnLine(text) {
  const match = /\btype\s*:\s*(['"])(.*?)\1/.exec(text);
  return match ? match[2] : undefined;
}

function inferTypeContext(document, position) {
  const property = propertyBeforeCursor(document, position);
  const around = textBefore(document, position, 80);
  if (property !== 'type') {
    return undefined;
  }
  return inferStructuralContext(around);
}

function inferStructuralContext(text) {
  const cleaned = stripStringsAndComments(text);
  const lastTrigger = cleaned.lastIndexOf('trigger');
  const lastCondition = Math.max(cleaned.lastIndexOf('condition'), cleaned.lastIndexOf('if'));
  const lastEffects = Math.max(cleaned.lastIndexOf('effects'), cleaned.lastIndexOf('do'), cleaned.lastIndexOf('then'), cleaned.lastIndexOf('else'));
  const lastSteps = cleaned.lastIndexOf('steps');
  const max = Math.max(lastTrigger, lastCondition, lastEffects, lastSteps);
  if (max < 0) {
    return 'effect';
  }
  if (max === lastTrigger) {
    return 'trigger';
  }
  if (max === lastCondition) {
    return 'condition';
  }
  if (max === lastSteps && lastSteps > lastEffects) {
    return 'sceneStep';
  }
  return 'effect';
}

function inferObjectContextFromPath(pathParts, documentKind) {
  const parts = pathParts.map(String);
  const last = parts[parts.length - 1] || '';
  const joined = parts.join('.');
  if (['command', 'npcCommand', 'npc_command', 'worldCommand', 'world_command'].includes(last)) {
    return 'command';
  }
  if (last === 'trigger' || joined.endsWith('.trigger')) {
    return 'trigger';
  }
  if (['condition', 'if'].includes(last) || joined.includes('.conditions.')) {
    return 'condition';
  }
  if (['effects', 'do', 'then', 'else', 'thenEffects', 'elseEffects', 'startEffects', 'completeEffects', 'abortEffects', 'onStartEffects', 'onCompleteEffects'].includes(last)) {
    return 'effect';
  }
  if (last === 'steps' && documentKind === 'scene') {
    return 'sceneStep';
  }
  return documentKind === 'event' ? 'eventRoot' : `${documentKind}Root`;
}

function textBefore(document, position, maxLines) {
  const from = Math.max(0, position.line - maxLines);
  const lines = [];
  for (let line = from; line <= position.line; line += 1) {
    const text = document.lineAt(line).text;
    lines.push(line === position.line ? text.slice(0, position.character) : text);
  }
  return lines.join('\n');
}

function pathAtOffset(text, offset) {
  const stack = [];
  let currentProperty = undefined;
  let stringQuote = '';
  let escaped = false;
  let lineComment = false;
  let blockComment = false;
  let token = '';
  let expectingProperty = true;

  for (let index = 0; index < Math.min(offset, text.length); index += 1) {
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
    if (stringQuote) {
      if (escaped) {
        escaped = false;
        continue;
      }
      if (char === '\\') {
        escaped = true;
        continue;
      }
      if (char === stringQuote) {
        stringQuote = '';
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
      stringQuote = char;
      continue;
    }

    if (/[A-Za-z0-9_$-]/.test(char)) {
      token += char;
      continue;
    }

    if (char === ':' && token && expectingProperty) {
      currentProperty = token;
      expectingProperty = false;
      token = '';
      continue;
    }

    if (char === '{') {
      stack.push(currentProperty || '<object>');
      currentProperty = undefined;
      expectingProperty = true;
      token = '';
      continue;
    }

    if (char === '[') {
      stack.push(currentProperty || '<array>');
      currentProperty = undefined;
      expectingProperty = false;
      token = '';
      continue;
    }

    if (char === '}' || char === ']') {
      stack.pop();
      currentProperty = undefined;
      expectingProperty = false;
      token = '';
      continue;
    }

    if (char === ',') {
      currentProperty = undefined;
      expectingProperty = stack.length === 0 || stack[stack.length - 1] !== '<array>';
      token = '';
      continue;
    }

    if (!/\s/.test(char)) {
      token = '';
    }
  }

  return stack.filter(part => part && !part.startsWith('<'));
}

function stripStringsAndComments(text) {
  return String(text || '')
    .replace(/\/\/.*$/gm, '')
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .replace(/"(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*'/g, '""');
}

function isReferenceField(fieldName) {
  return REFERENCE_FIELD_KIND.has(fieldName);
}

function referenceKindForField(fieldName) {
  return REFERENCE_FIELD_KIND.get(fieldName);
}

function referenceFieldNames() {
  return Array.from(REFERENCE_FIELD_KIND.keys());
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

module.exports = {
  lineText,
  propertyBeforeCursor,
  propertyOnLine,
  isCompletingProperty,
  isCompletingStringValue,
  wordRangeAtPosition,
  quotedValueRange,
  typeValueOnLine,
  inferTypeContext,
  inferStructuralContext,
  inferObjectContextFromPath,
  pathAtOffset,
  isReferenceField,
  referenceKindForField,
  referenceFieldNames
};
