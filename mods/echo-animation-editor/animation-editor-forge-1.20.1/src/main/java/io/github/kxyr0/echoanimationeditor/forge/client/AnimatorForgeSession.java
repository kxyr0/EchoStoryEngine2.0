package io.github.kxyr0.echoanimationeditor.forge.client;

import io.github.kxyr0.echoanimationeditor.animationadapter.authoring.EchoAuthoringTransform;
import io.github.kxyr0.echoanimationeditor.api.gizmo.AnimatorGizmoHandle;
import io.github.kxyr0.echoanimationeditor.api.gizmo.AnimatorTransformDelta;
import io.github.kxyr0.echoanimationeditor.api.mode.AnimatorEditorMode;
import io.github.kxyr0.echoanimationeditor.api.mode.AnimatorTool;
import io.github.kxyr0.echoanimationeditor.api.viewport.AnimatorPanelRect;
import io.github.kxyr0.echoanimationeditor.core.state.AnimatorDraftState;
import io.github.kxyr0.echoanimationeditor.core.state.AnimatorEditorState;
import io.github.kxyr0.echoanimationeditor.forge.client.camera.AnimatorCameraController;
import io.github.kxyr0.echoanimationeditor.forge.client.camera.AnimatorCameraInput;
import io.github.kxyr0.echoanimationeditor.forge.client.camera.AnimatorCameraStateSnapshot;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;

public final class AnimatorForgeSession {
    private static final String DEFAULT_CLIP = "jdh:animator_editor_clip", DEFAULT_RIG = "jdh:humanoid";
    private static final int AUTOSAVE_INTERVAL_TICKS = 600;
    private final AnimatorCameraStateSnapshot snapshot;
    private final AnimatorCameraInput input = new AnimatorCameraInput();
    private final AnimatorCameraController camera;
    private final AnimatorAdapterStatuses statuses;
    private final AnimatorSessionGizmo gizmo = new AnimatorSessionGizmo();
    private AnimatorAuthoringState authoring; private AnimatorDiscoveryState discovery;
    private AnimatorEditorState state; private AnimatorDraftState draft;
    private AnimatorTransformDelta previewDelta = AnimatorTransformDelta.none();
    private int ticksSinceAutosave; private boolean previewActive;
    private String autosaveMessage = "Autosave has not run.";
    private String lastMessage = "Editor opened.";

    private AnimatorForgeSession(AnimatorCameraStateSnapshot snapshot, AnimatorCameraController camera,
                                 AnimatorEditorState state, AnimatorAdapterStatuses statuses) {
        this.snapshot = snapshot;
        this.camera = camera;
        this.state = state;
        this.draft = AnimatorDraftState.create();
        this.statuses = statuses;
        this.authoring = AnimatorAuthoringState.open(state.selection().clipId());
        this.discovery = authoring.discover(clipId(), DEFAULT_RIG);
        this.state = discoveredSelection(state);
    }

    public static AnimatorForgeSession start(Minecraft minecraft, AnimatorCameraStateSnapshot snapshot,
                                             Optional<String> subjectId, Optional<String> clipId,
                                             AnimatorAdapterStatuses statuses) {
        AnimatorCameraController camera = AnimatorCameraController.detach(minecraft);
        AnimatorEditorState state = AnimatorEditorState.open(subjectId, clipId);
        return new AnimatorForgeSession(snapshot, camera, state, statuses);
    }

    public AnimatorCameraInput input() { return input; }

    public AnimatorEditorState state() { return state; }

    public AnimatorDiscoveryState discovery() { return discovery; }

    public String lastMessage() { return lastMessage; }

    public void markEdited() {
        state = state.markDirty();
        draft = draft.touch();
    }

    public void selectTool(AnimatorTool tool) {
        state = state.tool(tool);
        lastMessage = "Tool: " + tool;
    }

    public void selectMode(AnimatorEditorMode mode) {
        state = state.mode(mode);
        lastMessage = "Mode: " + mode;
    }

    public String selectBone(String boneId) {
        if (!discovery.bones().isEmpty() && !discovery.bones().contains(boneId)) {
            return record("Bone is not in discovered rig: " + boneId);
        }
        state = state.selectBone(boneId);
        return record("Selected bone: " + boneId);
    }

    public String selectFaceElement(String faceElementId) {
        if (!discovery.faceElements().isEmpty() && !discovery.faceElements().contains(faceElementId)) {
            return record("Face element is not discovered: " + faceElementId);
        }
        state = state.selectFaceElement(faceElementId);
        return record("Selected face element: " + faceElementId);
    }

