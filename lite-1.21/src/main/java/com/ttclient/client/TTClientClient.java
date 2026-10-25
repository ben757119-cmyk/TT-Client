package com.ttclient.client;

import com.ttclient.TTClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

@Mod(value = TTClient.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = TTClient.MOD_ID, value = Dist.CLIENT)
public class TTClientClient {
    private static boolean fullbright = false;
    private static boolean sprint = true;
    private static double savedGamma = 0.5;

    public TTClientClient() {
        TTClient.LOGGER.info("TT Client Lite client ready (1.21.x)");
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options == null) return;

        if (sprint && mc.player.input != null && mc.player.input.forwardImpulse > 0.01f) {
            mc.options.keySprint.setDown(true);
        }

        if (fullbright) {
            try {
                if (mc.options.gamma().get() < 16.0) {
                    savedGamma = mc.options.gamma().get();
                    mc.options.gamma().set(16.0);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;

        if (event.getKey() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            fullbright = !fullbright;
            if (!fullbright && mc.options != null) {
                try {
                    mc.options.gamma().set(savedGamma);
                } catch (Throwable ignored) {
                }
            }
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    Component.literal("§d[TT] §fFullbright " + (fullbright ? "§aON" : "§cOFF")
                        + " §7| Sprint " + (sprint ? "§aON" : "§cOFF")),
                    true);
            }
        }
        if (event.getKey() == GLFW.GLFW_KEY_G && (event.getModifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
            sprint = !sprint;
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    Component.literal("§d[TT] §fSprint " + (sprint ? "§aON" : "§cOFF")), true);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        GuiGraphics g = event.getGuiGraphics();
        g.drawString(mc.font, "§dTT Client §7Lite §8| §f" + TTClient.VERSION, 4, 4, 0xFFFFFF, true);
        g.drawString(mc.font, "§7" + mc.getFps() + " fps"
            + (fullbright ? " §eFB" : "")
            + (sprint ? " §aSPR" : ""), 4, 14, 0xFFFFFF, true);
    }
}
