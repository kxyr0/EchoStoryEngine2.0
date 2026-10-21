package io.github.kxyr0.echoaudio.forge.dialogue;

import io.github.kxyr0.echoaudio.api.AudioCue;
import io.github.kxyr0.echoaudio.forge.server.AudioLocationLookupEvent;
import io.github.kxyr0.echoaudio.forge.server.AudioServer;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.forge.api.EchoDialogueServiceLookupEvent;
import io.github.kxyr0.echodialogue.forge.api.wait.DialogueExternalWaitDispatchEvent;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class AudioDialogueListener {
    private final Map<MinecraftServer, Map<DialogueSessionId, AudioDialogueSession>> hosts = new IdentityHashMap<>();
    @SubscribeEvent public void dispatch(DialogueExternalWaitDispatchEvent event) {
        if (!event.externalWait().waitType().equals("echo_audio:cue")) return;
        MinecraftServer server = event.player().map(ServerPlayer::getServer)
                .orElseGet(ServerLifecycleHooks::getCurrentServer);
        if (server == null) return;
        var lookup = new EchoDialogueServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(lookup);
        if (lookup.providers().size() != 1) return;
        DialogueService service = lookup.providers().get(0);
        if (event.player().isEmpty()) {
            com.mojang.logging.LogUtils.getLogger().warn("AUDIO_PLAYBACK_INTERRUPTED: {}",
                    event.session().sessionId());
            service.cancel(event.session().sessionId());
            return;
        }
        var sessions = hosts.computeIfAbsent(server, key -> new HashMap<>());
        var id = event.session().sessionId();
        String owner = "dialogue:" + id.value();
        var session = sessions.get(id);
        if (session == null) {
            session = new AudioDialogueSession(service, id, () -> {
                sessions.remove(id);
                AudioServer.of(server).cancelOwned(owner);
            });
            sessions.put(id, session);
            session.observe();
        }
        session.dispatch(event.externalWait(), (cue, completion) ->
                play(event.player().orElseThrow(), owner, cue, completion));
    }
    private void play(ServerPlayer player, String owner, AudioCue cue, Consumer<String> completion) {
        AudioLocationLookupEvent.Position location = new AudioLocationLookupEvent.Position("", 0, 0, 0);
        if (!cue.location().isEmpty()) {
            var lookup = new AudioLocationLookupEvent(cue.location());
            MinecraftForge.EVENT_BUS.post(lookup);
            location = lookup.require();
        }
        AudioServer.of(player.getServer()).play(player, owner, cue, location.dimension(),
                location.x(), location.y(), location.z(), completion);
    }
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGH) public void stopping(ServerStoppingEvent event) {
        var sessions = hosts.remove(event.getServer());
        if (sessions != null) java.util.List.copyOf(sessions.values()).forEach(AudioDialogueSession::close);
    }
}
