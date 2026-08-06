package me.kall.narutotv.core.renderer;

import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.core.world.light.LightAccessor;
import me.kall.narutotv.core.world.light.Lighter;
import me.kall.narutotv.data.world.Wall;
import me.kall.narutotv.produce.video.AbstractFrameProducer;
import me.kall.narutotv.produce.video.BufferFrameProducer;
import me.kall.narutotv.shader.AbstractGLEngine;
import me.kall.narutotv.shader.GuiGLEngine;
import me.kall.narutotv.shader.WorldGLEngine;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Arrays;

public abstract class BufferFrameRenderer implements FrameRenderer<ByteBuffer> {
    private final Loading loading = new Loading();
    private AbstractGLEngine engine;

    @Override
    public @NotNull AbstractFrameProducer<ByteBuffer> initVideo(@NotNull MediaArgs args) {
        return new BufferFrameProducer(args, 2);
    }

    @Override
    public void setup(@NotNull MediaArgs mediaArgs, double seekTo) {
        this.engine = this.initGLEngine(mediaArgs);
        this.update(mediaArgs, null);
    }

    protected abstract AbstractGLEngine initGLEngine(MediaArgs mediaArgs);

    @Override
    public void update(@NotNull MediaArgs mediaArgs, @Nullable ByteBuffer frame) {
        if (this.engine == null) return;
        if (frame == null) frame = this.loading.get(mediaArgs.width(), mediaArgs.height());
        this.engine.update(frame);
    }

    @Override
    public void render() {
        if (this.engine != null) this.engine.render();
    }

    @Override
    public void shutdown() {
        if (this.engine != null) {
            this.engine.shutdown();
            this.engine = null;
        }
    }

    private static final class Loading {
        private int width;
        private int height;

        private @Nullable ByteBuffer buffer;

        private @NotNull ByteBuffer get(int width, int height) {
            if (this.width == width && this.height == height && this.buffer != null) return this.buffer.rewind();

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(Color.WHITE);

            int fontSize = Math.max(12, height / 6);
            Font font = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
            graphics.setFont(font);
            FontMetrics metrics = graphics.getFontMetrics();
            String text = "Loading...";
            int textWidth = metrics.stringWidth(text);
            while (textWidth > width * 0.9 && fontSize > 10) {
                fontSize--;
                font = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
                graphics.setFont(font);
                metrics = graphics.getFontMetrics();
                textWidth = metrics.stringWidth(text);
            }

            int textHeight = metrics.getHeight();
            int x = (width - textWidth) / 2;
            int y = (height - textHeight) / 2 + metrics.getAscent();
            graphics.drawString(text, x, y);
            graphics.dispose();

            int ySize = width * height;
            int uvSize = (width / 2) * (height / 2);
            ByteBuffer buffer = ByteBuffer.allocateDirect(ySize + 2 * uvSize);

            for (int rgb : image.getRGB(0, 0, width, height, null, 0, width)) {
                buffer.put((byte) (int) (0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF)));
            }

            byte[] uvPlane = new byte[uvSize];
            Arrays.fill(uvPlane, (byte) 128);

            if (this.buffer != null) this.buffer.clear();

            this.buffer = buffer.put(uvPlane).put(uvPlane).flip();
            this.width = width;
            this.height = height;

            return this.buffer;
        }
    }

    public static final class Early extends BufferFrameRenderer  {
        @Override
        @Contract("_ -> new")
        protected @NotNull AbstractGLEngine initGLEngine(MediaArgs mediaArgs) {
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
            return new GuiGLEngine(fragmentSource, vertexSource, mediaArgs);
        }
    }

    public static final class Gui extends BufferFrameRenderer {
        @Contract("_ -> new")
        @Override
        protected @NotNull AbstractGLEngine initGLEngine(MediaArgs mediaArgs) {
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
                    texCoord = vec2(Position.x * 0.5 + 0.5, 0.5 - Position.y * 0.5);
                    gl_Position = vec4(Position, 0.0, 1.0);
                }
                """;

            return new GuiGLEngine(fragmentSource, vertexSource, mediaArgs);
        }
    }

    public static final class World extends BufferFrameRenderer implements LightAccessor {
        private final Wall wall;
        private final Lighter lighter;

        public World(Wall wall) {
            this.wall = wall;
            this.lighter = new Lighter(wall);
        }

        @Override
        @Contract("_ -> new")
        protected @NotNull AbstractGLEngine initGLEngine(MediaArgs mediaArgs) {
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
            return new WorldGLEngine(fragmentSource, vertexSource, mediaArgs, this.wall);
        }

        @Override
        public void update(@NotNull MediaArgs mediaArgs, @Nullable ByteBuffer frame) {
            super.update(mediaArgs, frame);
            if (frame != null) this.lighter.updateLight(Lighter.forBuffer(frame, mediaArgs.width(), mediaArgs.height()));
        }

        @Override
        public int getLight() {
            return this.lighter.getLight();
        }
    }
}
