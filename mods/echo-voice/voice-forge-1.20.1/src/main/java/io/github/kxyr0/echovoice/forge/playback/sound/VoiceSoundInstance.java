package io.github.kxyr0.echovoice.forge.playback.sound;

import io.github.kxyr0.echovoice.api.identity.VoicePlaybackId;
import io.github.kxyr0.echovoice.forge.decode.DecodedPcm;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;

/** Dynamic relative streaming SoundInstance routed through Minecraft SoundSource.VOICE. */
public final class VoiceSoundInstance extends AbstractSoundInstance {
    private final DecodedPcm pcm;
    private final WeighedSoundEvents event;

    public VoiceSoundInstance(VoicePlaybackId id, DecodedPcm pcm, double lineVolume) {
        this(id, pcm, lineVolume, 0.0D, 0.0D, 0.0D, 16, false);
    }

    public VoiceSoundInstance(VoicePlaybackId id, DecodedPcm pcm, double lineVolume,
            double x, double y, double z, int maximumDistance) {
        this(id, pcm, lineVolume, x, y, z, maximumDistance, true);
    }

    private VoiceSoundInstance(VoicePlaybackId id, DecodedPcm pcm, double lineVolume,
            double x, double y, double z, int maximumDistance, boolean spatial) {
        super(new ResourceLocation("echo_voice", "dynamic/" + id), SoundSource.VOICE,
                SoundInstance.createUnseededRandom());
        this.pcm = pcm;
        this.volume = (float) lineVolume;
        this.looping = false;
        this.relative = !spatial;
        this.attenuation = spatial ? SoundInstance.Attenuation.LINEAR : SoundInstance.Attenuation.NONE;
        this.x = x; this.y = y; this.z = z;
        this.sound = new Sound(location.toString(), ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1,
                Sound.Type.FILE, true, false, Math.max(1, maximumDistance));
        this.event = new WeighedSoundEvents(location, null);
        this.event.addSound(sound);
    }

    @Override
    public WeighedSoundEvents resolve(SoundManager manager) {
        return event;
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary buffers, Sound selected, boolean looping) {
        return CompletableFuture.completedFuture(new VoicePcmAudioStream(pcm));
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
