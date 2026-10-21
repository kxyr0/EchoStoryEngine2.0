package io.github.kxyr0.echotrigger.forge.provider.interaction.block;

import io.github.kxyr0.echotrigger.api.event.TriggerEvent;
import io.github.kxyr0.echotrigger.forge.provider.payload.BlockPayloads;
import io.github.kxyr0.echotrigger.forge.provider.payload.ForgeIds;
import io.github.kxyr0.echotrigger.forge.service.ForgeTriggerServices;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

public final class BlockInteractionEventBridge {
    private final ForgeTriggerServices services;
    private final Map<String, Long> recentInteractions = new LinkedHashMap<>();

    public BlockInteractionEventBridge(ForgeTriggerServices services) {
        this.services = services;
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide() != LogicalSide.SERVER || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (coalesced(player, event.getPos(), state)) {
            return;
        }
        Map<String, String> payload = BlockPayloads.interaction(event.getLevel(), event.getPos(),
                state, event.getHand());
        publish(player, "BlockInteracted", payload);
        publish(player, "ContainerOpened", payload);
    }

    @SubscribeEvent
    public void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && !event.isCanceled()
                && event.getLevel() instanceof Level level) {
            publishBlock(player, "BlockBroken", level, event.getPos(), event.getState());
        }
    }

    @SubscribeEvent
    public void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer player && !event.isCanceled() && event.getLevel() instanceof Level level) {
            publishBlock(player, "BlockPlaced", level, event.getPos(), event.getPlacedBlock());
        }
    }

    @SubscribeEvent
    public void onBedEntered(PlayerSleepInBedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Map<String, String> payload = new LinkedHashMap<>();
            payload.put("dimension", ForgeIds.dimension(player.level()));
            event.getOptionalPos().ifPresent(pos -> payload.put("position", ForgeIds.position(pos)));
            publish(player, "BedEntered", payload);
        }
    }

    @SubscribeEvent
    public void onBedLeft(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            publish(player, "BedLeft", Map.of(
                    "dimension", ForgeIds.dimension(player.level()),
                    "position", ForgeIds.position(player.blockPosition())));
        }
    }

    private void publishBlock(
            ServerPlayer player,
            String expressionName,
            Level level,
            BlockPos pos,
            BlockState state
    ) {
        publish(player, expressionName, BlockPayloads.block(level, pos, state));
    }

    private boolean coalesced(ServerPlayer player, BlockPos pos, BlockState state) {
        long tick = player.serverLevel().getGameTime();
        recentInteractions.entrySet().removeIf(entry -> entry.getValue() < tick - 1L);
        String key = player.getUUID() + "|" + ForgeIds.dimension(player.level())
                + "|" + pos + "|" + ForgeIds.block(state);
        Long previous = recentInteractions.put(key, tick);
        return previous != null && previous == tick;
    }

    private void publish(ServerPlayer player, String expressionName, Map<String, String> payload) {
        services.applyEvent(TriggerEvent.vanilla(ForgeIds.owner(player), player.serverLevel().getGameTime(),
                expressionName, payload));
    }
}
