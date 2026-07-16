package me.kall.narutotv.impl.qol;

import me.kall.narutotv.app.file.AppInstances;
import me.kall.narutotv.base.data.Paths;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.impl.config.NarutoConfig;
import me.kall.narutotv.impl.gui.NarutoGuiCenter;
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
    private static final KeybindCenter INSTANCE = new KeybindCenter();

    public static void register(@NotNull IEventBus forgeBus) {
        forgeBus.addListener(INSTANCE::tickClient);
    }

    private int interval = 0;

    private static final Set<String> DOWNLOADING = ConcurrentHashMap.newKeySet();

    public void tickClient(TickEvent.@NotNull ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!NarutoGuiCenter.ACTIVE.get().isRunning()) return;

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
                NarutoConfig.toggleMuteMusic();
            } else if (f) {
                NarutoConfig.toggleFadable();
            } else if (shift) {
                NarutoGuiCenter.swap();
            } else {
                NarutoGuiCenter.ACTIVE.get().shutdown();
            }
        } else if (ctrl && v) {
            this.interval = 10;
            this.pasteUrl();
        }
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
                            Minecraft.getInstance().execute(NarutoGuiCenter.ACTIVE.get()::shutdown);
                        });
            }
        } catch (IOException | UnsupportedFlavorException exception) {
            exception.printStackTrace(System.err);
        }
    }
}
