package me.kall.narutotv.impl.drag;

import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.base.data.NarutoPaths;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.impl.gui.NarutoGuiCenter;
import me.kall.narutotv.impl.screen.NameSetScreen;
import me.kall.narutotv.impl.screen.NarutoGuiScreen;
import net.minecraft.client.Minecraft;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFWDropCallback;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class DragVideoInvoker implements SourceDragCenter.Invoker {
    private static final Logger LOGGER = LogManager.getLogger(DragVideoInvoker.class);

    @Override
    public void invoke(long window, int count, long names) {
        File source = Path.of(GLFWDropCallback.getName(names, 0)).toFile();

        Minecraft.getInstance().setScreen(new NameSetScreen(name -> CompletableFuture.runAsync(() -> {
            try {
                Path dir = NarutoPaths.SOURCES.resolve(name);
                Files.createDirectories(dir);
                Path target = dir.resolve("video." + FileNameUtils.getExtension(source.getName()));

                FileUtils.copyFile(source, target.toFile());
                Sources.cutInLine(target, null);

                Minecraft.getInstance().execute(() -> NarutoGuiCenter.getActive().shutdown());

                String targetStr = target.toString();
                NarutoGuiScreen.sync(targetStr, targetStr);
            } catch (IOException exception) {
                LOGGER.error("Exception dragging video {}.", source.getName());
                LOGGER.error("Details: ", exception);
            }
        }, NarutoTV.io()), Minecraft.getInstance().screen));
    }
}
