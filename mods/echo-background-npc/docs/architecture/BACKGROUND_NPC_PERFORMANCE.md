# Производительность и проверка

Главные ограничения обеспечиваются структурой, а не настройкой: render body не является `Mob`; время не опрашивается по агентам; A* вычисляется по событию и общему ключу; дальние агенты не имеют `Entity`; одинаковые stationary transforms не формируют custom snapshot; overlay и voice рассматривают только активные conversations.

Production defaults: materialize 64 blocks, dematerialize 76, LOD update intervals 1/2/4 ticks, route planning budget 2 per server tick, route cache 256, один ambient overlay на игрока и максимум три ambient voices. Изменение definition revision инвалидирует старые routes.

## Автоматические проверки

`test` проверяет canonical DSL, namespace/path ID inference, typed symbols, invalid time, deterministic appearance, late-load/Time.Set semantics, exactly-once occurrences, LKG, 100 virtual agents, hysteresis, shared route coalescing, planning storm budget, local spatial grid, pose cache, stationary snapshot suppression, conversation auto-advance и voice budget.

`sourceShapeCheck` завершает build ошибкой при production Java/EchoScript больше 180 logical LOC, более 7 файлов в leaf package/folder, wildcard imports, `Map<String,Object>` и fake split folder names.

## Воспроизводимый runtime benchmark

Testmod не входит в production JAR. В одинаковом мире, положении камеры, resolution и shader configuration:

1. `/echo_background_npc_benchmark scene 0`
2. `/echo_background_npc_frame_benchmark 1200`
3. Записать median и p95 baseline.
4. `/echo_background_npc_benchmark scene 30`
5. Повторить frame benchmark и вычислить абсолютную/процентную delta.
6. Повторить с shaders OFF и обычным project shader preset.

Сцены принимают 0–100 и предназначены для обязательных 1/10/30/60/100 профилей. Серверный замер запускается `/echo_background_npc_benchmark server 1200`; для acceptance сначала измеряется scene 0, затем 100 agents/30 visible. Результаты выводятся в chat и server log в миллисекундах. Production `/echo_background_npc performance` показывает population, materialization, queued routes и cache hit rate.

Frame-time нельзя подменять unit-тестом или оценкой. Итоговыми считаются только числа с reference machine при реально отрисованных 30 NPC; целевые дополнительные median/p95 — не более 1.5/2.5 ms. Если бюджет не выдержан, задача остаётся performance-blocked до профилирования.
