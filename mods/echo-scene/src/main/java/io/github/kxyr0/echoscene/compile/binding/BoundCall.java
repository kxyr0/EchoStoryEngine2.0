package io.github.kxyr0.echoscene.compile.binding;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.List;

record BoundCall(String receiver, String method, List<BoundValue> arguments, SourceSpan span) {
    BoundCall {
        arguments = List.copyOf(arguments);
    }

    record BoundValue(String value, boolean literal, SourceSpan span) {
    }
}
