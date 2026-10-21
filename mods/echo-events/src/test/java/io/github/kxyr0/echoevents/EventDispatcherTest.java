package io.github.kxyr0.echoevents;

import static org.junit.jupiter.api.Assertions.*;

import io.github.kxyr0.echoevents.api.catalog.EventKind;
import io.github.kxyr0.echoevents.api.context.EventAudience;
import io.github.kxyr0.echoevents.api.context.EventSource;
import io.github.kxyr0.echoevents.api.delivery.EventDelivery;
import io.github.kxyr0.echoevents.api.delivery.EventReactionPort.Acceptance;
import io.github.kxyr0.echoevents.api.event.EventEnvelope;
import io.github.kxyr0.echoevents.api.event.EventMutationPolicy;
import io.github.kxyr0.echoevents.api.identity.EventIds.*;
import io.github.kxyr0.echoevents.api.payload.DialogueFact;
import io.github.kxyr0.echoevents.api.subscription.EventFilter;
import io.github.kxyr0.echoevents.api.subscription.EventSubscription;
import io.github.kxyr0.echoevents.core.journal.EventJournal;
import io.github.kxyr0.echoevents.core.journal.EventJournalImage;
import io.github.kxyr0.echoevents.core.journal.EventJournalStore;
import io.github.kxyr0.echoevents.core.runtime.EventDispatcher;
import io.github.kxyr0.echoevents.persistence.FileEventJournalStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class EventDispatcherTest {
    @TempDir Path directory;
    private final UUID alice = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID bob = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test void durablePublicationSurvivesRestartAndDeduplicatesEverySubscription() {
        var store = new FileEventJournalStore(directory.resolve("journal"));
        List<String> actions = new ArrayList<>();
        var before = runtime(store, delivery -> { fail("Publish must enqueue only"); return Acceptance.REJECTED; });
        before.replaceSubscriptions(List.of(subscription("b", false), subscription("a", false)));
        var event = completed("execution-1", alice);
        assertTrue(before.publish(event, EventMutationPolicy.NORMAL));
        assertEquals(2, store.load().pending().size());
        var after = runtime(store, delivery -> { actions.add(delivery.subscription().id().value()); return Acceptance.ACCEPTED; });
        assertEquals(2, after.tick(1, 128));
        assertEquals(List.of("a", "b"), actions);
        assertFalse(after.publish(event, EventMutationPolicy.NORMAL));
        assertEquals(0, runtime(store, delivery -> { fail("Already accepted"); return Acceptance.REJECTED; }).tick(2, 128));
        System.out.println("Crash recovery occurrence=" + event.id().value() + "; receipts=" + store.load().deliveries().size());
    }

    @Test void crashAfterActionAcceptanceUsesTheSameDurableExecutionToken() throws IOException {
        Path journalPath = directory.resolve("journal");
        Path tokens = directory.resolve("action-receipts");
        var disk = new FileEventJournalStore(journalPath);
        var failing = new MemoryStore();
        failing.delegate = disk;
        var before = runtime(failing, delivery -> {
            try { Files.writeString(tokens, delivery.executionToken()); }
            catch (IOException error) { throw new IllegalStateException(error); }
            failing.failNext = true;
            return Acceptance.ACCEPTED;
        });
        before.replaceSubscriptions(List.of(subscription("reaction", false)));
        before.publish(completed("execution-1", alice), EventMutationPolicy.NORMAL);
        assertThrows(IllegalStateException.class, () -> before.tick(1, 128));
        String token = Files.readString(tokens);
        AtomicInteger duplicateActions = new AtomicInteger();
        var after = runtime(disk, delivery -> {
            if (!delivery.executionToken().equals(token)) duplicateActions.incrementAndGet();
            return Acceptance.ACCEPTED;
        });
        after.tick(2, 128);
        assertEquals(0, duplicateActions.get());
        assertTrue(disk.load().pending().isEmpty());
        System.out.println("Accepted action receipt=" + token + "; duplicate actions=0 (idempotent port)");
    }

    @Test void independentReceiptsFailureIsolationAndBoundedRetry() {
        var store = new MemoryStore();
        AtomicInteger accepted = new AtomicInteger();
        var runtime = runtime(store, delivery -> {
            if (delivery.subscription().id().value().equals("a")) throw new IllegalStateException("Recoverable");
            accepted.incrementAndGet();
            return Acceptance.ACCEPTED;
        });
        runtime.replaceSubscriptions(List.of(subscription("a", false), subscription("b", false)));
        runtime.publish(completed("execution-1", alice), EventMutationPolicy.NORMAL);
        runtime.tick(0, 128);
        assertEquals(1, accepted.get());
        assertEquals(1, store.load().pending().size());
        for (long tick : List.of(20L, 60L, 140L, 300L)) runtime.tick(tick, 128);
        assertTrue(store.load().pending().isEmpty());
        assertEquals(1, store.load().deliveries().values().stream().filter(d -> d.status() == EventDelivery.Status.FAILED).count());
        assertEquals(1, accepted.get());
    }

    @Test void indexMatchesOnlyRelevantResourceAmongFiveThousandSubscriptions() {
        var runtime = runtime(new MemoryStore(), delivery -> Acceptance.ACCEPTED);
        List<EventSubscription> subscriptions = new ArrayList<>();
        for (int index = 0; index < 5000; index++) subscriptions.add(new EventSubscription(new EventSubscriptionId("sub-" + index),
                new EventFilter(EventKind.DIALOGUE_COMPLETED, new ResourceId("jdh:dialogue_" + index), Optional.empty()), "r1", false));
        runtime.replaceSubscriptions(subscriptions);
        var source = source("execution-1", alice, "jdh:dialogue_194");
        var event = EventEnvelope.occurrence(source, DialogueFact.lifecycle(DialogueFact.Phase.COMPLETED));
        assertEquals(1, runtime.index().candidates(event).size());
        long started = System.nanoTime();
        runtime.publish(event, EventMutationPolicy.NORMAL);
        assertEquals(1, runtime.tick(1, 128));
        System.out.println("subscriptions=5000; candidates=1; publish+dispatch ns=" + (System.nanoTime() - started));
    }

    @Test void hotReloadKeepsPinnedEligibilityWithoutReplayingHistory() {
        var store = new MemoryStore();
        List<String> revisions = new ArrayList<>();
        var runtime = runtime(store, delivery -> { revisions.add(delivery.subscription().revision()); return Acceptance.ACCEPTED; });
        var old = subscription("a", false);
        runtime.replaceSubscriptions(List.of(old));
        runtime.publish(completed("execution-1", alice), EventMutationPolicy.NORMAL);
        var replacement = new EventSubscription(old.id(), new EventFilter(EventKind.DIALOGUE_COMPLETED,
                new ResourceId("jdh:other"), Optional.empty()), "r2", false);
        runtime.replaceSubscriptions(List.of(replacement));
        runtime.tick(1, 128);
        assertEquals(List.of("r1"), revisions);
        runtime.publish(completed("execution-2", alice), EventMutationPolicy.NORMAL);
        assertEquals(0, runtime.tick(2, 128));
        assertThrows(IllegalArgumentException.class, () -> runtime.replaceSubscriptions(List.of(old, old)));
        assertEquals("r2", runtime.index().subscriptions().get(0).revision());
    }

    @Test void onceIsPerPlayerAndSurvivesReloadAndRestart() {
        var store = new MemoryStore();
        List<UUID> players = new ArrayList<>();
        var runtime = runtime(store, delivery -> {
            players.add(((EventAudience.Player) delivery.event().source().audience()).playerId());
            return Acceptance.ACCEPTED;
        });
        runtime.replaceSubscriptions(List.of(subscription("a", true)));
        runtime.publish(completed("execution-1", alice), EventMutationPolicy.NORMAL);
        runtime.publish(completed("execution-2", alice), EventMutationPolicy.NORMAL);
        runtime.publish(completed("execution-3", bob), EventMutationPolicy.NORMAL);
        runtime.tick(1, 128);
        assertEquals(Set.of(alice, bob), new HashSet<>(players));
        assertEquals(2, players.size());
        var after = runtime(store, delivery -> { fail("Once replay"); return Acceptance.REJECTED; });
        after.replaceSubscriptions(List.of(subscription("a", true)));
        after.publish(completed("execution-4", alice), EventMutationPolicy.NORMAL);
        after.tick(2, 128);
    }

    @Test void hydrationMigrationAndDevSetupDoNotPublishAndTransientHistoryIsBounded() {
        var store = new MemoryStore();
        var runtime = runtime(store, delivery -> Acceptance.ACCEPTED);
        for (var policy : List.of(EventMutationPolicy.RESTORE, EventMutationPolicy.MIGRATION, EventMutationPolicy.DEV_SETUP)) {
            assertFalse(runtime.publish(completed("execution-1", alice), policy));
        }
        assertTrue(store.load().occurrences().isEmpty());
        for (int index = 0; index < 600; index++) {
            runtime.publish(EventEnvelope.occurrence(source("execution-" + index, alice, "jdh:father"),
                    DialogueFact.lifecycle(DialogueFact.Phase.STARTED)), EventMutationPolicy.NORMAL);
        }
        assertEquals(16, runtime.history().size());
        assertTrue(store.load().occurrences().isEmpty());
    }

    @Test void cancelledAndFailedNeverMatchCompleted() {
        var runtime = runtime(new MemoryStore(), delivery -> { fail("Wrong terminal kind"); return Acceptance.REJECTED; });
        runtime.replaceSubscriptions(List.of(subscription("a", false)));
        runtime.publish(EventEnvelope.occurrence(source("execution-1", alice, "jdh:father"),
                DialogueFact.lifecycle(DialogueFact.Phase.CANCELLED)), EventMutationPolicy.NORMAL);
        runtime.publish(EventEnvelope.occurrence(source("execution-2", alice, "jdh:father"),
                DialogueFact.failed(new SymbolId("CONTENT_ERROR"))), EventMutationPolicy.NORMAL);
        assertEquals(0, runtime.tick(1, 128));
    }

    @Test void budgetStagesWorkAndCallbacksCannotReenterDispatcher() {
        var store = new MemoryStore();
        EventDispatcher[] holder = new EventDispatcher[1];
        holder[0] = runtime(store, delivery -> {
            assertThrows(IllegalStateException.class, () -> holder[0].tick(0, 128));
            return Acceptance.ACCEPTED;
        });
        holder[0].replaceSubscriptions(List.of(subscription("a", false)));
        for (int index = 0; index < 20; index++) holder[0].publish(completed("execution-" + index, alice), EventMutationPolicy.NORMAL);
        assertEquals(3, holder[0].tick(0, 3));
        assertEquals(17, holder[0].pendingCount());
        assertEquals(17, holder[0].tick(1, 128));
    }

    @Test void causalLoopStopsWithinOneRootAcrossRestart() {
        var store = new FileEventJournalStore(directory.resolve("causal-journal"));
        AtomicInteger actions = new AtomicInteger();
        var runtime = runtime(store, delivery -> { actions.incrementAndGet(); return Acceptance.ACCEPTED; });
        runtime.replaceSubscriptions(List.of(subscription("a", false)));
        for (int index = 0; index < 40; index++) {
            var source = source("execution-" + index, alice, "jdh:father");
            source = new EventSource(source.resource(), source.execution(), new ExecutionId("root"), source.transition(),
                    source.audience(), source.module(), source.gameTick(), source.echoTimeMicros(), source.definitionRevision(), source.origin());
            runtime.publish(EventEnvelope.occurrence(source, DialogueFact.lifecycle(DialogueFact.Phase.COMPLETED)), EventMutationPolicy.NORMAL);
        }
        runtime.tick(1, 128);
        assertEquals(32, actions.get());
        assertEquals(8, store.load().deliveries().values().stream().filter(d -> d.diagnostic().equals("ECHO_EVENT_CAUSAL_LIMIT")).count());
        var restarted = runtime(new FileEventJournalStore(directory.resolve("causal-journal")), delivery -> {
            actions.incrementAndGet();
            return Acceptance.ACCEPTED;
        });
        restarted.replaceSubscriptions(List.of(subscription("a", false)));
        var original = source("execution-after-restart", alice, "jdh:father");
        var continued = new EventSource(original.resource(), original.execution(), new ExecutionId("root"),
                original.transition(), original.audience(), original.module(), 1, original.echoTimeMicros(),
                original.definitionRevision(), original.origin());
        restarted.publish(EventEnvelope.occurrence(continued, DialogueFact.lifecycle(DialogueFact.Phase.COMPLETED)),
                EventMutationPolicy.NORMAL);
        restarted.tick(2, 128);
        assertEquals(32, actions.get());
    }

    @Test void corruptedOrUnknownSchemaNeverResetsJournalSilently() throws IOException {
        Path path = directory.resolve("journal");
        var store = new FileEventJournalStore(path);
        store.commit(EventJournalImage.empty());
        byte[] bytes = Files.readAllBytes(path);
        bytes[bytes.length - 1] ^= 1;
        Files.write(path, bytes);
        assertThrows(RuntimeException.class, store::load);
        assertThrows(IllegalArgumentException.class, () -> new EventJournalImage(2, Map.of(), Map.of(), Set.of(), Map.of()));
    }

    @Test void sourceOutboxPinsEligibilityBeforeJournalAppendAndReload() {
        var store = new MemoryStore();
        var original = runtime(store, delivery -> Acceptance.ACCEPTED);
        original.replaceSubscriptions(List.of(subscription("old-a", false), subscription("old-b", false)));
        var codec = new io.github.kxyr0.echoevents.persistence.codec.EventOutboxCodec();
        String sourceCheckpoint = codec.encode(List.of(original.prepare(completed("source-commit", alice))));
        assertTrue(store.load().occurrences().isEmpty());
        List<String> accepted = new ArrayList<>();
        var restarted = runtime(store, delivery -> {
            accepted.add(delivery.subscription().id().value());
            return Acceptance.ACCEPTED;
        });
        restarted.replaceSubscriptions(List.of(subscription("new-only", false)));
        codec.decode(sourceCheckpoint).forEach(value -> restarted.publish(value, EventMutationPolicy.NORMAL));
        restarted.tick(1, 128);
        assertEquals(List.of("old-a", "old-b"), accepted);
        codec.decode(sourceCheckpoint).forEach(value -> restarted.publish(value, EventMutationPolicy.NORMAL));
        assertEquals(0, restarted.tick(2, 128));
    }

    private EventDispatcher runtime(EventJournalStore store, io.github.kxyr0.echoevents.api.delivery.EventReactionPort actions) {
        return new EventDispatcher(new EventJournal(store), actions, 16, 64);
    }
    private EventSubscription subscription(String id, boolean once) {
        return new EventSubscription(new EventSubscriptionId(id), new EventFilter(EventKind.DIALOGUE_COMPLETED,
                new ResourceId("jdh:father"), Optional.empty()), "r1", once);
    }
    private EventEnvelope<DialogueFact> completed(String execution, UUID player) {
        return EventEnvelope.occurrence(source(execution, player, "jdh:father"), DialogueFact.lifecycle(DialogueFact.Phase.COMPLETED));
    }
    private EventSource source(String execution, UUID player, String resource) {
        return new EventSource(new ResourceId(resource), new ExecutionId(execution), new ExecutionId(execution),
                new TransitionId("terminal"), new EventAudience.Player(player), "echo_dialogue", 0,
                OptionalLong.empty(), "revision-1", Optional.empty());
    }
    private static final class MemoryStore implements EventJournalStore {
        private EventJournalImage image = EventJournalImage.empty();
        private EventJournalStore delegate;
        private boolean failNext;
        public EventJournalImage load() { return delegate == null ? image : delegate.load(); }
        public void commit(EventJournalImage next) {
            if (failNext) { failNext = false; throw new IllegalStateException("Simulated crash"); }
            if (delegate != null) delegate.commit(next);
            image = next;
        }
    }
}
