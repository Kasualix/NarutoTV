package me.kall.narutotv.base.renderer.gl;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL30C.*;

public class YuvGLEngine {
    private int[] pboArray;
    private long frameCount;
    private int program, vao, vbo;

    private int @Nullable [] textures;
    private int width, height;

    private final String fragmentSource, vertexSource;

    public YuvGLEngine(String fragmentSource, String vertexSource) {
        this.fragmentSource = fragmentSource;
        this.vertexSource = vertexSource;
    }

    public synchronized void initTexture(int width, int height) {
        this.width = width;
        this.height = height;

        int vertex = this.compileShader(GL_VERTEX_SHADER, this.vertexSource);
        int fragment = this.compileShader(GL_FRAGMENT_SHADER, this.fragmentSource);

        this.program = glCreateProgram();
        glAttachShader(this.program, vertex);
        glAttachShader(this.program, fragment);
        glLinkProgram(this.program);
        glDeleteShader(vertex);
        glDeleteShader(fragment);

        if (glGetProgrami(this.program, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(this.program);
            glDeleteProgram(this.program);
            throw new RuntimeException("Shader link failed: " + log);
        }

        glUseProgram(this.program);
        glUniform1i(glGetUniformLocation(this.program, "uTexY"), 0);
        glUniform1i(glGetUniformLocation(this.program, "uTexU"), 1);
        glUniform1i(glGetUniformLocation(this.program, "uTexV"), 2);
        glUseProgram(0);

        float[] vertexList = {-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f};

        this.vao = glGenVertexArrays();
        this.vbo = glGenBuffers();

        glBindVertexArray(this.vao);
        glBindBuffer(GL_ARRAY_BUFFER, this.vbo);

        FloatBuffer floatBuffer = MemoryUtil.memAllocFloat(vertexList.length);
        try {
            floatBuffer.put(vertexList).flip();
            glBufferData(GL_ARRAY_BUFFER, floatBuffer, GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(floatBuffer);
        }

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0L);
        glEnableVertexAttribArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        int wHalf = width / 2;
        int hHalf = height / 2;

        this.textures = new int[3];
        glGenTextures(textures);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        glBindTexture(GL_TEXTURE_2D, this.textures[0]);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, width, height, 0, GL_RED, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        this.applyTexParams();

        glBindTexture(GL_TEXTURE_2D, this.textures[1]);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, wHalf, hHalf, 0, GL_RED, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        this.applyTexParams();

        glBindTexture(GL_TEXTURE_2D, this.textures[2]);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, wHalf, hHalf, 0, GL_RED, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        this.applyTexParams();

        glBindTexture(GL_TEXTURE_2D, 0);

        int[] old = this.pboArray;
        if (old != null) glDeleteBuffers(old);

        long ySize = (long) width * height;
        long uvSize = (long) (width / 2) * (height / 2);

        this.pboArray = new int[6];
        glGenBuffers(this.pboArray);

        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, this.pboArray[0]); glBufferData(GL_PIXEL_UNPACK_BUFFER, ySize, GL_STREAM_DRAW);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, this.pboArray[1]); glBufferData(GL_PIXEL_UNPACK_BUFFER, ySize, GL_STREAM_DRAW);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, this.pboArray[2]); glBufferData(GL_PIXEL_UNPACK_BUFFER, uvSize, GL_STREAM_DRAW);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, this.pboArray[3]); glBufferData(GL_PIXEL_UNPACK_BUFFER, uvSize, GL_STREAM_DRAW);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, this.pboArray[4]); glBufferData(GL_PIXEL_UNPACK_BUFFER, uvSize, GL_STREAM_DRAW);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, this.pboArray[5]); glBufferData(GL_PIXEL_UNPACK_BUFFER, uvSize, GL_STREAM_DRAW);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);

        this.frameCount = 0L;
    }

    private int compileShader(int type, String src) {
        int id = glCreateShader(type);
        glShaderSource(id, src);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(id);
            glDeleteShader(id);
            throw new RuntimeException("Shader compile failed: " + log);
        }
        return id;
    }

    private void applyTexParams() {
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
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

        this.upload(textures[0], this.width, this.height, this.pboArray[uploadSlot]);
        this.upload(textures[1], this.width / 2, this.height / 2, this.pboArray[2 + uploadSlot]);
        this.upload(textures[2], this.width / 2, this.height / 2, this.pboArray[4 + uploadSlot]);

        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private void stage(int pbo, ByteBuffer src, int srcOffset, int length) {
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pbo);

        ByteBuffer dst = glMapBufferRange(GL_PIXEL_UNPACK_BUFFER, 0, length, GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT | GL_MAP_UNSYNCHRONIZED_BIT);
        if (dst != null) {
            MemoryUtil.memCopy(MemoryUtil.memSlice(src, srcOffset, length), dst);
            glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);
        }
    }

    private void upload(int texName, int w, int h, int pbo) {
        glBindTexture(GL_TEXTURE_2D, texName);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pbo);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, w, h, GL_RED, GL_UNSIGNED_BYTE, 0L);
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

        glBindVertexArray(this.vao);
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
        if (this.vbo != 0) glDeleteBuffers(this.vbo);
        if (this.vao != 0) glDeleteVertexArrays(this.vao);
        if (this.program != 0) glDeleteProgram(this.program);
        this.vbo = 0;
        this.vao = 0;
        this.program = 0;
        this.pboArray = null;
        this.textures = null;
        this.width = 0;
        this.height = 0;
    }
}