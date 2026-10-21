# Echo Audio — EA1

## Аудит 19.10.2026

Результат: CASE B. Универсальной системы SFX нет. Новый мод `echo_audio`, версия `0.1.0`,
создаётся в `D:/JDHSeason/mods/echo-audio`.

Проверены исходники всех sibling-модов, особенно echo-voice, echo-effects, echo-scene,
echo-dialogue, echo-animation, echo-npc, echo-background-npc, echo-cutscene, echo-script,
echo-story-engine и echo-content-doctor; build/bin исключены из поиска.

* `echo-effects`: `EffectChannelReader` распознаёт `Sound.PlayAt(tick, sound: ...)` только
  внутри EffectResource. `ForgeTimelineEventDispatcher` вызывает vanilla `playSound` для
  зарегистрированного SoundEvent. Нет файлового SFX-каталога, loop/stop/receipts/fade.
* `echo-voice`: `VoiceDecoderRegistry` → `VoiceAudioDecoder` → `DecodedPcm`: независимая
  от речевых очередей граница декодирования WAV (Java Sound), MP3 (JLayer), OGG (Minecraft
  Vorbis). Она переиспользуется напрямую; реализация декодеров не копируется.
* Voice имеет async priority queue, revision-keyed leased cache, watcher/debounce/LKG,
  foreground lane, ambient voice lane (максимум три голоса, подавляется foreground).
  `VoiceSoundInstance` использует VOICE, one-shot, без pitch/loop; его нельзя считать SFX.
* Речь разрешается через `echo_voice`, не через `sounds`; Voice API сохраняет свою семантику.
* Scene имеет собственные typed location symbols и LocationCatalog, blocking modes,
  command receipts и cancelOwned. Локальные playback handles пока не поддерживаются.
* Story lowering создаёт namespaced external actions, adapters исполняют их через
  Forge extension API. Runtime/persistence остаются в Story, SFX — во внешнем моде.
* Doctor использует typed descriptors/references, а не выполняет EchoScript.
* На момент исходного аудита в live `jdh` обнаружены два WAV в `echo_voice`; `sounds/`, `audio/`, `voice/`,
  `ambience/` отсутствуют. Использующий Sound.PlayAt effect уже существует и сохраняется.

## Где лежат звуки

`C:/Users/kxyr0/AppData/Roaming/PrismLauncher/instances/Форма Мысли/minecraft/story/content-pack/jdh/sounds`

`sounds/door_knock.wav` → `jdh:door_knock`.
`sounds/interior/door_knock.ogg` → `jdh:interior/door_knock`.
Namespace — имя папки внутри `content-pack`; path — относительный путь внутри `sounds`
без последнего расширения. Регистр — lowercase. Вызов с `.wav`/`.ogg`/`.mp3` в ID ошибочен.
Одинаковый ID в нескольких форматах запрещён: AUDIO_ASSET_AMBIGUOUS показывает все пути.

Форматы: WAV через Java Sound (поддерживаемые JVM WAV codecs), OGG Vorbis через Minecraft,
MP3 через существующий JLayer. Другие контейнеры/кодеки не заявляются.
Требуется Echo Voice 0.2.1+: общие WAV/OGG codecs проверяют duration во время чтения
PCM chunks. MP3 уже ограничивал накопление по каждому frame. Копии декодеров в Audio нет.

## Команды

Простейший случай — локальный non-spatial one-shot текущему игроку:

```csharp
Sound.Play("jdh:door_knock");
Sound.Play("jdh:door_knock", volume: 0.7);
Sound.Play("jdh:door_knock", pitch: 0.9);
Sound.Play("jdh:door_knock", volume: 0.8, pitch: 0.95);
```

Параметры необязательны: volume=1.0, pitch=1.0. Диапазоны: volume 0..1, pitch 0.5..2.
Отрицательные, нечисловые, NaN/Infinity и выход за диапазон отвергаются, без silent clamp.
Сценарий принимает числовые литералы; вычисляемые выражения здесь не исполняются.

