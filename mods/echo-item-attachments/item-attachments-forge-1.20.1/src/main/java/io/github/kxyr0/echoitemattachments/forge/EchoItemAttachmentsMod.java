package io.github.kxyr0.echoitemattachments.forge;

import io.github.kxyr0.echoitemattachments.forge.api.client.ClientAttachmentLookupEvent;
import io.github.kxyr0.echoitemattachments.forge.api.service.ItemAttachmentServiceLookupEvent;
import io.github.kxyr0.echoitemattachments.forge.command.AttachmentCommands;
import io.github.kxyr0.echoitemattachments.forge.network.AttachmentNetwork;
import io.github.kxyr0.echoitemattachments.forge.network.ClientAttachmentStore;
import io.github.kxyr0.echoitemattachments.forge.runtime.ServerAttachmentHost;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EchoItemAttachmentsMod.MOD_ID)
public final class EchoItemAttachmentsMod {
    public static final String MOD_ID = "echo_item_attachments";
    private final AttachmentNetwork network = new AttachmentNetwork();
    private volatile ServerAttachmentHost host;

    public EchoItemAttachmentsMod() {
        network.register();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                io.github.kxyr0.echoitemattachments.forge.client.EchoItemAttachmentsClient.register(
                        FMLJavaModLoadingContext.get().getModEventBus()));
        MinecraftForge.EVENT_BUS.addListener(this::started);
        MinecraftForge.EVENT_BUS.addListener(this::stopping);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::lookup);
        MinecraftForge.EVENT_BUS.addListener(this::login);
        MinecraftForge.EVENT_BUS.addListener(this::death);
        MinecraftForge.EVENT_BUS.addListener(ClientAttachmentStore::lookup);
        MinecraftForge.EVENT_BUS.addListener(new AttachmentCommands(() -> host)::register);
    }

    private void started(ServerStartedEvent event) {
        host = new ServerAttachmentHost(event.getServer(), network);
    }

    private void stopping(ServerStoppingEvent event) {
        host = null;
        ClientAttachmentStore.clear();
    }

    private void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && host != null) host.tick();
    }

    private void lookup(ItemAttachmentServiceLookupEvent event) {
        ServerAttachmentHost current = host;
        if (current != null && current.server() == event.server()) event.register(current);
    }

    private void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (host != null && event.getEntity() instanceof ServerPlayer player) {
            network.sendAll(player, host.records());
        }
    }

    private void death(LivingDeathEvent event) {
        if (host != null && event.getEntity() instanceof ServerPlayer player) {
            host.dropPlayerOnDeath(player.getUUID());
        }
    }
}
