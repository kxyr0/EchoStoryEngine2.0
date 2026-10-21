# Как добавлять и менять EchoScript методы в расширении

Этот файл нужен для поддержки VS Code расширения `EchoStoryEngine Authoring Tools`.
Если в движке появился новый EchoScript метод, его надо добавить не в одно место, а в несколько слоев: hover, signature help, completions, подсказки аргументов, подсветка и smoke-тест.

## Главное правило

Сначала проверь, что метод реально существует как EchoScript DSL/intrinsic.

Ищи не только action type, а именно binder/listener/reader, который принимает вызов вида `Npc.Move(...)`, `Dialogue.Start(...)`, `StartDialogue(...)`.

Пример поиска из корня `D:\JDHSeason`:

```powershell
rg -n 'Npc\.NewMethod|NewMethod|case "NewMethod"|equals\("Npc.NewMethod"\)' mods docs -g '*.java' -g '*.md' -g '*.es'
```

Если найден только raw action type вроде `echo_npc:teleport`, но нет биндинга `Npc.Teleport(...)`, такой метод в hover/completion добавлять нельзя. Иначе расширение начнет предлагать код, который движок не компилирует.

## Где что лежит

Основные файлы:

- `src/language/echoscriptSupport.js` - hover-сигнатуры, русские hover-описания, список известных namespace/member методов.
- `src/language/echoscriptSignatureHelp.js` - подсказка параметров внутри скобок, например когда курсор в `Npc.Move(|)`.
- `src/language/echoscriptCompletionModel.js` - автодополнение методов, snippets, resource refs и named arguments.
- `src/language/hoverProvider.js` - hover на named arguments, например `x`, `next`, `dimension` внутри вызова.
- `src/language/echoscriptStaticSymbols.js` - статические namespace-значения вроде `Items.*`, `Layer.*`, `Hand.*`.
- `syntaxes/echoscript.tmLanguage.json` - подсветка новых глобальных или qualified методов.
- `scripts/smoke-test.js` - обязательные проверки, чтобы новый метод не сломался позже.

## Добавление нового namespace метода

Пример: появился реальный метод `Npc.NewMethod(npc, value, next)`.

### 1. Добавь hover-сигнатуру

Файл: `src/language/echoscriptSupport.js`.

Добавь или обнови запись в `METHOD_SIGNATURES` или в актуальном version-массиве:

```js
['NewMethod', 'Npc.NewMethod(npc: NpcRef, value: string, next?: string) -> Effect']
```

Если метод относится к новой версии движка, лучше добавлять в соответствующий блок:

- `ENGINE_1_8_METHOD_SIGNATURES`
- `ENGINE_1_8_4_METHOD_SIGNATURES`
- `ENGINE_1_8_5_METHOD_SIGNATURES`

Если метод должен быть виден при наведении на namespace `Npc`, добавь строку в aggregate hover для `Npc`, чтобы hover на `Npc` показывал новый метод среди остальных.

### 2. Добавь русский hover summary

В том же файле найди `METHOD_HOVER_SUMMARIES_RU`.

Добавь короткое описание на русском:

```js
['NewMethod', 'Выполняет новое действие NPC и после завершения может перейти в next.']
```

Не оставляй английский fallback первым. Плохие варианты:

```js
['NewMethod', 'Creates ...']
['NewMethod', 'Alias for ...']
```

Нормальные варианты:

```js
['NewMethod', 'Создает ...']
['NewMethod', 'Короткая форма Npc.OtherMethod: ...']
```

### 3. Добавь метод в список namespace members

Файл: `src/language/echoscriptSupport.js`.

Найди `NAMESPACE_MEMBER_METHODS` и добавь имя в нужный namespace:

```js
['Npc', methods('Spawn', 'SpawnAt', 'NewMethod', ...)]
```

Это нужно, чтобы расширение понимало `Npc.` и показывало метод как member.

Если это fluent/builder метод вроде `Line(...).NewMethod(...)`, добавляй не сюда, а в `FLUENT_MEMBER_METHODS`.

### 4. Добавь signature help

Файл: `src/language/echoscriptSignatureHelp.js`.

Для qualified вызова ключ должен быть полным:

```js
'Npc.NewMethod': [
  signature(
    'Npc.NewMethod(npc: NpcRef, value: string, next?: string) -> Effect',
    'Русское описание того, что делает метод.',
    [
      ['npc', 'NPC, над которым выполняется действие.'],
      ['value', 'Значение для нового действия.'],
      ['next', 'Следующий local story node id после завершения.']
    ]
  )
]
```

Для глобального метода ключ простой:

```js
StartDialogue: [
  signature(...)
]
```

Если метод является alias другого метода, можно переиспользовать сигнатуры:

```js
SIGNATURES['Npc.NewAlias'] = SIGNATURES['Npc.NewMethod'];
```

### 5. Добавь completions

Файл: `src/language/echoscriptCompletionModel.js`.

Если первый аргумент должен подсказывать NPC refs, добавь target в `POSITIONAL_REFERENCE_KINDS`:

```js
'Npc.NewMethod': { 0: ['npc'] },
```

Если метод показывается после `Npc.`, добавь snippet в `MEMBER_SNIPPETS.Npc`:

```js
['NewMethod', 'NewMethod(${1:Sasha}, value: "${2:value}");', 'Короткое описание для списка автодополнения']
```

Если нужен глобальный snippet без namespace, добавь в `GLOBAL_SNIPPETS`.

Для named argument completions добавь `ARGUMENT_SNIPPETS`:

