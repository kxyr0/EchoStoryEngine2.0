# ADR 0003: DialogueResource Semantics Outside EchoScript

Status: accepted for D1.

EchoScript отвечает только за syntax tree. Echo Dialogue adapter владеет значением `DialogueResource`, `Dialogue`, `Start`, `Alias`, `Node` и `Line`.

Reason:

- один EchoScript parser должен оставаться domain-neutral;
- unsupported Dialogue features должны быть diagnostics адаптера;
- `.dialogue.es` не выполняется как код.
