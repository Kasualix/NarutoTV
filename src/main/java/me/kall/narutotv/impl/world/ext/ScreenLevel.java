package me.kall.narutotv.impl.world.ext;

import net.minecraft.server.level.ServerLevel;

public interface ScreenLevel {
    boolean narutotv$isCleaning();
    void narutotv$setCleaning(boolean cleaning);

    static boolean isCleaning(ServerLevel level) {
        return ((ScreenLevel)level).narutotv$isCleaning();
    }

    static void setCleaning(ServerLevel level, boolean cleaning) {
        ((ScreenLevel)level).narutotv$setCleaning(cleaning);
    }
}
