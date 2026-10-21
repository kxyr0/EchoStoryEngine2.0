package io.github.kxyr0.echoscript.language;

import io.github.kxyr0.echoscript.api.EchoScriptParser;
import io.github.kxyr0.echoscript.language.parser.DefaultEchoScriptParser;

/**
 * Entry point for obtaining the default EchoScript language frontend.
 */
public final class EchoScriptLanguage {
    private EchoScriptLanguage() {
    }

    public static EchoScriptParser parser() {
        return new DefaultEchoScriptParser();
    }
}
