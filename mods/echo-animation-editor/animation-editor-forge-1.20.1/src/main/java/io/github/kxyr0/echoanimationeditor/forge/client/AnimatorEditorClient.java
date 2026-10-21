package io.github.kxyr0.echoanimationeditor.forge.client;

import io.github.kxyr0.echoanimationeditor.animationadapter.EchoAnimationPublicApiProbe;
import io.github.kxyr0.echoanimationeditor.api.diagnostic.AnimatorDiagnostic;
import io.github.kxyr0.echoanimationeditor.api.diagnostic.AnimatorPublicApiStatus;
import io.github.kxyr0.echoanimationeditor.api.mode.AnimatorEditorMode;
import io.github.kxyr0.echoanimationeditor.core.diagnostic.AnimatorDiagnosticLedger;
import io.github.kxyr0.echoanimationeditor.forge.client.camera.AnimatorCameraStateSnapshot;
import io.github.kxyr0.echoanimationeditor.forge.client.screen.AnimatorEditorScreen;
import io.github.kxyr0.echoanimationeditor.npcadapter.EchoNpcPublicApiProbe;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;

public final class AnimatorEditorClient {
    private final AnimatorDiagnosticLedger diagnostics = new AnimatorDiagnosticLedger(100);
    private final EchoAnimationPublicApiProbe animationProbe = new EchoAnimationPublicApiProbe();
    private final EchoNpcPublicApiProbe npcProbe = new EchoNpcPublicApiProbe();
    private AnimatorForgeSession session;

    public boolean active() {
        return session != null;
    }

    public int open(Optional<String> subjectId, Optional<String> clipId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (session != null) {
            diagnostics.warn("ANIMATOR_EDITOR_SESSION_ALREADY_ACTIVE", "Editor session already active.");
            return 0;
        }
        minecraft.execute(() -> start(minecraft, subjectId, clipId));
        return 1;
    }

    public int close() {
        if (session == null) {
            diagnostics.warn("ANIMATOR_EDITOR_SESSION_NOT_ACTIVE", "Editor session is not active.");
            return 0;
        }
        Minecraft.getInstance().execute(() -> forceClose("command close"));
        return 1;
    }

    public int toggle() {
        return active() ? close() : open(Optional.empty(), Optional.empty());
    }

    public String save() {
        if (session == null) {
            diagnostics.warn("ANIMATOR_EDITOR_SESSION_NOT_ACTIVE", "Editor session is not active.");
            return "Editor session is not active.";
        }
        return session.save();
    }

    public String hotReload() {
        return session == null ? inactive() : session.hotReload();
    }

    public String undo() {
        return session == null ? inactive() : session.undo();
    }

    public String redo() {
        return session == null ? inactive() : session.redo();
    }

    public String autosave() {
        return session == null ? inactive() : session.autosave();
    }

    public String recover() {
        return session == null ? inactive() : session.recover();
    }

    public String preview() {
        return session == null ? inactive() : session.applyPreview();
    }

    public String clearPreview() {
        return session == null ? inactive() : session.clearPreview();
    }

    public String selectBone(String boneId) {
        return session == null ? inactive() : session.selectBone(boneId);
    }

    public String selectFaceElement(String faceElementId) {
        return session == null ? inactive() : session.selectFaceElement(faceElementId);
    }

    public String selectMode(AnimatorEditorMode mode) {
        if (session == null) {
            return inactive();
        }
        session.selectMode(mode);
        return "Selected mode: " + mode;
    }

    public void tick() {
        if (session != null) {
            session.tick();
        }
    }

    public List<String> statusLines() {
        List<String> lines = new ArrayList<>();
        lines.add("session active=" + active());
        if (session != null) {
            lines.addAll(session.statusLines());
        }
        diagnostics.snapshot().stream().map(AnimatorDiagnostic::toString).forEach(lines::add);
        return lines;
    }

    public List<String> boneSuggestions() {
        return session == null ? List.of() : session.discovery().bones();
    }

    public List<String> faceElementSuggestions() {
        return session == null ? List.of() : session.discovery().faceElements();
    }

    public AnimatorForgeSession session() {
        return session;
    }

    public void forceClose(String reason) {
        if (session == null) {
            return;
        }
        session.restore();
        session = null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof AnimatorEditorScreen) {
            minecraft.setScreen(null);
        }
        diagnostics.info("ANIMATOR_EDITOR_CLOSED", "Editor closed: " + reason);
    }

    private void start(Minecraft minecraft, Optional<String> subjectId, Optional<String> clipId) {
        if (minecraft.level == null || minecraft.player == null) {
            diagnostics.error("ANIMATOR_EDITOR_CAMERA_ATTACH_FAILED", "Client world or player is not ready.");
            return;
        }
        AnimatorAdapterStatuses statuses = new AnimatorAdapterStatuses(animationProbe.probeClient(),
                npcProbe.probe(minecraft.getSingleplayerServer()));
        AnimatorCameraStateSnapshot snapshot = AnimatorCameraStateSnapshot.capture(minecraft);
        session = AnimatorForgeSession.start(minecraft, snapshot, subjectId, clipId, statuses);
        minecraft.setScreen(new AnimatorEditorScreen(this, session));
        diagnostics.info("ANIMATOR_EDITOR_OPENED", "Editor session opened.");
    }

    private String inactive() {
        diagnostics.warn("ANIMATOR_EDITOR_SESSION_NOT_ACTIVE", "Editor session is not active.");
        return "Editor session is not active.";
    }
}
