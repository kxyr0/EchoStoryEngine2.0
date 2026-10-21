package io.github.kxyr0.echocore.api;

import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public interface ChapterBackend {
    Set<String> chapters();
    ChapterSnapshot snapshot(String chapter);
    CompletionStage<ChapterResult> start(String chapter);
    CompletionStage<ChapterResult> continueChapter(String chapter);
    CompletionStage<ChapterResult> restart(String chapter);
    AutoCloseable onResult(Consumer<ChapterResult> listener);
}
