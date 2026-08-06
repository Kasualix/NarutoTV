package me.kall.narutotv.override;

import me.kall.narutotv.core.AbstractTV;
import me.kall.narutotv.data.system.RenderProps;
import org.jetbrains.annotations.NotNull;

public class GuiSceneControl {
    private static final GuiSceneTV.Buffer BUFFER = new GuiSceneTV.Buffer();
    private static final GuiSceneTV.Image IMAGE = new GuiSceneTV.Image();

    public static @NotNull AbstractTV<?> active = BUFFER;

    public static boolean isCompatMode() {
        return active == IMAGE;
    }

    public static void init() {
        RenderProps.turnAccel(true);
        double earlyCost = RenderProps.earlyCost();
        if (earlyCost != 0D) active.setup(earlyCost);
    }

    public static void swap() {
        if (GuiSceneControl.isCompatMode()) {
            BUFFER.mediaArgs = IMAGE.mediaArgs;
            IMAGE.shutdownEntire(false);
            active = BUFFER;
        } else {
            IMAGE.mediaArgs = BUFFER.mediaArgs;
            BUFFER.shutdownEntire(false);
            active = IMAGE;
        }

        active.setup(0D);
    }
}
