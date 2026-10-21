# Bed integration

N4 поддерживает vanilla `minecraft:*_bed` через `VanillaBedRestProvider`. Provider отвечает за canonical bed resolution, sleep anchor, yaw, occupied state, enter sleep pose, release и restart validation.

Перед `Npc.SleepAt` проверьте:

- точный `BlockPosition` authored кровати;
- фактический namespaced `BlockId`;
- head/foot половину и направление кровати;
- что chunk загружен;
- что NPC находится рядом с кроватью;
- что кровать не занята другим actor или vanilla occupant.

Для Sasha audit заполните `docs/diagnostics/SASHA_BED_INTEGRATION_AUDIT.md`: block id, mod id, block state, facing, head/foot, occupancy, provider и результат `inspect_bed`.
