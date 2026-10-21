package io.github.kxyr0.echoscript.language.diagnostic;

public final class DiagnosticCodes {
    public static final String UNSUPPORTED_LANGUAGE_VERSION = "ESL-LANGUAGE-001";
    public static final String SOURCE_TOO_LARGE = "ESL-LIMIT-001";
    public static final String TOKEN_LIMIT_EXCEEDED = "ESL-LIMIT-002";
    public static final String DIAGNOSTIC_LIMIT_EXCEEDED = "ESL-LIMIT-003";
    public static final String NESTING_LIMIT_EXCEEDED = "ESL-LIMIT-004";
    public static final String MEMBER_LIMIT_EXCEEDED = "ESL-LIMIT-005";
    public static final String STATEMENT_LIMIT_EXCEEDED = "ESL-LIMIT-006";
    public static final String ARGUMENT_LIMIT_EXCEEDED = "ESL-LIMIT-007";
    public static final String ARRAY_ELEMENT_LIMIT_EXCEEDED = "ESL-LIMIT-008";
    public static final String STRING_LIMIT_EXCEEDED = "ESL-LIMIT-009";
    public static final String QUALIFIED_NAME_LIMIT_EXCEEDED = "ESL-LIMIT-010";
    public static final String UNEXPECTED_CHARACTER = "ESL-LEX-001";
    public static final String INVALID_STRING_ESCAPE = "ESL-LEX-002";
    public static final String UNTERMINATED_STRING = "ESL-LEX-003";
    public static final String UNTERMINATED_BLOCK_COMMENT = "ESL-LEX-004";
    public static final String UNEXPECTED_TOKEN = "ESL-PARSE-001";
    public static final String EXPECTED_TOKEN = "ESL-PARSE-002";
    public static final String MISSING_SEMICOLON = "ESL-PARSE-003";
    public static final String MISSING_CLOSING_PARENTHESIS = "ESL-PARSE-004";
    public static final String MISSING_CLOSING_BRACKET = "ESL-PARSE-005";
    public static final String MISSING_CLOSING_BRACE = "ESL-PARSE-006";
    public static final String MALFORMED_FIELD = "ESL-PARSE-007";
    public static final String MALFORMED_METHOD = "ESL-PARSE-008";
    public static final String MALFORMED_NAMED_ARGUMENT = "ESL-PARSE-009";
    public static final String DUPLICATE_NAMED_ARGUMENT = "ESL-PARSE-010";
    public static final String POSITIONAL_AFTER_NAMED_ARGUMENT = "ESL-PARSE-011";
    public static final String MULTIPLE_CLASSES = "ESL-PARSE-012";
    public static final String MISSING_CLASS = "ESL-PARSE-013";
    public static final String UNSUPPORTED_SYNTAX = "ESL-PARSE-014";

    private DiagnosticCodes() {
    }
}