```csharp
Sound.Loop("jdh:rain_room");
Sound.Loop("jdh:door_knocking", duration: Seconds(8));
Sound.Loop("jdh:rain_room", as: Rain, fadeIn: Seconds(2));
Sound.Stop(Rain);
Sound.Stop(Rain, fadeOut: Seconds(2));
Sound.Loop("jdh:rain_room", duration: Seconds(10), fadeIn: Seconds(2), fadeOut: Seconds(1));
```

Используется `duration:`, не `for:`. Seconds принимает литерал 0..3600, округление вверх
до gameplay tick; duration должна быть положительной. Fade неотрицателен и не длиннее
явной duration. FadeOut на timed loop включён в duration, а не добавляется после неё.
Обычная остановка немедленная. Loop повторяет весь PCM в streaming cursor, без Play по таймеру.

`as: Rain` создаёт semantic name в пределах execution + audience. Stop не принимает ResourceId
и не останавливает чужие playbacks того же файла/имени. Повтор имени у того же владельца,
пока оно активно, отклоняется. Для одновременного запуска одного ресурса можно не давать имя
или использовать разные имена. Внутренний AudioPlaybackId — UUID, не arbitrary integer.

## Позиционный звук

```csharp
Sound.PlayAt("jdh:door_knock", BedroomDoor);
Sound.LoopAt("jdh:door_knocking", BedroomDoor, duration: Seconds(6), volume: 0.8);
```

Используется существующий Scene location catalog: JSON в `data/<namespace>/echo_scene/locations`.
Scene binder разрешает typed symbol BedroomDoor в namespaced location ID. Story получает то же
место через lookup-событие; допустима также строка `"jdh:bedroom_door"`. Нет второй registry.
Неизвестное/неоднозначное место даёт AUDIO_LOCATION_NOT_FOUND.

Spatial PCM приводится к mono, linear attenuation — 32 блока. Local PCM сохраняет каналы.
Аудитория — текущий игрок Scene/Story, включая spatial-звук личной сцены. Это не broadcast
всем игрокам. При неоднозначном multiplayer-контексте команда отклоняется; first-player fallback
не используется (единственный подключённый игрок допустим как локальный authoring-контекст).
Каждый клиент должен иметь те же файлы content pack: audio payload не передаётся по сети.

Прямые XYZ и actor-attached PlayFrom/LoopFrom в EA1 не реализованы.

## Blocking и жизненный цикл

Play/Loop/PlayAt/LoopAt — FIRE_AND_CONTINUE. Явное ожидание:

```csharp
Sound.PlayAndWait("jdh:long_creak", pitch: 0.75);
Sound.LoopAndWait("jdh:rain_room", duration: Seconds(3));
```

PlayAndWait заканчивается по terminal state backend, а не по исходной длительности файла.
Pitch меняет длительность one-shot, но не gameplay deadline timed loop.
Декодирование должно закончиться до запуска. Блокирующий вызов ждёт terminal acknowledgement
клиента; очередь загрузки не считается завершением звука. Сбой старта/декодирования завершает
ожидание с ошибкой.

Gameplay ticks следуют pause/tick policy Minecraft sound engine, независимо от Echo Time HH:mm,
его ускорения и rewind. В multiplayer обычное открытие меню не останавливает серверную игру.
Это tick-clock, а не отдельный wall-clock timer: при низком TPS/задержках время может растянуться.

Loop принадлежит запустившей execution и останавливается при её завершении/отмене; чужие
звуки не затрагиваются. One-shot без явного wait продолжает естественно. Если последняя команда
Scene/Story — nonblocking Loop, execution сразу завершится и её loop будет убран: держите
execution активной другой командой либо используйте LoopAndWait.

