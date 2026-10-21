# Echo Item Pack

Data-driven Forge 1.20.1 мод для предметов EchoScript.

Первый этап реализует только оружие ближнего боя. Предметы описываются в `.weapon.es`,
компилируются через публичный EchoScript API в immutable `WeaponDefinition` и исполняются
одним generic Java-классом `EchoWeaponItem`.

Целевой внешний workspace:

```text
<gameDir>/story/item-pack/
```

Для текущего PrismLauncher instance это:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\item-pack\
```
