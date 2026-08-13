package me.kall.narutotv.mixin.qol;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRules.class)
public abstract class MixinGameRules {
    @WrapOperation(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;register(Ljava/lang/String;Lnet/minecraft/world/level/GameRules$Category;Lnet/minecraft/world/level/GameRules$Type;)Lnet/minecraft/world/level/GameRules$Key;"))
    private static <T extends GameRules.Value<T>> GameRules.Key<T> modifyInitialBlockLimit(String name, GameRules.Category category, GameRules.Type<T> type, Operation<GameRules.Key<T>> original) {
        return original.call(name, category, !name.equals("commandModificationBlockLimit") ? type: GameRules.IntegerValue.create(99999999));
    }
}
