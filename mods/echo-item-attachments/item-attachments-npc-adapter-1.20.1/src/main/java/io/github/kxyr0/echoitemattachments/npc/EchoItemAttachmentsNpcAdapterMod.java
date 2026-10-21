package io.github.kxyr0.echoitemattachments.npc;

import io.github.kxyr0.echoitemattachments.api.identity.SubjectKind;
import io.github.kxyr0.echoitemattachments.forge.api.anchor.AttachmentAnchorSampleEvent;
import io.github.kxyr0.echoitemattachments.forge.api.subject.AttachmentSubjectLookupEvent;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.forge.api.navigation.EchoNpcNavigationBodyBridge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoItemAttachmentsNpcAdapterMod.MOD_ID)
public final class EchoItemAttachmentsNpcAdapterMod {
    public static final String MOD_ID = "echo_item_attachments_npc_adapter";

    public EchoItemAttachmentsNpcAdapterMod() {
        MinecraftForge.EVENT_BUS.addListener(this::subject);
        MinecraftForge.EVENT_BUS.addListener(this::anchor);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> NpcClientRegistration::register);
    }

    private void subject(AttachmentSubjectLookupEvent event) {
        if (event.subject().kind() != SubjectKind.NPC) return;
        ActorId actor;
        try {
            actor = new ActorId(event.subject().value());
        } catch (IllegalArgumentException exception) {
            return;
        }
        var lookup = EchoNpcNavigationBodyBridge.lookup(event.server(), actor);
        if (lookup.status() == io.github.kxyr0.echonpc.forge.api.navigation.NpcNavigationBodyLookupResult.Status.RESOLVED) {
            event.register(new NpcSubjectAccess(lookup.body().orElseThrow()));
        }
    }

    private void anchor(AttachmentAnchorSampleEvent event) {
        if (event.subject().kind() != SubjectKind.NPC
                || ModList.get().isLoaded("echo_animation")) return;
        var lookup = EchoNpcNavigationBodyBridge.lookup(event.server(), new ActorId(event.subject().value()));
        lookup.body().ifPresent(body -> event.register(NpcVanillaAnchor.sample(
                body.mob(), event.point(), body.level().getGameTime())));
    }
}
