package io.github.kxyr0.echoeffects.forge.timeline;

import io.github.kxyr0.echoeffects.api.channel.EffectChannelId;
import io.github.kxyr0.echoeffects.api.channel.EffectTimelineEvent;
import io.github.kxyr0.echoeffects.api.diagnostic.EffectDiagnostic;
import io.github.kxyr0.echoeffects.api.diagnostic.EffectDiagnosticCode;
import io.github.kxyr0.echoeffects.api.target.EffectTargetRef;
import io.github.kxyr0.echoeffects.api.target.EffectTargetType;
import io.github.kxyr0.echoeffects.core.runtime.EffectRuntimeTimelineEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public final class ForgeTimelineEventDispatcher {
    private static final ResourceLocation NAUSEA_ID = ResourceLocation.tryParse("minecraft:nausea");
    private static final ResourceLocation LEGACY_NAUSEA_ID = ResourceLocation.tryParse("minecraft:confusion");

    public List<EffectDiagnostic> dispatch(MinecraftServer server, List<EffectRuntimeTimelineEvent> events) {
        List<EffectDiagnostic> diagnostics = new ArrayList<>();
        for (EffectRuntimeTimelineEvent event : events) {
            Optional<ServerPlayer> target = player(server, event.target());
            if (target.isEmpty()) {
                continue;
            }
            dispatch(target.orElseThrow(), event).ifPresent(diagnostics::add);
        }
        return List.copyOf(diagnostics);
    }

    private Optional<EffectDiagnostic> dispatch(ServerPlayer player, EffectRuntimeTimelineEvent event) {
        EffectTimelineEvent timelineEvent = event.event();
        if (EffectChannelId.SOUND_PLAY.equals(timelineEvent.channelId())) {
            return playSound(player, timelineEvent, event);
        }
        if (EffectChannelId.VANILLA_MOB_EFFECT_APPLY.equals(timelineEvent.channelId())) {
            return applyMobEffect(player, timelineEvent, event);
        }
        return Optional.of(error(EffectDiagnosticCode.EFFECT_UNKNOWN_CHANNEL,
                "Unsupported timeline channel: " + timelineEvent.channelId().value(), event));
    }

    private Optional<EffectDiagnostic> playSound(
            ServerPlayer player,
            EffectTimelineEvent timelineEvent,
            EffectRuntimeTimelineEvent event
    ) {
        Map<String, String> payload = timelineEvent.payload();
        ResourceLocation location = parseLocation(payload.get("sound"));
        if (location == null) {
            return Optional.of(error(EffectDiagnosticCode.EFFECT_SOUND_EVENT_INVALID,
                    "Invalid sound id: " + payload.get("sound"), event));
        }
        Optional<SoundEvent> sound = BuiltInRegistries.SOUND_EVENT.getOptional(location);
        if (sound.isEmpty()) {
            return Optional.of(error(EffectDiagnosticCode.EFFECT_SOUND_EVENT_INVALID,
                    "Unknown sound id: " + location, event));
        }
        float volume = parseFloat(payload, "volume", 1.0F);
        float pitch = parseFloat(payload, "pitch", 1.0F);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound.orElseThrow(), SoundSource.PLAYERS, volume, pitch);
        return Optional.empty();
    }

    private Optional<EffectDiagnostic> applyMobEffect(
            ServerPlayer player,
            EffectTimelineEvent timelineEvent,
            EffectRuntimeTimelineEvent event
    ) {
        Map<String, String> payload = timelineEvent.payload();
        Optional<MobEffect> effect = mobEffect(payload.get("effect"));
        if (effect.isEmpty()) {
            return Optional.of(error(EffectDiagnosticCode.EFFECT_VANILLA_EFFECT_INVALID,
                    "Unknown vanilla effect id: " + payload.get("effect"), event));
        }
        int duration = Math.max(1, parseInt(payload, "durationTicks", 20));
        int amplifier = Math.max(0, parseInt(payload, "amplifier", 0));
        boolean ambient = parseBoolean(payload, "ambient", false);
        boolean visible = parseBoolean(payload, "visible", true);
        boolean showIcon = parseBoolean(payload, "showIcon", true);
        player.addEffect(new MobEffectInstance(effect.orElseThrow(), duration, amplifier,
                ambient, visible, showIcon));
        return Optional.empty();
    }

    private Optional<ServerPlayer> player(MinecraftServer server, EffectTargetRef target) {
        if (target.type() != EffectTargetType.PLAYER) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(server.getPlayerList().getPlayer(UUID.fromString(target.key())));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private Optional<MobEffect> mobEffect(String value) {
        ResourceLocation location = parseLocation(value);
        if (location == null) {
            return Optional.empty();
        }
        Optional<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getOptional(location);
        if (effect.isEmpty() && NAUSEA_ID.equals(location)) {
            return BuiltInRegistries.MOB_EFFECT.getOptional(LEGACY_NAUSEA_ID);
        }
        return effect;
    }

    private ResourceLocation parseLocation(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.contains(":") ? value : "minecraft:" + value;
        return ResourceLocation.tryParse(normalized);
    }

    private int parseInt(Map<String, String> payload, String key, int fallback) {
        try {
            return Integer.parseInt(payload.getOrDefault(key, Integer.toString(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private float parseFloat(Map<String, String> payload, String key, float fallback) {
        try {
            return Float.parseFloat(payload.getOrDefault(key, Float.toString(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private boolean parseBoolean(Map<String, String> payload, String key, boolean fallback) {
        return Boolean.parseBoolean(payload.getOrDefault(key, Boolean.toString(fallback)));
    }

    private EffectDiagnostic error(
            EffectDiagnosticCode code,
            String message,
            EffectRuntimeTimelineEvent event
    ) {
        return EffectDiagnostic.error(code, message, event.instanceId().value().toString());
    }
}
