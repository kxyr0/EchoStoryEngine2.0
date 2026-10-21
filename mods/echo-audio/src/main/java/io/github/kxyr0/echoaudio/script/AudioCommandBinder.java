package io.github.kxyr0.echoaudio.script;

import io.github.kxyr0.echoaudio.api.AudioCue;
import io.github.kxyr0.echoaudio.api.AudioFailure;
import io.github.kxyr0.echoaudio.api.AudioOptions;
import io.github.kxyr0.echoscript.api.syntax.expression.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Single semantic reader used by all host compilers. Exceptions retain the host call's source span. */
public final class AudioCommandBinder {
    public boolean accepts(ExpressionSyntax expression) {
        return expression instanceof InvocationExpressionSyntax call
                && call.target() instanceof MemberAccessExpressionSyntax member
                && member.target() instanceof NameExpressionSyntax name && name.name().equals("Sound");
    }

    public AudioCue bind(InvocationExpressionSyntax call) {
        String operation = ((MemberAccessExpressionSyntax) call.target()).memberName();
        if (!AudioCue.OPERATIONS.contains(operation)) fail("Unknown Sound operation: " + operation);
        boolean stop = operation.equals("Stop");
        boolean at = operation.endsWith("At");
        Map<String, ExpressionSyntax> named = new LinkedHashMap<>();
        List<ArgumentSyntax> positional = call.arguments().stream().filter(a -> a.name().isEmpty()).toList();
        boolean seenNamed = false;
        for (ArgumentSyntax arg : call.arguments()) {
            if (arg.name().isEmpty() && seenNamed) fail("Positional argument after named argument");
            if (arg.name().isPresent()) {
                seenNamed = true;
                if (named.putIfAbsent(arg.name().orElseThrow(), arg.value()) != null) fail("Duplicate argument");
            }
        }
        Set<String> allowed = stop ? Set.of("fadeOut") : Set.of("volume", "pitch", "duration", "fadeIn", "fadeOut", "as");
        if (!allowed.containsAll(named.keySet()) || positional.size() != (at ? 2 : 1)) {
            fail("Expected sound ID" + (at ? " and typed location" : "") + "; invalid arguments " + named.keySet());
        }
        String resource = stop ? "" : string(positional.get(0).value());
        String name = stop ? reference(positional.get(0).value())
                : named.containsKey("as") ? reference(named.get("as")) : "";
        long duration = ticks(named.get("duration"), "AUDIO_DURATION_INVALID");
        if (named.containsKey("duration") && (duration == 0 || !operation.startsWith("Loop"))) {
            throw new AudioFailure("AUDIO_DURATION_INVALID", "Positive duration is only valid on a loop");
        }
        return new AudioCue(operation, resource, name, at ? reference(positional.get(1).value()) : "",
                new AudioOptions(number(named.get("volume"), 1, "AUDIO_VOLUME_INVALID"),
                        number(named.get("pitch"), 1, "AUDIO_PITCH_INVALID"), duration,
                        ticks(named.get("fadeIn"), "AUDIO_FADE_INVALID"),
                        ticks(named.get("fadeOut"), "AUDIO_FADE_INVALID")),
                new io.github.kxyr0.echoaudio.api.AudioSource(call.span().sourceId().value(),
                        call.span().start().line(), call.span().start().column()));
    }

    private String string(ExpressionSyntax value) {
        if (value instanceof StringLiteralExpressionSyntax literal) return literal.value();
        throw new AudioFailure("AUDIO_ASSET_NOT_FOUND", "Expected extension-free string ResourceId");
    }
    private String reference(ExpressionSyntax value) {
        if (value instanceof NameExpressionSyntax name) return name.name();
        if (value instanceof StringLiteralExpressionSyntax literal) return literal.value();
        throw new AudioFailure("AUDIO_LOCATION_NOT_FOUND", "Expected named reference");
    }
    private double number(ExpressionSyntax value, double fallback, String code) {
        if (value == null) return fallback;
        if (value instanceof IntegerLiteralExpressionSyntax number) return number.value();
        if (value instanceof DecimalLiteralExpressionSyntax number) return number.value().doubleValue();
        throw new AudioFailure(code, "Expected finite numeric literal");
    }
    private long ticks(ExpressionSyntax value, String code) {
        if (value == null) return 0;
        if (!(value instanceof InvocationExpressionSyntax call)
                || !(call.target() instanceof NameExpressionSyntax name) || !name.name().equals("Seconds")
                || call.arguments().size() != 1 || call.arguments().get(0).name().isPresent()) {
            throw new AudioFailure(code, "Expected Seconds(number)");
        }
        double seconds = number(call.arguments().get(0).value(), 0, code);
        if (!Double.isFinite(seconds) || seconds < 0 || seconds > 3600) {
            throw new AudioFailure(code, "Expected 0..3600 seconds");
        }
        return (long) Math.ceil(seconds * 20);
    }
    private void fail(String detail) { throw new AudioFailure("AUDIO_PLAYBACK_START_FAILED", detail); }
}
