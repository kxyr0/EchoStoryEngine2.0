package io.github.kxyr0.echoquests.core.catalog;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class QuestCatalogHost {
    private final AtomicReference<QuestCatalog> current = new AtomicReference<>(QuestCatalog.empty());

    public QuestCatalog current() { return current.get(); }
    public void replace(QuestCatalog catalog) { current.set(Objects.requireNonNull(catalog, "catalog")); }
}
