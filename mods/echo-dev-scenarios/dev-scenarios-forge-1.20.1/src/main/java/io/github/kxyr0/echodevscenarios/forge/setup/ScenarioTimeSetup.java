package io.github.kxyr0.echodevscenarios.forge.setup;

import io.github.kxyr0.echodevscenarios.api.ScenarioSessionId;
import io.github.kxyr0.echodevscenarios.port.ScenarioSetupPort.StartResult;
import io.github.kxyr0.echodevscenarios.port.ScenarioSetupRequest;
import io.github.kxyr0.echotime.api.identity.TimeMutationId;
import io.github.kxyr0.echotime.api.mutation.TimeCrossingPolicy;
import io.github.kxyr0.echotime.api.mutation.TimeMutationStatus;
import io.github.kxyr0.echotime.api.mutation.TimeRequestSource;
import io.github.kxyr0.echotime.api.mutation.TimeSetRequest;
import io.github.kxyr0.echotime.api.value.EchoDay;
import io.github.kxyr0.echotime.api.value.EchoInstant;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import io.github.kxyr0.echotime.forge.api.EchoTimeServiceAccess;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;

final class ScenarioTimeSetup {
    private final Supplier<ServerPlayer> player;

    ScenarioTimeSetup(Supplier<ServerPlayer> player) { this.player = player; }

    StartResult set(ScenarioSetupRequest request) {
        var args = request.step().arguments();
        long day = Long.parseLong(args.get(0).replaceAll("[^0-9]", ""));
        ServerPlayer current = requirePlayer();
        var services = EchoTimeServiceAccess.find(current.getServer()).orElse(null);
        if (services == null) return StartResult.failed("Echo Time service unavailable");
        String key = ForgeScenarioSetup.token(request);
        var source = new TimeRequestSource("echo_dev_scenarios", "scenario_setup", key,
                request.owner().value(), Optional.empty(), Optional.of(request.step().span().start().line()),
                "Dev Scenario Given");
        var result = services.mutations().set(new TimeSetRequest(new TimeMutationId(key),
                Optional.of(new EchoDay(day)), TimeOfDay.parse(args.get(1)),
                TimeCrossingPolicy.SKIP_CROSSED, source));
        return result.status() == TimeMutationStatus.APPLIED || result.status() == TimeMutationStatus.DUPLICATE
                ? StartResult.completed() : StartResult.failed(result.message());
    }

    EchoInstant snapshot() {
        ServerPlayer current = requirePlayer();
        return services(current).query().current().instant();
    }

    void restore(ScenarioSessionId owner, EchoInstant instant) {
        ServerPlayer current = requirePlayer();
        var services = services(current);
        String key = "echo_dev_scenarios:restore:" + owner.value() + ":"
                + services.query().current().instant().revision().value();
        var source = new TimeRequestSource("echo_dev_scenarios", "scenario_restore", key,
                owner.value(), Optional.empty(), Optional.empty(), "Restore Dev Scenario baseline");
        var result = services.mutations().set(new TimeSetRequest(new TimeMutationId(key),
                Optional.of(instant.day()), instant.timeOfDay(), TimeCrossingPolicy.SKIP_CROSSED, source));
        if (result.status() != TimeMutationStatus.APPLIED && result.status() != TimeMutationStatus.DUPLICATE) {
            throw new IllegalStateException("DEV_SCENARIO_RESTORE_FAILED: " + result.message());
        }
    }

    private io.github.kxyr0.echotime.forge.api.EchoTimeServiceBridge services(ServerPlayer current) {
        return EchoTimeServiceAccess.find(current.getServer())
                .orElseThrow(() -> new IllegalStateException("Echo Time service unavailable"));
    }

    private ServerPlayer requirePlayer() {
        ServerPlayer value = player.get();
        if (value == null) throw new IllegalStateException("player is unavailable");
        return value;
    }
}
