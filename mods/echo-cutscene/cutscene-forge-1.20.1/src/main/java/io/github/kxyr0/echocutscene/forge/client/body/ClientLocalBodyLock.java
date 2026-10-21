package io.github.kxyr0.echocutscene.forge.client.body;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class ClientLocalBodyLock {
    private ClientLocalBodyLock() {
    }

    public static Optional<Snapshot> capture() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return Optional.empty();
        }
        return Optional.of(new Snapshot(player.getYRot(), player.getXRot(), player.getInventory().selected));
    }

    public static void apply(Snapshot snapshot) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            apply(player, snapshot);
        }
    }

    public static void apply(double yaw, double pitch) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            apply(player, new Snapshot((float) yaw, (float) pitch, player.getInventory().selected));
        }
    }

    private static void apply(LocalPlayer player, Snapshot snapshot) {
        player.setYRot(snapshot.yaw());
        player.setXRot(snapshot.pitch());
        player.yRotO = snapshot.yaw();
        player.xRotO = snapshot.pitch();
        player.setYHeadRot(snapshot.yaw());
        player.yHeadRotO = snapshot.yaw();
        player.yBodyRot = snapshot.yaw();
        player.yBodyRotO = snapshot.yaw();
        player.getInventory().selected = snapshot.selectedSlot();
    }

    public record Snapshot(float yaw, float pitch, int selectedSlot) {
    }
}
