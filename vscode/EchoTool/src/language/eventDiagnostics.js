'use strict';

const catalog = require('../catalog/echoEvents.json');

// Conservative editor checks for complete predicates; the server binder remains authoritative.
function eventDiagnostics(text, index) {
  const source = maskComments(String(text || ''));
  const result = [];
  const code = source.replace(/"(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*'/g, value => ' '.repeat(value.length));
  for (const match of source.matchAll(/\bWhen\s*\(\s*([A-Za-z]+\.[A-Za-z]+)\s*\(([^()]*)\)\s*(?:,\s*(?:id|once)\s*:\s*(?:"(?:[^"\\]|\\.)*"|true|false)\s*)*\)/g)) {
    if (code.slice(match.index, match.index + 4) !== 'When') continue;
    const descriptor = catalog.find(value => value.predicate === match[1]);
    const offset = match.index + match[0].indexOf(match[1]);
    const prefix = source.slice(0, offset).split('\n');
    const problem = message => result.push({ line: prefix.length - 1, column: prefix.at(-1).length,
      length: match[1].length, severity: 'error', message });
    if (!descriptor) { problem(`Неизвестный предикат события: ${match[1]}.`); continue; }
    const args = match[2].match(/"(?:[^"\\]|\\.)*"|[^,]+/g) || [];
    if (args.length !== descriptor.filters.length) {
      problem(`${match[1]} требует ${descriptor.filters.length} аргументов.`); continue;
    }
    if (!index || typeof index.ids !== 'function') continue;
    descriptor.filters.forEach((field, position) => {
      if (field !== 'resource') return;
      const literal = /^\s*"([^"\\]+)"\s*$/.exec(args[position]);
      if (!literal) return;
      const kind = descriptor.predicate.split('.')[0].toLowerCase();
      const ids = index.ids(kind) || [];
      if (ids.length && !ids.includes(literal[1])) problem(`${kind} не найден: ${literal[1]}.`);
    });
  }
  return result;
}

function maskComments(text) {
  const output = text.split('');
  let quote = '', escaped = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (quote) {
      if (escaped) escaped = false;
      else if (c === '\\') escaped = true;
      else if (c === quote) quote = '';
    } else if (c === '"' || c === "'") quote = c;
    else if (c === '/' && text[i + 1] === '/') {
      while (i < text.length && text[i] !== '\n') output[i++] = ' ';
    } else if (c === '/' && text[i + 1] === '*') {
      output[i++] = ' '; output[i] = ' ';
      while (++i < text.length) {
        if (text[i] === '*' && text[i + 1] === '/') { output[i++] = ' '; output[i] = ' '; break; }
        if (text[i] !== '\n') output[i] = ' ';
      }
    }
  }
  return output.join('');
}

module.exports = { eventDiagnostics };
