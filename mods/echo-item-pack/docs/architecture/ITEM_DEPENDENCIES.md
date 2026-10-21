# Item dependencies

Разрешенная цепочка:

```text
echo-item-pack
-> echo-script public API
-> Minecraft/Forge host API
```

EchoScript integration:

- Maven group: `io.github.kxyr0.echoscript`
- artifact: `echo-script-language`
- version: `0.1.0-SNAPSHOT`
- parser entrypoint: `io.github.kxyr0.echoscript.language.EchoScriptLanguage.parser()`
- parser contract: `io.github.kxyr0.echoscript.api.EchoScriptParser`
- AST root: `io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax`
- binder entrypoint: `io.github.kxyr0.echoitem.core.script.WeaponDefinitionBinder`

Запрещено:

- `implementation project(":echo-script")`;
- `implementation files("D:/JDHSeason/mods/echo-script/...jar")`;
- копирование EchoScript parser classes;
- reflection для вызова EchoScript;
- зависимость от Echo Story, Dialogue, NPC, Animation, Navigation, Roads или Content Pack implementation.

Root task `prepareEchoScriptArtifacts` публикует sibling `echo-script` в build-local Maven repo:

```text
D:\JDHSeason\mods\echo-item-pack\build\external-maven\
```

Architecture/source scans проверяют forbidden imports и отсутствие duplicate EchoScript classes в production JAR.
