package io.github.kxyr0.echoscript.api.source;

import java.util.Objects;

/**

* Неизменяемый полуоткрытый диапазон в исходном тексте.
*
* <p>Начальное смещение включается в диапазон, а конечное — не включается.
* Позиции используют ту же систему индексации, что и {@link SourcePosition}.
* Код, создающий диапазоны, обязан следить за тем, чтобы они не выходили
* за пределы исходного текста, к которому относятся.</p>
*/

public record SourceSpan(
        SourceId sourceId,
        int startOffset,
        int endOffset,
        SourcePosition start,
        SourcePosition end
) {
    public SourceSpan {
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        start = Objects.requireNonNull(start, "start");
        end = Objects.requireNonNull(end, "end");
        if (startOffset < 0 || endOffset < startOffset) {
            throw new IllegalArgumentException("Invalid source span offsets.");
        }
    }
}
