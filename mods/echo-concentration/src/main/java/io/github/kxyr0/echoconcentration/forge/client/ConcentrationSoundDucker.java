package io.github.kxyr0.echoconcentration.forge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

final class ConcentrationSoundDucker {
    private static final float ACTIVE_GAIN = 0.45F;
    private static final int FADE_TICKS = 8;
    private float amount;
    private boolean applied;

    void tick(Minecraft minecraft, boolean active) {
        if (!active && amount <= 0.0F && !applied) return;
        float step = 1.0F / FADE_TICKS;
        amount = active ? Math.min(1.0F, amount + step) : Math.max(0.0F, amount - step);
        apply(minecraft);
        applied = amount > 0.0F;
    }

    void release(Minecraft minecraft) {
        if (applied || amount > 0.0F) {
            amount = 0.0F;
            apply(minecraft);
        }
        applied = false;
    }

    private void apply(Minecraft minecraft) {
        float configured = minecraft.options.getSoundSourceVolume(SoundSource.MASTER);
        float scale = 1.0F - (1.0F - ACTIVE_GAIN) * amount;
        minecraft.getSoundManager().updateSourceVolume(SoundSource.MASTER, configured * scale);
    }
}
