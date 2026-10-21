package io.github.kxyr0.echodialogue.forge.presentation;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.forge.api.EchoDialogueSpeakerPresentationRegistrationEvent;
import io.github.kxyr0.echodialogue.forge.api.SpeakerPresentationProviderCreationContext;
import io.github.kxyr0.echodialogue.forge.api.SpeakerPresentationProviderFactory;
import io.github.kxyr0.echodialogue.forge.diagnostic.ForgeDiagnosticSink;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationApiVersion;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderDescriptor;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderId;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

/**
 * Opens the Forge event and converts provider factories into one active catalog.
 */
public final class ForgeSpeakerPresentationRegistration {
    private ForgeSpeakerPresentationRegistration() {
    }

    public static ForgeSpeakerPresentationCatalog register(MinecraftServer server, ForgeDiagnosticSink sink) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(sink, "sink");
        sink.accept(ForgeSpeakerPresentationDiagnostics.info("EDLG-PRESENT-001",
                DiagnosticCategory.PROVIDER_REGISTRATION_OPENED,
                "Регистрация speaker presentation providers открыта."));
        EchoDialogueSpeakerPresentationRegistrationEvent event =
                new EchoDialogueSpeakerPresentationRegistrationEvent(server);
        MinecraftForge.EVENT_BUS.post(event);
        event.closeRegistration();
        List<ForgeSpeakerPresentationEntry> entries = activate(server, sink, event.factories());
        sink.accept(ForgeSpeakerPresentationDiagnostics.info("EDLG-PRESENT-002",
                DiagnosticCategory.PROVIDER_REGISTRATION_CLOSED,
                "Регистрация speaker presentation providers закрыта: " + entries.size()));
        return new ForgeSpeakerPresentationCatalog(entries, sink);
    }

    private static List<ForgeSpeakerPresentationEntry> activate(
            MinecraftServer server,
            ForgeDiagnosticSink sink,
            List<SpeakerPresentationProviderFactory> factories
    ) {
        List<Candidate> candidates = factories.stream()
                .map(factory -> candidate(factory, sink))
                .flatMap(OptionalCandidate::stream)
                .sorted()
                .toList();
        Map<SpeakerPresentationProviderId, Long> counts = candidates.stream()
                .map(Candidate::descriptor)
                .map(SpeakerPresentationProviderDescriptor::id)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        List<ForgeSpeakerPresentationEntry> entries = new ArrayList<>();
        for (Candidate candidate : candidates) {
            entries.add(entry(server, sink, candidate, counts.get(candidate.descriptor().id()) > 1));
        }
        return entries;
    }

    private static OptionalCandidate candidate(SpeakerPresentationProviderFactory factory, ForgeDiagnosticSink sink) {
        try {
            return new OptionalCandidate(new Candidate(factory, Objects.requireNonNull(factory.descriptor())));
        } catch (RuntimeException exception) {
            sink.accept(ForgeSpeakerPresentationDiagnostics.error("EDLG-PRESENT-006",
                    DiagnosticCategory.PROVIDER_SESSION_CREATION_FAILED,
                    "Provider descriptor не прочитан; factory отклонена."));
            return new OptionalCandidate(null);
        }
    }

    private static ForgeSpeakerPresentationEntry entry(
            MinecraftServer server,
            ForgeDiagnosticSink sink,
            Candidate candidate,
            boolean duplicate
    ) {
        SpeakerPresentationProviderDescriptor descriptor = candidate.descriptor();
        if (duplicate) {
            sink.accept(ForgeSpeakerPresentationDiagnostics.error("EDLG-PRESENT-004",
                    DiagnosticCategory.PROVIDER_DUPLICATE_ID, "Duplicate provider ID отклонён: " + descriptor.id()));
            return ForgeSpeakerPresentationEntry.rejected(descriptor, "Duplicate provider ID.");
        }
        if (!descriptor.compatibleWith(SpeakerPresentationApiVersion.current())) {
            sink.accept(ForgeSpeakerPresentationDiagnostics.error("EDLG-PRESENT-005",
                    DiagnosticCategory.PROVIDER_API_INCOMPATIBLE,
                    "Provider " + descriptor.id() + " требует несовместимый presentation API."));
            return ForgeSpeakerPresentationEntry.rejected(descriptor, "Presentation API incompatible.");
        }
        return active(server, sink, candidate);
    }

    private static ForgeSpeakerPresentationEntry active(
            MinecraftServer server,
            ForgeDiagnosticSink sink,
            Candidate candidate
    ) {
        try {
            SpeakerPresentationProviderSession session = Objects.requireNonNull(candidate.factory().create(
                    new SpeakerPresentationProviderCreationContext(server)));
            sink.accept(ForgeSpeakerPresentationDiagnostics.info("EDLG-PRESENT-003",
                    DiagnosticCategory.PROVIDER_ACCEPTED,
                    "Provider принят: " + candidate.descriptor().id()));
            return ForgeSpeakerPresentationEntry.active(candidate.descriptor(), session);
        } catch (RuntimeException exception) {
            sink.accept(ForgeSpeakerPresentationDiagnostics.error("EDLG-PRESENT-006",
                    DiagnosticCategory.PROVIDER_SESSION_CREATION_FAILED,
                    "Provider " + candidate.descriptor().id() + " не создал session."));
            return ForgeSpeakerPresentationEntry.rejected(candidate.descriptor(), "Session creation failed.");
        }
    }

    private record Candidate(
            SpeakerPresentationProviderFactory factory,
            SpeakerPresentationProviderDescriptor descriptor
    ) implements Comparable<Candidate> {
        @Override
        public int compareTo(Candidate other) {
            return descriptor.id().compareTo(other.descriptor.id());
        }
    }

    private record OptionalCandidate(Candidate candidate) {
        java.util.stream.Stream<Candidate> stream() {
            return candidate == null ? java.util.stream.Stream.empty() : java.util.stream.Stream.of(candidate);
        }
    }
}
