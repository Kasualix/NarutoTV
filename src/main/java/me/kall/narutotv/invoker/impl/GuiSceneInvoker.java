package me.kall.narutotv.invoker.impl;

import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.override.GuiSceneControl;
import net.minecraft.network.chat.Component;

public class GuiSceneInvoker extends VideoDropInvoker {
    private static final Component TOOLTIP = Component.translatable("invoker.narutotv.gui_scene.tooltip");

    @Override
    protected void forResolved(MediaArgs mediaArgs) {
        GuiSceneControl.active.shutdownEntire(false);
        GuiSceneControl.active.mediaArgs = mediaArgs;
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
