# История изменений

## 0.8.17

- Reduced additive spell brightness by 12%; deepened blood tint and warmed fire tint.
- Updated standard blood rapier and fire summoned-weapon colors while preserving other school hues.

## 0.8.16

- Enlarged central trail circles with deterministic spatial noise (1.30-2.10 times spell-defined size) for varied overlapping lobes without frame flicker.
- Retained small soft edge particles, school colors, dense spacing and original technique trajectories.

## 0.8.15

- Denser overlapping projectile trails retain the original circle, arc and line trajectories and school-specific textures/colors.
- Larger central particles and smaller dim side particles smooth the trail perimeter.
- Smoothstep alpha and size decay replace linear fading for all Echo billboard particles.

## 0.8.14

- Restored school-specific projectile particle styles, colors, core styles and configured thickness.
- Removed the universal ribbon interception that made all spell techniques look alike.
- Weapon ribbons preserve their own hue with alpha compositing, a stronger colored body and subdued white core.

## 0.8.13

- Fixed blending to respect soft edge alpha.
- Trails contain one colored BODY and one white CORE (22.5% of BODY width), with no outer glow or source cluster.
- Reduced BODY width by 40%, tapered both ends and halved CIRCLE technique thickness.

## 0.8.12

- Magic projectile trails now use continuous additive ribbons with a 40% wider colored body, soft outer glow and a white core.
- Added a travelling pulse, 7.5% width variation, a brief source cluster near the caster and rare detached motes.
- Ribbon samples share joins, expire with the configured trail lifetime and respect the particle budget; vanilla particle mode remains available.

## 0.8.11

- Added layered school-specific cast sounds and distinct projectile impact sounds.

## 0.8.10

- Line techniques now aim at the nearest visible valid enemy inside a generous forward cone.
- Auto-aim respects the spell range and target-relation adapters; without a target it keeps normal look aim.

## 0.8.9

- The gesture plane now follows player translation, so walking, jumping and knockback cannot bend the rune.
- Captured pen coordinates are clamped inside the visible HUD canvas with room for the preview ring.

## 0.8.8

- Added a translucent live pen-position ring before LMB is pressed and while a stroke is being drawn.
- The preview follows raw aim coordinates while recognition continues using the recorded smoothed path.

## 0.8.7

- Fixed the rune canvas being rendered once per vanilla overlay instead of once per frame.
- Replaced 224 per-overlay scanline fills with 19 constant-cost HUD rectangles.
- The canvas interior now has a true single-pass 5% alpha instead of accumulated opacity.

## 0.8.6

- Made the single HUD rune canvas almost fully transparent with softly rounded corners.
- Removed the title, sample count, similarity and retry labels from the canvas itself.

## 0.8.5

- Removed the duplicate world-space canvas; the stable HUD rune canvas is now the single drawing surface.
- Rebuilt personal handwriting matching around rotation-tolerant dynamic path alignment with relaxed variation.
- Personal training now learns the selected custom glyph instead of rejecting it for differing from the canonical rune.
- School gestures may span consecutive strokes, and Void accepts natural endpoint drift and uneven return speed.
- Training samples, templates and cast gestures now keep identical full-precision coordinates on client and server.
- School recognition is only announced after the authoritative server result, removing contradictory client messages.

## 0.8.4

- Gesture smoothing now pins sharp raw corners and never delays a forced stroke endpoint.
- Raised the adaptive cutoff to remove the heavy trailing feel without giving up jitter reduction.
- Replaced world-canvas and HUD dot chains with continuous layered ribbons and line-drawn markers.
- Void recognition now validates the actual return path, tolerates hand drift and preserves full client/server precision.

## 0.8.3

- Replaced expiring cursor dust with a stable off-centre rune canvas and persistent HUD preview.
- Added adaptive input smoothing, 64-point personal templates, three retained variants, direction/curvature scoring and limited rotation tolerance.
- Added structural candidate filtering, ambiguity rejection, training consistency feedback and a configurable retry key.
- Existing 32-point personal templates are migrated when player data loads.

## 0.8.2

- Third-person gesture particles now render between the active camera and the player instead of being hidden beyond the player model.
- Gesture recognition and server-side casting remain based on the player's actual view direction.

## 0.8.1

- Added three-sample personal handwriting training for the `WEAPON` triangle and fixed the training menu layout.

## 0.8.0

- Added the closed-triangle `WEAPON` gesture technique for school-specific summoned weapons.
- Added a public cross-mod link-cut event and summoned weapon visual-form metadata.

## 0.7.0

- Forge API получил универсальный NBT-контракт `SummonedWeaponData` для временного настоящего оружия.
- Контракт хранит владельца, срок жизни, возвращаемый предмет, цвет/длину particle-клинка и тип движения без зависимости от конкретной школы или Epic Fight.

## 0.6.0

