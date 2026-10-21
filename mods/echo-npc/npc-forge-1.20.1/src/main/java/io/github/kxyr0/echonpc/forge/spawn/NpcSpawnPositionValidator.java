package io.github.kxyr0.echonpc.forge.spawn;

import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.forge.host.ForgeWorldIds;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.AABB;

/**
 * Forge placement validation for authored SpawnAt commands.
 */
public final class NpcSpawnPositionValidator {
    private final MinecraftServer server;

    public NpcSpawnPositionValidator(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public Result validate(NpcWorldId worldId, NpcPosition position) {
        var level = server.getLevel(ForgeWorldIds.toKey(worldId));
        if (level == null) {
            return Result.rejected("NPC_SPAWN_DIMENSION_INVALID", "Target dimension does not exist.");
        }
        BlockPos blockPos = BlockPos.containing(position.x(), position.y(), position.z());
        if (!level.isInWorldBounds(blockPos) || !level.hasChunkAt(blockPos)) {
            return Result.rejected("NPC_SPAWN_POSITION_INVALID", "Target block is outside loaded world bounds.");
        }
        AABB body = new AABB(position.x() - 0.3D, position.y(), position.z() - 0.3D,
                position.x() + 0.3D, position.y() + 1.8D, position.z() + 0.3D);
        if (!level.noCollision(body)) {
            return Result.rejected("NPC_SPAWN_COLLISION", "Target volume collides with world blocks.");
        }
        return Result.ok();
    }

    public record Result(boolean accepted, String diagnosticCode, String message) {
        static Result ok() {
            return new Result(true, "NPC_SPAWN_POSITION_VALID", "Spawn position accepted.");
        }

        static Result rejected(String diagnosticCode, String message) {
            return new Result(false, diagnosticCode, message);
        }
    }
}
