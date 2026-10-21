package io.github.kxyr0.echocontentdoctor.model;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.List;
import java.util.Objects;

public record ContentNode(String id, boolean entry, boolean externallyReachable, List<String> edges, SourceSpan span, List<String> symbols) {
    public ContentNode(String id, boolean entry, boolean externallyReachable, List<String> edges, SourceSpan span) {
        this(id, entry, externallyReachable, edges, span, List.of());
    }
    public ContentNode {
        id = Objects.requireNonNull(id, "id");
        edges = List.copyOf(edges);
        symbols = List.copyOf(symbols);
        span = Objects.requireNonNull(span, "span");
    }
}
