package io.github.kxyr0.echoplayersleep.forge.bed;

import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepBedSnapshot;
import io.github.kxyr0.echoplayersleep.api.world.BlockPosition;
import io.github.kxyr0.echoplayersleep.forge.api.bed.PlayerSleepBedProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

public final class VanillaBedSleepProvider implements PlayerSleepBedProvider {
    public static final String PROVIDER_ID = "echo_player_sleep:vanilla_bed";

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean supports(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof BedBlock;
    }

    @Override
    public Optional<PlayerSleepBedSnapshot> snapshot(ServerPlayer player, BlockPos pos) {
        BlockState state = player.level().getBlockState(pos);
        if (!(state.getBlock() instanceof BedBlock)) {
            return Optional.empty();
        }
        Direction facing = state.getValue(BedBlock.FACING);
        BlockPos head = state.getValue(BedBlock.PART) == BedPart.HEAD ? pos : pos.relative(facing);
        List<BlockPosition> wake = wakeCandidates(head, facing);
        return Optional.of(new PlayerSleepBedSnapshot(ForgeSleepIds.dimension(player.level()),
                ForgeSleepIds.position(head), ForgeSleepIds.block(state), ForgeSleepIds.block(state),
                PROVIDER_ID, facing.getName(), wake));
    }

    @Override
    public boolean confirmsSleeping(ServerPlayer player, PlayerSleepBedSnapshot snapshot) {
        if (!player.isSleeping() || player.getSleepingPos().isEmpty()) {
            return false;
        }
        BlockPos head = ForgeSleepIds.blockPos(snapshot.canonicalPosition());
        Direction facing = Direction.byName(snapshot.facing());
        BlockPos foot = facing == null ? head : head.relative(facing.getOpposite());
        BlockPos sleeping = player.getSleepingPos().orElseThrow();
        return sleeping.equals(head) || sleeping.equals(foot);
    }

    private List<BlockPosition> wakeCandidates(BlockPos head, Direction facing) {
        List<BlockPosition> candidates = new ArrayList<>();
        candidates.add(ForgeSleepIds.position(head.relative(facing.getOpposite())));
        candidates.add(ForgeSleepIds.position(head.relative(facing.getClockWise())));
        candidates.add(ForgeSleepIds.position(head.relative(facing.getCounterClockWise())));
        candidates.add(ForgeSleepIds.position(head.above()));
        return candidates;
    }
}
