# Echo Water Magic

Отдельная школа водной магии для Echo Magic на Minecraft Forge 1.20.1.

Первая способность — `echo_water_magic:water_bolt`. Она использует точную копию геометрии
`void_circle.png`: изменена только палитра с зелёной на тёмно-синюю и cyan.

## Сборка

```powershell
Set-Location -LiteralPath 'D:\JDHSeason\mods\echo-water-magic'
.\gradlew.bat check build --console=plain
```

Production JAR:

```text
water-magic-forge-1.20.1/build/libs/echo-water-magic-0.1.0.jar
```

## Использование

```text
/reload
/echo_water_magic setup @s
```

После выбора используется переназначаемое действие `Применить выбранное заклинание`.

