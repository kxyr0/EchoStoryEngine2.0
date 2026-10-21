package io.github.kxyr0.echoiteminspection.client.integration.ipla;

import com.akicater.IPLA_Methods;
import com.akicater.blocks.LayingItemEntity;
import io.github.kxyr0.echoiteminspection.client.api.ItemInspectionClient;
import io.github.kxyr0.echoiteminspection.client.world.ItemInspectionWorldTarget;
import io.github.kxyr0.echoiteminspection.client.world.WorldInspectionDiagnostic;
import io.github.kxyr0.echoiteminspection.client.world.WorldInspectionDiagnostics;
import io.github.kxyr0.echoiteminspection.forge.InspectionClientConfig;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** One spatial block ray; at most one IPLA BlockEntity's 24 slots, never world/chunk iteration. */
public final class IplaInspectionTargetResolver {
    private final IplaTargetSnapshotCache cache = new IplaTargetSnapshotCache();
    private long queries;
    private int candidatesExamined;
    private net.minecraft.client.multiplayer.ClientLevel cachedLevel;

    public Optional<ItemInspectionWorldTarget> resolve() { return resolve(false); }
    public Optional<ItemInspectionWorldTarget> resolveFresh() { return resolve(true); }

    private Optional<ItemInspectionWorldTarget> resolve(boolean freshSelection) {
        queries++;
        candidatesExamined = 0;
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var camera = minecraft.gameRenderer.getMainCamera();
        if (level == null || minecraft.player == null || !camera.isInitialized()) return Optional.empty();
        if (cachedLevel != level) {
            clear();
            cachedLevel = level;
        }
        Vec3 start = camera.getPosition();
        double range = InspectionClientConfig.WORLD_DISTANCE.get();
        Vec3 end = start.add(new Vec3(camera.getLookVector()).scale(range));
        var hit = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, minecraft.getCameraEntity()));
        if (hit.getType() != HitResult.Type.BLOCK || !level.hasChunkAt(hit.getBlockPos())) {
            return missing(WorldInspectionDiagnostic.ITEM_INSPECTION_IPLA_TARGET_INVALID, "No placed block hit");
        }
        return inspectHit(level, hit, start, range, freshSelection);
    }

    private Optional<ItemInspectionWorldTarget> inspectHit(net.minecraft.client.multiplayer.ClientLevel level,
            net.minecraft.world.phys.BlockHitResult hit, Vec3 start, double range, boolean freshSelection) {
        if (!(level.getBlockEntity(hit.getBlockPos()) instanceof LayingItemEntity entity) || entity.isRemoved()) {
            return missing(WorldInspectionDiagnostic.ITEM_INSPECTION_IPLA_TARGET_OCCLUDED, "First hit is not an IPLA item");
        }
        double distance = start.distanceTo(hit.getLocation());
        if (distance > range + 0.000001) {
            return missing(WorldInspectionDiagnostic.ITEM_INSPECTION_IPLA_TARGET_TOO_FAR, "Hit exceeds configured range");
        }
        int slot = selectSlot(entity, hit, freshSelection);
        if (slot < 0 || slot >= entity.inv.size()) {
            return missing(WorldInspectionDiagnostic.ITEM_INSPECTION_IPLA_STACK_UNAVAILABLE, "No IPLA sub-slot at hit");
        }
        var stack = entity.inv.get(slot);
        if (!ItemInspectionClient.isInspectable(stack)) {
            return missing(WorldInspectionDiagnostic.ITEM_INSPECTION_IPLA_TARGET_INVALID, "Placed stack has no inspection");
        }
        WorldInspectionDiagnostics.record(WorldInspectionDiagnostic.NONE, "Synchronized IPLA placed stack");
        return Optional.of(cache.capture(entity, slot, hit.getLocation(), distance, level.dimension().location()));
    }

    private int selectSlot(LayingItemEntity entity, net.minecraft.world.phys.BlockHitResult hit, boolean freshSelection) {
        int cached = freshSelection ? -1 : cache.unchangedSlot(entity, hit.getLocation());
        if (cached >= 0) return cached;
        candidatesExamined = entity.inv.size();
        var indices = IPLA_Methods.getPreciseIndexFromHit(entity, hit, false);
        return indices.isEmpty() ? -1 : indices.get(0);
    }

    private Optional<ItemInspectionWorldTarget> missing(WorldInspectionDiagnostic code, String detail) {
        WorldInspectionDiagnostics.record(code, detail);
        return Optional.empty();
    }

    public long queries() { return queries; }
    public int candidatesExamined() { return candidatesExamined; }
    public void clear() {
        cache.clear();
        cachedLevel = null;
    }
}
