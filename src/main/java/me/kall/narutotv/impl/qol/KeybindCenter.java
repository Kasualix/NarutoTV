package me.kall.narutotv.impl.qol;

import me.kall.narutotv.app.file.AppInstances;
import me.kall.narutotv.base.data.Paths;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.impl.config.NarutoConfig;
import me.kall.narutotv.impl.gui.NarutoGuiCenter;
import me.kall.narutotv.impl.world.data.client.ClientRenderers;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class KeybindCenter {
    public static void register(@NotNull IEventBus forgeBus) {
        KeybindCenter keybindCenter = new KeybindCenter();
        forgeBus.addListener(keybindCenter::tickClient);
    }

    private int interval = 0;

    private static final Set<String> DOWNLOADING = ConcurrentHashMap.newKeySet();

    private void tickClient(TickEvent.@NotNull ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!NarutoGuiCenter.getActive().isRunning()) return;

        if (this.interval > 0) {
            this.interval--;
            return;
        }

        long window = Minecraft.getInstance().getWindow().getWindow();

        boolean f12 = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F12) == GLFW.GLFW_PRESS;
        boolean shift = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;
        boolean ctrl = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS;
        boolean v = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_V) == GLFW.GLFW_PRESS;
        boolean f = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F) == GLFW.GLFW_PRESS;
        boolean m = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_M) == GLFW.GLFW_PRESS;

        if (f12) {
            this.interval = 10;
            if (m) {
                NarutoConfig.Client.toggleMuteMusic();
            } else if (f) {
                NarutoConfig.Client.toggleFadable();
            } else if (shift) {
                this.swap();
            } else {
                NarutoGuiCenter.getActive().shutdown();
            }
        } else if (ctrl && v) {
            this.interval = 10;
            this.pasteUrl();
        }
    }

    private void swap() {
        NarutoGuiCenter.getInstance().swap();
        ClientRenderers.getInstance().swap();
    }

    private void pasteUrl() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                String url = (String) clipboard.getData(DataFlavor.stringFlavor);
                if (!url.startsWith("http")) return;
                System.out.println("[NarutoTV] Read " + url + " from user clipboard.");

                if (DOWNLOADING.contains(url)) {
                    System.out.println("[NarutoTV] Already downloading " + url + ". Skipping multiple inputs.");
                    return;
                }

                DOWNLOADING.add(url);
                AppInstances.ytDlp()
                        .download(url, Paths.SOURCES.resolve(String.valueOf(System.currentTimeMillis())))
                        .thenAccept(downloaded -> {
                            if (downloaded == null) return;
                            DOWNLOADING.remove(url);
                            Sources.cutInLine(downloaded.absVideoPath(), downloaded.absAudioPath());
                            Minecraft.getInstance().execute(NarutoGuiCenter.getActive()::shutdown);
                        });
            }
        } catch (IOException | UnsupportedFlavorException exception) {
            exception.printStackTrace(System.err);
        }
    }
}
