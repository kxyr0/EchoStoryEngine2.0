package io.github.kxyr0.echoiteminspection.testmod.client.ipla;

import com.akicater.blocks.LayingItemEntity;
import com.akicater.network.ItemPlacePayload;
import io.github.kxyr0.echoiteminspection.testmod.EchoItemInspectionTestMod;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class IplaSmokeFixture {
    static final BlockPos ITEM = new BlockPos(0, 100, -2);
    static final BlockPos OTHER = new BlockPos(2, 100, -2);
    static final BlockPos WALL = new BlockPos(0, 101, 0);
    private volatile boolean ready;
    private volatile boolean verified;
    private volatile boolean unchanged;
    private LayingItemEntity originalEntity;
    private ListTag inventoryBefore;
    private java.util.List<Float> rotationsBefore;
    private java.util.List<Boolean> quadBefore;
    private Vec3 look = Vec3.atLowerCornerOf(ITEM).add(0.5, 0.03125, 0.5);
    private Vec3 position = new Vec3(0.5, 100, 1.5);

    void prepare(Minecraft minecraft) {
        var id = minecraft.player.getUUID();
        minecraft.getSingleplayerServer().execute(() -> {
            var player = minecraft.getSingleplayerServer().getPlayerList().getPlayer(id);
            var level = player.serverLevel();
            player.closeContainer();
            player.setGameMode(GameType.SURVIVAL);
            player.setNoGravity(true);
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.getAbilities().invulnerable = true;
            player.onUpdateAbilities();
            player.teleportTo(0.5, 100, 1.5);
            player.getInventory().clearContent();
            player.getInventory().selected = 0;
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
            for (int x = 0; x < 10; x++) for (int z = 0; z < 10; z++) {
                place(level, player, new BlockPos(8 + x * 2, 100, 8 + z * 2), token());
            }
            place(level, player, ITEM, token());
            place(level, player, OTHER, new ItemStack(Items.COBBLESTONE));
            level.setBlockAndUpdate(WALL, Blocks.AIR.defaultBlockState());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PAPER, 7));
            player.inventoryMenu.broadcastChanges();
            originalEntity = (LayingItemEntity) level.getBlockEntity(ITEM);
            rotationsBefore = java.util.List.copyOf(originalEntity.rot);
            quadBefore = java.util.List.copyOf(originalEntity.quad);
            inventoryBefore = player.getInventory().save(new ListTag()).copy();
            ready = true;
        });
    }

    private static void place(ServerLevel level, net.minecraft.server.level.ServerPlayer player,
            BlockPos position, ItemStack stack) {
        level.setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(position.below(), Blocks.STONE.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new BlockHitResult(Vec3.atLowerCornerOf(position).add(0.5, 0, 0.5),
                Direction.UP, position.below(), false);
        ItemPlacePayload.receive(player, position.below(), hit);
    }

    boolean ready(Minecraft minecraft) {
        return ready && minecraft.level.getBlockEntity(ITEM) instanceof LayingItemEntity entity
                && ItemStack.matches(token(), entity.inv.get(4))
                && minecraft.player.getInventory().getItem(0).getCount() == 7;
    }

    String describe(Minecraft minecraft) {
        var entity = minecraft.level.getBlockEntity(ITEM);
        return "prepared=" + ready + " block=" + minecraft.level.getBlockState(ITEM)
                + " entity=" + entity + " stack=" + (entity instanceof LayingItemEntity item ? item.inv.get(4).save(
                        new net.minecraft.nbt.CompoundTag()) : "none")
                + " inventory0=" + minecraft.player.getInventory().getItem(0).save(new net.minecraft.nbt.CompoundTag());
    }

    void stabilize(Minecraft minecraft) {
        minecraft.player.setPos(position);
        minecraft.player.setNoGravity(true);
        minecraft.player.setDeltaMovement(Vec3.ZERO);
        Vec3 delta = look.subtract(minecraft.player.getEyePosition());
        float yaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
        minecraft.player.setYRot(yaw);
        minecraft.player.yRotO = yaw;
        minecraft.player.setXRot(pitch);
        minecraft.player.xRotO = pitch;
    }

    void lookAt(BlockPos target) { look = Vec3.atLowerCornerOf(target).add(0.5, 0.03125, 0.5); }
    void lookAway() { look = new Vec3(10, 101, 1.5); }
    void far(boolean far) { position = new Vec3(0.5, 100, far ? 8.5 : 1.5); }

    void mutate(Consumer<ServerLevel> action) {
        Minecraft.getInstance().getSingleplayerServer().execute(() ->
                action.accept(Minecraft.getInstance().getSingleplayerServer().overworld()));
    }

    void restore() {
        mutate(level -> {
            var player = level.getServer().getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
            var hand = player.getMainHandItem().copy();
            place(level, player, ITEM, token());
            player.setItemInHand(InteractionHand.MAIN_HAND, hand);
            player.inventoryMenu.broadcastChanges();
        });
    }

    void verifyUnchanged(Minecraft minecraft) {
        minecraft.getSingleplayerServer().execute(() -> {
            var player = minecraft.getSingleplayerServer().getPlayerList().getPlayer(minecraft.player.getUUID());
            var entity = player.serverLevel().getBlockEntity(ITEM);
            unchanged = entity == originalEntity && !originalEntity.isRemoved()
                    && ItemStack.matches(token(), originalEntity.inv.get(4))
                    && rotationsBefore.equals(originalEntity.rot) && quadBefore.equals(originalEntity.quad)
                    && inventoryBefore.equals(player.getInventory().save(new ListTag()));
            verified = true;
        });
    }

    boolean verified() { return verified; }
    boolean unchanged() { return unchanged; }

    static ItemStack token() {
        var stack = new ItemStack(EchoItemInspectionTestMod.TOKEN.get());
        stack.setHoverName(Component.literal("Точное письмо IPLA"));
        stack.getOrCreateTag().putInt("CustomModelData", 19);
        stack.getOrCreateTag().putInt("story_document", 4);
        stack.getOrCreateTag().putInt("Damage", 17);
        stack.enchant(Enchantments.UNBREAKING, 1);
        return stack;
    }
}
