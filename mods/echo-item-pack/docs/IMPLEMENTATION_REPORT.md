# Echo Item Pack implementation report

Дата проверки: 2026-07-23.

## Созданные modules

`D:\JDHSeason\mods\echo-item-pack\item-contracts`

- Ответственность: neutral public contracts.
- Dependencies: нет Minecraft, Forge, EchoScript.
- Public API: `EchoItemId`, `ItemKindId`, `ItemDefinition`, `WeaponDefinition`, diagnostics.
- Packaging: Java library, published to `build/integration-maven`.

`D:\JDHSeason\mods\echo-item-pack\item-core`

- Ответственность: EchoScript compiler facade, `WeaponDefinitionBinder`, catalog, manifest, reload, asset validation.
- Dependencies: `item-contracts`, Maven artifact `io.github.kxyr0.echoscript:echo-script-language:0.1.0-SNAPSHOT`, Gson.
- Public API: Java-only runtime classes, no Minecraft/Forge imports.
- Packaging: embedded into Forge production JAR.

`D:\JDHSeason\mods\echo-item-pack\item-forge-api-1.20.1`

- Ответственность: minimal public Forge-facing API boundary.
- Dependencies: `item-contracts`.
- Public API: catalog view, diagnostics view, reload listener summary, item kind binder/factory hooks.
- Packaging: Java library, published to `build/integration-maven`.

`D:\JDHSeason\mods\echo-item-pack\item-forge-1.20.1`

- Ответственность: Forge entrypoint, startup registration, `EchoWeaponItem`, commands, creative tab, server-to-client catalog sync, dynamic client resource pack, packaged resources.
- Dependencies: Forge 1.20.1, `item-contracts`, `item-core`, `item-forge-api-1.20.1`, Maven EchoScript artifact, Gson.
- Public API: Forge mod `echo_item_pack`.
- Packaging: production JAR `D:\JDHSeason\mods\echo-item-pack\item-forge-1.20.1\build\libs\echo-item-pack-0.2.2-all.jar`.

`D:\JDHSeason\mods\echo-item-pack\item-testmod-1.20.1`

- Ответственность: development-only test marker module.
- Dependencies: item public/core modules.
- Public API: none for production.
- Packaging: separate JAR, not embedded in production JAR.

## Ключевые файлы

- `D:\JDHSeason\mods\echo-item-pack\settings.gradle`
- `D:\JDHSeason\mods\echo-item-pack\build.gradle`
- `D:\JDHSeason\mods\echo-item-pack\item-contracts\src\main\java\io\github\kxyr0\echoitem\api\EchoItemId.java`
- `D:\JDHSeason\mods\echo-item-pack\item-contracts\src\main\java\io\github\kxyr0\echoitem\api\definition\WeaponDefinition.java`
- `D:\JDHSeason\mods\echo-item-pack\item-core\src\main\java\io\github\kxyr0\echoitem\core\script\EchoScriptWeaponCompiler.java`
- `D:\JDHSeason\mods\echo-item-pack\item-core\src\main\java\io\github\kxyr0\echoitem\core\script\WeaponDefinitionBinder.java`
- `D:\JDHSeason\mods\echo-item-pack\item-core\src\main\java\io\github\kxyr0\echoitem\core\catalog\WeaponDefinitionCatalog.java`
- `D:\JDHSeason\mods\echo-item-pack\item-core\src\main\java\io\github\kxyr0\echoitem\core\manifest\RegisteredItemManifestStore.java`
- `D:\JDHSeason\mods\echo-item-pack\item-forge-1.20.1\src\main\java\io\github\kxyr0\echoitem\forge\EchoItemPackMod.java`
- `D:\JDHSeason\mods\echo-item-pack\item-forge-1.20.1\src\main\java\io\github\kxyr0\echoitem\forge\client\EchoItemPackClient.java`
- `D:\JDHSeason\mods\echo-item-pack\item-forge-1.20.1\src\main\java\io\github\kxyr0\echoitem\forge\item\EchoWeaponItem.java`
- `D:\JDHSeason\mods\echo-item-pack\item-forge-1.20.1\src\main\java\io\github\kxyr0\echoitem\forge\command\EchoItemCommands.java`
- `D:\JDHSeason\mods\echo-item-pack\item-forge-1.20.1\src\main\java\io\github\kxyr0\echoitem\forge\network\CatalogSyncChannel.java`
- `D:\JDHSeason\mods\echo-item-pack\item-forge-1.20.1\src\main\java\io\github\kxyr0\echoitem\forge\network\WeaponDefinitionNetworkCodec.java`
- `D:\JDHSeason\mods\echo-item-pack\item-forge-1.20.1\src\main\java\io\github\kxyr0\echoitem\forge\reload\ItemPackReloadTicker.java`
- `D:\JDHSeason\mods\echo-item-pack\item-forge-1.20.1\src\main\java\io\github\kxyr0\echoitem\forge\reload\EquippedWeaponRefresher.java`
- `D:\JDHSeason\mods\echo-item-pack\item-forge-1.20.1\src\main\java\io\github\kxyr0\echoitem\forge\resource\ClientItemResourcePack.java`
- `D:\JDHSeason\mods\echo-item-pack\item-forge-1.20.1\src\main\java\io\github\kxyr0\echoitem\forge\resource\ClientItemAssetStore.java`

