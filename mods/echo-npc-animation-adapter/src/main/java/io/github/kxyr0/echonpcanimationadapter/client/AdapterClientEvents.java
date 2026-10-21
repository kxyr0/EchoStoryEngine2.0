package io.github.kxyr0.echonpcanimationadapter.client;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderExtensionDescriptor;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderExtensionId;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderRegistrationEvent;
import org.slf4j.Logger;

/**
 * Client-only registration for the NPC head render extension.
 */
public final class AdapterClientEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final NpcClientRenderExtensionDescriptor DESCRIPTOR =
            new NpcClientRenderExtensionDescriptor(
                    new NpcClientRenderExtensionId("echo_npc_animation_adapter:face_layers"),
                    "Echo NPC Animation face layers");

    private AdapterClientEvents() {
    }

    public static void registerClient() {
        NpcClientRenderRegistrationEvent.registerGlobal(DESCRIPTOR, EchoNpcFaceAnimationExtension::new);
        LOGGER.info("Echo NPC Animation Adapter render extension registered: {}", DESCRIPTOR.id());
    }
}
