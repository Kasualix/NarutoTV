package me.kall.narutotv.data.world.cape;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record Cape(UUID player, String video) {
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Cape other)) return false;
        return other.player.equals(this.player);
    }

    @Override
    public int hashCode() {
        return this.player.hashCode();
    }

    @Override
    public @NotNull String toString() {
        return "Cape{player=" + this.player.toString() + ", video=" + this.video + "}";
    }
}
