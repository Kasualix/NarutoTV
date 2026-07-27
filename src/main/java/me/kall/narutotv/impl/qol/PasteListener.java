package me.kall.narutotv.impl.qol;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.app.file.AppInstances;
import me.kall.narutotv.base.data.NarutoPaths;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.impl.gui.NarutoGuiCenter;
import me.kall.narutotv.impl.screen.NameSetScreen;
import me.kall.narutotv.impl.screen.NarutoGuiScreen;
import me.kall.narutotv.impl.screen.NarutoWorldScreen;
import me.kall.narutotv.impl.world.data.client.ClientRenderers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoTV.MOD_ID)
public final class PasteListener {
    private static final Set<String> DOWNLOADING = new ObjectOpenHashSet<>();

    private static final Logger LOGGER = LogManager.getLogger(PasteListener.class);

    @SubscribeEvent
    public static void listen(TickEvent.@NotNull ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        long window = minecraft.getWindow().getWindow();

        int rightCtrl = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
        int leftCtrl = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL);
        int v = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_V);

        if ((leftCtrl == GLFW.GLFW_PRESS || rightCtrl == GLFW.GLFW_PRESS) && (v == GLFW.GLFW_PRESS)) {
            try {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                if (!clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) return;

                String url = (String) clipboard.getData(DataFlavor.stringFlavor);

                if (!url.startsWith("http") || DOWNLOADING.contains(url)) return;

                DOWNLOADING.add(url);
                Minecraft.getInstance().setScreen(new NameSetScreen(name -> AppInstances.ytDlp()
                        .download(url, NarutoPaths.SOURCES.resolve(name))
                        .whenCompleteAsync((downloaded, throwable) -> {
                            DOWNLOADING.remove(url);
                            if (throwable != null) throw new RuntimeException(throwable);
                            if (downloaded == null) throw new IllegalArgumentException("Failed to download video and audio from" + url + ". Read latest.log for details.");
                            Path absVideoPath = downloaded.absVideoPath();
                            Path absAudioPath = downloaded.absAudioPath();

                            String videoStr = absVideoPath.toString();
                            String audioStr = absAudioPath.toString();

                            Sources.cutInLine(downloaded.absVideoPath(), downloaded.absAudioPath());

                            if (NarutoGuiCenter.getActive().isRunning()) {
                                NarutoGuiCenter.getActive().shutdown();
                                NarutoGuiScreen.sync(videoStr, audioStr);
                            }

                            ClientLevel level = minecraft.level;

                            if (minecraft.hitResult instanceof BlockHitResult target && target.getType().equals(HitResult.Type.BLOCK) && level != null) {
                                AbstractRenderer<?> renderer = ClientRenderers.get(level.dimension().location(), target.getBlockPos().asLong());
                                if (renderer != null && renderer.isRunning()) renderer.shutdown();
                                NarutoWorldScreen.sync(videoStr, audioStr);
                            }
                        }, minecraft), Minecraft.getInstance().screen));
            } catch (IOException | UnsupportedFlavorException exception) {
                LOGGER.error("Exception handling pasted url", exception);
            }
        }
    }
}
