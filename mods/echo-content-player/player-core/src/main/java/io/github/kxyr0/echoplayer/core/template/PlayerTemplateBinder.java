package io.github.kxyr0.echoplayer.core.template;

import io.github.kxyr0.echoplayer.api.appearance.PlayerAppearanceSnapshot;
import io.github.kxyr0.echoplayer.api.appearance.PlayerBaseSkin;
import io.github.kxyr0.echoplayer.api.appearance.PlayerSkinMode;
import io.github.kxyr0.echoplayer.api.identity.PlayerDisplayName;
import io.github.kxyr0.echoplayer.api.identity.PlayerNameMode;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileTemplateId;
import io.github.kxyr0.echoplayer.api.result.PlayerProfileDiagnostic;
import io.github.kxyr0.echoplayer.api.result.PlayerProfileResult;
import io.github.kxyr0.echoplayer.api.spark.PlayerSpark;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ReturnStatementSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * PlayerProfile EchoScript semantic binder.
 */
public final class PlayerTemplateBinder {
    private final TemplateArgumentReader arguments = new TemplateArgumentReader();
    private final TemplateValueReader values = new TemplateValueReader();

    public PlayerProfileResult<PlayerProfileTemplate> bind(CompilationUnitSyntax unit, String source,
                                                           String fingerprint) {
        List<PlayerProfileDiagnostic> diagnostics = new ArrayList<>();
        requireTemplate(unit.classDeclaration(), source, diagnostics);
        Optional<InvocationExpressionSyntax> call = buildReturn(unit.classDeclaration(), source, diagnostics);
        Optional<PlayerProfileTemplate> template = call.map(invocation -> template(invocation, source,
                fingerprint, diagnostics));
        if (diagnostics.stream().anyMatch(PlayerProfileDiagnostic::error)) {
            return PlayerProfileResult.invalid(diagnostics);
        }
        return PlayerProfileResult.valid(template.orElseThrow(), diagnostics);
    }

    private void requireTemplate(ClassSyntax resource, String source, List<PlayerProfileDiagnostic> diagnostics) {
        if (!"PlayerProfileTemplate".equals(resource.baseType().name().text())) {
            diagnostics.add(error("Source must declare class ... : PlayerProfileTemplate.", source));
        }
    }

    private Optional<InvocationExpressionSyntax> buildReturn(ClassSyntax resource, String source,
                                                             List<PlayerProfileDiagnostic> diagnostics) {
        List<MethodSyntax> matches = resource.members().stream()
                .filter(MethodSyntax.class::isInstance)
                .map(MethodSyntax.class::cast)
                .filter(method -> "Build".equals(method.name()))
                .toList();
        if (matches.size() != 1) {
            diagnostics.add(error("PlayerProfileTemplate must contain exactly one Build method.", source));
            return Optional.empty();
        }
        return profileReturn(matches.get(0), source, diagnostics);
    }

    private Optional<InvocationExpressionSyntax> profileReturn(MethodSyntax method, String source,
                                                               List<PlayerProfileDiagnostic> diagnostics) {
        return method.body().statements().stream()
                .filter(ReturnStatementSyntax.class::isInstance)
                .map(ReturnStatementSyntax.class::cast)
                .flatMap(statement -> statement.value().stream())
                .filter(InvocationExpressionSyntax.class::isInstance)
                .map(InvocationExpressionSyntax.class::cast)
                .filter(this::playerProfileCall)
                .findFirst()
                .or(() -> {
                    diagnostics.add(error("Build must return PlayerProfile(...).", source));
                    return Optional.empty();
                });
    }

    private PlayerProfileTemplate template(InvocationExpressionSyntax call, String source, String fingerprint,
                                           List<PlayerProfileDiagnostic> diagnostics) {
        Map<String, io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax> args =
                arguments.read(call.arguments(), source, diagnostics);
        PlayerProfileTemplateId id = readTemplateId(args, source, diagnostics);
        PlayerNameMode nameMode = enumValue(PlayerNameMode.class, values.symbol("nameMode", args,
                "ACCOUNT", source, diagnostics), PlayerNameMode.ACCOUNT, source, diagnostics);
        Optional<PlayerDisplayName> displayName = displayName(args, source, diagnostics);
        PlayerSpark spark = spark(args, source, diagnostics);
        PlayerAppearanceSnapshot appearance = appearance(args, source, diagnostics);
        return safeTemplate(id, nameMode, displayName, spark, appearance, fingerprint, source, diagnostics);
    }

    private PlayerProfileTemplateId readTemplateId(
            Map<String, io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax> args,
            String source,
            List<PlayerProfileDiagnostic> diagnostics
    ) {
        try {
            return PlayerProfileTemplateId.parse(values.requiredString("id", args, source, diagnostics));
        } catch (IllegalArgumentException ex) {
            diagnostics.add(error(ex.getMessage(), source));
            return PlayerProfileTemplateId.parse("jdh:invalid");
        }
    }

    private PlayerAppearanceSnapshot appearance(
            Map<String, io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax> args,
            String source,
            List<PlayerProfileDiagnostic> diagnostics
    ) {
        PlayerSkinMode mode = enumValue(PlayerSkinMode.class, values.symbol("skinMode", args,
                "ACCOUNT", source, diagnostics), PlayerSkinMode.ACCOUNT, source, diagnostics);
        return PlayerBaseSkin.snapshot(0L);
    }

    private Optional<PlayerDisplayName> displayName(
            Map<String, io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax> args,
            String source,
            List<PlayerProfileDiagnostic> diagnostics
    ) {
        try {
            return values.optionalString("displayName", args, source, diagnostics).map(PlayerDisplayName::of);
        } catch (IllegalArgumentException ex) {
            diagnostics.add(error(ex.getMessage(), source));
            return Optional.empty();
        }
    }

    private PlayerSpark spark(Map<String, io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax> args,
                              String source, List<PlayerProfileDiagnostic> diagnostics) {
        try {
            return new PlayerSpark(values.integer("spark", args, 100, source, diagnostics),
                    values.integer("maxSpark", args, 100, source, diagnostics));
        } catch (IllegalArgumentException ex) {
            diagnostics.add(error(ex.getMessage(), source));
            return PlayerSpark.full(100);
        }
    }

    private PlayerProfileTemplate safeTemplate(PlayerProfileTemplateId id, PlayerNameMode nameMode,
                                               Optional<PlayerDisplayName> displayName, PlayerSpark spark,
                                               PlayerAppearanceSnapshot appearance, String fingerprint,
                                               String source, List<PlayerProfileDiagnostic> diagnostics) {
        try {
            return new PlayerProfileTemplate(id, nameMode, displayName, spark, appearance, fingerprint);
        } catch (IllegalArgumentException ex) {
            diagnostics.add(error(ex.getMessage(), source));
            return PlayerProfileTemplate.fallback();
        }
    }

    private <T extends Enum<T>> T enumValue(Class<T> type, String raw, T fallback, String source,
                                           List<PlayerProfileDiagnostic> diagnostics) {
        try {
            return Enum.valueOf(type, raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            diagnostics.add(error(type.getSimpleName() + " value is not supported: " + raw, source));
            return fallback;
        }
    }

    private boolean playerProfileCall(InvocationExpressionSyntax call) {
        return call.target() instanceof NameExpressionSyntax target && "PlayerProfile".equals(target.name());
    }

    private PlayerProfileDiagnostic error(String message, String source) {
        return PlayerProfileDiagnostic.error("PLAYER_TEMPLATE_BIND_ERROR", message, source);
    }
}
