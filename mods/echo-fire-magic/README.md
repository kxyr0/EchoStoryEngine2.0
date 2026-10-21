# Echo Fire Magic

Отдельная школа огненной магии для Echo Magic на Minecraft Forge 1.20.1.

Первая способность — `echo_fire_magic:fire_bolt`: один быстрый огненный снаряд с собственной
круглой billboard-частицей. Её горячий жёлто-белый центр плавно переходит в оранжевые и
тёмно-красно-оранжевые края. Частицы поднимаются вверх на `0.04` блока за тик; это настроено
при регистрации particle provider и не меняет частицы других школ.

Registry ID `echo_fire_magic:fire_flame` сохранён для обратной совместимости, хотя визуальная
форма texture начиная с `0.1.1` является кругом.

## Сборка

```powershell
Set-Location -LiteralPath 'D:\JDHSeason\mods\echo-fire-magic'
.\gradlew.bat check build --console=plain
```

Production JAR:

```text
fire-magic-forge-1.20.1/build/libs/echo-fire-magic-0.1.1.jar
```

Требуется `echo-magic-0.1.1-all.jar` или новее.

## Использование

После установки обоих JAR и запуска мира с разрешёнными командами:

```text
/reload
/echo_fire_magic setup @s
```

Затем используется действие управления `Применить выбранное заклинание` в категории
`Магия Echo`. По умолчанию это `V`, но клавишу можно переназначить в обычных настройках
управления Minecraft.
