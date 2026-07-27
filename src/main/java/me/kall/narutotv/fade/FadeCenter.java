package me.kall.narutotv.fade;

import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.impl.config.NarutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoTV.MOD_ID)
public class FadeCenter {
    private static final float HIDDEN = 0F;
    private static final float FULL = 1F;

    private static final MousePos lastMouse = new MousePos();

    private static int stopTicks = 0;
    private static int fadeAlpha = 100;

    public static boolean isHidden() {
        return fadeAlpha == 0;
    }

    public static boolean isFull() {
        return fadeAlpha == 100;
    }

    public static float fadeAlpha() {
        if (isFull()) return FULL;
        if (isHidden()) return HIDDEN;
        return (float) fadeAlpha / 100F;
    }

    public static int modifyAlpha(int color) {
        if (!NarutoConfig.fadable() || isFull()) return color;
        if (isHidden()) return (color & 0x00FFFFFF);
        int alpha = (int)(fadeAlpha() * 255.0F) & 0xFF;
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    @SubscribeEvent
    public static void tickClient(TickEvent.@NotNull ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START && NarutoConfig.fadable()) {
            Minecraft minecraft = Minecraft.getInstance();
            MouseHandler mouseHandler = minecraft.mouseHandler;
            double x = mouseHandler.xpos();
            double y = mouseHandler.ypos();

            if (minecraft.level == null && minecraft.player == null && minecraft.screen != null && minecraft.getOverlay() == null && !lastMouse.init(x, y)) {
                stopTicks = lastMouse.same(x, y) ? stopTicks + 1 : 0;
                lastMouse.set(x, y);
            } else {
                stopTicks = 0;
            }

            fadeAlpha = stopTicks >= NarutoConfig.ticksBeforeFade() ? fadeAlpha - 1 : fadeAlpha + 5;
            if (fadeAlpha < 0) fadeAlpha = 0;
            if (fadeAlpha > 100) fadeAlpha = 100;
        }
    }

    @SubscribeEvent
    public static void anyInput(InputEvent event) {
        Minecraft.getInstance().execute(() -> stopTicks = 0);
    }

    @SubscribeEvent
    public static void mouseInput(InputEvent.MouseButton event) {
        Minecraft.getInstance().execute(() -> fadeAlpha = 100);
    }

    private static final class MousePos {
        double x = Double.NaN;
        double y = Double.NaN;

        boolean same(double x, double y) {
            return this.x == x && this.y == y;
        }

        boolean init(double x, double y) {
            if (Double.isNaN(this.x) || Double.isNaN(this.y)) {
                this.x = x;
                this.y = y;
                return true;
            }

            return false;
        }

        void set(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}