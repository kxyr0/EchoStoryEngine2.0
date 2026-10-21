package io.github.kxyr0.echonpcinteraction.forge.proxy;

import net.minecraft.server.level.ServerPlayer;

record ProxyCleanup(boolean openedMenu, boolean proxyWasPassenger) {
    static ProxyCleanup clean(ServerPlayer proxy) {
        boolean menu = proxy.containerMenu != proxy.inventoryMenu;
        boolean passenger = proxy.isPassenger();
        if (menu) {
            proxy.closeContainer();
        }
        if (passenger) {
            proxy.stopRiding();
        }
        proxy.getInventory().clearContent();
        return new ProxyCleanup(menu, passenger);
    }
}
