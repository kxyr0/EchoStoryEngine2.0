package io.github.kxyr0.echocutscene.forge.network;

import io.github.kxyr0.echocutscene.api.camera.CameraTrackSnapshot;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutscenePlaybackId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneEndBehavior;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneHudPolicy;
import io.github.kxyr0.echocutscene.forge.client.EchoCutsceneClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundCutsceneStartPacket(
        CutscenePlaybackId playbackId,
        CameraTrackSnapshot track,
        long elapsedMillis,
        double speed,
        CutsceneEndBehavior endBehavior,
        CutsceneHudPolicy hudPolicy
) {
    public ClientboundCutsceneStartPacket(
            CutscenePlaybackId playbackId,
            CameraTrackSnapshot track,
            long elapsedMillis,
            double speed,
            CutsceneEndBehavior endBehavior
    ) {
        this(playbackId, track, elapsedMillis, speed, endBehavior, CutsceneHudPolicy.defaultPlayback());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(playbackId.value());
        CameraTrackBuf.write(buffer, track);
        buffer.writeLong(elapsedMillis);
        buffer.writeDouble(speed);
        buffer.writeEnum(endBehavior);
        writeHudPolicy(buffer, hudPolicy);
    }

    public static ClientboundCutsceneStartPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundCutsceneStartPacket(new CutscenePlaybackId(buffer.readUtf()),
                CameraTrackBuf.read(buffer), buffer.readLong(), buffer.readDouble(),
                buffer.readEnum(CutsceneEndBehavior.class), readHudPolicy(buffer));
    }

    private static void writeHudPolicy(FriendlyByteBuf buffer, CutsceneHudPolicy policy) {
        buffer.writeBoolean(policy.hideGameplayHud());
        buffer.writeBoolean(policy.keepDialogueVisible());
        buffer.writeBoolean(policy.showDebugHud());
        buffer.writeBoolean(policy.cinematic());
    }

    private static CutsceneHudPolicy readHudPolicy(FriendlyByteBuf buffer) {
        return new CutsceneHudPolicy(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> EchoCutsceneClient.begin(this)));
        context.setPacketHandled(true);
    }
}
