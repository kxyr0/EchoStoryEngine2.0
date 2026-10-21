package io.github.kxyr0.echoanimation.testsupport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class TestSources {
    private TestSources() { }
    public static String read(String name) {
        try (var stream = Objects.requireNonNull(TestSources.class.getResourceAsStream(name), name)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) { throw new UncheckedIOException(failure); }
    }
}
