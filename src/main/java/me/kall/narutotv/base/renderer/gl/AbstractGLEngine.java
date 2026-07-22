package me.kall.narutotv.base.renderer.gl;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL30C.*;

public abstract class AbstractGLEngine {
    int[] pboArray;
    long frameCount;
    int program, vertexArray, buffer;

    int @Nullable [] textures;
    int width, height;

    final String fragmentSource, vertexSource;

    public AbstractGLEngine(String fragmentSource, String vertexSource) {
        this.fragmentSource = fragmentSource;
        this.vertexSource = vertexSource;
    }

    public synchronized void setup(int width, int height) {
        this.width = width;
        this.height = height;

        this.program = this.initProgram();
        this.initVaoVbo();
        this.textures = this.initTextures(width, height);
        this.pboArray = this.initPboArray(width, height);

        this.frameCount = 0L;
    }

    protected int initProgram() {
        int vertex = this.compileShader(GL_VERTEX_SHADER, this.vertexSource);
        int fragment = this.compileShader(GL_FRAGMENT_SHADER, this.fragmentSource);

        int program = glCreateProgram();
        glAttachShader(program, vertex);
        glAttachShader(program, fragment);
        glLinkProgram(program);
        glDeleteShader(vertex);
        glDeleteShader(fragment);

        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(program);
            glDeleteProgram(program);
            throw new RuntimeException("Shader link failed: " + log);
        }

        glUseProgram(program);
        glUniform1i(glGetUniformLocation(program, "uTexY"), 0);
        glUniform1i(glGetUniformLocation(program, "uTexU"), 1);
        glUniform1i(glGetUniformLocation(program, "uTexV"), 2);
        glUseProgram(0);

        return program;
    }

    protected abstract void initVaoVbo();

    protected int[] initTextures(int width, int height) {
        int wHalf = width / 2;
        int hHalf = height / 2;

        int[] textures = new int[3];
        glGenTextures(textures);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        glBindTexture(GL_TEXTURE_2D, textures[0]);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, width, height, 0, GL_RED, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        applyTexParams();

        glBindTexture(GL_TEXTURE_2D, textures[1]);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, wHalf, hHalf, 0, GL_RED, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        applyTexParams();

        glBindTexture(GL_TEXTURE_2D, textures[2]);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, wHalf, hHalf, 0, GL_RED, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        applyTexParams();

        glBindTexture(GL_TEXTURE_2D, 0);
        return textures;
    }

    protected int[] initPboArray(int width, int height) {
        long ySize = (long) width * height;
        long uvSize = (long) (width / 2) * (height / 2);

        int[] pboArray = new int[6];
        glGenBuffers(pboArray);

        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pboArray[0]); glBufferData(GL_PIXEL_UNPACK_BUFFER, ySize, GL_STREAM_DRAW);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pboArray[1]); glBufferData(GL_PIXEL_UNPACK_BUFFER, ySize, GL_STREAM_DRAW);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pboArray[2]); glBufferData(GL_PIXEL_UNPACK_BUFFER, uvSize, GL_STREAM_DRAW);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pboArray[3]); glBufferData(GL_PIXEL_UNPACK_BUFFER, uvSize, GL_STREAM_DRAW);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pboArray[4]); glBufferData(GL_PIXEL_UNPACK_BUFFER, uvSize, GL_STREAM_DRAW);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pboArray[5]); glBufferData(GL_PIXEL_UNPACK_BUFFER, uvSize, GL_STREAM_DRAW);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);

        return pboArray;
    }

    protected int compileShader(int type, String src) {
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

    protected void applyTexParams() {
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

        this.upload(this.textures[0], this.width, this.height, this.pboArray[uploadSlot]);
        this.upload(this.textures[1], this.width / 2, this.height / 2, this.pboArray[2 + uploadSlot]);
        this.upload(this.textures[2], this.width / 2, this.height / 2, this.pboArray[4 + uploadSlot]);

        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    void stage(int pbo, ByteBuffer src, int srcOffset, int length) {
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pbo);

        ByteBuffer dst = glMapBufferRange(GL_PIXEL_UNPACK_BUFFER, 0, length, GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT | GL_MAP_UNSYNCHRONIZED_BIT);
        if (dst != null) {
            MemoryUtil.memCopy(MemoryUtil.memSlice(src, srcOffset, length), dst);
            glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);
        }
    }

    void upload(int texName, int w, int h, int pbo) {
        glBindTexture(GL_TEXTURE_2D, texName);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pbo);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, w, h, GL_RED, GL_UNSIGNED_BYTE, 0L);
    }

    public abstract void render();

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
