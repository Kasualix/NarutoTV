package me.kall.narutotv.compat;

import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.Contract;

public class CompatCenter {
    public static final boolean HAS_SHADER_MOD = isLoaded("oculus") || isLoaded("iris");
    public static final ICompat COMPAT = HAS_SHADER_MOD ? new IrisCompat() : () -> false;

    public static boolean shaderUsing() {
        return COMPAT.shaderUsing();
    }

    @Contract(pure = true)
    public static boolean hasShaderMod() {
        return HAS_SHADER_MOD;
    }

    private static boolean isLoaded(String modID) {
        return FMLLoader.getLoadingModList().getModFileById(modID) != null;
    }
}
