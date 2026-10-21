package io.github.kxyr0.echonpc.forge.rest;

import io.github.kxyr0.echonpc.api.block.BlockId;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.rest.NpcRestProvider;
import io.github.kxyr0.echonpc.api.rest.command.NpcSleepRequest;
import io.github.kxyr0.echonpc.api.scene.NpcSceneCommandId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.core.rest.NpcRestAttachment;
import io.github.kxyr0.echonpc.forge.entity.EchoNpcEntity;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

public final class VanillaBedRestProvider implements NpcRestProvider {
    public static final String ID = "echo_npc:vanilla_bed";
    private static final BlockId ANY_BED = new BlockId("echo_npc:any_bed");

    @Override
    public String providerId() { return ID; }
    @Override
    public boolean supports(BlockId blockId) { return ANY_BED.equals(blockId) || isBedId(blockId); }

    ForgeRestResolution resolve(ServerLevel level, NpcSleepRequest request, String commandId, long revision) {
        BlockPos requested = toPos(request.bedPosition());
        if (!level.hasChunkAt(requested)) {
            return rejected(NpcResults.SleepStatus.BED_NOT_FOUND, Optional.empty(),
                    "NPC_SLEEP_BED_NOT_FOUND", "Bed chunk is not loaded.");
        }
        BlockState state = level.getBlockState(requested);
        Optional<BlockId> actual = actualBlock(state);
        if (state.isAir()) {
            return rejected(NpcResults.SleepStatus.BED_NOT_FOUND, actual,
                    "NPC_SLEEP_BED_NOT_FOUND", "Bed block is absent.");
        }
        if (!expectedMatches(request.expectedBlock(), actual)) {
            return rejected(NpcResults.SleepStatus.EXPECTED_BLOCK_MISMATCH, actual,
                    "NPC_SLEEP_EXPECTED_BLOCK_MISMATCH", "Actual block differs from expectedBlock.");
        }
        if (!isBedLike(state, actual)) {
            return rejected(NpcResults.SleepStatus.BED_UNSUPPORTED, actual,
                    "NPC_SLEEP_BED_UNSUPPORTED", "Block is not a supported bed-like block.");
        }
        return resolveBed(new BedCandidate(requested, state, actual.orElseThrow()), request, commandId, revision);
    }

    void applySleep(ServerLevel level, EchoNpcEntity entity, NpcRestAttachment attachment) {
        BlockPos canonical = toPos(attachment.canonicalBedPosition());
        Direction facing = facing(attachment.providerState());
        occupy(level, canonical, true);
        occupy(level, canonical.relative(facing.getOpposite()), true);
        entity.setNoAi(true);
        entity.setDeltaMovement(0.0D, 0.0D, 0.0D);
        entity.markSleeping(attachment);
        entity.startSleeping(canonical);
        VanillaBedSleepAnchor.logVanilla(canonical, facing, entity);
        entity.setYBodyRot(attachment.sleepYaw());
        entity.setYHeadRot(attachment.sleepYaw());
    }

    Optional<String> validateAttachment(ServerLevel level, NpcRestAttachment attachment) {
        BlockPos canonical = toPos(attachment.canonicalBedPosition());
        if (!level.hasChunkAt(canonical)) {
            return Optional.of("NPC_SLEEP_BED_REMOVED");
        }
        BlockState state = level.getBlockState(canonical);
        Optional<BlockId> actual = actualBlock(state);
        if (state.isAir() || !isBedLike(state, actual)) {
            return Optional.of("NPC_SLEEP_BED_REMOVED");
        }
        if (!expectedMatches(attachment.expectedBlock(), actual)) {
            return Optional.of("NPC_SLEEP_EXPECTED_BLOCK_MISMATCH");
        }
        return Optional.empty();
    }

    void release(ServerLevel level, EchoNpcEntity entity, NpcRestAttachment attachment) {
        BlockPos canonical = toPos(attachment.canonicalBedPosition());
        Direction facing = facing(attachment.providerState());
        occupy(level, canonical, false);
        occupy(level, canonical.relative(facing.getOpposite()), false);
        entity.stopSleeping();
        entity.setNoAi(false);
        entity.clearSleeping();
    }

    NpcPosition wakePosition(NpcRestAttachment attachment) {
        BlockPosition bed = attachment.canonicalBedPosition();
        Direction facing = facing(attachment.providerState());
        double x = bed.x() + 0.5D - facing.getStepX() * 1.25D;
        double z = bed.z() + 0.5D - facing.getStepZ() * 1.25D;
        return new NpcPosition(x, bed.y(), z);
    }

