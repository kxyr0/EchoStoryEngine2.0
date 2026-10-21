package io.github.kxyr0.echobackgroundnpc.api.definition;

import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcDefinitionId;
import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundAppearanceId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundAppearancePoolId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundLocationId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundNpcProfileId;
import io.github.kxyr0.echobackgroundnpc.api.schedule.BackgroundScheduleEntry;
import io.github.kxyr0.echobackgroundnpc.api.source.BackgroundSourceRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record BackgroundNpcDefinition(
        BackgroundNpcId id,
        BackgroundNpcDefinitionId definitionId,
        BackgroundLocationId home,
        Optional<BackgroundNpcProfileId> profile,
        Optional<BackgroundAppearanceId> appearance,
        Optional<BackgroundAppearancePoolId> appearancePool,
        Optional<Double> walkSpeed,
        List<BackgroundScheduleEntry> schedule,
        BackgroundSourceRef source,
        long revision
) {
    public BackgroundNpcDefinition {
        id = Objects.requireNonNull(id, "id");
        definitionId = Objects.requireNonNull(definitionId, "definitionId");
        home = Objects.requireNonNull(home, "home");
        profile = Objects.requireNonNull(profile, "profile");
        appearance = Objects.requireNonNull(appearance, "appearance");
        appearancePool = Objects.requireNonNull(appearancePool, "appearancePool");
        walkSpeed = Objects.requireNonNull(walkSpeed, "walkSpeed");
        schedule = List.copyOf(Objects.requireNonNull(schedule, "schedule")).stream().sorted().toList();
        source = Objects.requireNonNull(source, "source");
        if (walkSpeed.isPresent() && (!Double.isFinite(walkSpeed.get()) || walkSpeed.get() <= 0.0)) {
            throw new IllegalArgumentException("walkSpeed must be positive.");
        }
        if (revision < 0L) throw new IllegalArgumentException("revision cannot be negative.");
    }
}
