package io.github.kxyr0.echocontentdoctor.validation.semantic;

import io.github.kxyr0.echocontentdoctor.diagnostic.ContentDiagnostic;
import io.github.kxyr0.echocontentdoctor.fact.VoiceFact;
import io.github.kxyr0.echocontentdoctor.graph.ContentGraph;
import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import java.util.ArrayList;
import java.util.List;

public final class VoiceValidator {
    public List<ContentDiagnostic> validate(ContentGraph graph) {
        List<ContentDiagnostic> diagnostics = new ArrayList<>();
        for (ContentDescriptor descriptor : graph.snapshot().resources()) {
            for (VoiceFact voice : descriptor.voices()) {
                if (!voice.explicit()) continue;
                if (voice.assetId().isEmpty()) diagnostics.add(error(descriptor, voice, "Explicit voice asset is missing."));
                else if (!voice.supportedFormat()) diagnostics.add(error(descriptor, voice, "Voice format is unsupported."));
                else if (!voice.metadataValid()) diagnostics.add(error(descriptor, voice, "Voice decode metadata is invalid."));
            }
        }
        return diagnostics;
    }

    private ContentDiagnostic error(ContentDescriptor descriptor, VoiceFact voice, String message) {
        return new ContentDiagnostic(ContentSeverity.ERROR, "ECHO_DOCTOR_VOICE_INVALID", descriptor.id(),
                voice.span(), message, List.of());
    }
}
