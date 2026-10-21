package io.github.kxyr0.echomagic.forge.client;

import io.github.kxyr0.echomagic.forge.config.MagicClientConfig;
import io.github.kxyr0.echomagic.forge.client.gesture.MagicGestureCapture;
import io.github.kxyr0.echomagic.forge.client.screen.MagicSettingsEntryPoint;
import io.github.kxyr0.echomagic.forge.client.preview.MagicShortTeleportPreview;
import io.github.kxyr0.echomagic.forge.entity.MagicProjectileEntity;
import io.github.kxyr0.echomagic.forge.entity.MagicProjectileEntities;
import io.github.kxyr0.echomagic.forge.network.MagicClientPacketEvent;
import io.github.kxyr0.echomagic.api.gesture.MagicGestureTemplateTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

public final class MagicClientBootstrap {
    private static final ClientMagicState STATE = new ClientMagicState();
    private static final MagicParticleBudget BUDGET = new MagicParticleBudget();
    private final MagicDebugOverlay overlay = new MagicDebugOverlay();
    private static final MagicGestureCapture GESTURES = new MagicGestureCapture();

    private MagicClientBootstrap() { }

    public static void bootstrap(IEventBus modBus) {
        modBus.addListener(MagicKeyMappings::register);
        modBus.addListener(MagicClientBootstrap::registerRenderers);
        MagicSettingsEntryPoint.register();
        MagicClientBootstrap runtime = new MagicClientBootstrap();
        MinecraftForge.EVENT_BUS.addListener(runtime::clientTick);
        MinecraftForge.EVENT_BUS.addListener(runtime::renderTick);
        MinecraftForge.EVENT_BUS.addListener(runtime::livingTick);
        MinecraftForge.EVENT_BUS.addListener(runtime::mouseButton);
        MinecraftForge.EVENT_BUS.addListener(runtime::interaction);
        MinecraftForge.EVENT_BUS.addListener(runtime::renderOverlay);
        MinecraftForge.EVENT_BUS.addListener(runtime::cameraAngles);
        MinecraftForge.EVENT_BUS.addListener(runtime::impact);
        MinecraftForge.EVENT_BUS.addListener(runtime::rejected);
        MinecraftForge.EVENT_BUS.addListener(runtime::playerStateSnapshot);
        MinecraftForge.EVENT_BUS.addListener(runtime::schoolSelected);
        MinecraftForge.EVENT_BUS.addListener(runtime::trainingStatus);
        MinecraftForge.EVENT_BUS.addListener(runtime::templates);
    }

    public static ClientMagicState state() { return STATE; }
    public static MagicParticleBudget budget() { return BUDGET; }
    public static void beginTraining(MagicGestureTemplateTarget target) { GESTURES.beginTraining(target); }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MagicProjectileEntities.PROJECTILE.get(), MagicProjectileRenderer::new);
    }

    private void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        GESTURES.suppressAttackMapping();
        GESTURES.restorePlayerLook();
        if (!minecraft.isPaused()) STATE.tick();
        MagicShortTeleportPreview.tick(minecraft, GESTURES.shortTeleportPreview());
        if (minecraft.level != null) {
            for (Entity entity : minecraft.level.entitiesForRendering()) {
                if (entity instanceof MagicProjectileEntity projectile) MagicClientTrail.tick(projectile);
            }
        }
    }

    private void renderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            BUDGET.beginFrame();
            GESTURES.renderFrame(event.renderTickTime);
        } else {
            GESTURES.restorePlayerLook();
        }
    }

    private void livingTick(LivingEvent.LivingTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!event.getEntity().level().isClientSide || event.getEntity() instanceof Player
                || !minecraft.hasSingleplayerServer() || !GESTURES.active()
                || !MagicClientConfig.SLOW_TIME_WHILE_DRAWING.get()) return;
        if (event.getEntity().level().getGameTime() % 5L != 0L) event.setCanceled(true);
    }

    private void renderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!VanillaGuiOverlay.HOTBAR.id().equals(event.getOverlay().id())) return;
        overlay.render(event); GESTURES.renderHud(event);
    }

    private void cameraAngles(ViewportEvent.ComputeCameraAngles event) {
        GESTURES.lockCamera(event);
    }

    private void mouseButton(InputEvent.MouseButton.Pre event) {
        if (GESTURES.active() && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) event.setCanceled(true);
    }

    private void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        if (GESTURES.active() && event.isAttack()) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    private void impact(MagicClientPacketEvent.Impact event) {
        Minecraft minecraft = Minecraft.getInstance();
        var packet = event.packet();
        if (minecraft.level != null) MagicClientTrail.impact(minecraft.level, packet.style(),
                new Vec3(packet.x(), packet.y(), packet.z()), packet.red(), packet.green(), packet.blue(),
                packet.vanillaRed(), packet.vanillaGreen(), packet.vanillaBlue());
    }

    private void rejected(MagicClientPacketEvent.Rejected event) {
        STATE.rejected(event.packet().reason());
        Minecraft minecraft = Minecraft.getInstance();
        if (MagicClientConfig.SHOW_REJECTION.get() && minecraft.player != null) {
            Component message = switch (event.packet().reason()) {
                case "GESTURE_UNRECOGNIZED" -> Component.translatable(
                        "message.echo_magic.gesture.server_unrecognized");
                case "GESTURE_AMBIGUOUS" -> Component.translatable("message.echo_magic.gesture.ambiguous");
                case "GESTURE_SCHOOL_UNAVAILABLE" -> Component.translatable(
                        "message.echo_magic.gesture.school_unavailable");
                case "GESTURE_SCHOOL_REQUIRED" -> Component.translatable(
                        "message.echo_magic.gesture.school_required");
                case "GESTURE_TECHNIQUE_UNRECOGNIZED" -> Component.translatable(
                        "message.echo_magic.gesture.technique_unrecognized");
                case "GESTURE_TECHNIQUE_UNAVAILABLE" -> Component.translatable(
                        "message.echo_magic.gesture.technique_unavailable");
                case "GESTURE_HOOK_REQUIRES_POINT" -> Component.translatable(
                        "message.echo_magic.gesture.hook_requires_point");
                case "GESTURE_TARGET_REQUIRED" -> Component.translatable(
                        "message.echo_magic.gesture.target_required");
                case "ON_COOLDOWN" -> Component.translatable("message.echo_magic.cast.on_cooldown");
                default -> Component.literal(event.packet().reason());
            };
            minecraft.player.displayClientMessage(message, true);
        }
    }

    private void playerStateSnapshot(MagicClientPacketEvent.State event) {
        var packet = event.packet();
        STATE.update(packet.known(), packet.selected(), packet.gestureSchool(), packet.cooldownRemaining(),
                packet.revision());
    }

    private void schoolSelected(MagicClientPacketEvent.SchoolSelected event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) minecraft.player.displayClientMessage(Component.translatable(
                "message.echo_magic.gesture.school_selected",
                Component.translatable("gesture.echo_magic." + event.packet().school())), true);
    }

    private void trainingStatus(MagicClientPacketEvent.TrainingStatus event) {
        GESTURES.trainingStatus(event.packet());
    }

    private void templates(MagicClientPacketEvent.Templates event) {
        GESTURES.updateTemplates(event.packet().templates());
    }
}
