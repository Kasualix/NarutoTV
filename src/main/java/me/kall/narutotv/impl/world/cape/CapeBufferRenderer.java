package me.kall.narutotv.impl.world.cape;

import com.mojang.blaze3d.vertex.PoseStack;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.produce.audio.AudioProducer;
import me.kall.narutotv.base.renderer.ByteBufferRenderer;
import me.kall.narutotv.base.renderer.gl.AbstractGLEngine;
import me.kall.narutotv.base.renderer.gl.CapeGLEngine;
import me.kall.narutotv.impl.world.data.client.ClientVideoCapes;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CapeBufferRenderer extends ByteBufferRenderer {
    private final ClientVideoCapes.VideoCape videoCape;

    private final ThreadLocal<PoseStack> poseStack = new ThreadLocal<>();

    public CapeBufferRenderer(ClientVideoCapes.VideoCape videoCape) {
        this.videoCape = videoCape;
    }

    public void capture(PoseStack poseStack) {
        this.poseStack.set(poseStack);
    }

    public void deprecate() {
        this.poseStack.remove();
    }

    @Override
    public @Nullable CapeGLEngine engine() {
        return (CapeGLEngine) this.engine;
    }

    @Override
    protected AbstractGLEngine initEngine() {
        String fragmentSource = """
                #version 330 core

                uniform sampler2D uTexY;
                uniform sampler2D uTexU;
                uniform sampler2D uTexV;

                in vec2 texCoord;
                out vec4 fragColor;

                void main() {
                    float y = texture(uTexY, texCoord).r;
                    float u = texture(uTexU, texCoord).r;
                    float v = texture(uTexV, texCoord).r;

                    float c = y - 16.0 / 255.0;
                    float d = u - 128.0 / 255.0;
                    float e = v - 128.0 / 255.0;

                    float r = 1.164 * c + 1.596 * e;
                    float g = 1.164 * c - 0.392 * d - 0.813 * e;
                    float b = 1.164 * c + 2.017 * d;

                    fragColor = vec4(clamp(vec3(r, g, b), 0.0, 1.0), 1.0);
                }
                """;

        String vertexSource = """
                #version 330 core

                layout(location = 0) in vec3 Position;
                layout(location = 1) in vec2 TexCoord;

                uniform mat4 uMVP;

                out vec2 texCoord;

                void main() {
                    gl_Position = uMVP * vec4(Position, 1.0);
                    texCoord = TexCoord;
                }
                """;

        return new CapeGLEngine(fragmentSource, vertexSource, this.videoCape.mediaArgs());
    }

    @Override
    public @NotNull MediaArgs initMediaArgs() {
        return this.videoCape.mediaArgs();
    }

    @Override
    public boolean isRunnable() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            this.shutdown();
            return false;
        }
        return true;
    }

    @Override
    public void render() {
        super.render();
        CapeGLEngine engine = this.engine();
        if (engine != null) engine.render(this.poseStack.get());
    }

    @Override
    public float initVolume() {
        return 0.0F;
    }

    @Override
    public float getVolume() {
        return 0.0F;
    }

    @Override
    public void setVolume(float volume) {}

    @Override
    public @Nullable AudioProducer initAudio(double seekTo) {
        return null;
    }

    @Override
    public Runnable pauseAudio() {
        return () -> {};
    }

    @Override
    public Runnable resumeAudio() {
        return () -> {};
    }
}