# Bed Providers

Public API: `PlayerSleepBedProvider` и `PlayerSleepBedProviderRegistry`.

Built-in provider:

`echo_player_sleep:vanilla_bed`

Он поддерживает стандартные `BedBlock`, нормализует head/foot в canonical head position, сохраняет facing, block id и wake candidates.

Furniture beds должны подключаться отдельным adapter после аудита конкретного мода. Runtime не зависит от furniture implementations.
