package me.kall.narutotv.fade;

import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.config.NarutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = NarutoTV.MOD_ID)
public class FadeCenter {
    private static final MousePos LAST_MOUSE = new MousePos();

    private static int stopTicks = 0;
    private static int fadeAlpha = 100;

    public static boolean isHidden() {
        return fadeAlpha == 0;
    }

    public static boolean isFull() {
        return fadeAlpha == 100;
    }

    public static float fadeAlpha() {
        return (float) fadeAlpha / 100.0F;
    }

    public static int modifyAlpha(int color) {
        if (isFull() || !NarutoConfig.fadable()) return color;
        if (isHidden()) return (color & 0x00FFFFFF);
        int alpha = (int)(fadeAlpha() * 255.0F) & 0xFF;
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Pre event) {
        if (NarutoConfig.fadable()) {
            Minecraft minecraft = Minecraft.getInstance();
            MouseHandler mouseHandler = minecraft.mouseHandler;

            double x = mouseHandler.xpos();
            double y = mouseHandler.ypos();

            if (minecraft.level == null && minecraft.player == null && minecraft.screen != null && minecraft.getOverlay() == null && !LAST_MOUSE.init(x, y)) {
                stopTicks = LAST_MOUSE.same(x, y) ? stopTicks + 1 : 0;
                LAST_MOUSE.set(x, y);
            } else {
                stopTicks = 0;
            }

            fadeAlpha = stopTicks >= NarutoConfig.ticksBeforeFade() ? fadeAlpha - 1 : fadeAlpha + 5;
            if (fadeAlpha < 0) fadeAlpha = 0;
            if (fadeAlpha > 100) fadeAlpha = 100;
        }
    }

    @SubscribeEvent
    public static void anyInput(InputEvent.MouseButton.Pre event) {
        onAnyInput();
    }

    @SubscribeEvent
    public static void anyInput(InputEvent.MouseButton.Post event) {
        onAnyInput();
    }

    @SubscribeEvent
    public static void anyInput(InputEvent.MouseScrollingEvent event) {
        onAnyInput();
    }

    @SubscribeEvent
    public static void anyInput(InputEvent.Key event) {
        onAnyInput();
    }

    @SubscribeEvent
    public static void anyInput(InputEvent.InteractionKeyMappingTriggered event) {
        onAnyInput();
    }

    private static void onAnyInput() {
        Minecraft.getInstance().execute(() -> stopTicks = 0);
    }

    @SubscribeEvent
    public static void mouseInput(InputEvent.MouseButton.Pre event) {
        Minecraft.getInstance().execute(() -> fadeAlpha = 100);
    }

    static final class MousePos {
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
