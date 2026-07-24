package me.kall.narutotv.impl.gui;

import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.base.data.Graphics;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.base.renderer.NativeImageRenderer;
import me.kall.narutotv.fade.FadeApi;
import me.kall.narutotv.impl.NarutoProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class GuiImageRenderer extends NativeImageRenderer {
    @Override
    public boolean isRunnable() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (screen instanceof WinScreen || screen instanceof GenericDirtMessageScreen) return true;
        if (minecraft.getOverlay() instanceof LoadingOverlay && minecraft.level == null) return true;
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

    @Override
    public void render() {
        super.render();
        GuiGraphics guiGraphics = Graphics.get();
        if (guiGraphics == null || this.textureLocation == null || this.dynamicTexture == null) return;

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        guiGraphics.blit(this.textureLocation, 0, 0, 0, 0, width, height, width, height);
    }
}
