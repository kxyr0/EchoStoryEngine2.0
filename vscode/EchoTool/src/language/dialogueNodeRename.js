'use strict';

const DIALOGUE_NODE_REFERENCE_FIELDS = new Set([
  'start',
  'startNodeId',
  'next',
  'goto',
  'to'
]);

function dialogueNodeRenameTarget(text, offset) {
  const nodeIds = dialogueNodeIdsFromText(text);
  for (const declaration of dialogueNodeDeclarationRanges(text)) {
    if (containsOffset(declaration.range, offset)) {
      return {
        id: declaration.id,
        range: declaration.range,
        source: 'declaration'
      };
    }
  }

  for (const reference of dialogueNodeReferenceRanges(text)) {
    if (containsOffset(reference.range, offset) && nodeIds.has(reference.id)) {
      return {
        id: reference.id,
        range: reference.range,
        source: 'reference'
      };
    }
  }
  return undefined;
}

function dialogueNodeRenameRanges(text, id) {
  const target = String(id || '');
  if (!target) {
    return [];
  }
  return dedupeRanges([
    ...dialogueNodeDeclarationRanges(text).filter(entry => entry.id === target).map(entry => entry.range),
    ...dialogueNodeReferenceRanges(text).filter(entry => entry.id === target).map(entry => entry.range)
  ]);
}

function dialogueNodeIdsFromText(text) {
  return new Set(dialogueNodeDeclarationRanges(text).map(entry => entry.id).filter(Boolean));
}

function dialogueNodeDeclarationRanges(text) {
  const source = String(text || '');
  const result = [];
  for (const container of propertyContainers(source, 'nodes')) {
    if (container.open === '{') {
      result.push(...objectNodeDeclarationRanges(source, container));
    } else if (container.open === '[') {
      result.push(...arrayNodeDeclarationRanges(source, container));
    }
  }
  return dedupeEntries(result);
}

function dialogueNodeReferenceRanges(text) {
  return propertyStringAssignments(String(text || ''))
    .filter(entry => DIALOGUE_NODE_REFERENCE_FIELDS.has(entry.key) && entry.value)
    .map(entry => ({
      id: entry.value,
      range: entry.valueRange,
      field: entry.key
    }));
}

function objectNodeDeclarationRanges(text, container) {
  const result = [];
  let index = container.start + 1;
  while (index < container.end) {
    index = skipWhitespaceAndComments(text, index, container.end);
    if (index >= container.end || text[index] === '}') {
      break;
    }

    const key = readPropertyKey(text, index, container.end);
    if (!key) {
      index += 1;
      continue;
    }
    let cursor = skipWhitespaceAndComments(text, key.next, container.end);
    if (text[cursor] !== ':') {
      index = key.next;
      continue;
    }

    result.push({ id: key.value, range: key.range, source: 'declaration' });
    cursor = skipWhitespaceAndComments(text, cursor + 1, container.end);
    index = skipValue(text, cursor, container.end);
    if (text[index] === ',') {
      index += 1;
    }
  }
  return result;
}

function arrayNodeDeclarationRanges(text, container) {
  const result = [];
  let index = container.start + 1;
  while (index < container.end) {
    index = skipWhitespaceAndComments(text, index, container.end);
    if (index >= container.end || text[index] === ']') {
      break;
    }

    if (text[index] === '{') {
      const objectEnd = matchingBracket(text, index, container.end);
      if (objectEnd > index) {
        result.push(...topLevelStringAssignmentsInRange(text, index + 1, objectEnd)
          .filter(entry => entry.key === 'id' && entry.value)
          .map(entry => ({ id: entry.value, range: entry.valueRange, source: 'declaration' })));
        index = objectEnd + 1;
        continue;
      }
    }

    index = skipValue(text, index, container.end);
    if (text[index] === ',') {
      index += 1;
    }
  }
  return result;
}

function propertyContainers(text, propertyName) {
  const result = [];
  for (const assignment of propertyAssignments(text)) {
    if (assignment.key !== propertyName) {
      continue;
    }
    const cursor = skipWhitespaceAndComments(text, assignment.valueStart, text.length);
    const open = text[cursor];
    if (open !== '{' && open !== '[') {
      continue;
    }
    const end = matchingBracket(text, cursor, text.length);
    if (end > cursor) {
      result.push({
        start: cursor,
        end,
        open
      });
    }
  }
  return result;
}

function propertyStringAssignments(text) {
  return propertyAssignments(text).filter(entry => entry.valueType === 'string');
}

function topLevelStringAssignmentsInRange(text, start, end) {
  const result = [];
  let index = start;
  while (index < end) {
    index = skipWhitespaceAndComments(text, index, end);
    if (index >= end) {
      break;
    }

    const key = readPropertyKey(text, index, end);
    if (!key) {
      index += 1;
      continue;
    }
    let cursor = skipWhitespaceAndComments(text, key.next, end);
    if (text[cursor] !== ':') {
      index = key.next;
      continue;
    }
    cursor = skipWhitespaceAndComments(text, cursor + 1, end);
    const value = readString(text, cursor, end);
    if (value) {
      result.push({
        key: key.value,
        keyRange: key.range,
        value: value.value,
        valueRange: value.range,
        valueType: 'string'
      });
      index = value.next;
    } else {
      index = skipValue(text, cursor, end);
    }
    if (text[index] === ',') {
      index += 1;
    }
  }
  return result;
}

