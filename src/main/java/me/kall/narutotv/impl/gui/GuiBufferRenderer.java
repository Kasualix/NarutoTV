package me.kall.narutotv.impl.gui;

import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.base.renderer.ByteBufferRenderer;
import me.kall.narutotv.base.renderer.gl.GuiGLEngine;
import me.kall.narutotv.impl.agent.NarutoProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import org.jetbrains.annotations.NotNull;

public class GuiBufferRenderer extends ByteBufferRenderer {
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
                        texCoord = vec2(Position.x * 0.5 + 0.5, 0.5 - Position.y * 0.5);
                        gl_Position = vec4(Position, 0.0, 1.0);
                    }
                    """;
        return new GuiGLEngine(fragmentSource, vertexSource);
    }

    @Override
    public boolean isRunnable() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (screen instanceof WinScreen || screen instanceof GenericDirtMessageScreen) return true;
        if (screen != null && screen.isPauseScreen()) return true;
        if (minecraft.getOverlay() instanceof LoadingOverlay) return true;
        if (minecraft.level != null) {
            this.shutdown();
            return false;
        }

        return minecraft.isRunning();
    }

    @Override
    public @NotNull MediaArgs initMediaArgs() {
        MediaArgs initial = NarutoProperties.sync();
        return initial == null ? Sources.get() : initial;
    }
}
