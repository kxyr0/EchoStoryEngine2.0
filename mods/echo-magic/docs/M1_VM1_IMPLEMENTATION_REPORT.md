# Отчёт M1/VM1

Дата автоматической проверки: 2026-08-02.

## Репозитории и артефакты

### Echo Magic 0.1.0 (`echo_magic`)

- Репозиторий: `D:/JDHSeason/mods/echo-magic`.
- Модули: `magic-contracts`, `magic-core`, `magic-forge-api-1.20.1`, `magic-forge-1.20.1`, `magic-script-adapter-1.20.1`, `magic-testmod-1.20.1`.
- Production JAR: `D:/JDHSeason/mods/echo-magic/magic-forge-1.20.1/build/libs/echo-magic-0.1.0-all.jar`.
- Зависимости: Java 17, Minecraft 1.20.1, Forge 47.4.10, EchoScript 0.1.1-SNAPSHOT.
- Ключевые production classes: `EchoMagicMod`, `MagicService`, `EchoScriptSpellCompiler`, `ForgeCastRuntime`, `MagicProjectileEntity`, `MagicDamageExecutor`, `MagicBillboardParticle`.

### Echo Void Magic 0.1.0 (`echo_void_magic`)

- Репозиторий: `D:/JDHSeason/mods/echo-void-magic`.
- Модули: `void-magic-forge-1.20.1`, `void-magic-content`, `void-magic-testmod-1.20.1`.
- Production JAR: `D:/JDHSeason/mods/echo-void-magic/void-magic-forge-1.20.1/build/libs/echo-void-magic-0.1.0.jar`.
- Зависимости: Minecraft 1.20.1, Forge 47.4.10 и только публичные `magic-contracts`/`magic-forge-api-1.20.1` версии 0.1.0.
- Ключевые production classes: `EchoVoidMagicMod`, `VoidMagicParticles`, `VoidMagicCommands`, `VoidMagicIds`.
- Java-классов `VoidDaggersSpell` и `VoidDaggerEntity` нет. Spell и projectile реализованы definition/runtime generic engine.

Оба каталога инициализированы как отдельные Git-репозитории. Проверка production JAR не обнаружила bundled `net/minecraft`, testmod или Void-классов в engine и не обнаружила bundled Echo Magic implementation в Void JAR.

## Asset

- Исходник: `D:/JDHSeason/assets/void_circle.png`.
- Target: `D:/JDHSeason/mods/echo-void-magic/void-magic-forge-1.20.1/src/main/resources/assets/echo_void_magic/textures/particle/void_circle.png`.
- ResourceLocation particle/texture: `echo_void_magic:void_circle` / `echo_void_magic:textures/particle/void_circle.png`.
- Размер: 256×256; PNG bit depth 8, color type 6 (RGBA, alpha поддерживается).
- SHA-256 обоих файлов: `7c783d4aab755642b503eb7cc6c341dc35ae34d14ac781f66e6d75b091d1e398`.
- Particle зарегистрирован через `MagicBillboardParticleType`, `VoidMagicParticles` и client provider; tint задаётся runtime-опциями, рендер additive с восстановлением render state.

## Spell definition

- Spell ID: `echo_void_magic:void_daggers`.
- Файл: `../echo-story-content-pack/src/main/resources/data/jdh/echo_magic/spells/void/void_daggers.es`.
- Parser: PASS; Init-only binder: PASS; validator: PASS. Интеграционный тест читает этот фактический sibling resource; skipped = 0.
- Compiled revision SHA-256: `2d80e9c04602a0d165627aa97df34ce9e671c7d8159e751b8bebe7aeefbbe855`.
- count 3; interval 2 ticks; speed 1.6 block/tick; range 24 blocks; radius 0.22; damage 2.0; cooldown 16 ticks.

## Cast evidence

Server runtime разрешает `PlayerRef` и selected `SpellId`, проверяет known/cooldown, фиксирует revision на cast и выдаёт accepted/rejected packet. Для детерминированного автоматического примера:

- PlayerRef: `00000000-0000-0000-0000-000000000001`.
- selected: `echo_void_magic:void_daggers`.
- accepted tick: 100; sequence: 0.
- SpellCastCommandId: `793f4a5fc1fc91b56d479de5c37351c7`.
- SpellCastId: `659ae44f7c0bc6580d0c5683d02118a6`.
- SpellProjectileIds: `5623a23389c297a932abbc0321659b84`, `9a29cd4ce8a750ddaab5debbb2302168`, `c232c925289668b0aa4bf5c744ab2a3a`.
- Emission ticks: 100, 102, 104; cooldown ready tick: 116.

