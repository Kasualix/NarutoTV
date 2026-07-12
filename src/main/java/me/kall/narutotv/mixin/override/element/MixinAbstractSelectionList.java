package me.kall.narutotv.mixin.override.element;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.kall.narutotv.override.CustomOverride;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractSelectionList.class)
public abstract class MixinAbstractSelectionList {
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;renderBackground(Lnet/minecraft/client/gui/GuiGraphics;)V"))
    private void skipRenderBackground(AbstractSelectionList<?> instance, GuiGraphics guiGraphics, @NotNull Operation<Void> original) {
        if (CustomOverride.getInstance().overridable()) {
            CustomOverride.getInstance().override();
        } else {
            original.call(instance, guiGraphics);
        }
    }

    @WrapOperation(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;renderTopAndBottom:Z", opcode = Opcodes.GETFIELD))
    private boolean skipRenderTopAndBottom(AbstractSelectionList<?> instance, Operation<Boolean> original) {
        return !CustomOverride.getInstance().overridable() && original.call(instance);
    }

    @WrapOperation(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;renderBackground:Z", opcode = Opcodes.GETFIELD))
    private boolean skipRenderBackground(AbstractSelectionList<?> instance, Operation<Boolean> original) {
        return !CustomOverride.getInstance().overridable() && original.call(instance);
    }
}