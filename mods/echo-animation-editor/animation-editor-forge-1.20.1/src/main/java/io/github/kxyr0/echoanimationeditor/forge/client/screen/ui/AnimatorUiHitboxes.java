package io.github.kxyr0.echoanimationeditor.forge.client.screen.ui;

import io.github.kxyr0.echoanimationeditor.api.mode.AnimatorEditorMode;
import io.github.kxyr0.echoanimationeditor.api.mode.AnimatorTool;
import io.github.kxyr0.echoanimationeditor.api.viewport.AnimatorPanelRect;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AnimatorUiHitboxes {
    private final List<UiHitbox> hitboxes = new ArrayList<>();

    public void clear() {
        hitboxes.clear();
    }

    void add(AnimatorPanelRect rect, AnimatorUiHitKind kind, String value) {
        hitboxes.add(new UiHitbox(rect, kind, value));
    }

    public Optional<AnimatorTool> toolAt(double mouseX, double mouseY) {
        return hit(AnimatorUiHitKind.TOOL, mouseX, mouseY).map(AnimatorTool::valueOf);
    }

    public Optional<AnimatorEditorMode> modeAt(double mouseX, double mouseY) {
        return hit(AnimatorUiHitKind.MODE, mouseX, mouseY).map(AnimatorEditorMode::valueOf);
    }

    public Optional<AnimatorUiAction> actionAt(double mouseX, double mouseY) {
        return hit(AnimatorUiHitKind.ACTION, mouseX, mouseY).map(AnimatorUiAction::valueOf);
    }

    public Optional<String> boneAt(double mouseX, double mouseY) {
        return hit(AnimatorUiHitKind.BONE, mouseX, mouseY);
    }

    public Optional<String> faceElementAt(double mouseX, double mouseY) {
        return hit(AnimatorUiHitKind.FACE, mouseX, mouseY);
    }

    private Optional<String> hit(AnimatorUiHitKind kind, double mouseX, double mouseY) {
        int x = (int) Math.round(mouseX);
        int y = (int) Math.round(mouseY);
        return hitboxes.stream()
                .filter(hitbox -> hitbox.kind() == kind && hitbox.rect().contains(x, y))
                .findFirst()
                .map(UiHitbox::value);
    }

    private record UiHitbox(AnimatorPanelRect rect, AnimatorUiHitKind kind, String value) {
    }
}
