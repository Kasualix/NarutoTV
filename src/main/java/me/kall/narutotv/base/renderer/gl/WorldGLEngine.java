package me.kall.narutotv.base.renderer.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.kall.narutotv.impl.world.data.BlockScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL30C.*;

public class WorldGLEngine extends AbstractGLEngine {
    private static final double SURFACE_OFFSET = 0.01D;

    final BlockScreen screen;

    int mvpUniformLocation;
    int mirrorUniformLocation;

    Vec3 normal;

    public WorldGLEngine(String fragmentSource, String vertexSource, @NotNull BlockScreen screen) {
        super(fragmentSource, vertexSource);
        this.screen = screen;
    }

    @Override
    public synchronized void setup(int width, int height) {
        super.setup(width, height);
        this.mvpUniformLocation = glGetUniformLocation(this.program, "uMVP");
        this.mirrorUniformLocation = glGetUniformLocation(this.program, "uMirror");
    }

    @Override
    protected void initVaoVbo() {
        float[] vertices = this.genVertices();

        this.vertexArray = glGenVertexArrays();
        this.buffer = glGenBuffers();
        glBindVertexArray(this.vertexArray);
        glBindBuffer(GL_ARRAY_BUFFER, this.buffer);
        FloatBuffer buf = MemoryUtil.memAllocFloat(vertices.length);
        buf.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, buf, GL_STATIC_DRAW);
        MemoryUtil.memFree(buf);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private float @NotNull [] genVertices() {
        BlockPos leftBottom = this.screen.leftBottom;
        BlockPos rightBottom = this.screen.rightBottom;
        BlockPos leftTop = this.screen.leftTop;
        BlockPos rightTop = this.screen.rightTop;

        Vec3 bottomEdge = new Vec3(rightBottom.getX() - leftBottom.getX(), rightBottom.getY() - leftBottom.getY(), rightBottom.getZ() - leftBottom.getZ());
        Vec3 leftEdge = new Vec3(leftTop.getX() - leftBottom.getX(), leftTop.getY() - leftBottom.getY(), leftTop.getZ() - leftBottom.getZ());

        Vec3 normal = bottomEdge.cross(leftEdge);
        double length = normal.length();
        this.normal = length > 1.0E-6D ? normal.scale(1.0D / length) : new Vec3(0D, 1D, 0D);

        double offsetX = this.normal.x * SURFACE_OFFSET;
        double offsetY = this.normal.y * SURFACE_OFFSET;
        double offsetZ = this.normal.z * SURFACE_OFFSET;

        return new float[]{
                (float) (leftBottom.getX() + offsetX), (float) (leftBottom.getY() + offsetY), (float) (leftBottom.getZ() + offsetZ), 0f, 0f,
                (float) (rightBottom.getX() + offsetX), (float) (rightBottom.getY() + offsetY), (float) (rightBottom.getZ() + offsetZ), 1f, 0f,
                (float) (leftTop.getX() + offsetX), (float) (leftTop.getY() + offsetY), (float) (leftTop.getZ() + offsetZ), 0f, 1f,
                (float) (rightTop.getX() + offsetX), (float) (rightTop.getY() + offsetY), (float) (rightTop.getZ() + offsetZ), 1f, 1f
        };
    }

    public synchronized void render(PoseStack poseStack, Vec3 camera) {
        if (this.textures == null || this.program == 0 || this.vertexArray == 0 || this.normal == null) return;

        boolean mirrored = camera.subtract(this.screen.centerX, this.screen.centerY, this.screen.centerZ).dot(this.normal) < 0D;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer mvpBuffer = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(poseStack.last().pose()).get(stack.mallocFloat(16));

            int prevProgram = glGetInteger(GL_CURRENT_PROGRAM);
            int prevVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
            int prevActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE);
            boolean wasCullFace = glIsEnabled(GL_CULL_FACE);

            glUseProgram(this.program);
            if (this.mvpUniformLocation != -1) glUniformMatrix4fv(this.mvpUniformLocation, false, mvpBuffer);
            if (this.mirrorUniformLocation != -1) glUniform1i(this.mirrorUniformLocation, mirrored ? 1 : 0);

            glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, this.textures[0]);
            glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D, this.textures[1]);
            glActiveTexture(GL_TEXTURE2); glBindTexture(GL_TEXTURE_2D, this.textures[2]);

            glDisable(GL_CULL_FACE);

            glBindVertexArray(this.vertexArray);
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

            if (wasCullFace) glEnable(GL_CULL_FACE);
            glBindVertexArray(prevVao);
            glUseProgram(prevProgram);
            glActiveTexture(prevActiveTexture);
        }
    }

    @Override
    public synchronized void render() {
    }
}