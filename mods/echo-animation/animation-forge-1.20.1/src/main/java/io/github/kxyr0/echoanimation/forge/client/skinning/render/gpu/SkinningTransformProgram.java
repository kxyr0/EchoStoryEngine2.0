package io.github.kxyr0.echoanimation.forge.client.skinning.render.gpu;

import com.mojang.logging.LogUtils;
import java.nio.FloatBuffer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

/** Raster-free dual-quaternion skinning whose output can enter any entity shader. */
public final class SkinningTransformProgram {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CAPACITY = 48;
    private static final String SOURCE = """
            #version 150
            in vec3 Position;
            in uvec4 BoneIndices;
            in vec4 BoneWeights;
            in vec3 Normal;
            in vec4 Tangent;
            uniform vec4 BoneReal[48];
            uniform vec4 BoneDual[48];
            uniform int RootOnly;
            out vec3 tfPosition;
            out vec3 tfNormal;
            out vec4 tfTangent;
            vec4 multiplyQuat(vec4 left, vec4 right) {
                return vec4(left.w * right.xyz + right.w * left.xyz + cross(left.xyz, right.xyz),
                    left.w * right.w - dot(left.xyz, right.xyz));
            }
            vec3 rotateQuat(vec4 rotation, vec3 value) {
                return value + 2.0 * cross(rotation.xyz,
                    cross(rotation.xyz, value) + rotation.w * value);
            }
            void main() {
                vec4 r0 = BoneReal[int(BoneIndices.x)];
                vec4 r1 = BoneReal[int(BoneIndices.y)];
                vec4 r2 = BoneReal[int(BoneIndices.z)];
                vec4 r3 = BoneReal[int(BoneIndices.w)];
                vec4 d0 = BoneDual[int(BoneIndices.x)];
                vec4 d1 = BoneDual[int(BoneIndices.y)];
                vec4 d2 = BoneDual[int(BoneIndices.z)];
                vec4 d3 = BoneDual[int(BoneIndices.w)];
                float s1 = dot(r0, r1) < 0.0 ? -1.0 : 1.0;
                float s2 = dot(r0, r2) < 0.0 ? -1.0 : 1.0;
                float s3 = dot(r0, r3) < 0.0 ? -1.0 : 1.0;
                vec4 real = r0 * BoneWeights.x + r1 * BoneWeights.y * s1
                    + r2 * BoneWeights.z * s2 + r3 * BoneWeights.w * s3;
                vec4 dual = d0 * BoneWeights.x + d1 * BoneWeights.y * s1
                    + d2 * BoneWeights.z * s2 + d3 * BoneWeights.w * s3;
                if (RootOnly != 0) {
                    real = BoneReal[0];
                    dual = BoneDual[0];
                }
                float inverseLength = 1.0 / max(length(real), 0.000001);
                real *= inverseLength;
                dual *= inverseLength;
                dual -= real * dot(real, dual);
                vec3 translation = 2.0 * multiplyQuat(dual, vec4(-real.xyz, real.w)).xyz;
                tfPosition = rotateQuat(real, Position) + translation;
                tfNormal = normalize(rotateQuat(real, Normal));
                tfTangent = vec4(normalize(rotateQuat(real, Tangent.xyz)), Tangent.w);
                gl_Position = vec4(tfPosition, 1.0);
            }
            """;
    private final float[] realData = new float[CAPACITY * 4];
    private final float[] dualData = new float[CAPACITY * 4];
    private final FloatBuffer realBuffer = BufferUtils.createFloatBuffer(CAPACITY * 4);
    private final FloatBuffer dualBuffer = BufferUtils.createFloatBuffer(CAPACITY * 4);
    private final Matrix4f identity = new Matrix4f();
    private final Quaternionf rotation = new Quaternionf();
    private final Vector3f translation = new Vector3f();
    private int program;
    private int realLocation;
    private int dualLocation;
    private int rootOnlyLocation;

