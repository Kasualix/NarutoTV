package me.kall.narutotv.impl.qol;

import net.minecraft.client.Minecraft;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;

public class SourceDragCenter extends GLFWDropCallback {
    private static final SourceDragCenter INSTANCE = new SourceDragCenter();

    public static void register(@NotNull IEventBus modBus) {
        modBus.addListener(INSTANCE::setup);
    }

    private @Nullable GLFWDropCallback last;

    private final Invoker[] invokers = new Invoker[]{DragVideoInvoker.INSTANCE};

    private void setup(FMLClientSetupEvent event) {
        this.last = GLFW.glfwSetDropCallback(Minecraft.getInstance().getWindow().getWindow(), new SourceDragCenter());
    }

    @Override
    public void invoke(long window, int count, long names) {
        if (this.last != null) this.last.invoke(window, count, names);
        for (Invoker invoker : this.invokers) invoker.invoke(window, count, names);
    }

    public interface Invoker {
        void invoke(long window, int count, long names);
    }
}
