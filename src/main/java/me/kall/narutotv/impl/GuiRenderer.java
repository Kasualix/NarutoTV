package me.kall.narutotv.impl;

import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.base.renderer.NativeImageRenderer;
import me.kall.narutotv.fade.FadeApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import org.jetbrains.annotations.NotNull;

public class GuiRenderer extends NativeImageRenderer {
    private static final GuiRenderer INSTANCE = new GuiRenderer();

    public static GuiRenderer getInstance() {
        return INSTANCE;
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
        return Sources.roll();
    }

    @Override
    public void onSetup(double seekTo) {
        super.onSetup(seekTo);

        FadeApi.getInstance().setUnfadable(this.textureLocation, true);
    }
}
