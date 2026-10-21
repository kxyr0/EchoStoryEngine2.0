# Player Actor quickstart

Echo Player Actor даёт EchoScript команды для настоящего тела игрока. Команды не создают fake player, не меняют GameMode и не симулируют клавиши.

```es
private Vector3 LightSwitch = new Vector3(124.5, 65.5, 190.5);
private Vector3 RoomExit = new Vector3(132, 64, 201);

private void LeaveRoom()
{
    Player.LookAt(LightSwitch, duration: Ticks(12), interpolation: SMOOTH);
    Player.Interact(124, 65, 190, face: WEST, hand: MAIN_HAND);
    Player.StandUp();
    Player.Move(RoomExit);
}
```

Команды выполняются от текущего player actor, который задаёт host выполнения: Dialogue viewer, Cutscene controlled actor, Story owner или Trigger player context.