Нет persistent/world ambience и восстановления бесконечной петли после reconnect.
Disconnect/dimension change очищает presentation. После crash незавершённый Story audio action
получает FAILURE/AUDIO_PLAYBACK_INTERRUPTED, а не бесконечный retry. Завершённые durable
Scene/Story steps повторно не исполняются; незавершённый Scene step подчиняется Scene recovery.

Весь SFX идёт через Minecraft BLOCKS, учитывает master/category settings и существующий
ConcentrationSoundDucker (он меняет MASTER). Речевые lanes и foreground Voice не изменяются.

## Полные примеры

```csharp
class DoorAudio : SceneResource
{
    Scene Start()
    {
        Sound.PlayAt("jdh:door_knock", BedroomDoor);
        Sound.LoopAndWait("jdh:rain_room", duration: Seconds(3), volume: 0.5);
    }
}
```

BedroomDoor должен существовать в location catalog. Scene не имеет общего `Wait(Seconds(...))`
в текущем standard registry, поэтому такой несуществующий вызов здесь не предлагается.

```csharp
class AudioStory : StoryResource
{
    OnStart()
    {
        Sound.Play("jdh:door_knock", volume: 0.7);
        Sound.LoopAndWait("jdh:rain_room", duration: Seconds(3));
    }
}
```

Observation OnComplete использует те же Sound-команды для one-shot и finite nonblocking loop.
Этот линейный completion block не поддерживает await; бесконечный Loop и AndWait отклоняются.
Для длительной управляемой последовательности можно использовать существующий Story.Start,
внутри StoryResource — тот же Sound DSL. Отдельного ObservationSound/StorySound API нет.
Trigger может запускать такую Story через свой существующий action mechanism.

```csharp
class AudioObservation : ObservationResource
{
    Observation Door()
    {
        OnComplete()
        {
            Sound.Play("jdh:door_knock", volume: 0.7);
            Sound.Loop("jdh:rain_room", duration: Seconds(3));
        }
    }
}
```

Для production Observation добавьте размещение/anchor обычным authoring-инструментом.

## Загрузка, кеш и reload

Вся scan/hash/decode/mono работа идёт на одном daemon worker, не на render/main thread.
Короткие звуки кешируются по ResourceId + SHA-256 revision. Concurrent plays декодируют
revision один раз, пока она в кеше. LRU PCM budget — 64 MiB (local + mono), active playback
удерживает старую immutable revision до конца. Cursor не копирует полный PCM при каждом play.
Есть Java API `AudioCatalog.preload(AudioResourceId)`; обязательной команды preload для автора нет.

Лимиты EA1: compressed file ≤16 MiB, decoded duration ≤120 секунд, PCM должен помещаться
в budget. Это короткие SFX/ambience; disk streaming для длинной музыки не реализован.
Stream playback читает уже декодированный PCM порциями. Кешированные активные revisions могут
временно добавить память сверх LRU budget; pinned PCM ограничена 128 MiB, concurrency —
128 presentation на клиенте. Очередь загрузки имеет timeout 600 gameplay ticks; Stop/world change
отменяет ещё не начавшуюся работу и запрещает late callback запускать старый звук.

F3+T/resource-pack reload и `/echo_audio reload` используют тот же reload entry point.
Нет filesystem polling каждый tick и отдельного watcher. Следующий play получает новую revision;
играющий sample остаётся прежним. При corrupt replacement закешированная предыдущая LKG
сохраняется с диагностикой. Если старая PCM уже вытеснена и исходный файл перезаписан, восстановить
её невозможно: новая загрузка выдаст ошибку. Замена во время decode не отравляет revision cache.

## Content Doctor и ошибки

Doctor получает AudioAssetDescriptor из того же scanner и AUDIO references из Scene/Story
compiler; completion blocks Observation проверяет общий AudioScriptInspection над EchoScript AST.
Missing asset — AUDIO_ASSET_NOT_FOUND с подсказкой похожего ID; ambiguity и
unsupported format — ошибки. Volume/pitch/duration/fade проверяет общий binder.
Offline Doctor не декодирует Forge/Vorbis PCM: corrupt codec диагностируется async runtime.
Runtime source provenance сохраняет исходный файл, строку и столбец команды.

