# NPC Move

Script authors keep using:

```text
Npc.Move(Sasha, 120, 64, 180);
```

The active runtime backend decides how the route is planned and executed. The script API does not expose
`MoveAStar`, `MoveVanilla` or backend parameters.
