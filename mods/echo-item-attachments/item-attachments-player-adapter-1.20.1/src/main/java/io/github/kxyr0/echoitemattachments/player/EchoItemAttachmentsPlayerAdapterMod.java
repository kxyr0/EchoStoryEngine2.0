package io.github.kxyr0.echoitemattachments.player;

import io.github.kxyr0.echoitemattachments.api.identity.SubjectKind;
import io.github.kxyr0.echoitemattachments.forge.api.anchor.AttachmentAnchorSampleEvent;
import io.github.kxyr0.echoitemattachments.forge.api.subject.AttachmentSubjectLookupEvent;
import java.util.UUID;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoItemAttachmentsPlayerAdapterMod.MOD_ID)
public final class EchoItemAttachmentsPlayerAdapterMod {
    public static final String MOD_ID = "echo_item_attachments_player_adapter";

    public EchoItemAttachmentsPlayerAdapterMod() {
        MinecraftForge.EVENT_BUS.addListener(this::subject);
        MinecraftForge.EVENT_BUS.addListener(this::anchor);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> PlayerClientRegistration::register);
    }

    private void subject(AttachmentSubjectLookupEvent event) {
        if (event.subject().kind() != SubjectKind.PLAYER) return;
        try {
            var player = event.server().getPlayerList().getPlayer(UUID.fromString(event.subject().value()));
            if (player != null) event.register(new PlayerSubjectAccess(event.server(), player));
        } catch (IllegalArgumentException ignored) {
            // Explicit malformed player ids never fall back to another online player.
        }
    }

    private void anchor(AttachmentAnchorSampleEvent event) {
        if (event.subject().kind() != SubjectKind.PLAYER
                || ModList.get().isLoaded("echo_animation")) return;
        try {
            var player = event.server().getPlayerList().getPlayer(UUID.fromString(event.subject().value()));
            if (player != null) event.register(PlayerVanillaAnchor.sample(
                    player, event.point(), player.serverLevel().getGameTime()));
        } catch (IllegalArgumentException ignored) {
            // No implicit player selection.
        }
    }
}
