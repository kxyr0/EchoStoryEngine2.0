# N4 execution plan

## Scope

N4 создает independent Forge adapter между Echo Dialogue D6 presentation boundary и
Echo NPC public service.

In scope:

- register D6 speaker-presentation provider;
- resolve `SpeakerId` as exact `ActorId`;
- query current Echo NPC actor presentation;
- return dynamic display name and optional skin head;
- preserve authored visual when NPC has no skin;
- fall back to authored Dialogue presentation on missing/unavailable/blocked cases.

Out of scope:

- starting dialogues;
- NPC interaction ownership;
- distance checks;
- NPC locking, look-at, navigation or animation;
- Story actions;
- spawn, teleport, despawn;
- persistence or restart recovery;
- remote skins, `GameProfile`, HTTP skin services.

## Public artifacts

Dialogue:

- `dialogue-contracts`;
- `dialogue-presentation-api`;
- `dialogue-forge-api-1.20.1`.

NPC:

- `npc-contracts`;
- `npc-forge-api-1.20.1`.

Generated Maven repositories:

- `../echo-dialogue/build/integration-maven`;
- `../echo-npc/build/integration-maven`.

## Acceptance

1. Provider listed by `/echo_dialogue providers`.
2. `jdh:john_father` LINE uses Echo NPC actor display name.
3. `jdh:john_father` CHOICE prompt uses Echo NPC actor display name.
4. With `NpcSkinId("jdh:john_father")`, visual is `SKIN_HEAD`.
5. Without `NpcSkinId`, authored `Portrait("jdh:john_father")` remains.
6. Missing actor returns authored fallback and no ordinary non-NPC diagnostic.
7. Unavailable Echo NPC service returns authored fallback and one bounded warning.
8. Durable actor with `desiredPresence == ABSENT` returns authored fallback.
9. Stopped Echo NPC service returns authored fallback with temporary diagnostic.
10. Query failure and blocked/corrupt actor return authored fallback with
    provider diagnostic.
11. Adapter JAR inspection contains no copied Dialogue/NPC API classes.
12. Headless N4 acceptance server with Echo Story Content Pack acceptance
    resources proves real `NpcService` lookup and dynamic LINE/CHOICE provider
    resolution for materialized `jdh:content_pack_father`.
13. Headless restart-at-CHOICE acceptance preserves the same
    `story-action/jdh:n4_restart_choice/play_greeting` session at `revision=2`,
    resumes after server restart, advances to `revision=3` and completes
    `jdh:content_pack_accepted`.

Client visual acceptance remains separate: do not mark real skin-head rendering
or overlay layout passed without observing a real client.
