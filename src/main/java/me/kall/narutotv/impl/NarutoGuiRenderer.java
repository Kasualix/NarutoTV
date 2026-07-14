package me.kall.narutotv.impl;

import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.base.renderer.ByteBufferRenderer;
import me.kall.narutotv.base.renderer.NativeImageRenderer;
import me.kall.narutotv.fade.FadeApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public final class NarutoGuiRenderer {
    private static final AtomicReference<Engine> ACTIVE = new AtomicReference<>(Engine.YUV_GL);

    static {
        String endStr = System.getProperty(NarutoProperties.EARLY_END);
        String startStr = System.getProperty(NarutoProperties.EARLY_START);
        if (endStr != null && startStr != null) {
            double earlyEnd = (double) Long.parseLong(endStr);
            double earlyStart = (double) Long.parseLong(startStr);
            ACTIVE.get().renderer.restart((earlyEnd - earlyStart) / 1_000_000_000.0D);

            System.clearProperty(NarutoProperties.EARLY_END);
            System.clearProperty(NarutoProperties.EARLY_START);
        }
    }

    public static synchronized void switchType() {
        shutdown();
        if (ACTIVE.get().equals(Engine.NATIVE_IMAGE)) {
            ACTIVE.set(Engine.YUV_GL);
        } else {
            ACTIVE.set(Engine.NATIVE_IMAGE);
        }
    }

    public static boolean isRunning() {
        return ACTIVE.get().renderer.isRunning();
    }

    public static boolean isRunnable() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (screen instanceof WinScreen || screen instanceof GenericDirtMessageScreen) return true;
        if (screen != null && screen.isPauseScreen()) return true;
        if (minecraft.getOverlay() instanceof LoadingOverlay) return true;
        if (minecraft.level != null) {
            shutdown();
            return false;
        }

        return minecraft.isRunning();
    }

    public static void render() {
        ACTIVE.get().renderer.render();
    }

    public static void shutdown() {
        ACTIVE.get().renderer.shutdown();
    }

    enum Engine {
        NATIVE_IMAGE(new NativeImageRenderer() {
            @Override
            public boolean isRunnable() {
                return true;
            }

            @Override
            public @NotNull MediaArgs initMediaArgs() {
                return Engine.initMediaArgs();
            }

            @Override
            @Contract(" -> new")
            protected @NotNull ResourceLocation setLocation() {
                return ResourceLocation.fromNamespaceAndPath(NarutoTV.MOD_ID, "general_client_gui");
            }

            @Override
            public void onSetup(double seekTo) {
                super.onSetup(seekTo);

                FadeApi.getInstance().setUnfadable(this.textureLocation, true);
            }
        }),
        YUV_GL(new ByteBufferRenderer() {
            @Override
            protected String fragmentSource() {
                return """
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
            }

            @Override
            protected String vertexSource() {
                return """
                    #version 330 core
    
                    layout(location = 0) in vec2 Position;
    
                    out vec2 texCoord;
    
                    void main() {
                        texCoord = vec2(Position.x * 0.5 + 0.5, 0.5 - Position.y * 0.5);
                        gl_Position = vec4(Position, 0.0, 1.0);
                    }
                    """;
            }

            @Override
            public boolean isRunnable() {
                return true;
            }

            @Override
            public @NotNull MediaArgs initMediaArgs() {
                return Engine.initMediaArgs();
            }
        });

        final AbstractRenderer<?> renderer;

        Engine(AbstractRenderer<?> renderer) {
            this.renderer = renderer;
        }

        static MediaArgs initMediaArgs() {
            String initial = System.getProperty(NarutoProperties.INITIAL_MEDIA);
            MediaArgs mediaArgs;
            if (initial == null) {
                mediaArgs = Sources.roll();
            } else {
                mediaArgs = MediaArgs.fromString(initial);
                System.clearProperty(NarutoProperties.INITIAL_MEDIA);
            }
            return mediaArgs;
        }
    }
}