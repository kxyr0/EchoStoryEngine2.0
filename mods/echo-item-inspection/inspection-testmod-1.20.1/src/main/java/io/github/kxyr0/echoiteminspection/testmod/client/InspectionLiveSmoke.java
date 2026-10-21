package io.github.kxyr0.echoiteminspection.testmod.client;

import io.github.kxyr0.echoiteminspection.client.screen.ItemInspectionScreen;
import io.github.kxyr0.echoiteminspection.testmod.EchoItemInspectionTestMod;
import io.github.kxyr0.echoiteminspection.testmod.client.text.SmokeTextChecks;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in integration smoke against a separate development client and its integrated server. */
@Mod.EventBusSubscriber(modid = EchoItemInspectionTestMod.MOD_ID, value = Dist.CLIENT)
public final class InspectionLiveSmoke {
    private static final boolean ENABLED = Boolean.getBoolean("echo_item_inspection.smoke");
    private static final SmokeEvidence EVIDENCE = new SmokeEvidence();
    private static final SmokeClientFixture FIXTURE = new SmokeClientFixture();
    private static final SmokeInventoryChecks INVENTORY = new SmokeInventoryChecks(EVIDENCE);
    private static final SmokeContainerChecks CONTAINER = new SmokeContainerChecks(EVIDENCE);
    private static final SmokeReloadChecks RELOAD = new SmokeReloadChecks(EVIDENCE);
    private static final SmokeTextChecks TEXT = new SmokeTextChecks(EVIDENCE);
    private static SmokePresentationChecks presentation;
    private static int stage;
    private static int ticks;
    private static int nextTick;
    private static boolean done;
    private static int ignoredExternalInputCount;
    static boolean syntheticInput;

    private InspectionLiveSmoke() { }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void isolateExternalInput(ScreenEvent event) {
        if (!ENABLED || done || stage == 0 || syntheticInput) return;
        if (event instanceof ScreenEvent.MouseButtonPressed.Pre
                || event instanceof ScreenEvent.MouseButtonReleased.Pre
                || event instanceof ScreenEvent.MouseDragged.Pre
                || event instanceof ScreenEvent.MouseScrolled.Pre
                || event instanceof ScreenEvent.KeyPressed.Pre
                || event instanceof ScreenEvent.KeyReleased.Pre) {
            ignoredExternalInputCount++;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || done || event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.getSingleplayerServer() == null) return;
        ticks++;
        try {
            if (ticks > 2400) throw new AssertionError("Live smoke timeout in stage " + stage);
            if (ticks < nextTick) return;
            step(minecraft);
        } catch (Throwable failure) {
            finish(minecraft, failure);
        }
    }

    private static void step(Minecraft minecraft) throws java.io.IOException {
        switch (stage) {
            case 0 -> {
                EVIDENCE.begin();
                minecraft.options.pauseOnLostFocus = false;
                minecraft.setScreen(null);
                EVIDENCE.record("framebuffer", minecraft.getWindow().getWidth() + "x" + minecraft.getWindow().getHeight());
                FIXTURE.prepare(minecraft);
                advance(1);
            }
            case 1 -> {
                if (!FIXTURE.ready(minecraft)) return;
                INVENTORY.open(minecraft);
                presentation = new SmokePresentationChecks(EVIDENCE);
                advance(2);
            }
            case 2 -> {
                EVIDENCE.capture("generated");
                presentation.controls((ItemInspectionScreen) minecraft.screen);
                advance(3);
            }
            case 3 -> {
                EVIDENCE.capture("generated-rotated");
                INVENTORY.returnAndSplit(minecraft);
                advance(4);
            }
            case 4 -> {
                if (presentation.tick(minecraft)) {
                    advance(10);
                } else nextTick = ticks + 10;
            }
            case 10 -> {
                if (RELOAD.tick(minecraft)) {
                    advance(11);
                } else nextTick = ticks + 10;
            }
            case 11 -> {
                if (TEXT.tick(minecraft)) {
                    CONTAINER.requestChest(minecraft);
                    advance(5);
                } else nextTick = ticks + 10;
            }
            case 5 -> {
                if (!CONTAINER.chestReady(minecraft)) return;
                CONTAINER.inspect(minecraft);
                advance(6);
            }
            case 6 -> {
                EVIDENCE.capture("chest-inspection");
                CONTAINER.returnToChest(minecraft);
                advance(7);
            }
            case 7 -> {
                if (!CONTAINER.serverVerified()) return;
                CONTAINER.invalidateOnServer(minecraft);
                advance(8);
            }
            case 8 -> {
                if (!CONTAINER.invalidationArrived(minecraft)) return;
                CONTAINER.checkStaleReturn(minecraft);
                advance(9);
            }
            case 9 -> {
                if (EVIDENCE.capturesReady()) finish(minecraft, null);
            }
            default -> throw new AssertionError("Unknown live smoke stage " + stage);
        }
    }

    private static void advance(int target) {
        stage = target;
        nextTick = ticks + 10;
        EVIDENCE.record("stage", target);
    }

    private static void finish(Minecraft minecraft, Throwable failure) {
        done = true;
        EVIDENCE.record("ignoredExternalInputCount", ignoredExternalInputCount);
        try {
            RELOAD.cleanup();
        } catch (java.io.IOException exception) {
            if (failure == null) failure = exception;
            else failure.addSuppressed(exception);
        }
        try {
            TEXT.cleanup();
        } catch (java.io.IOException exception) {
            if (failure == null) failure = exception;
            else failure.addSuppressed(exception);
        }
        EVIDENCE.finish(failure);
        if (!"false".equals(System.getProperty("echo_item_inspection.smoke.exit"))) minecraft.stop();
    }
}
