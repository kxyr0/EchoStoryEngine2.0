# Safety

EchoScript source is never executed. The language frontend does not use scripting engines, Java compilation, bytecode
generation, reflection invocation, dynamic class loading, background threads, global parser caches, or log files.

Default limits:

- maximum source length: `1_000_000`;
- maximum token count: `50_000`;
- maximum diagnostics: `100`;
- maximum parser nesting depth: `64`;
- maximum class members: `512`;
- maximum method statements: `1_000`;
- maximum invocation arguments: `128`;
- maximum array literal elements: `1_024`;
- maximum string literal length: `16_384`;
- maximum qualified-name segments: `64`.

When a fatal limit is exceeded, parsing stops safely and returns structured diagnostics.
