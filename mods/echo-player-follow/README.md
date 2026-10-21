# Echo Player Follow

Forge 1.20.1 / Java 17 mod that lets the player press a remappable key to follow a targeted active Echo story NPC.

The server discovers nearby materialized NPC actors that currently have an active Echo Navigation move. The client
shows a localized HUD prompt while the crosshair targets that moving NPC, then drives only vanilla movement input toward a point behind
the NPC. Camera rotation stays fully manual.

## Manual Check

1. Install the built jar into a Forge 1.20.1 instance with Echo NPC and Echo Navigation.
2. Start a story/dialogue command that moves an NPC through Echo Navigation.
3. Stand within 7 blocks of the moving NPC.
4. Aim at the moving NPC and hold `Follow Character` for 1 second.
5. Verify the player walks behind the NPC, sprints only when the NPC gets far ahead, and can look around freely.
6. Open dialogue while already following; follow should keep running.
7. Move only the mouse; follow should keep running.
8. Press a movement key or press `Follow Character` again after releasing it once; follow should stop immediately.
