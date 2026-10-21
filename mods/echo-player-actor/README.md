# Echo Player Actor

Echo Player Actor управляет настоящим `ServerPlayer` как актёром для EchoScript и катсцен.

Мод не создаёт fake player, не переводит игрока в spectator и не симулирует клавиши. Все команды описывают действие тела игрока: `Player.Rotate`, `Player.LookAt`, `Player.Move`, `Player.Stop`, `Player.Interact`, `Player.StandUp`, `Player.StopUsingItem` и predicates.

Главный авторский справочник с полными Story/Dialogue примерами: [docs/player_actor.md](docs/player_actor.md).
