package io.github.kxyr0.echostorynpcadapter.mutation;

import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;

/**
 * UUID-v5-style deterministic mapping from Story action execution to NPC mutation.
 */
public final class StoryMutationMapper {
    private static final String OUTPUT_NAMESPACE = "echo_story_npc_adapter";
    private static final UUID NAMESPACE = UUID.fromString("fb7f05be-0b5c-5ad6-8f77-50f0a6a5b4b8");

    public ActorMutationId mutationId(ActionExecutionId actionExecutionId) {
        byte[] digest = sha1(namespaceBytes(), actionExecutionId.asString().getBytes(StandardCharsets.UTF_8));
        digest[6] = (byte) ((digest[6] & 0x0F) | 0x50);
        digest[8] = (byte) ((digest[8] & 0x3F) | 0x80);
        ByteBuffer buffer = ByteBuffer.wrap(digest);
        UUID uuid = new UUID(buffer.getLong(), buffer.getLong());
        return new ActorMutationId(OUTPUT_NAMESPACE + ":" + uuid.toString().toLowerCase(Locale.ROOT));
    }

    private static byte[] namespaceBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(NAMESPACE.getMostSignificantBits());
        buffer.putLong(NAMESPACE.getLeastSignificantBits());
        return buffer.array();
    }

    private static byte[] sha1(byte[] namespace, byte[] name) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(namespace);
            digest.update(name);
            return digest.digest();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 недоступен в JRE.", exception);
        }
    }
}
