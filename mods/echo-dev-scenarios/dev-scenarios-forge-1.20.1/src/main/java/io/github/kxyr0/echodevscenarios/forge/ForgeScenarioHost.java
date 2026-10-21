package io.github.kxyr0.echodevscenarios.forge;

import io.github.kxyr0.echodevscenarios.api.ManagedStateSnapshot;
import io.github.kxyr0.echodevscenarios.api.ScenarioId;
import io.github.kxyr0.echodevscenarios.api.ScenarioInspection;
import io.github.kxyr0.echodevscenarios.api.ScenarioSessionId;
import io.github.kxyr0.echodevscenarios.api.ScenarioTarget;
import io.github.kxyr0.echodevscenarios.forge.setup.ForgeScenarioSetup;
import io.github.kxyr0.echodevscenarios.launcher.EchoPlayService;
import io.github.kxyr0.echodevscenarios.port.ManagedStatePort;
import io.github.kxyr0.echodevscenarios.port.ScenarioTargetPort;
import io.github.kxyr0.echodevscenarios.runtime.DevScenarioRuntime;
import io.github.kxyr0.echoscene.forge.api.EchoSceneCancelEvent;
import io.github.kxyr0.echoscene.forge.api.EchoSceneLaunchEvent;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExecutionBridge;
import io.github.kxyr0.echotime.api.value.EchoInstant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

final class ForgeScenarioHost {
    private final ForgeScenarioCatalog catalog;
    private final Map<String, Baseline> baselines = new LinkedHashMap<>();
    private final Map<ScenarioSessionId, TargetExecution> targetExecutions = new LinkedHashMap<>();
    private final DevScenarioRuntime runtime;
    private final ForgeScenarioSetup setup;
    private final EchoPlayService service;
    private ServerPlayer player;

    ForgeScenarioHost(ForgeScenarioCatalog catalog) {
        this.catalog = catalog;
        setup = new ForgeScenarioSetup(catalog, () -> player);
        runtime = new DevScenarioRuntime(() -> true, catalog.store()::error,
                new ManagedState(), setup, new Targets(), ignored -> { });
        setup.bind((step, succeeded, detail) -> runtime.complete(step, succeeded, detail));
        service = new EchoPlayService(catalog.store(), runtime);
    }

    EchoPlayService service() { return service; }

    void tick() { setup.tick(); }

    ScenarioInspection play(ServerPlayer current, String id) {
        player = current;
        return service.play(new ScenarioId(id));
    }

    ScenarioInspection restart(ServerPlayer current) {
        player = current;
        return service.restart();
    }

    ScenarioInspection stop(ServerPlayer current) {
        player = current;
        return service.stop();
    }

    private final class ManagedState implements ManagedStatePort {
        @Override
        public ManagedStateSnapshot capture(ScenarioSessionId owner) {
            requirePlayer();
            String token = owner.value() + "-baseline";
            baselines.put(token, new Baseline(player.serverLevel(), player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot(), setup.snapshotTime()));
            return new ManagedStateSnapshot(token);
        }

        @Override
        public void restore(ScenarioSessionId owner, ManagedStateSnapshot snapshot) {
            Baseline baseline = baselines.get(snapshot.token());
            if (baseline == null || player == null) return;
            player.teleportTo(baseline.level, baseline.x, baseline.y, baseline.z, baseline.yaw, baseline.pitch);
            setup.restoreTime(owner, baseline.time);
        }
    }

    private final class Targets implements ScenarioTargetPort {
        @Override
        public LaunchResult launch(ScenarioSessionId owner, ScenarioTarget target) {
            if (target.kind() == ScenarioTarget.Kind.STORY) return launchStory(owner, target);
            EchoSceneLaunchEvent event = new EchoSceneLaunchEvent(requirePlayer(), target.resourceId());
            MinecraftForge.EVENT_BUS.post(event);
            return event.result().map(value -> {
                targetExecutions.put(owner, new TargetExecution(target.kind(), value.executionId().value()));
                return LaunchResult.started(value.executionId().value());
            }).orElseGet(() -> LaunchResult.failed("Scene is missing, invalid, or could not start."));
        }

        private LaunchResult launchStory(ScenarioSessionId owner, ScenarioTarget target) {
            var lookup = EchoStoryExecutionBridge.lookup(requirePlayer().getServer());
            if (lookup.starter().isEmpty()) return LaunchResult.failed(lookup.message());
            String execution = "echo_dev_scenarios:" + owner.value().replaceAll("[^a-zA-Z0-9_.-]", "_");
            lookup.starter().orElseThrow().start(StoryId.of(target.resourceId()), ExecutionId.of(execution));
            targetExecutions.put(owner, new TargetExecution(target.kind(), execution));
            return LaunchResult.started(execution);
        }

        @Override
        public void stopOwned(ScenarioSessionId owner) {
            TargetExecution execution = targetExecutions.remove(owner);
            if (execution == null || player == null) return;
            if (execution.kind() == ScenarioTarget.Kind.SCENE) {
                MinecraftForge.EVENT_BUS.post(new EchoSceneCancelEvent(player.getServer(),
                        new io.github.kxyr0.echoscene.api.identity.SceneExecutionId(execution.id())));
            }
        }
    }

    private ServerPlayer requirePlayer() {
        if (player == null) throw new IllegalStateException("DEV_SCENARIO_SETUP_FAILED: player is unavailable");
        return player;
    }

    private record Baseline(ServerLevel level, double x, double y, double z, float yaw, float pitch,
                            EchoInstant time) { }
    private record TargetExecution(ScenarioTarget.Kind kind, String id) { }
}
