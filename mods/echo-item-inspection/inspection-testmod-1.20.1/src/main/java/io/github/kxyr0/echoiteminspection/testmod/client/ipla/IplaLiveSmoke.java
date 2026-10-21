package io.github.kxyr0.echoiteminspection.testmod.client.ipla;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.kxyr0.echoconcentration.api.ConcentrationServiceBridge;
import io.github.kxyr0.echoiteminspection.client.input.WorldInspectionInput;
import io.github.kxyr0.echoiteminspection.client.render.WorldInspectionPromptRenderer;
import io.github.kxyr0.echoiteminspection.client.screen.ItemInspectionScreen;
import io.github.kxyr0.echoiteminspection.client.world.WorldInspectionPermissionEvent;
import io.github.kxyr0.echoiteminspection.client.world.WorldInspectionRuntime;
import io.github.kxyr0.echoiteminspection.testmod.EchoItemInspectionTestMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = EchoItemInspectionTestMod.MOD_ID, value = Dist.CLIENT)
public final class IplaLiveSmoke {
    private static final boolean ENABLED = Boolean.getBoolean("echo_item_inspection.iplaSmoke");
    private static final IplaSmokeEvidence EVIDENCE = new IplaSmokeEvidence();
    private static final IplaSmokeFixture FIXTURE = ENABLED ? new IplaSmokeFixture() : null;
    private static final IplaSmokeInputChecks INPUT = ENABLED ? new IplaSmokeInputChecks(EVIDENCE) : null;
    private static final IplaSmokeTargetChecks TARGETS = ENABLED ? new IplaSmokeTargetChecks(EVIDENCE, FIXTURE) : null;
    private static final IplaSmokeLayoutChecks LAYOUT = ENABLED ? new IplaSmokeLayoutChecks(EVIDENCE) : null;
    private static int stage;
    private static int ticks;
    private static int next;
    private static int inventoryOpenings;
    private static boolean exclusive;
    private static boolean done;

    private IplaLiveSmoke() { }

    @SubscribeEvent public static void foreground(WorldInspectionPermissionEvent event) {
        if (ENABLED && exclusive) event.setCanceled(true);
    }

