package com.ttclient.client;

import com.ttclient.TTClient;
import com.ttclient.config.ConfigManager;
import com.ttclient.gui.ClickGUIScreen;
import com.ttclient.modules.Module;
import com.ttclient.modules.ModuleManager;
import com.ttclient.modules.client.ClickGUIModule;
import com.ttclient.modules.client.StreamerMode;
import com.ttclient.modules.misc.AutoChatGame;
import com.ttclient.modules.player.Freecam;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = TTClient.MOD_ID, dist = Dist.CLIENT)
public class TTClientClient {

    public static ModuleManager modules;
    public static ConfigManager config;
    public static final int MOUSE_BIND_BASE = 2000;

    public TTClientClient() {
        modules = new ModuleManager();
        TTClient.modules = modules;
        config = new ConfigManager();
        TTClient.config = config;
        modules.init();
        config.load();
        NeoForge.EVENT_BUS.register(this);
        TTClient.LOGGER.info("TT Client {} ready with {} modules",
                TTClient.VERSION, modules.getModules().size());
    }

    private static Screen currentScreen(Minecraft mc) {
        try { return mc.gui.screen(); } catch (Throwable t) { return null; }
    }

    private static void openScreen(Minecraft mc, Screen screen) {
        try { mc.gui.setScreen(screen); return; } catch (Throwable ignored) {}
        try { Minecraft.class.getMethod("setScreen", Screen.class).invoke(mc, screen); } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        if (modules == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        modules.onTick();
    }

    @SubscribeEvent
    public void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Freecam fc = modules != null ? modules.getModule(Freecam.class) : null;
        if (fc != null && fc.isEnabled()) fc.onCameraAngles(event);
    }

    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        Freecam fc = modules != null ? modules.getModule(Freecam.class) : null;
        if (fc == null || !fc.active()) return;
        try {
            Camera cam = null;
            try {
                var m = event.getClass().getMethod("getCamera");
                Object o = m.invoke(event);
                if (o instanceof Camera c) cam = c;
            } catch (Throwable ignored) {}
            if (cam == null) {
                for (var f : event.getClass().getDeclaredFields()) {
                    if (Camera.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Object o = f.get(event);
                        if (o instanceof Camera c) { cam = c; break; }
                    }
                }
            }
            if (cam == null) {
                Minecraft mc = Minecraft.getInstance();
                try { cam = mc.gameRenderer.mainCamera(); } catch (Throwable ignored) {}
            }
            if (cam != null) Freecam.applyTo(cam);
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        if (config != null) config.save();
    }

    @SubscribeEvent
    public void onKey(InputEvent.Key event) {
        if (modules == null) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        int key = event.getKey();
        if (key <= 0 || key == GLFW.GLFW_KEY_UNKNOWN) return;
        Minecraft mc = Minecraft.getInstance();
        Screen screen = currentScreen(mc);

        StreamerMode streamer = modules.getModule(StreamerMode.class);
        if (streamer != null && streamer.getKeyBind() > 0 && streamer.getKeyBind() == key) {
            if (screen == null || screen instanceof ClickGUIScreen) {
                streamer.toggle();
                if (config != null) config.save();
            }
            return;
        }

        ClickGUIModule gui = modules.getModule(ClickGUIModule.class);
        int guiKey = gui != null ? gui.getKeyBind() : GLFW.GLFW_KEY_RIGHT_SHIFT;
        if (guiKey <= 0) guiKey = GLFW.GLFW_KEY_RIGHT_SHIFT;

        if (key == guiKey) {
            if (screen instanceof ClickGUIScreen) {
                openScreen(mc, null);
                if (config != null) config.save();
            } else if (screen == null) {
                openScreen(mc, new ClickGUIScreen());
            }
            return;
        }

        if (screen != null) return;
        for (Module mod : modules.getModules()) {
            if (mod instanceof ClickGUIModule || mod instanceof StreamerMode) continue;
            int bind = mod.getKeyBind();
            if (bind > 0 && bind < MOUSE_BIND_BASE && bind == key) {
                mod.toggle();
                if (config != null) config.save();
            }
        }
    }

    @SubscribeEvent
    public void onMouse(InputEvent.MouseButton.Post event) {
        if (modules == null) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        int button = event.getButton();
        if (button != GLFW.GLFW_MOUSE_BUTTON_4 && button != GLFW.GLFW_MOUSE_BUTTON_5
                && button != GLFW.GLFW_MOUSE_BUTTON_MIDDLE) return;
        Minecraft mc = Minecraft.getInstance();
        if (currentScreen(mc) != null) return;
        int code = MOUSE_BIND_BASE + button;
        for (Module mod : modules.getModules()) {
            if (mod instanceof ClickGUIModule) continue;
            if (mod.getKeyBind() == code) {
                mod.toggle();
                if (config != null) config.save();
            }
        }
    }

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        if (modules == null) return;
        if (StreamerMode.shouldHideLocalUi()) return;
        modules.onRender2D(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    @SubscribeEvent
    public void onClientChat(ClientChatReceivedEvent event) {
        if (modules == null) return;
        String text = null;
        try {
            Component msg = event.getMessage();
            if (msg != null) text = msg.getString();
        } catch (Throwable t) {
            try {
                Object m = event.getClass().getMethod("getMessage").invoke(event);
                if (m instanceof Component c) text = c.getString();
                else if (m != null) text = String.valueOf(m);
            } catch (Throwable ignored) {}
        }
        if (text == null) return;
        AutoChatGame game = modules.getModule(AutoChatGame.class);
        if (game != null && game.isEnabled()) game.handleChat(text);
    }

    @SubscribeEvent
    public void onChatSend(ClientChatEvent event) {
        try {
            String msg = event.getMessage();
            if (msg == null) return;
            if (!msg.trim().startsWith(ClientCommands.PREFIX)) return;
            event.setCanceled(true);
            ClientCommands.handle(msg);
        } catch (Throwable t) {
            try { event.setCanceled(true); } catch (Throwable ignored) {}
        }
    }
}
