package io.github.kxyr0.echovoice.forge.playback.sound;

import com.mojang.blaze3d.audio.Channel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.client.event.sound.PlayStreamingSourceEvent;

/** Executes a 5-10 ms hard-stop gain ramp on Minecraft's sound executor. */
public final class VoiceStopRamp implements AutoCloseable {
    private static final int STEPS = 4;
    private final Map<VoiceSoundInstance, Source> sources = new ConcurrentHashMap<>();
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "echo-voice-stop-ramp");
        thread.setDaemon(true);
        return thread;
    });

    public void started(PlayStreamingSourceEvent event) {
        if (event.getSound() instanceof VoiceSoundInstance voice) {
            sources.put(voice, new Source(event.getEngine(), event.getChannel()));
        }
    }

    public void stop(VoiceSoundInstance sound, int rampMillis) {
        Source source = sources.get(sound);
        if (source == null || rampMillis <= 0) {
            Minecraft.getInstance().getSoundManager().stop(sound);
            return;
        }
        float category = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.VOICE);
        float initial = Math.max(0.0F, Math.min(1.0F, sound.getVolume() * category));
        for (int step = 1; step <= STEPS; step++) {
            int current = step;
            long delay = Math.max(1L, (long) rampMillis * step / STEPS);
            timer.schedule(() -> source.engine.executor.execute(() -> apply(sound, source, initial, current)),
                    delay, TimeUnit.MILLISECONDS);
        }
    }

    public void reset() {
        sources.clear();
    }

    public void release(VoiceSoundInstance sound) {
        sources.remove(sound);
    }

    @Override
    public void close() {
        timer.shutdownNow();
        sources.clear();
    }

    private void apply(VoiceSoundInstance sound, Source source, float initial, int step) {
        if (sources.get(sound) != source) {
            return;
        }
        source.channel.setVolume(initial * (STEPS - step) / STEPS);
        if (step == STEPS) {
            source.channel.stop();
            sources.remove(sound, source);
        }
    }

    private record Source(SoundEngine engine, Channel channel) {
    }
}
