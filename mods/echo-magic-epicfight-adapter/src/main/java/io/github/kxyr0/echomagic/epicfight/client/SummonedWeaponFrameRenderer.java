package io.github.kxyr0.echomagic.epicfight.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kxyr0.echomagic.epicfight.EchoMagicEpicFightMod;
import io.github.kxyr0.echomagic.forge.api.summoned.SummonedWeaponData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

@Mod.EventBusSubscriber(modid = EchoMagicEpicFightMod.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SummonedWeaponFrameRenderer {
    private SummonedWeaponFrameRenderer() { }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        // Own buffer avoids keeping a consumer after another render type flushes its builder.
        MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.BufferBuilder(4096));
        PoseStack view = RenderSystem.getModelViewStack();
        view.pushPose(); view.setIdentity(); RenderSystem.applyModelViewMatrix();
        try {
            for (Player player : minecraft.level.players()) renderPlayer(event, buffers, player);
            buffers.endBatch();
        } finally {
            view.popPose(); RenderSystem.applyModelViewMatrix();
        }
    }

    private static void renderPlayer(RenderLevelStageEvent event, MultiBufferSource.BufferSource buffers,
                                     Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!SummonedWeaponData.isSummoned(stack)) return;
        PlayerPatch<?> patch = EpicFightCapabilities.getPlayerPatch(player);
        OpenMatrix4f main = EpicFightHandAnchor.resolve(patch, InteractionHand.MAIN_HAND, event.getPartialTick());
        if (main == null) return;
        OpenMatrix4f off = EpicFightHandAnchor.resolve(patch, InteractionHand.OFF_HAND, event.getPartialTick());
        SummonedWeaponGeometry.Shape shape = SummonedWeaponGeometry.weapon(player, main, off, stack);
        int color = SummonedWeaponData.color(stack);
        double time = player.level().getGameTime() + event.getPartialTick();
        SummonedWeaponParticleRenderer.render(event, buffers, shape, SummonedWeaponData.form(stack), color, time);
    }
}
