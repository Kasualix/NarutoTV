package me.kall.narutotv.invoker.impl;

import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.data.file.GamePaths;
import me.kall.narutotv.override.GuiSceneControl;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

public class GuiSceneInvoker extends VideoDropInvoker {
    private static final Component ID = Component.translatable("invoker.narutotv.gui_scene");
    private static final Component TOOLTIP = Component.translatable("invoker.narutotv.gui_scene.tooltip");

    @Override
    protected void forResolved(MediaArgs mediaArgs) {
        GuiSceneControl.active.shutdownEntire(false);
        GuiSceneControl.active.mediaArgs = mediaArgs;
    }

    @Override
    protected Path copyTarget() {
        return GamePaths.SOURCES;
    }

    @Override
    public Component id() {
        return ID;
    }

    @Override
    public Component tooltip() {
        return TOOLTIP;
    }

    @Override
    public boolean isRunnable() {
        return GuiSceneControl.active.isRunning();
    }
}
