# Player Sleep Project Map

## Modules

- `player-sleep-contracts`: neutral ids, policies, session snapshots, typed event payloads and Story claim contracts.
- `player-sleep-core`: arm registry, scenario catalog, session factory, state machine and repository.
- `player-sleep-forge-api-1.20.1`: Forge lookup bridge, lifecycle event, claim service and context bridge.
- `player-sleep-forge-1.20.1`: vanilla bed interception, runtime services, event ledger, diagnostic commands and
  network eye overlay.
- `player-sleep-script-adapter-1.20.1`: Story `Sleep.*` external actions with inherited sleep context fallback.
- `player-sleep-testmod-1.20.1`: development-only test mod.

## Normal Sleep

```text
No Arm
-> vanilla sleep
-> no narrative event
-> no Story
```

## Narrative Sleep

```text
Sleep.Arm
-> VanillaSleepInterception
-> PlayerSleepSessionFactory
-> WAITING_FOR_STORY_CLAIM
-> PlayerSleepEventPublisher CONFIRMED
-> Trigger subscription adapter
-> PlayerSleepStoryClaimService
-> STORY_CLAIMED
-> Story Sleep.CloseEyes
```

## Context Inheritance

```text
PlayerSleepEventSnapshot
-> SleepStoryContextInheritance
-> PlayerSleepStoryContextBridge
-> SleepActionHandler
-> PlayerSleepServiceBridge.sessions
```

## Conflict

```text
one Session
-> multiple claiming subscriptions
-> PLAYER_SLEEP_STORY_SUBSCRIPTION_CONFLICT
-> safe wake
-> arm preserved
```

## Dependencies

Sleep runtime does not import Dialogue or Cutscene implementation. Trigger/Story routing belongs to
`echo-trigger-player-sleep-adapter`; Story actions remain in `player-sleep-script-adapter-1.20.1`.

## Diagnostic Commands

- `/echo_sleep arm status <player>`
- `/echo_sleep session inspect <player>`
- `/echo_sleep event inspect <event_id>`
- `/echo_sleep claim inspect <player>`
- `/echo_sleep diagnostics`
