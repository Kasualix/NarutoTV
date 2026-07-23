package me.kall.narutotv.impl.qol;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.kall.narutotv.app.file.AppInstances;
import me.kall.narutotv.base.data.Paths;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.impl.config.NarutoConfig;
import me.kall.narutotv.impl.gui.NarutoGuiCenter;
import me.kall.narutotv.impl.world.data.client.ClientRenderers;
import me.kall.narutotv.impl.world.ext.BindScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Set;
import java.util.function.Predicate;

public class KeybindCenter {
    private static final Logger LOGGER = LogManager.getLogger(KeybindCenter.class);

    public static void register(@NotNull IEventBus forgeBus) {
        KeybindCenter keybindCenter = new KeybindCenter();
        forgeBus.addListener(keybindCenter::tickClient);
    }

    private int interval = 0;

    private static final Set<String> DOWNLOADING = new ObjectOpenHashSet<>();

    private void tickClient(TickEvent.@NotNull ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

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

        boolean mouseRight = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        if (f12) {
            this.interval = 10;
            if (m) NarutoConfig.Client.toggleMuteMusic();
            else if (f) NarutoConfig.Client.toggleFadable();
            else if (shift) this.swap();
            else this.restart();
        } else if (ctrl && v) {
            this.interval = 10;
            this.pasteUrl();
        } else if (mouseRight && shift) {
            this.interval = 10;

            Screen screen = Minecraft.getInstance().screen;
            if (screen == null) return;

            for (Renderable renderable : screen.renderables) {
                if (renderable instanceof EditBox editBox && editBox.isFocused()) editBox.setValue("");
            }
        }
    }

    private void restart() {
        NarutoGuiCenter.getActive().shutdown();
        ClientRenderers.getInstance().forEach(AbstractRenderer::shutdown);
    }

    private void swap() {
        if (NarutoGuiCenter.getActive().isRunning()) NarutoGuiCenter.getInstance().swap();
        ClientRenderers.getInstance().swap();
    }

    private void pasteUrl() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                String url = (String) clipboard.getData(DataFlavor.stringFlavor);
                if (!url.startsWith("http")) return;
                LOGGER.info("[NarutoTV] Read {} from user clipboard.", url);

                if (DOWNLOADING.contains(url)) {
                    LOGGER.info("[NarutoTV] Already downloading {}. Skipping multiple inputs.", url);
                    return;
                }

                Minecraft minecraft = Minecraft.getInstance();

                DOWNLOADING.add(url);
                AppInstances.ytDlp()
                        .download(url, Paths.SOURCES.resolve(String.valueOf(System.currentTimeMillis())))
                        .whenCompleteAsync((downloaded, throwable) -> {
                            DOWNLOADING.remove(url);
                            if (throwable != null) throw new RuntimeException(throwable);
                            if (downloaded == null) return;
                            Sources.cutInLine(downloaded.absVideoPath(), downloaded.absAudioPath());
                            NarutoGuiCenter.getActive().shutdown();

                            if (minecraft.hitResult instanceof BlockHitResult target) {
                                Predicate<AbstractRenderer<?>> condition = (renderer) -> ((BindScreen)renderer).screen().borderInvolved().contains(target.getBlockPos().asLong());
                                ClientRenderers.getInstance().forSpecific(condition, AbstractRenderer::shutdown);
                            }
                        }, minecraft);
            }
        } catch (IOException | UnsupportedFlavorException exception) {
            LOGGER.error("Exception handling pasted url", exception);
        }
    }
}
