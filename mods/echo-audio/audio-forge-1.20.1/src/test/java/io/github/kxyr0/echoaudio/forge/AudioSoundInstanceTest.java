package io.github.kxyr0.echoaudio.forge;

import io.github.kxyr0.echoaudio.api.AudioCue;
import io.github.kxyr0.echoaudio.api.AudioOptions;
import io.github.kxyr0.echoaudio.api.AudioPlaybackId;
import io.github.kxyr0.echoaudio.forge.network.AudioIntent;
import io.github.kxyr0.echoaudio.forge.sound.AudioSoundInstance;
import io.github.kxyr0.echoaudio.pcm.AudioPcm;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AudioSoundInstanceTest {
    private AudioPcm pcm() { return new AudioPcm(new byte[]{1, 0, 2, 0}, 8000, 1); }
    @Test void spatialSourceUsesWorldPositionAttenuationPitchAndMinecraftCategory() {
        var cue = new AudioCue("PlayAt", "jdh:knock", "", "BedroomDoor", new AudioOptions(.75, 1.25, 0, 0, 0));
        var sound = new AudioSoundInstance(new AudioIntent(AudioPlaybackId.next(), "scene:a", cue,
                "minecraft:overworld", 10.5, 64.8, -32), pcm());
        assertFalse(sound.isRelative()); assertEquals(SoundInstance.Attenuation.LINEAR, sound.getAttenuation());
        assertEquals(SoundSource.BLOCKS, sound.getSource()); assertEquals(10.5, sound.getX());
        assertEquals(64.8, sound.getY()); assertEquals(-32, sound.getZ());
        assertEquals(.75f, sound.getVolume()); assertEquals(1.25f, sound.getPitch());
    }
    @Test void localTimedLoopStartsClockOnStreamPreparationAndStopsAtDeadline() throws Exception {
        var cue = new AudioCue("Loop", "jdh:rain", "", "", new AudioOptions(1, .75, 60, 0, 0));
        var sound = new AudioSoundInstance(new AudioIntent(AudioPlaybackId.next(), "scene:a", cue, "", 0, 0, 0), pcm());
        assertTrue(sound.isRelative()); assertEquals(SoundInstance.Attenuation.NONE, sound.getAttenuation());
        sound.tick(); assertEquals(0, sound.elapsed());
        try (var stream = sound.getStream(null, null, false).join()) {
            assertEquals(100, stream.read(100).remaining());
            for (int i = 0; i < 59; i++) sound.tick();
            assertFalse(sound.isStopped()); sound.tick(); assertTrue(sound.isStopped());
        }
    }
}
