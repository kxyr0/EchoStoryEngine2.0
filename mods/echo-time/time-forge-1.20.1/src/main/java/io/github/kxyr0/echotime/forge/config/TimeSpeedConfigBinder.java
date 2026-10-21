package io.github.kxyr0.echotime.forge.config;

import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.ParseResult;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.SyntaxModifier;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MemberSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import io.github.kxyr0.echotime.api.config.TimeConfigDiagnostic;
import io.github.kxyr0.echotime.api.config.TimeConfigResult;
import io.github.kxyr0.echotime.api.config.TimeSpeedConfig;
import io.github.kxyr0.echotime.api.value.EchoDay;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import io.github.kxyr0.echotime.api.value.TimeRate;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TimeSpeedConfigBinder {
    private static final long MAX_GAME_MICROS_PER_TICK = TimeOfDay.MICROS_PER_DAY;
    private final TimeConfigCallReader calls = new TimeConfigCallReader();

    public TimeConfigResult bind(String text, String source, long revision) {
        ParseResult parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(new SourceId(source), text));
        if (!parsed.successful() || parsed.syntaxTree().isEmpty()) {
            List<TimeConfigDiagnostic> diagnostics = parsed.diagnostics().stream().map(value ->
                    new TimeConfigDiagnostic("ECHO_TIME_CONFIG_PARSE_FAILED", value.message(), source,
                            value.span().map(span -> span.start().line()))).toList();
            return new TimeConfigResult(Optional.empty(), diagnostics.isEmpty()
                    ? List.of(diagnostic("ECHO_TIME_CONFIG_PARSE_FAILED", "EchoScript parser rejected config.", source))
                    : diagnostics);
        }
        return bindUnit(parsed.syntaxTree().orElseThrow(), source, revision);
    }

    private TimeConfigResult bindUnit(CompilationUnitSyntax unit, String source, long revision) {
        List<TimeConfigDiagnostic> diagnostics = new ArrayList<>();
        String namespace = unit.namespaceDeclaration().map(value -> value.name().text()).orElse("");
        if (!namespace.equals("JDH.Config.Time")) diagnostics.add(diagnostic(
                "ECHO_TIME_CONFIG_BIND_FAILED", "Namespace must be JDH.Config.Time.", source));
        if (!unit.classDeclaration().baseType().name().text().equals("TimeSpeedConfigResource")) {
            diagnostics.add(diagnostic("ECHO_TIME_CONFIG_BIND_FAILED",
                    "Config class must extend TimeSpeedConfigResource.", source));
        }
        Optional<MethodSyntax> init = unit.classDeclaration().members().stream()
                .filter(MethodSyntax.class::isInstance).map(MethodSyntax.class::cast)
                .filter(method -> method.name().equals("Init")).findFirst();
        if (init.isEmpty() || unit.classDeclaration().members().size() != 1
                || !init.orElseThrow().modifiers().equals(List.of(SyntaxModifier.PRIVATE))
                || !init.orElseThrow().returnType().isVoid() || !init.orElseThrow().parameters().isEmpty()) {
            diagnostics.add(diagnostic("ECHO_TIME_CONFIG_BIND_FAILED",
                    "Config requires only private void Init().", source));
            return new TimeConfigResult(Optional.empty(), diagnostics);
        }
        Values values = new Values();
        for (var statement : init.orElseThrow().body().statements()) {
            Optional<InvocationExpressionSyntax> call = calls.call(statement);
            if (call.isEmpty()) {
                diagnostics.add(diagnostic("ECHO_TIME_CONFIG_BIND_FAILED", "Init supports only config calls.", source));
                continue;
            }
            read(call.orElseThrow(), values, diagnostics, source);
        }
        if (!diagnostics.isEmpty()) return new TimeConfigResult(Optional.empty(), diagnostics);
        try {
            TimeRate rate = TimeRate.compile(required(values.gameMinutes, "Speed.gameMinutes"),
                    required(values.realSeconds, "Speed.realSeconds"));
            validateRate(rate);
            TimeSpeedConfig config = new TimeSpeedConfig(required(values.id, "Id"),
                    new EchoDay(required(values.initialDay, "InitialDay")),
                    TimeOfDay.parse(required(values.initialTime, "InitialTime")), rate, values.running,
                    values.syncSun, values.syncMoon, List.copyOf(values.dimensions), revision, 1024);
            return new TimeConfigResult(Optional.of(config), List.of());
        } catch (IllegalArgumentException | ArithmeticException exception) {
            return new TimeConfigResult(Optional.empty(), List.of(diagnostic(
                    "ECHO_TIME_CONFIG_INVALID", exception.getMessage(), source)));
        }
    }

    private void read(InvocationExpressionSyntax call, Values values,
                      List<TimeConfigDiagnostic> diagnostics, String source) {
        String name = calls.name(call);
        switch (name) {
            case "Id" -> values.id = calls.string(call, 0).orElse(null);
            case "InitialDay" -> values.initialDay = calls.integer(call, 0).map(Integer::longValue).orElse(null);
            case "InitialTime" -> values.initialTime = calls.string(call, 0).orElse(null);
            case "Running" -> values.running = calls.bool(call, 0).orElse(false);
            case "SyncSun" -> values.syncSun = calls.bool(call, 0).orElse(false);
            case "SyncMoon" -> values.syncMoon = calls.bool(call, 0).orElse(false);
            case "SunDimension" -> calls.string(call, 0).ifPresent(value -> {
                if (!values.dimensionExplicit) values.dimensions.clear();
                values.dimensionExplicit = true;
                values.dimensions.add(value);
            });
            case "Speed" -> {
                values.gameMinutes = calls.decimal(call, "gameMinutes").orElse(null);
                values.realSeconds = calls.decimal(call, "realSeconds").orElse(null);
            }
            default -> diagnostics.add(new TimeConfigDiagnostic("ECHO_TIME_CONFIG_BIND_FAILED",
                    "Unsupported TimeSpeedConfigResource call: " + name, source,
                    Optional.of(call.span().start().line())));
        }
    }

    private void validateRate(TimeRate rate) {
        BigInteger numerator = BigInteger.valueOf(rate.gameMicroseconds());
        BigInteger maximum = BigInteger.valueOf(rate.simulationTicks())
                .multiply(BigInteger.valueOf(MAX_GAME_MICROS_PER_TICK));
        if (numerator.compareTo(maximum) > 0) throw new IllegalArgumentException("ECHO_TIME_RATE_TOO_HIGH");
    }

    private <T> T required(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is required.");
        return value;
    }

    private TimeConfigDiagnostic diagnostic(String code, String message, String source) {
        return new TimeConfigDiagnostic(code, message, source, Optional.empty());
    }

    private static final class Values {
        private String id;
        private Long initialDay;
        private String initialTime;
        private BigDecimal gameMinutes;
        private BigDecimal realSeconds;
        private boolean running = true;
        private boolean syncSun = true;
        private boolean syncMoon = true;
        private boolean dimensionExplicit;
        private final List<String> dimensions = new ArrayList<>(List.of("minecraft:overworld"));
    }
}
