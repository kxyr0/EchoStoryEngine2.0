package jdh.echo.animator.epicfight.client;

import jdh.echo.animator.epicfight.EchoAnimatorEpicFightMod;
import jdh.echo.animator.epicfight.client.render.EchoEpicFightFaceLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent;
import yesman.epicfight.client.renderer.patched.entity.PatchedEntityRenderer;
import yesman.epicfight.client.renderer.patched.entity.PatchedLivingEntityRenderer;

public final class ClientBootstrap {
    private static final String ECHO_FACE_LAYER_CLASS =
            "io.github.kxyr0.echoanimation.forge.client.EchoContentPlayerFaceLayer";
    private static boolean installed;
    private static boolean warnedMissingLayer;
    private static boolean warnedMissingRenderer;

    private ClientBootstrap() {
    }

    public static void register() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientBootstrap::onModifyPatchedRenderers);
    }

    private static void onModifyPatchedRenderers(PatchedRenderersEvent.Modify event) {
        PatchedEntityRenderer<?, ?, ?, ?> renderer = event.get(EntityType.PLAYER);
        if (!(renderer instanceof PatchedLivingEntityRenderer<?, ?, ?, ?, ?> livingRenderer)) {
            warnMissingPlayerRenderer(renderer);
            return;
        }

        Class<?> echoLayerClass = findEchoFaceLayerClass();
        if (echoLayerClass == null) {
            return;
        }

        installLayer(livingRenderer, echoLayerClass);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void installLayer(PatchedLivingEntityRenderer renderer, Class<?> echoLayerClass) {
        renderer.addPatchedLayerAlways(echoLayerClass, new EchoEpicFightFaceLayer());
        if (!installed) {
            EchoAnimatorEpicFightMod.LOGGER.info(
                    "Installed Epic Fight patched layer for Echo Animation face renderer {}",
                    ECHO_FACE_LAYER_CLASS);
            installed = true;
        }
    }

    private static Class<?> findEchoFaceLayerClass() {
        try {
            return Class.forName(ECHO_FACE_LAYER_CLASS, false, ClientBootstrap.class.getClassLoader());
        } catch (ClassNotFoundException exception) {
            if (!warnedMissingLayer) {
                EchoAnimatorEpicFightMod.LOGGER.warn(
                        "Echo Animation face layer class was not found: {}", ECHO_FACE_LAYER_CLASS);
                warnedMissingLayer = true;
            }
            return null;
        }
    }

    private static void warnMissingPlayerRenderer(PatchedEntityRenderer<?, ?, ?, ?> renderer) {
        if (!warnedMissingRenderer) {
            EchoAnimatorEpicFightMod.LOGGER.warn(
                    "Epic Fight player renderer is not a PatchedLivingEntityRenderer: {}", renderer);
            warnedMissingRenderer = true;
        }
    }
}
