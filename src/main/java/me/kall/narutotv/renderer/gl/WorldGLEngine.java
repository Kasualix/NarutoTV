package me.kall.narutotv.renderer.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.context.RenderCaptured;
import me.kall.narutotv.data.world.wall.Wall;
import me.kall.narutotv.util.NarutoMath;
import me.kall.narutotv.world.api.RenderCoordsEvent;
import net.minecraft.client.Camera;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL46C.*;

public class WorldGLEngine extends AbstractGLEngine {
    private int mvpUniformLocation;

    private final Wall wall;

    public WorldGLEngine(String fragmentSource, String vertexSource, @NotNull MediaArgs mediaArgs, Wall wall) {
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

    @Override
    public void render() {
        Camera camera = RenderCaptured.camera();
        PoseStack poseStack = RenderCaptured.poseStack();

        if (this.textures == null || this.program == 0 || this.vertexArray == 0 || camera == null || poseStack == null) return;

        NarutoMath.Coords coords = NarutoMath.computeCoords(this.wall, camera);

        NeoForge.EVENT_BUS.post(new RenderCoordsEvent(coords, this.wall.dimension));

        float[] vertexData = new float[]{
                (float) coords.bottomFromX(), (float) coords.bottomFromY(), (float) coords.bottomFromZ(), coords.u0(), coords.v0(),
                (float) coords.bottomToX(), (float) coords.bottomToY(), (float) coords.bottomToZ(), coords.u1(), coords.v1(),
                (float) coords.topFromX(), (float) coords.topFromY(), (float) coords.topFromZ(), coords.u3(), coords.v3(),
                (float) coords.topToX(), (float) coords.topToY(), (float) coords.topToZ(), coords.u2(), coords.v2(),
        };

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
}
