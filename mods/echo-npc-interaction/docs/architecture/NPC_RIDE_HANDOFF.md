# NPC Ride Handoff

Стул определяется поведением, а не Block ID и не mod ID.

Если после proxy use:

```text
proxy.getVehicle() != null
```

runtime классифицирует `PROXY_STARTED_RIDING` и запускает generic handoff.

Стратегии:

- `DIRECT_ATTACH_WHILE_OCCUPIED`: сначала пробует посадить NPC на тот же vehicle, пока proxy ещё сидит.
- `ECHO_SEAT_ANCHOR`: если чужой transient vehicle был создан текущим use и прямой attach невозможен, создаётся собственный anchor.
- `UNSUPPORTED_EXISTING_VEHICLE`: pre-existing vehicle не переносится вслепую.

Production code не импортирует Handcrafted или другие мебельные моды.
