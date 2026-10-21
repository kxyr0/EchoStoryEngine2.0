package io.github.kxyr0.echoitem.forge.client;

import io.github.kxyr0.echoitem.forge.ForgeItemRuntimeState;
import io.github.kxyr0.echoitem.forge.resource.ClientItemAssetStore;
import io.github.kxyr0.echoitem.forge.resource.ClientItemResourcePack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Client-only wiring for dynamic item resources and client catalog refresh.
 */
public final class EchoItemPackClient {
    private EchoItemPackClient() {
    }

    public static void bootstrap(IEventBus modBus, ForgeItemRuntimeState state) {
        ClientItemAssetStore assets = new ClientItemAssetStore(FMLPaths.GAMEDIR.get());
        ClientItemResourcePack pack = new ClientItemResourcePack(state.catalog(), assets);
        DynamicItemModelOverrides modelOverrides = new DynamicItemModelOverrides(state.catalog());
        modBus.addListener(pack::onAddPackFinders);
        modBus.addListener(modelOverrides::onRegisterAdditional);
        modBus.addListener(modelOverrides::onModifyBakingResult);
    }
}
