package io.github.kxyr0.echobloodmagic.forge.combat;

import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

record BloodMark(ResourceKey<Level> dimension, UUID caster, long nextPulseTick, int pulsesLeft) {
    BloodMark next(long tick) { return new BloodMark(dimension, caster, tick, pulsesLeft - 1); }
}
