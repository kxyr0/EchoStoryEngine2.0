package io.github.kxyr0.echoitem.forge.gametest;

import static io.github.kxyr0.echoitem.forge.gametest.ForgeItemGameTestSupport.assertCommandParses;
import static io.github.kxyr0.echoitem.forge.gametest.ForgeItemGameTestSupport.countItem;
import static io.github.kxyr0.echoitem.forge.gametest.ForgeItemGameTestSupport.execute;
import static io.github.kxyr0.echoitem.forge.gametest.ForgeItemGameTestSupport.mark;
import static io.github.kxyr0.echoitem.forge.gametest.ForgeItemGameTestSupport.modifierAmount;
import static io.github.kxyr0.echoitem.forge.gametest.ForgeItemGameTestSupport.modifierCount;
import static io.github.kxyr0.echoitem.forge.gametest.ForgeItemGameTestSupport.registered;
import static io.github.kxyr0.echoitem.forge.gametest.ForgeItemGameTestSupport.weapon;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.core.manifest.RegisteredItemManifest;
import io.github.kxyr0.echoitem.core.manifest.RegisteredItemManifestStore;
import io.github.kxyr0.echoitem.forge.EchoItemPackMod;
import io.github.kxyr0.echoitem.forge.item.EchoWeaponItem;
import io.github.kxyr0.echoitem.forge.reload.EquippedWeaponRefresher;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.gametest.GameTestHolder;

@GameTestHolder(EchoItemPackMod.MOD_ID)
public final class ForgeItemGameTests {
    private static final double EPSILON = 0.0001D;
    private static final String RUN_SUFFIX = Long.toString(Math.abs(System.nanoTime()), 36).toLowerCase(Locale.ROOT);

