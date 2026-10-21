package io.github.kxyr0.echonpcbackpack.forge.runtime;

import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackAttachmentPoint;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackStatus;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionPhase;
import io.github.kxyr0.echonpcbackpack.forge.sophisticated.BackpackPayloadSnapshot;
import io.github.kxyr0.echonpcbackpack.forge.sophisticated.SophisticatedBackpackAccess;
import io.github.kxyr0.echonpcbackpack.forge.sophisticated.SophisticatedBackpackPlacer;
import io.github.kxyr0.echonpcbackpack.forge.state.BackpackActorRecord;
import java.io.IOException;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/**
 * The only code allowed to cross world/payload destructive boundaries.
 */
public final class BackpackWorldMutations {
    private final SophisticatedBackpackAccess access;
    private final SophisticatedBackpackPlacer placer;

    public BackpackWorldMutations(SophisticatedBackpackAccess access) {
        this.access = access;
        placer = new SophisticatedBackpackPlacer(access);
    }

    public MutationResult extract(BackpackActorRecord record, ServerLevel level, BlockPos source,
            DurableCommit commit) throws IOException {
        Optional<BackpackPayloadSnapshot> inspected = access.inspect(level, source);
        if (inspected.isEmpty()) return MutationResult.failed("ENPC-SB-SOURCE-MISSING", "Backpack source is missing.");
        BackpackPayloadSnapshot payload = inspected.orElseThrow();
        if (record.expectedFingerprint.isPresent()
                && !record.expectedFingerprint.orElseThrow().equals(payload.reference().fingerprint())) {
            return MutationResult.failed("ENPC-SB-SOURCE-CHANGED", "Backpack changed after command validation.");
        }
        record.payload = payload.stack();
        record.expectedFingerprint = Optional.of(payload.reference().fingerprint());
        record.phase = Optional.of(NpcBackpackTransitionPhase.PAYLOAD_PERSISTED);
        record.revision++;
        commit.save();
        var removal = access.removeExact(level, source, payload.reference().fingerprint());
        if (removal.status() != SophisticatedBackpackAccess.Status.REMOVED) {
            return MutationResult.failed("ENPC-SB-REMOVE-FAILED", "Source removal failed: " + removal.status());
        }
        record.phase = Optional.of(NpcBackpackTransitionPhase.SOURCE_REMOVED);
        record.attachmentPoint = NpcBackpackAttachmentPoint.RIGHT_HAND;
        record.status = NpcBackpackStatus.TRANSITIONING;
        record.revision++;
        commit.save();
        return MutationResult.ok("Exact backpack payload persisted and source removed.");
    }

    public MutationResult place(BackpackActorRecord record, ServerLevel level, BlockPos destination,
            Direction facing, DurableCommit commit) throws IOException {
        if (record.payload.isEmpty()) return MutationResult.failed("ENPC-SB-PAYLOAD-MISSING", "NPC has no payload.");
        SophisticatedBackpackPlacer.PlacementResult placement =
                placer.place(level, destination, record.payload, facing);
        if (!placement.succeeded()) {
            return MutationResult.failed("ENPC-SB-PLACE-" + placement.status().name(), placement.message());
        }
        record.phase = Optional.of(NpcBackpackTransitionPhase.DESTINATION_PLACED);
        record.revision++;
        commit.save();
        record.payload = net.minecraft.world.item.ItemStack.EMPTY;
        record.attachmentPoint = NpcBackpackAttachmentPoint.NONE;
        record.status = NpcBackpackStatus.NONE;
        record.revision++;
        commit.save();
        return MutationResult.ok(placement.message());
    }

    @FunctionalInterface
    public interface DurableCommit {
        void save() throws IOException;
    }

    public record MutationResult(boolean succeeded, String code, String message) {
        static MutationResult ok(String message) {
            return new MutationResult(true, "ENPC-SB-OK", message);
        }

        static MutationResult failed(String code, String message) {
            return new MutationResult(false, code, message);
        }
    }
}
