package me.kall.narutotv.mixin.context;

import me.kall.narutotv.data.world.wall.ClientWalls;
import me.kall.narutotv.produce.util.LifetimeController;
import me.kall.narutotv.produce.video.AbstractFrameProducer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ResourceLoadStateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResourceLoadStateTracker.class)
public abstract class MixinResourceLoadStateTracker {
    @Inject(method = "finishReload", at = @At("TAIL"))
    private void onFinishReload(CallbackInfo ci) {
        Minecraft.getInstance().execute(() -> ClientWalls.forEach(tv -> {
            double seekTo = 0D;
            AbstractFrameProducer<?> video = tv.video;
            if (video != null) {
                LifetimeController life = video.life();
                if (life != null) seekTo = life.sinceSetupSec();
            }
            tv.shutdownEntire(true);
            tv.setup(seekTo);
        }));
    }
}
