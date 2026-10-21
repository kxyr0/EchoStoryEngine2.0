package io.github.kxyr0.echoitem.forge.command;

import com.mojang.brigadier.context.CommandContext;
import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.definition.WeaponDefinition;
import io.github.kxyr0.echoitem.core.reload.ItemReloadOutcome;
import io.github.kxyr0.echoitem.forge.ForgeItemRuntimeState;
import io.github.kxyr0.echoitem.forge.network.CatalogSyncChannel;
import io.github.kxyr0.echoitem.forge.reload.EquippedWeaponRefresher;
import io.github.kxyr0.echoitem.forge.registry.RegisteredWeaponItems;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class EchoItemCommandActions {
    private final ForgeItemRuntimeState state;
    private final RegisteredWeaponItems items;
    private final EquippedWeaponRefresher equippedWeapons;
    private final CatalogSyncChannel catalogSync;
    private final EchoItemCommandFormatting formatting = new EchoItemCommandFormatting();

    EchoItemCommandActions(ForgeItemRuntimeState state, RegisteredWeaponItems items,
                           EquippedWeaponRefresher equippedWeapons, CatalogSyncChannel catalogSync) {
        this.state = Objects.requireNonNull(state, "state");
        this.items = Objects.requireNonNull(items, "items");
        this.equippedWeapons = Objects.requireNonNull(equippedWeapons, "equippedWeapons");
        this.catalogSync = Objects.requireNonNull(catalogSync, "catalogSync");
    }

    int reload(CommandContext<CommandSourceStack> context) {
        ItemReloadOutcome outcome = state.reload();
        if (outcome.applied()) {
            equippedWeapons.refresh(context.getSource().getServer());
            catalogSync.sendToAll(context.getSource().getServer(), state.catalog().snapshot());
        }
        send(context, "Reload applied=" + outcome.applied() + ", changed scripts="
                + outcome.counters().changedWeaponFiles() + ", parser calls=" + outcome.counters().parserCalls()
                + ", binder calls=" + outcome.counters().binderCalls());
        return outcome.applied() ? 1 : 0;
    }

    int validate(CommandContext<CommandSourceStack> context) {
        ItemReloadOutcome outcome = state.validateOnly();
        send(context, "Validate valid=" + outcome.applied() + ", diagnostics=" + outcome.diagnostics().size()
                + ", source count=" + outcome.snapshot().weaponIds().size());
        return outcome.applied() ? 1 : 0;
    }

    int list(CommandContext<CommandSourceStack> context) {
        var ids = state.catalog().snapshot().weaponIds().stream().sorted().toList();
        if (ids.isEmpty()) {
            send(context, "Echo Item catalog is empty.");
            return 1;
        }
        ids.forEach(id -> send(context, "Echo Item: " + id));
        return ids.size();
    }

    int inspect(CommandContext<CommandSourceStack> context) {
        EchoItemId id = itemId(context);
        WeaponDefinition definition = state.catalog().current(id);
        send(context, formatting.inspect(definition, state.registeredIds().contains(id)));
        return definition.available() ? 1 : 0;
    }

    int sources(CommandContext<CommandSourceStack> context) {
        EchoItemId id = itemId(context);
        WeaponDefinition definition = state.catalog().current(id);
        send(context, "source id=" + id + ", fingerprint=" + definition.sourceFingerprint().sha256()
                + ", precedence=valid external > packaged > last-known-good");
        return 1;
    }

    int pending(CommandContext<CommandSourceStack> context) {
        String pending = state.registrationPlan().pendingRestartIds().isEmpty()
                ? "No pending item registrations."
                : state.registrationPlan().pendingRestartIds().toString();
        send(context, pending);
        return state.registrationPlan().pendingRestartIds().size();
    }

    int giveOne(CommandContext<CommandSourceStack> context) {
        return giveCount(context, 1);
    }

    int give(CommandContext<CommandSourceStack> context) {
        return giveCount(context, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "count"));
    }

    private int giveCount(CommandContext<CommandSourceStack> context, int count) {
        try {
            EchoItemId id = itemId(context);
            Item item = Optional.ofNullable(items.item(id)).orElseThrow();
            EntityArgument.getPlayer(context, "player").addItem(new ItemStack(item, count));
            send(context, "Gave " + count + " x " + id + ".");
            return count;
        } catch (Exception ex) {
            send(context, "Cannot give Echo item: " + ex.getMessage());
            return 0;
        }
    }

    private EchoItemId itemId(CommandContext<CommandSourceStack> context) {
        var resource = ResourceLocationArgument.getId(context, "item_id");
        return new EchoItemId(resource.getNamespace(), resource.getPath());
    }

    private void send(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendSuccess(() -> Component.literal(message), false);
    }
}
