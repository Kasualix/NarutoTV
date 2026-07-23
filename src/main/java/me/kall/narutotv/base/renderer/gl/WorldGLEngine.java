package me.kall.narutotv.base.renderer.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.impl.world.data.BlockScreen;
import me.kall.narutotv.impl.world.util.WorldMath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL30C.*;

public class WorldGLEngine extends AbstractGLEngine {
    private final Vec3[] corners = new Vec3[4];

    private final WorldMath.Bounds bounds;
    private final Vec3 normal;
    private final Vec3 center;

    private int mvpUniformLocation;

    private final ThreadLocal<PoseStack> poseStack = new ThreadLocal<>();
    private final ThreadLocal<Vec3> camera = new ThreadLocal<>();

    public WorldGLEngine(String fragmentSource, String vertexSource, @NotNull BlockScreen screen, MediaArgs mediaArgs) {
        super(fragmentSource, vertexSource, mediaArgs);

        BlockPos leftBottom = screen.leftBottom;
        BlockPos rightBottom = screen.rightBottom;
        BlockPos leftTop = screen.leftTop;
        BlockPos rightTop = screen.rightTop;

        this.corners[0] = new Vec3(leftBottom.getX(), leftBottom.getY(), leftBottom.getZ());
        this.corners[1] = new Vec3(rightBottom.getX(), rightBottom.getY(), rightBottom.getZ());
        this.corners[2] = new Vec3(leftTop.getX(), leftTop.getY(), leftTop.getZ());
        this.corners[3] = new Vec3(rightTop.getX(), rightTop.getY(), rightTop.getZ());

        this.bounds = WorldMath.computeBounds(this.corners);
        this.normal = WorldMath.computeNormal(this.corners);
        this.center = WorldMath.computeCenter(this.corners);
    }

    public void capture(PoseStack poseStack, Vec3 camera) {
        this.poseStack.set(poseStack);
        this.camera.set(camera);
    }

    public void deprecate() {
        this.poseStack.remove();
        this.camera.remove();
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

    @Contract(" -> new")
    private float @NotNull [] computeVertexData() {
        WorldMath.QuadData quad = WorldMath.computeQuad(this.corners, this.bounds, this.normal, this.center, this.camera.get(), this.width, this.height);
        return quad.toVertexArray();
    }

    @Override
    public synchronized void render() {
        if (this.textures == null || this.program == 0 || this.vertexArray == 0 || this.normal == null || this.camera.get() == null || this.poseStack.get() == null) return;

        float[] vertexData = this.computeVertexData();

        glBindBuffer(GL_ARRAY_BUFFER, this.buffer);
        FloatBuffer floatBuffer = MemoryUtil.memAllocFloat(vertexData.length);
        try {
            floatBuffer.put(vertexData).flip();
            glBufferData(GL_ARRAY_BUFFER, floatBuffer, GL_STREAM_DRAW);
        } finally {
            MemoryUtil.memFree(floatBuffer);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer mvpBuffer = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(this.poseStack.get().last().pose()).get(stack.mallocFloat(16));

            int prevProgram = glGetInteger(GL_CURRENT_PROGRAM);
            int prevVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
            int prevActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE);
            boolean wasCullFace = glIsEnabled(GL_CULL_FACE);

            glUseProgram(this.program);
            if (this.mvpUniformLocation != -1) glUniformMatrix4fv(this.mvpUniformLocation, false, mvpBuffer);

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, textures[0]);
            glActiveTexture(GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, textures[1]);
            glActiveTexture(GL_TEXTURE2);
            glBindTexture(GL_TEXTURE_2D, textures[2]);

            glDisable(GL_CULL_FACE);

            glBindVertexArray(this.vertexArray);
            glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

            if (wasCullFace) glEnable(GL_CULL_FACE);
            glBindVertexArray(prevVao);
            glUseProgram(prevProgram);
            glActiveTexture(prevActiveTexture);
        }
    }
}