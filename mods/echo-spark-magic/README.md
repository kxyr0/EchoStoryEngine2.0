# Echo Spark Magic

Movement-only школа магии искры для Echo Magic на Minecraft Forge 1.20.1.

Spark Magic намеренно не содержит `SpellResource`, projectile или damage type. После включения
для игрока маленькие жёлтые круги появляются у ног только при фактическом изменении позиции.
В покое, spectator mode и во время езды particle не создаются.

Центр texture ярко-жёлтый (`#FFFA04`), края тёмно-жёлтые/охристые. Геометрия и
alpha-map совпадают с `void_circle.png`.

## Сборка

```powershell
Set-Location -LiteralPath 'D:\JDHSeason\mods\echo-spark-magic'
.\gradlew.bat check build --console=plain
```

Production JAR:

```text
spark-magic-forge-1.20.1/build/libs/echo-spark-magic-0.1.0.jar
```

## Использование

```text
/echo_spark_magic setup @s
/echo_spark_magic status @s
/echo_spark_magic disable @s
/echo_spark_magic enable @s
```

Состояние хранится у игрока и копируется после respawn. Клавиша применения заклинания здесь
не используется: это passive movement trail, а не cast.

