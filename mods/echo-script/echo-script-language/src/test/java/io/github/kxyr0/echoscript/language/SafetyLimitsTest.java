package io.github.kxyr0.echoscript.language;

import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.ProcessingLimits;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.diagnostic.DiagnosticCodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SafetyLimitsTest {
    @Test
    void enforcesSourceTokenDiagnosticNestingAndMemberLimits() {
        assertHasCode(parse("class A : B {}", limits(5, 100, 10, 10, 10, 10, 10, 10, 10, 10)),
                DiagnosticCodes.SOURCE_TOO_LARGE);
        assertHasCode(parse(
                "class A : B { private T a; private T b; }",
                limits(1000, 8, 10, 10, 10, 10, 10, 10, 10, 10)
        ),
                DiagnosticCodes.TOKEN_LIMIT_EXCEEDED);
        assertHasCode(parse("@ @ @ @ @", limits(1000, 100, 2, 10, 10, 10, 10, 10, 10, 10)),
                DiagnosticCodes.DIAGNOSTIC_LIMIT_EXCEEDED);
        assertHasCode(parse("class A : B { private void M() { (((((1))))); } }",
                limits(1000, 100, 10, 2, 10, 10, 10, 10, 10, 10)), DiagnosticCodes.NESTING_LIMIT_EXCEEDED);
        assertHasCode(parse("class A : B { private T a; private T b; }",
                limits(1000, 100, 10, 10, 1, 10, 10, 10, 10, 10)), DiagnosticCodes.MEMBER_LIMIT_EXCEEDED);
    }

    @Test
    void enforcesStatementArgumentArrayAndStringLimits() {
        assertHasCode(parse("class A : B { private void M() { A(); B(); } }",
                limits(1000, 100, 10, 10, 10, 1, 10, 10, 10, 10)), DiagnosticCodes.STATEMENT_LIMIT_EXCEEDED);
        assertHasCode(parse("class A : B { private void M() { Call(1, 2); } }",
                limits(1000, 100, 10, 10, 10, 10, 1, 10, 10, 10)), DiagnosticCodes.ARGUMENT_LIMIT_EXCEEDED);
        assertHasCode(parse("class A : B { private T[] x = [1, 2]; }",
                limits(1000, 100, 10, 10, 10, 10, 10, 1, 10, 10)), DiagnosticCodes.ARRAY_ELEMENT_LIMIT_EXCEEDED);
        assertHasCode(parse("class A : B { private T x = \"abc\"; }",
                limits(1000, 100, 10, 10, 10, 10, 10, 10, 1, 10)), DiagnosticCodes.STRING_LIMIT_EXCEEDED);
    }

    @Test
    void limitFailuresRemainBoundedAndDefaultsAcceptCanonicalShape() {
        ParseResult bounded = parse("@ @ @ @ @", limits(1000, 100, 2, 10, 10, 10, 10, 10, 10, 10));
        ParseResult canonicalShape = parse("class A : B { private void M() { Call(\"Стоять\", key: \"k\"); } }",
                ProcessingLimits.defaults());

        assertTrue(bounded.diagnostics().size() <= 2);
        assertTrue(canonicalShape.successful());
    }

    private ParseResult parse(String source, ProcessingLimits limits) {
        return EchoScriptLanguage.parser().parse(new ParseRequest(
                io.github.kxyr0.echoscript.api.LanguageVersion.V1,
                new SourceId("limits.es"),
                source,
                limits
        ));
    }

    private static ProcessingLimits limits(
            int source,
            int tokens,
            int diagnostics,
            int nesting,
            int members,
            int statements,
            int arguments,
            int elements,
            int string,
            int qualified
    ) {
        return new ProcessingLimits(
                source,
                tokens,
                diagnostics,
                nesting,
                members,
                statements,
                arguments,
                elements,
                string,
                qualified
        );
    }

    private static void assertHasCode(ParseResult result, String code) {
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals(code)));
    }
}
