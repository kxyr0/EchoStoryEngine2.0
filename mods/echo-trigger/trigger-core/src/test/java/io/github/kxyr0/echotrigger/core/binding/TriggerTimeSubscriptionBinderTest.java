package io.github.kxyr0.echotrigger.core.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echotrigger.core.definition.TriggerCompileResult;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import org.junit.jupiter.api.Test;

final class TriggerTimeSubscriptionBinderTest {
    private final TriggerDefinitionBinder binder = new TriggerDefinitionBinder();

    @Test
    void bindsDailyAndSpecificDayTimeSelectorsWithRealParser() {
        var daily = bind("Time.At(\"09:50\")", "StartDialogue(\"jdh:warning\")");
        var specific = bind("Time.At(Day(2), \"10:00\")", "StartStory(\"jdh:meeting\")");

        assertTrue(daily.successful(), daily.diagnostics().toString());
        assertTrue(specific.successful(), specific.diagnostics().toString());
        var dailySelector = daily.definition().orElseThrow().storySubscription().orElseThrow().selector();
        var specificSelector = specific.definition().orElseThrow().storySubscription().orElseThrow().selector();
        assertEquals("echo_time:daily/09_50", dailySelector.scenarioId());
        assertEquals("echo_time:day/2/10_00", specificSelector.scenarioId());
        assertTrue(daily.definition().orElseThrow().storySubscription().orElseThrow().startsDialogue());
    }

    private TriggerCompileResult bind(String selector, String target) {
        String source = """
                class TimeTrigger : TriggerResource
                {
                    private void Init()
                    {
                        Id("jdh:time_trigger");
                        Subscribe(%s);
                        %s;
                    }
                }
                """.formatted(selector, target);
        var parse = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(
                new SourceId("time.trigger.es"), source));
        assertTrue(parse.successful(), parse.diagnostics().toString());
        return binder.bind(parse.syntaxTree().orElseThrow(), "time.trigger.es", new TriggerRevision(1));
    }
}
