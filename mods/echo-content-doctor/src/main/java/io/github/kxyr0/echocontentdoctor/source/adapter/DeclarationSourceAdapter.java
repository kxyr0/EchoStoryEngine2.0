package io.github.kxyr0.echocontentdoctor.source.adapter;

import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echocontentdoctor.source.SourceAdapter;
import io.github.kxyr0.echocontentdoctor.source.SourceIdentities;
import io.github.kxyr0.echocontentdoctor.source.SourceSpans;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

public final class DeclarationSourceAdapter implements SourceAdapter {
    private static final Pattern ID = Pattern.compile("\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    @Override
    public boolean accepts(Path path) {
        String value = path.toString().replace('\\', '/');
        return value.endsWith(".json") && (value.contains("/echo_scene/locations/")
                || value.contains("/echo_scene/cameras/"));
    }

    @Override
    public AdaptedSource adapt(String sourceId, String sourceText) {
        var match = ID.matcher(sourceText);
        ContentResourceId id = match.find() ? new ContentResourceId(match.group(1))
                : SourceIdentities.inferred(sourceId);
        ContentResourceKind kind = sourceId.contains("/locations/")
                ? ContentResourceKind.LOCATION : ContentResourceKind.CAMERA;
        ContentDescriptor descriptor = ContentDescriptor.simple(id, kind,
                SourceSpans.file(sourceId, sourceText), List.of());
        return new AdaptedSource(List.of(descriptor), List.of());
    }
}
