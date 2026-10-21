# Vanilla Backend

Backend ID: `echo_navigation:vanilla`.

Implementation modules:

- `navigation-vanilla-1.20.1` contains backend logic.
- `navigation-forge-1.20.1` registers it with the runtime host.

Behavior:

- validates same dimension and loaded destination chunk;
- verifies feet space, head space and floor collision;
- configures ground navigation to pass and open wooden doors;
- creates a vanilla `Path`;
- falls back to a bounded loaded-block path for ladder columns when vanilla path creation cannot reach the target;
- calls `Mob.getNavigation().moveTo(path, 1.0D)`;
- opens the next wooden door on the active route when it is within one block of the NPC;
- applies climb assist on vertical ladder path segments;
- samples current position, remaining distance and path node index;
- verifies final block-centered arrival separately from `PathNavigation.isDone()`;
- cancels by stopping the vanilla navigation.

It does not force-load chunks, move across dimensions, teleport on failure, own authored navigation graphs or own NPC
state.
