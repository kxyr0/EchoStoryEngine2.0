package io.github.kxyr0.echoanimationeditor.animationadapter.authoring;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;

public final class EchoAuthoringDraftClient {
    private static final String BRIDGE =
            "io.github.kxyr0.echoanimationauthoring.forge.api.AnimationAuthoringServiceBridge";
    private static final String OPEN_REQUEST =
            "io.github.kxyr0.echoanimationauthoring.api.service.AuthoringDraftOpenRequest";
    private static final String DRAFT_ID = "io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringDraftId";
    private static final String TRANSFORM = "io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringTransform";
    private static final String MUTATION =
            "io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringKeyframeMutation";

    public EchoAuthoringResult open(String clipId, String rigId) {
        try {
            Object lookup = Class.forName(BRIDGE).getMethod("lookup").invoke(null);
            Optional<?> service = optional(lookup, "service");
            if (service.isEmpty()) {
                return EchoAuthoringResult.failure(value(lookup, "message"));
            }
            Object result = open(service.orElseThrow(), clipId, rigId);
            Optional<?> draft = optional(result, "draft");
            if (draft.isEmpty()) {
                return EchoAuthoringResult.failure(value(result, "status") + " " + value(result, "message"));
            }
            return EchoAuthoringResult.opened(handle(service.orElseThrow(), draft.orElseThrow()),
                    value(result, "message"));
        } catch (ReflectiveOperationException exception) {
            return EchoAuthoringResult.failure(cause(exception));
        }
    }

    public EchoAuthoringResult applyKeyframe(EchoAuthoringDraftHandle handle, String boneId, int frame,
                                             EchoAuthoringTransform transform, String interpolation) {
        try {
            Object mutation = mutation(boneId, frame, transform, interpolation);
            Object result = handle.service().getClass()
                    .getMethod("applyKeyframe", Class.forName(DRAFT_ID), Class.forName(MUTATION))
                    .invoke(handle.service(), handle.draftId(), mutation);
            return statusResult(result, handle);
        } catch (ReflectiveOperationException exception) {
            return EchoAuthoringResult.failure(cause(exception));
        }
    }

    public EchoAuthoringResult save(EchoAuthoringDraftHandle handle) {
        return invokeDraftMethod(handle, "save");
    }

    public EchoAuthoringResult hotReload(EchoAuthoringDraftHandle handle) {
        return invokeDraftMethod(handle, "hotReload");
    }

    public EchoAuthoringResult undo(EchoAuthoringDraftHandle handle) {
        return invokeDraftMethod(handle, "undo");
    }

    public EchoAuthoringResult redo(EchoAuthoringDraftHandle handle) {
        return invokeDraftMethod(handle, "redo");
    }

    public EchoAuthoringResult autosave(EchoAuthoringDraftHandle handle) {
        return invokeDraftMethod(handle, "autosave");
    }

    public EchoAuthoringResult applyPreview(EchoAuthoringDraftHandle handle) {
        return invokeDraftMethod(handle, "applyPreview");
    }

    public EchoAuthoringResult clearPreview(EchoAuthoringDraftHandle handle) {
        return invokeDraftMethod(handle, "clearPreview");
    }

    public EchoAuthoringResult recover(String clipId, String rigId) {
        try {
            Object lookup = Class.forName(BRIDGE).getMethod("lookup").invoke(null);
            Optional<?> service = optional(lookup, "service");
            if (service.isEmpty()) {
                return EchoAuthoringResult.failure(value(lookup, "message"));
            }
            Object result = invokeRequestMethod(service.orElseThrow(), "recoverDraft", clipId, rigId);
            Optional<?> draft = optional(result, "draft");
            if (draft.isEmpty()) {
                return EchoAuthoringResult.failure(value(result, "status") + " " + value(result, "message"));
            }
            return EchoAuthoringResult.opened(handle(service.orElseThrow(), draft.orElseThrow()),
                    value(result, "status") + " " + value(result, "message"));
        } catch (ReflectiveOperationException exception) {
            return EchoAuthoringResult.failure(cause(exception));
        }
    }

