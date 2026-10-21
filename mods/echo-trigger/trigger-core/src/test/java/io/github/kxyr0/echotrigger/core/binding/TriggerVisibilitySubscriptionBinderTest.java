package io.github.kxyr0.echotrigger.core.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import org.junit.jupiter.api.Test;

class TriggerVisibilitySubscriptionBinderTest {
    @Test
    void bindsLostSightPerArmSubscription() {
        var parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(new SourceId("visibility.trigger.es"), """
                class FatherVanishTrigger : TriggerResource
                {
                    private VisibilityWatchId Father = "jdh:home/father_vanish";
                    private StoryId RemoveFather = "jdh:chapter_1/remove_father";
                    private void Init()
                    {
                        Id("jdh:father_vanish_trigger");
                        Subscribe(Player.LostSightOf(Father));
                        StartStory(RemoveFather);
                        FireOnlyOncePerArm();
                    }
                }
                """));
        assertTrue(parsed.successful(), () -> parsed.diagnostics().toString());
        var result = new TriggerDefinitionBinder().bind(
                parsed.syntaxTree().orElseThrow(), "test", new TriggerRevision(1));

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        var subscription = result.definition().orElseThrow().storySubscription().orElseThrow();
        assertEquals("echo_visibility:events", subscription.selector().providerId());
        assertEquals("LOST_SIGHT", subscription.selector().eventKind());
        assertEquals("jdh:home/father_vanish", subscription.selector().scenarioId());
        assertTrue(subscription.fireOnlyOnce());
    }
}
