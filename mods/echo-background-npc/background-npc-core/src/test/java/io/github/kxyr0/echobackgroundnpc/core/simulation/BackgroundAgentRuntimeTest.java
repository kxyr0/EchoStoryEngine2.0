package io.github.kxyr0.echobackgroundnpc.core.simulation;

import io.github.kxyr0.echobackgroundnpc.api.conversation.AmbientDialogue;
import io.github.kxyr0.echobackgroundnpc.api.conversation.AmbientLine;
import io.github.kxyr0.echobackgroundnpc.api.definition.BackgroundAppearancePool;
import io.github.kxyr0.echobackgroundnpc.api.definition.BackgroundNpcDefinition;
import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcDefinitionId;
import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.AmbientDialogueId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundAppearanceId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundAppearancePoolId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundLocationId;
import io.github.kxyr0.echobackgroundnpc.api.schedule.BackgroundScheduleEntry;
import io.github.kxyr0.echobackgroundnpc.api.source.BackgroundSourceRef;
import io.github.kxyr0.echobackgroundnpc.api.task.BackgroundTask;
import io.github.kxyr0.echobackgroundnpc.core.catalog.BackgroundCatalog;
import io.github.kxyr0.echobackgroundnpc.core.catalog.BackgroundCatalogHost;
import io.github.kxyr0.echobackgroundnpc.core.catalog.DeterministicAppearanceResolver;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BackgroundAgentRuntimeTest {
    private final BackgroundSourceRef source = new BackgroundSourceRef("mira.es", 1, 1);
    private final BackgroundNpcId mira = new BackgroundNpcId("jdh:mira");
    private final BackgroundNpcId anton = new BackgroundNpcId("jdh:anton");

    @Test
    void timeSetRecomputesStateAndDoesNotReplayOneShot() {
        BackgroundAgentRuntime runtime = new BackgroundAgentRuntime(catalog(1L, "room"), 8 * 60);
        var transitions = runtime.recomputeAll(12 * 60, "TIME_SET");
        assertFalse(transitions.isEmpty());
        var travel = assertInstanceOf(BackgroundTask.Travel.class, runtime.state(mira).orElseThrow().task());
        assertEquals(location("cafeteria"), travel.destination());
    }

    @Test
    void crossedConversationIsExactlyOnceAndOnlyPresentedWhenEligible() {
        BackgroundAgentRuntime runtime = new BackgroundAgentRuntime(catalog(1L, "room"), 9 * 60 + 45);
        var first = runtime.cross("echo_background_npc/jdh:mira/590", "day2/mira/590", true);
        var second = runtime.cross("echo_background_npc/jdh:mira/590", "day2/mira/590", true);
        assertTrue(first.conversation().isPresent());
        assertTrue(second.conversation().isEmpty());
    }

    @Test
    void hotReloadRecomputesCurrentDestinationAndBrokenCandidateKeepsLkg() {
        BackgroundAgentRuntime runtime = new BackgroundAgentRuntime(catalog(1L, "room"), 10 * 60 + 30);
        runtime.replaceCatalog(catalog(2L, "library"), 10 * 60 + 30);
        var task = assertInstanceOf(BackgroundTask.Travel.class, runtime.state(mira).orElseThrow().task());
        assertEquals(location("library"), task.destination());
        BackgroundCatalogHost host = new BackgroundCatalogHost();
        assertTrue(host.commit(Optional.of(catalog(1L, "room")), List.of()));
        assertFalse(host.commit(Optional.empty(), List.of()));
        assertEquals(1L, host.current().revision());
    }

    @Test
    void oneHundredVirtualAgentsHaveNoMaterializedBodies() {
        var definitions = java.util.stream.IntStream.range(0, 100).mapToObj(index -> simple(index)).toList();
        BackgroundAgentRuntime runtime = new BackgroundAgentRuntime(
                BackgroundCatalog.of(1L, definitions, List.of(), List.of(), List.of()), 600);
        assertEquals(100, runtime.states().size());
        assertEquals(0, runtime.states().stream().filter(value -> value.materialization().name()
                .equals("MATERIALIZED")).count());
    }

    @Test
    void appearanceSelectionDoesNotChangeWithDefinitionRevision() {
        DeterministicAppearanceResolver resolver = new DeterministicAppearanceResolver();
        BackgroundCatalog first = catalog(1L, "room");
        BackgroundCatalog reloaded = catalog(2L, "room");
        assertEquals(resolver.resolve(first.definitions().get(mira), first),
                resolver.resolve(reloaded.definitions().get(mira), reloaded));
    }

    private BackgroundCatalog catalog(long revision, String tenOclockTarget) {
        var pool = new BackgroundAppearancePool(new BackgroundAppearancePoolId("jdh:students"),
                List.of(new BackgroundAppearanceId("jdh:a"), new BackgroundAppearanceId("jdh:b")), revision);
        var dialogue = new AmbientDialogue(new AmbientDialogueId("jdh:talk"),
                List.of(new AmbientLine("mira", "Привет", Optional.empty(), source)), revision);
        var miraDefinition = definition(mira, revision, tenOclockTarget);
        var antonDefinition = new BackgroundNpcDefinition(anton, new BackgroundNpcDefinitionId(anton.value()),
                location("anton_home"), Optional.empty(), Optional.of(new BackgroundAppearanceId("jdh:a")),
                Optional.empty(), Optional.empty(), List.of(), source, revision);
        return BackgroundCatalog.of(revision, List.of(miraDefinition, antonDefinition), List.of(), List.of(pool),
                List.of(dialogue));
    }

    private BackgroundNpcDefinition definition(BackgroundNpcId id, long revision, String target) {
        return new BackgroundNpcDefinition(id, new BackgroundNpcDefinitionId(id.value()), location("home"),
                Optional.empty(), Optional.empty(), Optional.of(new BackgroundAppearancePoolId("jdh:students")),
                Optional.empty(), List.of(
                        entry(540, new BackgroundTask.Travel(location("university"))),
                        entry(590, new BackgroundTask.Converse(anton, new AmbientDialogueId("jdh:talk"),
                                Optional.empty())),
                        entry(600, new BackgroundTask.Travel(location(target))),
                        entry(690, new BackgroundTask.Travel(location("cafeteria")))), source, revision);
    }
    private BackgroundScheduleEntry entry(int minute, BackgroundTask task) {
        return new BackgroundScheduleEntry(minute, task, "echo_background_npc/jdh:mira/" + minute, source);
    }
    private BackgroundNpcDefinition simple(int index) {
        BackgroundNpcId id = new BackgroundNpcId("jdh:agent_" + index);
        return new BackgroundNpcDefinition(id, new BackgroundNpcDefinitionId(id.value()), location("home"),
                Optional.empty(), Optional.of(new BackgroundAppearanceId("jdh:a")), Optional.empty(),
                Optional.empty(), List.of(), source, 1L);
    }
    private BackgroundLocationId location(String value) { return new BackgroundLocationId("jdh:" + value); }
}
