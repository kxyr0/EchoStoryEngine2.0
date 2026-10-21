package io.github.kxyr0.echowardrobe.testmod.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kxyr0.echowardrobe.testmod.EchoWardrobeTestmod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

/**
 * Development-only equipment helpers.
 */
public final class EchoWardrobeTestCommands {
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_wardrobe_test")
                .then(Commands.literal("equip_origin_full").executes(context -> equipOrigin(context.getSource())))
                .then(Commands.literal("equip_classic_full").executes(context -> equipClassic(context.getSource())))
                .then(Commands.literal("clear").executes(context -> clear(context.getSource())))
                .then(Commands.literal("cycle_pants").executes(context -> cyclePants(context.getSource())))
                .then(Commands.literal("show_portrait").executes(context -> showPortrait(context.getSource()))));
    }

    private int equipOrigin(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        equip(source, EchoWardrobeTestmod.ORIGIN_GLASSES, EchoWardrobeTestmod.ORIGIN_ROBE,
                EchoWardrobeTestmod.ORIGIN_PANTS, EchoWardrobeTestmod.ORIGIN_BOOTS);
        return say(source, "origin full equipped");
    }

    private int equipClassic(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        equip(source, EchoWardrobeTestmod.CLASSIC_GLASSES, EchoWardrobeTestmod.CLASSIC_HOODIE,
                EchoWardrobeTestmod.CLASSIC_PANTS, EchoWardrobeTestmod.CLASSIC_BOOTS);
        return say(source, "classic full equipped");
    }

    private int clear(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        source.getPlayerOrException().setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        source.getPlayerOrException().setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        source.getPlayerOrException().setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        source.getPlayerOrException().setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        return say(source, "wardrobe equipment cleared");
    }

    private int cyclePants(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ItemStack current = source.getPlayerOrException().getItemBySlot(EquipmentSlot.LEGS);
        boolean alternative = current.is(EchoWardrobeTestmod.CLASSIC_PANTS.get());
        RegistryObject<net.minecraft.world.item.Item> next = alternative ? EchoWardrobeTestmod.CLASSIC_ALT_PANTS
                : EchoWardrobeTestmod.CLASSIC_PANTS;
        source.getPlayerOrException().setItemSlot(EquipmentSlot.LEGS, new ItemStack(next.get()));
        return say(source, "pants cycled");
    }

    private int showPortrait(CommandSourceStack source) {
        return say(source, "open dialogue portrait acceptance manually; command is a marker only");
    }

    private void equip(CommandSourceStack source, RegistryObject<net.minecraft.world.item.Item> head,
            RegistryObject<net.minecraft.world.item.Item> chest, RegistryObject<net.minecraft.world.item.Item> legs,
            RegistryObject<net.minecraft.world.item.Item> feet)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        source.getPlayerOrException().setItemSlot(EquipmentSlot.HEAD, new ItemStack(head.get()));
        source.getPlayerOrException().setItemSlot(EquipmentSlot.CHEST, new ItemStack(chest.get()));
        source.getPlayerOrException().setItemSlot(EquipmentSlot.LEGS, new ItemStack(legs.get()));
        source.getPlayerOrException().setItemSlot(EquipmentSlot.FEET, new ItemStack(feet.get()));
    }

    private int say(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