function propertyAssignments(text) {
  const source = String(text || '');
  const result = [];
  let index = 0;
  while (index < source.length) {
    index = skipWhitespaceAndComments(source, index, source.length);
    const key = readPropertyKey(source, index, source.length);
    if (!key) {
      index = skipToken(source, index, source.length);
      continue;
    }

    let cursor = skipWhitespaceAndComments(source, key.next, source.length);
    if (source[cursor] !== ':') {
      index = key.next;
      continue;
    }

    cursor = skipWhitespaceAndComments(source, cursor + 1, source.length);
    const value = readString(source, cursor, source.length);
    result.push({
      key: key.value,
      keyRange: key.range,
      value: value ? value.value : undefined,
      valueRange: value ? value.range : undefined,
      valueType: value ? 'string' : valueTypeAt(source, cursor),
      valueStart: cursor,
      valueEnd: value ? value.next : skipValue(source, cursor, source.length)
    });
    index = value ? value.next : skipValue(source, cursor, source.length);
  }
  return result;
}

function readPropertyKey(text, index, limit) {
  if (index >= limit) {
    return undefined;
  }
  const string = readString(text, index, limit);
  if (string) {
    return {
      value: string.value,
      range: string.range,
      next: string.next
    };
  }
  if (!isIdentifierStart(text[index])) {
    return undefined;
  }
  let cursor = index + 1;
  while (cursor < limit && isIdentifierPart(text[cursor])) {
    cursor += 1;
  }
  return {
    value: text.slice(index, cursor),
    range: { start: index, end: cursor },
    next: cursor
  };
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

function skipToken(text, index, limit) {
  if (index >= limit) {
    return limit;
  }
  const char = text[index];
  if (char === '"' || char === "'") {
    const string = readString(text, index, limit);
    return string ? string.next : index + 1;
  }
  return index + 1;
}

function skipValue(text, index, limit) {
  let cursor = skipWhitespaceAndComments(text, index, limit);
  if (cursor >= limit) {
    return limit;
  }

  const char = text[cursor];
  if (char === '"' || char === "'") {
    const string = readString(text, cursor, limit);
    return string ? string.next : cursor + 1;
  }
  if (char === '{' || char === '[') {
    const end = matchingBracket(text, cursor, limit);
    return end > cursor ? end + 1 : cursor + 1;
  }

  while (cursor < limit && text[cursor] !== ',' && text[cursor] !== '}' && text[cursor] !== ']') {
    if (text[cursor] === '/' && (text[cursor + 1] === '/' || text[cursor + 1] === '*')) {
      cursor = skipWhitespaceAndComments(text, cursor, limit);
      continue;
    }
    cursor += 1;
  }
  return cursor;
}

function matchingBracket(text, start, limit) {
  const open = text[start];
  const close = open === '{' ? '}' : open === '[' ? ']' : '';
  if (!close) {
    return -1;
  }

  let depth = 0;
  let cursor = start;
  while (cursor < limit) {
    const char = text[cursor];
    if (char === '"' || char === "'") {
      const string = readString(text, cursor, limit);
      cursor = string ? string.next : cursor + 1;
      continue;
    }
    if (char === '/' && (text[cursor + 1] === '/' || text[cursor + 1] === '*')) {
      cursor = skipWhitespaceAndComments(text, cursor, limit);
      continue;
    }
    if (char === open) {
      depth += 1;
    } else if (char === close) {
      depth -= 1;
      if (depth === 0) {
        return cursor;
      }
    }
    cursor += 1;
  }
  return -1;
}

function valueTypeAt(text, index) {
  const char = text[index];
  if (char === '{') {
    return 'object';
  }
  if (char === '[') {
    return 'array';
  }
  return 'literal';
}

function containsOffset(range, offset) {
  return offset >= range.start && offset <= range.end;
}

function dedupeEntries(entries) {
  const seen = new Set();
  const result = [];
  for (const entry of entries) {
    const key = `${entry.id}:${entry.range.start}:${entry.range.end}`;
    if (!seen.has(key)) {
      seen.add(key);
      result.push(entry);
    }
  }
  return result.sort((left, right) => left.range.start - right.range.start);
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

function isValidDialogueNodeId(value) {
  return /^[A-Za-z0-9_.:-]+$/.test(String(value || ''));
}

function isIdentifierStart(char) {
  return /[A-Za-z_$]/.test(char || '');
}

function isIdentifierPart(char) {
  return /[A-Za-z0-9_$-]/.test(char || '');
}

module.exports = {
  DIALOGUE_NODE_REFERENCE_FIELDS,
  dialogueNodeRenameTarget,
  dialogueNodeRenameRanges,
  dialogueNodeDeclarationRanges,
  dialogueNodeReferenceRanges,
  dialogueNodeIdsFromText,
  isValidDialogueNodeId
};
