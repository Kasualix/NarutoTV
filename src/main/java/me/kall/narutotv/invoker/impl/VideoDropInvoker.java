package me.kall.narutotv.invoker.impl;

import me.kall.dragit.api.Invoker;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.data.file.GamePaths;
import me.kall.narutotv.data.file.Sources;
import me.kall.narutotv.screen.DoubleCheckScreen;
import me.kall.narutotv.screen.NameSetScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.commons.compress.utils.FileNameUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public abstract class VideoDropInvoker implements Invoker {
    private static final Logger LOGGER = LoggerFactory.getLogger(VideoDropInvoker.class);
    private static final Component ID = Component.translatable("invoker.narutotv.video");

    @Override
    public void run(String absVideoPath) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new NameSetScreen(name -> {
            Path directory = GamePaths.SOURCES.resolve(name);
            Path target = directory.resolve("video." + FileNameUtils.getExtension(absVideoPath));

            if (Files.exists(target)) {
                minecraft.setScreen(new DoubleCheckScreen(useExisting -> this.forPath(useExisting ? target::toString : () -> copyVideo(absVideoPath, target, directory)), minecraft.screen, name));
            } else {
                this.forPath(() -> copyVideo(absVideoPath, target, directory));
            }
        }, minecraft.screen));
    }

    private void forPath(Supplier<String> pathSupplier) {
        CompletableFuture.supplyAsync(pathSupplier, NarutoTV.io())
                .thenApplyAsync(Sources::get, NarutoTV.io())
                .whenCompleteAsync((mediaArgs, throwable) -> {
                    if (throwable != null) throw new RuntimeException(throwable);
                    this.forResolved(mediaArgs);
                }, Minecraft.getInstance());
    }

    private static @NotNull String copyVideo(String absVideoPath, Path target, Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            LOGGER.error("Exception creating directory {}", directory);
            LOGGER.error("Details:", exception);
            throw new RuntimeException(exception);
        }

        try {
            Files.copy(Path.of(absVideoPath), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            LOGGER.error("Exception copying {} to {}", absVideoPath, target);
            LOGGER.error("Details:", exception);
            throw new RuntimeException(exception);
        }

        return target.toString();
    }

    protected abstract void forResolved(MediaArgs mediaArgs);

    @Override
    public Component id() {
        return ID;
    }
}