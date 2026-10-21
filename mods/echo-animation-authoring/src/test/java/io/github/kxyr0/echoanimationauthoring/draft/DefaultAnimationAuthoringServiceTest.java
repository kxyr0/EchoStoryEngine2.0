package io.github.kxyr0.echoanimationauthoring.draft;

import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringKeyframeMutation;
import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringTransform;
import io.github.kxyr0.echoanimationauthoring.api.service.AuthoringDraftOpenRequest;
import io.github.kxyr0.echoanimationauthoring.api.service.AuthoringDraftOpenResult;
import io.github.kxyr0.echoanimationauthoring.api.service.AuthoringOperationResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class DefaultAnimationAuthoringServiceTest {
    @TempDir
    Path temp;

    @Test
    void draftCreatesCanonicalClipPathInsideWorkspace() {
        var service = new DefaultAnimationAuthoringService(() -> Optional.of(temp));

        AuthoringDraftOpenResult result = service.openDraft(new AuthoringDraftOpenRequest(
                "jdh:editor/wave", "jdh:humanoid"));

        Assertions.assertEquals(AuthoringDraftOpenResult.Status.OPENED, result.status());
        Path path = result.draft().orElseThrow().sourcePath();
        Assertions.assertTrue(path.startsWith(temp));
        Assertions.assertTrue(path.toString().contains("animation_clips"));
        Assertions.assertTrue(path.toString().endsWith("wave.animation-clip.es"));
    }

    @Test
    void mutationUpdatesDraftWithoutSavingSource() {
        var service = new DefaultAnimationAuthoringService(() -> Optional.of(temp));
        var draft = service.openDraft(new AuthoringDraftOpenRequest("jdh:editor/wave", "jdh:humanoid"))
                .draft().orElseThrow();

        var result = service.applyKeyframe(draft.draftId(), new AuthoringKeyframeMutation("right_arm", 12,
                new AuthoringTransform(1.0D, 2.0D, 3.0D, 0.0D, 0.0D, 15.0D, 1.0D, 1.0D, 1.0D), "LINEAR"));

        Assertions.assertEquals(2, result.draft().orElseThrow().keyframes());
        Assertions.assertFalse(java.nio.file.Files.exists(draft.sourcePath()));
    }

    @Test
    void undoRestoresPreviousDraftKeyframeSet() {
        var service = new DefaultAnimationAuthoringService(() -> Optional.of(temp));
        var draft = service.openDraft(new AuthoringDraftOpenRequest("jdh:editor/wave", "jdh:humanoid"))
                .draft().orElseThrow();
        service.applyKeyframe(draft.draftId(), new AuthoringKeyframeMutation("right_arm", 12,
                AuthoringTransform.identity(), "LINEAR"));

        AuthoringOperationResult result = service.undo(draft.draftId());

        Assertions.assertEquals(AuthoringOperationResult.Status.UNDONE, result.status());
        Assertions.assertEquals(1, result.draft().orElseThrow().keyframes());
    }

    @Test
    void autosaveAndRecoverUseSidecarDraftState() throws java.io.IOException {
        var service = new DefaultAnimationAuthoringService(() -> Optional.of(temp));
        var draft = service.openDraft(new AuthoringDraftOpenRequest("jdh:editor/wave", "jdh:humanoid"))
                .draft().orElseThrow();
        service.applyKeyframe(draft.draftId(), new AuthoringKeyframeMutation("right_arm", 12,
                AuthoringTransform.identity(), "LINEAR"));

        AuthoringOperationResult autosave = service.autosave(draft.draftId());
        AuthoringOperationResult recovered = service.recoverDraft(new AuthoringDraftOpenRequest(
                "jdh:editor/wave", "jdh:humanoid"));

        Assertions.assertEquals(AuthoringOperationResult.Status.AUTOSAVED, autosave.status());
        Assertions.assertTrue(Files.isRegularFile(autosave.path().orElseThrow()));
        Assertions.assertEquals(AuthoringOperationResult.Status.RECOVERED, recovered.status());
        Assertions.assertEquals(2, recovered.draft().orElseThrow().keyframes());
        Assertions.assertFalse(Files.exists(draft.sourcePath()));
    }
}
