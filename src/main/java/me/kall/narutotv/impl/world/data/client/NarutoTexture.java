package me.kall.narutotv.impl.world.data.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NarutoTexture {
    public @Nullable ResourceLocation textureLocation;
    public @Nullable DynamicTexture dynamicTexture;

    public NarutoTexture() {}

    private NarutoTexture(@NotNull ResourceLocation textureLocation, @NotNull DynamicTexture dynamicTexture) {
        this.textureLocation = textureLocation;
        this.dynamicTexture = dynamicTexture;
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull NarutoTexture of(@NotNull ResourceLocation textureLocation, @NotNull DynamicTexture dynamicTexture) {
        return new NarutoTexture(textureLocation, dynamicTexture).register();
    }

    public NarutoTexture register() {
        if (this.textureLocation == null || this.dynamicTexture == null) throw new IllegalArgumentException();
        Minecraft.getInstance().getTextureManager().register(this.textureLocation, this.dynamicTexture);
        return this;
    }

    public void close() {
        if (this.textureLocation != null) Minecraft.getInstance().getTextureManager().release(this.textureLocation);
        if (this.dynamicTexture != null) this.dynamicTexture.close();
        this.dynamicTexture = null;
    }
}
