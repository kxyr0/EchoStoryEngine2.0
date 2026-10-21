package io.github.kxyr0.echoiteminspection.testmod;

import io.github.kxyr0.echoiteminspection.api.ItemStackSnapshot;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EchoItemInspectionTestMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InspectionStackGameTests {
    private InspectionStackGameTests() { }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void snapshotPreservesCountAndExactNbt(GameTestHelper helper) {
        ItemStack original = new ItemStack(Items.WRITTEN_BOOK, 7);
        original.setHoverName(Component.literal("Inspection witness"));
        CompoundTag tag = original.getOrCreateTag();
        tag.putString("title", "Exact book");
        tag.putString("author", "EII1 test fixture");
        tag.putInt("CustomModelData", 19);
        tag.putString("echo_payload", "specific-stack-instance");
        ItemStackSnapshot snapshot = new ItemStackSnapshot(original);
        helper.assertTrue(ItemStack.matches(original, snapshot.copyStack()), "Snapshot must preserve exact count and NBT");
        helper.assertTrue(snapshot.count() == 7 && snapshot.itemId().equals("minecraft:written_book"), "Snapshot identity/count");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void snapshotOwnsDefensiveCopies(GameTestHelper helper) {
        ItemStack original = new ItemStack(Items.PAPER, 11);
        original.getOrCreateTag().putString("witness", "before");
        ItemStackSnapshot snapshot = new ItemStackSnapshot(original);
        original.setCount(2);
        original.getOrCreateTag().putString("witness", "after");
        ItemStack exposed = snapshot.copyStack();
        exposed.shrink(1);
        exposed.getOrCreateTag().putString("witness", "renderer-mutation");
        ItemStack fresh = snapshot.copyStack();
        helper.assertTrue(fresh.getCount() == 11 && fresh.getTag().getString("witness").equals("before"),
                "Neither source nor renderer mutations may change snapshot");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void blockAndCustomModelStackSurviveSnapshot(GameTestHelper helper) {
        ItemStack block = new ItemStack(Items.STONE, 64);
        ItemStack custom = new ItemStack(EchoItemInspectionTestMod.TOKEN.get(), 13);
        custom.getOrCreateTag().putInt("CustomModelData", 19);
        for (ItemStack stack : java.util.List.of(block, custom)) {
            helper.assertTrue(ItemStack.matches(stack, new ItemStackSnapshot(stack).copyStack()),
                    "Block/custom model stack must retain exact input to ItemRenderer");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void damageAndGlintSurviveSnapshot(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(87);
        sword.enchant(Enchantments.SHARPNESS, 3);
        ItemStack copy = new ItemStackSnapshot(sword).copyStack();
        helper.assertTrue(copy.hasFoil() && copy.getDamageValue() == 87 && ItemStack.matches(sword, copy),
                "Renderer input must preserve enchantments, glint and damage");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void vanillaRightClickSplitsUnclaimedContainerStack(GameTestHelper helper) {
        var player = helper.makeMockSurvivalPlayer();
        ChestMenu menu = ChestMenu.oneRow(17, player.getInventory());
        menu.getSlot(0).set(new ItemStack(Items.COBBLESTONE, 64));
        menu.clicked(0, 1, ClickType.PICKUP, player);
        helper.assertTrue(menu.getSlot(0).getItem().getCount() == 32 && menu.getCarried().getCount() == 32,
                "Unclaimed RMB must retain vanilla half-stack behavior");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void snapshotNeverMutatesContainerSlot(GameTestHelper helper) {
        var player = helper.makeMockSurvivalPlayer();
        ChestMenu menu = ChestMenu.oneRow(18, player.getInventory());
        ItemStack original = new ItemStack(EchoItemInspectionTestMod.TOKEN.get(), 64);
        original.getOrCreateTag().putInt("CustomModelData", 19);
        menu.getSlot(0).set(original);
        ItemStack before = original.copy();
        new ItemStackSnapshot(menu.getSlot(0).getItem()).copyStack().shrink(32);
        helper.assertTrue(ItemStack.matches(before, menu.getSlot(0).getItem()) && menu.getCarried().isEmpty(),
                "Presentation capture cannot split, move, duplicate, or pick up the slot item");
        helper.succeed();
    }
}
