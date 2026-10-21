# Echo Player Sleep: быстрый старт

Echo Player Sleep запускает сюжетный сон только после настоящего ванильного сна.

Минимальный поток:

1. Story вызывает `Sleep.Arm` для конкретного scenario id.
2. Игрок обычным ПКМ ложится на кровать.
3. Forge runtime ждёт `ServerPlayer.isSleeping()` и sleeping bed position.
4. Создаётся durable `PlayerSleepSessionSnapshot`.
5. Arm помечается consumed.
6. Клиент получает close-eyes transition.
7. После закрытия глаз Story может вызвать `EnterDream`, `ReturnToBed`, `CompleteNight`, `OpenEyes` и `Complete`.

Без arm мод не вмешивается в сон. Клик по кровати не считается подтверждением сна.
