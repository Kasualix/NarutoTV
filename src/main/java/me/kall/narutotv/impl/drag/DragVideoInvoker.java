package me.kall.narutotv.impl.drag;

import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.base.data.NarutoPaths;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.impl.gui.NarutoGuiCenter;
import me.kall.narutotv.impl.screen.NameSetScreen;
import me.kall.narutotv.impl.screen.NarutoGuiScreen;
import me.kall.narutotv.impl.screen.NarutoWorldScreen;
import me.kall.narutotv.impl.world.data.client.ClientWalls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FileUtils;
import org.lwjgl.glfw.GLFWDropCallback;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class DragVideoInvoker implements SourceDragCenter.Invoker {
    @Override
    public boolean invoke(long window, int count, long names) {
        File source = Path.of(GLFWDropCallback.getName(names, 0)).toFile();

        Minecraft minecraft = Minecraft.getInstance();

        HitResult hitResult = minecraft.hitResult;
        ClientLevel level = minecraft.level;

        minecraft.setScreen(new NameSetScreen(name -> CompletableFuture.supplyAsync(() -> {
            try {
                Path dir = NarutoPaths.SOURCES.resolve(name);
                Files.createDirectories(dir);
                Path target = dir.resolve("video." + FileNameUtils.getExtension(source.getName()));

                if (Files.exists(target)) Files.delete(target);
                FileUtils.copyFile(source, target.toFile());

                return target.toString();
            } catch (IOException exception) {
                LOGGER.error("Exception copying video file {}.", source.toString());
                LOGGER.error("Details: ", exception);
                throw new RuntimeException(exception);
            }
        }, NarutoTV.io()).whenCompleteAsync((absVideoPath, throwable) -> {
            if (throwable != null) throw new RuntimeException(throwable);

            Sources.cutInLine(absVideoPath, null);

            if (hitResult instanceof BlockHitResult && level != null) {
                BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
                ResourceLocation dimension = level.dimension().location();
                AbstractRenderer<?> renderer = ClientWalls.get(dimension, pos.asLong());
                if (renderer != null) {
                    renderer.shutdown();
                    NarutoWorldScreen.sync(absVideoPath, absVideoPath);
                }
            }

            NarutoGuiCenter.getActive().shutdown();
            NarutoGuiScreen.sync(absVideoPath, absVideoPath);
        }, minecraft), minecraft.screen));
        return true;
    }
}
