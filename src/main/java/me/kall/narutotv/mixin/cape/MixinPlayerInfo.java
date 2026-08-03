package me.kall.narutotv.mixin.cape;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.authlib.GameProfile;
import me.kall.narutotv.impl.world.data.client.ClientVideoCapes;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

@Mixin(PlayerInfo.class)
public abstract class MixinPlayerInfo {
    @Shadow
    @Final
    private GameProfile profile;

    @WrapMethod(method = "getCapeLocation")
    private ResourceLocation getVideoCape(Operation<ResourceLocation> original) {
        UUID uuid = this.profile.getId();
        ClientVideoCapes.VideoCape videoCape = ClientVideoCapes.get(uuid);
        return videoCape == null ? original.call() : videoCape.narutoTexture.textureLocation;
    }
}
