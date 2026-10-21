# Navigation World Snapshot

The A* backend builds a bounded snapshot on the server thread.

The snapshot contains:

- surface route nodes;
- typed traversal edges;
- bounds;
- world snapshot revision;
- agent profile revision;
- maximum expanded node limit.

It does not contain `Mob`, `ServerLevel`, `PathNavigation`, open set, closed set or collision objects.
