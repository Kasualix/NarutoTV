package me.kall.narutotv.invoker.impl;

import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.app.FFmpeg;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.data.file.GamePaths;
import me.kall.narutotv.data.world.cape.Cape;
import me.kall.narutotv.data.world.cape.ClientCapes;
import me.kall.narutotv.network.packet.cape.CapeUpdatePacket;
import me.kall.narutotv.world.CapeTV;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class CapeDropInvoker extends VideoDropInvoker {
    private static final Component ID = Component.translatable("invoker.narutotv.cape");
    private static final Component TOOLTIP = Component.translatable("invoker.narutotv.cape.tooltip");

    @Override
    protected void forPath(Supplier<String> pathSupplier) {
        CompletableFuture.supplyAsync(pathSupplier, NarutoTV.io())
                .thenApplyAsync((path) -> FFmpeg.read(path, null), NarutoTV.io())
                .whenCompleteAsync((mediaArgs, throwable) -> {
                    if (throwable != null) throw new RuntimeException(throwable);
                    this.forResolved(mediaArgs);
                }, Minecraft.getInstance());
    }

    @Override
    protected void forResolved(@NotNull MediaArgs mediaArgs) {
        LocalPlayer player = Minecraft.getInstance().player;
        assert player != null;
        UUID uuid = player.getUUID();
        String path = GamePaths.relConfig(mediaArgs.absVideoPath());
        ClientCapes.add(new Cape(uuid, path)).ifPresent(CapeTV.DEATH);
        PacketDistributor.sendToServer(new CapeUpdatePacket(uuid, path));
    }

    @Override
    protected Path copyTarget() {
        return GamePaths.CAPES;
    }

    @Override
    public Component id() {
        return ID;
    }

    @Override
    public Component tooltip() {
        return TOOLTIP;
    }

    @Override
    public boolean isRunnable() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null && minecraft.player != null;
    }
}
