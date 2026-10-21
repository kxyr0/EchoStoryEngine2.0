package io.github.kxyr0.echostoryengine.story.persistence.schema;

/**
 * Expected decode failure for strict save schema reads.
 */
public final class SnapshotDecodeException extends Exception {
    private static final long serialVersionUID = 1L;

    private final String diagnosticCode;

    public SnapshotDecodeException(String diagnosticCode, String message) {
        super(message);
        this.diagnosticCode = diagnosticCode;
    }

    public SnapshotDecodeException(String diagnosticCode, String message, Throwable cause) {
        super(message, cause);
        this.diagnosticCode = diagnosticCode;
    }

    /**
     * Returns the persistence diagnostic code for this failure.
     */
    public String diagnosticCode() {
        return diagnosticCode;
    }
}
