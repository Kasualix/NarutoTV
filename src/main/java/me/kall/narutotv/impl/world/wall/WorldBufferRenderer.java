package me.kall.narutotv.impl.world.wall;

import com.mojang.blaze3d.vertex.PoseStack;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.produce.audio.AudioProducer;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.base.renderer.ByteBufferRenderer;
import me.kall.narutotv.base.renderer.gl.AbstractGLEngine;
import me.kall.narutotv.base.renderer.gl.WorldGLEngine;
import me.kall.narutotv.impl.world.data.Wall;
import me.kall.narutotv.impl.world.ext.InWorld;
import me.kall.narutotv.impl.world.sound.LocalSoundDelegate;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WorldBufferRenderer extends ByteBufferRenderer implements InWorld {
    private final Wall wall;
    private final LocalSoundDelegate soundDelegate;

    private final ThreadLocal<PoseStack> poseStack = new ThreadLocal<>();
    private final ThreadLocal<Camera> camera = new ThreadLocal<>();

    public WorldBufferRenderer(Wall wall) {
        this.wall = wall;
        this.soundDelegate = new LocalSoundDelegate(wall, this::life, super::getVolume, super::setVolume, super::initAudio, super::pauseAudio, super::resumeAudio);
    }

    public void capture(PoseStack poseStack, Camera camera) {
        this.poseStack.set(poseStack);
        this.camera.set(camera);
    }

    public void deprecate() {
        this.poseStack.remove();
        this.camera.remove();
    }

    @Override
    public @Nullable WorldGLEngine engine() {
        return (WorldGLEngine) this.engine;
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

        return new WorldGLEngine(fragmentSource, vertexSource, this.wall, this.mediaArgs());
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
    public @NotNull MediaArgs initMediaArgs() {
        return Sources.get();
    }

    @Override
    public Wall wall() {
        return this.wall;
    }

    public void render() {
        super.render();
        WorldGLEngine engine = this.engine();
        if (engine != null) engine.render(this.poseStack.get(), this.camera.get());
    }

    @Override
    public synchronized void shutdown() {
        super.shutdown();
        this.soundDelegate.shutdown();
    }

    @Override
    public float initVolume() {
        return this.wall().volume;
    }

    @Override
    public float getVolume() {
        return this.soundDelegate.getVolume();
    }

    @Override
    public void setVolume(float volume) {
        this.soundDelegate.setVolume(volume);
    }

    @Override
    public @Nullable AudioProducer initAudio(double seekTo) {
        return this.soundDelegate.initAudio(seekTo);
    }

    @Override
    public Runnable pauseAudio() {
        return this.soundDelegate.pauseAudio();
    }

    @Override
    public Runnable resumeAudio() {
        return this.soundDelegate.resumeAudio();
    }
}