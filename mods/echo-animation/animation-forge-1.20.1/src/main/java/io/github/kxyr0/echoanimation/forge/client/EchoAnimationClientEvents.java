package io.github.kxyr0.echoanimation.forge.client;

import io.github.kxyr0.echoanimation.forge.client.dialogue.DialogueVisualFaceLayerBridge;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoanimation.echoscript.EchoScriptAnimationCompiler;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationClientServiceLookupEvent;
import io.github.kxyr0.echoanimation.forge.client.debug.FaceDebugOverlay;
import io.github.kxyr0.echoanimation.forge.client.reload.AnimationClientCatalogHotReloader;
import io.github.kxyr0.echoanimation.forge.client.reload.AnimationClientHotReloadWatcher;
import io.github.kxyr0.echoanimation.forge.client.skinning.render.SkinnedModelRenderer;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogState;
import io.github.kxyr0.echoanimation.forge.resource.AnimationResourceCandidateCompiler;
import io.github.kxyr0.echoanimation.forge.resource.AnimationResourceDiscovery;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

/**
 * Client lifecycle wiring for public visual presentation.
 */
public final class EchoAnimationClientEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final FaceLayerTextureCache textureCache = new FaceLayerTextureCache();
    private final FaceLayerQuadRenderer quadRenderer = new FaceLayerQuadRenderer(textureCache);
    private final AnimationClientFaceRenderService renderService;
    private final AnimationClientHotReloadWatcher hotReloadWatcher;

    private EchoAnimationClientEvents(AnimationCatalogState catalogState) {
        AnimationCatalogState state = Objects.requireNonNull(catalogState, "catalogState");
        java.nio.file.Path contentPackRoot = FMLPaths.GAMEDIR.get().resolve("story").resolve("content-pack");
        AnimationClientCatalogHotReloader catalogReloader = new AnimationClientCatalogHotReloader(
                state,
                new AnimationResourceDiscovery(),
                new AnimationResourceCandidateCompiler(new EchoScriptAnimationCompiler()),
                contentPackRoot);
        this.renderService = new AnimationClientFaceRenderService(state, quadRenderer);
        this.hotReloadWatcher = new AnimationClientHotReloadWatcher(
                contentPackRoot,
                task -> Minecraft.getInstance().execute(task),
                catalogReloader::reload);
    }

    public static void register(IEventBus modBus, AnimationCatalogState catalogState) {
        EchoAnimationClientEvents events = new EchoAnimationClientEvents(catalogState);
        SkinnedModelRenderer.registerShader(modBus);
        modBus.addListener(events::clearTextureCache);
        modBus.addListener(events::addPlayerFaceLayer);
        MinecraftForge.EVENT_BUS.addListener(events::onClientServiceLookup);
        MinecraftForge.EVENT_BUS.addListener(FaceDebugOverlay::renderHud);
        MinecraftForge.EVENT_BUS.addListener(events::onGameShuttingDown);
        events.hotReloadWatcher.start();
        DialogueVisualFaceLayerBridge.register(catalogState, events.textureCache);
    }

    private void onClientServiceLookup(AnimationClientServiceLookupEvent event) {
        event.provide(renderService);
    }

    private void clearTextureCache(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new TextureCacheReloadListener(textureCache));
        event.registerReloadListener(renderService.skinnedModelRenderer());
    }

    private void addPlayerFaceLayer(EntityRenderersEvent.AddLayers event) {
        int registered = 0;
        for (String skin : event.getSkins()) {
            if (event.getPlayerSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new EchoContentPlayerFaceLayer(renderer, renderService));
                registered++;
            }
        }
        LOGGER.info("Echo Animation Content Player face layer registered for {} player skin renderer(s).", registered);
    }

    private void onGameShuttingDown(GameShuttingDownEvent event) {
        hotReloadWatcher.close();
    }

    private static final class TextureCacheReloadListener extends SimplePreparableReloadListener<Void> {
        private final FaceLayerTextureCache textureCache;

        private TextureCacheReloadListener(FaceLayerTextureCache textureCache) {
            this.textureCache = textureCache;
        }

        @Override
        protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            return null;
        }

        @Override
        protected void apply(Void value, ResourceManager resourceManager, ProfilerFiller profiler) {
            textureCache.clear();
        }
    }
}
