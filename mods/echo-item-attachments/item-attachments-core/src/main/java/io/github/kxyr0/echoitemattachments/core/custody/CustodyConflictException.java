package io.github.kxyr0.echoitemattachments.core.custody;

public final class CustodyConflictException extends IllegalStateException {
    private final String diagnosticCode;

    public CustodyConflictException(String diagnosticCode, String message) {
        super(message);
        this.diagnosticCode = diagnosticCode;
    }

    public String diagnosticCode() {
        return diagnosticCode;
    }
}
