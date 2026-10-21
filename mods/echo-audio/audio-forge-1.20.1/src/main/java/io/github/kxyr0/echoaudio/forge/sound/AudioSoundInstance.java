package io.github.kxyr0.echoaudio.forge.sound;

import io.github.kxyr0.echoaudio.forge.network.AudioIntent;
import io.github.kxyr0.echoaudio.pcm.AudioPcm;
import io.github.kxyr0.echoaudio.playback.AudioEnvelope;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.client.sounds.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;

public final class AudioSoundInstance extends AbstractSoundInstance implements TickableSoundInstance {
    private final AudioPcm pcm;
    private WeighedSoundEvents event;
    private final AudioEnvelope envelope;
    private final boolean loop;
    private volatile boolean streamRequested;
    private boolean stopped;
    public AudioSoundInstance(AudioIntent intent, AudioPcm pcm) {
        super(new ResourceLocation("echo_audio", "dynamic/" + intent.id().value()), SoundSource.BLOCKS,
                SoundInstance.createUnseededRandom());
        this.pcm = pcm; this.loop = intent.cue().loop(); envelope = new AudioEnvelope(intent.cue().options());
        pitch = (float) intent.cue().options().pitch(); volume = (float) envelope.gain();
        relative = !intent.spatial(); attenuation = relative ? Attenuation.NONE : Attenuation.LINEAR;
        x = intent.x(); y = intent.y(); z = intent.z();
        // The stream loops its cursor; SoundEngine must not reopen or restart the instance.
        looping = false;
    }
    @Override public WeighedSoundEvents resolve(SoundManager manager) {
        if (event == null) {
            sound = new Sound(location.toString(), ConstantFloat.of(1), ConstantFloat.of(1), 1,
                    Sound.Type.FILE, true, false, 32);
            event = new WeighedSoundEvents(location, null); event.addSound(sound);
        }
        return event;
    }
    @Override public float getVolume() { return volume; }
    @Override public float getPitch() { return pitch; }
    @Override public CompletableFuture<AudioStream> getStream(SoundBufferLibrary buffers, Sound selected, boolean looping) {
        streamRequested = true;
        return CompletableFuture.completedFuture(new AudioPcmStream(pcm, loop));
    }
    @Override public boolean canStartSilent() { return true; }
    @Override public void tick() {
        if (!streamRequested) return;
        envelope.tick(); volume = (float) envelope.gain();
        if (envelope.finished()) stop();
    }
    public void fadeOut(long ticks) { envelope.stop(ticks); if (ticks == 0) stop(); }
    public long elapsed() { return envelope.elapsed(); }
    public boolean ready() { return streamRequested; }
    @Override public boolean isStopped() { return stopped; }
    private void stop() { stopped = true; }
}
