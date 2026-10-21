package io.github.kxyr0.echoplayersleep.forge.network.message;

import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepRevision;
import io.github.kxyr0.echoplayersleep.api.world.Ticks;
import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;

public record EyeTransitionMessage(
        PlayerSleepRevision revision,
        Ticks duration,
        boolean closing,
        boolean sleepyBlink,
        boolean showHud) {
    public EyeTransitionMessage {
        revision = Objects.requireNonNull(revision, "revision");
        duration = Objects.requireNonNull(duration, "duration");
    }

    public static void encode(EyeTransitionMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarLong(message.revision().value());
        buffer.writeVarLong(message.duration().value());
        buffer.writeBoolean(message.closing());
        buffer.writeBoolean(message.sleepyBlink());
        buffer.writeBoolean(message.showHud());
    }

    public static EyeTransitionMessage decode(FriendlyByteBuf buffer) {
        return new EyeTransitionMessage(new PlayerSleepRevision(buffer.readVarLong()),
                Ticks.of(buffer.readVarLong()), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
    }
}
