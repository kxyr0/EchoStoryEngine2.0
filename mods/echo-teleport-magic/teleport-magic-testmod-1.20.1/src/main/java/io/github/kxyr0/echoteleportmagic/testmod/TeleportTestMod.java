package io.github.kxyr0.echoteleportmagic.testmod;

import io.github.kxyr0.echoteleportmagic.api.TeleportDestination;
import io.github.kxyr0.echoteleportmagic.api.TeleportExecutionId;
import io.github.kxyr0.echoteleportmagic.forge.EchoTeleportMagic;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("echo_teleport_magic_test")
public final class TeleportTestMod {
    public TeleportTestMod() { MinecraftForge.EVENT_BUS.register(this); }
    @SubscribeEvent public void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("etm_fixture").requires(s -> s.hasPermission(2)).executes(c -> {
            var source = c.getSource();
            var subject = new ArmorStand(EntityType.ARMOR_STAND, source.getLevel());
            var position = source.getPosition().add(0, 0, 4);
            subject.moveTo(position.x, position.y, position.z, 180, 0);
            subject.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            source.getLevel().addFreshEntity(subject);
            EchoTeleportMagic.service().start(subject, new TeleportDestination("debug:fixture", source.getLevel().dimension()
                    .location().toString(), position.x, position.y, position.z, 180, 0),
                    TeleportExecutionId.owned("fixture", "preview-173"), "testmod", "fixture", true, ignored -> { });
            return 1;
        }));
    }
}
