# Navigation Stuck Detection

Progress is sampled on the server tick.

Tracked facts:

- current position;
- remaining distance;
- path index;
- server tick;
- repath count.

A movement is considered stuck when progress stays below `minimumProgress` for at least `stuckTicks`. The runtime can
request bounded replans. When `maxReplans` is reached, the movement fails with `NPC_MOVE_STUCK` and outcome `STUCK`.

Stuck detection must not:

- create a path every tick;
- force-load chunks;
- silently teleport the NPC;
- treat unloaded materialization as arrival.
