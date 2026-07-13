package me.kall.narutotv.impl;

import me.kall.narutotv.NarutoTV;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoTV.MOD_ID)
public class KeybindCenter {
    private static int interval = 0;

    @SubscribeEvent
    public static void tickClient(TickEvent.@NotNull ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!NarutoGuiRenderer.isRunning()) return;

        if (interval > 0) {
            interval--;
            return;
        }

        long window = Minecraft.getInstance().getWindow().getWindow();

        boolean f12 = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F12) == GLFW.GLFW_PRESS;
        boolean shift = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;
        boolean ctrl = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS;

        if (f12) {
            interval = 10;
            if (ctrl) {
            } else if (shift) {
                NarutoGuiRenderer.switchType();
            } else {
                NarutoGuiRenderer.shutdown();
            }
        }
    }
}
