package io.github.kxyr0.echotime.forge.daylight;

import io.github.kxyr0.echotime.api.config.TimeSpeedConfig;
import io.github.kxyr0.echotime.api.value.EchoInstant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public final class DaylightOwnership {
    private final VanillaDaylightProjection projection = new VanillaDaylightProjection();
    private boolean owned;
    private boolean previousDaylightCycle;

    public void acquire(MinecraftServer server, TimeSpeedConfig config) {
        Objects.requireNonNull(server, "server");
        if (!config.syncSun() || owned) return;
        GameRules.BooleanValue daylight = server.getGameRules().getRule(GameRules.RULE_DAYLIGHT);
        previousDaylightCycle = daylight.get();
        daylight.set(false, server);
        owned = true;
    }

    public DaylightSyncResult sync(MinecraftServer server, TimeSpeedConfig config, EchoInstant instant) {
        long expected = projection.derivedDayTime(instant);
        if (!config.syncSun()) return new DaylightSyncResult(expected, List.of(), false);
        GameRules.BooleanValue daylight = server.getGameRules().getRule(GameRules.RULE_DAYLIGHT);
        boolean gameruleConflict = daylight.get();
        if (gameruleConflict) daylight.set(false, server);
        List<String> external = new ArrayList<>();
        for (String dimension : config.sunDimensions()) {
            ServerLevel level = level(server, dimension);
            if (level == null) continue;
            if (level.getDayTime() != expected) external.add(dimension);
            level.setDayTime(expected);
        }
        return new DaylightSyncResult(expected, external, gameruleConflict);
    }

    public void release(MinecraftServer server) {
        if (!owned) return;
        server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(previousDaylightCycle, server);
        owned = false;
    }

    public long derived(EchoInstant instant) {
        return projection.derivedDayTime(instant);
    }

    private ServerLevel level(MinecraftServer server, String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) return null;
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        return server.getLevel(key);
    }
}