    public EchoAuthoringDiscovery discover(String clipId, String rigId) {
        try {
            Object lookup = Class.forName(BRIDGE).getMethod("lookup").invoke(null);
            Optional<?> service = optional(lookup, "service");
            if (service.isEmpty()) {
                return EchoAuthoringDiscovery.unavailable(value(lookup, "message"));
            }
            Object result = invokeRequestMethod(service.orElseThrow(), "discover", clipId, rigId);
            return new EchoAuthoringDiscovery(!value(result, "status").contains("UNAVAILABLE"),
                    list(result, "subjects"), list(result, "clips"), list(result, "rigs"),
                    list(result, "bones"), list(result, "faceElements"), value(result, "message"));
        } catch (ReflectiveOperationException exception) {
            return EchoAuthoringDiscovery.unavailable(cause(exception));
        }
    }

    private EchoAuthoringResult invokeDraftMethod(EchoAuthoringDraftHandle handle, String method) {
        try {
            Object result = handle.service().getClass()
                    .getMethod(method, Class.forName(DRAFT_ID))
                    .invoke(handle.service(), handle.draftId());
            return statusResult(result, handle);
        } catch (ReflectiveOperationException exception) {
            return EchoAuthoringResult.failure(cause(exception));
        }
    }

    private Object open(Object service, String clipId, String rigId) throws ReflectiveOperationException {
        return invokeRequestMethod(service, "openDraft", clipId, rigId);
    }

    private Object invokeRequestMethod(Object service, String method, String clipId, String rigId)
            throws ReflectiveOperationException {
        Class<?> requestClass = Class.forName(OPEN_REQUEST);
        Object request = requestClass.getConstructor(String.class, String.class).newInstance(clipId, rigId);
        return service.getClass().getMethod(method, requestClass).invoke(service, request);
    }

    private Object mutation(String boneId, int frame, EchoAuthoringTransform value, String interpolation)
            throws ReflectiveOperationException {
        Class<?> transformClass = Class.forName(TRANSFORM);
        Object transform = transformClass
                .getConstructor(double.class, double.class, double.class, double.class, double.class,
                        double.class, double.class, double.class, double.class)
                .newInstance(value.x(), value.y(), value.z(), value.rotX(), value.rotY(), value.rotZ(),
                        value.scaleX(), value.scaleY(), value.scaleZ());
        return Class.forName(MUTATION)
                .getConstructor(String.class, int.class, transformClass, String.class)
                .newInstance(boneId, frame, transform, interpolation);
    }

    private EchoAuthoringDraftHandle handle(Object service, Object draft) throws ReflectiveOperationException {
        Object draftId = draft.getClass().getMethod("draftId").invoke(draft);
        Object sourcePath = draft.getClass().getMethod("sourcePath").invoke(draft);
        Object clipId = draft.getClass().getMethod("clipId").invoke(draft);
        return new EchoAuthoringDraftHandle(service, draftId, String.valueOf(clipId), String.valueOf(sourcePath));
    }

    private EchoAuthoringResult statusResult(Object result, EchoAuthoringDraftHandle handle)
            throws ReflectiveOperationException {
        String status = value(result, "status");
        String message = status + " " + value(result, "message");
        return operationSuccess(status) ? EchoAuthoringResult.opened(handle, message)
                : EchoAuthoringResult.failure(message);
    }

    private static boolean operationSuccess(String status) {
        return !status.contains("FAILURE") && !status.contains("NOT_FOUND") && !status.contains("NOT_SAVED")
                && !status.contains("NOTHING");
    }

    private static List<String> list(Object target, String method) throws ReflectiveOperationException {
        return ((List<?>) target.getClass().getMethod(method).invoke(target)).stream().map(String::valueOf).toList();
    }

    private static Optional<?> optional(Object target, String method) throws ReflectiveOperationException {
        return (Optional<?>) target.getClass().getMethod(method).invoke(target);
    }

    private static String value(Object target, String method) throws ReflectiveOperationException {
        return String.valueOf(target.getClass().getMethod(method).invoke(target));
    }

    private static String cause(ReflectiveOperationException exception) {
        Throwable cause = exception instanceof InvocationTargetException target
                ? target.getTargetException()
                : exception;
        return cause.getMessage();
    }
}
