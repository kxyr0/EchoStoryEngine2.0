# Echo Item Inspection

Independent Forge 1.20.1 mod, version 0.2.2. Inspect explicitly authored items
from inventory and normal container slots using the original stack snapshot.

Build and verify with Java 17:

```powershell
.\gradlew.bat check build --offline
.\gradlew.bat :inspection-testmod-1.20.1:runGameTestServer --offline
.\gradlew.bat :inspection-testmod-1.20.1:runClient -PinspectionManual=true --offline
.\gradlew.bat :inspection-testmod-1.20.1:runClient -PinspectionIplaSmoke=true --offline
.\gradlew.bat :inspection-testmod-1.20.1:runClient -PinspectionIplaManual=true --offline
```

Production artifact: `inspection-forge-1.20.1/build/libs/echo-item-inspection-0.2.2-all.jar`.
After validation, `./scripts/deploy-prism.ps1` deploys the production JAR to Prism
with SHA-256 checks, persistent backups, lock detection, and duplicate removal.

- [Architecture, lifecycle, and verification](docs/architecture/ITEM_INSPECTION.md)
- [Minimal and advanced EchoScript examples](docs/runtime/ITEM_INSPECTION_DSL.md)
- [Changelog](docs/changelogs/README.md)

Testmod fixtures and the client smoke harness are development-only and excluded
from the production artifact.
Validated on 2026-10-17: 83 JUnit tests and the real IPLA client smoke passed for 0.2.2,
including live prompt sizing, persisted configuration, invalid scale rejection and reset.
Earlier integration validation also passed 18 Content Doctor tests, six dedicated
GameTests, 12 source-shape enforcement probes, and the inventory Forge client smoke.
The IPLA client smoke also passed against the installed IPLA 6.3.1 and Architectury 9.2.14:
exact placed NBT/model/glint/damage, zero inventory opens on inspect, vanilla E without
a target, R/mouse rebinding, real Concentration priority, exclusive foreground/detached
camera veto, occlusion/range/stale/removal safety, all requested GUI scales and resolutions.
The client smoke verifies inventory/container clicks, exact NBT, actual model rendering,
controls, same-parent return, server invalidation, external reload/LKG and pinned revisions.
The text smoke also checks authored Unicode/paragraphs, hover-name fallback, page/scrollbar
controls, wheel zoom over text, responsive narrow layout, and the dark Blackspace shader background.
GUI Scale 1/2/3/4/Auto produced identical 391 x 321 pixel model bounds at 1920 x 1080;
image comparisons also confirmed visible animated foil against an unenchanted control.
Test fixtures are development-only; production items require authored `.inspection.es` definitions.

IPLA 6.3.1 adds a read-only world path using the same definitions and viewer.
Look at a placed inspectable item within 4.5 blocks, then use the configurable
inspect key (default E). With no target, E retains vanilla inventory behavior.
The bottom-left prompt follows the actual binding and framebuffer reference canvas.
Local `/echo_item_inspection prompt` commands change its scale, minimum width and height
immediately and save the client settings; see the DSL guide for examples and limits.
Inventory, dialogue, Thought Space, detached cutscene camera and Concentration take priority.
No new mod or integration JAR is required. Without IPLA, inventory inspection remains available.
Other IPLA versions disable only the world integration with a diagnostic.
Build-time IPLA is resolved from the installed Prism mods directory; override its path
with `-PprismModsDirectory=<mods-directory>`. Optional integrations are compile-only,
never embedded. `inspectionIplaSmoke` adds audited IPLA/Architectury/Concentration
to the development runtime only; regular client/server runs also exercise their absence.
Automated client runs seed missing test worlds from the isolated GameTest server's
`run/world`; run the GameTest command first on a fresh checkout.
`inspectionIplaManual` opens an interactive Russian-language IPLA scene in Creative mode and stays open.
It seeds inspectable/non-inspectable placed items and inventory examples; its authored
sample description is temporary and development-only. The world prompt uses an orange accent.
