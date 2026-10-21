package io.github.kxyr0.echoanimationeditor.forge.client;

import io.github.kxyr0.echoanimationeditor.animationadapter.authoring.EchoAuthoringDraftClient;
import io.github.kxyr0.echoanimationeditor.animationadapter.authoring.EchoAuthoringDraftHandle;
import io.github.kxyr0.echoanimationeditor.animationadapter.authoring.EchoAuthoringDiscovery;
import io.github.kxyr0.echoanimationeditor.animationadapter.authoring.EchoAuthoringResult;
import io.github.kxyr0.echoanimationeditor.animationadapter.authoring.EchoAuthoringTransform;
import java.util.Optional;

record AnimatorAuthoringState(
        EchoAuthoringDraftClient client,
        Optional<EchoAuthoringDraftHandle> draft,
        String message
) {
    static AnimatorAuthoringState open(Optional<String> clipId) {
        EchoAuthoringDraftClient client = new EchoAuthoringDraftClient();
        EchoAuthoringResult result = client.open(clipId.orElse("jdh:animator_editor_clip"), "jdh:humanoid");
        return new AnimatorAuthoringState(client, result.draft(), result.message());
    }

    AnimatorAuthoringState apply(String boneId, int frame, EchoAuthoringTransform transform) {
        if (draft.isEmpty()) {
            return new AnimatorAuthoringState(client, draft, "Authoring draft is unavailable: " + message);
        }
        EchoAuthoringResult result = client.applyKeyframe(draft.orElseThrow(), boneId, frame, transform, "LINEAR");
        return new AnimatorAuthoringState(client, result.draft().or(() -> draft), result.message());
    }

    AnimatorAuthoringState save() {
        if (draft.isEmpty()) {
            return new AnimatorAuthoringState(client, draft, "Authoring draft is unavailable: " + message);
        }
        EchoAuthoringResult result = client.save(draft.orElseThrow());
        return new AnimatorAuthoringState(client, result.draft().or(() -> draft), result.message());
    }

    AnimatorAuthoringState hotReload() {
        if (draft.isEmpty()) {
            return new AnimatorAuthoringState(client, draft, "Authoring draft is unavailable: " + message);
        }
        EchoAuthoringResult result = client.hotReload(draft.orElseThrow());
        return new AnimatorAuthoringState(client, result.draft().or(() -> draft), result.message());
    }

    AnimatorAuthoringState undo() {
        return invoke(EchoAuthoringDraftClient::undo);
    }

    AnimatorAuthoringState redo() {
        return invoke(EchoAuthoringDraftClient::redo);
    }

    AnimatorAuthoringState autosave() {
        return invoke(EchoAuthoringDraftClient::autosave);
    }

    AnimatorAuthoringState applyPreview() {
        return invoke(EchoAuthoringDraftClient::applyPreview);
    }

    AnimatorAuthoringState clearPreview() {
        return invoke(EchoAuthoringDraftClient::clearPreview);
    }

    AnimatorAuthoringState recover(String clipId, String rigId) {
        EchoAuthoringResult result = client.recover(clipId, rigId);
        return new AnimatorAuthoringState(client, result.draft().or(() -> draft), result.message());
    }

    AnimatorDiscoveryState discover(String clipId, String rigId) {
        EchoAuthoringDiscovery discovery = client.discover(clipId, rigId);
        return AnimatorDiscoveryState.from(discovery);
    }

    private AnimatorAuthoringState invoke(AuthoringCall call) {
        if (draft.isEmpty()) {
            return new AnimatorAuthoringState(client, draft, "Authoring draft is unavailable: " + message);
        }
        EchoAuthoringResult result = call.invoke(client, draft.orElseThrow());
        return new AnimatorAuthoringState(client, result.draft().or(() -> draft), result.message());
    }

    private interface AuthoringCall {
        EchoAuthoringResult invoke(EchoAuthoringDraftClient client, EchoAuthoringDraftHandle draft);
    }
}
