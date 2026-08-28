package me.kall.narutotv.invoker;

import me.kall.dragit.api.InvokerRegistry;
import me.kall.narutotv.invoker.impl.CapeDropInvoker;
import me.kall.narutotv.invoker.impl.GuiSceneInvoker;
import me.kall.narutotv.invoker.impl.LevelWallInvoker;
import org.jetbrains.annotations.NotNull;

public final class NarutoInvokers {
    public static void invokerRegistry(@NotNull InvokerRegistry event) {
        event.register(new GuiSceneInvoker());
        event.register(new LevelWallInvoker());
        event.register(new CapeDropInvoker());
    }
}
