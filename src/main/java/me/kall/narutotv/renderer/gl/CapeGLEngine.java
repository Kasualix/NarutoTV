package me.kall.narutotv.renderer.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.context.RenderCaptured;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL46C.*;

public class CapeGLEngine extends AbstractGLEngine {
    private int mvpUniformLocation;

    public CapeGLEngine(String fragmentSource, String vertexSource, @NotNull MediaArgs mediaArgs) {
        super(fragmentSource, vertexSource, mediaArgs);
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

        float[] vertexData = new float[]{
                -0.3125F, 0.0F, 0.0F, 0.0F, 0.0F,
                +0.3125F, 0.0F, 0.0F, 1.0F, 0.0F,
                -0.3125F, 1.0F, 0.0F, 0.0F, 1.0F,
                +0.3125F, 1.0F, 0.0F, 1.0F, 1.0F,
        };

        FloatBuffer floatBuffer = MemoryUtil.memAllocFloat(vertexData.length);
        try {
            floatBuffer.put(vertexData).flip();
            glBufferData(GL_ARRAY_BUFFER, floatBuffer, GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(floatBuffer);
        }

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    @Override
    public void render() {
        PoseStack poseStack = RenderCaptured.poseStack();
        if (this.textures == null || this.program == 0 || this.vertexArray == 0 || poseStack == null) return;

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
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

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
