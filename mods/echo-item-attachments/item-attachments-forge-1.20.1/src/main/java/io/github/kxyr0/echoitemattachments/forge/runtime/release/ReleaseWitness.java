package io.github.kxyr0.echoitemattachments.forge.runtime.release;

import io.github.kxyr0.echoitemattachments.api.identity.ReleaseId;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

public final class ReleaseWitness {
    public static final String TAG = "echo_item_attachments_release_id";

    public void mark(ItemEntity entity, ReleaseId releaseId) {
        entity.setUUID(entityId(releaseId));
        entity.getPersistentData().putString(TAG, releaseId.value());
    }

    public Optional<ItemEntity> find(MinecraftServer server, ReleaseId releaseId) {
        UUID entityId = entityId(releaseId);
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(entityId);
            if (entity instanceof ItemEntity item && item.isAlive()
                    && item.getPersistentData().getString(TAG).equals(releaseId.value())) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    public UUID entityId(ReleaseId releaseId) {
        return UUID.nameUUIDFromBytes(("echo-item-release:" + releaseId.value())
                .getBytes(StandardCharsets.UTF_8));
    }
}
