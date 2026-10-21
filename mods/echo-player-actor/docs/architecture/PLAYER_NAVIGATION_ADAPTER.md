# Player Navigation Adapter

`echo-player-navigation-adapter` зависит только от public API Echo Player Actor и Echo Navigation.

Он мапит `PlayerActorId` в `NavigationSubjectId` и подключается через `PlayerActorMovementBridgeRegistrationEvent`.

Текущий Echo Navigation public `NavigationAgentHandle` ожидает `Mob`, поэтому adapter не регистрирует `ServerPlayer` как navigation `Mob` и не создаёт fake entity. Фактический body motor остаётся в Echo Player Actor.
