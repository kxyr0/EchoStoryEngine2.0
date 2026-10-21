package io.github.kxyr0.echonpcanimationadapter.command;

import io.github.kxyr0.echoanimation.api.animation.AnimationPlayResult;
import io.github.kxyr0.echoanimation.api.animation.AnimationStopResult;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/**
 * Formats adapter command results without coupling command routing to output text.
 */
final class AdapterCommandFeedback {
    private AdapterCommandFeedback() {
    }

    static void sendPlayResult(CommandSourceStack source, ActorId actorId, AnimationPlayResult result) {
        Component message = Component.literal("Echo Animation play " + result.status()
                + " npc=" + actorId
                + " animation=" + result.animationId()
                + " subject=" + result.subjectId()
                + result.playbackId().map(id -> " playback=" + id).orElse("")
                + " code=" + result.diagnosticCode()
                + " message=" + result.message());
        if (result.status() == AnimationPlayResult.Status.STARTED
                || result.status() == AnimationPlayResult.Status.ALREADY_APPLIED) {
            source.sendSuccess(() -> message, false);
        } else {
            source.sendFailure(message);
        }
    }

    static void sendStopResult(CommandSourceStack source, ActorId actorId, AnimationStopResult result) {
        Component message = Component.literal("Echo Animation stop " + result.status()
                + " npc=" + actorId
                + " subject=" + result.subjectId()
                + result.playbackId().map(id -> " playback=" + id).orElse("")
                + " code=" + result.diagnosticCode()
                + " message=" + result.message());
        if (result.status() == AnimationStopResult.Status.STOP_REQUESTED
                || result.status() == AnimationStopResult.Status.ALREADY_APPLIED) {
            source.sendSuccess(() -> message, false);
        } else {
            source.sendFailure(message);
        }
    }
}
