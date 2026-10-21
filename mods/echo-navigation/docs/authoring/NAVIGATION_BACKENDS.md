# Navigation Backends

`Npc.Move(...)` does not change between backends.

Use the server config:

```toml
backend = "echo_navigation:astar"
```

Available backend IDs in 0.2.0:

- `echo_navigation:vanilla`
- `echo_navigation:astar`

Use `/echo_navigation backend list` in game to inspect registered backends.
