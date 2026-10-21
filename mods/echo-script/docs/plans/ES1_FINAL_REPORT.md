# ES1 Final Report

## 1. Краткий итог

Создан standalone-проект EchoScript: plain Java 17 Gradle library с двумя модулями, immutable public API,
hand-written lexer, recoverable recursive-descent parser, structured diagnostics, safety limits, canonical fixture,
JUnit/ArchUnit/Checkstyle/PMD/source-shape/whitespace gates. Runtime execution and consumer-domain semantics отсутствуют.

## 2. Фактическая структура репозитория

- Root: `settings.gradle`, `build.gradle`, `gradle.properties`, Gradle Wrapper, `.gitignore`, `.gitattributes`.
- Modules: `echo-script-api`, `echo-script-language`.
- Docs: `AGENTS.md`, `PLANS.md`, `PROGRESS.md`, `README.md`, `docs/**`.
- Fixture: `echo-script-language/src/test/resources/fixtures/chapter_1_home.es`.
- License: `LICENSE` отсутствует, выбор лицензии не сделан.

## 3. Gradle dependency graph

- `echo-script-api`: runtimeClasspath has no dependencies.
- `echo-script-language`: runtimeClasspath -> `project :echo-script-api`.

## 4. Public API

- Parser contracts: `EchoScriptParser`, `ParseRequest`, `ParseResult`.
- Version/limits: `LanguageVersion`, `ProcessingLimits`.
- Source model: `SourceId`, `SourcePosition`, `SourceSpan`.
- Diagnostics: `LanguageDiagnostic`, `DiagnosticSeverity`.
- Syntax tree: records and sealed contracts under `io.github.kxyr0.echoscript.api.syntax`.

## 5. Полный поддерживаемый V1 syntax subset

Optional namespace, one class, class/member modifiers, base type, fields, methods, parameters, blocks, `var` locals,
return statements, expression statements, identifiers, string/integer/boolean literals, member access, invocation,
object creation, array literals, parenthesized expressions, positional arguments, named arguments, array type suffixes.

## 6. Полный список неподдерживаемых конструкций

using/import/include, attributes, annotations, generics, interfaces, enums, properties, indexers, lambdas, delegates,
operators, arithmetic, comparisons, assignment statements, if/else/switch/for/foreach/while/do, try/catch/throw,
async/await, reflection, preprocessor directives, interpolation, macros, runtime functions, multiple classes,
multiple inheritance, dynamic typing, arbitrary code execution.

## 7. Lexer behavior

Recognizes all V1 keywords, identifiers, integers, strings, braces, parentheses, brackets, commas, dots, colons,
semicolons, assignment, EOF. Skips whitespace, `//` comments, and `/* */` comments. Supports `\"`, `\\`, `\n`,
`\r`, `\t`. Preserves Russian text and source spans. Reports unexpected characters, invalid escapes, unterminated
strings, unterminated block comments, token limit, and string limit.

## 8. Parser behavior

Parses compilation unit, namespace, class, fields, methods, parameters, blocks, local declarations, return statements,
expression statements, literals, member access, invocations, object creation, arrays, positional arguments, and named
arguments into immutable API syntax records. It rejects unknown language versions before V1 parsing.

## 9. Error recovery behavior

Parser reports expected/missing tokens, synchronizes at semicolons, braces, and member boundaries, and collects later
independent diagnostics when safe. Failed parses return diagnostics and no syntax tree in `ParseResult`.

## 10. Safety limits

Defaults: source length `1_000_000`, token count `50_000`, diagnostics `100`, nesting `64`, class members `512`,
method statements `1_000`, invocation arguments `128`, array elements `1_024`, string length `16_384`,
qualified-name segments `64`.

## 11. Diagnostic code families

- `ESL-LANGUAGE-xxx`
- `ESL-LEX-xxx`
- `ESL-PARSE-xxx`
- `ESL-LIMIT-xxx`

Implemented codes are listed in `docs/language/DIAGNOSTICS.md`.

## 12. Canonical fixture result

