package io.github.kxyr0.echoplayerhud.forge.client;

import io.github.kxyr0.echoplayerhud.api.metric.HudMetricId;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricIds;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricProvider;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Client-local vanilla health metric. Gameplay authority remains vanilla health.
 */
public final class LocalHealthMetricProvider implements HudMetricProvider {
    @Override
    public HudMetricId metricId() {
        return HudMetricIds.HEALTH;
    }

    @Override
    public HudMetricSnapshot snapshot() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return HudMetricSnapshot.unavailable(metricId());
        }
        return HudMetricSnapshot.available(metricId(), Math.max(0.0F, player.getHealth()),
                player.getMaxHealth(), player.tickCount);
    }
}
