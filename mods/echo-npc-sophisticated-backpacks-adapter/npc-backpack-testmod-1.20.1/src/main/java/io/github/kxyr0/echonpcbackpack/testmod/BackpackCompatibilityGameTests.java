package io.github.kxyr0.echonpcbackpack.testmod;

import io.github.kxyr0.echonpcbackpack.forge.sophisticated.SophisticatedBackpackAccess;
import io.github.kxyr0.echonpcbackpack.forge.sophisticated.SophisticatedBackpackPlacer;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;

@GameTestHolder("echo_npc_sophisticated_backpacks")
@PrefixGameTestTemplate(false)
public final class BackpackCompatibilityGameTests {
    private BackpackCompatibilityGameTests() {
    }

    @GameTest(template = "empty")
    public static void officialPlacementPreservesUuidColorsAndContents(GameTestHelper helper) {
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("sophisticatedbackpacks", "backpack"));
        helper.assertTrue(item instanceof BackpackItem, "Base Sophisticated Backpack item is missing.");
        ItemStack stack = new ItemStack(item);
        UUID uuid = UUID.randomUUID();
        stack.getOrCreateTag().putUUID("contentsUuid", uuid);
        BackpackItem.setColors(stack, 0x123456, 0x654321);
        CompoundTag contents = new CompoundTag();
        contents.putString("testMarker", "exact-payload");
        BackpackStorage.get().setBackpackContents(uuid, contents);
        BlockPos sourcePosition = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos destination = helper.absolutePos(new BlockPos(3, 2, 1));
        helper.getLevel().setBlock(sourcePosition.below(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(destination.below(), Blocks.STONE.defaultBlockState(), 3);
        SophisticatedBackpackAccess access = new SophisticatedBackpackAccess();
        SophisticatedBackpackPlacer placer = new SophisticatedBackpackPlacer(access);
        placer.place(helper.getLevel(), sourcePosition, stack, Direction.SOUTH);
        var source = access.inspect(helper.getLevel(), sourcePosition).orElseThrow();
        var removal = access.removeExact(helper.getLevel(), sourcePosition,
                source.reference().fingerprint());
        helper.assertTrue(removal.status() == SophisticatedBackpackAccess.Status.REMOVED,
                "Exact source backpack was not removed.");
        var result = placer.place(helper.getLevel(), destination, source.stack(), Direction.SOUTH);
        helper.assertTrue(result.succeeded(), result.message());
        var actual = access.inspect(helper.getLevel(), destination).orElseThrow();
        helper.assertTrue(actual.reference().contentsUuid().equals(uuid), "contentsUuid changed.");
        helper.assertTrue(actual.reference().fingerprint().equals(source.reference().fingerprint()),
                "Exact normalized payload fingerprint changed.");
        helper.assertTrue(actual.mainColor() == 0x123456 && actual.accentColor() == 0x654321,
                "Backpack colors changed.");
        helper.assertTrue(BackpackStorage.get().getOrCreateBackpackContents(uuid)
                .getString("testMarker").equals("exact-payload"), "Backpack contents changed.");
        helper.succeed();
    }
}
