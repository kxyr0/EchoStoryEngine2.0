# Echo Air Magic

Отдельная школа воздушной магии для Echo Magic на Minecraft Forge 1.20.1.

Первая способность — `echo_air_magic:air_bolt`: быстрый лёгкий projectile с
серебристо-небесным следом. Геометрия particle совпадает с `void_circle.png`, изменена только
палитра. После появления sprite медленно поднимается на `0.025` блока за тик.

## Сборка

```powershell
Set-Location -LiteralPath 'D:\JDHSeason\mods\echo-air-magic'
.\gradlew.bat check build --console=plain
```

Production JAR:

```text
air-magic-forge-1.20.1/build/libs/echo-air-magic-0.1.0.jar
```

Требуется `echo-magic-0.1.1-all.jar` или новее.

## Использование

```text
/reload
/echo_air_magic setup @s
```

