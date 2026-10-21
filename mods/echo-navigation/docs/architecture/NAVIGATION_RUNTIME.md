# Navigation Runtime

`navigation-core` owns movement state without Minecraft imports.

Runtime responsibilities:

- validate requests;
- enforce one active move per subject;
- make command IDs idempotent;
- pin a backend to each accepted move;
- track status and outcome;
- sample progress;
- detect stuck movement;
- expose immutable snapshots.

Forge host responsibilities:

- resolve `NavigationSubjectId` into a live `Mob`;
- call the pinned backend on the server thread;
- save snapshots before dispatch;
- write diagnostics;
- bridge commands and public services.

Main statuses:

`CREATED`, `PLANNING`, `MOVING`, `REPLANNING`, `WAITING_FOR_MATERIALIZATION`, `SUSPENDED_UNLOADED`, `ARRIVED`,
`FAILED`, `BLOCKED`, `CANCELLED`.
