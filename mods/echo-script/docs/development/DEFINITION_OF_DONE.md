# Definition of Done

A change is done only when:

- implementation stays syntax-only;
- runtime dependencies obey the project map;
- public API remains immutable and Java-friendly;
- malformed source returns diagnostics instead of expected uncaught exceptions;
- canonical fixture still parses with zero diagnostics;
- `.\gradlew.bat check` passes;
- documentation reflects implemented behavior;
- no commit is created unless explicitly requested.
