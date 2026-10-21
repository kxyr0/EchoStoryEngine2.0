# Echo Observation architecture

EA1: OnComplete использует общий AudioCommandBinder и исполняет portable `echo_audio:cue`
через optional ObservationAudioExecutor. Разрешены one-shot и finite nonblocking loop;
бесконечные/блокирующие команды отклоняются общим completion guard. Playback ownership
привязан к completion + player, receipt остаётся в существующем Observation progress.
Аудио: [canonical AUDIO_DSL.md](../../../../docs/runtime/AUDIO_DSL.md).
Изменения: [реестр](../changelogs/README.md).

EO1 состоит из трёх независимых модов: `echo-concentration` владеет режимом концентрации и grayscale post-effect; `echo-observation` компилирует данные, хранит per-player progress, выполняет server-authoritative focus и рисует presentation; `echo-observation-authoring` — удаляемый client-only dev-инструмент. Удаление authoring JAR не меняет gameplay.

Observation — неизменяемое виртуальное определение, не Entity и не BlockEntity. Resource reload строит полный candidate catalog; candidate с любой error-диагностикой отклоняется целиком, поэтому остаётся last-known-good catalog. После успешной установки сервер отправляет sequence/revision snapshot. Клиент индексирует только eligible active points по dimension и chunk bucket и не обходит весь сезон каждый кадр.

Клиент получает active-camera snapshot и реальную projection matrix из Echo Visibility. World anchor проецируется в physical framebuffer; GUI Scale используется только при переводе готовых физических координат в координаты `GuiGraphics`. Occlusion — block raycast от active camera. Выбор при пересечении колец детерминирован: distance to screen center, depth, затем ObservationId.

Focus начинается пакетом с ObservationId и definition revision. Клиент отправляет bounded heartbeat, но никогда процент выполнения. Сервер закрепляет definition revision, считает ticks, проверяет resource/point state, dimension, distance, направление взгляда и line of sight. Устаревшая или пропавшая heartbeat-сессия удаляется. Completion ID и action execution IDs детерминированы; receipts сохраняются вместе с progress.

Progress принадлежит UUID игрока: active resources, disabled points, completions и action receipts. `Formerly("old_local_id")` переносит completion/disabled state на новый локальный ID при установке catalog. Story-команды регистрируются через optional Echo Story extension; без Story Engine основной мод продолжает загружаться.

Порядок completion: durable mark → lifecycle event и inspection text → generic `OnComplete` actions → sync. Registry маршрутизирует actions по строковому target; clue-specific Java handlers и switch по типам content отсутствуют.
