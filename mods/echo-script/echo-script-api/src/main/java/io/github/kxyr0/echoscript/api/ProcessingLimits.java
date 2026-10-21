package io.github.kxyr0.echoscript.api;

/**
 * Immutable bounded-processing configuration for a parse request.
 *
 * <p>Defaults are intentionally high enough for normal `.es` files and low enough to bound memory and diagnostics.
 * All values must be positive. Limit failures are reported as structured diagnostics.</p>
 */
public record ProcessingLimits(
        int maxSourceLength,
        int maxTokenCount,
        int maxDiagnostics,
        int maxParserNestingDepth,
        int maxClassMembers,
        int maxMethodStatements,
        int maxInvocationArguments,
        int maxArrayElements,
        int maxStringLiteralLength,
        int maxQualifiedNameSegments
) {
    private static final int DEFAULT_SOURCE_LENGTH = 1_000_000;
    private static final int DEFAULT_TOKEN_COUNT = 50_000;
    private static final int DEFAULT_DIAGNOSTICS = 100;
    private static final int DEFAULT_NESTING_DEPTH = 64;
    private static final int DEFAULT_CLASS_MEMBERS = 512;
    private static final int DEFAULT_METHOD_STATEMENTS = 1_000;
    private static final int DEFAULT_INVOCATION_ARGUMENTS = 128;
    private static final int DEFAULT_ARRAY_ELEMENTS = 1_024;
    private static final int DEFAULT_STRING_LENGTH = 16_384;
    private static final int DEFAULT_QUALIFIED_NAME_SEGMENTS = 64;

    public ProcessingLimits {
        requirePositive(maxSourceLength, "maxSourceLength");
        requirePositive(maxTokenCount, "maxTokenCount");
        requirePositive(maxDiagnostics, "maxDiagnostics");
        requirePositive(maxParserNestingDepth, "maxParserNestingDepth");
        requirePositive(maxClassMembers, "maxClassMembers");
        requirePositive(maxMethodStatements, "maxMethodStatements");
        requirePositive(maxInvocationArguments, "maxInvocationArguments");
        requirePositive(maxArrayElements, "maxArrayElements");
        requirePositive(maxStringLiteralLength, "maxStringLiteralLength");
        requirePositive(maxQualifiedNameSegments, "maxQualifiedNameSegments");
    }

    public static ProcessingLimits defaults() {
        return new ProcessingLimits(
                DEFAULT_SOURCE_LENGTH,
                DEFAULT_TOKEN_COUNT,
                DEFAULT_DIAGNOSTICS,
                DEFAULT_NESTING_DEPTH,
                DEFAULT_CLASS_MEMBERS,
                DEFAULT_METHOD_STATEMENTS,
                DEFAULT_INVOCATION_ARGUMENTS,
                DEFAULT_ARRAY_ELEMENTS,
                DEFAULT_STRING_LENGTH,
                DEFAULT_QUALIFIED_NAME_SEGMENTS
        );
    }

    private static void requirePositive(int value, String name) {
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be positive.");
        }
    }
}
