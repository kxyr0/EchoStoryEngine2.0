package io.github.kxyr0.echovisibility.script.binding;

import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnostic;
import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnosticCode;
import io.github.kxyr0.echovisibility.api.identity.VisibilityDefinitionRevision;
import io.github.kxyr0.echovisibility.script.VisibilityWatchCompileResult;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MemberSyntax;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

public final class VisibilityWatchBinder {
    public VisibilityWatchCompileResult bind(
            CompilationUnitSyntax unit,
            String sourceId,
            String source,
            VisibilityDefinitionRevision revision) {
        List<VisibilityDiagnostic> diagnostics = new ArrayList<>();
        validateClass(unit, diagnostics);
        List<MethodSyntax> methods = unit.classDeclaration().members().stream()
                .filter(MethodSyntax.class::isInstance).map(MethodSyntax.class::cast).toList();
        Optional<MethodSyntax> init = methods.stream().filter(method -> method.name().equals("Init")).findFirst();
        if (init.isEmpty()) {
            diagnostics.add(diagnostic("VisibilityWatchResource must declare Init()."));
        }
        if (methods.stream().anyMatch(method -> !method.name().equals("Init"))) {
            diagnostics.add(diagnostic("VisibilityWatchResource is Init-only and cannot declare callback methods."));
        }
        VisibilityInitDraft draft = new VisibilityInitDraft();
        VisibilityFieldCatalog fields = VisibilityFieldCatalog.from(unit.classDeclaration().members());
        init.ifPresent(method -> draft.read(method, fields, diagnostics));
        var definition = draft.build(revision, fingerprint(source), diagnostics);
        List<VisibilityDiagnostic> sourced = diagnostics.stream().map(value -> withSource(value, sourceId)).toList();
        return new VisibilityWatchCompileResult(definition, sourced, 1, 1);
    }

    private static void validateClass(
            CompilationUnitSyntax unit,
            List<VisibilityDiagnostic> diagnostics) {
        if (!unit.classDeclaration().baseType().name().text().equals("VisibilityWatchResource")) {
            diagnostics.add(diagnostic("Source must contain class ... : VisibilityWatchResource."));
        }
        long initCount = unit.classDeclaration().members().stream().filter(MethodSyntax.class::isInstance)
                .map(MethodSyntax.class::cast).filter(method -> method.name().equals("Init")).count();
        if (initCount > 1L) {
            diagnostics.add(diagnostic("VisibilityWatchResource must declare exactly one Init()."));
        }
    }

    private static String fingerprint(String source) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private static VisibilityDiagnostic diagnostic(String message) {
        return VisibilityDiagnostic.of(VisibilityDiagnosticCode.VISIBILITY_WATCH_INVALID, message);
    }

    private static VisibilityDiagnostic withSource(VisibilityDiagnostic diagnostic, String sourceId) {
        return new VisibilityDiagnostic(diagnostic.code(), diagnostic.message(), diagnostic.watchId(),
                diagnostic.generation(), Optional.of(sourceId));
    }
}