Основные коды: AUDIO_ASSET_NOT_FOUND, AUDIO_ASSET_AMBIGUOUS, AUDIO_FORMAT_UNSUPPORTED,
AUDIO_DECODE_FAILED, AUDIO_VOLUME_INVALID, AUDIO_PITCH_INVALID, AUDIO_DURATION_INVALID,
AUDIO_FADE_INVALID, AUDIO_LOCATION_NOT_FOUND, AUDIO_PLAYBACK_NOT_FOUND,
AUDIO_BACKEND_UNAVAILABLE, AUDIO_PLAYBACK_START_FAILED.

## Debug

Клиентские команды в текущем мире:

```text
/echo_audio list
/echo_audio play jdh:door_knock
/echo_audio status
/echo_audio stop <playback-uuid>
/echo_audio reload
```

List показывает ID, format, source file и duration/cache metadata для декодированных звуков.
Status показывает identity, owner, loop, position, volume, pitch, elapsed/remaining ticks,
state и source. Debug play — локальный one-shot с defaults; не server broadcast.
Reload complete означает завершение прохода: ошибки нужно смотреть в list/log.

## Проверки и установка

Финальный последовательный прогон: `echo-audio/tools/Verify-EA1.ps1`.
У всех шести проектов прошли check/build или production jarJar, включая parser/binder,
WAV/OGG/MP3 fixtures, bounded decode, cache, playback envelope/stream, spatial SoundInstance
parameters, owner/audience isolation, Scene/Story/Observation integration, Doctor, hot reload,
sourceShapeCheck и действующие Checkstyle/PMD/architecture gates.

| Проект | Тестов | Failures / errors / skipped |
|---|---:|---|
| echo-voice | 25 | 0 / 0 / 0 |
| echo-audio | 27 | 0 / 0 / 0 |
| echo-scene | 7 | 0 / 0 / 0 |
| echo-observation | 21 | 0 / 0 / 0 |
| echo-story-engine | 202 | 0 / 0 / 0 |
| echo-content-doctor | 22 | 0 / 0 / 0 |
| Всего | 304 | 0 / 0 / 0 |

Пять Forge JAR установлены в Prism `Форма Мысли/minecraft/mods` через проверенные
`.deploying` и rename. Post-deploy scanner проверил mod IDs и SHA-256: active duplicates=0.
Старые версии — вне active mods, в
`minecraft/mod-backups/ea1-20261019-183654`.
Content Doctor 0.2.0 — отдельный CLI-инструмент; в Minecraft mods он не устанавливается.

| Production JAR | SHA-256 |
|---|---|
| echo-audio-0.1.0-all.jar | ef02fc2d88462e5550c81f6e6fe2fb10e2c559e7d60c288ac2de7407301d9828 |
| echo-voice-0.2.1-all.jar | 84a0b488d8e1e5bc4ba8089f83749307a8ae81973179807342e8ed165e311da5 |
| echo-scene-0.2.0-all.jar | f81b068aae20c5a5bae4d2ddd9990c612cdd54161b5e5714b8dbc275c38f4c42 |
| echo-observation-1.3.0-all.jar | 57cc7875a23c12ac269e37e229df5a5b0443c34f3d39772e667c204fc0d0d34e |
| echo-story-engine-1.11.0-all.jar | c8d85a2f845856da1430717692e34c915ed1aaed6f4b8c984ae8a04cbd4a8035 |

Все четыре потребителя содержат одинаковый Jar-in-Jar `echo-audio` library:
`49a897bdc01675bbdfb1f880355e5d59ad4e70169067b97b3c28361d29012e02`.
Он дедуплицируется Forge по Maven identity. Codec implementation остаётся в Echo Voice.
Машинный отчёт: `D:/JDHSeason/mods/echo-audio/build/reports/ea1/release.json`.

