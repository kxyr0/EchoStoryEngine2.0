'use strict';

const JSON5 = require('json5');

class Json5Parser {
  parse(text, label = 'JSON5 document') {
    try {
      return {
        ok: true,
        value: JSON5.parse(text),
        error: undefined
      };
    } catch (error) {
      return {
        ok: false,
        value: undefined,
        error: normalizeJson5Error(error, text, label)
      };
    }
  }

  stringify(value, options = {}) {
    return stringifyJson5(value, options);
  }
}

function normalizeJson5Error(error, text, label) {
  const line = typeof error.lineNumber === 'number'
    ? Math.max(0, error.lineNumber - 1)
    : inferLineFromMessage(error.message, text);
  const column = typeof error.columnNumber === 'number'
    ? Math.max(0, error.columnNumber - 1)
    : 0;
  return {
    label,
    message: error && error.message ? error.message : String(error),
    line,
    column
  };
}

function inferLineFromMessage(message, text) {
  const match = /at\s+(\d+):(\d+)/i.exec(String(message || ''));
  if (match) {
    return Math.max(0, Number(match[1]) - 1);
  }
  return Math.max(0, String(text || '').split(/\r?\n/g).length - 1);
}

function stringifyJson5(value, options = {}) {
  const indent = options.useTabs ? '\t' : ' '.repeat(options.tabSize || 2);
  const trailingComma = options.trailingComma !== false;
  const quote = options.quote === 'single' ? "'" : '"';
  const lines = renderValue(value, 0, { indent, trailingComma, quote });
  return `${lines}\n`;
}

function renderValue(value, depth, options) {
  if (Array.isArray(value)) {
    return renderArray(value, depth, options);
  }
  if (value && typeof value === 'object') {
    return renderObject(value, depth, options);
  }
  return renderScalar(value, options);
}

function renderObject(value, depth, options) {
  const entries = Object.entries(value);
  if (entries.length === 0) {
    return '{}';
  }

  const currentIndent = options.indent.repeat(depth);
  const childIndent = options.indent.repeat(depth + 1);
  const lines = ['{'];
  for (let index = 0; index < entries.length; index += 1) {
    const [key, entryValue] = entries[index];
    const comma = options.trailingComma || index < entries.length - 1 ? ',' : '';
    lines.push(`${childIndent}${formatKey(key, options)}: ${renderValue(entryValue, depth + 1, options)}${comma}`);
  }
  lines.push(`${currentIndent}}`);
  return lines.join('\n');
}

function renderArray(value, depth, options) {
  if (value.length === 0) {
    return '[]';
  }

  if (canRenderInlineArray(value)) {
    return `[${value.map(entry => renderScalar(entry, options)).join(', ')}${options.trailingComma ? ',' : ''}]`;
  }

  const currentIndent = options.indent.repeat(depth);
  const childIndent = options.indent.repeat(depth + 1);
  const lines = ['['];
  for (let index = 0; index < value.length; index += 1) {
    const comma = options.trailingComma || index < value.length - 1 ? ',' : '';
    lines.push(`${childIndent}${renderValue(value[index], depth + 1, options)}${comma}`);
  }
  lines.push(`${currentIndent}]`);
  return lines.join('\n');
}

function canRenderInlineArray(value) {
  return value.length <= 4 && value.every(entry => entry == null || ['string', 'number', 'boolean'].includes(typeof entry));
}

function renderScalar(value, options) {
  if (value === null) {
    return 'null';
  }
  if (typeof value === 'string') {
    return quoteString(value, options.quote);
  }
  if (typeof value === 'number') {
    if (Number.isNaN(value)) {
      return 'NaN';
    }
    if (value === Infinity) {
      return 'Infinity';
    }
    if (value === -Infinity) {
      return '-Infinity';
    }
    return String(value);
  }
  if (typeof value === 'boolean') {
    return value ? 'true' : 'false';
  }
  return quoteString(String(value), options.quote);
}

function formatKey(key, options) {
  return isIdentifierKey(key) ? key : quoteString(key, options.quote);
}

function isIdentifierKey(key) {
  return /^[A-Za-z_$][\w$]*$/.test(key);
}

function quoteString(value, quote) {
  const escaped = String(value)
    .replace(/\\/g, '\\\\')
    .replace(/\r/g, '\\r')
    .replace(/\n/g, '\\n')
    .replace(/\t/g, '\\t');
  if (quote === "'") {
    return `'${escaped.replace(/'/g, "\\'")}'`;
  }
  return `"${escaped.replace(/"/g, '\\"')}"`;
}

module.exports = {
  Json5Parser,
  stringifyJson5
};
