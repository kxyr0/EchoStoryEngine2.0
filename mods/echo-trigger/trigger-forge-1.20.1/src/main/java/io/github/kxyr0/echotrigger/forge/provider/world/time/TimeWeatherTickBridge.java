package io.github.kxyr0.echotrigger.forge.provider.world.time;

import io.github.kxyr0.echotrigger.api.event.TriggerEvent;
import io.github.kxyr0.echotrigger.forge.provider.payload.ForgeIds;
import io.github.kxyr0.echotrigger.forge.service.ForgeTriggerServices;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

public final class TimeWeatherTickBridge {
    private final ForgeTriggerServices services;
    private final Map<String, TimeWeatherState> snapshots = new LinkedHashMap<>();

    public TimeWeatherTickBridge(ForgeTriggerServices services) {
        this.services = services;
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.side != LogicalSide.SERVER || event.phase != TickEvent.Phase.END
                || !(event.level instanceof ServerLevel level)) {
            return;
        }
        String dimension = ForgeIds.dimension(level);
        TimeWeatherState current = TimeWeatherState.of(level);
        TimeWeatherState previous = snapshots.put(dimension, current);
        level.players().forEach(player -> publishPredicates(player, dimension, current));
        if (previous != null) {
            level.players().forEach(player -> publishEdges(player, dimension, previous, current));
        }
    }

    private void publishPredicates(ServerPlayer player, String dimension, TimeWeatherState state) {
        publishPredicate(player, "TimeBetween", Map.of(
                "dimension", dimension,
                "value", Integer.toString(state.dayTime())));
        publishPredicate(player, "DayAtLeast", Map.of(
                "dimension", dimension,
                "day", Long.toString(state.day())));
        publishPredicate(player, "WeatherIs", Map.of(
                "dimension", dimension,
                "target", state.weather(),
                "weather", state.weather()));
    }

    private void publishEdges(
            ServerPlayer player,
            String dimension,
            TimeWeatherState previous,
            TimeWeatherState current
    ) {
        publish(player, "TimeReached", Map.of(
                "dimension", dimension,
                "value", Integer.toString(current.dayTime()),
                "previousValue", Integer.toString(previous.dayTime())));
        if (current.day() > previous.day()) {
            publish(player, "DayReached", Map.of(
                    "dimension", dimension,
                    "day", Long.toString(current.day()),
                    "previousDay", Long.toString(previous.day())));
        }
        if (!previous.weather().equals(current.weather())) {
            publish(player, "WeatherChanged", weatherPayload(dimension, previous, current));
        }
    }

    private static Map<String, String> weatherPayload(
            String dimension,
            TimeWeatherState previous,
            TimeWeatherState current
    ) {
        return Map.of("dimension", dimension,
                "from", previous.weather(),
                "to", current.weather(),
                "target", current.weather());
    }

    private void publishPredicate(ServerPlayer player, String expressionName, Map<String, String> payload) {
        services.applyEvent(TriggerEvent.predicateTrue(ForgeIds.owner(player), tick(player), expressionName, payload));
    }

    private void publish(ServerPlayer player, String expressionName, Map<String, String> payload) {
        services.applyEvent(TriggerEvent.vanilla(ForgeIds.owner(player), tick(player), expressionName, payload));
    }

    private static long tick(ServerPlayer player) {
        return player.serverLevel().getGameTime();
    }
}