В live sounds/ea1 добавлены только три собственных generated test tones:
test_tone.wav, test_loop_ogg.ogg, test_loop_mp3.mp3. Пользовательский story/content не удалялся.
Быстрая проверка после запуска мира:

```text
/echo_audio play jdh:ea1/test_tone
```

LIVE_SMOKE_NOT_RUN: headless tests не подтверждают слышимость, направление на реальном
устройстве или субъективное качество loop seam. Для этого нужны Minecraft world и прослушивание.

## Звуки в DialogueResource (20.10.2026)

Echo Dialogue 0.15.0 с Echo Audio 0.2.0 поддерживает те же семь команд `Sound.*`
в методах узлов диалога. Команда исполняется после подтверждения предыдущей реплики,
перед показом следующей. Несколько команд в одной позиции идут в порядке исходника.

```csharp
Line("john", "Нужно разобраться с этим сном.");
Sound.Play("jdh:door_knock");
Line("sasha", "Лололошка? Ты что, проснулся?");
```

`Sound.Play` запускает звук и продолжает диалог сразу; используйте
`Sound.PlayAndWait("jdh:door_knock")`, чтобы следующая реплика появилась после окончания звука.
Поддерживаются `Loop`, `LoopAt`, `PlayAt`, `LoopAndWait` и `Stop` с общими параметрами.
У `LoopAndWait` обязательна конечная `duration: Seconds(...)`.
`PlayAt`/`LoopAt` разрешают локацию через существующий каталог Scene.
Звук получает конкретный игрок диалога. Имена `as:` и `Sound.Stop` изолированы сессией.
Завершение/отмена диалога останавливает принадлежащие ему петли и ожидаемые звуки;
обычный короткий `Play` может доиграть. Поздняя ошибка такого звука записывается в лог.
Ошибка ожидаемого звука отменяет диалог, не оставляя его в вечном ожидании.
Команды после Choice/Complete/перехода отклоняются при компиляции.

Повторная доставка одной ожидающей команды не запускает второй звук; повторный визит
в тот же узел считается новым исполнением. При восстановлении сохранённого звукового
ожидания после перезапуска диалог отменяется с `AUDIO_PLAYBACK_INTERRUPTED` в логе:
аудио не воспроизводится заново без подтверждения. Воспроизведение проверено автоматическими
тестами; проверка звука в запущенном Minecraft для этого изменения не выполнялась.

Проверки интеграции: Echo Dialogue — 197 unit-тестов, Echo Audio — 33;
ошибок, падений и пропусков нет. Из них 4 новых проверки компиляции диалогов
(включая полный AfterDreamDialogue) и 6 проверок исполнения/отмены/повторной доставки.
Сборка Audio: `gradlew.bat check :audio-forge-1.20.1:jarJar --max-workers=2`.
Сборка Dialogue: `gradlew.bat check :dialogue-forge-1.20.1:jarJar --max-workers=2`.

Установлено 20.10.2026 в основной Prism-экземпляр «Форма Мысли»; проверено отсутствие
дубликатов mod ID и совпадение SHA-256 с production-сборками:

- `echo-dialogue-0.15.0-all.jar` — `c8cf2b17544d42d7357bb2136983b037c82f7327727f691d41f4eab2efc0c389`.
- `echo-audio-0.2.0-all.jar` — `433b2687734c1b73511a3571a3104e5aef59e10f5854dc149e64cf23288464c5`.

Резервные JAR сохранены вне активного mods в `minecraft/mod-backups/ea1-20261020-192542`.
Общая библиотека Audio сохранена без изменения версии и содержимого; повторных копий классов
в runtime-адаптерах нет. Проверка звука в Minecraft: `LIVE_SMOKE_NOT_RUN`.
