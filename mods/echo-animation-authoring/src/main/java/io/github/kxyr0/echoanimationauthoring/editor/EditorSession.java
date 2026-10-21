package io.github.kxyr0.echoanimationauthoring.editor;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable editor session state.
 */
public record EditorSession(
        Optional<Path> workspace,
        String selectedTool,
        boolean compareDefault,
        int timelineFrame,
        boolean hasGlasses
) {
    public static final int MAX_TIMELINE_FRAME = 120;

    public EditorSession(Optional<Path> workspace, String selectedTool, boolean compareDefault, int timelineFrame) {
        this(workspace, selectedTool, compareDefault, timelineFrame, false);
    }

    public EditorSession {
        workspace = Objects.requireNonNull(workspace, "workspace");
        selectedTool = Objects.requireNonNull(selectedTool, "selectedTool");
        timelineFrame = Math.max(0, Math.min(MAX_TIMELINE_FRAME, timelineFrame));
    }

    public static EditorSession unconfigured() {
        return new EditorSession(Optional.empty(), AuthoringLabels.TRANSLATE, false, 0);
    }

    public boolean writeEnabled() {
        return workspace.isPresent();
    }

    public EditorSession selectTool(String tool) {
        return new EditorSession(workspace, tool, compareDefault, timelineFrame, hasGlasses);
    }

    public EditorSession toggleCompareDefault() {
        return new EditorSession(workspace, selectedTool, !compareDefault, timelineFrame, hasGlasses);
    }

    public EditorSession scrubTimeline(int frameDelta) {
        return new EditorSession(workspace, selectedTool, compareDefault, timelineFrame + frameDelta, hasGlasses);
    }

    public EditorSession withGlassesConfiguration(boolean enabled) {
        return new EditorSession(workspace, selectedTool, compareDefault, timelineFrame, enabled);
    }

    public String workspaceLabel() {
        return workspace.map(Path::toString).orElse(AuthoringLabels.WORKSPACE_NOT_CONFIGURED);
    }

    public String writeStateLabel() {
        return writeEnabled() ? AuthoringLabels.WRITES_ENABLED : AuthoringLabels.WRITES_BLOCKED;
    }
}
