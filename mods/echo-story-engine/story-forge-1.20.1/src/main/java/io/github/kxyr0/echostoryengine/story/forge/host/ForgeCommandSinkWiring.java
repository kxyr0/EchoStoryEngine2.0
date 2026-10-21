package io.github.kxyr0.echostoryengine.story.forge.host;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalCommand;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.core.port.DiagnosticSink;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionAcceptance;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeRuntimeDiagnostics;
import io.github.kxyr0.echostoryengine.story.forge.environment.catalog.EnvironmentCatalogState;
import io.github.kxyr0.echostoryengine.story.forge.environment.runtime.EnvironmentBlockAccess;
import io.github.kxyr0.echostoryengine.story.forge.environment.runtime.EnvironmentService;
import io.github.kxyr0.echostoryengine.story.forge.environment.runtime.EnvironmentSetCommandSupport;
import io.github.kxyr0.echostoryengine.story.forge.environment.runtime.ForgeEnvironmentBlockAccess;
import io.github.kxyr0.echostoryengine.story.forge.execution.ExternalCommandSink;
import io.github.kxyr0.echostoryengine.story.forge.extension.catalog.ExtensionCatalog;
import io.github.kxyr0.echostoryengine.story.forge.extension.dispatch.ExtensionCommandSink;
import io.github.kxyr0.echostoryengine.story.forge.extension.dispatch.ExtensionCompletionQueue;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

final class ForgeCommandSinkWiring {
    private final EnvironmentCatalogState environmentCatalogState;
    private final DiagnosticSink diagnosticSink;
    private final StoryClock clock;
    private final ForgeRuntimeDiagnostics diagnostics;

    ForgeCommandSinkWiring(
            EnvironmentCatalogState environmentCatalogState,
            DiagnosticSink diagnosticSink,
            StoryClock clock,
            ForgeRuntimeDiagnostics diagnostics) {
        this.environmentCatalogState = environmentCatalogState;
        this.diagnosticSink = diagnosticSink;
        this.clock = clock;
        this.diagnostics = diagnostics;
    }

    ExternalCommandSink create(
            MinecraftServer server,
            ExtensionCatalog extensions,
            ExtensionCompletionQueue completions,
            StoryLaunchWiring storyLaunch) {
        ExtensionCommandSink extensionSink = new ExtensionCommandSink(
                extensions,
                completions,
                diagnosticSink,
                clock,
                diagnostics);
        return new EnvironmentAwareCommandSink(server, completions, extensionSink,
                new EnvironmentSetCommandSupport(new EnvironmentService(environmentCatalogState)),
                new StoryStartCommandSupport(),
                storyLaunch.storyExists(),
                storyLaunch.storyStarter());
    }

    record StoryLaunchWiring(
            Predicate<StoryId> storyExists,
            BiConsumer<StoryId, ExecutionId> storyStarter) {
    }

    private final class EnvironmentAwareCommandSink implements ExternalCommandSink {
        private final MinecraftServer server;
        private final ExtensionCompletionQueue completions;
        private final ExternalCommandSink fallback;
        private final EnvironmentSetCommandSupport environment;
        private final StoryStartCommandSupport storyStart;
        private final Predicate<StoryId> storyExists;
        private final BiConsumer<StoryId, ExecutionId> storyStarter;

        private EnvironmentAwareCommandSink(
                MinecraftServer server,
                ExtensionCompletionQueue completions,
                ExternalCommandSink fallback,
                EnvironmentSetCommandSupport environment,
                StoryStartCommandSupport storyStart,
                Predicate<StoryId> storyExists,
                BiConsumer<StoryId, ExecutionId> storyStarter) {
            this.server = server;
            this.completions = completions;
            this.fallback = fallback;
            this.environment = environment;
            this.storyStart = storyStart;
            this.storyExists = storyExists;
            this.storyStarter = storyStarter;
        }

        @Override
        public void dispatch(StoryExecutionState state, ExternalCommand command) {
            if (environment.handles(command)) {
                completions.track(state, command);
                ExternalActionResultCategory result = environment.apply(
                        new EnvironmentSetCommandSupport.EnvironmentCommandContext(
                                state, command, this::world, diagnosticSink, clock));
                complete(state, command, result);
                return;
            }
            if (storyStart.handles(command)) {
                completions.track(state, command);
                ExternalActionResultCategory result = storyStart.apply(
                        new StoryStartCommandSupport.StoryStartCommandContext(
                                state, command, storyExists, storyStarter));
                complete(state, command, result);
                return;
            }
            fallback.dispatch(state, command);
        }

        @Override
        public void recover(StoryExecutionState state, ExternalCommand command) {
            dispatch(state, command);
        }

        private void complete(
                StoryExecutionState state,
                ExternalCommand command,
                ExternalActionResultCategory result) {
            ActionCompletionAcceptance accepted = completions.sink().complete(new ActionCompletion(
                    state.storyId(), state.executionId(), command.actionExecutionId(), command.actionType(),
                    result, Map.of()));
            if (accepted != ActionCompletionAcceptance.ACCEPTED) {
                diagnosticSink.accept(diagnostics.extensionCompletionRejected(
                        state.executionId(), command.actionType().asString(), accepted.name(), clock.now()));
            }
        }

        private Optional<EnvironmentBlockAccess> world(NamespacedId dimension) {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(dimension.namespace(), dimension.path());
            ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, location);
            ServerLevel level = server.getLevel(key);
            return level == null ? Optional.empty() : Optional.of(new ForgeEnvironmentBlockAccess(level));
        }
    }
}
