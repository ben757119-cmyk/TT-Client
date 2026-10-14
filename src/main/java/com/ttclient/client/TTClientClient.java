package com.ttclient.client;

import com.ttclient.TTClient;
import com.ttclient.config.ConfigManager;
import com.ttclient.gui.ClickGUIScreen;
import com.ttclient.modules.Module;
import com.ttclient.modules.ModuleManager;
import com.ttclient.modules.client.ClickGUIModule;
import com.ttclient.modules.misc.AutoChatGame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/**
 * Client entry. Event handlers are instance methods registered via
 * NeoForge.EVENT_BUS.register(this). Do NOT use @EventBusSubscriber here —
 * that path requires static @SubscribeEvent methods.
 */
@Mod(value = TTClient.MOD_ID, dist = Dist.CLIENT)
public class TTClientClient {

    public static ModuleManager modules;
    public static ConfigManager config;

    public TTClientClient() {
        modules = new ModuleManager();
        TTClient.modules = modules;
        config = new ConfigManager();
        TTClient.config = config;
        modules.init();
        config.load();
        NeoForge.EVENT_BUS.register(this);
        TTClient.LOGGER.info("TT Client {} ready with {} modules", TTClient.VERSION, modules.getModules().size());
    }

    private static Screen currentScreen(Minecraft mc) {
        try {
            return mc.gui.screen();
        } catch (Throwable t) {
            return null;
        }
    }

    private static void openScreen(Minecraft mc, Screen screen) {
        try {
            mc.gui.setScreen(screen);
            return;
        } catch (Throwable ignored) {}
        try {
            Minecraft.class.getMethod("setScreen", Screen.class).invoke(mc, screen);
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        if (modules == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        modules.onTick();
    }

    @SubscribeEvent
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        if (config != null) config.save();
    }

    @SubscribeEvent
    public void onKey(InputEvent.Key event) {
        if (modules == null) return;
        Minecraft mc = Minecraft.getInstance();
        Screen screen = currentScreen(mc);
        if (screen != null && !(screen instanceof ClickGUIScreen)) return;

        if (event.getAction() == GLFW.GLFW_PRESS) {
            ClickGUIModule gui = modules.getModule(ClickGUIModule.class);
            if (gui != null && event.getKey() == gui.getKeyBind()) {
                if (screen instanceof ClickGUIScreen) {
                    openScreen(mc, null);
                    if (config != null) config.save();
                } else if (screen == null) {
                    openScreen(mc, new ClickGUIScreen());
                }
                return;
            }

            for (Module mod : modules.getModules()) {
                if (mod.getKeyBind() == event.getKey() && !(mod instanceof ClickGUIModule)) {
                    mod.toggle();
                    if (config != null) config.save();
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        if (modules == null) return;
        modules.onRender2D(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    @SubscribeEvent
    public void onClientChat(ClientChatReceivedEvent event) {
        if (modules == null) return;
        AutoChatGame game = modules.getModule(AutoChatGame.class);
        if (game == null || !game.isEnabled()) return;
        try {
            Component msg = event.getMessage();
            if (msg != null) game.handleChat(msg.getString());
        } catch (Throwable t) {
            try {
                Object m = event.getClass().getMethod("getMessage").invoke(event);
                if (m instanceof Component c) game.handleChat(c.getString());
                else if (m != null) game.handleChat(String.valueOf(m));
            } catch (Throwable ignored) {}
        }
    }
}
