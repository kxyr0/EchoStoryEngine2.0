# Sleep с Cutscene

Echo Player Sleep не импортирует Cutscene implementation.

После `EnterDream` Story может запустить Cutscene через существующий Cutscene adapter. Перед возвратом Story вызывает `Sleep.CloseEyes`, затем `Sleep.ReturnToBed`.

Teleport не используется как доказательство безопасного dream entry: безопасным entry считается только переход после закрытого кадра и успешного player placement.
