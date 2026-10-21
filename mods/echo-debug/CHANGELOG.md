# Changelog

## 0.4.4

- Declared compatibility with Echo Navigation 0.3.x so the current authoring stack can load together.

## 0.4.3

- Replaced custom trigger overlay render types with Minecraft's shader-compatible debug render types.
- Moved world overlays to Forge's `AFTER_PARTICLES` stage for Oculus/Embeddium compatibility.
- Added see-through trigger labels above rendered areas.

## 0.4.2

- Made Trigger Wand squares filled, see-through, and independent from the item-entity render target.
- Fixed square edge normals and added an immediate orange square for every selected point.
- Aligned Echo Seating trigger squares with the floor below seat anchors.
- Fixed reverse-order box selections excluding the first selected block.

## 0.4.1

- Fixed Trigger Wand footprint squares being rendered inside blocks instead of above their top faces.
- Added Trigger Wand selection and edit handling when aiming at entities such as NPCs.
- Registered a dedicated always-on-top Trigger Wand HUD that requests and reports the authoritative catalog.

## 0.4.0

- Trigger Wand now refreshes its authoritative area catalog when equipped and while held.
- Rendered spatial triggers as bright world-space block squares instead of faint volume edges.
- Added optional Echo Seating resolution so triggers subscribed to seat zones and slots appear at their blocks.

## 0.3.2

- Widened Echo Trigger compatibility to allow Echo Trigger 0.5.x.

## 0.3.1

- Fixed NPC movement trail rendering to draw stored NPC world positions with camera offset applied only during render.
- Widened Echo Trigger compatibility to allow Echo Trigger 0.4.x.

## 0.3.0

- Added Echo Cutscene C2 debug commands for camera mode and target selection.

## 0.2.0

- Added optional Echo Cutscene debug commands: `/echo_debug cutscene enter|exit|status`.

## 0.1.3

- Added optional Echo Navigation actual NPC movement trail capture, rendering and client-side clearing.

## 0.1.2

- Removed the `echo-debug.enabled` runtime gate; installing the mod enables debug tools.

## 0.1.1

- Added `/echo_debug gameMenu on|off` client command for focus-loss Game Menu control.

## 0.1.0

- Added initial development-only Trigger Wand repository.
