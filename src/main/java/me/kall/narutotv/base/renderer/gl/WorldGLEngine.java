package me.kall.narutotv.base.renderer.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.impl.world.data.Wall;
import me.kall.narutotv.impl.world.util.NarutoMath;
import net.minecraft.client.Camera;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL30C.*;

public class WorldGLEngine extends AbstractGLEngine {
    private int mvpUniformLocation;

    private final Wall wall;

    private final ThreadLocal<PoseStack> poseStack = new ThreadLocal<>();
    private final ThreadLocal<Camera> camera = new ThreadLocal<>();

    public WorldGLEngine(String fragmentSource, String vertexSource, @NotNull Wall wall, MediaArgs mediaArgs) {
        super(fragmentSource, vertexSource, mediaArgs);
        this.wall = wall;
    }

    @Override
    public synchronized void setup() {
        super.setup();
        this.mvpUniformLocation = glGetUniformLocation(this.program, "uMVP");
    }

    @Override
    protected void initVaoVbo() {
        this.vertexArray = glGenVertexArrays();
        this.buffer = glGenBuffers();

        glBindVertexArray(this.vertexArray);
        glBindBuffer(GL_ARRAY_BUFFER, this.buffer);

        glBufferData(GL_ARRAY_BUFFER, (long) 4 * 5 * Float.BYTES, GL_STREAM_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    @Override public void render() {}

    public synchronized void render(PoseStack poseStack, Camera camera) {
        if (this.textures == null || this.program == 0 || this.vertexArray == 0 || camera == null || poseStack == null) return;

        float[] vertexData = prepare(NarutoMath.computeCoords(this.wall, camera));

        int prevProgram = glGetInteger(GL_CURRENT_PROGRAM);
        int prevVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int prevActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE);
        boolean wasBlend = glIsEnabled(GL_BLEND);
        boolean wasDepthTest = glIsEnabled(GL_DEPTH_TEST);
        boolean wasDepthMask = glGetBoolean(GL_DEPTH_WRITEMASK);
        int depthFunc = glGetInteger(GL_DEPTH_FUNC);
        boolean wasCullFace = glIsEnabled(GL_CULL_FACE);

        glActiveTexture(GL_TEXTURE0); int prevTex0 = glGetInteger(GL_TEXTURE_BINDING_2D);
        glActiveTexture(GL_TEXTURE1); int prevTex1 = glGetInteger(GL_TEXTURE_BINDING_2D);
        glActiveTexture(GL_TEXTURE2); int prevTex2 = glGetInteger(GL_TEXTURE_BINDING_2D);

        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);

        glUseProgram(this.program);

        Matrix4f mvp = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(poseStack.last().pose());
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer matrixBuffer = stack.mallocFloat(16);
            mvp.get(matrixBuffer);
            glUniformMatrix4fv(this.mvpUniformLocation, false, matrixBuffer);
        }

        glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, this.textures[0]);
        glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D, this.textures[1]);
        glActiveTexture(GL_TEXTURE2); glBindTexture(GL_TEXTURE_2D, this.textures[2]);

        glBindVertexArray(this.vertexArray);
        glBindBuffer(GL_ARRAY_BUFFER, this.buffer);

        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertexData.length);
        try {
            vertexBuffer.put(vertexData).flip();
            glBufferSubData(GL_ARRAY_BUFFER, 0L, vertexBuffer);
        } finally {
            MemoryUtil.memFree(vertexBuffer);
        }

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, prevTex0);
        glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D, prevTex1);
        glActiveTexture(GL_TEXTURE2); glBindTexture(GL_TEXTURE_2D, prevTex2);

        glBindVertexArray(prevVao);
        glUseProgram(prevProgram);
        glActiveTexture(prevActiveTexture);
        if (wasBlend) glEnable(GL_BLEND); else glDisable(GL_BLEND);
        if (!wasDepthTest) glDisable(GL_DEPTH_TEST);
        glDepthMask(wasDepthMask);
        glDepthFunc(depthFunc);
        if (wasCullFace) glEnable(GL_CULL_FACE); else glDisable(GL_CULL_FACE);
    }

    @Contract("_ -> new")
    private static float @NotNull [] prepare(NarutoMath.@NotNull Coords coords) {
        double offsetX = coords.normalX() * 0.1, offsetY = coords.normalY() * 0.1, offsetZ = coords.normalZ() * 0.1;

        return new float[]{
                (float) (coords.bottomFromX() + offsetX), (float) (coords.bottomFromY() + offsetY), (float) (coords.bottomFromZ() + offsetZ), coords.u0(), coords.v0(),
                (float) (coords.bottomToX() + offsetX), (float) (coords.bottomToY() + offsetY), (float) (coords.bottomToZ() + offsetZ), coords.u1(), coords.v1(),
                (float) (coords.topFromX() + offsetX), (float) (coords.topFromY() + offsetY), (float) (coords.topFromZ() + offsetZ), coords.u3(), coords.v3(),
                (float) (coords.topToX() + offsetX), (float) (coords.topToY() + offsetY), (float) (coords.topToZ() + offsetZ), coords.u2(), coords.v2(),
        };
    }
}