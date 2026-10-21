package io.github.kxyr0.echomagic.forge.client.screen;

import io.github.kxyr0.echomagic.forge.config.MagicClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.network.chat.Component;

public final class MagicSettingsScreen extends Screen {
    private static final double[] ECHO_SCALES = {0.50D, 0.60D, 0.70D, 0.80D, 1.00D, 1.25D};
    private static final double[] VANILLA_SCALES = {1.00D, 1.50D, 2.00D, 2.50D, 3.00D, 4.00D, 5.00D};
    private final Screen parent;
    private Button slowdown;
    private Button particles;
    private Button echoScale;
    private Button vanillaScale;

    public MagicSettingsScreen(Screen parent) {
        super(Component.translatable("screen.echo_magic.settings.title"));
        this.parent = parent;
    }

    @Override protected void init() {
        slowdown = addRenderableWidget(Button.builder(slowdownLabel(), button -> toggleSlowdown())
                .bounds(width / 2 - 100, height / 2 - 42, 200, 20).build());
        particles = addRenderableWidget(Button.builder(particlesLabel(), button -> toggleParticles())
                .bounds(width / 2 - 100, height / 2 - 18, 200, 20).build());
        echoScale = addRenderableWidget(Button.builder(echoScaleLabel(), button -> cycleEchoScale())
                .bounds(width / 2 - 100, height / 2 + 6, 200, 20).build());
        vanillaScale = addRenderableWidget(Button.builder(vanillaScaleLabel(), button -> cycleVanillaScale())
                .bounds(width / 2 - 100, height / 2 + 30, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.echo_magic.settings.handwriting"),
                button -> minecraft.setScreen(new MagicHandwritingScreen(this)))
                .bounds(width / 2 - 100, height / 2 + 54, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.echo_magic.settings.controls"),
                button -> minecraft.setScreen(new ControlsScreen(this, minecraft.options)))
                .bounds(width / 2 - 100, height / 2 + 78, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20).build());
    }

    private void toggleSlowdown() {
        MagicClientConfig.SLOW_TIME_WHILE_DRAWING.set(!MagicClientConfig.SLOW_TIME_WHILE_DRAWING.get());
        slowdown.setMessage(slowdownLabel());
    }

    private Component slowdownLabel() {
        String state = MagicClientConfig.SLOW_TIME_WHILE_DRAWING.get() ? "options.on" : "options.off";
        return Component.translatable("screen.echo_magic.settings.slow_time", Component.translatable(state));
    }

    private void toggleParticles() {
        boolean vanilla = !MagicClientConfig.USE_VANILLA_COLORED_PARTICLES.get();
        MagicClientConfig.USE_VANILLA_COLORED_PARTICLES.set(vanilla);
        particles.setMessage(particlesLabel());
    }

    private Component particlesLabel() {
        String mode = MagicClientConfig.USE_VANILLA_COLORED_PARTICLES.get()
                ? "screen.echo_magic.settings.particles.vanilla"
                : "screen.echo_magic.settings.particles.spell";
        return Component.translatable("screen.echo_magic.settings.particles", Component.translatable(mode));
    }

    private void cycleEchoScale() {
        MagicClientConfig.ECHO_PARTICLE_SCALE.set(next(MagicClientConfig.ECHO_PARTICLE_SCALE.get(), ECHO_SCALES));
        echoScale.setMessage(echoScaleLabel());
    }

    private void cycleVanillaScale() {
        MagicClientConfig.VANILLA_PARTICLE_SCALE.set(next(
                MagicClientConfig.VANILLA_PARTICLE_SCALE.get(), VANILLA_SCALES));
        vanillaScale.setMessage(vanillaScaleLabel());
    }

    private Component echoScaleLabel() {
        return scaleLabel("screen.echo_magic.settings.echo_scale", MagicClientConfig.ECHO_PARTICLE_SCALE.get());
    }

    private Component vanillaScaleLabel() {
        return scaleLabel("screen.echo_magic.settings.vanilla_scale", MagicClientConfig.VANILLA_PARTICLE_SCALE.get());
    }

    private Component scaleLabel(String key, double scale) {
        return Component.translatable(key, Math.round(scale * 100.0D));
    }

    private double next(double current, double[] values) {
        for (double value : values) {
            if (value > current + 0.001D) return value;
        }
        return values[0];
    }

    @Override public void onClose() { minecraft.setScreen(parent); }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 24, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.echo_magic.settings.hint"),
                width / 2, height / 2 - 57, 0xA9A9A9);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
