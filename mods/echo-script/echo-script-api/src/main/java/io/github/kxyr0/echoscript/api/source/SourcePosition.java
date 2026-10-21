package io.github.kxyr0.echoscript.api.source;

/**
 * Immutable source position.
 *
 * <p>Offsets are zero-based UTF-16 code-unit offsets into the decoded source text. Lines and columns are one-based
 * and account for LF and CRLF line endings consistently.</p>
 */
public record SourcePosition(int offset, int line, int column) {
    public SourcePosition {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must not be negative.");
        }
        if (line < 1) {
            throw new IllegalArgumentException("Line must be one-based.");
        }
        if (column < 1) {
            throw new IllegalArgumentException("Column must be one-based.");
        }
    }
}