## EchoScript integration

- Artifact: `io.github.kxyr0.echoscript:echo-script-language:0.1.0-SNAPSHOT`.
- Publication source: sibling repo `D:\JDHSeason\mods\echo-script`, task `prepareEchoScriptArtifacts`.
- Parser entrypoint: `io.github.kxyr0.echoscript.language.EchoScriptLanguage.parser()`.
- Parser contract: `io.github.kxyr0.echoscript.api.EchoScriptParser`.
- AST root: `io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax`.
- Decimal syntax is optional at runtime; authored examples use quoted numeric strings for compatibility with older EchoScript API jars.
- Binder entrypoint: `io.github.kxyr0.echoitem.core.script.WeaponDefinitionBinder`.
- Parser duplication in production JAR: `0` classes under `io/github/kxyr0/echoscript/`.

## External content

Created external authoring files:

- `C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\item-pack\jdh\items\weapons\swords\void_sword.weapon.es`
- `C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\item-pack\jdh\items\weapons\daggers\training_dagger.weapon.es`
- `C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\item-pack\jdh\assets\textures\item\void_sword.png`
- `C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\item-pack\jdh\assets\textures\item\training_dagger.png`

## Working items

`jdh:void_sword`

- Source: `item-forge-1.20.1/src/main/resources/data/jdh/echo_item/items/weapons/swords/void_sword.weapon.es`
- Texture: `item-forge-1.20.1/src/main/resources/assets/jdh/textures/item/void_sword.png`
- Model: `item-forge-1.20.1/src/main/resources/assets/jdh/models/item/void_sword.json`
- Damage: `13`
- Attacks per second: `1.6`
- Durability: `2031`
- Revision in tests: `1` or caller-supplied immutable revision.

`jdh:training_dagger`

- Source: `item-forge-1.20.1/src/main/resources/data/jdh/echo_item/items/weapons/daggers/training_dagger.weapon.es`
- Texture: `item-forge-1.20.1/src/main/resources/assets/jdh/textures/item/training_dagger.png`
- Model: `item-forge-1.20.1/src/main/resources/assets/jdh/models/item/training_dagger.json`
- Damage: `4`
- Attacks per second: `2.4`
- Durability: `180`
- Revision in tests: `1` or caller-supplied immutable revision.

## Hot reload evidence

Automated unit evidence from `ItemReloadPlannerTest`:

- changed scripts = `1`
- parser calls = `1`
- binder calls = `1`
- unchanged parser calls = `0`
- unchanged binder calls = `0`
- malformed candidate keeps previous snapshot revision
- `LAST_KNOWN_GOOD_RETAINED` diagnostic emitted
- valid external `.weapon.es` override wins over packaged fallback
- broken external override keeps the previous known-good external definition
- deleting an external override reveals the packaged fallback on the next valid reload
- same-origin duplicate item IDs reject the whole reload batch
- `ForgeItemGameTests.serverTickReloadsExistingExternalWeapon` writes an external `jdh:training_dagger` override while the GameTest server is running; `ItemPackReloadTicker` applies the live batch with `changed weapon files=2`, `diagnostics=0` in the latest GameTest run because the pending-restart test writes a second valid weapon before the same reload tick
- `ItemPackReloadTicker` refreshes equipped Echo weapons and sends the updated catalog snapshot to all connected players after a successful changed reload

Texture evidence from `AssetValidatorTest`:

