package io.github.kxyr0.echonpcanimationadapter;

import io.github.kxyr0.echonpcanimationadapter.command.AdapterAnimationCommands;
import io.github.kxyr0.echonpcanimationadapter.client.AdapterClientEvents;
import io.github.kxyr0.echonpcanimationadapter.lifecycle.NpcAnimationLifecycleAdapter;
import io.github.kxyr0.echonpcanimationadapter.lifecycle.NpcNavigationLocomotionAnimator;
import io.github.kxyr0.echonpcanimationadapter.resource.NpcAnimationBindingCatalogState;
import io.github.kxyr0.echonpcanimationadapter.resource.NpcAnimationBindingCompiler;
import io.github.kxyr0.echonpcanimationadapter.resource.NpcAnimationBindingDiscovery;
import io.github.kxyr0.echonpcanimationadapter.resource.NpcAnimationBindingReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Thin Forge entrypoint for the public Echo NPC to Echo Animation adapter.
 */
@Mod(EchoNpcAnimationAdapterMod.MOD_ID)
public final class EchoNpcAnimationAdapterMod {
    public static final String MOD_ID = "echo_npc_animation_adapter";
    private final NpcAnimationBindingCatalogState bindings = new NpcAnimationBindingCatalogState();
    private final NpcAnimationLifecycleAdapter lifecycle = new NpcAnimationLifecycleAdapter(bindings);
    private final NpcNavigationLocomotionAnimator locomotion = new NpcNavigationLocomotionAnimator(lifecycle);

    public EchoNpcAnimationAdapterMod() {
        MinecraftForge.EVENT_BUS.addListener(this::onReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(locomotion::onNavigationLifecycle);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> AdapterClientEvents::registerClient);
    }

    private void onReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new NpcAnimationBindingReloadListener(
                new NpcAnimationBindingDiscovery(),
                new NpcAnimationBindingCompiler(),
                bindings,
                FMLPaths.GAMEDIR.get().resolve("story").resolve("content-pack")));
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        new AdapterAnimationCommands(bindings, lifecycle).register(event.getDispatcher());
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            lifecycle.tick(event.getServer());
            locomotion.tick(event.getServer());
        }
    }
}
