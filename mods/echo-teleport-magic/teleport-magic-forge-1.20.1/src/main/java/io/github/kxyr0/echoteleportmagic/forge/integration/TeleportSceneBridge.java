package io.github.kxyr0.echoteleportmagic.forge.integration;

import io.github.kxyr0.echoscene.api.command.SceneCommandStart;
import io.github.kxyr0.echoscene.api.definition.SceneCapabilityKind;
import io.github.kxyr0.echoscene.forge.api.SceneExternalCommandEvent;
import io.github.kxyr0.echoscene.forge.api.SceneExternalCancelEvent;
import io.github.kxyr0.echoteleportmagic.api.TeleportDestination;
import io.github.kxyr0.echoteleportmagic.api.TeleportExecutionId;
import io.github.kxyr0.echoteleportmagic.api.TeleportState;
import io.github.kxyr0.echoteleportmagic.forge.EchoTeleportMagic;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.forge.api.navigation.EchoNpcNavigationBodyBridge;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class TeleportSceneBridge {
    @SubscribeEvent public void start(SceneExternalCommandEvent event) {
        var r = event.request();
        if (!r.command().receiver().equals("TeleportMagic")) return;
        try {
            if (!r.command().operation().equals("To") || event.player() == null) throw new IllegalArgumentException("Invalid teleport Scene");
            var location = event.locations().find(r.command().arguments().get(1)).orElseThrow();
            boolean player = r.command().capabilities().stream().anyMatch(c -> c.kind() == SceneCapabilityKind.PLAYER);
            LivingEntity subject = player ? event.player() : EchoNpcNavigationBodyBridge.lookup(event.player().getServer(),
                    new ActorId(r.command().arguments().get(0))).body().orElseThrow().mob();
            var id = TeleportExecutionId.owned(r.executionId().value(), r.command().stepId().value());
            var destination = new TeleportDestination(location.id(), location.dimension(), location.x(), location.y(), location.z(),
                    subject.getYRot(), subject.getXRot());
            var result = EchoTeleportMagic.service().startActor(subject, destination, id, r.executionId().value(), r.rootOwner(),
                    r.command().arguments().get(0),
                    r.command().span().sourceId().value() + ":" + r.command().span().start().line()
                            + ":" + r.command().span().start().column(), false,
                    e -> event.completion().complete(r.executionId(), r.command().stepId(),
                            e.state() == TeleportState.COMPLETED, e.state().name()));
            event.provide(result.state() == TeleportState.COMPLETED ? SceneCommandStart.completed()
                    : SceneCommandStart.waiting(id.value().toString()));
        } catch (RuntimeException failure) { event.provide(SceneCommandStart.failed(failure.getMessage())); }
    }
    @SubscribeEvent public void cancel(SceneExternalCancelEvent event) {
        EchoTeleportMagic.service().coordinator().cancelOwned(event.owner().value());
    }
}
