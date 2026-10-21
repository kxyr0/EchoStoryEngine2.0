package io.github.kxyr0.echocore.client;

import io.github.kxyr0.echocore.api.ChapterBackend;
import io.github.kxyr0.echocore.api.ChapterDefinition;
import io.github.kxyr0.echocore.api.ChapterResult;
import io.github.kxyr0.echocore.api.ChapterResult.Code;
import io.github.kxyr0.echocore.api.ChapterSnapshot;
import io.github.kxyr0.echocore.api.ChapterSnapshot.State;
import io.github.kxyr0.echocore.core.ChapterFailure;
import io.github.kxyr0.echocore.core.ChapterSaveStore;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChapterClientRuntime implements ChapterBackend {
    private static final Logger LOG = LoggerFactory.getLogger("echo-core");
    private final Map<String, ChapterDefinition> chapters;
    private final ChapterSaveStore store;
    private final AtomicBoolean busy = new AtomicBoolean();
    private final Map<String, ChapterResult> results = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<ChapterResult>> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "echo-core-chapters"); thread.setDaemon(true); return thread;
    });
    private volatile String active;
    private String loading;
    private Screen origin;
    private long loadRequested;

    public ChapterClientRuntime(Map<String, ChapterDefinition> chapters, ChapterSaveStore store) {
        this.chapters = Map.copyOf(chapters); this.store = store;
    }
    @Override public Set<String> chapters() { return chapters.keySet(); }
    @Override public ChapterSnapshot snapshot(String id) {
        ChapterDefinition chapter = chapters.get(id);
        ChapterResult last = results.getOrDefault(id, new ChapterResult(id, Code.NONE, ""));
        if (chapter == null) return new ChapterSnapshot(id, State.UNKNOWN_CHAPTER, false, false, "", last);
        State state = id.equals(active) ? State.ACTIVE : busy.get() ? State.BUSY : store.state(chapter);
        return new ChapterSnapshot(id, state, store.exists(chapter), store.templateExists(chapter), chapter.saveId(), last);
    }
    @Override public CompletableFuture<ChapterResult> start(String id) { return submit(id, "start"); }
    @Override public CompletableFuture<ChapterResult> continueChapter(String id) { return submit(id, "continue"); }
    @Override public CompletableFuture<ChapterResult> restart(String id) { return submit(id, "restart"); }
    @Override public AutoCloseable onResult(Consumer<ChapterResult> listener) {
        listeners.add(listener); return () -> listeners.remove(listener);
    }

    private CompletableFuture<ChapterResult> submit(String id, String action) {
        CompletableFuture<ChapterResult> answer = new CompletableFuture<>();
        Minecraft.getInstance().execute(() -> begin(id, action, answer));
        return answer;
    }
    private void begin(String id, String action, CompletableFuture<ChapterResult> answer) {
        ChapterDefinition chapter = chapters.get(id);
        Minecraft mc = Minecraft.getInstance();
        if (chapter == null) { answer.complete(publish(id, Code.UNKNOWN_CHAPTER, "Unknown chapter: " + id)); return; }
        if (busy.get()) { answer.complete(publish(id, Code.BUSY, "A chapter operation is already running.")); return; }
        if (mc.level != null || mc.getSingleplayerServer() != null || mc.getConnection() != null) {
            answer.complete(publish(id, Code.WORLD_OPEN, "Exit the current world before starting or resetting a chapter.")); return;
        }
        busy.set(true);
        CompletableFuture.runAsync(() -> {
            try {
                if (!action.equals("continue")) store.create(chapter, action.equals("restart"));
                store.continueChapter(chapter);
            } catch (Exception failure) { throw new java.util.concurrent.CompletionException(failure); }
        }, worker).whenComplete((unused, failure) -> mc.execute(() -> {
            if (failure != null) {
                busy.set(false);
                Throwable cause = failure.getCause() == null ? failure : failure.getCause();
                Code code = cause instanceof ChapterFailure typed ? typed.code() : Code.IO_ERROR;
                answer.complete(publish(id, code, cause.getMessage()));
                return;
            }
            if (mc.level != null || mc.getSingleplayerServer() != null || mc.getConnection() != null) {
                busy.set(false);
                answer.complete(publish(id, Code.WORLD_OPEN, "A world was opened while the chapter operation was running.")); return;
            }
            try {
                origin = mc.screen; loading = id; loadRequested = System.nanoTime();
                mc.createWorldOpenFlows().loadLevel(origin, chapter.saveId());
                answer.complete(publish(id, Code.LOAD_REQUESTED, chapter.saveId()));
            } catch (Exception error) {
                loading = null; busy.set(false);
                answer.complete(publish(id, Code.IO_ERROR, error.getMessage()));
            }
        }));
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (loading != null && mc.getSingleplayerServer() != null && mc.player != null) {
            var path = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            if (path.equals(store.save(chapters.get(loading)))) {
                active = loading; loading = null; busy.set(false); publish(active, Code.ACTIVE, "Chapter world loaded.");
            }
        } else if (loading != null && mc.level == null && mc.getSingleplayerServer() == null
                && mc.screen == origin && System.nanoTime() - loadRequested > 500_000_000L) {
            String id = loading; loading = null; busy.set(false); publish(id, Code.LOAD_CANCELLED, "World loading was cancelled or failed.");
        }
        if (active != null && mc.level == null && mc.getSingleplayerServer() == null) active = null;
    }
    private ChapterResult publish(String id, Code code, String detail) {
        ChapterResult result = new ChapterResult(id, code, detail == null ? "" : detail);
        results.put(id, result);
        LOG.info("Chapter {}: {} {}", id, code, result.detail());
        for (var listener : listeners) {
            try { listener.accept(result); }
            catch (RuntimeException failure) { LOG.warn("Chapter result listener failed", failure); }
        }
        return result;
    }
}
