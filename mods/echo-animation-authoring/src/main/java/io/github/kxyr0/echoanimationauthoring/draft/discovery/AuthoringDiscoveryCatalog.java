package io.github.kxyr0.echoanimationauthoring.draft.discovery;

import io.github.kxyr0.echoanimationauthoring.api.discovery.AuthoringDiscoveryResult;
import io.github.kxyr0.echoanimationauthoring.api.service.AuthoringDraftOpenRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;

public final class AuthoringDiscoveryCatalog {
    private static final String ANIMATION_BRIDGE = "io.github.kxyr0.echoanimation.forge.api.AnimationServiceBridge";
    private static final String ANIMATION_RIG_ID = "io.github.kxyr0.echoanimation.api.rig.AnimationRigId";
    private static final List<String> HUMANOID_BONES = List.of("root", "body", "head", "left_shoulder",
            "left_upper_arm", "left_forearm", "right_shoulder", "right_upper_arm", "right_forearm",
            "left_thigh", "left_shin", "right_thigh", "right_shin");
    private static final List<String> FACE_ELEMENTS = List.of("left_eye", "right_eye", "left_brow", "right_brow",
            "mouth", "jaw", "glasses", "hair_front", "hair_back");

    public AuthoringDiscoveryResult discover(AuthoringDraftOpenRequest request) {
        Optional<Object> service = animationService();
        if (service.isEmpty()) {
            return new AuthoringDiscoveryResult(AuthoringDiscoveryResult.Status.FALLBACK, List.of(),
                    List.of(request.clipId()), List.of(request.rigId()), HUMANOID_BONES, FACE_ELEMENTS,
                    "Echo Animation service unavailable; using authoring fallback discovery.");
        }
        List<String> rigBones = rigBones(service.orElseThrow(), request.rigId());
        return new AuthoringDiscoveryResult(AuthoringDiscoveryResult.Status.DISCOVERED, List.of(),
                values(service.orElseThrow(), "animationClips"), values(service.orElseThrow(), "animationRigs"),
                rigBones.isEmpty() ? HUMANOID_BONES : rigBones, FACE_ELEMENTS,
                rigBones.isEmpty()
                        ? "Echo Animation public ids discovered; rig bones unavailable, using fallback bones."
                        : "Echo Animation public ids and rig bones discovered.");
    }

    private Optional<Object> animationService() {
        try {
            Object lookup = Class.forName(ANIMATION_BRIDGE).getMethod("lookup").invoke(null);
            Optional<?> service = optional(lookup, "service");
            return service.map(Object.class::cast);
        } catch (ReflectiveOperationException exception) {
            return Optional.empty();
        }
    }

    private static List<String> values(Object service, String method) {
        try {
            return ((List<?>) service.getClass().getMethod(method).invoke(service)).stream()
                    .map(String::valueOf)
                    .sorted()
                    .toList();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            return List.of();
        }
    }

    private static List<String> rigBones(Object service, String rigId) {
        try {
            Class<?> rigIdClass = Class.forName(ANIMATION_RIG_ID);
            Object parsedRigId = rigIdClass.getMethod("parse", String.class).invoke(null, rigId);
            return ((List<?>) service.getClass().getMethod("animationRigBones", rigIdClass)
                    .invoke(service, parsedRigId)).stream()
                    .map(String::valueOf)
                    .sorted()
                    .toList();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException
                 | ClassNotFoundException exception) {
            return List.of();
        }
    }

    private static Optional<?> optional(Object target, String method) throws ReflectiveOperationException {
        return (Optional<?>) target.getClass().getMethod(method).invoke(target);
    }
}
