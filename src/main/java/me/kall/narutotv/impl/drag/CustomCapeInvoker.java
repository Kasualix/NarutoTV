package me.kall.narutotv.impl.drag;

import me.kall.narutotv.base.data.NarutoPaths;
import me.kall.narutotv.impl.world.data.client.ClientVideoCapes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FileUtils;
import org.lwjgl.glfw.GLFWDropCallback;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class CustomCapeInvoker implements SourceDragCenter.Invoker {
    @Override
    public boolean invoke(long window, int count, long names) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;

        if (level == null || player == null) return false;

        if (minecraft.options.getCameraType().isFirstPerson()) return false;

        File source = Path.of(GLFWDropCallback.getName(names, 0)).toFile();

        try {
            Path target = NarutoPaths.CAPES.resolve(System.currentTimeMillis() + "." + FileNameUtils.getExtension(source.getName()));
            FileUtils.copyFile(source, target.toFile());
            ClientVideoCapes.register(player.getUUID(), target.toString());
        } catch (IOException exception) {
            LOGGER.error("Exception customizing cape for {}.", source.toPath().toString());
            LOGGER.error("Details: ", exception);
        }

        return true;
    }
}
