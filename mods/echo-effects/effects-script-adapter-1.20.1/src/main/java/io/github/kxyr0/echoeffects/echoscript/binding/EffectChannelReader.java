package io.github.kxyr0.echoeffects.echoscript.binding;

import io.github.kxyr0.echoeffects.api.channel.EffectChannelId;
import io.github.kxyr0.echoeffects.api.channel.EffectColor;
import io.github.kxyr0.echoeffects.api.channel.EffectTimelineEvent;
import io.github.kxyr0.echoeffects.api.channel.EffectTrack;
import io.github.kxyr0.echoeffects.api.definition.EffectDefinitionDraft;
import io.github.kxyr0.echoeffects.api.time.EffectTime;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EffectChannelReader {
    private final EffectExpressionReader reader = new EffectExpressionReader();

    public boolean read(InvocationExpressionSyntax invocation, EffectDefinitionDraft draft) {
        return switch (EffectCallSyntax.qualifiedName(invocation)) {
            case "Movement.SpeedMultiplier" -> addTrack(invocation, draft, EffectChannelId.MOVEMENT_SPEED);
            case "Camera.FovOffset" -> addTrack(invocation, draft, EffectChannelId.CAMERA_FOV);
            case "Camera.Roll" -> addTrack(invocation, draft, EffectChannelId.CAMERA_ROLL);
            case "Camera.Shake" -> addNamedTrack(invocation, draft, EffectChannelId.CAMERA_SHAKE, "amplitude");
            case "Screen.Tint" -> addScreenTrack(invocation, draft, EffectChannelId.SCREEN_TINT);
            case "Screen.Vignette" -> addScreenTrack(invocation, draft, EffectChannelId.SCREEN_VIGNETTE);
            case "Sound.PlayAt" -> addSoundEvent(invocation, draft);
            case "VanillaEffect.Apply" -> addVanillaMobEffect(invocation, draft);
            default -> false;
        };
    }

    private boolean addTrack(InvocationExpressionSyntax invocation, EffectDefinitionDraft draft,
                             EffectChannelId channelId) {
        draft.addTrack(new EffectTrack(channelId, reader.curve(EffectCallSyntax.positionalInvocation(invocation, 0)
                .orElseThrow()), neutral(channelId)));
        return true;
    }

    private boolean addNamedTrack(InvocationExpressionSyntax invocation, EffectDefinitionDraft draft,
                                  EffectChannelId channelId, String argument) {
        draft.addTrack(new EffectTrack(channelId, reader.curve(EffectCallSyntax.namedInvocation(invocation, argument)
                .orElseThrow()), neutral(channelId)));
        return true;
    }

    private boolean addScreenTrack(InvocationExpressionSyntax invocation, EffectDefinitionDraft draft,
                                   EffectChannelId channelId) {
        EffectCallSyntax.namedInvocation(invocation, "color")
                .flatMap(color -> EffectCallSyntax.positionalString(color, 0))
                .ifPresent(EffectColor::hex);
        return addNamedTrack(invocation, draft, channelId, "opacity");
    }

    private boolean addSoundEvent(InvocationExpressionSyntax invocation, EffectDefinitionDraft draft) {
        long tick = EffectCallSyntax.namedInvocation(invocation, "time").map(reader::timeTicks).orElse(0L);
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("sound", EffectCallSyntax.namedString(invocation, "sound").orElseThrow());
        payload.put("volume", Double.toString(EffectCallSyntax.namedNumber(invocation, "volume").orElse(1.0D)));
        payload.put("pitch", Double.toString(EffectCallSyntax.namedNumber(invocation, "pitch").orElse(1.0D)));
        draft.addTimelineEvent(new EffectTimelineEvent(new EffectTime(tick, 0.0F), EffectChannelId.SOUND_PLAY, payload));
        return true;
    }

    private boolean addVanillaMobEffect(InvocationExpressionSyntax invocation, EffectDefinitionDraft draft) {
        long tick = EffectCallSyntax.namedInvocation(invocation, "time").map(reader::timeTicks).orElse(0L);
        long durationTicks = EffectCallSyntax.namedInvocation(invocation, "duration")
                .map(reader::timeTicks)
                .orElseThrow(() -> new IllegalArgumentException("VanillaEffect.Apply requires duration."));
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("effect", EffectCallSyntax.namedString(invocation, "effect")
                .orElseThrow(() -> new IllegalArgumentException("VanillaEffect.Apply requires effect.")));
        payload.put("durationTicks", Long.toString(durationTicks));
        payload.put("amplifier", Integer.toString(EffectCallSyntax.namedNumber(invocation, "amplifier")
                .map(Double::intValue)
                .orElse(0)));
        payload.put("ambient", Boolean.toString(EffectCallSyntax.namedBoolean(invocation, "ambient").orElse(false)));
        payload.put("visible", Boolean.toString(EffectCallSyntax.namedBoolean(invocation, "visible").orElse(true)));
        payload.put("showIcon", Boolean.toString(EffectCallSyntax.namedBoolean(invocation, "showIcon").orElse(true)));
        draft.addTimelineEvent(new EffectTimelineEvent(new EffectTime(tick, 0.0F),
                EffectChannelId.VANILLA_MOB_EFFECT_APPLY, payload));
        return true;
    }

    private double neutral(EffectChannelId channelId) {
        return EffectChannelId.MOVEMENT_SPEED.equals(channelId) ? 1.0D : 0.0D;
    }
}
