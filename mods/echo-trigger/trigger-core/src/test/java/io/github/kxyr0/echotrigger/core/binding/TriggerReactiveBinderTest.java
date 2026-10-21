package io.github.kxyr0.echotrigger.core.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackKind;
import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackSignature;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echotrigger.api.policy.TriggerMode;
import io.github.kxyr0.echotrigger.api.status.TriggerDiagnosticCode;
import io.github.kxyr0.echotrigger.core.definition.TriggerCompileResult;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import org.junit.jupiter.api.Test;

class TriggerReactiveBinderTest {
    private final TriggerDefinitionBinder binder = new TriggerDefinitionBinder();

    @Test
    void bindsReactiveCallbacksWithoutStartSequence() {
        TriggerCompileResult result = bind("""
                class ReactiveHouse : TriggerResource
                {
                    private TriggerArea House = BoxArea(Vector3(0, 64, 0), Vector3(4, 68, 4));
                    private void Init()
                    {
                        Id("jdh:reactive_house");
                        Dimension("minecraft:overworld");
                        Bind(House);
                        FireOnlyOnce();
                    }
                    private void OnTriggerEnter(PlayerRef player)
                    {
                        Dialogue.Start("jdh:house_enter");
                    }
                }
                """);

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        var definition = result.definition().orElseThrow();
        var callback = definition.reactive().orElseThrow().callbacks().get(TriggerCallbackKind.ENTER);
        assertEquals(TriggerMode.REPEAT, definition.mode());
        assertEquals(TriggerCallbackSignature.PLAYER_REF, callback.signature());
        assertEquals("Dialogue.Start", callback.actions().get(0).name());
    }

    @Test
    void bindsNpcMoveCallbackActionArguments() {
        TriggerCompileResult result = bind("""
                class ReactiveGate : TriggerResource
                {
                    private TriggerArea Gate = BoxArea(Vector3(0, 64, 0), Vector3(4, 68, 4));
                    private void Init()
                    {
                        Id("jdh:reactive_gate");
                        Dimension("minecraft:overworld");
                        Bind(Gate);
                    }
                    private void OnTriggerEnter(PlayerRef player)
                    {
                        Npc.Move("jdh:father", 200, 64, 180);
                    }
                }
                """);

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        var action = result.definition().orElseThrow().reactive().orElseThrow()
                .callbacks().get(TriggerCallbackKind.ENTER).actions().get(0);
        assertEquals("Npc.Move", action.name());
        assertEquals("jdh:father", action.arguments().get("arg0"));
        assertEquals("200", action.arguments().get("arg1"));
    }

    @Test
    void bindsNpcSprintVectorAndSpeed() {
        TriggerCompileResult result = bind("""
                class ReactiveSprint : TriggerResource
                {
                    private TriggerArea Area = CircleArea(Vector3(0, 64, 0), 2);
                    private void Init()
                    {
                        Id("jdh:reactive_sprint");
                        Dimension("minecraft:overworld");
                        Bind(Area);
                    }
                    private void OnTriggerEnter(PlayerRef player)
                    {
                        Npc.Sprint("jdh:father", Vector3(200, 64, 180), speed: 5.5);
                    }
                }
                """);

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        var action = result.definition().orElseThrow().reactive().orElseThrow()
                .callbacks().get(TriggerCallbackKind.ENTER).actions().get(0);
        assertEquals("Npc.Sprint", action.name());
        assertEquals("Vector3(200,64,180)", action.arguments().get("arg1"));
        assertEquals("5.5", action.arguments().get("speed"));
    }

    @Test
    void bindsTriggerEventCallbackSignature() {
        TriggerCompileResult result = bind("""
                class ReactiveExit : TriggerResource
                {
                    private TriggerArea Exit = SphereArea(center: Vector3(10, 70, 10), radius: 3);
                    private void Init()
                    {
                        Id("jdh:reactive_exit");
                        Dimension("minecraft:overworld");
                        Bind(Exit);
                    }
                    private void OnTriggerExit(TriggerEvent event) { Debug.Mark("exit"); }
                }
                """);

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        assertEquals(TriggerCallbackSignature.TRIGGER_EVENT, result.definition().orElseThrow()
                .reactive().orElseThrow().callbacks().get(TriggerCallbackKind.EXIT).signature());
    }

    @Test
    void rejectsCallbackOverloadsAsAmbiguous() {
        TriggerCompileResult result = bind("""
                class Ambiguous : TriggerResource
                {
                    private TriggerArea A = CircleArea(Vector3(0, 64, 0), 2);
                    private void Init() { Id("jdh:ambiguous"); Dimension("minecraft:overworld"); Bind(A); }
                    private void OnTriggerEnter() { Debug.Mark("a"); }
                    private void OnTriggerEnter(PlayerRef player) { Debug.Mark("b"); }
                }
                """);

        assertFalse(result.successful());
        assertTrue(has(result, TriggerDiagnosticCode.TRIGGER_CALLBACK_SIGNATURE_AMBIGUOUS));
    }

    @Test
    void rejectsInvalidCallbackParameter() {
        TriggerCompileResult result = bind("""
                class BadParam : TriggerResource
                {
                    private TriggerArea A = CircleArea(Vector3(0, 64, 0), 2);
                    private void Init() { Id("jdh:bad_param"); Dimension("minecraft:overworld"); Bind(A); }
                    private void OnTriggerStay(string value) { Debug.Mark("stay"); }
                }
                """);

        assertFalse(result.successful());
        assertTrue(has(result, TriggerDiagnosticCode.TRIGGER_CALLBACK_SIGNATURE_INVALID));
    }

    @Test
    void rejectsMultiplePrimaryAreas() {
        TriggerCompileResult result = bind("""
                class MultipleAreas : TriggerResource
                {
                    private TriggerArea A = BoxArea(Vector3(0, 64, 0), Vector3(2, 66, 2));
                    private TriggerArea B = BoxArea(Vector3(4, 64, 0), Vector3(6, 66, 2));
                    private void Init()
                    {
                        Id("jdh:multiple_areas");
                        Dimension("minecraft:overworld");
                        Bind(A);
                        Bind(B);
                    }
                    private void OnTriggerEnter() { Debug.Mark("enter"); }
                }
                """);

        assertFalse(result.successful());
        assertTrue(has(result, TriggerDiagnosticCode.TRIGGER_MULTIPLE_PRIMARY_AREAS));
    }

    @Test
    void rejectsExplicitRepeatWithFireOnlyOnce() {
        TriggerCompileResult result = bind("""
                class FireConflict : TriggerResource
                {
                    private TriggerArea A = CircleArea(Vector3(0, 64, 0), 2);
                    private void Init()
                    {
                        Id("jdh:fire_conflict");
                        Dimension("minecraft:overworld");
                        Bind(A);
                        Mode(REPEAT);
                        FireOnlyOnce();
                    }
                    private void OnTriggerEnter() { Debug.Mark("enter"); }
                }
                """);

        assertFalse(result.successful());
        assertTrue(has(result, TriggerDiagnosticCode.TRIGGER_FIRE_POLICY_CONFLICT));
    }

    private TriggerCompileResult bind(String source) {
        ParseResult parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(
                new SourceId("test.trigger.es"), source));
        assertTrue(parsed.successful(), () -> parsed.diagnostics().toString());
        return binder.bind(parsed.syntaxTree().orElseThrow(), "test", new TriggerRevision(1));
    }

    private static boolean has(TriggerCompileResult result, TriggerDiagnosticCode code) {
        return result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code() == code);
    }
}