ID вычислены тем же SHA-256 contract, который покрыт `DeterministicMagicIdsTest`; burst/revision pinning покрыты `SpellCastInstanceTest`.

## Projectile evidence

- Runtime origin: player eyes + server look × 0.35; direction: нормализованный server look.
- На каждом server tick хранится previous/current position; collision использует весь swept segment.
- Сначала вычисляется block clip, затем кандидаты living entity по расширенным radius AABB. Выбирается минимальная дистанция вдоль segment; при равенстве entity ID служит стабильным tie-breaker. Более близкий block прекращает projectile.
- Пройденная дистанция накапливается и сравнивается с range; terminal results разделены на entity hit, block hit и exhausted range.
- Численный тест: segment `(0,0,0) → (1.6,0,0)`, тонкая AABB около x=0.8, inflate 0.22; первое пересечение = 0.35625 доли segment. Это не endpoint-only тест.

Конкретный block/entity target и terminal result из живого мира должны быть добавлены после ручного Minecraft-прогона; unit evidence не выдаётся за runtime capture.

## Damage evidence

- Damage type: `echo_void_magic:void_magic`; amount: 2.0.
- `DamageSource` создаётся из data-driven holder; direct entity — generic `MagicProjectileEntity`, caster — `ServerPlayer`, target — выбранный `LivingEntity`; authoritative вызов — server-side `target.hurt(...)`.
- Receipt содержит projectile/cast/target/type/amount/tick/hurt result. `MagicHitLedgerTest`: первая запись = true, повтор той же receipt = false.
- Пример unit receipt: projectile `projectile`, cast `cast`, target `00000000-0000-0000-0000-000000000002`, type `test:magic`, amount 2, tick 20, hurt result true.

Фактический target UUID и `hurt` result из Minecraft-мира требуют ручного прогона.

## Particle evidence

- Texture: `echo_void_magic:textures/particle/void_circle.png`; radial green texture, trail tint `#0F6B32`, core `#45FF4A`.
- Trail spacing 0.18, size 0.30, lifetime 8 ticks; sampling хранит остаток расстояния, поэтому плотность не зависит от FPS.
- Packets per trail point = 0: сервер синхронизирует projectile/impact, точки trail создаются клиентом.
- Defaults: 512 spawn/frame и 4096 active. При превышении бюджета пропускается только cosmetic trail; collision/damage не меняются.

## Future spell wheel и removal boundary

Публичные `SpellCatalogView`, `KnownSpellView`, `SelectedSpellView`, `SpellSelectionService`, `SpellCastService` и immutable definition types находятся в Echo Magic API и не зависят от Void. Missing selection очищается при reconciliation. Поиск по production source Echo Magic дал 0 ссылок на `echo_void_magic`, `VoidDaggers` или `void_magic`; source-shape task также запрещает такие ссылки.

Фактический removal smoke test с удалением JAR и запуском dedicated server ещё не выполнен. Архитектурно engine не загружает Void-классы, а production JAR verifier подтверждает отсутствие их байткода, но это не подменяет требуемый runtime test.

## Автоматическая проверка

- `echo-magic`: `gradlew check build` — PASS, 59 tasks; 13 tests, 0 skipped, 0 failed.
- `echo-void-magic`: `gradlew check build` — PASS, 26 tasks; production JAR verification PASS.
- `echo-platform-docs`: `pnpm check` — PASS; Vitest 3/3, Astro 99 pages, UI language 99 files, Pagefind 98 indexed pages.
- Java files > 180 logical LOC = 0.
- EchoScript files > 180 logical LOC = 0.
- Leaf Java packages > 7 files = 0.
- Leaf content folders > 7 files = 0.
- Dynamic `Map<String,Object>`/reflection hits в production Echo Magic = 0.

## Ручная приёмка

Не выполнены без отдельного интерактивного запуска Minecraft: first/third person, FPS 30/60/144, GUI Scale 2/3/4, светлый/тёмный фон, test arena hits, dedicated-server startup и физическое удаление Void JAR. Поэтому пункт DoD «Manual Minecraft acceptance проведён» остаётся открытым; все перечисленные выше PASS относятся только к воспроизводимым автоматическим проверкам.