```js
ARGUMENT_SNIPPETS['Npc.NewMethod'] = [
  ['npc', 'npc: ${1:Sasha}', 'NPC ref'],
  ['value', 'value: "${1:value}"', 'Значение действия'],
  ['next', 'next: "${1:after_new_method}"', 'Следующий story node']
];
```

Не используй константу до ее объявления. Если shared-массив объявлен ниже, либо перенеси блок ниже, либо распиши элементы вручную.

### 6. Добавь hover для named arguments

Файл: `src/language/hoverProvider.js`.

Найди `ECHOSCRIPT_ARGUMENT_DOCS` и добавь документацию:

```js
ECHOSCRIPT_ARGUMENT_DOCS['Npc.NewMethod'] = {
  npc: 'NPC ref или typed symbol.',
  value: 'Значение, которое метод передает в runtime.',
  next: 'Следующий local story node id после завершения.'
};
```

Это отдельный hover: он появляется при наведении на имя аргумента `value:` внутри вызова.

### 7. Добавь подсветку

Файл: `syntaxes/echoscript.tmLanguage.json`.

Если метод qualified, добавь его в regex `entity.name.function.member.echoscript`:

```json
Npc\\.NewMethod
```

Если метод глобальный, добавь имя в regex `entity.name.function.echoscript`.

## Как менять старый метод

При изменении сигнатуры старого метода обновляй все места вместе:

1. `src/language/echoscriptSupport.js`: сигнатура и русский hover summary.
2. `src/language/echoscriptSignatureHelp.js`: список параметров и docs каждого параметра.
3. `src/language/echoscriptCompletionModel.js`: snippets, reference kinds, named argument snippets.
4. `src/language/hoverProvider.js`: hover для named arguments.
5. `syntaxes/echoscript.tmLanguage.json`: подсветка, если поменялось имя.
6. `scripts/smoke-test.js`: тесты на новый hover/signature/completion.

Если старое имя еще поддерживается движком как alias, не удаляй его. Оставь сигнатуру и напиши в русском hover, что это короткая форма или старое имя.

## Что добавить в smoke-test

Файл: `scripts/smoke-test.js`.

Минимальный набор проверок для нового метода:

```js
assert(signatureForWord('NewMethod').includes('Npc.NewMethod'), 'Hover signatures should include Npc.NewMethod.');
assert(hoverSummaryForWord('NewMethod').includes('новое действие'), 'Hover summary should describe Npc.NewMethod in Russian.');
assert(signatureHelpTargets().includes('Npc.NewMethod'), 'Signature help should include Npc.NewMethod.');

const newMethodArg = echoScriptSignatureHelp('Npc.NewMethod(Sasha, value: ', 'Npc.NewMethod(Sasha, value: '.length);
assert(newMethodArg && newMethodArg.target === 'Npc.NewMethod', 'Signature help should detect Npc.NewMethod.');

const labels = echoScriptCompletionEntries('Npc.NewMethod(Sasha, ', 'Npc.NewMethod(Sasha, '.length, fakeIndex).map(entry => entry.label);
assert(labels.includes('value') && labels.includes('next'), 'Npc.NewMethod argument completions should include value and next.');
```

Если метод должен появляться в `Npc.` completions, добавь проверку на `MEMBER_SNIPPETS` через существующий helper в smoke-test.

## Проверки перед сборкой

Из `D:\JDHSeason\vscode\EchoTool`:

```powershell
npm run check
npm run smoke
```

Отдельный audit, чтобы hover не откатился на английские fallback-и:

```powershell
@'
const { hoverWords, hoverSummaryForWord } = require('./src/language/echoscriptSupport');
const words = hoverWords();
const englishFallbacks = words.filter((word) => {
  const summary = hoverSummaryForWord(word) || '';
  return /^(Creates|Builds|Adds|Sets|Starts|Stops|Moves|Spawns|Teleports|Checks|Reads|Writes|Alias|Use|Waits|Returns|Clears|Applies|Configures)\b/.test(summary);
});
console.log(`hover words: ${words.length}`);
console.log(`english fallback summaries: ${englishFallbacks.length}`);
if (englishFallbacks.length) {
  console.log(englishFallbacks.slice(0, 50).join(', '));
  process.exitCode = 1;
}
'@ | node -
```

Сборка `.vsix`:

```powershell
npm run package
```

## Частые ошибки

- Добавили `METHOD_SIGNATURES`, но забыли `METHOD_HOVER_SUMMARIES_RU`: hover есть, но описание будет слабым или английским.
- Добавили `MEMBER_SNIPPETS.Npc`, но забыли `NAMESPACE_MEMBER_METHODS`: метод может не появиться там, где расширение ищет namespace members.
- Добавили `Npc.Method` в signature help как `Method`: signature help не сработает для qualified вызова.
- Добавили `ARGUMENT_SNIPPETS` до объявления shared-константы: расширение упадет при загрузке модуля.
- Добавили raw action type как метод: VS Code начнет предлагать некомпилируемый EchoScript.

## Быстрый чеклист

- [ ] Метод подтвержден в engine binder/listener/reader.
- [ ] Добавлена hover-сигнатура.
- [ ] Добавлен русский hover summary.
- [ ] Добавлен namespace или fluent member list.
- [ ] Добавлен signature help.
- [ ] Добавлены completions/snippets.
- [ ] Добавлены named argument completions.
- [ ] Добавлен hover для named arguments.
- [ ] Обновлена подсветка, если метод новый по имени.
- [ ] Добавлены smoke-тесты.
- [ ] `npm run check` прошел.
- [ ] `npm run smoke` прошел.
- [ ] Hover audit показывает `english fallback summaries: 0`.
- [ ] `.vsix` пересобран через `npm run package`.
