package io.github.kxyr0.echotime.forge.host;

import io.github.kxyr0.echotime.api.value.EchoInstant;
import java.util.ArrayDeque;
import java.util.List;

public final class EchoTimeTrace {
    private static final int CAPACITY = 128;
    private final ArrayDeque<String> entries = new ArrayDeque<>();

    public void add(String event, String detail) {
        if (entries.size() == CAPACITY) entries.removeFirst();
        entries.addLast(event + " " + detail);
    }

    public List<String> entries() {
        return List.copyOf(entries);
    }

    void addInstant(String event, EchoInstant instant) {
        add(event, "Day " + instant.day().value() + " " + instant.timeOfDay().formatHHmm()
                + " absolute=" + instant.absoluteMicroseconds() + " revision=" + instant.revision().value());
    }
}
