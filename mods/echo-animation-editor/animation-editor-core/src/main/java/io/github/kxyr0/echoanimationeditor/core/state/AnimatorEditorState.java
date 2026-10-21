package io.github.kxyr0.echoanimationeditor.core.state;

import io.github.kxyr0.echoanimationeditor.api.mode.AnimatorEditorMode;
import io.github.kxyr0.echoanimationeditor.api.mode.AnimatorSpace;
import io.github.kxyr0.echoanimationeditor.api.mode.AnimatorTool;
import io.github.kxyr0.echoanimationeditor.api.selection.AnimatorSelection;
import io.github.kxyr0.echoanimationeditor.api.session.AnimatorEditorSessionId;
import java.util.Optional;

public record AnimatorEditorState(
        AnimatorEditorSessionId sessionId,
        AnimatorEditorMode mode,
        AnimatorTool tool,
        AnimatorSpace space,
        AnimatorSelection selection,
        int currentFrame,
        boolean playing,
        boolean dirty
) {
    public static AnimatorEditorState open(Optional<String> subjectId, Optional<String> clipId) {
        AnimatorSelection selection = new AnimatorSelection(subjectId, clipId, Optional.empty(), Optional.empty());
        return new AnimatorEditorState(AnimatorEditorSessionId.random(), AnimatorEditorMode.BODY,
                AnimatorTool.TRANSLATE, AnimatorSpace.LOCAL, selection, 0, false, false);
    }

    public AnimatorEditorState mode(AnimatorEditorMode nextMode) {
        return new AnimatorEditorState(sessionId, nextMode, tool, space, selection, currentFrame, playing, dirty);
    }

    public AnimatorEditorState tool(AnimatorTool nextTool) {
        return new AnimatorEditorState(sessionId, mode, nextTool, space, selection, currentFrame, playing, dirty);
    }

    public AnimatorEditorState frame(int frame) {
        return new AnimatorEditorState(sessionId, mode, tool, space, selection,
                Math.max(0, frame), playing, dirty);
    }

    public AnimatorEditorState selectBone(String boneId) {
        return new AnimatorEditorState(sessionId, mode, tool, space, selection.bone(boneId),
                currentFrame, playing, dirty);
    }

    public AnimatorEditorState selectFaceElement(String faceElementId) {
        return new AnimatorEditorState(sessionId, mode, tool, space, selection.faceElement(faceElementId),
                currentFrame, playing, dirty);
    }

    public AnimatorEditorState togglePlayback() {
        return new AnimatorEditorState(sessionId, mode, tool, space, selection, currentFrame, !playing, dirty);
    }

    public AnimatorEditorState markDirty() {
        return new AnimatorEditorState(sessionId, mode, tool, space, selection, currentFrame, playing, true);
    }

    public AnimatorEditorState markSaved() {
        return new AnimatorEditorState(sessionId, mode, tool, space, selection, currentFrame, playing, false);
    }
}
