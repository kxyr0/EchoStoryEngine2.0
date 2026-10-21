package io.github.kxyr0.echomagic.forge.sound;

import io.github.kxyr0.echomagic.api.definition.SpellDefinition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class MagicSpellSounds {
    private MagicSpellSounds() { }

    public static void cast(ServerLevel level, Vec3 point, SpellDefinition definition) {
        Profile profile = profile(definition.school().value());
        play(level, point, profile.cast(), 0.56F, profile.pitch());
        play(level, point, profile.accent(), 0.28F, profile.pitch() + 0.18F);
    }

    public static void impact(ServerLevel level, Vec3 point, SpellDefinition definition) {
        Profile profile = profile(definition.school().value());
        play(level, point, profile.impact(), 0.62F, profile.pitch() - 0.08F);
    }

    private static Profile profile(String school) {
        if (school.contains("fire")) {
            return new Profile(SoundEvents.BLAZE_SHOOT, SoundEvents.FIRECHARGE_USE,
                    SoundEvents.GENERIC_EXPLODE, 1.08F);
        }
        if (school.contains("water")) {
            return new Profile(SoundEvents.TRIDENT_THROW, SoundEvents.PLAYER_SPLASH,
                    SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.18F);
        }
        if (school.contains("earth")) {
            return new Profile(SoundEvents.STONE_BREAK, SoundEvents.IRON_GOLEM_REPAIR,
                    SoundEvents.IRON_GOLEM_ATTACK, 0.72F);
        }
        if (school.contains("air")) {
            return new Profile(SoundEvents.PHANTOM_FLAP, SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundEvents.ENDER_DRAGON_FLAP, 1.34F);
        }
        if (school.contains("blood")) {
            return new Profile(SoundEvents.WARDEN_HEARTBEAT, SoundEvents.WITHER_SHOOT,
                    SoundEvents.WITHER_HURT, 0.82F);
        }
        if (school.contains("void")) {
            return new Profile(SoundEvents.END_PORTAL_FRAME_FILL, SoundEvents.ENDERMAN_TELEPORT,
                    SoundEvents.ENDERMAN_HURT, 0.68F);
        }
        if (school.contains("spark")) {
            return new Profile(SoundEvents.AMETHYST_BLOCK_CHIME, SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundEvents.LIGHTNING_BOLT_IMPACT, 1.45F);
        }
        return new Profile(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F);
    }

    private static void play(ServerLevel level, Vec3 point, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, point.x, point.y, point.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    private record Profile(SoundEvent cast, SoundEvent accent, SoundEvent impact, float pitch) { }
}
