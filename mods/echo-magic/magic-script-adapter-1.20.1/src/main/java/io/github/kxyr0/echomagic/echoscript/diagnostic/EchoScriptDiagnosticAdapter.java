package io.github.kxyr0.echomagic.echoscript.diagnostic;

import io.github.kxyr0.echomagic.api.diagnostic.MagicDiagnostic;
import io.github.kxyr0.echomagic.api.diagnostic.MagicDiagnosticCode;
import io.github.kxyr0.echoscript.api.diagnostic.LanguageDiagnostic;
import java.util.List;

public final class EchoScriptDiagnosticAdapter {
    public List<MagicDiagnostic> adapt(List<LanguageDiagnostic> diagnostics) {
        return diagnostics.stream().map(value -> MagicDiagnostic.error(MagicDiagnosticCode.MAGIC_DEFINITION_BIND_FAILED,
                value.code() + ": " + value.message(), value.sourceId().value())).toList();
    }
}

