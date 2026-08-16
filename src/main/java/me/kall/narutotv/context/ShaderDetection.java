package me.kall.narutotv.context;

import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.compat.CompatCenter;
import me.kall.narutotv.data.world.cape.ClientCapes;
import me.kall.narutotv.data.world.wall.ClientWalls;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = NarutoTV.MOD_ID, value = Dist.CLIENT)
public class ShaderDetection {
    private static boolean last;
    private static int interval;

    private static final Component SHADER_COMPAT = Component.translatable("message.narutotv.shader").withStyle(ChatFormatting.YELLOW);

    @SubscribeEvent
    public static void tickClient(ClientTickEvent.Pre event) {
        if (CompatCenter.hasShaderMod()) {
            if (interval >= 0) {
                interval--;
                return;
            }

            interval = 10;

            boolean current = CompatCenter.shaderUsing();
            if (current != last) {
                boolean changed = false;

                if (!ClientWalls.isEmpty()) {
                    ClientWalls.setCompatMode();
                    changed = true;
                }

                if (!ClientCapes.isEmpty()) {
                    ClientCapes.setCompatMode();
                    changed = true;
                }

                if (changed) {
                    LocalPlayer player = Minecraft.getInstance().player;
                    if (player != null) player.displayClientMessage(SHADER_COMPAT, false);
                }
            }

            last = current;
        }
    }
}