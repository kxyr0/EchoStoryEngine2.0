package io.github.kxyr0.echocore.fancymenu;

import io.github.kxyr0.echocore.api.ChapterApi;
import io.github.kxyr0.echocore.api.ChapterBackend;
import io.github.kxyr0.echocore.api.ChapterResult;
import io.github.kxyr0.echocore.api.ChapterResult.Code;
import io.github.kxyr0.echocore.api.ChapterSnapshot;
import io.github.kxyr0.echocore.api.ChapterSnapshot.State;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FancyMenuBridgeTest {
    private static final FakeBackend BACKEND = new FakeBackend();
    @BeforeAll static void install() { ChapterApi.install(BACKEND); }

    @Test void placeholdersWorkWithInstalledFancyMenuAndExposeNoSaveResult() {
        for (String field : new String[] {"state", "save_exists", "template_exists", "can_continue", "last_result", "last_error"}) {
            var placeholder = new ChapterPlaceholder(field);
            var input = placeholder.getDefaultPlaceholderString();
            assertEquals("chapter_1", input.values.get("chapter"));
            assertTrue(input.toString().contains("chapter_1"));
            String expected = switch (field) {
                case "state" -> "NO_SAVE"; case "template_exists" -> "true";
                case "last_result" -> "NO_SAVE"; case "last_error" -> "No save exists for chapter_1"; default -> "false";
            };
            assertEquals(expected, placeholder.getReplacementFor(input));
            assertFalse(placeholder.canRunAsync());
        }
    }

    @Test void editorActionsDelegateToGenericBackendForAnyChapter() {
        for (String operation : new String[] {"start", "continue", "restart"}) {
            var action = new ChapterAction(operation);
            assertEquals("echo_core_chapter_" + operation, action.getIdentifier());
            action.execute("chapter_3");
            assertEquals(operation + ":chapter_3", BACKEND.called);
            action.execute("");
            assertEquals(operation + ":chapter_1", BACKEND.called);
            assertFalse(action.canRunAsync());
        }
    }

    private static final class FakeBackend implements ChapterBackend {
        private String called;
        @Override public Set<String> chapters() { return Set.of("chapter_1", "chapter_3"); }
        @Override public ChapterSnapshot snapshot(String id) {
            return new ChapterSnapshot(id, State.NO_SAVE, false, true, "echo_core_" + id,
                    new ChapterResult(id, Code.NO_SAVE, "No save exists for " + id));
        }
        private CompletableFuture<ChapterResult> call(String action, String id) {
            called = action + ":" + id;
            return CompletableFuture.completedFuture(new ChapterResult(id, Code.LOAD_REQUESTED, ""));
        }
        @Override public CompletableFuture<ChapterResult> start(String id) { return call("start", id); }
        @Override public CompletableFuture<ChapterResult> continueChapter(String id) { return call("continue", id); }
        @Override public CompletableFuture<ChapterResult> restart(String id) { return call("restart", id); }
        @Override public AutoCloseable onResult(Consumer<ChapterResult> listener) { return () -> { }; }
    }
}
