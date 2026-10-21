package io.github.kxyr0.echoobservation.forge.client.projection;

import io.github.kxyr0.echoobservation.api.model.ObservationAnchor;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class ObservationRenderFrame {
    private final Matrix4f worldToClip;
    private final Vec3 camera;
    private final int framebufferWidth;
    private final int framebufferHeight;

    private ObservationRenderFrame(Matrix4f worldToClip, Vec3 camera, int width, int height) {
        this.worldToClip = new Matrix4f(worldToClip);
        this.camera = camera;
        framebufferWidth = width;
        framebufferHeight = height;
    }

    public static ObservationRenderFrame capture(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Matrix4f worldToClip = new Matrix4f(event.getProjectionMatrix())
                .mul(event.getPoseStack().last().pose());
        return new ObservationRenderFrame(worldToClip, event.getCamera().getPosition(),
                minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
    }

    static ObservationRenderFrame create(Matrix4f view, Matrix4f projection,
            Vec3 camera, int width, int height) {
        return new ObservationRenderFrame(new Matrix4f(projection).mul(view), camera, width, height);
    }

    public Optional<ObservationScreenPoint> project(ObservationAnchor anchor) {
        Vector4f clip = new Vector4f((float) (anchor.worldX() - camera.x),
                (float) (anchor.worldY() - camera.y), (float) (anchor.worldZ() - camera.z), 1.0F)
                .mul(worldToClip);
        if (!clip.isFinite() || clip.w() <= 0.001F) return Optional.empty();
        double ndcX = clip.x() / clip.w();
        double ndcY = clip.y() / clip.w();
        double ndcZ = clip.z() / clip.w();
        if (Math.abs(ndcX) > 1.0D || Math.abs(ndcY) > 1.0D || ndcZ < -1.0D || ndcZ > 1.0D) {
            return Optional.empty();
        }
        double x = (ndcX * 0.5D + 0.5D) * framebufferWidth;
        double y = (0.5D - ndcY * 0.5D) * framebufferHeight;
        return Optional.of(new ObservationScreenPoint(x, y, clip.w(), ndcX, ndcY));
    }

    public int framebufferWidth() {
        return framebufferWidth;
    }

    public int framebufferHeight() {
        return framebufferHeight;
    }
}
