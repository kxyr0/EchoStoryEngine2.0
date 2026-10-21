# PlayerSleepResource

Целевой resource type PS1: `PlayerSleepResource`.

External content path:

`<gameDir>/story/content-pack/<namespace>/echo_player_sleep/scenarios/`

Packaged fallback path:

`data/<namespace>/echo_player_sleep/scenarios/`

Текущий код содержит runtime catalog и тестовый scenario `echo_player_sleep:test/vanilla_bed_dream`. Production coordinates и Block ID не угаданы. Рабочий production parser/binder должен устанавливать `PlayerSleepScenarioDefinition` с реальным `BedAt`, `ReturnMode`, `Recovery` и `Control`.

Документированные псевдопримеры не считаются рабочими `.es`, пока они не проходят фактический EchoScript parser и sleep binder.
