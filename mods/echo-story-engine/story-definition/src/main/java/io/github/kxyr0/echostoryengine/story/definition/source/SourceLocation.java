package io.github.kxyr0.echostoryengine.story.definition.source;

/**
 * Optional one-based source location for diagnostics.
 */
public record SourceLocation(int line, int column) {
    private static final SourceLocation UNKNOWN = new SourceLocation(0, 0);

    public SourceLocation {
        if (line < 0 || column < 0) {
            throw new IllegalArgumentException("source location values must be non-negative");
        }
        if ((line == 0) != (column == 0)) {
            throw new IllegalArgumentException("line and column must both be known or both be unknown");
        }
    }

    public static SourceLocation unknown() {
        return UNKNOWN;
    }

    public static SourceLocation of(int line, int column) {
        if (line <= 0 || column <= 0) {
            throw new IllegalArgumentException("known source location values must be positive");
        }
        return new SourceLocation(line, column);
    }

    public boolean isKnown() {
        return line > 0;
    }

    public String asSubjectValue() {
        return isKnown() ? line + ":" + column : "";
    }
}
