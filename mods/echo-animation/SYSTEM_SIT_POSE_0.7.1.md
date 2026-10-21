# System Sit Pose — runtime 0.7.1

`SegmentedHumanoidModelRenderer` больше не добавляет ванильные углы сидения к плечам и ногам.

Когда `AnimationHumanoidRenderContext.sitting()` возвращает `true`, `HumanoidRenderDispatcher` ищет:

1. `jdh:pose_sit_main` — канонический clip от Exporter 3.3.1+;
2. `jdh:pose_sit` — fallback для старого плоского ID.

Clip семплируется на последнем кадре как базовая поза. Если поверх сидящего NPC играет сюжетная анимация, runtime заменяет только кости, реально присутствующие в активном/предыдущем clip. Поэтому upper-body реплика может работать поверх сидения, не сбрасывая ноги.

Если системный clip отсутствует, runtime один раз пишет warning и НЕ подмешивает старую ванильную позу.
