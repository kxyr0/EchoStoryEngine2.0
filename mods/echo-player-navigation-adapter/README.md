# Echo Player Navigation Adapter

Adapter связывает `Player.Move`/`Player.Stop` с public API Echo Navigation и с `PlayerActorMotor`.

Текущий Echo Navigation public agent contract ожидает `Mob`, поэтому adapter не подменяет настоящего игрока и не создаёт fake entity. Он выполняет preflight через Navigation service, а фактическое движение настоящего `ServerPlayer` оставляет motor-слою Echo Player Actor.
