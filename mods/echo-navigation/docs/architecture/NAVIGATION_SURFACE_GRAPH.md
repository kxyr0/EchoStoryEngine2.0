# Navigation Surface Graph

Surface nodes represent NPC feet positions, not raw empty blocks.

Each `NavigationRouteNode` stores:

- block X/Y/Z;
- quantized surface height in sixteenths;
- derived stance position.

The Forge sampler reads collision shapes, lava state, climbable tags and door state. Clearance checks use the actual
NPC width and height from `NavigationAgentHandle`, so child and adult bodies can receive different valid routes without
Echo Navigation importing Echo NPC internals.
