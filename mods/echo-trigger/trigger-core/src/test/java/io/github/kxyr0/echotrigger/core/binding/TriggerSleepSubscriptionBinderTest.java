package io.github.kxyr0.echotrigger.core.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echotrigger.api.status.TriggerDiagnosticCode;
import io.github.kxyr0.echotrigger.core.definition.TriggerCompileResult;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import org.junit.jupiter.api.Test;

class TriggerSleepSubscriptionBinderTest {
    private final TriggerDefinitionBinder binder = new TriggerDefinitionBinder();

    @Test
    void bindsDeclarativeSleepStorySubscription() {
        TriggerCompileResult result = bind("""
                class FirstNightSleepSubscription : TriggerResource
                {
                    private PlayerSleepScenarioId FirstNight = "jdh:chapter_1/first_night";
                    private StoryId FirstNightDream = "jdh:chapter_1/first_night_dream";
                    private void Init()
                    {
                        Id("jdh:first_night_sleep");
                        Subscribe(PlayerSleep.Confirmed(FirstNight));
                        StartStory(FirstNightDream);
                        FireOnlyOnce();
                    }
                }
                """);

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        var definition = result.definition().orElseThrow();
        var subscription = definition.storySubscription().orElseThrow();
        assertTrue(definition.sequence().isEmpty());
        assertEquals("echo_player_sleep:events", subscription.selector().providerId());
        assertEquals("CONFIRMED", subscription.selector().eventKind());
        assertEquals("jdh:chapter_1/first_night", subscription.selector().scenarioId());
        assertEquals("jdh:chapter_1/first_night_dream", subscription.storyId());
        assertTrue(subscription.fireOnlyOnce());
    }

    @Test
    void bindsExactSleepCountSubscription() {
        TriggerCompileResult result = bind("""
                class FifthNightSleepSubscription : TriggerResource
                {
                    private void Init()
                    {
                        Id("jdh:fifth_night_sleep");
                        Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/night", sleepCount: 5));
                        StartStory("jdh:chapter_1/fifth_night_dream");
                    }
                }
                """);

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        var selector = result.definition().orElseThrow().storySubscription().orElseThrow().selector();
        assertTrue(selector.sleepCount().isPresent());
        assertEquals(5L, selector.sleepCount().orElseThrow());
        assertTrue(selector.sleepCountAtLeast().isEmpty());
        assertEquals("echo_player_sleep:events|CONFIRMED|jdh:chapter_1/night|sleepCount=5",
                selector.exactKey());
    }

    @Test
    void bindsMinimumSleepCountSubscription() {
        TriggerCompileResult result = bind("""
                class AfterFifthNightSleepSubscription : TriggerResource
                {
                    private void Init()
                    {
                        Id("jdh:after_fifth_night_sleep");
                        Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/night", sleepCountAtLeast: 5));
                        StartStory("jdh:chapter_1/after_fifth_night_dream");
                    }
                }
                """);

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        var selector = result.definition().orElseThrow().storySubscription().orElseThrow().selector();
        assertTrue(selector.sleepCount().isEmpty());
        assertTrue(selector.sleepCountAtLeast().isPresent());
        assertEquals(5L, selector.sleepCountAtLeast().orElseThrow());
        assertTrue(selector.acceptsSleepCount(5L));
        assertTrue(selector.acceptsSleepCount(6L));
        assertFalse(selector.acceptsSleepCount(4L));
    }

    @Test
    void rejectsConflictingSleepCountConstraints() {
        TriggerCompileResult result = bind("""
                class BadSleepCountSubscription : TriggerResource
                {
                    private void Init()
                    {
                        Id("jdh:bad_sleep_count");
                        Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/night", sleepCount: 5, sleepCountAtLeast: 5));
                        StartStory("jdh:chapter_1/bad");
                    }
                }
                """);

        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code()
                        == TriggerDiagnosticCode.TRIGGER_SLEEP_SELECTOR_INVALID));
    }

    @Test
    void rejectsNonPositiveSleepCount() {
        TriggerCompileResult result = bind("""
                class BadSleepCountSubscription : TriggerResource
                {
                    private void Init()
                    {
                        Id("jdh:bad_sleep_count");
                        Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/night", sleepCount: 0));
                        StartStory("jdh:chapter_1/bad");
                    }
                }
                """);

        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code()
                        == TriggerDiagnosticCode.TRIGGER_SLEEP_SELECTOR_INVALID));
    }

    @Test
    void rejectsDirectDialogueForSleepSubscription() {
        TriggerCompileResult result = bind("""
                class SleepDialogueSubscription : TriggerResource
                {
                    private void Init()
                    {
                        Id("jdh:sleep_dialogue");
                        Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/night"));
                        StartDialogue("jdh:dialogue/sleep");
                    }
                }
                """);

        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code()
                        == TriggerDiagnosticCode.TRIGGER_STORY_SUBSCRIPTION_INVALID));
    }

    @Test
    void rejectsManualSleepCallbackAuthoring() {
        TriggerCompileResult result = bind("""
                class ManualSleepHandler : TriggerResource
                {
                    private void Init()
                    {
                        Id("jdh:manual_sleep");
                    }
                    private void OnPlayerSleepConfirmed(PlayerSleepEvent event)
                    {
                        Story.Start("jdh:first_night_dream");
                    }
                }
                """);

        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code()
                        == TriggerDiagnosticCode.TRIGGER_STORY_SUBSCRIPTION_INVALID));
    }

    private TriggerCompileResult bind(String source) {
        ParseResult parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(
                new SourceId("test.trigger.es"), source));
        assertTrue(parsed.successful(), () -> parsed.diagnostics().toString());
        return binder.bind(parsed.syntaxTree().orElseThrow(), "test", new TriggerRevision(1));
    }
}
