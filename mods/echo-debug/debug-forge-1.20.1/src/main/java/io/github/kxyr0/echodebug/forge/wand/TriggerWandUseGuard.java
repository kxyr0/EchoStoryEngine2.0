package io.github.kxyr0.echodebug.forge.wand;

import io.github.kxyr0.echodebug.core.diagnostic.DebugDiagnosticCode;
import io.github.kxyr0.echodebug.forge.registry.EchoDebugItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class TriggerWandUseGuard {
    public DebugDiagnosticCode check(ServerPlayer player) {
        if (!player.hasPermissions(2)) {
            return DebugDiagnosticCode.DEBUG_PERMISSION_DENIED;
        }
        return held(player) ? DebugDiagnosticCode.OK : DebugDiagnosticCode.DEBUG_TRIGGER_WAND_NOT_HELD;
    }

    public static boolean held(Player player) {
        return isWand(player.getMainHandItem()) || isWand(player.getOffhandItem());
    }

    private static boolean isWand(ItemStack stack) {
        return stack.is(EchoDebugItems.TRIGGER_WAND.get());
    }
}
