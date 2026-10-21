package io.github.kxyr0.echocontentdoctor.source;

import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.api.source.SourcePosition;
import io.github.kxyr0.echoscript.api.source.SourceSpan;

public final class SourceSpans {
    private SourceSpans() { }

    public static SourceSpan file(String sourceId, String text) {
        return at(sourceId, text, 1, 1);
    }

    public static SourceSpan at(String sourceId, String text, int line, int column) {
        int safeLine = Math.max(1, line);
        int safeColumn = Math.max(1, column);
        int offset = offset(text, safeLine, safeColumn);
        SourcePosition position = new SourcePosition(offset, safeLine, safeColumn);
        return new SourceSpan(new SourceId(sourceId), offset, offset, position, position);
    }

    public static SourceSpan find(String sourceId, String text, String needle) {
        int offset = Math.max(0, text.indexOf(needle));
        int line = 1;
        int column = 1;
        for (int index = 0; index < offset; index++) {
            if (text.charAt(index) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        SourcePosition start = new SourcePosition(offset, line, column);
        int endOffset = Math.min(text.length(), offset + needle.length());
        SourcePosition end = new SourcePosition(endOffset, line, column + endOffset - offset);
        return new SourceSpan(new SourceId(sourceId), offset, endOffset, start, end);
    }

    private static int offset(String text, int line, int column) {
        int currentLine = 1;
        int offset = 0;
        while (offset < text.length() && currentLine < line) {
            if (text.charAt(offset++) == '\n') currentLine++;
        }
        return Math.min(text.length(), offset + column - 1);
    }
}
