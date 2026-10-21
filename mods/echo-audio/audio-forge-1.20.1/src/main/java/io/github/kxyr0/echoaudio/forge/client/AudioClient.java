package io.github.kxyr0.echoaudio.forge.client;

import io.github.kxyr0.echoaudio.api.*;
import io.github.kxyr0.echoaudio.asset.AudioCatalog;
import io.github.kxyr0.echoaudio.forge.decode.VoiceDecoderBridge;
import io.github.kxyr0.echoaudio.forge.network.*;
import io.github.kxyr0.echoaudio.forge.sound.AudioSoundInstance;
import java.util.*;
import net.minecraft.client.Minecraft;

public final class AudioClient {
    private static AudioClient instance;
    private final AudioCatalog catalog;
    private final Map<AudioPlaybackId, Active> active = new LinkedHashMap<>();
    private final Map<AudioPlaybackId, Pending> loading = new LinkedHashMap<>();
    private net.minecraft.client.multiplayer.ClientLevel level;
    private long session;
    private long ticks;
    private AudioClient() {
        catalog = new AudioCatalog(Minecraft.getInstance().gameDirectory.toPath().resolve("story/content-pack"),
                new VoiceDecoderBridge(), 64L * 1024 * 1024);
        catalog.reload();
    }
    public static AudioClient instance() { if (instance == null) instance = new AudioClient(); return instance; }
    public AudioCatalog catalog() { return catalog; }
    public void play(AudioIntent intent) {
        ensureWorld();
        if (active.containsKey(intent.id()) || loading.containsKey(intent.id())) return;
        if (active.size() + loading.size() >= 128) { report(intent.id(), "AUDIO_PLAYBACK_START_FAILED: capacity"); return; }
        long expected = session;
        var future = catalog.preload(new AudioResourceId(intent.cue().resource()));
        loading.put(intent.id(), new Pending(intent, ticks, future));
        future.whenComplete((ready, failure) ->
                Minecraft.getInstance().execute(() -> {
                    if (expected != session || loading.remove(intent.id()) == null) return;
                    if (failure != null) { report(intent.id(), reason(failure) + " at " + intent.cue().source()); return; }
                    start(intent, ready);
                }));
    }
    private void start(AudioIntent intent, AudioCatalog.Ready ready) {
        var mc = Minecraft.getInstance();
        var pinned = new HashSet<AudioCatalog.Ready>();
        active.values().forEach(value -> pinned.add(value.pinned())); pinned.add(ready);
        if (pinned.stream().mapToLong(AudioCatalog.Ready::bytes).sum() > 128L * 1024 * 1024) {
            report(intent.id(), "AUDIO_PLAYBACK_START_FAILED: pinned PCM budget exceeded"); return;
        }
        if (mc.level == null || intent.spatial()
                && !mc.level.dimension().location().toString().equals(intent.dimension())) {
            report(intent.id(), "AUDIO_PLAYBACK_START_FAILED: world changed"); return;
        }
        try {
            var sound = new AudioSoundInstance(intent, intent.spatial() ? ready.spatial() : ready.local());
            mc.getSoundManager().play(sound); active.put(intent.id(), new Active(intent, ready, sound, ticks));
        } catch (RuntimeException failure) { report(intent.id(), "AUDIO_BACKEND_UNAVAILABLE: " + failure.getMessage()); }
    }
    public void stop(AudioPlaybackId id, long fade) {
        var pending = loading.remove(id);
        if (pending != null) { pending.future().cancel(false); report(id, ""); }
        Active value = active.get(id);
        if (value != null) value.sound().fadeOut(fade);
    }
    public void tick() {
        var mc = Minecraft.getInstance();
        ensureWorld();
        if (mc.isPaused()) return;
        ticks++;
        for (var pending : List.copyOf(loading.values())) {
            if (ticks - pending.created() <= 600) continue;
            loading.remove(pending.intent().id()); pending.future().cancel(false);
            report(pending.intent().id(), "AUDIO_PLAYBACK_START_FAILED: loading timeout");
        }
        for (Active value : List.copyOf(active.values())) {
            var sound = value.sound();
            String problem = value.intent().spatial() && (mc.level == null
                    || !mc.level.dimension().location().toString().equals(value.intent().dimension()))
                    ? "AUDIO_PLAYBACK_START_FAILED: dimension changed" : "";
            boolean timedOut = !sound.ready() && ticks - value.created() > 600;
            boolean ended = sound.isStopped() || sound.ready() && ticks - value.created() > 20
                    && !mc.getSoundManager().isActive(sound);
            if (!problem.isEmpty() || timedOut || ended) {
                mc.getSoundManager().stop(sound); active.remove(value.intent().id());
                report(value.intent().id(), timedOut ? "AUDIO_PLAYBACK_START_FAILED: backend timeout" : problem);
            }
        }
    }
    public void disconnect() {
        session++; loading.values().forEach(p -> p.future().cancel(false)); loading.clear();
        active.values().forEach(value -> Minecraft.getInstance().getSoundManager().stop(value.sound())); active.clear();
    }
    private void ensureWorld() {
        var current = Minecraft.getInstance().level;
        if (level == current) return;
        List.copyOf(loading.keySet()).forEach(id -> report(id, "AUDIO_PLAYBACK_CANCELLED: world changed"));
        List.copyOf(active.keySet()).forEach(id -> report(id, "AUDIO_PLAYBACK_CANCELLED: world changed"));
        disconnect(); level = current;
    }
    public List<String> status() {
        List<String> result = new ArrayList<>();
        active.values().forEach(v -> result.add(v.intent().id().value() + " sound=" + v.intent().cue().resource()
                + " owner=" + v.intent().owner() + " loop=" + v.intent().cue().loop() + " position="
                + (v.intent().spatial() ? v.intent().x() + "," + v.intent().y() + "," + v.intent().z() : "local")
                + " volume=" + v.intent().cue().options().volume() + " pitch=" + v.intent().cue().options().pitch()
                + " elapsedTicks=" + v.sound().elapsed() + " remainingTicks="
                + (v.intent().cue().options().durationTicks() == 0 ? "natural/infinite"
                        : Math.max(0, v.intent().cue().options().durationTicks() - v.sound().elapsed()))
                + " state=" + (v.sound().ready() ? "PLAYING" : "STARTING") + " source=" + v.intent().cue().source()));
        result.add("loading=" + loading.size()); return result;
    }
    private void report(AudioPlaybackId id, String detail) {
        if (!detail.isEmpty()) com.mojang.logging.LogUtils.getLogger().warn("{}", detail);
        if (Minecraft.getInstance().getConnection() != null) AudioPackets.ended(id, detail.length() > 1000 ? detail.substring(0, 1000) : detail);
    }
    private String reason(Throwable failure) {
        while (failure.getCause() != null) failure = failure.getCause();
        return failure.getMessage() == null ? "AUDIO_DECODE_FAILED" : failure.getMessage();
    }
    private record Active(AudioIntent intent, AudioCatalog.Ready pinned, AudioSoundInstance sound, long created) { }
    private record Pending(AudioIntent intent, long created, java.util.concurrent.CompletableFuture<AudioCatalog.Ready> future) { }
}
