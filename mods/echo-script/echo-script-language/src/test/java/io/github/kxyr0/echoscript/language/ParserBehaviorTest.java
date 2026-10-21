package io.github.kxyr0.echoscript.language;

import io.github.kxyr0.echoscript.api.EchoScriptParser;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.SyntaxModifier;
import io.github.kxyr0.echoscript.api.syntax.declaration.FieldSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassSectionSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.DialogueNodeDeclarationSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.DecimalLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.IfStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.LocalDeclarationStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ReturnStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.SectionStatementSyntax;
import io.github.kxyr0.echoscript.language.diagnostic.DiagnosticCodes;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserBehaviorTest {
    private final EchoScriptParser parser = EchoScriptLanguage.parser();

    @Test
    void canonicalFixtureParsesWithExpectedTreeAndRussianText() throws IOException {
        ParseResult result = parse(fixture());

        assertTrue(result.successful());
        assertTrue(result.diagnostics().isEmpty());
        CompilationUnitSyntax unit = result.syntaxTree().orElseThrow();
        assertEquals(
                "EchoStoryEngine.Story.Dialogues.Chapter1",
                unit.namespaceDeclaration().orElseThrow().name().text()
        );
        ClassSyntax clazz = unit.classDeclaration();
        assertEquals("Chapter1Home", clazz.name());
        assertEquals("DialogueResource", clazz.baseType().name().text());
        assertEquals(2, clazz.members().stream().filter(FieldSyntax.class::isInstance).count());
        assertEquals(5, clazz.members().stream().filter(MethodSyntax.class::isInstance).count());

        FieldSyntax nodes = (FieldSyntax) clazz.members().get(1);
        assertEquals("NodeDef", nodes.type().name().text());
        assertEquals(1, nodes.type().arrayRank());

        MethodSyntax build = method(clazz, "Build");
        assertEquals(List.of(SyntaxModifier.PUBLIC, SyntaxModifier.OVERRIDE), build.modifiers());
        assertInstanceOf(ReturnStatementSyntax.class, build.body().statements().get(0));

        MethodSyntax home = method(clazz, "Home");
        assertInstanceOf(LocalDeclarationStatementSyntax.class, home.body().statements().get(0));
        InvocationExpressionSyntax line = (InvocationExpressionSyntax) expressionStatement(home, 1);
        assertEquals("Стоять", stringArgument(line, 1));
        assertEquals(List.of("key", "face"), namedArguments(line));
    }

    @Test
    void syntaxCollectionsAreImmutable() {
        CompilationUnitSyntax unit = parse("class A : B { private T x; }").syntaxTree().orElseThrow();
        ClassSyntax clazz = unit.classDeclaration();

        assertThrows(UnsupportedOperationException.class, () -> clazz.members().add(clazz.members().get(0)));
    }

    @Test
    void reportsPreciseDelimiterAndMalformedDeclarationDiagnostics() {
        assertHasCode(parse("class A : B { private T x }"), DiagnosticCodes.MISSING_SEMICOLON);
        assertHasCode(
                parse("class A : B { private void M( { return; } }"),
                DiagnosticCodes.MISSING_CLOSING_PARENTHESIS
        );
        assertHasCode(
                parse("class A : B { private T[] x; private T y = [1, 2; }"),
                DiagnosticCodes.MISSING_CLOSING_BRACKET
        );
        assertHasCode(parse("class A : B { private T x;"), DiagnosticCodes.MISSING_CLOSING_BRACE);
        assertHasCode(parse("class A : B { private T = 1; }"), DiagnosticCodes.MALFORMED_FIELD);
        assertHasCode(parse("class A : B { private void M( }"), DiagnosticCodes.MALFORMED_METHOD);
    }

    @Test
    void reportsNamedArgumentErrorsAndRecoversToLaterIndependentErrors() {
        ParseResult duplicate = parse("class A : B { private void M() { Call(a: 1, a: 2); } }");
        ParseResult positional = parse("class A : B { private void M() { Call(a: 1, 2); } }");
        ParseResult malformed = parse("class A : B { private void M() { Call(a:); } }");
        ParseResult recovered = parse("class A : B { private T x private void M() { Call(; Missing(; } }");

        assertHasCode(duplicate, DiagnosticCodes.DUPLICATE_NAMED_ARGUMENT);
        assertHasCode(positional, DiagnosticCodes.POSITIONAL_AFTER_NAMED_ARGUMENT);
        assertHasCode(malformed, DiagnosticCodes.MALFORMED_NAMED_ARGUMENT);
        assertHasCode(recovered, DiagnosticCodes.MISSING_SEMICOLON);
        assertHasCode(recovered, DiagnosticCodes.MISSING_CLOSING_PARENTHESIS);
    }

    @Test
    void parsesDecimalLiteralsAsSyntaxOnlyNumbers() {
        ParseResult result = parse("class A : B { private void M() { Call(speed: -1.6); } }");

        InvocationExpressionSyntax call = (InvocationExpressionSyntax) expressionStatement(method(
                result.syntaxTree().orElseThrow().classDeclaration(), "M"), 0);

        DecimalLiteralExpressionSyntax speed = assertInstanceOf(
                DecimalLiteralExpressionSyntax.class,
                call.arguments().get(0).value()
        );
        assertEquals("-1.6", speed.value().toPlainString());
    }

    @Test
    void parsesGenericInvocationChainsWithoutDialogueSemantics() {
        ParseResult result = parse("""
                class A : B
                {
                    private void M()
                    {
                        Line("jf", "Text").SetAnimationFace(FaceId).SetAnimation(BodyId);
                    }
                }
                """);

        InvocationExpressionSyntax body = (InvocationExpressionSyntax) expressionStatement(method(
                result.syntaxTree().orElseThrow().classDeclaration(), "M"), 0);
        MemberAccessExpressionSyntax bodyTarget = assertInstanceOf(MemberAccessExpressionSyntax.class, body.target());
        InvocationExpressionSyntax face = assertInstanceOf(InvocationExpressionSyntax.class, bodyTarget.target());
        MemberAccessExpressionSyntax faceTarget = assertInstanceOf(MemberAccessExpressionSyntax.class, face.target());
        InvocationExpressionSyntax line = assertInstanceOf(InvocationExpressionSyntax.class, faceTarget.target());

        assertEquals("SetAnimation", bodyTarget.memberName());
        assertEquals("SetAnimationFace", faceTarget.memberName());
        assertEquals("Text", stringArgument(line, 1));
    }

    @Test
    void parsesIfElseAsSyntaxOnlyStatements() {
        ParseResult result = parse("""
                class A : B
                {
                    private void M()
                    {
                        if (PlayerInside(Exit)) {
                            Complete();
                        } else {
                            Wait(PlayerEnter(Exit));
                        }
                    }
                }
                """);

        MethodSyntax method = method(result.syntaxTree().orElseThrow().classDeclaration(), "M");
        IfStatementSyntax ifStatement = assertInstanceOf(IfStatementSyntax.class, method.body().statements().get(0));

        assertTrue(result.successful());
        assertTrue(result.diagnostics().isEmpty());
        assertTrue(ifStatement.elseStatement().isPresent());
        assertInstanceOf(InvocationExpressionSyntax.class, ifStatement.condition());
    }

    @Test
    void malformedSourceDoesNotThrowOrLoopAndLfCrLfHaveEquivalentMeaning() {
        assertDoesNotThrow(() -> parse("class A : B { private void M() { @@@ }"));

        ParseResult lf = parse("class A : B {\nprivate void M() {\nreturn;\n}\n}");
        ParseResult crlf = parse("class A : B {\r\nprivate void M() {\r\nreturn;\r\n}\r\n}");

        assertTrue(lf.successful());
        assertTrue(crlf.successful());
        assertEquals(lf.syntaxTree().orElseThrow().classDeclaration().name(), crlf.syntaxTree().orElseThrow()
                .classDeclaration()
                .name());
    }

    @Test
    void unsupportedAndMissingClassSyntaxAreDiagnostics() {
        assertHasCode(
                parse("class A : B { private void M() { while (true) { return; } } }"),
                DiagnosticCodes.UNSUPPORTED_SYNTAX
        );
        assertHasCode(parse("namespace A.B;"), DiagnosticCodes.MISSING_CLASS);
    }

    @Test
    void parsesMultipleTopLevelClassesInOneCompilationUnit() {
        ParseResult result = parse("class A : B {} class World : FacialAnimationSurface3d {}");

        assertTrue(result.successful());
        assertEquals(2, result.syntaxTree().orElseThrow().classDeclarations().size());
        assertEquals("A", result.syntaxTree().orElseThrow().classDeclaration().name());
        assertEquals("World", result.syntaxTree().orElseThrow().classDeclarations().get(1).name());
    }

    @Test
    void parsesHighLevelDialogueNodesAndSparseMetadataWithoutNamespace() {
        ParseResult result = parse("""
                class X : DialogueResource
                {
                    Id("jdh:x");
                    Speaker("sasha", display: "Незнакомка");
                    start Node Intro() { GoTo(Done); }
                    Node Done() { Formerly("old_done"); }
                }
                """);

        assertTrue(result.successful());
        assertTrue(result.syntaxTree().orElseThrow().namespaceDeclaration().isEmpty());
        ClassSyntax clazz = result.syntaxTree().orElseThrow().classDeclaration();
        assertEquals(2, clazz.members().stream().filter(ClassCallSyntax.class::isInstance).count());
        List<DialogueNodeDeclarationSyntax> nodes = clazz.members().stream()
                .filter(DialogueNodeDeclarationSyntax.class::isInstance)
                .map(DialogueNodeDeclarationSyntax.class::cast)
                .toList();
        assertEquals(List.of("Intro", "Done"), nodes.stream().map(DialogueNodeDeclarationSyntax::name).toList());
        assertTrue(nodes.get(0).entry());
    }

    @Test
    void nodeRemainsAnOrdinaryTypeOutsideDialogueResource() {
        ParseResult result = parse("class X : LibraryResource { Node Create() { return; } }");

        assertTrue(result.successful());
        assertInstanceOf(MethodSyntax.class, result.syntaxTree().orElseThrow().classDeclaration().members().get(0));
    }

    @Test
    void parsesInvocationSectionsWithoutAssigningDomainSemantics() {
        ParseResult result = parse("""
                class X : ObservationResource {
                    Observation DoorLock() {
                        OnComplete() { Story.Start("jdh:done"); }
                    }
                }
                """);

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        MethodSyntax observation = method(result.syntaxTree().orElseThrow().classDeclaration(), "DoorLock");
        SectionStatementSyntax section = assertInstanceOf(
                SectionStatementSyntax.class, observation.body().statements().get(0));
        assertEquals(1, section.body().statements().size());
    }

    @Test
    void parsesClassLevelIntentSectionsWithoutAssigningDomainSemantics() {
        ParseResult result = parse("""
                class StudentMira : BackgroundNpcResource {
                    Home("jdh:dorm_mira");
                    At("09:00") { GoTo("jdh:university"); }
                }
                """);

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        ClassSyntax clazz = result.syntaxTree().orElseThrow().classDeclaration();
        assertInstanceOf(ClassCallSyntax.class, clazz.members().get(0));
        ClassSectionSyntax section = assertInstanceOf(ClassSectionSyntax.class, clazz.members().get(1));
        assertEquals(1, section.body().statements().size());
    }

    @Test
    void preservesContextualStartModifierForDomainMethods() {
        ParseResult result = parse("class X : SceneResource { start Scene Morning() { Time.Set(\"08:20\"); } }");

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        MethodSyntax scene = method(result.syntaxTree().orElseThrow().classDeclaration(), "Morning");
        assertEquals(List.of(SyntaxModifier.START), scene.modifiers());
    }

    private ParseResult parse(String source) {
        return parser.parse(ParseRequest.ofV1(new SourceId("fixtures/chapter_1_home.es"), source));
    }

    private static MethodSyntax method(ClassSyntax clazz, String name) {
        return clazz.members().stream()
                .filter(MethodSyntax.class::isInstance)
                .map(MethodSyntax.class::cast)
                .filter(method -> method.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static Object expressionStatement(MethodSyntax method, int index) {
        return ((io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax) method.body()
                .statements()
                .get(index)).expression();
    }

    private static String stringArgument(InvocationExpressionSyntax invocation, int index) {
        return invocation.arguments().stream()
                .map(ArgumentSyntax::value)
                .filter(StringLiteralExpressionSyntax.class::isInstance)
                .map(StringLiteralExpressionSyntax.class::cast)
                .map(StringLiteralExpressionSyntax::value)
                .skip(index)
                .findFirst()
                .orElseThrow();
    }

    private static List<String> namedArguments(InvocationExpressionSyntax invocation) {
        return invocation.arguments().stream().flatMap(argument -> argument.name().stream()).toList();
    }

    private static void assertHasCode(ParseResult result, String code) {
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals(code)));
    }

    private static String fixture() throws IOException {
        try (InputStream stream = ParserBehaviorTest.class.getResourceAsStream("/fixtures/chapter_1_home.es")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
