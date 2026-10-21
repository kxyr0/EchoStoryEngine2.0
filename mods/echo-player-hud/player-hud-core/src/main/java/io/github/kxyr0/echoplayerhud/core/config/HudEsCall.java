package io.github.kxyr0.echoplayerhud.core.config;

import java.util.ArrayList;
import java.util.List;

record HudEsCall(String body) {
    static List<HudEsCall> find(String text, String name) {
        List<HudEsCall> calls = new ArrayList<>();
        String marker = name + "(";
        int from = 0;
        while (from < text.length()) {
            int start = text.indexOf(marker, from);
            if (start < 0) {
                return calls;
            }
            if (!boundary(text, start)) {
                from = start + marker.length();
                continue;
            }
            int bodyStart = start + marker.length();
            int bodyEnd = close(text, bodyStart);
            if (bodyEnd < 0) {
                return calls;
            }
            calls.add(new HudEsCall(text.substring(bodyStart, bodyEnd)));
            from = bodyEnd + 1;
        }
        return calls;
    }

    private static boolean boundary(String text, int start) {
        return start == 0 || !Character.isJavaIdentifierPart(text.charAt(start - 1));
    }

    private static int close(String text, int bodyStart) {
        int depth = 1;
        boolean quoted = false;
        boolean escaped = false;
        for (int index = bodyStart; index < text.length(); index++) {
            char ch = text.charAt(index);
            if (escaped) {
                escaped = false;
            } else if (ch == '\\' && quoted) {
                escaped = true;
            } else if (ch == '"') {
                quoted = !quoted;
            } else if (!quoted && ch == '(') {
                depth++;
            } else if (!quoted && ch == ')' && --depth == 0) {
                return index;
            }
        }
        return -1;
    }
}
