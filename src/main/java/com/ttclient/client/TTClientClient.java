package com.ttclient.client;

import com.ttclient.TTClient;
import com.ttclient.gui.ClickGUIScreen;
import com.ttclient.gui.TTBanScreen;
import com.ttclient.gui.TTConnectScreen;
import com.ttclient.gui.TTDisconnectedScreen;
import com.ttclient.gui.TTTitleScreen;
import com.ttclient.modules.Module;
import com.ttclient.modules.ModuleManager;
import com.ttclient.modules.client.ClickGUIModule;
import com.ttclient.modules.client.CustomMainMenu;
import com.ttclient.modules.player.Freecam;
import com.ttclient.util.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public class TTClientClient {
    public static ModuleManager modules;
    public static ConfigManager config;

    private boolean periodWasDown;
    private boolean guiKeyWasDown;

    public TTClientClient() {
        modules = new ModuleManager();
        modules.init();
        config = new ConfigManager();
        try { config.load(); } catch (Throwable ignored) {}
        NeoForge.EVENT_BUS.register(this);
    }

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    private static Screen currentScreen(Minecraft mc) {
        try { return mc.gui.screen(); } catch (Throwable t) {
            try {
                Object gui = mc.getClass().getField("gui").get(mc);
                if (gui != null) return (Screen) gui.getClass().getMethod("screen").invoke(gui);
            } catch (Throwable ignored) {}
            return null;
        }
    }

    @SubscribeEvent
    public void onScreenOpen(ScreenEvent.Opening event) {
        try {
            Screen next = event.getScreen();
            if (next == null) return;
            if (next instanceof TTTitleScreen || next instanceof TTConnectScreen
                    || next instanceof TTDisconnectedScreen || next instanceof TTBanScreen) return;

            String n = next.getClass().getName().toLowerCase(Locale.ROOT);
            String simple = next.getClass().getSimpleName().toLowerCase(Locale.ROOT);

            boolean isTitle = next instanceof TitleScreen
                    || n.endsWith(".titlescreen")
                    || simple.equals("titlescreen");
            if (isTitle && TTTitleScreen.isEnabled()) {
                event.setNewScreen(new TTTitleScreen());
                return;
            }

            boolean isConnect = (n.contains("connect") && !n.contains("disconnect"))
                    || n.contains("progressscreen")
                    || n.contains("receivinglevel")
                    || simple.contains("connect") && !simple.contains("disconnect");
            if (isConnect) {
                String status = "Connecting";
                if (n.contains("receiving") || simple.contains("receiving")) status = "Receiving terrain";
                else if (n.contains("progress") || n.contains("download")) status = "Loading";
                event.setNewScreen(new TTConnectScreen(next, status));
                return;
            }

            if (n.contains("disconnected") || simple.contains("disconnected")) {
                String reason = extractDisconnectReason(next);
                if (TTBanScreen.looksLikeBan(reason)) {
                    event.setNewScreen(new TTBanScreen(reason));
                } else {
                    event.setNewScreen(new TTDisconnectedScreen(reason));
                }
            }
        } catch (Throwable ignored) {}
    }

    private static String extractDisconnectReason(Screen s) {
        String best = null;
        try {
            for (var f : s.getClass().getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object v = f.get(s);
                    if (v instanceof Component c) {
                        String text = c.getString();
                        if (text != null && !text.isBlank() && text.length() > 2) {
                            if (best == null || text.length() > best.length()) best = text;
                        }
                    }
                    if (v instanceof String str && str.length() > 2) {
                        if (best == null || str.length() > best.length()) best = str;
                    }
                    if (v instanceof java.util.Collection<?> col) {
                        for (Object o : col) {
                            if (o instanceof Component c2) {
                                String text = c2.getString();
                                if (text != null && text.length() > 2
                                        && (best == null || text.length() > best.length()))
                                    best = text;
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return best != null ? best : "Disconnected from server";
    }

    @SubscribeEvent
    public void onScreenRender(ScreenEvent.Render.Post event) {
        try {
            Screen s = event.getScreen();
            if (s == null) return;
            if (s instanceof TTConnectScreen || s instanceof TTDisconnectedScreen
                    || s instanceof TTBanScreen || s instanceof TTTitleScreen) return;
            GuiGraphicsExtractor g = event.getGuiGraphics();
            if (g == null) return;
            TTBrand.drawAll(g, s);
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = mc();
        if (mc == null) return;

        try {
            long win = mc.getWindow().handle();
            boolean down = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_PERIOD) == GLFW.GLFW_PRESS;
            if (down && !periodWasDown) {
                Screen cur = currentScreen(mc);
                if (cur == null) {
                    try { mc.gui.setScreen(new ChatScreen(".")); } catch (Throwable ignored) {}
                }
            }
            periodWasDown = down;
        } catch (Throwable ignored) {}

        try {
            ClickGUIModule guiMod = modules != null ? modules.getModule(ClickGUIModule.class) : null;
            int guiKey = guiMod != null ? guiMod.getKeyBind() : GLFW.GLFW_KEY_RIGHT_SHIFT;
            if (guiKey > 0) {
                long win = mc.getWindow().handle();
                boolean down = GLFW.glfwGetKey(win, guiKey) == GLFW.GLFW_PRESS;
                if (down && !guiKeyWasDown) {
                    Screen screen = currentScreen(mc);
                    if (screen instanceof ClickGUIScreen) {
                        try { mc.gui.setScreen(null); } catch (Throwable ignored) {}
                    } else if (screen == null || screen instanceof TTTitleScreen) {
                        try { mc.gui.setScreen(new ClickGUIScreen()); } catch (Throwable ignored) {}
                    }
                }
                guiKeyWasDown = down;
            }
        } catch (Throwable ignored) {}

        if (modules != null) modules.onTick();

        Freecam fc = modules != null ? modules.getModule(Freecam.class) : null;
        // freecam camera angles handled elsewhere
    }

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        try {
            if (modules == null) return;
            Minecraft mc = mc();
            if (mc == null) return;
            Screen s = currentScreen(mc);
            if (s != null && !(s instanceof ClickGUIScreen)) return;
            GuiGraphicsExtractor g = event.getGuiGraphics();
            if (g == null) return;
            HudRenderer.render(g, event.getPartialTick());
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public void onKey(InputEvent.Key event) {
        // ClickGUI open/close is edge-triggered in onClientTick only — never toggle it here.
        try {
            if (modules == null) return;
            if (event.getAction() != GLFW.GLFW_PRESS) return;
            int key = event.getKey();
            if (key <= 0) return;
            Screen s = currentScreen(mc());
            if (s instanceof ClickGUIScreen) return;
            for (Module m : modules.getModules()) {
                if (m instanceof ClickGUIModule) continue;
                if (m.getKeyBind() == key) m.toggle();
            }
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public void onMouse(InputEvent.MouseButton.Pre event) {
        try {
            if (modules == null) return;
            if (event.getAction() != GLFW.GLFW_PRESS) return;
            int btn = event.getButton();
            // mouse binds: GLFW mouse buttons map as negative-ish or high keys depending on module
            for (Module m : modules.getModules()) {
                int bind = m.getKeyBind();
                if (bind < 0 && btn == -bind - 100) { // optional encoding
                    m.toggle();
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void chat(String msg) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.player != null) {
                mc.player.displayClientMessage(Component.literal(msg), false);
            }
        } catch (Throwable ignored) {}
    }

    public static void chat(Component c) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.player != null) {
                mc.player.displayClientMessage(c, false);
            }
        } catch (Throwable ignored) {}
    }
}
