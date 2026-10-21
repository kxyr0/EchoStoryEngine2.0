package io.github.kxyr0.echodialoguenpcadapter.productionclientacceptance;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodialogue.forge.client.visual.ClientSpeakerVisual;
import io.github.kxyr0.echodialogue.forge.client.visual.ClientSpeakerVisualRenderer;
import io.github.kxyr0.echodialogue.forge.client.visual.ClientSpeakerVisualResolver;
import io.github.kxyr0.echodialogue.forge.network.message.DialogueOpenMessage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(N4ProductionClientVisualAcceptanceMod.MOD_ID)
public final class N4ProductionClientVisualAcceptanceMod {
    public static final String MOD_ID = "echo_dialogue_npc_adapter_production_client_acceptance";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean AUTO = Boolean.getBoolean("echoDialogueNpcAdapterProductionClientAcceptance.auto");
    private boolean started;

    public N4ProductionClientVisualAcceptanceMod() {
        if (AUTO) {
            MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
        }
    }

    private void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || started) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.font == null || minecraft.getResourceManager() == null) {
            return;
        }
        started = true;
        LOGGER.info("EDNA-PROD-CLIENT-ACCEPTANCE START visual=jdh:john_father display=\"Отец\"");
        minecraft.setScreen(new VisualScreen(minecraft));
    }

    private static final class VisualScreen extends Screen {
        private static final String DISPLAY = "Отец";
        private static final String VISUAL_ID = "jdh:john_father";
        private static final int CAPTURE_FRAME = 12;
        private static final int NEXT_PHASE_FRAME = 24;
        private static final int TIMEOUT_TICKS = 300;
        private final Minecraft minecraft;
        private final ClientSpeakerVisualResolver resolver = new ClientSpeakerVisualResolver();
        private Phase phase = Phase.LINE;
        private int phaseFrames;
        private int ticks;
        private File lineScreenshot;
        private File choiceScreenshot;
        private boolean finished;

        VisualScreen(Minecraft minecraft) {
            super(Component.literal("N4 production client acceptance"));
            this.minecraft = minecraft;
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
            Optional<ClientSpeakerVisual> visual = resolver.resolve(Optional.of(
                    new DialogueOpenMessage.VisualPayload("SKIN_HEAD", VISUAL_ID)));
            if (visual.isEmpty()) {
                fail("skin head resource was not resolved");
                return;
            }
            drawPanel(graphics, visual.orElseThrow());
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

        private void drawPanel(GuiGraphics graphics, ClientSpeakerVisual visual) {
            int panelWidth = Math.min(560, Math.max(260, width - 32));
            int panelHeight = phase == Phase.LINE ? 96 : 150;
            int left = (width - panelWidth) / 2;
            int top = (height - panelHeight) / 2;
            int headSize = 48;
            graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xDD101010);
            ClientSpeakerVisualRenderer.render(graphics, visual, left + 18, top + 24, headSize);
            Font font = minecraft.font;
            int textLeft = left + 78;
            int y = top + 24;
            graphics.drawString(font, DISPLAY, textLeft, y, 0xFFFFAA00);
            if (phase == Phase.LINE) {
                graphics.drawString(font, "Production final-JAR client видит skin head.", textLeft + 46, y,
                        0xFFFFFFFF);
                graphics.drawString(font, "textures/echo_npc/skins/john_father.png", textLeft, y + 20, 0xFFE0E0E0);
            } else {
                graphics.drawString(font, "CHOICE visual в production client.", textLeft + 46, y, 0xFFFFFFFF);
                choice(graphics, font, textLeft, y + 34, "1. Принять");
                choice(graphics, font, textLeft, y + 62, "2. Отказаться");
            }
        }

        private void choice(GuiGraphics graphics, Font font, int x, int y, String text) {
            int width = font.width(text) + 18;
            graphics.fill(x, y - 6, x + width, y + 14, 0x88303030);
            graphics.renderOutline(x, y - 6, width, 20, 0xFF707070);
            graphics.drawString(font, text, x + 8, y, 0xFFFFFFFF);
        }

        private void captureWhenReady() {
            if (phaseFrames != CAPTURE_FRAME || finished) {
                return;
            }
            if (phase == Phase.LINE) {
                lineScreenshot = capture("n4-production-client-line.png", "LINE");
            } else {
                choiceScreenshot = capture("n4-production-client-choice.png", "CHOICE");
            }
        }

        private void advanceWhenReady() {
            if (finished || phaseFrames < NEXT_PHASE_FRAME) {
                return;
            }
            if (phase == Phase.LINE) {
                phase = Phase.CHOICE;
                phaseFrames = 0;
                return;
            }
            if (lineScreenshot != null && choiceScreenshot != null) {
                pass();
            }
        }

        private File capture(String name, String label) {
            File output = new File(minecraft.gameDirectory, "n4-production-client-acceptance");
            if (!output.isDirectory() && !output.mkdirs()) {
                fail("cannot create output dir: " + output.getAbsolutePath());
                return null;
            }
            File target = new File(output, name);
            try (NativeImage image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
                image.writeToFile(target);
                LOGGER.info("EDNA-PROD-CLIENT-ACCEPTANCE SCREENSHOT phase={} path={} size={}x{}",
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
            LOGGER.info("EDNA-PROD-CLIENT-ACCEPTANCE PASS line={} choice={} display=\"{}\" visual={} resource={}",
                    lineScreenshot.getAbsolutePath(), choiceScreenshot.getAbsolutePath(), DISPLAY, VISUAL_ID,
                    "assets/jdh/textures/echo_npc/skins/john_father.png");
            minecraft.stop();
        }

        private void fail(String reason) {
            finished = true;
            LOGGER.error("EDNA-PROD-CLIENT-ACCEPTANCE FAIL reason={}", reason);
            minecraft.stop();
        }

        private enum Phase {
            LINE,
            CHOICE
        }
    }
}
