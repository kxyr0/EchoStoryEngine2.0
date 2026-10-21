# Echo Content Doctor 0.2.0

Build-time semantic validation for typed Echo content catalogs.

Запуск: `./gradlew contentDoctor check build`.

Для inspection resources нужен экспорт реального item registry из запущенной сборки:

```powershell
.\gradlew.bat contentDoctor -PcontentRoot="D:\content-pack" -PitemCatalog="D:\minecraft\config\echo_item_inspection\item-catalog.txt"
```

CLI: `--resources <content-root> --item-catalog <file> <report.json>`.
Каталог — UTF-8, один canonical `namespace:item` ID на строке. Пустые строки и `#` комментарии разрешены.
Без каталога проверка inspection возвращает `ITEM_INSPECTION_ITEM_CATALOG_REQUIRED`, поскольку offline Doctor не может
подтвердить существование vanilla и modded items. `.weapon.es` проверяются настоящим Echo Item Pack compiler;
при переданном registry snapshot определения, ещё не зарегистрированные в игре, не удовлетворяют inspection binding.

[Changelog](docs/changelogs/README.md)
