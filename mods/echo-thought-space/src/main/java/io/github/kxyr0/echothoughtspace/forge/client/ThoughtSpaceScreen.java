package io.github.kxyr0.echothoughtspace.forge.client;

import io.github.kxyr0.echothoughtspace.core.canvas.CanvasPoint;
import io.github.kxyr0.echothoughtspace.core.canvas.CanvasTransform;
import io.github.kxyr0.echothoughtspace.core.canvas.CanvasViewport;
import io.github.kxyr0.echothoughtspace.core.canvas.ThoughtNodeHitTester;
import io.github.kxyr0.echothoughtspace.core.model.ThoughtCanvasNode;
import io.github.kxyr0.echothoughtspace.forge.client.input.CanvasDragGesture;
import io.github.kxyr0.echothoughtspace.forge.client.input.ThoughtConnectionGesture;
import io.github.kxyr0.echothoughtspace.forge.client.render.ThoughtSpaceRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.Optional;

public final class ThoughtSpaceScreen extends Screen {
    private final ThoughtSpaceRuntime runtime;
    private final ThoughtSpaceRenderer renderer = new ThoughtSpaceRenderer();
    private final CanvasDragGesture drag = new CanvasDragGesture();
    private final CanvasDragGesture rightDrag = new CanvasDragGesture();
    private final ThoughtConnectionGesture connection = new ThoughtConnectionGesture();
    private final ThoughtNodeHitTester hitTester = new ThoughtNodeHitTester();
    private CanvasPoint pointer = new CanvasPoint(0.0D, 0.0D);

    public ThoughtSpaceScreen(ThoughtSpaceRuntime runtime) {
        super(Component.translatable("screen.echo_thought_space.title"));
        this.runtime = runtime;
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        pointer = physical(mouseX, mouseY);
        CanvasTransform transform = transform();
        long nowNanos = System.nanoTime();
        ThoughtSpaceRuntime.Frame frame = runtime.frame(nowNanos);
        renderer.render(graphics, transform, pointer, runtime.nodes(), frame.opacity(),
                frame.deltaSeconds(), runtime.style(), runtime.unlockedCombinations(),
                connection.visual(nowNanos), runtime.debug(), runtime.connectionTraces());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ThoughtSpaceKeys.TOGGLE.matches(keyCode, scanCode)) runtime.toggleEdge();
        return true;
    }

    @Override public boolean keyReleased(int keyCode, int scanCode, int modifiers) { return true; }
    @Override public boolean charTyped(char codePoint, int modifiers) { return true; }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!validPointer(mouseX, mouseY)) return true;
        pointer = physical(mouseX, mouseY);
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            connection.reset();
            drag.finish();
            rightDrag.begin(pointer);
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !rightDrag.active()) {
            CanvasTransform currentTransform = transform();
            Optional<ThoughtCanvasNode> hit = hitNode(currentTransform);
            if (hit.isPresent()) {
                ThoughtCanvasNode node = hit.get();
                connection.begin(node.snapshot().id(), currentTransform.toCanvas(pointer), pointer);
            } else drag.begin(pointer);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!validPointer(mouseX, mouseY)) return true;
        pointer = physical(mouseX, mouseY);
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && rightDrag.active()) {
            CanvasTransform transform = transform();
            rightDrag.move(pointer).ifPresent(delta -> runtime.camera().pan(delta, transform));
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && connection.active()) {
            CanvasTransform currentTransform = transform();
            boolean valid = hitNode(currentTransform)
                    .filter(node -> !node.snapshot().id().equals(connection.sourceId()))
                    .filter(node -> runtime.canCombine(connection.sourceId(), node.snapshot().id()))
                    .isPresent();
            connection.move(pointer, currentTransform.toCanvas(pointer), valid);
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && drag.active()) {
            CanvasTransform transform = transform();
            drag.move(pointer).ifPresent(delta -> runtime.camera().pan(delta, transform));
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!validPointer(mouseX, mouseY)) {
            drag.finish(); rightDrag.finish(); connection.reset();
            return true;
        }
        pointer = physical(mouseX, mouseY);
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) rightDrag.finish();
        else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && connection.active()) {
            CanvasTransform currentTransform = transform();
            Optional<ThoughtCanvasNode> target = hitNode(currentTransform)
                    .filter(node -> !node.snapshot().id().equals(connection.sourceId()));
            var source = runtime.nodes().stream().filter(node -> node.snapshot().id().equals(connection.sourceId())).findFirst();
            boolean success = target.isPresent() && source.isPresent() && connection.dragged()
                    && runtime.combine(connection.sourceId(), target.get().snapshot().id(), renderer.acceptedPath(currentTransform,
                            new CanvasPoint(source.get().position().x(), source.get().position().y()),
                            new CanvasPoint(target.get().position().x(), target.get().position().y())));
            connection.move(pointer, currentTransform.toCanvas(pointer), success);
            connection.release(success, System.nanoTime());
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) drag.finish();
        return true;
    }

    @Override public void mouseMoved(double mouseX, double mouseY) { pointer = physical(mouseX, mouseY); }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!validPointer(mouseX, mouseY) || !Double.isFinite(delta)) return true;
        CanvasViewport currentViewport = viewport();
        pointer = new CanvasPoint(currentViewport.guiToPhysicalX(mouseX),
                currentViewport.guiToPhysicalY(mouseY));
        runtime.zoom(pointer, currentViewport, delta);
        return true;
    }

    private CanvasTransform transform() { return CanvasTransform.fit(runtime.camera(), viewport()); }
    private Optional<ThoughtCanvasNode> hitNode(CanvasTransform transform) {
        return hitTester.hit(runtime.nodes(), transform, pointer, runtime.style().circleRadius())
                .flatMap(hit -> runtime.nodes().stream()
                        .filter(node -> node.snapshot().id().equals(hit.nodeId())).findFirst());
    }
    private CanvasPoint physical(double guiX, double guiY) {
        if (!validPointer(guiX, guiY)) return pointer;
        CanvasViewport viewport = viewport();
        double x = viewport.guiToPhysicalX(guiX), y = viewport.guiToPhysicalY(guiY);
        return validPointer(x, y) ? new CanvasPoint(x, y) : pointer;
    }
    private static boolean validPointer(double x, double y) {
        return Double.isFinite(x) && Double.isFinite(y);
    }
    private CanvasViewport viewport() {
        var window = minecraft.getWindow();
        return new CanvasViewport(window.getWidth(), window.getHeight(), width, height);
    }
}
