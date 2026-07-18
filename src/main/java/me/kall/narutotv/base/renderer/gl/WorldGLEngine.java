package me.kall.narutotv.base.renderer.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.kall.narutotv.impl.world.data.BlockScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL30C.*;

public class WorldGLEngine extends GuiGLEngine {
    private int mvpUniformLocation = -1;

    private double leftBottomX, leftBottomY, leftBottomZ;
    private double leftTopX, leftTopY, leftTopZ;
    private double rightBottomX, rightBottomY, rightBottomZ;
    private double rightTopX, rightTopY, rightTopZ;

    private final double centerX;
    private final double centerY;
    private final double centerZ;

    private final double normalX;
    private final double normalY;
    private final double normalZ;

    private Boolean uploadedFrontFacing = null;

    public WorldGLEngine(String fragmentSource, String vertexSource, @NotNull BlockScreen screen) {
        super(fragmentSource, vertexSource);

        BlockPos leftBottom = screen.leftBottom;
        BlockPos leftTop = screen.leftTop;
        BlockPos rightBottom = screen.rightBottom;
        BlockPos rightTop = screen.rightTop;

        this.centerX = screen.centerX;
        this.centerY = screen.centerY;
        this.centerZ = screen.centerZ;

        this.leftBottomX = leftBottom.getX();
        this.leftBottomY = leftBottom.getY();
        this.leftBottomZ = leftBottom.getZ();

        this.leftTopX = leftTop.getX();
        this.leftTopY = leftTop.getY();
        this.leftTopZ = leftTop.getZ();

        this.rightBottomX = rightBottom.getX();
        this.rightBottomY = rightBottom.getY();
        this.rightBottomZ = rightBottom.getZ();

        this.rightTopX = rightTop.getX();
        this.rightTopY = rightTop.getY();
        this.rightTopZ = rightTop.getZ();

        boolean widthX = this.leftBottomX != this.rightBottomX;
        boolean widthY = this.leftBottomY != this.rightBottomY;
        boolean widthZ = this.leftBottomZ != this.rightBottomZ;

        boolean heightX = this.leftBottomX != this.leftTopX;
        boolean heightY = this.leftBottomY != this.leftTopY;
        boolean heightZ = this.leftBottomZ != this.leftTopZ;

        if (widthX && heightY) {
            if (this.leftTopY > this.leftBottomY) {
                this.leftTopY += 1.0;
                this.rightTopY += 1.0;
            } else {
                this.leftBottomY += 1.0;
                this.rightBottomY += 1.0;
            }

            if (this.rightBottomX > this.leftBottomX) {
                this.rightBottomX += 1.0;
                this.rightTopX += 1.0;
            } else {
                this.leftBottomX += 1.0;
                this.leftTopX += 1.0;
            }
        } else if (widthZ && heightY) {
            if (this.leftTopY > this.leftBottomY) {
                this.leftTopY += 1.0; this.rightTopY += 1.0;
            } else {
                this.leftBottomY += 1.0;
                this.rightBottomY += 1.0;
            }

            if (this.rightBottomZ > this.leftBottomZ) {
                this.rightBottomZ += 1.0;
                this.rightTopZ += 1.0;
            } else {
                this.leftBottomZ += 1.0;
                this.leftTopZ += 1.0;
            }
        } else if (widthX && heightZ) {
            if (this.leftTopZ > this.leftBottomZ) {
                this.leftTopZ += 1.0;
                this.rightTopZ += 1.0;
            } else {
                this.leftBottomZ += 1.0;
                this.rightBottomZ += 1.0;
            }

            if (this.rightBottomX > this.leftBottomX) {
                this.rightBottomX += 1.0;
                this.rightTopX += 1.0;
            } else {
                this.leftBottomX += 1.0;
                this.leftTopX += 1.0;
            }
        } else if (widthY && heightX) {
            if (this.leftTopX > this.leftBottomX) {
                this.leftTopX += 1.0;
                this.rightTopX += 1.0;
            } else {
                this.leftBottomX += 1.0;
                this.rightBottomX += 1.0;
            }

            if (this.rightBottomY > this.leftBottomY) {
                this.rightBottomY += 1.0;
                this.rightTopY += 1.0;
            } else {
                this.leftBottomY += 1.0;
                this.leftTopY += 1.0;
            }
        } else if (widthY && heightZ) {
            if (this.leftTopZ > this.leftBottomZ) {
                this.leftTopZ += 1.0;
                this.rightTopZ += 1.0;
            } else {
                this.leftBottomZ += 1.0; this.rightBottomZ += 1.0;
            }

            if (this.rightBottomY > this.leftBottomY) {
                this.rightBottomY += 1.0;
                this.rightTopY += 1.0;
            } else {
                this.leftBottomY += 1.0;
                this.leftTopY += 1.0;
            }
        } else if (widthZ && heightX) {
            if (this.leftTopX > this.leftBottomX) {
                this.leftTopX += 1.0;
                this.rightTopX += 1.0;
            } else {
                this.leftBottomX += 1.0;
                this.rightBottomX += 1.0;
            }

            if (this.rightBottomZ > this.leftBottomZ) {
                this.rightBottomZ += 1.0;
                this.rightTopZ += 1.0;
            } else {
                this.leftBottomZ += 1.0;
                this.leftTopZ += 1.0;
            }
        }

        double leftDeltaX = this.leftTopX - this.leftBottomX;
        double leftDeltaY = this.leftTopY - this.leftBottomY;
        double leftDeltaZ = this.leftTopZ - this.leftBottomZ;

        double rightDeltaX = this.rightBottomX - this.leftBottomX;
        double rightDeltaY = this.rightBottomY - this.leftBottomY;
        double rightDeltaZ = this.rightBottomZ - this.leftBottomZ;

        double normalX = leftDeltaY * rightDeltaZ - leftDeltaZ * rightDeltaY;
        double normalY = leftDeltaZ * rightDeltaX - leftDeltaX * rightDeltaZ;
        double normalZ = leftDeltaX * rightDeltaY - leftDeltaY * rightDeltaX;

        double length = Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);

