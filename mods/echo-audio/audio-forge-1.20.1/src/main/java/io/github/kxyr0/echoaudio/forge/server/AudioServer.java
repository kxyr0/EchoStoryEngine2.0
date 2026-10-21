package io.github.kxyr0.echoaudio.forge.server;

import io.github.kxyr0.echoaudio.api.*;
import io.github.kxyr0.echoaudio.forge.network.*;
import io.github.kxyr0.echoaudio.playback.AudioPlaybackRegistry;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Server-owned intents; client may acknowledge only its own playback. Never selects the first player. */
public final class AudioServer {
    private static final Map<MinecraftServer, AudioServer> HOSTS = new IdentityHashMap<>();
    private final AudioPlaybackRegistry<Entry> entries = new AudioPlaybackRegistry<>();
    public static AudioServer of(MinecraftServer server) { return HOSTS.computeIfAbsent(server, key -> new AudioServer()); }
    public static void close(MinecraftServer server) { HOSTS.remove(server); }
    public AudioPlaybackId play(ServerPlayer player, String owner, AudioCue cue, String dimension,
            double x, double y, double z, Consumer<String> completion) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !dimension.isEmpty() && net.minecraft.resources.ResourceLocation.tryParse(dimension) == null) {
            throw new AudioFailure("AUDIO_LOCATION_NOT_FOUND", "Invalid world position");
        }
        if (cue.operation().equals("Stop")) {
            var matches = entries.named(owner, player.getUUID().toString(), cue.name());
            if (matches.isEmpty()) throw new AudioFailure("AUDIO_PLAYBACK_NOT_FOUND", cue.name());
            matches.forEach(v -> AudioPackets.stop(player, v.id(), cue.options().fadeOutTicks()));
            completion.accept(""); return matches.get(0).id();
        }
        var intent = new AudioIntent(AudioPlaybackId.next(), owner, cue, dimension, x, y, z);
        entries.add(new AudioPlaybackRegistry.Entry<>(intent.id(), owner, player.getUUID().toString(), cue,
                new Entry(player, intent, completion)));
        try { AudioPackets.send(player, intent); }
        catch (RuntimeException failure) {
            entries.remove(intent.id());
            throw new AudioFailure("AUDIO_PLAYBACK_START_FAILED", failure.getMessage());
        }
        return intent.id();
    }
    public void ended(ServerPlayer player, AudioPlaybackId id, String detail) {
        var registered = entries.get(id);
        Entry entry = registered == null ? null : registered.value();
        if (entry == null || !entry.player().getUUID().equals(player.getUUID())) return;
        entries.remove(id); entry.completion().accept(detail);
    }
    public void cancelOwned(String owner) {
        for (var registered : entries.finishOwner(owner)) {
            Entry entry = registered.value();
            try { AudioPackets.stop(entry.player(), entry.intent().id(), 0); }
            catch (RuntimeException failure) {
                com.mojang.logging.LogUtils.getLogger().warn("AUDIO_PLAYBACK_CANCELLED: {}", failure.getMessage());
            }
            entry.completion().accept("AUDIO_PLAYBACK_CANCELLED");
        }
    }
    public void cancelOwners(String prefix) {
        entries.owners().stream().filter(owner -> owner.startsWith(prefix)).toList().forEach(this::cancelOwned);
    }
    public void disconnected(ServerPlayer player) {
        for (var registered : entries.disconnect(player.getUUID().toString())) {
            registered.value().completion().accept("AUDIO_PLAYBACK_CANCELLED: disconnect");
        }
    }
    private record Entry(ServerPlayer player, AudioIntent intent, Consumer<String> completion) { }
}
