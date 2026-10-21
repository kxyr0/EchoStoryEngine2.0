package io.github.kxyr0.echostoryengine.story.forge.environment.runtime;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalCommand;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.core.port.DiagnosticSink;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class EnvironmentSetCommandSupport {
    public static final NamespacedId ACTION_TYPE = NamespacedId.of("echo_story_engine:environment_set");

    private final EnvironmentService service;
    private final ForgeEnvironmentRuntimeDiagnostics diagnostics = new ForgeEnvironmentRuntimeDiagnostics();

    public EnvironmentSetCommandSupport(EnvironmentService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public boolean handles(ExternalCommand command) {
        return ACTION_TYPE.equals(command.actionType());
    }

    public ExternalActionResultCategory apply(EnvironmentCommandContext context) {
        try {
            service.set(context.command().payload().getOrDefault("environmentId", ""),
                    context.command().payload().getOrDefault("stateId", ""), context.worlds());
            return ExternalActionResultCategory.SUCCESS;
        } catch (EnvironmentApplyException exception) {
            context.diagnosticSink().accept(diagnostics.setFailed(
                    context.state(), context.command(), exception.getMessage(), context.clock().now()));
            return ExternalActionResultCategory.FAILURE;
        }
    }

    public record EnvironmentCommandContext(
            StoryExecutionState state,
            ExternalCommand command,
            Function<NamespacedId, Optional<EnvironmentBlockAccess>> worlds,
            DiagnosticSink diagnosticSink,
            StoryClock clock) {
    }
}
