package io.github.kxyr0.echostoryengine.story.forge.diagnostic;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.forge.extension.readmodel.ExtensionDetail;
import io.github.kxyr0.echostoryengine.story.forge.extension.readmodel.ExtensionSummary;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.CompiledStoryCatalogSnapshot;
import io.github.kxyr0.echostoryengine.story.forge.host.readmodel.ExecutionSummary;
import io.github.kxyr0.echostoryengine.story.forge.host.readmodel.HostHealth;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Форматирует operator-facing diagnostics без raw source, paths и stack traces.
 */
public final class ForgeDiagnosticFormatter {
    private final StoryDiagnosticDisplay display = new StoryDiagnosticDisplay();

    public List<String> health(
            HostHealth health,
            CompiledStoryCatalogSnapshot catalog,
            int recentWarningOrErrorCount) {
        return List.of(
                "Состояние Echo Story Engine",
                "Состояние хоста: " + health.state(),
                "Каталог: статус=" + health.latestReload().status() + " ревизия=" + catalog.revision(),
                "Скомпилированных историй: " + health.catalogStories(),
                "Активных выполнений: " + health.activeExecutions(),
                "Ожидающих выполнений: " + health.waitingExecutions(),
                "Очередь сохранения: " + usage(health.pendingPersistenceOperations(),
                        health.persistenceQueueCapacity()),
                "Очередь работ серверного потока: " + usage(
                        health.pendingServerWork(),
                        health.serverWorkQueueCapacity()),
                "Последний reload: источников=" + health.latestReload().sourceCount()
                        + " ошибок=" + health.latestReload().errorCount()
                        + " время=" + health.latestReload().occurredAt(),
                "Недавних предупреждений/ошибок: " + recentWarningOrErrorCount);
    }

    public List<String> stories(CompiledStoryCatalogSnapshot catalog, int limit) {
        List<String> stories = catalog.stories().entrySet().stream()
                .sorted((left, right) -> left.getKey().asString().compareTo(right.getKey().asString()))
                .limit(limit)
                .map(entry -> entry.getKey().asString()
                        + " отпечаток=" + shorten(entry.getValue().fingerprint().value())
                        + " источник=" + entry.getValue().sourceId())
                .toList();
        if (stories.isEmpty()) {
            return List.of("Story в каталоге нет.");
        }
        return prepend("Story в каталоге: " + stories.size(), stories);
    }

    public List<String> executions(List<ExecutionSummary> summaries, int limit) {
        List<String> executions = summaries.stream()
                .sorted(Comparator.comparing(summary -> summary.executionId().asString()))
                .limit(limit)
                .map(this::formatExecution)
                .toList();
        if (executions.isEmpty()) {
            return List.of("Выполнений в модели чтения нет.");
        }
        return prepend("Выполнений в модели чтения: " + executions.size(), executions);
    }

    public List<String> execution(ExecutionId executionId, Optional<ExecutionSummary> summary) {
        return summary
                .map(value -> List.of("Выполнение " + executionId.asString(), formatExecution(value)))
                .orElseGet(() -> List.of("Выполнение " + executionId.asString() + " не найдено."));
    }

    public List<String> extensions(List<ExtensionSummary> summaries, int limit) {
        List<String> lines = summaries.stream()
                .sorted(Comparator.comparing(ExtensionSummary::extensionId))
                .limit(limit)
                .map(summary -> summary.extensionId()
                        + " name=" + summary.displayName()
                        + " version=" + summary.implementationVersion()
                        + " api=" + summary.requiredApiVersion()
                        + " status=" + summary.status()
                        + " actions=" + summary.actionCount()
                        + " warnError=" + summary.warningOrErrorCount())
                .toList();
        if (lines.isEmpty()) {
            return List.of("Story extensions не зарегистрированы.");
        }
        return prepend("Story extensions: " + lines.size(), lines);
    }

    public List<String> extension(NamespacedId extensionId, Optional<ExtensionDetail> detail) {
        if (detail.isEmpty()) {
            return List.of("Story extension " + extensionId.asString() + " не найдена.");
        }
        ExtensionDetail value = detail.orElseThrow();
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        lines.add("Story extension " + value.summary().extensionId());
        lines.add("name=" + value.summary().displayName()
                + " version=" + value.summary().implementationVersion()
                + " api=" + value.summary().requiredApiVersion()
                + " status=" + value.summary().status());
        if (!value.rejectionReason().isBlank()) {
            lines.add("rejection=" + value.rejectionReason());
        }
        lines.addAll(value.actionLines());
        return List.copyOf(lines);
    }

    public List<String> diagnostics(List<StoryDiagnostic> diagnostics, int limit) {
        List<StoryDiagnostic> selected = diagnostics.stream()
                .sorted(Comparator.comparing(StoryDiagnostic::occurredAt).reversed())
                .limit(limit)
                .toList();
        if (selected.isEmpty()) {
            return List.of("Диагностик нет.");
        }
        return selected.stream().map(this::formatDiagnostic).toList();
    }

    public String formatDiagnostic(StoryDiagnostic diagnostic) {
        return display.command(diagnostic);
    }

    private String formatExecution(ExecutionSummary summary) {
        return summary.executionId().asString()
                + " история=" + summary.storyId().asString()
                + " фаза=" + summary.phase()
                + " узел=" + summary.currentNode()
                + pending(summary.pendingActionType());
    }

    private String pending(String pendingActionType) {
        return pendingActionType.isBlank() ? "" : " типОжидающегоДействия=" + pendingActionType;
    }

    private String usage(int used, int capacity) {
        if (capacity == 0) {
            return used + "/" + capacity + " (отдельная очередь не используется)";
        }
        return used + "/" + capacity;
    }

    private String shorten(String value) {
        return value.substring(0, Math.min(12, value.length()));
    }

    private List<String> prepend(String header, List<String> lines) {
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        result.add(header);
        result.addAll(lines);
        return List.copyOf(result);
    }
}
