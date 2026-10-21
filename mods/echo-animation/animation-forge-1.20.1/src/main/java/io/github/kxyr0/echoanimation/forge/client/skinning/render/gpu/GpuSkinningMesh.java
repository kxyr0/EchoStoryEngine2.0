package io.github.kxyr0.echoanimation.forge.client.skinning.render.gpu;

import com.mojang.blaze3d.vertex.BufferUploader;

import io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedMesh;
import io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedModel;
import io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedVertex;
import io.github.kxyr0.echoanimation.forge.client.skinning.model.weights.SkinWeights;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

/** Static source attributes plus a transform-feedback position/normal buffer and draw VAO. */
public final class GpuSkinningMesh implements AutoCloseable {
    private static final int SOURCE_STRIDE = 68;
    private static final int OUTPUT_STRIDE = 40;
    private final int vertexCount;
    private final int sourceBuffer;
    private final int outputBuffer;
    private final int transformVao;
    private final int drawVao;

    public GpuSkinningMesh(SkinnedModel model) {
        vertexCount = model.triangleVertexCount();
        sourceBuffer = GL15.glGenBuffers();
        outputBuffer = GL15.glGenBuffers();
        transformVao = GL30.glGenVertexArrays();
        drawVao = GL30.glGenVertexArrays();
        uploadSource(model);
        allocateOutput();
        configureTransformInput();
    }

    public int vertexCount() {
        return vertexCount;
    }

    public void captureTransformFeedback() {
        BufferUploader.invalidate();
        GL30.glBindVertexArray(transformVao);
        GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, outputBuffer);
        GL11.glEnable(GL30.GL_RASTERIZER_DISCARD);
        GL30.glBeginTransformFeedback(GL11.GL_POINTS);
        GL11.glDrawArrays(GL11.GL_POINTS, 0, vertexCount);
        GL30.glEndTransformFeedback();
        GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);
        GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, 0);
        GL30.glBindVertexArray(0);
    }

    public void bindForEntityShader(int overlay, int light, int entityId) {
        BufferUploader.invalidate();
        GL30.glBindVertexArray(drawVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, outputBuffer);
        floating(0, 3, GL11.GL_FLOAT, OUTPUT_STRIDE, 0);
        floating(5, 3, GL11.GL_FLOAT, OUTPUT_STRIDE, 12);
        floating(13, 4, GL11.GL_FLOAT, OUTPUT_STRIDE, 24);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, sourceBuffer);
        floating(2, 2, GL11.GL_FLOAT, SOURCE_STRIDE, 12);
        color(1);
        integer2(3, overlay);
        integer2(4, light);
        irisAttributes(entityId);
    }

    @Override
    public void close() {
        GL15.glDeleteBuffers(sourceBuffer);
        GL15.glDeleteBuffers(outputBuffer);
        GL30.glDeleteVertexArrays(transformVao);
        GL30.glDeleteVertexArrays(drawVao);
    }

    private void uploadSource(SkinnedModel model) {
        ByteBuffer data = BufferUtils.createByteBuffer(vertexCount * SOURCE_STRIDE);
        for (SkinnedMesh mesh : model.meshes()) {
            int[] triangles = mesh.triangles();
            for (int offset = 0; offset < triangles.length; offset += 3) {
                SkinnedVertex a = mesh.vertices().get(triangles[offset]);
                SkinnedVertex b = mesh.vertices().get(triangles[offset + 1]);
                SkinnedVertex c = mesh.vertices().get(triangles[offset + 2]);
                TriangleBasis basis = TriangleBasis.calculate(a, b, c);
                put(data, a, basis);
                put(data, b, basis);
                put(data, c, basis);
            }
        }
        data.flip();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, sourceBuffer);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
    }

    private void allocateOutput() {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, outputBuffer);
        GL15.glBufferData( GL15.GL_ARRAY_BUFFER, (long) vertexCount * OUTPUT_STRIDE, GL15.GL_DYNAMIC_DRAW);
    }

    private void configureTransformInput() {
        GL30.glBindVertexArray(transformVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, sourceBuffer);
        attribute(0, 3, GL11.GL_FLOAT, false, 0);
        GL20.glEnableVertexAttribArray(1);
        GL30.glVertexAttribIPointer(1, 4, GL11.GL_UNSIGNED_BYTE, SOURCE_STRIDE, 20);
        attribute(2, 4, GL11.GL_FLOAT, false, 24);
        attribute(3, 3, GL11.GL_BYTE, true, 40);
        attribute(4, 4, GL11.GL_FLOAT, false, 52);
        GL30.glBindVertexArray(0);
    }

    private void put(ByteBuffer data, SkinnedVertex vertex, TriangleBasis basis) {
        data.putFloat(vertex.x()).putFloat(vertex.y()).putFloat(vertex.z());
        data.putFloat(vertex.u()).putFloat(vertex.v());
        SkinWeights skin = vertex.skin();
        for (int slot = 0; slot < SkinWeights.LIMIT; slot++) {
            data.put((byte) skin.index(slot));
        }
        for (int slot = 0; slot < SkinWeights.LIMIT; slot++) {
            data.putFloat(skin.weight(slot));
        }
        data.put(normal(vertex.normalX())) .put(normal(vertex.normalY())) .put(normal(vertex.normalZ())) .put((byte) 0);
        data.putFloat(basis.midU()).putFloat(basis.midV());
        data.putFloat(basis.x()) .putFloat(basis.y()) .putFloat(basis.z()) .putFloat(basis.handedness());
    }

    private byte normal(float value) {
        return (byte) Math.round(Math.max(-1.0F, Math.min(1.0F, value)) * 127.0F);
    }

    private void attribute(int location, int size, int type, boolean normalized, long offset) {
        GL20.glEnableVertexAttribArray(location);
        GL20.glVertexAttribPointer(location, size, type, normalized, SOURCE_STRIDE, offset);
    }

    private void floating(int location, int size, int type, int stride, long offset) {
        GL20.glEnableVertexAttribArray(location);
        GL20.glVertexAttribPointer(location, size, type, false, stride, offset);
    }

    private void color(int location) {
        GL20.glDisableVertexAttribArray(location);
        GL20.glVertexAttrib4f(location, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void integer2(int location, int packed) {
        GL20.glDisableVertexAttribArray(location);
        GL30.glVertexAttribI2i(location, (short) (packed & 0xFFFF), (short) (packed >>> 16));
    }

    private void irisAttributes(int entityId) {
        GL20.glDisableVertexAttribArray(11);
        GL20.glVertexAttrib4f(11, entityId, 0.0F, 0.0F, 0.0F);
        floating(12, 2, GL11.GL_FLOAT, SOURCE_STRIDE, 44);
        GL20.glDisableVertexAttribArray(14);
        GL20.glVertexAttrib3f(14, 0.0F, 0.0F, 0.0F);
    }
}
