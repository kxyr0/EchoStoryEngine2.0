package io.github.kxyr0.echoitem.forge.api;

/**
 * Public listener hook for immutable reload outcomes.
 */
public interface EchoItemReloadListener {
    void onReload(EchoItemReloadSummary summary);
}
