# Echo Blood Magic

Отдельная школа кровавой магии для Echo Magic на Minecraft Forge 1.20.1.

`echo_blood_magic:blood_bolt` наносит 5 начального урона и ставит кровавую метку с пятью
импульсами по 1 урону раз в секунду. `echo_blood_magic:blood_rapier` на 12 секунд призывает
настоящий временный предмет `echo_blood_magic:blood_rapier`: ближние удары получают +2 урона
и обновляют метку.

У предмета намеренно нет PNG-спрайта. Его модель невидима, а клинок, гарда и след строятся
цветными частицами. Epic Fight получает профиль `spear`, поэтому в боевом режиме используется
колющая анимация. Общая визуализация находится в соседнем моде
`D:\JDHSeason\mods\echo-magic-epicfight-adapter` и работает с любым будущим временным оружием,
которое заполняет `SummonedWeaponData`.

## Сборка

```powershell
Set-Location -LiteralPath 'D:\JDHSeason\mods\echo-blood-magic'
.\gradlew.bat check build --console=plain
```

Production JAR:

```text
blood-magic-forge-1.20.1/build/libs/echo-blood-magic-0.3.0.jar
```

## Использование

```text
/echo_blood_magic setup @s
```

Выбери Кровь через `N`. Через `V` линия выпускает Blood Bolt, а отдельная удерживаемая точка
не меньше `0.45` секунды призывает Blood Rapier. Рапира временно занимает активный слот и
возвращает прежний предмет после рассеивания, смерти или выхода. Живые `.es` применяются hot
reload автоматически; добавление нового зарегистрированного предмета требует пересборки и
перезапуска Minecraft.
