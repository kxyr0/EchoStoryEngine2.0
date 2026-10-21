package io.github.kxyr0.echoanimation.forge.playback.state;

import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.CommandRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.PlaybackRecord;
import java.util.Set;

/**
 * Classifies runtime-derived animation state that must never survive a server session.
 * Locomotion, system idle/sit poses and equivalent generated state are recomputed from
 * the physical NPC state after a world/server reload.
 */
public final class AnimationPlaybackPersistencePolicy {
    private static final String LOCOMOTION_COMMAND_PREFIX = "echo_npc_animation:locomotion/";
    private static final Set<String> TRANSIENT_ANIMATION_PATHS = Set.of(
            "humanoid_walk",
            "humanoid_run",
            "humanoid_idle",
            "humanoid_idle_main",
            "modern_idle_relaxed",
            "modern_idle_relaxed_main",
            "pose_idle",
            "pose_idle_main",
            "pose_sit",
            "pose_sit_main"
    );

    private AnimationPlaybackPersistencePolicy() {
    }

    public static boolean transientPlayback(PlaybackRecord playback) {
        return transientAnimation(playback.animationId) || transientCommand(playback.commandId.value());
    }

    public static boolean transientCommand(CommandRecord command) {
        return transientCommand(command.commandId.value());
    }

    public static boolean transientAnimation(AnimationId animationId) {
        String value = animationId.value();
        int separator = value.indexOf(':');
        String path = separator >= 0 ? value.substring(separator + 1) : value;
        return TRANSIENT_ANIMATION_PATHS.contains(path);
    }

    public static boolean transientCommand(String commandId) {
        return commandId.startsWith(LOCOMOTION_COMMAND_PREFIX);
    }
}
