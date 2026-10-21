# Diagnostics

Diagnostic fields:

- stable code;
- severity;
- source ID;
- optional source span;
- concise message;
- bounded context;
- optional hint.

Code families:

- `ESL-LANGUAGE-xxx`
- `ESL-LEX-xxx`
- `ESL-PARSE-xxx`
- `ESL-LIMIT-xxx`

Implemented codes:

- `ESL-LANGUAGE-001`: unsupported language version.
- `ESL-LIMIT-001`: source too large.
- `ESL-LIMIT-002`: token limit exceeded.
- `ESL-LIMIT-003`: diagnostic limit exceeded.
- `ESL-LIMIT-004`: nesting limit exceeded.
- `ESL-LIMIT-005`: member limit exceeded.
- `ESL-LIMIT-006`: statement limit exceeded.
- `ESL-LIMIT-007`: argument limit exceeded.
- `ESL-LIMIT-008`: array element limit exceeded.
- `ESL-LIMIT-009`: string literal limit exceeded.
- `ESL-LIMIT-010`: qualified-name segment limit exceeded.
- `ESL-LEX-001`: unexpected character.
- `ESL-LEX-002`: invalid string escape.
- `ESL-LEX-003`: unterminated string.
- `ESL-LEX-004`: unterminated block comment.
- `ESL-PARSE-001`: unexpected token.
- `ESL-PARSE-002`: expected token.
- `ESL-PARSE-003`: missing semicolon.
- `ESL-PARSE-004`: missing closing parenthesis.
- `ESL-PARSE-005`: missing closing bracket.
- `ESL-PARSE-006`: missing closing brace.
- `ESL-PARSE-007`: malformed field declaration.
- `ESL-PARSE-008`: malformed method declaration.
- `ESL-PARSE-009`: malformed named argument.
- `ESL-PARSE-010`: duplicate named argument.
- `ESL-PARSE-011`: positional argument after named arguments.
- `ESL-PARSE-012`: multiple classes in V1.
- `ESL-PARSE-013`: missing class declaration.
- `ESL-PARSE-014`: unsupported syntax.
