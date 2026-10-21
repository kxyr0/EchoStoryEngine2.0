# Navigation Diagnostics

Diagnostics are structured facts, not hidden exceptions.

Useful commands:

```text
/echo_navigation status
/echo_navigation backend
/echo_navigation diagnostics
```

Diagnostic context should include:

- movement ID;
- command ID;
- subject ID;
- owner ID;
- destination;
- selected or pinned backend;
- status;
- outcome;
- failure;
- progress and repath count.

Acceptance rule:

- `PathNavigation.moveTo` returning accepted is not proof of movement success.
- `PathNavigation.isDone()` returning true is not proof of arrival.
- manual acceptance must verify final position and terminal navigation snapshot.
