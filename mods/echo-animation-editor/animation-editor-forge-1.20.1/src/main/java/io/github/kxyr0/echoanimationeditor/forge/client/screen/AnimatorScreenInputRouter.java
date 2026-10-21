package io.github.kxyr0.echoanimationeditor.forge.client.screen;

import io.github.kxyr0.echoanimationeditor.api.mode.AnimatorEditorMode;
import io.github.kxyr0.echoanimationeditor.api.mode.AnimatorTool;
import io.github.kxyr0.echoanimationeditor.forge.client.AnimatorForgeSession;
import io.github.kxyr0.echoanimationeditor.forge.client.screen.ui.AnimatorUiAction;
import java.util.Optional;
import org.lwjgl.glfw.GLFW;

final class AnimatorScreenInputRouter {
    private AnimatorScreenInputRouter() {
    }

    static boolean controlShortcut(AnimatorForgeSession session, int keyCode, int modifiers) {
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) == 0) {
            return false;
        }
        return switch (keyCode) {
            case GLFW.GLFW_KEY_S -> saveShortcut(session, modifiers);
            case GLFW.GLFW_KEY_Z -> runShortcut(session::undo);
            case GLFW.GLFW_KEY_Y -> runShortcut(session::redo);
            case GLFW.GLFW_KEY_R -> runShortcut(session::recover);
            case GLFW.GLFW_KEY_P -> runShortcut(session::applyPreview);
            case GLFW.GLFW_KEY_X -> runShortcut(session::clearPreview);
            default -> false;
        };
    }

    static boolean handleUiClick(AnimatorScreenRenderer renderer, AnimatorForgeSession session,
                                 double mouseX, double mouseY) {
        Optional<AnimatorUiAction> action = renderer.actionAt(mouseX, mouseY);
        if (action.isPresent()) {
            runAction(session, action.orElseThrow());
            return true;
        }
        Optional<AnimatorEditorMode> mode = renderer.modeAt(mouseX, mouseY);
        if (mode.isPresent()) {
            session.selectMode(mode.orElseThrow());
            return true;
        }
        Optional<AnimatorTool> tool = renderer.toolAt(mouseX, mouseY);
        if (tool.isPresent()) {
            session.selectTool(tool.orElseThrow());
            return true;
        }
        return handleTargetClick(renderer, session, mouseX, mouseY);
    }

    static Optional<AnimatorTool> toolKey(int keyCode, int modifiers) {
        if (!plainKey(modifiers)) {
            return Optional.empty();
        }
        return switch (keyCode) {
            case GLFW.GLFW_KEY_T -> Optional.of(AnimatorTool.TRANSLATE);
            case GLFW.GLFW_KEY_R -> Optional.of(AnimatorTool.ROTATE);
            case GLFW.GLFW_KEY_S -> Optional.of(AnimatorTool.SCALE);
            default -> Optional.empty();
        };
    }

    static Optional<AnimatorEditorMode> modeKey(int keyCode, int modifiers) {
        if (!plainKey(modifiers)) {
            return Optional.empty();
        }
        return switch (keyCode) {
            case GLFW.GLFW_KEY_B -> Optional.of(AnimatorEditorMode.BODY);
            case GLFW.GLFW_KEY_F -> Optional.of(AnimatorEditorMode.FACE);
            default -> Optional.empty();
        };
    }

    private static boolean handleTargetClick(AnimatorScreenRenderer renderer, AnimatorForgeSession session,
                                             double mouseX, double mouseY) {
        Optional<String> bone = renderer.boneAt(mouseX, mouseY);
        if (bone.isPresent()) {
            session.selectBone(bone.orElseThrow());
            session.selectMode(AnimatorEditorMode.BODY);
            return true;
        }
        Optional<String> faceElement = renderer.faceElementAt(mouseX, mouseY);
        if (faceElement.isPresent()) {
            session.selectFaceElement(faceElement.orElseThrow());
            session.selectMode(AnimatorEditorMode.FACE);
            return true;
        }
        return false;
    }

    private static void runAction(AnimatorForgeSession session, AnimatorUiAction action) {
        switch (action) {
            case SAVE -> session.save();
            case AUTOSAVE -> session.autosave();
            case UNDO -> session.undo();
            case REDO -> session.redo();
            case PREVIEW -> session.applyPreview();
            case CLEAR_PREVIEW -> session.clearPreview();
            case HOT_RELOAD -> session.hotReload();
        }
    }

    private static boolean saveShortcut(AnimatorForgeSession session, int modifiers) {
        return (modifiers & GLFW.GLFW_MOD_SHIFT) == 0
                ? runShortcut(session::save)
                : runShortcut(session::autosave);
    }

    private static boolean runShortcut(Runnable shortcut) {
        shortcut.run();
        return true;
    }

    private static boolean plainKey(int modifiers) {
        return (modifiers & (GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SHIFT
                | GLFW.GLFW_MOD_SUPER)) == 0;
    }
}
