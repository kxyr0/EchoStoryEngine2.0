package io.github.kxyr0.echodialogue.forge.command;

import io.github.kxyr0.echodialogue.forge.presentation.ForgeSpeakerPresentationProviderView;
import java.util.List;

/**
 * Russian command output for dynamic speaker presentation provider status.
 */
final class EchoDialogueProviderCommandLines {
    private static final int MAX_PROVIDERS = 50;

    private EchoDialogueProviderCommandLines() {
    }

    static List<String> providers(List<ForgeSpeakerPresentationProviderView> views) {
        if (views.isEmpty()) {
            return List.of("Presentation providers не зарегистрированы.");
        }
        return views.stream().limit(MAX_PROVIDERS).map(EchoDialogueProviderCommandLines::summary).toList();
    }

    static List<String> provider(List<ForgeSpeakerPresentationProviderView> views, String providerId) {
        return views.stream()
                .filter(view -> view.descriptor().id().toString().equals(providerId))
                .findFirst()
                .map(EchoDialogueProviderCommandLines::detail)
                .orElseGet(() -> List.of("Provider не найден: " + providerId));
    }

    private static String summary(ForgeSpeakerPresentationProviderView view) {
        return "Provider: " + view.descriptor().id()
                + " | " + view.descriptor().displayName()
                + " | version=" + view.descriptor().implementationVersion()
                + " | status=" + view.status();
    }

    private static List<String> detail(ForgeSpeakerPresentationProviderView view) {
        List<String> diagnostics = view.recentDiagnostics().isEmpty()
                ? List.of("recent diagnostics: нет")
                : view.recentDiagnostics().stream().map(line -> "recent: " + line).toList();
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(
                "Provider: " + view.descriptor().id(),
                "display: " + view.descriptor().displayName(),
                "implementationVersion: " + view.descriptor().implementationVersion(),
                "api required: major=" + view.descriptor().requiredApiMajor()
                        + ", minimumMinor=" + view.descriptor().minimumApiMinor(),
                "status: " + view.status(),
                "reason: " + (view.reason().isBlank() ? "нет" : view.reason()),
                "stats: resolved=" + view.resolved()
                        + ", notHandled=" + view.notHandled()
                        + ", fallback=" + view.fallback()), diagnostics.stream()).toList();
    }
}
