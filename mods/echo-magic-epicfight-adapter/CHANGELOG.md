# История изменений

## 0.3.14

- Added stable particle-size noise to weapon bodies and matching centers.
- Reduced core brightness and whiteness to retain the weapon hue.

## 0.3.13

- Replaced prominent perimeter ring chains with smaller, dimmer, closely overlapping circular particles.
- Reduced boundary particle radius while retaining large central particles and dense weapon fill.

## 0.3.12

- Increased shaft density by 50% and head/blade fill density about twofold; increased fringe sampling.
- Removed lateral sinusoidal offsets from circle rows and straightened the extended whip axis.
- Preserved smooth size taper, school colors and weapon silhouettes.

## 0.3.11

- Increased tip fill density, concentrating spear/dagger samples near the point and subdividing curved blades more finely.
- Smoothstep particle-size taper and size-dependent spacing preserve dense coverage while particles gradually shrink toward tips.
- Extended whip reach by 50% with additional curve control points.

## 0.3.10

- Increased shaft sampling density over fourfold and widened overlapping side discs to close internal gaps.
- Increased spearhead/weapon fill sampling density and body alpha; retained softer fringe circles and school colors.

## 0.3.9

- Nearly doubled shaft and fringe sampling density; added two small offset discs per shaft sample to fill gaps while keeping the large circles readable.
- Increased spearhead, dagger and blade fill density without changing colors or particle sizes.

## 0.3.8

- Sample shafts by cumulative distance (0.145 blocks), using one row of varying circular particles instead of five densely overlapping rows.
- Render bright school-tinted centers in a separate final batch with a small camera-facing depth offset.
- Widen and lengthen spearheads; reduce fringe size so the shaft and pointed head stay distinct.

## 0.3.7

- Enlarged circular particles to 0.070 at edges and 0.150 in the center; spaced them to retain visible rounded lobes.
- Added animated scalloped edges and bright school-tinted particle centers, preserving weapon silhouettes and anchors.

## 0.3.6

- Doubled boundary particle radius (0.013 -> 0.026) and increased central particles by 50% (0.044 -> 0.066).
- Increased sampling density and smoothed the center-to-edge size profile.
- Replaced independent particle flicker with subtle synchronized breathing for a steadier silhouette.

## 0.3.5

- Weapon bodies now consist of dense, frame-attached circular magic particles instead of ribbons.
- Filled spearheads, dagger blades and curved swords; gauntlets have filled circular cross-sections on both hands.
- Particle radius increases from about 0.013 at the boundary to 0.044 in the center; tip particles taper down.
- Added a neutral circle sprite so every weapon keeps its own NBT color, including attack trails.
- Preserved Epic Fight hand anchors and per-frame rendering to avoid detached particle copies.

## 0.3.4

- Restored weapon-specific contours: spearhead, curved blade, rapier guard, dagger edges and both gauntlet shells.
- Kept redundant parallel spear/rapier paths removed; no outer glow or cross planes.
- Stronger colored BODY and a subdued CORE preserve each weapon color from SummonedWeaponData.

## 0.3.3

- Removed duplicate weapon ribbons, accents and glow clusters; one BODY/CORE pair per hand remains.
- Uses alpha-aware soft edges, a 40% narrower BODY, a 22.5% CORE and tapered endpoints.
- Preserved hand anchors and isolated world render buffers.

## 0.3.2

- Isolated weapon render buffers and reset/restored the global model-view transform during the world pass.
- Guarded uninitialized hand-joint mappings against Epic Fight root-joint fallback.
- Replaced dense round motes with continuous soft additive ribbons, a 40% wider body, white core, broad glow, travelling pulse and sparse sparks.
- Added bright source clusters at both grips for gauntlets.

## 0.3.1

- Added a subtle full-bright halo by widening the existing translucent weapon ribbon.
- The halo reuses the same vertex count, buffer and render batch; particle count is unchanged.

## 0.3.0

- Replaced the blocky permanent silhouette with camera-facing glow ribbons over Catmull-Rom paths.
- A bright drawing pulse now travels continuously from each weapon's hilt toward its tip.
- Sinusoidal motes remain around the ribbons and pulse in opposing phases without losing the hand joint.

## 0.2.2

- Weapon particles orbit their current Epic Fight hand pose along individual sinusoidal paths.
- Brightness travels through every weapon from dark to bright and back; mirrored motes pulse in the opposite phase.

## 0.2.1

- Permanent summoned-weapon geometry is rendered from the current Epic Fight hand pose every frame, removing one-tick visual lag.
- Increased geometry density and added a bright inner core to every particle weapon.
- Attack trails contain more samples but expire twice as fast, so deliberate motion remains without stale weapon copies.

## 0.2.0

- Added distinct hand-anchored geometry for spears, whips, dual gauntlets, blades, rapiers and daggers.
- Water-whip curvature now responds to the player's current mouse motion.
- Server-side Epic Fight attack pulses let weapon mechanics count full misses and combo animation restarts.

## 0.1.1

- Клинок привязан к фактическому `Tool_R`/main-hand joint текущей позы Epic Fight вместо позиции камеры.
- Рукоять, гарда и острие теперь повторяют idle, ходьбу и каждую фазу анимации атаки.
- Постоянная форма оружия переведена на короткоживущие billboard-частицы; длинный след остаётся только у движения атаки.
- Собственный PNG не добавлен: particle использует ванильный atlas sprite.

## 0.1.0

- Добавлен универсальный Epic Fight trail для любого предмета с `SummonedWeaponData`.
- Поддержаны particle-движения `thrust` и `slash`, произвольные цвет и длина клинка.
- Добавлены обязательные source-shape проверки 200 строк / 7 Java-файлов.
