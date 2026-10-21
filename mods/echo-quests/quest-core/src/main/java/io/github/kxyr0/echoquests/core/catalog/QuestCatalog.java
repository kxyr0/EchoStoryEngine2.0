package io.github.kxyr0.echoquests.core.catalog;

import io.github.kxyr0.echoquests.api.identity.QuestId;
import io.github.kxyr0.echoquests.api.model.QuestDefinition;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class QuestCatalog {
    private final Map<QuestId, QuestDefinition> definitions;

    private QuestCatalog(Map<QuestId, QuestDefinition> definitions) {
        this.definitions = Map.copyOf(definitions);
    }

    public static QuestCatalog empty() { return new QuestCatalog(Map.of()); }

    public static QuestCatalog of(Collection<QuestDefinition> values) {
        Map<QuestId, QuestDefinition> indexed = new LinkedHashMap<>();
        for (QuestDefinition definition : values) {
            if (indexed.putIfAbsent(definition.id(), definition) != null) {
                throw new IllegalArgumentException("Duplicate quest ID: " + definition.id());
            }
            QuestDefinitionValidator.validate(definition);
        }
        return new QuestCatalog(indexed);
    }

    public Optional<QuestDefinition> find(QuestId id) { return Optional.ofNullable(definitions.get(id)); }
    public Collection<QuestDefinition> definitions() { return definitions.values(); }
    public int size() { return definitions.size(); }
}
