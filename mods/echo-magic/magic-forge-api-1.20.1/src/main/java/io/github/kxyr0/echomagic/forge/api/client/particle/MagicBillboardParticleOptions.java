package io.github.kxyr0.echomagic.forge.api.client.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Locale;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

public record MagicBillboardParticleOptions(MagicBillboardParticleType type, float red, float green, float blue,
        float size, int lifetime, float alphaStart, float alphaEnd) implements ParticleOptions {
    public static final Deserializer<MagicBillboardParticleOptions> DESERIALIZER = new Deserializer<>() {
        @Override
        public MagicBillboardParticleOptions fromCommand(ParticleType<MagicBillboardParticleOptions> type,
                StringReader reader) throws CommandSyntaxException {
            float red = read(reader); float green = read(reader); float blue = read(reader);
            float size = read(reader); int lifetime = Math.round(read(reader));
            return new MagicBillboardParticleOptions((MagicBillboardParticleType) type, red, green, blue,
                    size, lifetime, read(reader), read(reader));
        }

        @Override
        public MagicBillboardParticleOptions fromNetwork(ParticleType<MagicBillboardParticleOptions> type,
                FriendlyByteBuf buffer) {
            return new MagicBillboardParticleOptions((MagicBillboardParticleType) type, buffer.readFloat(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readVarInt(),
                    buffer.readFloat(), buffer.readFloat());
        }

        private float read(StringReader reader) throws CommandSyntaxException { reader.expect(' '); return reader.readFloat(); }
    };

    public MagicBillboardParticleOptions {
        if (type == null || size <= 0.0F || lifetime < 1) throw new IllegalArgumentException("Invalid billboard particle.");
    }

    static MagicBillboardParticleOptions defaults(MagicBillboardParticleType type) {
        return new MagicBillboardParticleOptions(type, 1.0F, 1.0F, 1.0F, 0.2F, 8, 1.0F, 0.0F);
    }
    @Override public ParticleType<?> getType() { return type; }
    @Override public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeFloat(red); buffer.writeFloat(green); buffer.writeFloat(blue); buffer.writeFloat(size);
        buffer.writeVarInt(lifetime); buffer.writeFloat(alphaStart); buffer.writeFloat(alphaEnd);
    }
    @Override public String writeToString() {
        return String.format(Locale.ROOT, "%s %.3f %.3f %.3f %.3f %d %.3f %.3f", type, red, green, blue,
                size, lifetime, alphaStart, alphaEnd);
    }
}

