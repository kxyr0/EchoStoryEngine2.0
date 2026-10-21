package io.github.kxyr0.echotrigger.forge.provider.world.time;

import io.github.kxyr0.echotrigger.forge.provider.payload.ForgeIds;
import net.minecraft.server.level.ServerLevel;

record TimeWeatherState(int dayTime, long day, String weather) {
    static TimeWeatherState of(ServerLevel level) {
        long fullTime = level.getDayTime();
        return new TimeWeatherState((int) Math.floorMod(fullTime, 24000L),
                Math.floorDiv(fullTime, 24000L), ForgeIds.weather(level));
    }
}
