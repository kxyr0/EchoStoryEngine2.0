package io.github.kxyr0.echotimehud.forge.config;

import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.SyntaxModifier;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import io.github.kxyr0.echotimehud.api.HudAnchor;
import io.github.kxyr0.echotimehud.api.HudConfig;
import io.github.kxyr0.echotimehud.api.HudScreenPolicy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class HudConfigBinder {
    private final HudConfigCallReader calls = new HudConfigCallReader();

    public HudConfigResult bind(String text, String source, long revision) {
        ParseResult parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(new SourceId(source), text));
        if (!parsed.successful() || parsed.syntaxTree().isEmpty()) {
            List<HudConfigDiagnostic> diagnostics = parsed.diagnostics().stream().map(value ->
                    new HudConfigDiagnostic("HUD_CONFIG_PARSE_FAILED", value.message(), source,
                            value.span().map(span -> span.start().line()))).toList();
            return new HudConfigResult(Optional.empty(), diagnostics.isEmpty()
                    ? List.of(diag("HUD_CONFIG_PARSE_FAILED", "EchoScript parser rejected config.", source)) : diagnostics);
        }
        return bindUnit(parsed.syntaxTree().orElseThrow(), source, revision);
    }

    private HudConfigResult bindUnit(CompilationUnitSyntax unit, String source, long revision) {
        List<HudConfigDiagnostic> diagnostics = new ArrayList<>();
        String namespace = unit.namespaceDeclaration().map(value -> value.name().text()).orElse("");
        if (!namespace.equals("JDH.Config.TimeHud")) diagnostics.add(diag(
                "HUD_CONFIG_BIND_FAILED", "Namespace must be JDH.Config.TimeHud.", source));
        if (!unit.classDeclaration().baseType().name().text().equals("TimeHudConfigResource")) diagnostics.add(diag(
                "HUD_CONFIG_BIND_FAILED", "Config class must extend TimeHudConfigResource.", source));
        List<MethodSyntax> methods = unit.classDeclaration().members().stream()
                .filter(MethodSyntax.class::isInstance).map(MethodSyntax.class::cast).toList();
        Optional<MethodSyntax> init = methods.stream().filter(value -> value.name().equals("Init")).findFirst();
        if (init.isEmpty() || unit.classDeclaration().members().size() != 1
                || !init.orElseThrow().modifiers().equals(List.of(SyntaxModifier.PRIVATE))
                || !init.orElseThrow().returnType().isVoid() || !init.orElseThrow().parameters().isEmpty()) {
            diagnostics.add(diag("HUD_CONFIG_BIND_FAILED", "Resource must contain only private void Init().", source));
            return new HudConfigResult(Optional.empty(), diagnostics);
        }
        Values values = new Values();
        for (var statement : init.orElseThrow().body().statements()) {
            Optional<InvocationExpressionSyntax> call = calls.call(statement);
            if (call.isEmpty()) diagnostics.add(diag("HUD_CONFIG_BIND_FAILED", "Init supports only config calls.", source));
            else read(call.orElseThrow(), values, diagnostics, source);
        }
        if (!diagnostics.isEmpty()) return new HudConfigResult(Optional.empty(), diagnostics);
        return build(values, source, revision);
    }

    private void read(InvocationExpressionSyntax call, Values v, List<HudConfigDiagnostic> errors, String source) {
        try {
            switch (calls.name(call)) {
                case "Id" -> v.id = calls.string(call, 0).orElse(null);
                case "Enabled" -> v.enabled = calls.bool(call, 0).orElseThrow();
                case "Anchor" -> v.anchor = HudAnchor.valueOf(calls.symbol(call, 0).orElseThrow());
                case "Offset" -> { v.offsetX = decimal(calls.number(call, "x")); v.offsetY = decimal(calls.number(call, "y")); }
                case "ReferenceCanvas" -> { v.referenceWidth = integer(calls.number(call, "width")); v.referenceHeight = integer(calls.number(call, "height")); }
                case "SafeMargin" -> v.safeMargin = decimal(calls.number(call, 0));
                case "TextScale" -> v.textScale = decimal(calls.number(call, 0));
                case "Format" -> v.format = calls.string(call, 0).orElse(null);
                case "ShowDay" -> v.showDay = calls.bool(call, 0).orElseThrow();
                case "Shadow" -> v.shadow = calls.bool(call, 0).orElseThrow();
                case "Background" -> v.background = calls.bool(call, 0).orElseThrow();
                case "Opacity" -> v.opacity = decimal(calls.number(call, 0));
                case "RespectHideHud" -> v.respectHideHud = calls.bool(call, 0).orElseThrow();
                case "ClampToScreen" -> v.clamp = calls.bool(call, 0).orElseThrow();
                case "ScreenPolicy" -> v.policy = HudScreenPolicy.valueOf(calls.symbol(call, 0).orElseThrow());
                default -> errors.add(diag("HUD_CONFIG_BIND_FAILED", "Unsupported TimeHudConfigResource call: " + calls.name(call), source));
            }
        } catch (IllegalArgumentException | java.util.NoSuchElementException exception) {
            errors.add(new HudConfigDiagnostic("HUD_CONFIG_BIND_FAILED", "Invalid " + calls.name(call) + " arguments.",
                    source, Optional.of(call.span().start().line())));
        }
    }

    private HudConfigResult build(Values v, String source, long revision) {
        if (!"HH:mm".equals(v.format)) return new HudConfigResult(Optional.empty(), List.of(diag(
                "HUD_CONFIG_FORMAT_INVALID", "Only Format(\"HH:mm\") is supported; last-known-good retained.", source)));
        try {
            HudConfig config = new HudConfig(required(v.id, "Id"), v.enabled, v.anchor, v.offsetX, v.offsetY,
                    v.referenceWidth, v.referenceHeight, v.safeMargin, v.textScale, v.format, v.showDay,
                    v.shadow, v.background, v.opacity, v.respectHideHud, v.clamp, v.policy, revision);
            return new HudConfigResult(Optional.of(config), List.of());
        } catch (IllegalArgumentException exception) {
            return new HudConfigResult(Optional.empty(), List.of(diag("HUD_CONFIG_INVALID", exception.getMessage(), source)));
        }
    }

    private double decimal(Optional<BigDecimal> value) { return value.orElseThrow().doubleValue(); }
    private int integer(Optional<BigDecimal> value) { return value.orElseThrow().intValueExact(); }
    private <T> T required(T value, String name) { if (value == null) throw new IllegalArgumentException(name + " is required."); return value; }
    private HudConfigDiagnostic diag(String code, String message, String source) { return new HudConfigDiagnostic(code, message, source, Optional.empty()); }

    private static final class Values {
        private String id; private boolean enabled = true; private HudAnchor anchor = HudAnchor.TOP_RIGHT;
        private double offsetX = -24, offsetY = 24; private int referenceWidth = 1920, referenceHeight = 1080;
        private double safeMargin = 12, textScale = 1; private String format; private boolean showDay;
        private boolean shadow = true, background; private double opacity = 1; private boolean respectHideHud = true;
        private boolean clamp = true; private HudScreenPolicy policy = HudScreenPolicy.SHOW_DURING_GAME_SCREENS;
    }
}