    public boolean beginGizmo(double mouseX, double mouseY, AnimatorPanelRect viewport) {
        return gizmo.begin(state.tool(), viewport, mouseX, mouseY);
    }

    public boolean finishGizmo(double mouseX, double mouseY) {
        Optional<AnimatorTransformDelta> delta = gizmo.finish(mouseX, mouseY);
        delta.ifPresent(this::applyGizmoDelta);
        return delta.isPresent();
    }

    public AnimatorGizmoHandle activeGizmoHandle() { return gizmo.activeHandle(); }

    public AnimatorTransformDelta previewDelta() { return previewDelta; }

    public String undo() { return history(authoring.undo(), "UNDONE"); }

    public String redo() { return history(authoring.redo(), "REDONE"); }

    private String history(AnimatorAuthoringState next, String token) {
        authoring = next;
        if (authoring.message().contains(token)) {
            markEdited();
            applyPreview();
        }
        return record(authoring.message());
    }

    public String autosave() {
        authoring = authoring.autosave();
        autosaveMessage = authoring.message();
        ticksSinceAutosave = 0;
        return record(autosaveMessage);
    }

    public String recover() {
        authoring = authoring.recover(clipId(), DEFAULT_RIG);
        if (authoring.message().contains("RECOVERED")) {
            markEdited();
            discovery = authoring.discover(clipId(), DEFAULT_RIG);
        }
        return record(authoring.message());
    }

    public String applyPreview() {
        authoring = authoring.applyPreview();
        previewActive = authoring.message().contains("PREVIEW_APPLIED");
        return record(authoring.message());
    }

    public String clearPreview() {
        authoring = authoring.clearPreview();
        previewActive = false;
        previewDelta = AnimatorTransformDelta.none();
        return record(authoring.message());
    }

    private void applyGizmoDelta(AnimatorTransformDelta delta) {
        String boneId = state.selection().boneId().orElse("body");
        authoring = authoring.apply(boneId, state.currentFrame(), transform(delta));
        markEdited();
        if (authoring.message().contains("APPLIED")) {
            previewDelta = delta;
            applyPreview();
        }
    }

    public String save() {
        authoring = authoring.save();
        if (authoring.message().contains("SAVED")) {
            state = state.markSaved();
            draft = draft.saved();
            ticksSinceAutosave = 0;
        }
        return record(authoring.message());
    }

    public String hotReload() { authoring = authoring.hotReload(); return record(authoring.message()); }

    public void tick() {
        camera.tick(input);
        if (state.dirty() && ++ticksSinceAutosave >= AUTOSAVE_INTERVAL_TICKS) {
            autosave();
        }
    }

    public void restore() {
        camera.restore(snapshot);
    }

    public List<String> statusLines() {
        return List.of("session id=" + state.sessionId().value(),
                "selected subject=" + state.selection().subjectId().orElse("<none>")
                        + " clip=" + state.selection().clipId().orElse("<none>"),
                "selected bone=" + state.selection().boneId().orElse("<none>") + " face="
                        + state.selection().faceElementId().orElse("<none>"),
                "mode=" + state.mode() + " tool=" + state.tool() + " space=" + state.space(),
                "frame=" + state.currentFrame() + " playing=" + state.playing() + " dirty=" + state.dirty(),
                "preview=" + previewActive + " draft=" + draft.draftId().value() + " revision="
                        + draft.revision().value(),
                "discovery bones=" + discovery.bones().size() + " faceElements=" + discovery.faceElements().size(),
                "api animation=" + statuses.animation().status() + " npc=" + statuses.npc().status(),
                "authoring=" + authoring.message() + " autosave=" + autosaveMessage,
                "discovery=" + discovery.message(), "camera=" + camera.pose());
    }

    private AnimatorEditorState discoveredSelection(AnimatorEditorState opened) {
        AnimatorEditorState next = discovery.firstBone().map(opened::selectBone).orElse(opened);
        return discovery.firstFaceElement().map(next::selectFaceElement).orElse(next);
    }

    private String clipId() { return state.selection().clipId().orElse(DEFAULT_CLIP); }

    private String record(String message) {
        lastMessage = message;
        return message;
    }

    private static EchoAuthoringTransform transform(AnimatorTransformDelta delta) {
        return new EchoAuthoringTransform(delta.translateX(), delta.translateY(), delta.translateZ(),
                delta.rotateX(), delta.rotateY(), delta.rotateZ(), delta.scaleX(), delta.scaleY(), delta.scaleZ());
    }
}
