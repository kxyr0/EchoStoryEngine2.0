package io.github.kxyr0.echovisibility.forge.service;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.forge.api.navigation.NpcNavigationBodyLookupEvent;
import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnostic;
import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnosticCode;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityAabb;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityDimensionId;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityVector3;
import io.github.kxyr0.echovisibility.api.model.VisibilityWatchDefinition;
import io.github.kxyr0.echovisibility.api.target.VisibilityBoxTarget;
import io.github.kxyr0.echovisibility.api.target.VisibilityNpcTarget;
import io.github.kxyr0.echovisibility.api.target.VisibilityTargetBounds;
import io.github.kxyr0.echovisibility.forge.api.VisibilityTargetResolution;
import io.github.kxyr0.echovisibility.forge.api.VisibilityTargetResolver;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;

public final class ForgeVisibilityTargetResolver implements VisibilityTargetResolver {
    @Override
    public VisibilityTargetResolution resolve(ServerPlayer observer, VisibilityWatchDefinition definition) {
        if (definition.target() instanceof VisibilityBoxTarget box) {
            VisibilityTargetBounds bounds = new VisibilityTargetBounds(box.dimension(), box.bounds(),
                    definition.revision());
            return resolved(observer, bounds, OptionalInt.empty());
        }
        return resolveNpc(observer, definition, (VisibilityNpcTarget) definition.target());
    }

    private VisibilityTargetResolution resolveNpc(
            ServerPlayer observer,
            VisibilityWatchDefinition definition,
            VisibilityNpcTarget target) {
        NpcNavigationBodyLookupEvent event = new NpcNavigationBodyLookupEvent(
                observer.getServer(), new ActorId(target.npcId()));
        MinecraftForge.EVENT_BUS.post(event);
        if (event.bodies().size() != 1) {
            return failure(VisibilityTargetResolution.Status.TARGET_NOT_MATERIALIZED,
                    VisibilityDiagnosticCode.VISIBILITY_NPC_NOT_MATERIALIZED,
                    "NPC target is not materialized exactly once: " + target.npcId());
        }
        var body = event.bodies().get(0);
        AABB minecraftBounds = body.mob().getBoundingBox().inflate(target.boundsPadding());
        VisibilityTargetBounds bounds = new VisibilityTargetBounds(
                new VisibilityDimensionId(body.dimensionId()), convert(minecraftBounds), definition.revision());
        return resolved(observer, bounds, OptionalInt.of(body.mob().getId()));
    }

    private VisibilityTargetResolution resolved(
            ServerPlayer observer,
            VisibilityTargetBounds bounds,
            OptionalInt entityId) {
        if (!observer.level().dimension().location().toString().equals(bounds.dimension().value())) {
            return failure(VisibilityTargetResolution.Status.DIMENSION_MISMATCH,
                    VisibilityDiagnosticCode.VISIBILITY_TARGET_NOT_FOUND,
                    "Observer and visibility target are in different dimensions.");
        }
        return new VisibilityTargetResolution(VisibilityTargetResolution.Status.RESOLVED,
                Optional.of(bounds), entityId, Optional.empty());
    }

    private static VisibilityAabb convert(AABB bounds) {
        return new VisibilityAabb(new VisibilityVector3(bounds.minX, bounds.minY, bounds.minZ),
                new VisibilityVector3(bounds.maxX, bounds.maxY, bounds.maxZ));
    }

    private static VisibilityTargetResolution failure(
            VisibilityTargetResolution.Status status,
            VisibilityDiagnosticCode code,
            String message) {
        return new VisibilityTargetResolution(status, Optional.empty(), OptionalInt.empty(),
                Optional.of(VisibilityDiagnostic.of(code, message)));
    }
}
