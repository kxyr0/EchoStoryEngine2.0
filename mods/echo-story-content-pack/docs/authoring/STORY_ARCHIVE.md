# Архив текущего сюжета

Закройте Minecraft перед переключением. Нужен Echo Story Engine 1.10.0 или новее.
Из корня репозитория `echo-story-content-pack`:

```powershell
.\tools\Set-PrismStoryArchive.ps1 -Action Archive
.\tools\Set-PrismStoryArchive.ps1 -Action Status
.\tools\Set-PrismStoryArchive.ps1 -Action Restore
```

Archive сначала создаёт полный ZIP `minecraft/story-archives/jdh-*.zip`, затем
ставит `story/content-pack/jdh/.archived` с путём снимка и SHA256. Имена папок и
рабочие файлы остаются на месте. Engine не регистрирует этот external data/resource
pack и не дописывает в него встроенные seed-файлы, пока маркер существует.
Restore снимает маркер и включает текущие рабочие файлы без перезаписи из ZIP.
ZIP остаётся резервным снимком исходного состояния, в том числе скрытых файлов.
`-WhatIf` показывает действие без изменений; повторное действие безопасно.

После переключения перезапустите Minecraft. Архивируется именно `content-pack/jdh`:
отдельные `story/configs`, `story/triggers`, `story/item-pack`, другие datapacks,
мировые NPC и сохранённый прогресс этим инструментом не изменяются.
Сюжет пока остаётся активным: возможность архивирования подготовлена, Archive запускается пользователем.
