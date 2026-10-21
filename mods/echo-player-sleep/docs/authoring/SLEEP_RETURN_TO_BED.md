# ReturnToBed

`Sleep.ReturnToBed` возвращает настоящего игрока к исходной кровати по `PlayerSleepBedSnapshot`.

Для vanilla beds built-in provider сохраняет canonical head position, facing, actual block id и deterministic safe wake candidates. Текущая реализация выбирает первый safe wake candidate и переводит session в `RETURNED_AWAKE`.

Дальше авторский сценарий вызывает `CompleteNight`, `OpenEyes` и `Complete`.