- changed textures = `1`
- PNG validations = `1`
- parser calls = `0`
- binder calls = `0`
- core resource reload counter = `1` (does not call Minecraft client resource reload)
- `ClientItemAssetStoreTest.brokenTextureKeepsPreviousGoodAndDeletionRevealsFallback` verifies a valid external PNG is served, a broken PNG keeps previous known-good bytes, and deletion reveals fallback by removing the external override.
- Automatic client calls to `Minecraft.reloadResourcePacks()` are intentionally disabled; item texture/model visual refresh is manual through an explicit client resource reload or client restart.

Model evidence from `AssetValidatorTest`:

- changed models = `1`
- model validations = `1`
- parser calls = `0`
- broken model returns invalid candidate
- `ClientItemResourcePack` is registered through `AddPackFindersEvent` as a required top-position client resource pack.
- `ClientItemResourcePack` serves generated `minecraft:item/handheld` item models from the active catalog without writing JSON into the authoring directory.
- `ClientItemResourcePackTest.packServesGeneratedModelForCatalogItem` verifies `PackResources.getResource(CLIENT_RESOURCES, jdh:models/item/void_sword.json)` returns generated model JSON from the active catalog.
- `ClientItemResourcePackTest.packServesValidExternalTexture` verifies `PackResources.getResource(CLIENT_RESOURCES, jdh:textures/item/void_sword.png)` returns valid external PNG bytes from `<gameDir>/story/item-pack`.
- `ClientItemAssetStoreTest.brokenModelKeepsPreviousGoodAndDeletionRevealsFallback` verifies a valid external model is served, a broken model keeps previous known-good bytes, and deletion reveals fallback.

## Registry evidence

Automated evidence:

- Startup union is covered by `RegisteredItemManifestTest.startupIncludesManifestOnlyMissingDefinition`.
- New live item pending restart is covered by `RegisteredItemManifestTest.liveNewItemBecomesPendingRestart`.
- Manifest atomic write/read is covered by `RegisteredItemManifestTest.manifestWritesAndReadsAtomically`.
- Forge registration code uses `RegisterEvent` and generic `EchoWeaponItem` for each startup ID.
- `ForgeItemGameTests.startupRegistersPackagedWeapons` proves a launched Forge GameTest server registers `jdh:void_sword` and `jdh:training_dagger` as `EchoWeaponItem`.
- `ForgeItemGameTests.commandsParseCoreWorkflows` parses `reload`, `validate`, `pending`, `inspect`, `sources`, and `give` command shapes, then executes `validate`, `inspect`, and `pending`.
- `ForgeItemGameTests.giveCommandCreatesStack` executes `/echo_item give @s jdh:void_sword 2` against a fake player and verifies the stack appears in inventory.
- `ForgeItemGameTests.serverTickReloadsExistingExternalWeapon` proves the same registered `EchoWeaponItem` object receives an external live definition without runtime registry mutation.
- `ForgeItemGameTests.newLiveWeaponIsPersistedAsPendingRestart` creates a new valid `.weapon.es` during a launched server, proves it is not registered after freeze, verifies `/echo_item pending`, and checks `registered-items.json` contains `pendingRegistration=true`.

## Runtime evidence

Automated evidence:

- `WeaponDefinitionCatalogTest.sameCatalogBackedItemIdResolvesNewDefinitionAfterPublish` proves the same catalog-backed ID resolves updated name and damage after publishing a new snapshot.
- `EchoWeaponItem` stores only `EchoItemId` and reads text, damage, speed, durability, rarity, glint, and fire resistance from `WeaponDefinitionCatalog.current(id)`.
- `ForgeItemGameTests.weaponValuesComeFromCatalog` verifies a registered live `EchoWeaponItem` exposes name, damage modifier, speed modifier, durability, enchantability, rarity, glint, and fire resistance from the startup `.weapon.es` catalog.
- `ForgeItemGameTests.serverTickReloadsExistingExternalWeapon` verifies same Java registry object after reload, updated name, updated attack damage modifier, modifier count `1`, same held `ItemStack` object after refresh, player `ATTACK_DAMAGE=7.0`, player `ATTACK_SPEED=3.0`, and a fully charged `Player.attack(target)` reducing a real zombie to `14.0` health or lower.
- `WeaponDefinitionNetworkCodecTest.snapshotRoundTripsWeaponDefinitions` verifies the server-authoritative catalog snapshot binary packet preserves text, visuals, combat values, durability, rarity, creative metadata, revision, fingerprint, and availability.
- `ItemDefinitionSynchronizerTest.clientSnapshotUpdatesMirrorAndCatalog` verifies an incoming client snapshot updates both the sync mirror and the `WeaponDefinitionCatalog` that client `EchoWeaponItem` instances read.
- `CatalogSyncChannel` sends a full catalog snapshot on player login and after successful command/ticker reloads; `/echo_item reload` also refreshes equipped Echo weapon attributes before syncing clients.
- Client snapshot application updates the mirrored catalog without scheduling a Minecraft resource reload, so joining a world does not reload all client textures.