- Изменения, создание, переименование и удаление `.es` в `story/content-pack/**/echo_magic/spells/` теперь автоматически подхватываются без `/reload`.
- Несколько быстрых сохранений объединяются в одну пересборку каталога; проверка выполняется на server thread без фонового доступа к Minecraft API.
- Ошибка компиляции или конфликт Spell ID оставляет активной последнюю рабочую версию каталога и показывает уведомление администраторам.

## 0.5.4

- Одинаковое `.es` определение больше не попадает в catalog дважды, если внешний JDH content pack одновременно зарегистрирован Story Engine как Minecraft resource pack и просканирован fallback-загрузчиком Echo Magic.
- Отличающиеся определения с одинаковым Spell ID по-прежнему не склеиваются и отклоняются как настоящий конфликт.

## 0.5.3

- Убрана фиолетовая примесь `reverse_portal` из предпросмотра Пустотного шага и обмена позициями.
- Все частицы зон и перемещения теперь используют только ярко-зелёный центр и тёмно-зелёный край.

## 0.5.2

- Добавлен общий безопасный resolver короткого телепорта на 6 блоков: препятствия, пол, коллизии сущности, занятая зона и граница мира проверяются одинаково на клиенте и сервере.
- При распознавании удерживаемой точки школы Пустоты клиент заранее показывает зелёную круговую зону назначения.
- Preview появляется до завершения жеста и исчезает при движущемся штрихе или отсутствии безопасной точки.

## 0.5.1

- Пустотный `HOOK + точка` теперь отмечает цель и безопасно меняет её местами с заклинателем.
- Обмен проверяет размеры обеих сущностей, коллизии блоков, границу мира, транспорт и свободное место в обеих точках.
- Нестабильная задержка всего server thread заменена настоящим замедлением тиков всех неигровых живых существ до 20% скорости.
- В одиночной игре клиентская анимация существ замедляется синхронно с серверной логикой; сам игрок продолжает рисовать с обычной скоростью.

## 0.5.0

- Добавлена самостоятельная техника `POINT`: удержание ЛКМ с почти неподвижным прицелом не меньше 0.45 секунды.
- Одиночная `POINT` не конфликтует с точкой, нарисованной вторым штрихом как модификатор задержки.
- `MagicLifecycleEvent` получил `markHandled()`: content-мод может исполнить утилитарное заклинание сам, не создавая фиктивный projectile.

## 0.4.4

- Экран `Эхо магии → Обучить личный почерк` позволяет выбрать любую школу или технику и нарисовать её три раза.
- Три образца нормализуются по положению и масштабу, усредняются в персональный шаблон и получают допуск, учитывающий различия между примерами.
- Персональный шаблон проверяется раньше встроенной геометрии, а при несовпадении движок автоматически использует прежние правила.
- Шаблоны хранятся сервером отдельно для каждого игрока и мира, синхронизируются клиенту и переживают перезаход.

## 0.4.3

- Удерживаемая точка теперь определяется по устойчивому центру движения, а не по суммарной длине микродвижений мыши.
- Допускаются небольшой дрейф прицела и один случайный рывок, включая рывок непосредственно перед отпусканием ЛКМ.
- Область постановки точки около конца основной фигуры немного расширена; настоящий движущийся штрих точкой не считается.

## 0.4.2

- Добавлена техника `HOOK`: длинный штрих сверху вниз с загнутым вверх концом, похожий на `J`.
- `HOOK + удерживаемая точка` использует `LINE`-заклинание активной школы и связывает врага перед игроком на расстоянии до 18 блоков.
- Связь видна как цветная линия частиц, занимает active cast slot и сразу запускает cooldown.
- Связь срабатывает, когда цель погибает/удаляется либо отдаляется от точки привязки ещё на 3 блока. Выстрел перенаправляется к последнему положению цели.
- Неактивированная связь исчезает через 20 секунд и отменяется при смерти, выходе или смене измерения владельца.
- Добавлены локализованные причины отказа для крюка без точки и отсутствующей цели.

## 0.4.1

- Удерживаемая неподвижная точка отдельным штрихом теперь распознаётся как боевой модификатор задержки.
- `LINE/ARC + точка` создаёт видимую малую метку и выпускает атаку через 2 секунды из сохранённых позиции и направления.
- `CIRCLE + точка` ставит цветную печать в трёх блоках перед игроком. Она взводится через 0.5 секунды, живёт 30 секунд и выпускает круговую атаку при входе допустимого врага.
- Развилка распознаётся как `V` вторым штрихом через конец основной формы либо как две отдельные расходящиеся ветви.
- Модификатор развилки утраивает projectile burst и мгновенно разводит его по трём направлениям; существующие дуги также получают три ветви.
- Отложенные атаки и печати занимают server-authoritative cast slot, сразу запускают cooldown и отменяются при смерти, выходе или смене измерения владельца.

## 0.4.0