    private ForgeRestResolution resolveBed(
            BedCandidate candidate,
            NpcSleepRequest request,
            String commandId,
            long revision
    ) {
        Direction facing = facing(candidate.state());
        BlockPos canonical = isHead(candidate.state()) ? candidate.requested() : candidate.requested()
                .relative(facing);
        NpcPosition anchor = VanillaBedSleepAnchor.initial(canonical);
        String providerState = providerState(candidate.state(), facing);
        BlockId expectedBlock = ANY_BED.equals(request.expectedBlock()) ? candidate.actualBlock()
                : request.expectedBlock();
        var attachment = new NpcRestAttachment(request.npcId(), ID, request.worldId(), request.bedPosition(),
                expectedBlock, fromPos(canonical), anchor, facing.toYRot(), "NPC_SLEEPING", revision,
                providerState, true, new NpcSceneCommandId(commandId));
        return ForgeRestResolution.accepted(attachment, candidate.actualBlock());
    }

    private String providerState(BlockState state, Direction facing) {
        StringBuilder builder = new StringBuilder("facing=").append(facing.getName());
        propertyString(state, "part").ifPresent(value -> builder.append(";part=").append(value));
        propertyString(state, "shape").ifPresent(value -> builder.append(";shape=").append(value));
        return builder.toString();
    }

    private void occupy(ServerLevel level, BlockPos pos, boolean occupied) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BedBlock && state.hasProperty(BedBlock.OCCUPIED)) {
            level.setBlock(pos, state.setValue(BedBlock.OCCUPIED, occupied), 3);
            return;
        }
        booleanProperty(state, "occupied").ifPresent(property ->
                level.setBlock(pos, state.setValue(property, occupied), 3));
    }

    private boolean expectedMatches(BlockId expected, Optional<BlockId> actual) {
        return ANY_BED.equals(expected) ? actual.isPresent() : actual.map(expected::equals).orElse(false); }
    private boolean isBedLike(BlockState state, Optional<BlockId> actual) {
        return state.getBlock() instanceof BedBlock || state.is(BlockTags.BEDS) || actual.map(this::isBedId)
                .orElse(false);
    }

    private boolean isBedId(BlockId blockId) {
        String id = blockId.value().toLowerCase(Locale.ROOT);
        int separator = id.indexOf(':');
        String path = separator < 0 ? id : id.substring(separator + 1);
        return path.equals("bed") || path.endsWith("_bed") || path.contains("_bed_")
                || path.startsWith("bed_") || path.contains("/bed") || path.contains("bed/");
    }

    private Direction facing(BlockState state) {
        if (state.getBlock() instanceof BedBlock && state.hasProperty(BedBlock.FACING)) {
            return state.getValue(BedBlock.FACING);
        } return directionProperty(state, "facing").orElse(Direction.SOUTH);
    }

    private boolean isHead(BlockState state) {
        if (state.getBlock() instanceof BedBlock && state.hasProperty(BedBlock.PART)) {
            return state.getValue(BedBlock.PART) == BedPart.HEAD;
        } return propertyString(state, "part").map("head"::equals).orElse(true);
    }

    private Optional<Direction> directionProperty(BlockState state, String name) {
        for (Property<?> property : state.getProperties()) {
            if (!property.getName().equals(name)) { continue; }
            Comparable<?> value = state.getValue(property);
            if (value instanceof Direction direction) {
                return Optional.of(direction);
            }
        }
        return Optional.empty();
    }

    private Optional<BooleanProperty> booleanProperty(BlockState state, String name) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof BooleanProperty booleanProperty && property.getName().equals(name)) {
                return Optional.of(booleanProperty);
            }
        }
        return Optional.empty();
    }

    private Optional<String> propertyString(BlockState state, String name) {
        for (Property<?> property : state.getProperties()) {
            if (!property.getName().equals(name)) { continue; }
            return Optional.of(state.getValue(property).toString().toLowerCase(Locale.ROOT));
        }
        return Optional.empty();
    }

    private Direction facing(String providerState) {
        for (String entry : providerState.split(";")) {
            if (!entry.startsWith("facing=")) { continue; }
            Direction direction = Direction.byName(entry.substring("facing=".length()));
            return direction == null ? Direction.SOUTH : direction;
        }
        return Direction.SOUTH;
    }

    private ForgeRestResolution rejected(NpcResults.SleepStatus status, Optional<BlockId> actual,
            String code, String message) { return ForgeRestResolution.rejected(status, actual, code, message); }

    private Optional<BlockId> actualBlock(BlockState state) {
        var key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return key == null ? Optional.empty() : Optional.of(new BlockId(key.toString())); }
    private BlockPos toPos(BlockPosition position) { return new BlockPos(position.x(), position.y(), position.z()); }
    private BlockPosition fromPos(BlockPos position) {
        return new BlockPosition(position.getX(), position.getY(), position.getZ()); }
    private record BedCandidate(BlockPos requested, BlockState state, BlockId actualBlock) {
    }
}
