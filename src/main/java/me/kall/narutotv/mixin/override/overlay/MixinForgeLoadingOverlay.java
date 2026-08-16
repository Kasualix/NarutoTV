package me.kall.narutotv.mixin.override.overlay;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.platform.Window;
import me.kall.narutotv.override.OverrideCenter;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.neoforged.fml.earlydisplay.DisplayWindow;
import net.neoforged.fml.loading.progress.ProgressMeter;
import net.neoforged.neoforge.client.loading.NeoForgeLoadingOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(NeoForgeLoadingOverlay.class)
public abstract class MixinForgeLoadingOverlay {
    @Shadow(remap = false) @Final private Minecraft minecraft;
    @Shadow(remap = false) @Final private ReloadInstance reload;
    @Shadow(remap = false) @Final private Consumer<Optional<Throwable>> onFinish;
    @Shadow(remap = false) @Final private DisplayWindow displayWindow;
    @Shadow(remap = false) private long fadeOutStart;
    @Shadow(remap = false) @Final private ProgressMeter progressMeter;

    @WrapMethod(method = "render")
    private void renderOverride(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, Operation<Void> original) {
        if (OverrideCenter.getInstance().overridable()) {
            OverrideCenter.getInstance().override();

            Minecraft minecraft = this.minecraft;
            ReloadInstance reload = this.reload;
            ProgressMeter progress = this.progressMeter;
            long fadeOutStart = this.fadeOutStart;

            progress.setAbsolute(Mth.clamp((int) (reload.getActualProgress() * 100F), 0, 100));

            float fadeOutTimer = fadeOutStart > -1L ? (float) (Util.getMillis() - fadeOutStart) / 1000.0F : -1.0F;

            this.override$processOverlay(guiGraphics, mouseX, mouseY, partialTick, minecraft, fadeOutTimer);

            if (fadeOutStart == -1L && reload.isDone()) this.override$finalize(minecraft, reload);
        } else {
            original.call(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Unique
    private void override$processOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, Minecraft minecraft, float fadeOutTimer) {
        if (fadeOutTimer > 1.0F) {
            Screen screen = minecraft.screen;
            if (screen != null) screen.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        DisplayWindow displayWindow = this.displayWindow;
        displayWindow.render(0xFF);

        if (fadeOutTimer >= 2.0F) {
            this.progressMeter.complete();
            minecraft.setOverlay(null);
            displayWindow.close();
        }
    }

    @Unique
    private void override$finalize(Minecraft minecraft, ReloadInstance reload) {
        this.fadeOutStart = Util.getMillis();

        Consumer<Optional<Throwable>> onFinish = this.onFinish;

        try {
            reload.checkExceptions();
            onFinish.accept(Optional.empty());
        } catch (Throwable throwable) {
            onFinish.accept(Optional.of(throwable));
        }

        Screen screen = minecraft.screen;
        if (screen != null) {
            Window window = minecraft.getWindow();
            screen.init(minecraft, window.getGuiScaledWidth(), window.getGuiScaledHeight());
        }
    }
}