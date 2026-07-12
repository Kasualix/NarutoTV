package me.kall.narutotv.base.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.file.AppPaths;
import me.kall.narutotv.app.produce.video.AbstractFrameProducer;
import me.kall.narutotv.app.produce.video.ImageFrameProducer;
import me.kall.narutotv.base.data.Graphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class NativeImageRenderer extends AbstractRenderer<NativeImage> {
    private final ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(NarutoTV.MOD_ID, "dynamic");

    private DynamicTexture dynamicTexture;

    @Override
    public @NotNull AbstractFrameProducer<NativeImage> initVideo() {
        return ImageFrameProducer.create(this.mediaArgs(), 2, AppPaths.absFFmpegPath());
    }

    @Override
    public void update(@Nullable NativeImage frame) {
        if (frame == null) return;
        DynamicTexture dynamicTexture = this.dynamicTexture;
        if (dynamicTexture == null) return;
        dynamicTexture.setPixels(frame);
        dynamicTexture.upload();
        frame.close();
    }

    @Override
    public void onSetup(double seekTo) {
        MediaArgs mediaArgs = this.mediaArgs();
        assert mediaArgs != null;

        int width = mediaArgs.width();
        int height = mediaArgs.height();

        this.dynamicTexture = new DynamicTexture(width, height, false);
        Minecraft.getInstance().getTextureManager().register(this.textureLocation, this.dynamicTexture);

        //TODO: FadeApi.getInstance().setUnfadable(this.textureLocation, true);
    }

    @Override
    public void render() {
        super.render();

        GuiGraphics guiGraphics = Graphics.get();
        if (guiGraphics == null || this.dynamicTexture == null) return;

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        guiGraphics.blit(this.textureLocation, 0, 0, 0, 0, width, height, width, height);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        this.dynamicTexture.close();
        this.dynamicTexture = null;
    }
}