Not yet manually proven:

- manual Minecraft client/server hit damage after hot reload without re-equip;
- manual Minecraft client attack cooldown UI change without re-equip;
- manual client visual refresh in inventory, main hand, and third person.

## Dependency scans

Observed module dependency trees:

- `item-core` depends on `item-contracts`, `io.github.kxyr0.echoscript:echo-script-language:0.1.0-SNAPSHOT`, Gson.
- `item-forge-api-1.20.1` depends only on `item-contracts`.
- `item-forge-1.20.1` depends on item modules, Forge, Maven EchoScript artifact, Gson.

Verified counts:

- forbidden imports in `item-contracts` and `item-core`: `0`
- duplicate EchoScript classes in production JAR: `0`
- other Echo engine implementation classes in production JAR: `0`
- testmod classes in production JAR: `0`
- GameTest classes in production JAR: `0`
- production JAR entries: `131`
- production JAR size: `221725` bytes
- absolute Windows paths inside text JAR entries: `0`
- max production class logical LOC observed: `187`
- max leaf package production Java files observed: `7`
- max leaf content directory EchoScript files observed: `1`

## Tests

Commands run successfully:

- `D:\JDHSeason\mods\echo-script\gradlew.bat check --console=plain`
- `D:\JDHSeason\mods\echo-item-pack\gradlew.bat projects --console=plain`
- `D:\JDHSeason\mods\echo-item-pack\gradlew.bat :item-contracts:test :item-core:test --console=plain`
- `D:\JDHSeason\mods\echo-item-pack\gradlew.bat :item-forge-api-1.20.1:test :item-forge-1.20.1:compileJava :item-forge-1.20.1:test --console=plain`
- `D:\JDHSeason\mods\echo-item-pack\gradlew.bat architectureTest sourceShapeCheck checkWhitespace verifyForbiddenText --console=plain`
- `D:\JDHSeason\mods\echo-item-pack\gradlew.bat dependencies --console=plain`
- `D:\JDHSeason\mods\echo-item-pack\gradlew.bat :item-core:dependencies --configuration runtimeClasspath --console=plain`
- `D:\JDHSeason\mods\echo-item-pack\gradlew.bat :item-forge-api-1.20.1:dependencies --configuration runtimeClasspath --console=plain`
- `D:\JDHSeason\mods\echo-item-pack\gradlew.bat :item-forge-1.20.1:dependencies --configuration runtimeClasspath --console=plain`
- `D:\JDHSeason\mods\echo-item-pack\gradlew.bat :item-testmod-1.20.1:dependencies --configuration runtimeClasspath --console=plain`
- `D:\JDHSeason\mods\echo-item-pack\gradlew.bat check --console=plain`
- `D:\JDHSeason\mods\echo-item-pack\gradlew.bat build --console=plain`
- `D:\JDHSeason\mods\echo-item-pack\git diff --check`
- `D:\JDHSeason\mods\echo-item-pack\gradlew.bat :item-core:test --console=plain`
- `D:\JDHSeason\mods\echo-item-pack\gradlew.bat sourceShapeCheck --console=plain`
- `D:\JDHSeason\mods\echo-item-pack\gradlew.bat :item-forge-1.20.1:gameTestClasses --console=plain`
- `D:\JDHSeason\mods\echo-item-pack\gradlew.bat :item-forge-1.20.1:runGameTestServer --console=plain`
- `D:\JDHSeason\mods\echo-item-pack\gradlew.bat check --console=plain`

GameTest server evidence:

- discovered tests: `6`
- executed tests: `6`
- passed required tests: `6`
- required markers: all `EITEM-GAMETEST-RUN ForgeItemGameTests.*` markers present in `item-forge-1.20.1/run/logs/latest.log`
- server hot reload marker: `Echo Item Pack reload applied=true, changed weapon files=2, diagnostics=0`
- pending restart marker: `PendingItemRegistration[id=jdh:pending_..., kind=echo_item:weapon, message=New item ... after restart.]`

Remaining manual checks:

- Manual Minecraft visual refresh checklist after an explicit client resource reload or client restart.
- Manual Minecraft client/server combat damage/cooldown verification.
