package me.kall.narutotv.impl.world.event;

import it.unimi.dsi.fastutil.objects.ObjectCollection;
import me.kall.duplicationless.ext.RegistryEntry;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.impl.config.NarutoConfig;
import me.kall.narutotv.impl.screen.NarutoWorldScreen;
import me.kall.narutotv.impl.world.data.Wall;
import me.kall.narutotv.impl.world.data.client.ClientWalls;
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

        if (!RegistryEntry.get(player.getMainHandItem()).toString().equals(NarutoConfig.teleControl())) return;
        if (!player.isShiftKeyDown()) return;

        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getViewVector(minecraft.getPartialTick());

        ObjectCollection<AbstractRenderer<?>> renderers = ClientWalls.getIn(level.dimension().location());
        if (renderers.isEmpty()) return;

        Wall target = null;
        double minDist = Double.MAX_VALUE;

        for (AbstractRenderer<?> renderer : renderers) {
            Wall wall = ((InWorld)renderer).wall();
            Vec3 intersection = getIntersection(eye, view, wall);
            if (intersection == null) continue;

            double dist = eye.distanceToSqr(intersection);
            if (dist < minDist) {
                minDist = dist;
                target = wall;
            }
        }

        if (target != null) minecraft.setScreen(new NarutoWorldScreen(minecraft.screen, target));
    }

    @Nullable
    private static Vec3 getIntersection(@NotNull Vec3 origin, @NotNull Vec3 direction, @NotNull Wall wall) {
        double leftBottomX = wall.leftBottom.getX();
        double leftBottomY = wall.leftBottom.getY();
        double leftBottomZ = wall.leftBottom.getZ();

        double rightBottomX = wall.rightBottom.getX();
        double rightBottomY = wall.rightBottom.getY();
        double rightBottomZ = wall.rightBottom.getZ();

        double leftTopX = wall.leftTop.getX();
        double leftTopY = wall.leftTop.getY();
        double leftTopZ = wall.leftTop.getZ();

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

        double hitDist = ((leftBottomX - originX) * normalX + (leftBottomY - originY) * normalY + (leftBottomZ - originZ) * normalZ) / denominator;
        if (hitDist < 0) return null;

        double hitX = originX + directionX * hitDist;
        double hitY = originY + directionY * hitDist;
        double hitZ = originZ + directionZ * hitDist;

        double rightVecX = rightBottomX - leftBottomX;
        double rightVecY = rightBottomY - leftBottomY;
        double rightVecZ = rightBottomZ - leftBottomZ;

        double rightVecLength = rightVecX * rightVecX + rightVecY * rightVecY + rightVecZ * rightVecZ;

        double upVecX = leftTopX - leftBottomX;
        double upVecY = leftTopY - leftBottomY;
        double upVecZ = leftTopZ - leftBottomZ;

        double upVecLength = upVecX * upVecX + upVecY * upVecY + upVecZ * upVecZ;

        double toHitX = hitX - leftBottomX;
        double toHitY = hitY - leftBottomY;
        double toHitZ = hitZ - leftBottomZ;

        double localU = (toHitX * rightVecX + toHitY * rightVecY + toHitZ * rightVecZ) / rightVecLength;
        double localV = (toHitX * upVecX + toHitY * upVecY + toHitZ * upVecZ) / upVecLength;

        return localU >= 0 && localU <= 1 && localV >= 0 && localV <= 1 ? new Vec3(hitX, hitY, hitZ) : null;
    }
}