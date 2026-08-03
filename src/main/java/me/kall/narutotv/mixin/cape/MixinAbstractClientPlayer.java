package me.kall.narutotv.mixin.cape;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.authlib.GameProfile;
import me.kall.narutotv.impl.world.data.client.ClientVideoCapes;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractClientPlayer.class)
public abstract class MixinAbstractClientPlayer extends Player {
    public MixinAbstractClientPlayer(Level level, BlockPos pos, float yRot, GameProfile gameProfile) {
        super(level, pos, yRot, gameProfile);
    }

    @WrapMethod(method = "getCloakTextureLocation")
    private ResourceLocation customCape(@NotNull Operation<ResourceLocation> original) {
        ClientVideoCapes.VideoCape videoCape = ClientVideoCapes.get(this.getUUID());
        return videoCape == null ? original.call() : videoCape.narutoTexture.textureLocation;
    }
}
