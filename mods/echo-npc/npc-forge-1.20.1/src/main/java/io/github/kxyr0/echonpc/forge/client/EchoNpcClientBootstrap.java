package io.github.kxyr0.echonpc.forge.client;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echonpc.forge.EchoNpcMod;
import io.github.kxyr0.echonpc.forge.api.client.attachment.NpcBodyAttachmentRenderRegistrationEvent;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderRegistrationEvent;
import io.github.kxyr0.echonpc.forge.client.attachment.NpcBodyAttachmentExtensionSet;
import io.github.kxyr0.echonpc.forge.registry.EchoNpcEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Client-only renderer and resource lifecycle registration.
 */
@Mod.EventBusSubscriber(
        modid = EchoNpcMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class EchoNpcClientBootstrap {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final SkinResourceCache SKINS =
            new SkinResourceCache();

    private static final SkinModelVariantCache SKIN_MODEL_VARIANTS =
            new SkinModelVariantCache();

    private EchoNpcClientBootstrap() {
    }

    public static SkinResourceCache skins() {
        return SKINS;
    }

    static SkinModelVariantCache skinModelVariants() {
        return SKIN_MODEL_VARIANTS;
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event) {

        event.registerEntityRenderer(
                EchoNpcEntities.ECHO_NPC.get(),
                EchoNpcEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerRenderExtensions(
            EntityRenderersEvent.AddLayers event) {

        EchoNpcEntityRenderer renderer =
                event.getRenderer(
                        EchoNpcEntities.ECHO_NPC.get());

        if (renderer == null) {
            return;
        }

        NpcClientRenderExtensionSet extensions =
                collectRenderExtensions();

        NpcBodyAttachmentExtensionSet attachments =
                collectBodyAttachments();

        if (extensions.empty() && attachments.empty()) {
            return;
        }

        renderer.installRenderExtensions(extensions);
        renderer.installBodyAttachmentExtensions(attachments);

        renderer.addLayer(
                new NpcClientRenderExtensionLayer(
                        renderer,
                        extensions,
                        attachments));
    }

    @SubscribeEvent
    public static void clearSkinCache(
            RegisterClientReloadListenersEvent event) {

        SKINS.clear();
        SKIN_MODEL_VARIANTS.clear();
    }

    private static NpcClientRenderExtensionSet
            collectRenderExtensions() {

        NpcClientRenderRegistrationEvent event =
                new NpcClientRenderRegistrationEvent();

        MinecraftForge.EVENT_BUS.post(event);

        List<NpcClientRenderRegistrationEvent.Registration> registrations =
                new ArrayList<>();

        registrations.addAll(
                event.closeAndSnapshot());

        registrations.addAll(
                NpcClientRenderRegistrationEvent
                        .globalRegistrations());

        List<NpcClientRenderExtensionSet.Binding> bindings =
                new ArrayList<>();

        for (NpcClientRenderRegistrationEvent.Registration registration
                : registrations) {

            try {
                bindings.add(
                        new NpcClientRenderExtensionSet.Binding(
                                registration.descriptor(),
                                registration.factory().create()));
            } catch (RuntimeException exception) {
                LOGGER.warn(
                        "Echo NPC render extension factory failed: {}",
                        registration.descriptor().id(),
                        exception);
            }
        }

        LOGGER.info(
                "Echo NPC render extensions collected: {}",
                bindings.size());

        return new NpcClientRenderExtensionSet(bindings);
    }

    private static NpcBodyAttachmentExtensionSet
            collectBodyAttachments() {

        NpcBodyAttachmentRenderRegistrationEvent event =
                new NpcBodyAttachmentRenderRegistrationEvent();

        MinecraftForge.EVENT_BUS.post(event);

        List<NpcBodyAttachmentRenderRegistrationEvent.Registration> registrations =
                new ArrayList<>();

        registrations.addAll(
                event.closeAndSnapshot());

        registrations.addAll(
                NpcBodyAttachmentRenderRegistrationEvent
                        .globalRegistrations());

        List<NpcBodyAttachmentExtensionSet.Binding> bindings =
                new ArrayList<>();

        for (NpcBodyAttachmentRenderRegistrationEvent.Registration registration
                : registrations) {

            try {
                bindings.add(
                        new NpcBodyAttachmentExtensionSet.Binding(
                                registration.descriptor(),
                                registration.factory().get()));
            } catch (RuntimeException exception) {
                LOGGER.warn(
                        "Echo NPC body attachment factory failed: {}",
                        registration.descriptor().id(),
                        exception);
            }
        }

        LOGGER.info(
                "Echo NPC body attachment extensions collected: {}",
                bindings.size());

        return new NpcBodyAttachmentExtensionSet(bindings);
    }
}