`chapter_1_home.es` parses successfully with zero diagnostics. The tree preserves namespace, class name, base type,
fields, array types, field initializers, method declarations, modifiers, local `var`, object creation, method
invocations, member access, array literal, positional arguments, named arguments, Russian text, return statements, and
source spans.

## 13-14. Все production-типы и ответственность

| Type | Responsibility |
| --- | --- |
| `LanguageVersion` | Immutable selected EchoScript language version. |
| `EchoScriptParser` | Public syntax-only parser contract. |
| `ParseRequest` | Immutable parser input. |
| `ParseResult` | Immutable parser output with diagnostics and optional tree. |
| `ProcessingLimits` | Immutable processing limit configuration. |
| `SourceId` | Logical platform-neutral source identity. |
| `SourcePosition` | Offset plus one-based line/column. |
| `SourceSpan` | Half-open span in one source. |
| `DiagnosticSeverity` | Diagnostic severity enum. |
| `LanguageDiagnostic` | Immutable structured diagnostic. |
| `SyntaxNode` | Common syntax node span contract. |
| `SyntaxModifier` | Preserved syntactic modifier enum. |
| `QualifiedNameSyntax` | Dotted-name syntax. |
| `NamespaceSyntax` | Namespace declaration syntax. |
| `CompilationUnitSyntax` | V1 compilation unit syntax. |
| `ClassSyntax` | Class declaration syntax. |
| `MemberSyntax` | Sealed member syntax contract. |
| `TypeSyntax` | Type name plus array rank. |
| `ParameterSyntax` | Method parameter syntax. |
| `FieldSyntax` | Field declaration syntax. |
| `MethodSyntax` | Method declaration syntax. |
| `StatementSyntax` | Sealed statement syntax contract. |
| `BlockStatementSyntax` | Block statement syntax. |
| `LocalDeclarationStatementSyntax` | Local `var` declaration syntax. |
| `ReturnStatementSyntax` | Return statement syntax. |
| `ExpressionStatementSyntax` | Expression statement syntax. |
| `ExpressionSyntax` | Sealed expression syntax contract. |
| `NameExpressionSyntax` | Identifier expression syntax. |
| `StringLiteralExpressionSyntax` | String literal expression syntax. |
| `IntegerLiteralExpressionSyntax` | Integer literal expression syntax. |
| `BooleanLiteralExpressionSyntax` | Boolean literal expression syntax. |
| `MemberAccessExpressionSyntax` | Member access expression syntax. |
| `InvocationExpressionSyntax` | Invocation expression syntax. |
| `ObjectCreationExpressionSyntax` | Object creation expression syntax. |
| `ArrayLiteralExpressionSyntax` | Array literal expression syntax. |
| `ParenthesizedExpressionSyntax` | Parenthesized expression syntax. |
| `ArgumentSyntax` | Positional or named argument syntax. |
| `EchoScriptLanguage` | Factory for the default parser. |
| `SourceText` | Internal immutable decoded source model. |
| `DiagnosticCodes` | Stable diagnostic code constants. |
| `DiagnosticBag` | Bounded internal diagnostic collection. |
| `TokenKind` | Lexer token kind enum. |
| `Token` | Internal token with span and value. |
| `LexResult` | Lexer output. |
| `EchoScriptLexer` | Deterministic token scanner. |
| `KeywordClassifier` | Keyword-to-token classification. |
| `TriviaSkipper` | Whitespace and comment skipping. |
| `TokenCursor` | Parser token cursor. |
| `ParseContext` | Parser source/limits/diagnostic context. |
| `TypeParser` | Type and qualified-name parsing. |
| `ArgumentListParser` | Argument list parsing and validation. |
| `ExpressionParser` | Expression parsing. |
| `StatementParser` | Statement and block parsing. |
| `MemberParser` | Field, method, and parameter parsing. |
| `SyntaxParser` | Compilation unit and class-shell parsing. |
| `DefaultEchoScriptParser` | API parser implementation. |

## 15. Production type count by module and package

