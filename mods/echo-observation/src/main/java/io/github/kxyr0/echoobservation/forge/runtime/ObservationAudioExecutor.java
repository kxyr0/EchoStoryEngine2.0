package io.github.kxyr0.echoobservation.forge.runtime;

import io.github.kxyr0.echoaudio.api.AudioCue;
import io.github.kxyr0.echoaudio.forge.server.AudioLocationLookupEvent;
import io.github.kxyr0.echoaudio.forge.server.AudioServer;
import io.github.kxyr0.echoobservation.api.action.ObservationActionContext;
import io.github.kxyr0.echoobservation.api.action.ObservationActionExecutor;
import io.github.kxyr0.echoobservation.api.action.ObservationScriptAction;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

final class ObservationAudioExecutor implements ObservationActionExecutor {
    private final MinecraftServer server;
    ObservationAudioExecutor(MinecraftServer server) { this.server = server; }
    @Override public String target() { return "echo_audio:cue"; }
    @Override public boolean execute(ObservationActionContext context, ObservationScriptAction action) {
        var player = server.getPlayerList().getPlayer(context.playerId());
        if (player == null) return false;
        try {
            Map<String, String> payload = new LinkedHashMap<>();
            action.arguments().forEach(argument -> payload.put(argument.name().orElseThrow(), argument.value()));
            AudioCue cue = AudioCue.decode(payload);
            AudioLocationLookupEvent.Position position = null;
            if (!cue.location().isEmpty()) {
                var lookup = new AudioLocationLookupEvent(cue.location());
                MinecraftForge.EVENT_BUS.post(lookup); position = lookup.require();
            }
            AudioServer.of(server).play(player, "observation:" + context.completionId().value(), cue,
                    position == null ? "" : position.dimension(), position == null ? 0 : position.x(),
                    position == null ? 0 : position.y(), position == null ? 0 : position.z(), detail -> { });
            return true;
        } catch (RuntimeException failure) { return false; }
    }
}
