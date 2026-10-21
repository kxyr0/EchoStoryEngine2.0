package io.github.kxyr0.echoitemattachments.testmod;

import io.github.kxyr0.echoitemattachments.api.command.ItemSelector;
import io.github.kxyr0.echoitemattachments.api.model.WorldPosition;
import io.github.kxyr0.echoitemattachments.forge.persistence.ItemStackCodec;
import io.github.kxyr0.echoitemattachments.forge.runtime.source.SourceItemResolver;
import io.github.kxyr0.echoitemattachments.forge.runtime.source.SourceSelection;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EchoItemAttachmentsTestMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AttachmentGameTests {
    private AttachmentGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void exactStackNbtRoundTrips(GameTestHelper helper) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK, 3);
        CompoundTag custom = book.getOrCreateTag();
        custom.putString("echo_unique_payload", "JDH-IA1");
        custom.putInt("CustomModelData", 412);
        book.setHoverName(Component.literal("Molly's exact book"));
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(Enchantments.SHARPNESS, 3);
        sword.setDamageValue(87);
        for (ItemStack original : java.util.List.of(book, sword)) {
            ItemStack restored = new ItemStackCodec().decode(new ItemStackCodec().encode(original));
            helper.assertTrue(ItemStack.matches(original, restored),
                    "ItemStack NBT/count/glint/damage must survive custody codec");
        }
        ItemStack world = new ItemStack(Items.BOOK, 15);
        world.getOrCreateTag().putString("echo_unique_payload", "split-witness");
        ItemStack payload = world.copyWithCount(1);
        world.shrink(1);
        helper.assertTrue(world.getCount() == 14 && payload.getCount() == 1
                && ItemStack.isSameItemSameTags(world, payload), "Exact stack split must preserve both witnesses");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void ambiguousWorldSourceIsRejected(GameTestHelper helper) {
        Vec3 centre = helper.absoluteVec(new Vec3(2.0D, 1.0D, 2.0D));
        helper.getLevel().addFreshEntity(new ItemEntity(helper.getLevel(), centre.x, centre.y, centre.z,
                new ItemStack(Items.BOOK)));
        helper.getLevel().addFreshEntity(new ItemEntity(helper.getLevel(), centre.x + 0.2D, centre.y, centre.z,
                new ItemStack(Items.BOOK)));
        WorldPosition position = new WorldPosition(helper.getLevel().dimension().location().toString(),
                centre.x, centre.y, centre.z);
        SourceSelection result = new SourceItemResolver().resolve(helper.getLevel().getServer(), position,
                new ItemSelector(java.util.Optional.of("minecraft:book"), 1, 0.75D));
        helper.assertTrue(result.status() == SourceSelection.Status.AMBIGUOUS,
                "Resolver must not pick first or nearest when two sources match");
        helper.succeed();
    }
}
