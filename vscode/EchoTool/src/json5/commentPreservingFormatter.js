'use strict';

const prettier = require('prettier');

class CommentPreservingJson5Formatter {
  async format(text, options) {
    return prettier.format(text, {
      parser: 'json5',
      useTabs: options.useTabs,
      tabWidth: options.tabSize,
      trailingComma: options.trailingComma ? 'all' : 'none',
      singleQuote: options.quote === 'single',
      quoteProps: 'as-needed',
      bracketSpacing: true,
      printWidth: 100
    });
  }
}

module.exports = {
  CommentPreservingJson5Formatter
};
