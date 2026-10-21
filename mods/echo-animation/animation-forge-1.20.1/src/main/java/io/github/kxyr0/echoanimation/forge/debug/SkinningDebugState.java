package io.github.kxyr0.echoanimation.forge.debug;

import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Process-local switch for manual smooth-skinning previews. */
public final class SkinningDebugState {
    public enum Mode {
        LEGACY,
        MATRIX,
        THERIG_BIND
    }

    private static final AtomicReference<AnimationSubjectId> TARGET = new AtomicReference<>();
    private static final AtomicReference<Mode> MODE = new AtomicReference<>(Mode.LEGACY);
    private static final AtomicReference<Float> THERIG_OFFSET_Y = new AtomicReference<>();
    private static final AtomicReference<Boolean> THERIG_ATTACHMENTS = new AtomicReference<>(false);

    private SkinningDebugState() {
    }

    public static Optional<AnimationSubjectId> target() {
        return Optional.ofNullable(TARGET.get());
    }

    public static Mode mode() {
        return MODE.get();
    }

    public static void target(String subjectId) {
        target(subjectId, Mode.LEGACY);
    }

    public static void target(String subjectId, Mode mode) {
        TARGET.set(new AnimationSubjectId(subjectId));
        MODE.set(mode);
    }

    public static boolean targets(AnimationSubjectId subjectId) {
        return subjectId.equals(TARGET.get());
    }

    public static Optional<Float> therigOffsetY() {
        return Optional.ofNullable(THERIG_OFFSET_Y.get());
    }

    public static void therigOffsetY(float pixels) {
        THERIG_OFFSET_Y.set(pixels);
    }

    public static void clearTherigOffsetY() {
        THERIG_OFFSET_Y.set(null);
    }

    public static boolean therigAttachmentsEnabled() {
        return Boolean.TRUE.equals(THERIG_ATTACHMENTS.get());
    }

    public static void therigAttachmentsEnabled(boolean enabled) {
        THERIG_ATTACHMENTS.set(enabled);
    }

    public static void clear() {
        TARGET.set(null);
        MODE.set(Mode.LEGACY);
        THERIG_OFFSET_Y.set(null);
        THERIG_ATTACHMENTS.set(false);
    }
}
