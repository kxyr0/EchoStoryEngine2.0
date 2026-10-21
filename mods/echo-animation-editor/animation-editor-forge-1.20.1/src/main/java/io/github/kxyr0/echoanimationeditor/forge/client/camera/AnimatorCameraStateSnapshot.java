package io.github.kxyr0.echoanimationeditor.forge.client.camera;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public record AnimatorCameraStateSnapshot(Entity cameraEntity, CameraType cameraType, boolean hideGui) {
    public static AnimatorCameraStateSnapshot capture(Minecraft minecraft) {
        return new AnimatorCameraStateSnapshot(minecraft.getCameraEntity(),
                minecraft.options.getCameraType(), minecraft.options.hideGui);
    }

    public void restore(Minecraft minecraft) {
        Entity target = cameraEntity != null ? cameraEntity : minecraft.player;
        if (target != null) {
            minecraft.setCameraEntity(target);
        }
        minecraft.options.setCameraType(cameraType);
        minecraft.options.hideGui = hideGui;
    }
}
