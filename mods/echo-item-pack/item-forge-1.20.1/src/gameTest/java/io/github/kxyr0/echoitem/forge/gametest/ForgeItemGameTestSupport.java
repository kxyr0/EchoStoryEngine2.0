package io.github.kxyr0.echoitem.forge.gametest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoitem.forge.item.EchoWeaponItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

final class ForgeItemGameTestSupport {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ForgeItemGameTestSupport() {
    }

    static void mark(String testName) {
        LOGGER.info("EITEM-GAMETEST-RUN {}", testName);
    }

    static EchoWeaponItem weapon(GameTestHelper helper, String namespace, String path) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(namespace, path));
        helper.assertTrue(item instanceof EchoWeaponItem, namespace + ":" + path + " must be an EchoWeaponItem");
        return (EchoWeaponItem) item;
    }

    static boolean registered(String namespace, String path) {
        return ForgeRegistries.ITEMS.containsKey(new ResourceLocation(namespace, path));
    }

    static void assertCommandParses(GameTestHelper helper, CommandSourceStack source, String command) {
        CommandDispatcher<CommandSourceStack> dispatcher = helper.getLevel().getServer()
                .getCommands()
                .getDispatcher();
        ParseResults<CommandSourceStack> parsed = dispatcher.parse(command, source);
        helper.assertTrue(parsed.getReader().getRemaining().isEmpty(), command + " must parse to the end");
        helper.assertTrue(parsed.getExceptions().isEmpty(), command + " must not report parse exceptions");
    }

    static int execute(GameTestHelper helper, CommandSourceStack source, String command) {
        try {
            return helper.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
        } catch (CommandSyntaxException ex) {
            helper.assertTrue(false, command + " failed: " + ex.getMessage());
            return 0;
        }
    }

    static double modifierAmount(EchoWeaponItem item, Attribute attribute) {
        return item.getDefaultAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                .get(attribute)
                .stream()
                .mapToDouble(AttributeModifier::getAmount)
                .sum();
    }

    static int modifierCount(EchoWeaponItem item, Attribute attribute) {
        return item.getDefaultAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                .get(attribute)
                .size();
    }

    static int countItem(ServerPlayer player, Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