    @SubscribeEvent public static void screen(ScreenEvent.Opening event) {
        if (ENABLED && event.getNewScreen() instanceof InventoryScreen) inventoryOpenings++;
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || done || event.phase != TickEvent.Phase.END) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.getSingleplayerServer() == null) return;
        try {
            if (++ticks > 2400) throw new AssertionError("IPLA smoke timeout stage=" + stage);
            if (stage > 0) FIXTURE.stabilize(minecraft);
            if (ticks < next) return;
            step(minecraft);
        } catch (Throwable failure) { finish(minecraft, failure); }
    }

    private static void step(Minecraft minecraft) {
        switch (stage) {
            case 0 -> {
                EVIDENCE.record("startedAt", java.time.Instant.now().toString());
                minecraft.options.pauseOnLostFocus = false;
                minecraft.options.hideGui = false;
                minecraft.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                minecraft.setScreen(null);
                FIXTURE.prepare(minecraft);
            }
            case 1 -> {
                if (!FIXTURE.ready(minecraft)) {
                    if (ticks > 200) throw new AssertionError(FIXTURE.describe(minecraft));
                    return;
                }
            }
            case 2 -> {
                EVIDENCE.capture("world-prompt-e");
                INPUT.openExact(minecraft, GLFW.GLFW_KEY_E);
            }
            case 3 -> {
                EVIDENCE.check("zeroInventoryScreenOpeningsOnInspectE", inventoryOpenings == 0);
                EVIDENCE.capture("exact-world-model-glint");
                INPUT.close(minecraft);
                FIXTURE.verifyUnchanged(minecraft);
            }
            case 4 -> {
                if (!FIXTURE.verified()) return;
                EVIDENCE.check("serverIdentityStackPositionAndInventoryUnchanged", FIXTURE.unchanged());
                FIXTURE.lookAway();
            }
            case 5 -> IplaSmokeInputChecks.press(GLFW.GLFW_KEY_E);
            case 6 -> {
                EVIDENCE.check("eWithoutTargetOpensVanillaInventory", minecraft.screen instanceof InventoryScreen);
                minecraft.screen.onClose();
                IplaSmokeInputChecks.bind(GLFW.GLFW_KEY_R);
                FIXTURE.lookAt(IplaSmokeFixture.ITEM);
            }
            case 7 -> {
                EVIDENCE.check("reboundPromptShowsActualRBinding", WorldInspectionInput.INSPECT.matches(GLFW.GLFW_KEY_R, 0)
                        && WorldInspectionPromptRenderer.label().getString().contains("["
                                + InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_R).getDisplayName().getString() + "]"));
                EVIDENCE.record("reboundKeyDisplay", WorldInspectionInput.INSPECT.getTranslatedKeyMessage().getString());
                EVIDENCE.capture("world-prompt-r");
                IplaSmokeInputChecks.press(GLFW.GLFW_KEY_E);
            }
            case 8 -> {
                EVIDENCE.check("eAfterRebindOpensInventoryEvenAtTarget", minecraft.screen instanceof InventoryScreen);
                minecraft.screen.onClose();
            }
            case 9 -> INPUT.openExact(minecraft, GLFW.GLFW_KEY_R);
            case 10 -> {
                INPUT.close(minecraft);
                IplaSmokeInputChecks.bind(GLFW.GLFW_KEY_E);
                EVIDENCE.check("realConcentrationEntered", ConcentrationServiceBridge.lookup().orElseThrow().enter());
            }
            case 11 -> {
                WorldInspectionRuntime.refresh();
                EVIDENCE.check("concentrationHidesIplaPrompt", WorldInspectionRuntime.prompt().activeTarget() == null);
                IplaSmokeInputChecks.press(GLFW.GLFW_KEY_E);
            }
            case 12 -> {
                EVIDENCE.check("concentrationRetainsInputPriority", minecraft.screen == null);
                ConcentrationServiceBridge.lookup().orElseThrow().exit();
                exclusive = true;
            }
            case 13 -> {
                WorldInspectionRuntime.refresh();
                EVIDENCE.check("exclusiveForegroundVetoesPromptAndActivation",
                        WorldInspectionRuntime.prompt().activeTarget() == null && !WorldInspectionRuntime.activate());
                exclusive = false;
                var detached = new net.minecraft.world.entity.Marker(net.minecraft.world.entity.EntityType.MARKER, minecraft.level);
                minecraft.setCameraEntity(detached);
                WorldInspectionRuntime.refresh();
                EVIDENCE.check("detachedCutsceneCameraBlocksWorldInspection",
                        WorldInspectionRuntime.prompt().activeTarget() == null && !WorldInspectionRuntime.activate());
                minecraft.setCameraEntity(minecraft.player);
                WorldInspectionInput.INSPECT.setKey(InputConstants.Type.MOUSE.getOrCreate(3));
                KeyMapping.resetMapping();
            }
            case 14 -> {
                WorldInspectionRuntime.refresh();
                EVIDENCE.check("mouseBindingUsesLocalizedDisplay", WorldInspectionPromptRenderer.label().getString()
                        .contains(WorldInspectionInput.INSPECT.getTranslatedKeyMessage().getString()));
                var mouse = new InputEvent.MouseButton.Pre(3, GLFW.GLFW_PRESS, 0);
                MinecraftForge.EVENT_BUS.post(mouse);
                EVIDENCE.check("reboundMouseConsumedAndOpensViewer", mouse.isCanceled() && minecraft.screen instanceof ItemInspectionScreen);
            }
            case 15 -> {
                INPUT.close(minecraft);
                IplaSmokeInputChecks.bind(GLFW.GLFW_KEY_E);
            }
            case 16 -> { if (!TARGETS.tick(minecraft)) { next = ticks + 12; return; } }
            case 17 -> { if (!LAYOUT.tick(minecraft)) { next = ticks + 12; return; } }
            case 18 -> {
                LAYOUT.performance(minecraft);
                finish(minecraft, null);
                return;
            }
            default -> throw new AssertionError("Unknown IPLA smoke stage " + stage);
        }
        stage++;
        next = ticks + 12;
        EVIDENCE.record("stage", stage);
    }

    private static void finish(Minecraft minecraft, Throwable failure) {
        done = true;
        exclusive = false;
        IplaSmokeInputChecks.bind(GLFW.GLFW_KEY_E);
        EVIDENCE.finish(failure);
        minecraft.stop();
    }
}
