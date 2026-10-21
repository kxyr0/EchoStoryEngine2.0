package io.github.kxyr0.echoplayeractor.forge.inventory;

import io.github.kxyr0.echoplayeractor.api.command.PlayerActorFailure;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorOutcome;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorResult;
import io.github.kxyr0.echoplayeractor.api.pose.PlayerActorSnapshot;
import io.github.kxyr0.echoplayeractor.api.request.hotbar.PlayerHotbarItemRequest;
import io.github.kxyr0.echoplayeractor.api.request.hotbar.PlayerHotbarSelectionRequest;
import io.github.kxyr0.echoplayeractor.forge.body.ServerPlayerBodyResolver;
import java.util.function.Function;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

public final class PlayerHotbarCommand {
    private final ServerPlayerBodyResolver resolver;
    private final Function<ServerPlayer, PlayerActorSnapshot> snapshots;

    public PlayerHotbarCommand(
            ServerPlayerBodyResolver resolver,
            Function<ServerPlayer, PlayerActorSnapshot> snapshots
    ) {
        this.resolver = resolver;
        this.snapshots = snapshots;
    }

    public PlayerActorResult setItem(PlayerHotbarItemRequest request) {
        return resolver.resolve(request.actorId()).map(player -> setItem(player, request))
                .orElseGet(() -> PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_OFFLINE,
                        "Player actor is offline."));
    }

    public PlayerActorResult selectSlot(PlayerHotbarSelectionRequest request) {
        return resolver.resolve(request.actorId()).map(player -> selectSlot(player, request))
                .orElseGet(() -> PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_OFFLINE,
                        "Player actor is offline."));
    }

    private PlayerActorResult setItem(ServerPlayer player, PlayerHotbarItemRequest request) {
        Item item = resolveItem(request.itemId());
        if (item == null) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_ITEM_NOT_FOUND,
                    "Item ID was not found in the Minecraft item registry.");
        }
        ItemStack target = new ItemStack(item, request.count());
        if (request.count() > target.getMaxStackSize()) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_ITEM_COUNT_INVALID,
                    "Item count exceeds the stack maximum.");
        }
        int index = request.slot() - 1;
        ItemStack current = player.getInventory().getItem(index);
        if (!current.isEmpty() && !current.is(item) && !request.replace()) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_HOTBAR_SLOT_OCCUPIED,
                    "Hotbar slot contains a different item.");
        }
        player.getInventory().setItem(index, target);
        syncInventory(player);
        return PlayerActorResult.success(PlayerActorOutcome.HOTBAR_ITEM_SET, snapshots.apply(player),
                "Hotbar item set.");
    }

    private PlayerActorResult selectSlot(ServerPlayer player, PlayerHotbarSelectionRequest request) {
        int index = request.slot() - 1;
        player.getInventory().selected = index;
        player.connection.send(new ClientboundSetCarriedItemPacket(index));
        syncInventory(player);
        return PlayerActorResult.success(PlayerActorOutcome.HOTBAR_SLOT_SELECTED, snapshots.apply(player),
                "Hotbar slot selected.");
    }

    private Item resolveItem(String itemId) {
        ResourceLocation location = ResourceLocation.tryParse(itemId);
        if (location == null) {
            return null;
        }
        Item item = ForgeRegistries.ITEMS.getValue(location);
        return item == null || item == Items.AIR ? null : item;
    }

    private void syncInventory(ServerPlayer player) {
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
    }
}
