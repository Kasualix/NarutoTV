package me.kall.narutotv.impl.agent;

import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.base.renderer.ByteBufferRenderer;
import me.kall.narutotv.base.renderer.gl.GuiGLEngine;
import me.kall.narutotv.impl.NarutoProperties;
import org.jetbrains.annotations.NotNull;

public class NarutoEarlyRenderer extends ByteBufferRenderer {
    private static final NarutoEarlyRenderer INSTANCE = new NarutoEarlyRenderer();

    @SuppressWarnings("unused")
    public static void bridge() {
        INSTANCE.render();
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
    
                    layout(location = 0) in vec2 Position;
    
                    out vec2 texCoord;
    
                    void main() {
                        texCoord = vec2(Position.x * 0.5 + 0.5, 0.5 + Position.y * 0.5);
                        gl_Position = vec4(Position, 0.0, 1.0);
                    }
                    """;
        return new GuiGLEngine(fragmentSource, vertexSource, this.mediaArgs());
    }

    @Override
    public boolean isRunnable() {
        String shutdown = System.getProperty(NarutoProperties.SHUTDOWN);
        if (shutdown == null) {
            if (System.getProperty(NarutoProperties.EARLY_START) == null) System.setProperty(NarutoProperties.EARLY_START, String.valueOf(System.nanoTime()));
            return true;
        }
        this.shutdown();
        return false;
    }

    @Override
    public float initVolume() {
        return 1.0F;
    }

    @Override
    public @NotNull MediaArgs initMediaArgs() {
        MediaArgs mediaArgs;
        String initial = System.getProperty(NarutoProperties.INITIAL_MEDIA);
        if (initial == null) {
            mediaArgs = Sources.get();
            System.setProperty(NarutoProperties.INITIAL_MEDIA, mediaArgs.toString());
        } else {
            mediaArgs = MediaArgs.fromString(initial);
        }
        return mediaArgs;
    }
}
