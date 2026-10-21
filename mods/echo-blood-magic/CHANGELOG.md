# Changelog

## 0.4.0

- The second rapier hit now creates a temporary blood link.
- Sneak-use shatters the temporary rapier into seven damaging particle needles.
- Void weapons can sever blood links through the public Echo Magic API.

## 0.3.0

- Blood Rapier теперь настоящий временный `SwordItem`, который Epic Fight распознаёт как колющее оружие.
- Предыдущий предмет главной руки сохраняется внутри NBT рапиры и безопасно возвращается при окончании, смерти или выходе.
- Стандартная item-модель невидима: весь клинок по-прежнему строится только частицами, без PNG-спрайта.

## 0.2.0

- Кровавый урон накладывает видимую метку: пять слабых импульсов по 1 урону раз в секунду.
- Добавлено заклинание `echo_blood_magic:blood_rapier` для `Technique(POINT)`.
- Кровавая рапира существует 12 секунд как клинок из частиц, добавляет 2 урона ближним атакам и обновляет метку.

## 0.1.2

- `.es` заклинания перенесены в JDH story content pack; школьный JAR теперь содержит только runtime и ресурсы Blood Magic.

## 0.1.1

- Для ванильного particle-режима добавлен тёмно-красный след с ярко-красным ядром.

## 0.1.0

- Добавлена школа `echo_blood_magic:blood`.
- Добавлено DSL-заклинание `echo_blood_magic:blood_bolt`.
- Добавлены blood damage type и particle `echo_blood_magic:blood_circle`.
- Particle использует ярко-красный центр и плавный тёмно-красный градиент к краям.
- Добавлена команда `/echo_blood_magic setup <player>`.