- `echo-script-api`: `api` 5, `api.diagnostic` 2, `api.source` 3, `api.syntax` 6,
  `api.syntax.declaration` 5, `api.syntax.expression` 11, `api.syntax.statement` 5.
- `echo-script-language`: `language` 1, `language.diagnostic` 2, `language.lexer` 6,
  `language.parser` 9, `language.source` 1.

## 16. Все lexer tests

- `recognizesKeywordsIdentifiersPunctuationArraysIntegersAndBooleans`
- `preservesUtf8StringsAndRecognizesEscapes`
- `skipsLineAndBlockCommentsAndTracksLfCrLfSpans`
- `reportsUnexpectedCharactersInvalidEscapesAndUnterminatedText`
- `malformedInputTerminatesWithoutExpectedExceptions`

## 17. Все parser tests

- `canonicalFixtureParsesWithExpectedTreeAndRussianText`
- `syntaxCollectionsAreImmutable`
- `reportsPreciseDelimiterAndMalformedDeclarationDiagnostics`
- `reportsNamedArgumentErrorsAndRecoversToLaterIndependentErrors`
- `malformedSourceDoesNotThrowOrLoopAndLfCrLfHaveEquivalentMeaning`
- `unsupportedAndMultipleOrMissingClassSyntaxAreDiagnostics`

## 18. Все safety tests

- `enforcesSourceTokenDiagnosticNestingAndMemberLimits`
- `enforcesStatementArgumentArrayAndStringLimits`
- `limitFailuresRemainBoundedAndDefaultsAcceptCanonicalShape`

## 19. Все architecture tests

- `modulesKeepTheRequiredGradleDependencyDirection`
- `productionSourcesDoNotUseForbiddenTechnologiesOrDomainImports`
- `packageSlicesHaveNoCycles`
- `productionClassAndMethodHardLimitsAreRespected`

## 20-21. Все выполненные команды и результат

- `java -version`: Java 21 runtime available; Gradle compiles with `--release 17`.
- `gradle -v`: failed, global Gradle not installed.
- Gradle Wrapper generation via downloaded Gradle 8.10.2: wrapper files created.
- `.\gradlew.bat compileJava`: passed after using `--release 17`.
- `.\gradlew.bat projects`: passed; root has `echo-script-api`, `echo-script-language`.
- `.\gradlew.bat test`: passed.
- `.\gradlew.bat architectureTest`: passed.
- `.\gradlew.bat check`: passed.
- `.\gradlew.bat build`: passed.
- `.\gradlew.bat checkWhitespace`: passed.
- `git diff --check`: passed.
- Runtime dependency reports: passed; graph is API none, language -> API.
- Explicit scans for forbidden production/build references: clean.
- Explicit scans for mutable static production fields, generic packages, and task markers: clean.

## 22. Проверки, которые не выполнялись, и причина

No commit, branch, tag, push, publication, Maven Central publication, mod packaging, and adapter integration checks were
run because they are explicit non-goals.

## 23. Оставшиеся риски

- V1 grammar is intentionally narrow.
- Parser package has 9 production types, below hard cap but above preferred target.
- License remains unresolved.

## 24. Осознанно отложенные возможности

Formatter, lossless trivia tree, imports, semantic type checking, runtime execution, language server, editor support,
source rewriting, publication, mod packaging, Story adapter, Dialogue adapter.

## 25. Подтверждение отсутствия Story semantics

Production/build scans for Story-domain references returned no matches. The canonical fixture names are parsed only as
syntax.

## 26. Подтверждение отсутствия Dialogue semantics

Production/build scans for Dialogue-domain references returned no matches. `DialogueResource` is not interpreted.

## 27. Подтверждение отсутствия Forge/Minecraft integration

Production/build scans for `net.minecraft`, `net.minecraftforge`, and `net.fabricmc` returned no matches. No mod
descriptor or loader entrypoint exists.

## 28. Следующие логичные этапы

- ES2: stabilize public API and publish locally.
- Story-EchoScript Adapter.
- Dialogue-EchoScript Adapter.
