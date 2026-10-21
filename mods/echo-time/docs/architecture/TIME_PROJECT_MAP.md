# Echo Time project map

Version: `0.1.0`. Forge target: `1.20.1`.

| Module | Real runtime classes | Responsibility |
|---|---|---|
| `time-contracts` | `EchoInstant`, `EchoDay`, `TimeOfDay`, `TimeRate`, `TimeSetRequest`, `TimeAddRequest`, `TimeScheduleEntry`, `EchoTimeSnapshot` | Typed, loader-neutral public contracts. |
| `time-core` | `EchoClock`, `TimeRateAccumulator`, `EchoTimeRuntime`, `TimeMutationExecutor`, `TimeMutationLedger`, `TimeOccurrenceSchedule`, `TimePauseRegistry`, `TimeWaitRegistry` | Pure deterministic clock, mutations, leases, waits, and indexed crossings. |
| `time-forge-api-1.20.1` | `EchoTimeServiceBridge`, `EchoTimeServiceAccess`, `EchoTimeOccurrenceEvent`, `EchoTimeWaitCompletedEvent`, `EchoTimeClientSnapshotEvent` | Public Forge service and event boundary. |
| `time-forge-1.20.1` | `EchoTimeMod`, `ForgeEchoTimeHost`, `EchoTimeSavedData`, `EchoTimeStateCodec`, `DaylightOwnership`, `VanillaDaylightProjection`, `TimeSpeedConfigLoader`, `TimeSpeedConfigBinder`, `EchoTimeNetwork` | Server host, persistence, daylight ownership, config, commands, and snapshots. |
| `time-script-adapter-1.20.1` | `TimeScriptIntegration`, `TimeActionExecutor`, `TimeStoryExtensionFactory`, `TimeDialogueController` | Executes the Story and Dialogue Time actions bound by their real EchoScript compilers. |
| `time-testmod-1.20.1` | none | Reserved manual Forge test source set; production logic is not duplicated here. |

Related repositories are separate mods: `echo-trigger-time-adapter` publishes Trigger subscriptions into `TimeOccurrenceSchedule`; `echo-time-hud` consumes `EchoTimeClientSnapshotEvent` and owns no clock.

