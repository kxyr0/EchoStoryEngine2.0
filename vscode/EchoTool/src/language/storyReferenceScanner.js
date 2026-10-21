'use strict';

const {
  propertyOnLine,
  quotedValueRange,
  wordRangeAtPosition,
  referenceKindForField
} = require('./languageContext');
const { resolveReference, shortcutReference, relatedKinds } = require('./referenceResolver');

function referenceQueryAtPosition(index, document, position) {
  const line = document.lineAt(position.line).text;
  const property = propertyOnLine(line);
  if (property) {
    const value = quotedValueRange(document, position.line, property);
    if (value && position.character >= value.start && position.character <= value.end) {
      const referenceKind = referenceKindForField(property);
      if (referenceKind) {
        const resolved = resolveReference(index, referenceKind, value.value);
        return {
          kind: resolved ? resolved.kind : referenceKind,
          id: value.value,
          sourceKind: referenceKind
        };
      }
      if (property === 'id') {
        const documentKind = index.documentKind(document.uri.fsPath);
        if (documentKind && documentKind !== 'generic') {
          return {
            kind: documentKind,
            id: value.value,
            declaration: true
          };
        }
      }
    }
  }

  const wordRange = wordRangeAtPosition(document, position);
  if (!wordRange) {
    return undefined;
  }
  const shortcut = shortcutReference(document.getText(wordRange));
  if (shortcut) {
    return {
      kind: shortcut.kind,
      id: shortcut.id,
      sourceKind: shortcut.kind
    };
  }
  return undefined;
}

function referenceOccurrencesInText(text) {
  const occurrences = [];
  const lines = String(text || '').split(/\r?\n/);
  for (let lineNumber = 0; lineNumber < lines.length; lineNumber += 1) {
    const line = lines[lineNumber];
    occurrences.push(...fieldReferenceOccurrences(line, lineNumber));
    occurrences.push(...shortcutReferenceOccurrences(line, lineNumber));
  }
  return dedupeRanges(occurrences);
}

function referenceRangesInText(text, query, documentKind, includeDeclaration) {
  const ranges = referenceOccurrencesInText(text)
    .filter(occurrence => occurrence.id === query.id && kindsOverlap(occurrence.kind, query.kind));
  if (includeDeclaration && kindsOverlap(documentKind, query.kind)) {
    ranges.push(...declarationRangesInText(text, query.id));
  }
  return dedupeRanges(ranges);
}

function referenceRenameEditsInText(text, query, documentKind, newId, includeDeclaration) {
  const edits = [];
  for (const occurrence of referenceOccurrencesInText(text)) {
    if (occurrence.id !== query.id || !kindsOverlap(occurrence.kind, query.kind)) {
      continue;
    }
    edits.push({
      line: occurrence.line,
      start: occurrence.start,
      end: occurrence.end,
      newText: occurrence.source === 'shortcut' && occurrence.prefix
        ? `${occurrence.prefix}:${newId}`
        : newId,
      source: occurrence.source
    });
  }
  if (includeDeclaration && kindsOverlap(documentKind, query.kind)) {
    for (const declaration of declarationRangesInText(text, query.id)) {
      edits.push({
        line: declaration.line,
        start: declaration.start,
        end: declaration.end,
        newText: newId,
        source: declaration.source
      });
    }
  }
  return dedupeRanges(edits);
}

function fieldReferenceOccurrences(line, lineNumber) {
  const result = [];
  const pattern = /\b([A-Za-z_$][\w$-]*)\s*:\s*(['"])(.*?)\2/g;
  for (const match of line.matchAll(pattern)) {
    const kind = referenceKindForField(match[1]);
    if (!kind || !match[3]) {
      continue;
    }
    const start = match.index + match[0].indexOf(match[3]);
    result.push({
      kind,
      id: match[3],
      line: lineNumber,
      start,
      end: start + match[3].length,
      field: match[1],
      source: 'field'
    });
  }
  return result;
}

function shortcutReferenceOccurrences(line, lineNumber) {
  const result = [];
  const pattern = /(['"])([A-Za-z_][\w-]*):([^'"]+)\1/g;
  for (const match of line.matchAll(pattern)) {
    const shortcut = shortcutReference(`${match[2]}:${match[3]}`);
    if (!shortcut) {
      continue;
    }
    const start = match.index + 1;
    result.push({
      kind: shortcut.kind,
      id: shortcut.id,
      prefix: shortcut.prefix,
      line: lineNumber,
      start,
      end: start + match[2].length + 1 + match[3].length,
      field: '',
      source: 'shortcut'
    });
  }
  return result;
}

function declarationRangesInText(text, id) {
  const result = [];
  const escaped = escapeRegExp(id);
  const pattern = new RegExp(`\\bid\\s*:\\s*(['"])${escaped}\\1`, 'g');
  const lines = String(text || '').split(/\r?\n/);
  for (let lineNumber = 0; lineNumber < lines.length; lineNumber += 1) {
    for (const match of lines[lineNumber].matchAll(pattern)) {
      const start = match.index + match[0].indexOf(id);
      result.push({
        kind: '',
        id,
        line: lineNumber,
        start,
        end: start + id.length,
        field: 'id',
        source: 'declaration'
      });
    }
  }
  return result;
}

function kindsOverlap(left, right) {
  if (!left || !right) {
    return false;
  }
  return left === right
    || relatedKinds(left).includes(right)
    || relatedKinds(right).includes(left);
}

function dedupeRanges(ranges) {
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

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

module.exports = {
  referenceQueryAtPosition,
  referenceOccurrencesInText,
  referenceRangesInText,
  referenceRenameEditsInText,
  kindsOverlap
};
