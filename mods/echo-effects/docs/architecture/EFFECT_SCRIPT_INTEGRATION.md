# Effect Script Integration

`EchoScriptEffectCompiler` использует существующий `EchoScriptLanguage.parser()`.

Flow:

```text
ParseRequest
-> ParseResult
-> EffectResourceBinder
-> EffectDefinitionValidator
-> EffectDefinition
```

Второй EchoScript parser не создается.
