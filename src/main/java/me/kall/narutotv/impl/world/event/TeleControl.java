package me.kall.narutotv.impl.world.event;

import it.unimi.dsi.fastutil.objects.ObjectCollection;
import me.kall.duplicationless.ext.RegistryEntry;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.impl.config.NarutoConfig;
import me.kall.narutotv.impl.screen.NarutoWorldScreen;
import me.kall.narutotv.impl.world.data.BlockScreen;
import me.kall.narutotv.impl.world.data.client.ClientRenderers;
import me.kall.narutotv.impl.world.ext.InWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoTV.MOD_ID)
public class TeleControl {
    @SubscribeEvent
    public static void configureScreen(PlayerInteractEvent.@NotNull RightClickItem event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) return;

        if (!RegistryEntry.get(player.getMainHandItem()).equals(NarutoConfig.Client.teleControl())) return;
        if (!player.isShiftKeyDown()) return;

        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getViewVector(minecraft.getPartialTick());

        ObjectCollection<AbstractRenderer<?>> renderers = ClientRenderers.getIn(level.dimension().location());
        if (renderers.isEmpty()) return;

        BlockScreen target = null;
        double minDist = Double.MAX_VALUE;

        for (AbstractRenderer<?> renderer : renderers) {
            BlockScreen screen = ((InWorld)renderer).screen();
            Vec3 intersection = getIntersection(eye, view, screen);
            if (intersection == null) continue;

            double dist = eye.distanceToSqr(intersection);
            if (dist < minDist) {
                minDist = dist;
                target = screen;
            }
        }

        if (target != null) minecraft.setScreen(new NarutoWorldScreen(minecraft.screen, target));
    }

    @Nullable
    private static Vec3 getIntersection(@NotNull Vec3 origin, @NotNull Vec3 direction, @NotNull BlockScreen screen) {
        double leftBottomX = screen.leftBottom.getX();
        double leftBottomY = screen.leftBottom.getY();
        double leftBottomZ = screen.leftBottom.getZ();

        double rightBottomX = screen.rightBottom.getX();
        double rightBottomY = screen.rightBottom.getY();
        double rightBottomZ = screen.rightBottom.getZ();

        double leftTopX = screen.leftTop.getX();
        double leftTopY = screen.leftTop.getY();
        double leftTopZ = screen.leftTop.getZ();

        double originX = origin.x;
        double originY = origin.y;
        double originZ = origin.z;

        double directionX = direction.x;
        double directionY = direction.y;
        double directionZ = direction.z;

        double edge1X = rightBottomX - leftBottomX;
        double edge1Y = rightBottomY - leftBottomY;
        double edge1Z = rightBottomZ - leftBottomZ;

        double edge2X = leftTopX - leftBottomX;
        double edge2Y = leftTopY - leftBottomY;
        double edge2Z = leftTopZ - leftBottomZ;

        double normalX = edge1Y * edge2Z - edge1Z * edge2Y;
        double normalY = edge1Z * edge2X - edge1X * edge2Z;
        double normalZ = edge1X * edge2Y - edge1Y * edge2X;

        double normalLength = Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        if (normalLength < 1e-8) return null;

        normalX = normalX * (1.0 / normalLength);
        normalY = normalY * (1.0 / normalLength);
        normalZ = normalZ * (1.0 / normalLength);

        double denominator = normalX * directionX + normalY * directionY + normalZ * directionZ;
        if (Math.abs(denominator) < 1e-8) return null;

        double t = ((leftBottomX - originX) * normalX + (leftBottomY - originY) * normalY + (leftBottomZ - originZ) * normalZ) / denominator;
        if (t < 0) return null;

        Vec3 hit = new Vec3(originX + directionX * t, originY + directionY * t, originZ + directionZ * t);

        double rightVecX = rightBottomX - leftBottomX;
        double rightVecY = rightBottomY - leftBottomY;
        double rightVecZ = rightBottomZ - leftBottomZ;

        double rightVecLength = rightVecX * rightVecX + rightVecY * rightVecY + rightVecZ * rightVecZ;

        double upVecX = leftTopX - leftBottomX;
        double upVecY = leftTopY - leftBottomY;
        double upVecZ = leftTopZ - leftBottomZ;

        double upVecLength = upVecX * upVecX + upVecY * upVecY + upVecZ * upVecZ;

        double toHitX = hit.x - leftBottomX;
        double toHitY = hit.y - leftBottomY;
        double toHitZ = hit.z - leftBottomZ;

        double a = (toHitX * rightVecX + toHitY * rightVecY + toHitZ * rightVecZ) / rightVecLength;
        double b = (toHitX * upVecX + toHitY * upVecY + toHitZ * upVecZ) / upVecLength;

        return a >= 0 && a <= 1 && b >= 0 && b <= 1 ? hit : null;
    }
}