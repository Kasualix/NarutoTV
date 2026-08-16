package me.kall.narutotv.override;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.config.NarutoConfig;
import me.kall.narutotv.core.AbstractTV;
import me.kall.narutotv.data.file.Sources;
import me.kall.narutotv.data.system.RenderProps;
import me.kall.narutotv.renderer.BufferFrameRenderer;
import me.kall.narutotv.renderer.FrameRenderer;
import me.kall.narutotv.renderer.ImageFrameRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public abstract class GuiSceneTV<T> extends AbstractTV<T> {
    public GuiSceneTV(FrameRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public boolean isRunnable() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (screen instanceof WinScreen || screen instanceof GenericMessageScreen) return true;
        if (minecraft.getOverlay() instanceof LoadingOverlay && minecraft.level == null) return true;
        if (minecraft.level != null) {
            this.shutdownEntire(false);
            return false;
        }
        return minecraft.isRunning();
    }

    @Override
    protected @NotNull MediaArgs newArgs() {
        MediaArgs init = RenderProps.syncInit();
        return init == null ? Sources.random(true) : init;
    }

    @Override
    protected float initVolume() {
        return NarutoConfig.volume();
    }

    public static final class Buffer extends GuiSceneTV<ByteBuffer> {
        public Buffer() {
            super(new BufferFrameRenderer.Gui());
        }
    }

    public static final class Image extends GuiSceneTV<NativeImage> {
        public Image() {
            super(new ImageFrameRenderer.Gui());
        }
    }
}
