package me.kall.narutotv.base.renderer.gl;

import me.kall.narutotv.app.data.MediaArgs;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL30C.*;

public class GuiGLEngine extends AbstractGLEngine {
    public GuiGLEngine(String fragmentSource, String vertexSource, MediaArgs mediaArgs) {
        super(fragmentSource, vertexSource, mediaArgs);
    }

    @Override
    protected void initVaoVbo() {
        float[] vertices = {-1F, -1F, 1F, -1F, -1F, 1F, 1F, 1F};

        this.vertexArray = glGenVertexArrays();
        this.buffer = glGenBuffers();

        glBindVertexArray(this.vertexArray);
        glBindBuffer(GL_ARRAY_BUFFER, this.buffer);

        FloatBuffer floatBuffer = MemoryUtil.memAllocFloat(vertices.length);
        try {
            floatBuffer.put(vertices).flip();
            glBufferData(GL_ARRAY_BUFFER, floatBuffer, GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(floatBuffer);
        }

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0L);
        glEnableVertexAttribArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public synchronized void render() {
        if (!this.running) this.setup();
        int prevProg = glGetInteger(GL_CURRENT_PROGRAM);
        int prevVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int prevActive = glGetInteger(GL_ACTIVE_TEXTURE);

        glActiveTexture(GL_TEXTURE0); int prevTex0 = glGetInteger(GL_TEXTURE_BINDING_2D);
        glActiveTexture(GL_TEXTURE1); int prevTex1 = glGetInteger(GL_TEXTURE_BINDING_2D);
        glActiveTexture(GL_TEXTURE2); int prevTex2 = glGetInteger(GL_TEXTURE_BINDING_2D);

        int prevFbo = glGetInteger(GL_FRAMEBUFFER_BINDING);
        int[] prevViewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, prevViewport);

        boolean wasBlend = glIsEnabled(GL_BLEND);
        boolean wasDepthTest = glIsEnabled(GL_DEPTH_TEST);
        boolean wasScissorTest = glIsEnabled(GL_SCISSOR_TEST);
        boolean wasCullFace = glIsEnabled(GL_CULL_FACE);
        boolean prevDepthMask = glGetBoolean(GL_DEPTH_WRITEMASK);

        glDisable(GL_BLEND);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_SCISSOR_TEST);
        glDisable(GL_CULL_FACE);
        glDepthMask(false);

        glUseProgram(this.program);

        glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, this.textures[0]);
        glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D, this.textures[1]);
        glActiveTexture(GL_TEXTURE2); glBindTexture(GL_TEXTURE_2D, this.textures[2]);

        glBindVertexArray(this.vertexArray);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, prevTex0);
        glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D, prevTex1);
        glActiveTexture(GL_TEXTURE2); glBindTexture(GL_TEXTURE_2D, prevTex2);
        glActiveTexture(prevActive);

        glBindVertexArray(prevVao);
        glUseProgram(prevProg);

        glBindFramebuffer(GL_FRAMEBUFFER, prevFbo);
        glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);

        glDepthMask(prevDepthMask);
        if (wasCullFace) glEnable(GL_CULL_FACE);
        if (wasScissorTest) glEnable(GL_SCISSOR_TEST);
        if (wasDepthTest) glEnable(GL_DEPTH_TEST);
        if (wasBlend) glEnable(GL_BLEND);
    }
}