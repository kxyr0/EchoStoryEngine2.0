package io.github.kxyr0.echotime.api.mutation;

import java.util.Objects;
import java.util.Optional;

public record TimeRequestSource(
        String module,
        String system,
        String immediateOwner,
        String rootOwner,
        Optional<String> sourceResource,
        Optional<Integer> sourceLine,
        String reason) {
    public TimeRequestSource {
        module = require(module, "module");
        system = require(system, "system");
        immediateOwner = require(immediateOwner, "immediateOwner");
        rootOwner = require(rootOwner, "rootOwner");
        sourceResource = Objects.requireNonNull(sourceResource, "sourceResource").map(String::trim);
        sourceLine = Objects.requireNonNull(sourceLine, "sourceLine");
        if (sourceLine.filter(line -> line <= 0).isPresent()) {
            throw new IllegalArgumentException("sourceLine must be positive.");
        }
        reason = require(reason, "reason");
    }

    public static TimeRequestSource command(String owner, String reason) {
        return new TimeRequestSource("echo_time", "command", owner, owner,
                Optional.empty(), Optional.empty(), reason);
    }

    private static String require(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) throw new IllegalArgumentException(name + " cannot be blank.");
        return checked;
    }
}
