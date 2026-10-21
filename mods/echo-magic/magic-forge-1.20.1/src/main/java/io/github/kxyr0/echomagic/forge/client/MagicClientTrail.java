package io.github.kxyr0.echomagic.forge.client;

import io.github.kxyr0.echomagic.forge.api.client.particle.MagicBillboardParticleOptions;
import io.github.kxyr0.echomagic.forge.api.client.particle.MagicBillboardParticleType;
import io.github.kxyr0.echomagic.forge.config.MagicClientConfig;
import io.github.kxyr0.echomagic.forge.entity.MagicProjectileEntity;
import io.github.kxyr0.echomagic.forge.entity.MagicProjectilePayload;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class MagicClientTrail {
    private static final Map<Integer, TrailState> STATES = new LinkedHashMap<>();
    private MagicClientTrail() { }

    public static void tick(MagicProjectileEntity projectile) {
        MagicProjectilePayload payload = projectile.payload();
        Vec3 current = projectile.position();
        TrailState state = STATES.getOrDefault(projectile.getId(), new TrailState(current, 0.0D));
        Vec3 delta = current.subtract(state.previous());
        double length = delta.length();
        double spacing = Math.max(0.012D, Math.min(payload.trailSpacing() * 0.45D,
                payload.trailSize() * 0.30D));
        double first = spacing - state.remainder();
        if (length > 1.0E-8D) {
            for (double distance = first; distance <= length; distance += spacing) {
                if (!budget().acquire(projectile.level().getGameTime(), payload.trailLifetime(), false)) break;
                Vec3 point = state.previous().add(delta.scale(distance / length));
                spawn(projectile, payload, point, false);
                shoulders(projectile, payload, point, delta);
            }
        }
        double remainder = (state.remainder() + length) % spacing;
        STATES.put(projectile.getId(), new TrailState(current, remainder));
        if (budget().acquire(projectile.level().getGameTime(), 2, true)) spawn(projectile, payload, current, true);
        if (STATES.size() > 2048) STATES.entrySet().removeIf(entry -> projectile.level().getEntity(entry.getKey()) == null);
    }

    public static void impact(ClientLevel level, String style, Vec3 point,
                              float red, float green, float blue,
                              float vanillaRed, float vanillaGreen, float vanillaBlue) {
        for (int index = 0; index < 8; index++) {
            if (!budget().acquire(level.getGameTime(), 8, index == 0)) break;
            double angle = index * Math.PI * 0.25D;
            Vec3 at = point.add(Math.cos(angle) * 0.12D, (index % 3) * 0.04D, Math.sin(angle) * 0.12D);
            add(level, style, at, red, green, blue, vanillaRed, vanillaGreen, vanillaBlue,
                    index == 0 ? 0.18F : 0.28F, 8, 0.9F, 0.0F);
        }
    }

    private static void spawn(MagicProjectileEntity projectile, MagicProjectilePayload payload, Vec3 point,
                              boolean core) {
        String style = core ? payload.coreParticleStyle() : payload.particleStyle();
        float red = core ? payload.coreRed() : payload.trailRed();
        float green = core ? payload.coreGreen() : payload.trailGreen();
        float blue = core ? payload.coreBlue() : payload.trailBlue();
        float vanillaRed = core ? payload.vanillaCoreRed() : payload.vanillaTrailRed();
        float vanillaGreen = core ? payload.vanillaCoreGreen() : payload.vanillaTrailGreen();
        float vanillaBlue = core ? payload.vanillaCoreBlue() : payload.vanillaTrailBlue();
        // Spatial noise is fixed per sample: size varies along the trail without frame flicker.
        double phase = point.x * 13.7D + point.y * 19.1D + point.z * 17.3D + projectile.getId() * 0.73D;
        float variation = (float) (1.70D + 0.28D * Math.sin(phase)
                + 0.12D * Math.sin(phase * 2.37D + 1.41D));
        float size = core ? payload.coreSize() : payload.trailSize() * variation;
        int life = core ? 2 : payload.trailLifetime();
        add((ClientLevel) projectile.level(), style, point, red, green, blue,
                vanillaRed, vanillaGreen, vanillaBlue, size, life,
                core ? 1.0F : Math.min(1.0F, payload.alphaStart() * 1.20F),
                core ? 0.0F : payload.alphaEnd());
    }

    private static void shoulders(MagicProjectileEntity projectile, MagicProjectilePayload payload,
                                  Vec3 point, Vec3 direction) {
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-10D) side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        side = side.normalize().scale(payload.trailSize() * 0.22D);
        for (int sign : new int[] {-1, 1}) {
            if (!budget().acquire(projectile.level().getGameTime(), payload.trailLifetime(), false)) return;
            add((ClientLevel) projectile.level(), payload.particleStyle(), point.add(side.scale(sign)),
                    payload.trailRed(), payload.trailGreen(), payload.trailBlue(),
                    payload.vanillaTrailRed(), payload.vanillaTrailGreen(), payload.vanillaTrailBlue(),
                    payload.trailSize() * 0.65F, payload.trailLifetime(), payload.alphaStart() * 0.32F,
                    payload.alphaEnd() * 0.32F);
        }
    }

    private static void add(ClientLevel level, String definedStyle, Vec3 point, float red, float green, float blue,
                            float vanillaRed, float vanillaGreen, float vanillaBlue, float size, int life,
                            float alphaStart, float alphaEnd) {
        // Preserve baked school textures while deepening their tint and limiting additive bloom.
        if (definedStyle.startsWith("echo_blood_magic:")) { red *= 0.80F; green *= 0.25F; blue *= 0.40F; }
        else if (definedStyle.startsWith("echo_fire_magic:")) { green *= 0.78F; blue *= 0.35F; }
        red *= 0.88F; green *= 0.88F; blue *= 0.88F;
        String style = MagicClientConfig.USE_VANILLA_COLORED_PARTICLES.get()
                ? "minecraft:dust" : definedStyle;
        ResourceLocation id = ResourceLocation.tryParse(style);
        if (id == null || !BuiltInRegistries.PARTICLE_TYPE.containsKey(id)) return;
        ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(id);
        if (type instanceof MagicBillboardParticleType billboard) {
            float scaled = size * MagicClientConfig.ECHO_PARTICLE_SCALE.get().floatValue();
            level.addParticle(new MagicBillboardParticleOptions(billboard, red, green, blue, scaled, life,
                    alphaStart, alphaEnd), point.x, point.y, point.z, 0.0D, 0.0D, 0.0D);
        }
        else if (type == ParticleTypes.DUST) {
            float scaled = Mth.clamp(size * MagicClientConfig.VANILLA_PARTICLE_SCALE.get().floatValue(),
                    0.01F, 4.0F);
            level.addParticle(new DustParticleOptions(new Vector3f(vanillaRed, vanillaGreen, vanillaBlue), scaled),
                    point.x, point.y, point.z, 0.0D, 0.0D, 0.0D);
        }
        else if (type instanceof SimpleParticleType simple) {
            level.addParticle(simple, point.x, point.y, point.z, 0.0D, 0.0D, 0.0D);
        }
    }

    private static MagicParticleBudget budget() { return MagicClientBootstrap.budget(); }
    private record TrailState(Vec3 previous, double remainder) { }
}
