package io.github.kxyr0.echoteleportmagic.forge.server;

import io.github.kxyr0.echoteleportmagic.api.TeleportAnchor;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;

public final class TeleportSubjects {
    private TeleportSubjects() { }
    public static LivingEntity require(MinecraftServer server, UUID id) {
        for (var level : server.getAllLevels()) {
            if (level.getEntity(id) instanceof LivingEntity living) return living;
        }
        throw new IllegalStateException("TELEPORT_SUBJECT_UNAVAILABLE: " + id);
    }
    public static TeleportAnchor anchor(LivingEntity subject) {
        return new TeleportAnchor(subject.getX(), subject.getY(), subject.getZ(), subject.getYRot(),
                subject.getBbWidth(), subject.getBbHeight());
    }
}