    public void deform(GpuSkinningMesh mesh, Matrix4f[] palette, boolean rootOnly) {
        ensureProgram();
        fillPalette(palette);
        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GL20.glUseProgram(program);
        GL20.glUniform4fv(realLocation, realBuffer);
        GL20.glUniform4fv(dualLocation, dualBuffer);
        GL20.glUniform1i(rootOnlyLocation, rootOnly ? 1 : 0);
        mesh.captureTransformFeedback();
        GL20.glUseProgram(previousProgram);
    }

    public void close() {
        if (program != 0) {
            GL20.glDeleteProgram(program);
            program = 0;
        }
    }

    private void ensureProgram() {
        if (program != 0) {
            return;
        }
        int vertex = compileVertex();
        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertex);
        GL20.glBindAttribLocation(program, 0, "Position");
        GL20.glBindAttribLocation(program, 1, "BoneIndices");
        GL20.glBindAttribLocation(program, 2, "BoneWeights");
        GL20.glBindAttribLocation(program, 3, "Normal");
        GL20.glBindAttribLocation(program, 4, "Tangent");
        GL30.glTransformFeedbackVaryings(program, new CharSequence[]{"tfPosition", "tfNormal", "tfTangent"},
                GL30.GL_INTERLEAVED_ATTRIBS);
        GL20.glLinkProgram(program);
        GL20.glDeleteShader(vertex);
        requireStatus(program, GL20.GL_LINK_STATUS, "link");
        realLocation = GL20.glGetUniformLocation(program, "BoneReal[0]");
        dualLocation = GL20.glGetUniformLocation(program, "BoneDual[0]");
        rootOnlyLocation = GL20.glGetUniformLocation(program, "RootOnly");
        if (realLocation < 0 || dualLocation < 0 || rootOnlyLocation < 0) {
            throw new IllegalStateException("Transform-feedback skinning uniforms are missing");
        }
        LOGGER.info("GPU dual-quaternion skinning transform-feedback program linked");
    }

    private int compileVertex() {
        int shader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(shader, SOURCE);
        GL20.glCompileShader(shader);
        requireStatus(shader, GL20.GL_COMPILE_STATUS, "compile");
        return shader;
    }

    private void requireStatus(int object, int property, String operation) {
        int status = property == GL20.GL_LINK_STATUS
                ? GL20.glGetProgrami(object, property) : GL20.glGetShaderi(object, property);
        if (status == 0) {
            String log = property == GL20.GL_LINK_STATUS
                    ? GL20.glGetProgramInfoLog(object) : GL20.glGetShaderInfoLog(object);
            throw new IllegalStateException("Cannot " + operation + " GPU skinning transform: " + log);
        }
    }

    private void fillPalette(Matrix4f[] palette) {
        for (int index = 0; index < CAPACITY; index++) {
            Matrix4f matrix = index < palette.length ? palette[index] : identity;
            matrix.getUnnormalizedRotation(rotation).normalize();
            matrix.getTranslation(translation);
            int offset = index * 4;
            realData[offset] = rotation.x;
            realData[offset + 1] = rotation.y;
            realData[offset + 2] = rotation.z;
            realData[offset + 3] = rotation.w;
            dualData[offset] = 0.5F * (translation.x * rotation.w + translation.y * rotation.z
                    - translation.z * rotation.y);
            dualData[offset + 1] = 0.5F * (-translation.x * rotation.z + translation.y * rotation.w
                    + translation.z * rotation.x);
            dualData[offset + 2] = 0.5F * (translation.x * rotation.y - translation.y * rotation.x
                    + translation.z * rotation.w);
            dualData[offset + 3] = -0.5F * (translation.x * rotation.x + translation.y * rotation.y
                    + translation.z * rotation.z);
        }
        realBuffer.clear();
        realBuffer.put(realData).flip();
        dualBuffer.clear();
        dualBuffer.put(dualData).flip();
    }
}
