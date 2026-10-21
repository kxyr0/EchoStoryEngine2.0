package io.github.kxyr0.echoscript.api;

/**
 * Public syntax-only parser contract for EchoScript source text.
 *
 * <p>Implementations must not execute source, load classes, touch files, log, or interpret consumer-domain semantics.
 * Expected malformed source is reported through {@link ParseResult} diagnostics instead of exceptions.</p>
 */
public interface EchoScriptParser {
    ParseResult parse(ParseRequest request);
}
