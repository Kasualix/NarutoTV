package me.kall.narutotv.base.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.file.AppPaths;
import me.kall.narutotv.app.produce.video.AbstractFrameProducer;
import me.kall.narutotv.app.produce.video.ImageFrameProducer;
import me.kall.narutotv.impl.world.data.client.NarutoTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class NativeImageRenderer extends AbstractRenderer<NativeImage> {
    protected final NarutoTexture narutoTexture = new NarutoTexture();

    protected abstract @NotNull ResourceLocation setLocation();

    @Override
    public @NotNull AbstractFrameProducer<NativeImage> initVideo() {
        return ImageFrameProducer.create(this.mediaArgs(), 2, AppPaths.absFFmpegPath());
    }

    @Override
    public void update(@Nullable NativeImage frame) {
        DynamicTexture dynamicTexture = this.narutoTexture.dynamicTexture;
        if (frame == null || dynamicTexture == null) return;
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

        this.narutoTexture.dynamicTexture = new DynamicTexture(width, height, false);
        this.narutoTexture.textureLocation = this.setLocation();
        this.narutoTexture.register();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        this.narutoTexture.close();
    }
}
