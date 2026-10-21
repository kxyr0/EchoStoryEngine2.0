package io.github.kxyr0.echocontentdoctor.engine;

import io.github.kxyr0.echocontentdoctor.graph.ContentCatalogSnapshot;
import io.github.kxyr0.echosavemigration.api.SaveSchemaVersion;
import io.github.kxyr0.echosavemigration.definition.MigrationDefinition;
import java.util.List;

public record ContentDoctorInput(
        ContentCatalogSnapshot catalog,
        SaveSchemaVersion minimumSupportedSchema,
        SaveSchemaVersion currentSchema,
        List<MigrationDefinition> migrations
) {
    public ContentDoctorInput {
        migrations = List.copyOf(migrations);
    }

    public static ContentDoctorInput catalogOnly(ContentCatalogSnapshot catalog) {
        SaveSchemaVersion zero = new SaveSchemaVersion(0);
        return new ContentDoctorInput(catalog, zero, zero, List.of());
    }
}