    private ForgeItemGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void startupRegistersPackagedWeapons(GameTestHelper helper) {
        mark("ForgeItemGameTests.startupRegistersPackagedWeapons");
        weapon(helper, "jdh", "void_sword");
        weapon(helper, "jdh", "training_dagger");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void weaponValuesComeFromCatalog(GameTestHelper helper) {
        mark("ForgeItemGameTests.weaponValuesComeFromCatalog");
        EchoWeaponItem item = weapon(helper, "jdh", "void_sword");
        ItemStack stack = new ItemStack(item);

        helper.assertTrue("Void Sword".equals(item.getName(stack).getString()), "display name must come from script");
        helper.assertTrue(item.getMaxDamage(stack) == 2031, "durability must come from script");
        helper.assertTrue(item.getEnchantmentValue() == 18, "enchantability must come from script");
        helper.assertTrue(item.getRarity(stack) == Rarity.EPIC, "rarity must come from script");
        helper.assertTrue(item.isFoil(stack), "glint flag must come from script");
        helper.assertTrue(item.isFireResistant(), "fire resistance must come from script");
        assertNear(helper, 12.0D, modifierAmount(item, Attributes.ATTACK_DAMAGE), "attack damage modifier");
        assertNear(helper, -2.4D, modifierAmount(item, Attributes.ATTACK_SPEED), "attack speed modifier");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void serverTickReloadsExistingExternalWeapon(GameTestHelper helper) {
        mark("ForgeItemGameTests.serverTickReloadsExistingExternalWeapon");
        Path source = externalTrainingDaggerSource();
        deleteIfExists(helper, source);
        execute(helper, helper.getLevel().getServer().createCommandSourceStack().withPermission(4), "echo_item reload");
        EchoWeaponItem original = weapon(helper, "jdh", "training_dagger");
        Player player = helper.makeMockSurvivalPlayer();
        ItemStack held = new ItemStack(original);
        player.setItemSlot(EquipmentSlot.MAINHAND, held);
        Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 2, 0);
        target.setHealth(20.0F);
        boolean[] attacked = new boolean[] {false};
        writeExternalDagger(helper, source);

        helper.succeedWhen(() -> {
            EchoWeaponItem item = weapon(helper, "jdh", "training_dagger");
            ItemStack stack = new ItemStack(item);
            helper.assertTrue(item == original, "hot reload must keep the same Java registry object");
            helper.assertTrue("Training Dagger Hot".equals(item.getName(stack).getString()),
                    "server tick reload must apply external name");
            assertNear(helper, 6.0D, modifierAmount(item, Attributes.ATTACK_DAMAGE),
                    "server tick reload damage modifier");
            helper.assertTrue(modifierCount(item, Attributes.ATTACK_DAMAGE) == 1,
                    "server tick reload must not duplicate damage modifiers");
            new EquippedWeaponRefresher().refreshEntity(player);
            helper.assertTrue(player.getMainHandItem() == held, "refresh must keep the held stack object");
            assertNear(helper, 7.0D, player.getAttributeValue(Attributes.ATTACK_DAMAGE),
                    "held player attack damage");
            assertNear(helper, 3.0D, player.getAttributeValue(Attributes.ATTACK_SPEED),
                    "held player attack speed");
            if (!attacked[0] && player.getAttackStrengthScale(0.5F) >= 1.0F) {
                player.moveTo(target.getX(), target.getY(), target.getZ());
                player.attack(target);
                attacked[0] = true;
            }
            if (!attacked[0]) {
                player.tick();
            }
            helper.assertTrue(target.getHealth() <= 14.0F,
                    "hot-reloaded weapon damage must affect a real entity hit");
            deleteIfExists(helper, source);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void newLiveWeaponIsPersistedAsPendingRestart(GameTestHelper helper) {
        mark("ForgeItemGameTests.newLiveWeaponIsPersistedAsPendingRestart");
        String pathName = "pending_" + RUN_SUFFIX;
        EchoItemId id = EchoItemId.parse("jdh:" + pathName);
        Path source = externalNewWeaponSource(pathName);
        deleteIfExists(helper, source);
        writeNewExternalWeapon(helper, source, id);

        helper.succeedWhen(() -> {
            CommandSourceStack commands = helper.getLevel().getServer().createCommandSourceStack().withPermission(4);
            helper.assertTrue(execute(helper, commands, "echo_item pending") >= 1,
                    "new live weapon must be listed as pending restart");
            helper.assertTrue(!registered("jdh", pathName), "new live weapon must not mutate frozen registry");
            helper.assertTrue(pendingInManifest(id), "pending restart ID must be persisted in manifest");
            deleteIfExists(helper, source);
            removeManifestRecord(helper, id);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void commandsParseCoreWorkflows(GameTestHelper helper) {
        mark("ForgeItemGameTests.commandsParseCoreWorkflows");
        CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack().withPermission(4);

        assertCommandParses(helper, source, "echo_item reload");
        assertCommandParses(helper, source, "echo_item validate");
        assertCommandParses(helper, source, "echo_item pending");
        assertCommandParses(helper, source, "echo_item inspect jdh:void_sword");
        assertCommandParses(helper, source, "echo_item sources jdh:void_sword");
        assertCommandParses(helper, source, "echo_item give @s jdh:void_sword 2");
        helper.assertTrue(execute(helper, source, "echo_item validate") == 1, "validate must execute successfully");
        helper.assertTrue(execute(helper, source, "echo_item inspect jdh:void_sword") == 1,
                "inspect must find registered weapon");
        helper.assertTrue(execute(helper, source, "echo_item pending") >= 0, "pending must execute successfully");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void giveCommandCreatesStack(GameTestHelper helper) {
        mark("ForgeItemGameTests.giveCommandCreatesStack");
        EchoWeaponItem item = weapon(helper, "jdh", "void_sword");
        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.getInventory().clearContent();
        CommandSourceStack source = player.createCommandSourceStack().withPermission(4);

        int result = execute(helper, source, "echo_item give @s jdh:void_sword 2");

        helper.assertTrue(result == 2, "give command must return the created count");
        helper.assertTrue(countItem(player, item) == 2, "fake player inventory must contain the given stack");
        helper.succeed();
    }

    private static void assertNear(GameTestHelper helper, double expected, double actual, String label) {
        helper.assertTrue(Math.abs(expected - actual) < EPSILON,
                label + " expected " + expected + " but was " + actual);
    }

    private static Path externalTrainingDaggerSource() {
        return FMLPaths.GAMEDIR.get().resolve("story/item-pack/jdh/items/weapons/daggers/training_dagger.weapon.es");
    }

    private static Path externalNewWeaponSource(String pathName) {
        return FMLPaths.GAMEDIR.get().resolve("story/item-pack/jdh/items/weapons/maces/" + pathName + ".weapon.es");
    }

    private static void writeExternalDagger(GameTestHelper helper, Path source) {
        try {
            Files.createDirectories(source.getParent());
            Files.writeString(source, externalDaggerText(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            helper.assertTrue(false, "failed to write external dagger source: " + ex.getMessage());
        }
    }

    private static void writeNewExternalWeapon(GameTestHelper helper, Path source, EchoItemId id) {
        try {
            Files.createDirectories(source.getParent());
            Files.writeString(source, externalNewWeaponText(id), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            helper.assertTrue(false, "failed to write new external weapon source: " + ex.getMessage());
        }
    }

    private static void deleteIfExists(GameTestHelper helper, Path source) {
        try {
            Files.deleteIfExists(source);
        } catch (IOException ex) {
            helper.assertTrue(false, "failed to delete external dagger source: " + ex.getMessage());
        }
    }

    private static boolean pendingInManifest(EchoItemId id) {
        return manifestStore().load().find(id).filter(record -> record.pendingRegistration()).isPresent();
    }

    private static void removeManifestRecord(GameTestHelper helper, EchoItemId id) {
        try {
            RegisteredItemManifest manifest = manifestStore().load();
            manifestStore().write(new RegisteredItemManifest(manifest.records().stream()
                    .filter(record -> !record.id().equals(id))
                    .toList()));
        } catch (IOException ex) {
            helper.assertTrue(false, "failed to clean pending manifest record: " + ex.getMessage());
        }
    }

    private static RegisteredItemManifestStore manifestStore() {
        return new RegisteredItemManifestStore(FMLPaths.GAMEDIR.get());
    }

    private static String externalDaggerText() {
        return """
                class TrainingDagger : WeaponResource
                {
                    public override WeaponDefinition Build()
                    {
                        return Weapon(
                            id: "jdh:training_dagger",
                            name: "Training Dagger Hot",
                            texture: "jdh:item/training_dagger",
                            damage: 7,
                            attacksPerSecond: "3.0",
                            durability: 181,
                            rarity: COMMON
                        );
                    }
                }
                """;
    }

    private static String externalNewWeaponText(EchoItemId id) {
        return """
                class PendingWeapon : WeaponResource
                {
                    public override WeaponDefinition Build()
                    {
                        return Weapon(
                            id: "%s",
                            name: "Pending Weapon",
                            texture: "jdh:item/training_dagger",
                            damage: 5,
                            attacksPerSecond: "1.5",
                            durability: 120,
                            rarity: COMMON
                        );
                    }
                }
                """.formatted(id.value());
    }
}