- Выбор школы отделён от применения техники: `N` выбирает и запоминает школу, `V` рисует заклинание активной школы.
- Рисование идёт только при удержании ЛКМ. Отпускание ЛКМ завершает штрих, но не выпускает магию; пока `N` или `V` удерживается, можно начать следующий штрих.
- Составной рисунок передаётся на сервер как список отдельных штрихов без ложных соединительных линий. Это основа для модификаторов `точка` и `развилка`.
- Фиолетовый след убран: выбор школы рисуется светлым нейтральным цветом, техника — цветом активной школы.
- Обе клавиши переназначаются в стандартном меню управления; из экрана `Эхо магии` добавлен прямой переход к настройке клавиш.
- Клиент и сервер получают явный режим жеста, поэтому круг техники больше не может случайно выбрать школу Воздуха.

## 0.3.4

- Большой замкнутый круг распознаётся как `CIRCLE` сразу после отпускания клавиши, без удержания в конечной точке.
- Маленькая петля остаётся символом школы Воздуха; большой круг без активной школы больше не выбирает Воздух.
- Action bar теперь подсказывает разные жесты для выбора школы и применения техники.

## 0.3.3

- Стандартный масштаб Echo billboard-частиц уменьшен до `70%`.
- Стандартный масштаб ванильной цветной пыли увеличен до `300%`.
- Оба масштаба можно независимо переключать в локализованном экране настроек магии.

## 0.3.2

- DSL принимает ванильные simple particle ID и цветную `minecraft:dust` без регистрации Java-частицы.
- `Trail` и `Core` получили необязательный `vanillaColor`; разные particle ID для trail/core теперь исполняются корректно.
- В локализованные настройки добавлена мгновенная замена всех частиц магии на ванильную цветную пыль.

## 0.3.1

- Пологая дуга отделена от линии более строгой проверкой отклонения.
- Быстрая петля выбирает Воздух; замкнутая петля с удержанием от 280 ms вызывает `CIRCLE`.
- Добавлены `PLAYER_CENTER`, `PLAYER_HORIZONTAL_LOOK` и синхронный burst для круговых областей.

## 0.3.0

- Добавлены техники `LINE`, `ARC` и большой `CIRCLE` с серверной перепроверкой жеста.
- DSL получил `Technique(...)`, веер `spread: Degrees(...)` и эффекты `Ignite`, `Extinguish`, `Knockback`, `DeflectProjectiles`.
- Projectile применяют горение, тушение и направленное отталкивание; воздушные дуги отклоняют стрелы.

## 0.2.2

- Распознавание школ стало строже, чтобы линия техники не переключала школу из-за небольшого дрожания камеры.
- Выбор Огня требует аккуратного вертикального штриха длительностью не меньше 0.32 секунды; быстрый штрих остаётся техникой активной школы.
- Cooldown теперь сохраняется как оставшаяся длительность, а не абсолютный серверный tick.
- Старые абсолютные cooldown автоматически отбрасываются при миграции, устраняя вечный cooldown после перезапуска мира.
- Клиентский индикатор cooldown теперь отсчитывает время, а состояние runtime сбрасывается между мирами.

## 0.2.1

- Символ школы теперь выбирает и запоминает школу, но не вызывает заклинание сразу.
- Прямой штрих после выбора школы применяет технику этой школы; линия может быть вертикальной, горизонтальной или диагональной.
- Выбранная жестом школа сохраняется в данных мира, синхронизируется клиенту и переживает перезаход.
- Добавлены отдельные сообщения выбора школы, отсутствующей школы и нераспознанной техники.
- Если активна другая школа, жест `↑` сначала переключает её на огонь; если огонь уже активен, тот же прямой штрих выполняет огненный cast.

## 0.2.0

- Клавиша `V` переведена из мгновенного cast в удерживаемый режим рисования символа в воздухе.
- Заклинание отправляется на применение только после отпускания клавиши и успешного распознавания жеста.
- Клиентский результат повторно проверяется server-authoritative распознавателем по ограниченному набору точек.
- Добавлены символы школ: огонь `↑`, вода `~`, земля `⊥`, воздух `○`, кровь `•`, пустота `↔`.
- Добавлен светящийся world-space след и цветное подтверждение распознанной школы.
- Добавлен экран `Эхо магия` и настройка замедления времени во время рисования.
- Замедление разрешено по умолчанию только для integrated server; dedicated server защищён отдельной server-настройкой.
- Стандартный переназначаемый keybind сохранён, но теперь называется `Рисовать символ магии`.

## 0.1.1

- Добавлен обратносуместимый `MagicParticleMotion` для style-specific движения billboard-частиц.
- Billboard-частицы теперь применяют заданную скорость каждый tick; существующие provider без motion остаются неподвижными.

## 0.1.0

- Добавлены Init-only `SpellResource`, каталог и last-known-good reload.
- Добавлены known/selected spells, cooldown и SavedData.
- Добавлены server cast intent, burst и generic projectile runtime.
- Добавлены swept collision, damage receipts и public API будущего spell wheel.
- Добавлены generic additive billboard particles, trail sampling и budget.
- Добавлены команды, диагностика, тестовый мод и русская документация.
- Применение spell добавлено в локализованные настройки управления Minecraft с переназначаемой default-клавишей `V`.
