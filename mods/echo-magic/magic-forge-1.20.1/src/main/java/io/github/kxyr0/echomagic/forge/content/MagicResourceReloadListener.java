package io.github.kxyr0.echomagic.forge.content;

import io.github.kxyr0.echomagic.forge.service.ForgeMagicServices;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public final class MagicResourceReloadListener implements ResourceManagerReloadListener {
    private final ForgeSpellContentLoader loader;
    private final ForgeMagicServices services;
    public MagicResourceReloadListener(ForgeSpellContentLoader loader, ForgeMagicServices services) {
        this.loader = loader;
        this.services = services;
    }
    @Override public void onResourceManagerReload(ResourceManager resourceManager) { services.accept(loader.load(resourceManager)); }
}
