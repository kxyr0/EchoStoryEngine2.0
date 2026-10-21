# Trigger Player Sleep Adapter Map

## Purpose

`echo-trigger-player-sleep-adapter` is the single bridge from Echo Player Sleep lifecycle events to Echo Trigger story
subscriptions. It does not create a second Sleep runtime or a second Trigger runtime.

## Runtime Flow

```text
PlayerSleepLifecycleEvent.sleepEvent
-> SleepTriggerEventProvider
-> SleepSubscriptionIndex exact lookup
-> SleepSubscriptionConflictValidator
-> TriggerServiceBridge.runtime.start
-> EchoStoryExecutionBridge.start
-> PlayerSleepStoryClaimService.claim
```

## Normal Sleep

```text
No Arm
-> vanilla sleep
-> no PlayerSleepEventSnapshot
-> no Trigger subscription lookup
-> no Story
```

## Narrative Sleep

```text
Sleep.Arm
-> vanilla confirmation
-> PlayerSleepSession
-> Confirmed selector
-> Trigger subscription
-> Story claim
-> StoryResource
```

## Context Inheritance

```text
Sleep event
-> Trigger execution context
-> Story execution context
-> implicit current SleepSession
```

`SleepStoryContextInheritance` installs context into `PlayerSleepStoryContextBridge` before Story start. `Sleep.*`
actions merge explicit payload over inherited context.

## Conflict

```text
one Session
-> multiple claiming subscriptions
-> conflict
-> no registration-order selection
-> safe wake
-> arm preserved
```

## Files

- `EchoTriggerPlayerSleepAdapterMod`: Forge entrypoint and server lifecycle wiring.
- `SleepTriggerEventProvider`: Sleep event to Trigger/Story/claim route.
- `SleepSubscriptionIndex`: exact provider/event/scenario index.
- `SleepSubscriptionConflictValidator`: missing/single/conflict classification.
- `SleepStoryContextInheritance`: sleep context payload builder.
- `SleepTriggerRecovery`: 20 tick missing-claim timeout.
