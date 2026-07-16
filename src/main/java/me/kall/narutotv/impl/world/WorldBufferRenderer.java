package me.kall.narutotv.impl.world;

import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.base.renderer.ByteBufferRenderer;
import me.kall.narutotv.base.renderer.gl.GuiGLEngine;
import me.kall.narutotv.base.renderer.gl.WorldGLEngine;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

public class WorldBufferRenderer extends ByteBufferRenderer {
    private final BlockScreen screen;

    public WorldBufferRenderer(BlockScreen screen) {
        this.screen = screen;
    }

    @Override
    protected GuiGLEngine initEngine() {
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

        return new WorldGLEngine(fragmentSource, vertexSource, this.screen);
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
        return Sources.roll();
    }
}
