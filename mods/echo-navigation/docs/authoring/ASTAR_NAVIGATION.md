# A* Navigation

The A* backend is intended for authored spaces with doors, full-block steps, slabs, stairs and ladders.

It plans over surface nodes and then moves the NPC with a custom motor. If a route edge becomes blocked, the same
movement can request a bounded replan instead of silently switching to vanilla navigation.
