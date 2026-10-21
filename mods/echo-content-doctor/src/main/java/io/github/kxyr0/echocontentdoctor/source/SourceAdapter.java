package io.github.kxyr0.echocontentdoctor.source;

import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echosavemigration.definition.MigrationDefinition;
import java.nio.file.Path;
import java.util.List;

public interface SourceAdapter {
    boolean accepts(Path path);

    AdaptedSource adapt(String sourceId, String sourceText);

    record AdaptedSource(List<ContentDescriptor> descriptors, List<ContentSourceProblem> problems,
                         List<MigrationDefinition> migrations) {
        public AdaptedSource(List<ContentDescriptor> descriptors, List<ContentSourceProblem> problems) {
            this(descriptors, problems, List.of());
        }

        public AdaptedSource {
            descriptors = List.copyOf(descriptors);
            problems = List.copyOf(problems);
            migrations = List.copyOf(migrations);
        }
    }
}
