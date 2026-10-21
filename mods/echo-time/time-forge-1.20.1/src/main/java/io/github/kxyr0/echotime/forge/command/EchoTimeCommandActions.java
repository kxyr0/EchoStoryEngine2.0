package io.github.kxyr0.echotime.forge.command;

import io.github.kxyr0.echotime.api.identity.TimeMutationId;
import io.github.kxyr0.echotime.api.identity.TimePauseOwner;
import io.github.kxyr0.echotime.api.mutation.TimeAddRequest;
import io.github.kxyr0.echotime.api.mutation.TimeCrossingPolicy;
import io.github.kxyr0.echotime.api.mutation.TimeMutationResult;
import io.github.kxyr0.echotime.api.mutation.TimeRequestSource;
import io.github.kxyr0.echotime.api.mutation.TimeSetRequest;
import io.github.kxyr0.echotime.api.value.EchoDay;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import io.github.kxyr0.echotime.forge.api.EchoTimeServiceBridge;
import io.github.kxyr0.echotime.forge.api.event.EchoTimeResetEvent;
import io.github.kxyr0.echotime.forge.host.ForgeEchoTimeHost;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;

final class EchoTimeCommandActions {
    private final CommandSourceStack source;
    private final ForgeEchoTimeHost host;
    private final EchoTimeServiceBridge services;

    EchoTimeCommandActions(CommandSourceStack source, ForgeEchoTimeHost host, EchoTimeServiceBridge services) {
        this.source = source;
        this.host = host;
        this.services = services;
    }

    int status() { return lines(requireHost() ? new EchoTimeStatusText().status(host) : List.of()); }
    int next() { return lines(requireHost() ? new EchoTimeStatusText().next(host) : List.of()); }

    int set(String rawTime) {
        if (!requireServices()) return 0;
        try {
            return mutation(services.mutations().set(new TimeSetRequest(id(), Optional.empty(),
                    TimeOfDay.parse(rawTime), TimeCrossingPolicy.SKIP_CROSSED, provenance("set"))));
        } catch (IllegalArgumentException exception) {
            return failure(exception.getMessage());
        }
    }

    int setDay(int day, String rawTime) {
        if (!requireServices()) return 0;
        try {
            return mutation(services.mutations().set(new TimeSetRequest(id(), Optional.of(new EchoDay(day)),
                    TimeOfDay.parse(rawTime), TimeCrossingPolicy.SKIP_CROSSED, provenance("set-day"))));
        } catch (IllegalArgumentException exception) {
            return failure(exception.getMessage());
        }
    }

    int reset() {
        if (!requireHost() || !requireServices()) return 0;
        var config = host.config();
        TimeMutationResult result = services.mutations().set(new TimeSetRequest(id(),
                Optional.of(config.initialDay()), config.initialTime(), TimeCrossingPolicy.SKIP_CROSSED,
                provenance("reset")));
        if (result.applied()) {
            MinecraftForge.EVENT_BUS.post(new EchoTimeResetEvent(source.getServer(), result.current()));
        }
        return mutation(result);
    }

    int add(String rawDuration) {
        if (!requireServices()) return 0;
        try {
            return mutation(services.mutations().add(new TimeAddRequest(id(), TimeCommandParsers.duration(rawDuration),
                    TimeCrossingPolicy.FIRE_CROSSED, provenance("add"))));
        } catch (IllegalArgumentException | ArithmeticException exception) {
            return failure(exception.getMessage());
        }
    }

    int pause() {
        if (!requireServices()) return 0;
        var result = services.mutations().pause(owner(), id());
        return success("Pause " + result.status() + "; active leases=" + result.effectiveLeaseCount());
    }

    int resume() {
        if (!requireServices()) return 0;
        var result = services.mutations().resume(owner(), id());
        return success("Resume " + result.status() + "; active leases=" + result.effectiveLeaseCount());
    }

    int pauses() {
        if (!requireHost()) return 0;
        if (host.pauseLeases().isEmpty()) return success("No active pause leases.");
        return lines(host.pauseLeases().stream().map(lease -> lease.owner().value()
                + " mutation=" + lease.acquiredBy().value()).toList());
    }

    int speed() {
        if (!requireHost()) return 0;
        var rate = host.config().rate();
        return success("Configured rate: " + rate.gameMicroseconds() + "/"
                + rate.simulationTicks() + " game micros/simulation tick; running=" + host.config().running());
    }

    int reload() {
        if (!requireHost()) return 0;
        host.reload();
        return success("Echo Time config reload requested; active revision=" + host.config().revision());
    }

    int resync() {
        if (!requireHost()) return 0;
        host.resync();
        return success("Echo Time snapshot sent to connected clients.");
    }

    int diagnostics() {
        if (!requireHost()) return 0;
        List<String> values = host.diagnostics().stream().map(Object::toString).toList();
        if (values.isEmpty()) values = List.of("No active config diagnostics.");
        return lines(values);
    }

    private int mutation(TimeMutationResult result) {
        String message = result.status() + " " + result.current().day().value() + " "
                + result.current().timeOfDay().formatHHmm() + " (" + result.message() + ")";
        return result.applied() || result.status().name().equals("DUPLICATE") ? success(message) : failure(message);
    }

    private TimeMutationId id() { return new TimeMutationId("command:" + UUID.randomUUID()); }
    private TimePauseOwner owner() { return new TimePauseOwner("command:" + source.getTextName()); }
    private TimeRequestSource provenance(String reason) { return TimeRequestSource.command(owner().value(), reason); }

    private boolean requireHost() {
        if (host != null) return true;
        failure("Echo Time is not initialized for this server.");
        return false;
    }

    private boolean requireServices() {
        if (requireHost() && services != null) return true;
        if (host != null) failure("Echo Time production services are unavailable.");
        return false;
    }

    private int lines(List<String> values) { values.forEach(this::success); return values.size(); }
    private int success(String value) { source.sendSuccess(() -> Component.literal(value), false); return 1; }
    private int failure(String value) { source.sendFailure(Component.literal(value)); return 0; }
}
