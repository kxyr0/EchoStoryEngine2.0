package io.github.kxyr0.echoscript.language.source;

import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.api.source.SourcePosition;
import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SourceText {
    private final SourceId sourceId;
    private final String text;
    private final int[] lineStarts;

    public SourceText(SourceId sourceId, String text) {
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId");
        this.text = Objects.requireNonNull(text, "text");
        this.lineStarts = computeLineStarts(text);
    }

    public SourceId sourceId() {
        return sourceId;
    }

    public String text() {
        return text;
    }

    public int length() {
        return text.length();
    }

    public char charAt(int offset) {
        return text.charAt(offset);
    }

    public SourceSpan span(int startOffset, int endOffset) {
        int checkedStart = Math.max(0, Math.min(startOffset, text.length()));
        int checkedEnd = Math.max(checkedStart, Math.min(endOffset, text.length()));
        return new SourceSpan(sourceId, checkedStart, checkedEnd, positionAt(checkedStart), positionAt(checkedEnd));
    }

    public String context(SourceSpan span, int maxLength) {
        int start = Math.max(0, span.startOffset() - maxLength / 4);
        int end = Math.min(text.length(), Math.max(span.endOffset(), start) + maxLength);
        String sliced = text.substring(start, end).replace("\r", "\\r").replace("\n", "\\n");
        if (sliced.length() <= maxLength) {
            return sliced;
        }
        return sliced.substring(0, maxLength);
    }

    private SourcePosition positionAt(int offset) {
        int lineIndex = 0;
        int low = 0;
        int high = lineStarts.length - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            if (lineStarts[middle] <= offset) {
                lineIndex = middle;
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        int column = offset - lineStarts[lineIndex] + 1;
        return new SourcePosition(offset, lineIndex + 1, column);
    }

    private static int[] computeLineStarts(String text) {
        List<Integer> starts = new ArrayList<>();
        starts.add(0);
        int index = 0;
        while (index < text.length()) {
            char ch = text.charAt(index);
            if (ch == '\r') {
                index = consumeCr(text, starts, index);
            } else if (ch == '\n') {
                index++;
                starts.add(index);
            } else {
                index++;
            }
        }
        return starts.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int consumeCr(String text, List<Integer> starts, int index) {
        int next = index + 1;
        if (next < text.length() && text.charAt(next) == '\n') {
            next++;
        }
        starts.add(next);
        return next;
    }
}
