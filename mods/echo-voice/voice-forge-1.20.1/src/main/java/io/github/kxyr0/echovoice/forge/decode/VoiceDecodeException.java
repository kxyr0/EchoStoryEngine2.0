package io.github.kxyr0.echovoice.forge.decode;

import io.github.kxyr0.echovoice.api.diagnostic.VoiceDiagnosticCode;

public final class VoiceDecodeException extends Exception {
    private final VoiceDiagnosticCode code;

    public VoiceDecodeException(VoiceDiagnosticCode code, String message) {
        super(message);
        this.code = code;
    }

    public VoiceDecodeException(VoiceDiagnosticCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public VoiceDiagnosticCode code() {
        return code;
    }
}
