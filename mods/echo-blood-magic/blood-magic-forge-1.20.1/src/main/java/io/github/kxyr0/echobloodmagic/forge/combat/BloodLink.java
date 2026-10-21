package io.github.kxyr0.echobloodmagic.forge.combat;

import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

record BloodLink(ResourceKey<Level> dimension, UUID caster, long expiresAt) { }
