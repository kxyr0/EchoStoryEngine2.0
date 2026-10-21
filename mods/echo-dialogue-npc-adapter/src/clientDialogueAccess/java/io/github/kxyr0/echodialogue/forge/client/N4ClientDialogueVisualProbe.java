package io.github.kxyr0.echodialogue.forge.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import io.github.kxyr0.echodialogue.forge.network.message.DialogueOpenMessage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

/**
 * Test-only dev-runtime bridge that exercises the package-private Dialogue client renderer.
 */
public final class N4ClientDialogueVisualProbe {
    private static final Logger LOGGER = LogUtils.getLogger();

    private N4ClientDialogueVisualProbe() {
    }

    public static Screen screen(Minecraft minecraft) {
        return new AcceptanceScreen(minecraft);
    }

    private static final class AcceptanceScreen extends Screen {
        private static final String DISPLAY = "Отец";
        private static final String VISUAL_ID = "jdh:john_father";
        private static final int CAPTURE_FRAME = 12;
        private static final int NEXT_PHASE_FRAME = 24;
        private static final int TIMEOUT_TICKS = 300;
        private final Minecraft minecraft;
        private final ClientDialogueAnimationState animation = new ClientDialogueAnimationState();
        private Phase phase = Phase.LINE;
        private int phaseFrames;
        private int ticks;
        private File lineScreenshot;
        private File choiceScreenshot;
        private boolean finished;

        AcceptanceScreen(Minecraft minecraft) {
            super(Component.literal("N4 client visual acceptance"));
            this.minecraft = minecraft;
        }

        @Override
        protected void init() {
            ClientDialogueRenderer.clearVisualCache();
            open(lineMessage(), Phase.LINE);
        }

        @Override
        public void tick() {
            ticks++;
            if (ticks > TIMEOUT_TICKS && !finished) {
                fail("timeout");
            }
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            ClientDialogueRenderer.render(graphics, animation, width, height, Util.getMillis());
            graphics.flush();
            phaseFrames++;
            captureWhenReady();
            advanceWhenReady();
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return false;
        }

        private void open(DialogueOpenMessage message, Phase nextPhase) {
            phase = nextPhase;
            phaseFrames = 0;
            animation.open(message, Util.getMillis() - 4_000L);
        }

        private void captureWhenReady() {
            if (phaseFrames != CAPTURE_FRAME || finished) {
                return;
            }
            if (phase == Phase.LINE) {
                lineScreenshot = capture("n4-client-line.png", "LINE");
            } else {
                choiceScreenshot = capture("n4-client-choice.png", "CHOICE");
            }
        }

        private void advanceWhenReady() {
            if (finished || phaseFrames < NEXT_PHASE_FRAME) {
                return;
            }
            if (phase == Phase.LINE) {
                open(choiceMessage(), Phase.CHOICE);
                return;
            }
            if (lineScreenshot != null && choiceScreenshot != null) {
                pass();
            }
        }

        private File capture(String name, String label) {
            File output = new File(minecraft.gameDirectory, "n4-client-acceptance");
            if (!output.isDirectory() && !output.mkdirs()) {
                fail("cannot create output dir: " + output.getAbsolutePath());
                return null;
            }
            File target = new File(output, name);
            try (NativeImage image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
                image.writeToFile(target);
                LOGGER.info("EDNA-CLIENT-ACCEPTANCE SCREENSHOT phase={} path={} size={}x{}",
                        label, target.getAbsolutePath(), image.getWidth(), image.getHeight());
                return target;
            } catch (IOException | RuntimeException exception) {
                fail("screenshot " + label + ": " + exception.getClass().getSimpleName()
                        + ":" + exception.getMessage());
                return null;
            }
        }

        private void pass() {
            finished = true;
            LOGGER.info("EDNA-CLIENT-ACCEPTANCE PASS line={} choice={} display=\"{}\" visual={} resource={}",
                    lineScreenshot.getAbsolutePath(), choiceScreenshot.getAbsolutePath(), DISPLAY, VISUAL_ID,
                    "assets/jdh/textures/echo_npc/skins/john_father.png");
            minecraft.stop();
        }

        private void fail(String reason) {
            finished = true;
            LOGGER.error("EDNA-CLIENT-ACCEPTANCE FAIL reason={}", reason);
            minecraft.stop();
        }

        private DialogueOpenMessage lineMessage() {
            return new DialogueOpenMessage(
                    "n4-client-line",
                    1,
                    DialoguePresentationKind.LINE,
                    DISPLAY,
                    visual(),
                    "Я дома. Проверь, что имя и skin head отрисованы клиентом.",
                    List.of(),
                    true,
                    false);
        }

        private DialogueOpenMessage choiceMessage() {
            return new DialogueOpenMessage(
                    "n4-client-choice",
                    2,
                    DialoguePresentationKind.CHOICE,
                    DISPLAY,
                    visual(),
                    "Выбери ответ для проверки CHOICE presentation.",
                    List.of(
                            new DialogueOpenMessage.ChoicePayload("accept", "Принять"),
                            new DialogueOpenMessage.ChoicePayload("refuse", "Отказаться")
                    ),
                    false,
                    true);
        }

        private Optional<DialogueOpenMessage.VisualPayload> visual() {
            return Optional.of(new DialogueOpenMessage.VisualPayload("SKIN_HEAD", VISUAL_ID));
        }

        private enum Phase {
            LINE,
            CHOICE
        }
    }
}
