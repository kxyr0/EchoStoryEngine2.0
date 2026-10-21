package io.github.kxyr0.echostoryengine.story.extension.api.outcome;

import java.util.Objects;

/**
 * Immediate result returned by start, recover or cancel.
 */
public record ActionInvocationResult(
        ActionInvocationStatus status,
        String detail) {
    public ActionInvocationResult {
        status = Objects.requireNonNull(status, "status");
        detail = Objects.requireNonNull(detail, "detail");
    }

    public static ActionInvocationResult accepted() {
        return new ActionInvocationResult(ActionInvocationStatus.ACCEPTED, "");
    }

    public static ActionInvocationResult rejected(String detail) {
        return new ActionInvocationResult(ActionInvocationStatus.REJECTED, requireText(detail, "detail"));
    }

    public static ActionInvocationResult unsupported(String detail) {
        return new ActionInvocationResult(ActionInvocationStatus.UNSUPPORTED, requireText(detail, "detail"));
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return checked;
    }
}
