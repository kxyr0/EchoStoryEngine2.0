package io.github.kxyr0.echostoryengine.story.forge.extension.catalog;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.forge.extension.readmodel.ExtensionDetail;
import io.github.kxyr0.echostoryengine.story.forge.extension.readmodel.ExtensionSummary;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable server-scoped extension catalog.
 */
public final class ExtensionCatalog {
    private final Map<NamespacedId, ExtensionActionBinding> actions;
    private final Map<NamespacedId, RegisteredExtension> extensions;

    public ExtensionCatalog(
            Map<NamespacedId, ExtensionActionBinding> actions,
            Map<NamespacedId, RegisteredExtension> extensions) {
        this.actions = Map.copyOf(Objects.requireNonNull(actions, "actions"));
        this.extensions = Map.copyOf(Objects.requireNonNull(extensions, "extensions"));
    }

    public static ExtensionCatalog empty() {
        return new ExtensionCatalog(Map.of(), Map.of());
    }

    public Optional<ExtensionActionBinding> find(NamespacedId actionType) {
        return Optional.ofNullable(actions.get(Objects.requireNonNull(actionType, "actionType")));
    }

    public List<ExtensionSummary> summaries() {
        return extensions.values().stream()
                .map(registered -> registered.detail().summary())
                .sorted(Comparator.comparing(ExtensionSummary::extensionId))
                .toList();
    }

    public Optional<ExtensionDetail> detail(NamespacedId extensionId) {
        return Optional.ofNullable(extensions.get(Objects.requireNonNull(extensionId, "extensionId")))
                .map(RegisteredExtension::detail);
    }

    public int activeActionCount() {
        return actions.size();
    }

    public void close() {
        extensions.values().forEach(registered -> registered.session().ifPresent(StoryExtensionSessionCloser::close));
    }

    private static final class StoryExtensionSessionCloser {
        private StoryExtensionSessionCloser() {
        }

        static void close(io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession session) {
            session.close();
        }
    }
}
