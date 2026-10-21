package io.github.kxyr0.echocutscene.forge.network.authoring;

import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.forge.network.EchoCutsceneNetwork;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record ServerboundCameraRecordingSavePacket(String sessionId, CutsceneId cutsceneId, String source) {
    private static final int MAX_SOURCE_LENGTH = 262_144;

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(sessionId);
        buffer.writeUtf(cutsceneId.value());
        buffer.writeUtf(source, MAX_SOURCE_LENGTH);
    }

    public static ServerboundCameraRecordingSavePacket decode(FriendlyByteBuf buffer) {
        return new ServerboundCameraRecordingSavePacket(buffer.readUtf(), CutsceneId.parse(buffer.readUtf()),
                buffer.readUtf(MAX_SOURCE_LENGTH));
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> save(context.getSender()));
        context.setPacketHandled(true);
    }

    private void save(ServerPlayer player) {
        if (player != null && EchoCutsceneNetwork.authoringHost() != null) {
            EchoCutsceneNetwork.authoringHost().save(player, sessionId, cutsceneId, source);
        }
    }
}
