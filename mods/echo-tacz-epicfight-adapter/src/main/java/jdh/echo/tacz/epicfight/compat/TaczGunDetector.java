package jdh.echo.tacz.epicfight.compat;

import com.tacz.guns.api.item.IGun;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class TaczGunDetector {
    private TaczGunDetector() {
    }

    public static boolean isGunHeld(Player player) {
        return isGun(player.getMainHandItem()) || isGun(player.getOffhandItem());
    }

    private static boolean isGun(ItemStack stack) {
        return !stack.isEmpty() && IGun.getIGunOrNull(stack) != null;
    }
}
