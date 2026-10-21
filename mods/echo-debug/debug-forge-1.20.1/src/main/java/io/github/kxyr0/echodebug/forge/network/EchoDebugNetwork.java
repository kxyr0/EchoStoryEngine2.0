package io.github.kxyr0.echodebug.forge.network;

import io.github.kxyr0.echodebug.core.diagnostic.DebugOperationResult;
import io.github.kxyr0.echodebug.core.selection.TriggerSelectionSnapshot;
import io.github.kxyr0.echodebug.forge.EchoDebugMod;
import io.github.kxyr0.echodebug.forge.creation.TriggerCreationResult;
import io.github.kxyr0.echodebug.forge.creation.TriggerCreationService;
import io.github.kxyr0.echodebug.forge.integration.OptionalTriggerAreaCatalog;
import io.github.kxyr0.echodebug.forge.network.catalog.ServerboundAreaCatalogRequestPacket;
import io.github.kxyr0.echodebug.forge.wand.TriggerWandSessionStore;
import io.github.kxyr0.echotrigger.forge.api.authoring.TriggerAuthoringServiceBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class EchoDebugNetwork {
    private static final String PROTOCOL = "4";
    private static SimpleChannel channel;
    private static TriggerWandSessionStore sessions;
    private static TriggerCreationService creationService;
    private static int nextMessageId;

    private EchoDebugNetwork() {
    }

    public static void register(TriggerWandSessionStore sessionStore, TriggerCreationService creation) {
        sessions = sessionStore;
        creationService = creation;
        channel = NetworkRegistry.newSimpleChannel(new ResourceLocation(EchoDebugMod.MOD_ID, "main"),
                () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
        registerMessages();
    }

    public static TriggerWandSessionStore sessions() {
        return sessions;
    }

    public static TriggerCreationService creationService() {
        return creationService;
    }

    public static void sendSelection(
            ServerPlayer player,
            DebugOperationResult<TriggerSelectionSnapshot> result,
            boolean openCreationScreen
    ) {
        channel.send(PacketDistributor.PLAYER.with(() -> player),
                ClientboundSelectionPacket.from(result, openCreationScreen));
    }

    public static void sendCreationResult(ServerPlayer player, TriggerCreationResult result) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), ClientboundCreationResultPacket.from(result));
    }

    public static void sendCatalog(ServerPlayer player) {
        var lookup = TriggerAuthoringServiceBridge.lookup(player.getServer());
        lookup.service().ifPresent(service -> {
            var areas = OptionalTriggerAreaCatalog.append(player.getServer(), service.areas());
            channel.send(PacketDistributor.PLAYER.with(() -> player), new ClientboundAreaCatalogPacket(areas));
        });
    }

    public static void sendNpcTrail(ClientboundNpcTrailPacket packet) {
        channel.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void sendToServer(Object packet) {
        channel.sendToServer(packet);
    }

    private static void registerMessages() {
        channel.messageBuilder(ServerboundSelectionActionPacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundSelectionActionPacket::encode)
                .decoder(ServerboundSelectionActionPacket::decode)
                .consumerMainThread(ServerboundSelectionActionPacket::handle).add();
        channel.messageBuilder(ServerboundCreateTriggerPacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundCreateTriggerPacket::encode)
                .decoder(ServerboundCreateTriggerPacket::decode)
                .consumerMainThread(ServerboundCreateTriggerPacket::handle).add();
        channel.messageBuilder(ServerboundAreaCatalogRequestPacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundAreaCatalogRequestPacket::encode)
                .decoder(ServerboundAreaCatalogRequestPacket::decode)
                .consumerMainThread(ServerboundAreaCatalogRequestPacket::handle).add();
        channel.messageBuilder(ClientboundSelectionPacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundSelectionPacket::encode)
                .decoder(ClientboundSelectionPacket::decode)
                .consumerMainThread(ClientboundSelectionPacket::handle).add();
        channel.messageBuilder(ClientboundCreationResultPacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundCreationResultPacket::encode)
                .decoder(ClientboundCreationResultPacket::decode)
                .consumerMainThread(ClientboundCreationResultPacket::handle).add();
        channel.messageBuilder(ClientboundAreaCatalogPacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundAreaCatalogPacket::encode)
                .decoder(ClientboundAreaCatalogPacket::decode)
                .consumerMainThread(ClientboundAreaCatalogPacket::handle).add();
        channel.messageBuilder(ClientboundNpcTrailPacket.class, nextMessageId++,
                NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundNpcTrailPacket::encode)
                .decoder(ClientboundNpcTrailPacket::decode)
                .consumerMainThread(ClientboundNpcTrailPacket::handle).add();
    }
}
