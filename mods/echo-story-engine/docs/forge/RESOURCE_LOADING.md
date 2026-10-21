# Forge Resource Loading

## Resource location

Story resources - это server data resources:

```text
data/<namespace>/echo_story/stories/**/*.es
```

Minecraft `ResourceLocation` становится source ID, например:

```text
sample:echo_story/stories/intro.es
```

В ES2 data namespace и basename файла задают Story ID: `sample:.../intro.es` становится `sample:intro`.
`Id("namespace:path")` позволяет сохранить stable ID, если он намеренно отличается от имени файла. Legacy-профиль
по-прежнему получает ID из `Story("namespace:path", ...)` внутри `Build()`.

## Reload pipeline

`ForgeStoryResourceReloadListener` ищет `.es` под `echo_story/stories`, читает UTF-8 text, сортирует по source ID и
передает текст в `StoryResourceCandidateCompiler`.

Шаги:

1. Build `StorySourceInput` для каждого `.es` resource.
2. Compile through `EchoScriptStorySourceCompiler`.
3. Collect source и Forge diagnostics.
4. Reject whole candidate, если есть diagnostic severity `ERROR`.
5. Reject incompatible duplicate Story IDs через `ESE-FORGE-RESOURCE-003`.
6. Install accepted candidate через atomic `StoryCatalogState` replacement.

## Catalog policy

`CompiledStoryCatalogSnapshot` immutable. Readers видят предыдущий snapshot или следующий snapshot, но не partial
catalog. Invalid reload сохраняет предыдущий catalog и emits `ESE-FORGE-RESOURCE-002`.

New executions используют current catalog. Active executions держат выбранное compiled definition в host memory до
terminal state или возврата в `READY`.

## Diagnostics

M4 Forge resource diagnostics:

- `ESE-FORGE-RESOURCE-001`: catalog installed.
- `ESE-FORGE-RESOURCE-002`: candidate rejected.
- `ESE-FORGE-RESOURCE-003`: incompatible duplicate Story ID.
- `ESE-FORGE-RESOURCE-004`: resource read failed.

Diagnostics stored in Java sources are UTF-8. Windows console может показать mojibake, хотя source/tests остаются
валидным UTF-8.

## Архив внешнего content-pack

Echo Story Engine 1.10.0 поддерживает `story/content-pack/jdh/.archived`.
При наличии маркера external pack скрывается из SERVER_DATA и CLIENT_RESOURCES,
а seed extraction не добавляет отсутствующие файлы. Снятие маркера и перезапуск
возвращают рабочий pack. Снимки и переключение находятся в sibling content-pack:
`tools/Set-PrismStoryArchive.ps1`; ZIP хранится в `minecraft/story-archives`.
Configs, triggers, item-pack и сохранения мира имеют отдельный lifecycle.
