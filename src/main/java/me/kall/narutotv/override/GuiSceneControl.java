package me.kall.narutotv.override;

import me.kall.narutotv.core.AbstractTV;
import me.kall.narutotv.data.system.RenderProps;
import me.kall.narutotv.produce.util.LifetimeController;
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
        double seekTo = 0D;
        if (GuiSceneControl.isCompatMode()) {
            BUFFER.mediaArgs = IMAGE.mediaArgs;
            active = BUFFER;

            if (IMAGE.video != null) {
                LifetimeController life = IMAGE.video.life();
                if (life != null) seekTo = life.sinceSetupSec();
            }
        } else {
            IMAGE.mediaArgs = BUFFER.mediaArgs;
            active = IMAGE;

            if (BUFFER.video != null) {
                LifetimeController life = BUFFER.video.life();
                if (life != null) seekTo = life.sinceSetupSec();
            }
        }

        BUFFER.shutdownEntire(true);
        IMAGE.shutdownEntire(true);

        active.setup(seekTo);
    }
}
