package io.github.kxyr0.echobackgroundnpc.forge.network;

import io.github.kxyr0.echobackgroundnpc.dialogue.EchoDialogueAmbientPresenter;
import io.github.kxyr0.echobackgroundnpc.api.conversation.AmbientLine;
import io.github.kxyr0.echobackgroundnpc.api.source.BackgroundSourceRef;
import java.util.Optional;
import io.github.kxyr0.echobackgroundnpc.voice.EchoAmbientVoiceAdapter;
import io.github.kxyr0.echobackgroundnpc.core.navigation.WorldPoint;
import net.minecraft.network.FriendlyByteBuf;

record AmbientOverlayMessage(String presentationId, String dialogueId, String speaker, String text,
        String voiceStem, String dimension, double x, double y, double z, long durationMillis, boolean visible) {
    void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(presentationId, 180);
        buffer.writeBoolean(visible);
        if (visible) {
            buffer.writeUtf(speaker, 180);
            buffer.writeUtf(text, 4096);
            buffer.writeUtf(dialogueId, 180);
            buffer.writeUtf(voiceStem, 180);
            buffer.writeUtf(dimension, 180);
            buffer.writeDouble(x); buffer.writeDouble(y); buffer.writeDouble(z);
            buffer.writeLong(durationMillis);
        }
    }

    static AmbientOverlayMessage decode(FriendlyByteBuf buffer) {
        String id = buffer.readUtf(180);
        boolean visible = buffer.readBoolean();
        if (!visible) return clear(id);
        String speaker = buffer.readUtf(180);
        String text = buffer.readUtf(4096);
        return new AmbientOverlayMessage(id, buffer.readUtf(180), speaker, text, buffer.readUtf(180),
                buffer.readUtf(180), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readLong(), true);
    }

    void present() {
        var presenter = new EchoDialogueAmbientPresenter();
        var voice = new EchoAmbientVoiceAdapter();
        if (!visible) { presenter.clear(presentationId); voice.stop(presentationId); return; }
        var line = new AmbientLine(speaker, text, Optional.of(voiceStem).filter(value -> !value.isBlank()),
                new BackgroundSourceRef("<network>", 1, 1));
        presenter.present(presentationId, line, durationMillis);
        voice.playNearby(presentationId, dialogueId, line, new WorldPoint(dimension, x, y, z));
    }

    static AmbientOverlayMessage clear(String id) {
        return new AmbientOverlayMessage(id, "", "", "", "", "", 0, 0, 0, 1L, false);
    }
}
