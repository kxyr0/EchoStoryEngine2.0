package io.github.kxyr0.echostoryengine.story.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import org.junit.jupiter.api.Test;

final class EchoScriptTimeCompilerTest {
    @Test
    void canonicalTimeDslCompilesThroughRealStoryBinder() {
        var result = EchoScriptAdapterFixture.compile("fixtures/time.story.es", """
                namespace JDH.Chapter1.Home;

                class WaitForMeeting : StoryResource
                {
                    public override StoryResourceDef Build()
                    {
                        return Story("jdh:wait_for_meeting", start: "start");
                    }

                    private void OnStart()
                    {
                        Time.Set("10:00");
                        Time.Set(Day(2), "08:30");
                        Time.Add(Minutes(10));
                        Time.Add(Hours(2), crossed: SKIP_CROSSED);
                        Time.Pause();
                        Time.Resume();
                        Time.WaitUntil("10:00");
                        Time.Is("09:50");
                        Time.IsBefore("10:00");
                        Time.IsAfter("08:00");
                        Time.IsBetween("23:00", "01:00");
                    }
                }
                """);

        assertTrue(result.compilationSucceeded(), result.diagnostics().toString());
        var story = result.compiledStory().orElseThrow();
        assertEquals("echo_time:set", story.find(NodeId.of("time_set")).orElseThrow().actionType().asString());
        assertEquals("2", story.find(NodeId.of("time_set_2")).orElseThrow().actionArguments().get("day"));
        assertEquals("Hours", story.find(NodeId.of("time_add_2")).orElseThrow()
                .actionArguments().get("durationUnit"));
        assertEquals("SKIP_CROSSED", story.find(NodeId.of("time_add_2")).orElseThrow()
                .actionArguments().get("crossingPolicy"));
        assertEquals("echo_time:wait_until", story.find(NodeId.of("time_wait_until")).orElseThrow()
                .actionType().asString());
        assertEquals("23:00", story.find(NodeId.of("time_query_4")).orElseThrow()
                .actionArguments().get("start"));
    }
}
