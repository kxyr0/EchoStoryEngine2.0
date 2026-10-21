package io.github.kxyr0.echocutscene.forge.client;

import io.github.kxyr0.echocutscene.api.camera.CameraPose;
import io.github.kxyr0.echocutscene.forge.api.client.DetachedCameraService;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;

final class ClientCameraRig implements DetachedCameraService {
    private Marker camera;

    @Override
    public boolean detached() {
        return camera != null && Minecraft.getInstance().getCameraEntity() == camera;
    }

    @Override
    public void detach(CameraPose initialPose) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        camera = new Marker(EntityType.MARKER, minecraft.level);
        apply(initialPose);
        minecraft.setCameraEntity(camera);
    }

    @Override
    public void attachToBody() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && Minecraft.getInstance().getCameraEntity() == camera) {
            minecraft.setCameraEntity(minecraft.player);
        }
        camera = null;
    }

    public void apply(CameraPose pose) {
        Entity target = camera;
        if (target == null) {
            return;
        }
        double x = pose.position().value().x();
        double y = pose.position().value().y();
        double z = pose.position().value().z();
        float yaw = (float) pose.rotation().yaw();
        float pitch = (float) pose.rotation().pitch();
        target.moveTo(x, y, z, yaw, pitch);
        target.yRotO = yaw;
        target.xRotO = pitch;
    }
}
