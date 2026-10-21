package io.github.kxyr0.echonpcbackpack.forge.sophisticated;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;

/**
 * Uses Sophisticated Backpacks' public placement lifecycle and verifies the exact UUID afterwards.
 */
public final class SophisticatedBackpackPlacer {
    private final SophisticatedBackpackAccess access;

    public SophisticatedBackpackPlacer(SophisticatedBackpackAccess access) {
        this.access = java.util.Objects.requireNonNull(access, "access");
    }

    public PlacementResult place(ServerLevel level, BlockPos position, ItemStack payload, Direction facing) {
        if (!(payload.getItem() instanceof BackpackItem backpackItem)) {
            return new PlacementResult(Status.NOT_BACKPACK, "Payload item is not a Sophisticated Backpack.");
        }
        if (!level.getBlockState(position).canBeReplaced()) {
            return access.exactAt(level, position, payload)
                    ? new PlacementResult(Status.ALREADY_PLACED, "Expected backpack already occupies destination.")
                    : new PlacementResult(Status.OCCUPIED, "Destination is occupied.");
        }
        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        ItemStack previous = player.getMainHandItem().copy();
        ItemStack working = payload.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, working);
        player.setPos(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D);
        player.setYRot(facing.toYRot());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(position), Direction.UP, position, false);
        InteractionResult result;
        try {
            result = backpackItem.tryPlace(player, facing, new BlockPlaceContext(
                    new UseOnContext(player, InteractionHand.MAIN_HAND, hit)));
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, previous);
        }
        if (!result.consumesAction()) {
            return new PlacementResult(Status.FAILED, "Sophisticated Backpacks rejected placement.");
        }
        if (!access.exactAt(level, position, payload)) {
            return new PlacementResult(Status.VERIFICATION_FAILED,
                    "Placed block does not match expected UUID and payload fingerprint.");
        }
        return new PlacementResult(Status.PLACED, "Exact backpack placed and verified.");
    }

    public record PlacementResult(Status status, String message) {
        public boolean succeeded() {
            return status == Status.PLACED || status == Status.ALREADY_PLACED;
        }
    }

    public enum Status {
        PLACED,
        ALREADY_PLACED,
        OCCUPIED,
        NOT_BACKPACK,
        FAILED,
        VERIFICATION_FAILED
    }
}
