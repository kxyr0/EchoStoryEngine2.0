# EchoScript grammar

The language stays a bounded Java-like DSL. `namespace` is optional for every resource. A `DialogueResource` also
has contextual high-level members; the same words remain ordinary identifiers/types outside that base resource.

```text
compilationUnit  := namespaceDecl? classDecl EOF
namespaceDecl    := "namespace" qualifiedName ";"
classDecl        := modifier* "class" Identifier ":" type "{" member* "}"
member           := fieldDecl | methodDecl | dialogueNodeDecl | classIntent
classIntent      := invocation ";" | invocation block
dialogueNodeDecl := "start"? "Node" Identifier "(" ")" block
dialogueMetadata := ("Id" | "Speaker" | "Formerly") "(" argumentList? ")" ";"
fieldDecl        := modifier* type Identifier ("=" expression)? ";"
methodDecl       := modifier* "start"? type Identifier "(" parameterList? ")" block
parameterList    := parameter ("," parameter)*
parameter        := type Identifier
type             := ("void" | qualifiedName) ("[" "]")*
block            := "{" statement* "}"
statement        := block | localDecl | returnStmt | sectionStmt | expressionStmt
localDecl        := "var" Identifier ("=" expression)? ";"
returnStmt       := "return" expression? ";"
sectionStmt      := invocation block
expressionStmt   := expression ";"
expression       := primary postfix*
postfix          := "." Identifier | "(" argumentList? ")"
primary          := Identifier | String | Integer | Decimal | Boolean | objectCreation | arrayLiteral | "(" expression ")"
objectCreation   := "new" type "(" argumentList? ")"
arrayLiteral     := "[" (expression ("," expression)*)? "]"
argumentList     := argument ("," argument)*
argument         := Identifier ":" expression | expression
qualifiedName    := Identifier ("." Identifier)*
modifier         := "public" | "private" | "protected" | "override"
```

`start` is a contextual class-member modifier preserved for domain binders such as Scene; it is not assigned runtime
meaning by EchoScript itself.

Inside a high-level `DialogueResource`, `GoTo(TargetNode)` is a symbolic transition. One node is the implicit entry;
multiple nodes require exactly one `start Node`. Resource identity defaults to the data-pack namespace plus the
`.dialogue.es` filename. Explicit `Id(...)`, sparse `Speaker(...)`, `NodeId(...)`, and `Formerly(...)` are typed,
bounded overrides. Legacy `Build/Init/Nodes/NodeDef` dialogue resources remain valid.

An invocation followed by a block is preserved as a syntax-only intent section. This is valid in method and class
bodies; domain binders decide whether names such as `At(...) { ... }` are valid.

Unsupported syntax includes imports, includes, attributes, annotations, generics, interfaces, enums, properties,
indexers, lambdas, delegates, arbitrary operators, arithmetic, comparisons, assignment statements, control flow,
exceptions, async/await, reflection, preprocessor directives, string interpolation, macros, multiple classes per file,
multiple inheritance, dynamic typing, and arbitrary code execution.
