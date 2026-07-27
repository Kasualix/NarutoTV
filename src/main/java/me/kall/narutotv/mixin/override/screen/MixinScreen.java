package me.kall.narutotv.mixin.override.screen;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.kall.narutotv.fade.FadeCenter;
import me.kall.narutotv.impl.gui.OverrideCenter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Screen.class)
public abstract class MixinScreen {
    @Shadow @Final public List<Renderable> renderables;

    @Unique boolean narutotv$hidden = false;

    @SuppressWarnings("UnstableApiUsage")
    @WrapMethod(method = "renderDirtBackground")
    private void skipRenderDirtBackground(GuiGraphics guiGraphics, Operation<Void> original) {
        if (OverrideCenter.getInstance().overridable()) {
            OverrideCenter.getInstance().override();
            MinecraftForge.EVENT_BUS.post(new ScreenEvent.BackgroundRendered((Screen) (Object) this, guiGraphics));
        } else {
            original.call(guiGraphics);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        boolean now = FadeCenter.isHidden();
        if (now != this.narutotv$hidden) {
            this.narutotv$hidden = now;
            for (Renderable renderable : this.renderables) {
                if (renderable instanceof EditBox editBox) editBox.setVisible(!this.narutotv$hidden);
            }
        }
    }
}