        this.normalX = normalX / length;
        this.normalY = normalY / length;
        this.normalZ = normalZ / length;
    }

    @Override
    public synchronized void initTexture(int width, int height) {
        super.initTexture(width, height);
        this.mvpUniformLocation = glGetUniformLocation(this.program, "uMVP");
        this.uploadedFrontFacing = null;
    }

    public synchronized void render(PoseStack poseStack, Vec3 camera) {
        if (this.textures == null || this.buffer == 0) return;

        boolean frontFacing = (this.normalX * (camera.x - this.centerX) + this.normalY * (camera.y - this.centerY) + this.normalZ * (camera.z - this.centerZ)) > 0;

        if (this.uploadedFrontFacing == null || this.uploadedFrontFacing != frontFacing) this.uploadVertices(frontFacing);

        int prevProg = glGetInteger(GL_CURRENT_PROGRAM);
        int prevVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int prevActive = glGetInteger(GL_ACTIVE_TEXTURE);

        glActiveTexture(GL_TEXTURE0); int prevTex0 = glGetInteger(GL_TEXTURE_BINDING_2D);
        glActiveTexture(GL_TEXTURE1); int prevTex1 = glGetInteger(GL_TEXTURE_BINDING_2D);
        glActiveTexture(GL_TEXTURE2); int prevTex2 = glGetInteger(GL_TEXTURE_BINDING_2D);

        boolean wasBlend = glIsEnabled(GL_BLEND);
        boolean wasCullFace = glIsEnabled(GL_CULL_FACE);
        boolean wasDepthTest = glIsEnabled(GL_DEPTH_TEST);

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glDisable(GL_BLEND);

        glUseProgram(this.program);

        Matrix4f mvp = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(poseStack.last().pose());
        float[] mvpArray = new float[16];
        mvp.get(mvpArray);
        glUniformMatrix4fv(this.mvpUniformLocation, false, mvpArray);

        glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, this.textures[0]);
        glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D, this.textures[1]);
        glActiveTexture(GL_TEXTURE2); glBindTexture(GL_TEXTURE_2D, this.textures[2]);

        glBindVertexArray(this.vertexArray);
        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

        glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, prevTex0);
        glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D, prevTex1);
        glActiveTexture(GL_TEXTURE2); glBindTexture(GL_TEXTURE_2D, prevTex2);
        glActiveTexture(prevActive);

        glBindVertexArray(prevVao);
        glUseProgram(prevProg);

        if (!wasDepthTest) glDisable(GL_DEPTH_TEST);
        if (wasCullFace) glEnable(GL_CULL_FACE);
        if (wasBlend) glEnable(GL_BLEND);
    }

    private void uploadVertices(boolean frontFacing) {
        double normalX = this.normalX, normalY = this.normalY, normalZ = this.normalZ;

        if (!frontFacing) { normalX = -normalX; normalY = -normalY; normalZ = -normalZ; }

        double leftBottomX = this.leftBottomX, leftBottomY = this.leftBottomY, leftBottomZ = this.leftBottomZ;
        double leftTopX = this.leftTopX, leftTopY = this.leftTopY, leftTopZ = this.leftTopZ;
        double rightBottomX = this.rightBottomX, rightBottomY = this.rightBottomY, rightBottomZ = this.rightBottomZ;
        double rightTopX = this.rightTopX, rightTopY = this.rightTopY, rightTopZ = this.rightTopZ;

        if (frontFacing) {
            leftBottomX += normalX; leftBottomY += normalY; leftBottomZ += normalZ;
            leftTopX += normalX; leftTopY += normalY; leftTopZ += normalZ;
            rightBottomX += normalX; rightBottomY += normalY; rightBottomZ += normalZ;
            rightTopX += normalX; rightTopY += normalY; rightTopZ += normalZ;
        }

        double epsilon = 0.05;
        leftBottomX += normalX * epsilon; leftBottomY += normalY * epsilon; leftBottomZ += normalZ * epsilon;
        leftTopX += normalX * epsilon; leftTopY += normalY * epsilon; leftTopZ += normalZ * epsilon;
        rightBottomX += normalX * epsilon; rightBottomY += normalY * epsilon; rightBottomZ += normalZ * epsilon;
        rightTopX += normalX * epsilon; rightTopY += normalY * epsilon; rightTopZ += normalZ * epsilon;

        float[] vertArray = frontFacing ?  new float[]{
                (float) leftBottomX, (float) leftBottomY, (float) leftBottomZ, 1, 1,
                (float) leftTopX, (float) leftTopY, (float) leftTopZ, 1, 0,
                (float) rightTopX, (float) rightTopY, (float) rightTopZ, 0, 0,
                (float) rightBottomX, (float) rightBottomY, (float) rightBottomZ, 0, 1,
        } : new float[]{
                (float) leftBottomX, (float) leftBottomY, (float) leftBottomZ, 0, 1,
                (float) leftTopX, (float) leftTopY, (float) leftTopZ, 0, 0,
                (float) rightTopX, (float) rightTopY, (float) rightTopZ, 1, 0,
                (float) rightBottomX, (float) rightBottomY, (float) rightBottomZ, 1, 1,
        };

        glBindVertexArray(this.vertexArray);
        glBindBuffer(GL_ARRAY_BUFFER, this.buffer);

        FloatBuffer buffer = MemoryUtil.memAllocFloat(vertArray.length);
        try {
            buffer.put(vertArray).flip();
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(buffer);
        }

        int stride = 5 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        this.uploadedFrontFacing = frontFacing;
    }

    @Override public synchronized void render() {}
}