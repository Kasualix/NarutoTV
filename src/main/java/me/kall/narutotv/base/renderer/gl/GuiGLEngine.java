package me.kall.narutotv.base.renderer.gl;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL30C.*;

public class GuiGLEngine extends AbstractGLEngine {
    int program, vertexArray, buffer;

    int[] textures;
    int width, height;

    public GuiGLEngine(String fragmentSource, String vertexSource) {
        super(fragmentSource, vertexSource);
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

    public synchronized void update(ByteBuffer frame) {
        if (this.textures == null) return;
        if (!glIsTexture(this.textures[0]) || !glIsTexture(this.textures[1]) || !glIsTexture(this.textures[2])) return;

        if (this.pboArray == null) return;

        long frameCount = this.frameCount++;

        int ySize = this.width * this.height;
        int uvSize = (this.width / 2) * (this.height / 2);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);

        int current = (int) (frameCount & 1);
        int previous = 1 - current;

        this.stage(this.pboArray[current], frame, 0, ySize);
        this.stage(this.pboArray[2 + current], frame, ySize, uvSize);
        this.stage(this.pboArray[4 + current], frame, ySize + uvSize, uvSize);

        int uploadSlot = (frameCount == 0) ? current : previous;

        this.upload(this.textures[0], this.width, this.height, this.pboArray[uploadSlot]);
        this.upload(this.textures[1], this.width / 2, this.height / 2, this.pboArray[2 + uploadSlot]);
        this.upload(this.textures[2], this.width / 2, this.height / 2, this.pboArray[4 + uploadSlot]);

        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public synchronized void render() {
        if (this.textures == null) return;
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

    public synchronized void shutdown() {
        if (this.textures != null) glDeleteTextures(this.textures);
        if (this.pboArray != null) glDeleteBuffers(this.pboArray);
        if (this.buffer != 0) glDeleteBuffers(this.buffer);
        if (this.vertexArray != 0) glDeleteVertexArrays(this.vertexArray);
        if (this.program != 0) glDeleteProgram(this.program);
        this.buffer = 0;
        this.vertexArray = 0;
        this.program = 0;
        this.pboArray = null;
        this.textures = null;
        this.width = 0;
        this.height = 0;
    }
}