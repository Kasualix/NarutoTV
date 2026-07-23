package me.kall.narutotv.impl.qol;

import me.kall.narutotv.NarutoTV;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoTV.MOD_ID)
public class SourceDragCenter extends GLFWDropCallback {
    private static @Nullable GLFWDropCallback last;

    private final Invoker[] invokers = new Invoker[]{new DragVideoInvoker()};

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        last = GLFW.glfwSetDropCallback(Minecraft.getInstance().getWindow().getWindow(), new SourceDragCenter());
    }

    @Override
    public void invoke(long window, int count, long names) {
        if (last != null) last.invoke(window, count, names);
        for (Invoker invoker : this.invokers) invoker.invoke(window, count, names);
    }

    public interface Invoker {
        void invoke(long window, int count, long names);
    }
}
