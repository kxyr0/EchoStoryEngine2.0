package io.github.kxyr0.echocontentdoctor.source.adapter.events;

import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echocontentdoctor.source.SourceAdapter;
import io.github.kxyr0.echocontentdoctor.source.SourceSpans;
import java.nio.file.Path;
import java.util.List;

/** Mirrors CutsceneResourceLoader's world-relative identity contract. */
public final class CutsceneSourceAdapter implements SourceAdapter {
    public boolean accepts(Path path) {
        return path.toString().replace('\\', '/').contains("/echo/cutscenes/")
                && path.toString().endsWith(".camera.es");
    }
    public AdaptedSource adapt(String sourceId, String text) {
        String normalized = sourceId.replace('\\', '/');
        int start = normalized.indexOf("echo/cutscenes/");
        String relative = start < 0 ? normalized : normalized.substring(start + "echo/cutscenes/".length());
        String id = "jdh:" + relative.substring(0, relative.length() - ".camera.es".length());
        return new AdaptedSource(List.of(ContentDescriptor.simple(new ContentResourceId(id),
                ContentResourceKind.CUTSCENE, SourceSpans.file(sourceId, text), List.of())), List.of());
    }
}
