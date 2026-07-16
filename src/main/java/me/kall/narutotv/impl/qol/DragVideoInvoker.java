package me.kall.narutotv.impl.qol;

import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.base.data.Paths;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.impl.gui.NarutoGuiCenter;
import net.minecraft.client.Minecraft;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FileUtils;
import org.lwjgl.glfw.GLFWDropCallback;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class DragVideoInvoker implements SourceDragCenter.Invoker {
    public static final DragVideoInvoker INSTANCE = new DragVideoInvoker();

    @Override
    public void invoke(long window, int count, long names) {
        File source = Path.of(GLFWDropCallback.getName(names, 0)).toFile();

        CompletableFuture.runAsync(() -> {
            try {
                Path dir = Paths.SOURCES.resolve(String.valueOf(System.currentTimeMillis()));
                Files.createDirectories(dir);
                Path target = dir.resolve("video." + FileNameUtils.getExtension(source.getName()));

                long start = System.nanoTime();
                System.out.println("Start to copy " + source + ". Target: " + target);
                FileUtils.copyFile(source, target.toFile());
                System.out.println("Successfully copy " + source + " to " + target + ". Time cost: " + ((System.nanoTime() - start) / 1_000_000_000.0D) + " seconds.");
                Sources.cutInLine(target, null);
                Minecraft.getInstance().execute(NarutoGuiCenter.ACTIVE.get()::shutdown);
            } catch (IOException exception) {
                System.err.println("Exception dragging video " + source.getName());
                exception.printStackTrace(System.err);
            }
        }, NarutoTV.io());
    }
}
