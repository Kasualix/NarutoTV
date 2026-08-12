package me.kall.narutotv.invoker;

import me.kall.dragit.api.InvokerRegistry;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.invoker.impl.CapeDropInvoker;
import me.kall.narutotv.invoker.impl.GuiSceneInvoker;
import me.kall.narutotv.invoker.impl.LevelWallInvoker;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = NarutoTV.MOD_ID, value = Dist.CLIENT)
public final class NarutoInvokers {
    @SubscribeEvent
    public static void invokerRegistry(@NotNull InvokerRegistry event) {
        event.register(new GuiSceneInvoker());
        event.register(new LevelWallInvoker());
        event.register(new CapeDropInvoker());
    }
}
