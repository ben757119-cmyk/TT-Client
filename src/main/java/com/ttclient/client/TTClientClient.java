package com.ttclient.client;

import com.ttclient.TTClient;
import com.ttclient.config.ConfigManager;
import com.ttclient.gui.ClickGUIScreen;
import com.ttclient.gui.TTConnectScreen;
import com.ttclient.gui.TTDisconnectedScreen;
import com.ttclient.gui.TTTitleScreen;
import com.ttclient.modules.Module;
import com.ttclient.modules.ModuleManager;
import com.ttclient.modules.client.ClickGUIModule;
import com.ttclient.modules.client.StreamerMode;
import com.ttclient.modules.misc.AutoChatGame;
import com.ttclient.modules.player.Freecam;
import com.ttclient.util.ChunkMeshReload;
import com.ttclient.util.FriendsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = TTClient.MOD_ID, dist = Dist.CLIENT)
public class TTClientClient {

    public static ModuleManager modules;
    public static ConfigManager config;
    public static final int MOUSE_BIND_BASE = 2000;

    private boolean periodWasDown = false;
    private boolean guiKeyWasDown = false;
    private boolean wasDead = false;

    public TTClientClient() {
        modules = new ModuleManager();
        TTClient.modules = modules;
        config = new ConfigManager();
        TTClient.config = config;
        modules.init();
        config.load();
        FriendsManager.load();
        NeoForge.EVENT_BUS.register(this);
        TTClient.LOGGER.info("TT Client {} ready with {} modules",
                TTClient.VERSION, modules.getModules().size());
    }

    private static Screen currentScreen(Minecraft mc) {
        try { return mc.gui.screen(); } catch (Throwable t) {
            try { return (Screen) Minecraft.class.getMethod("getScreen").invoke(mc); }
            catch (Throwable t2) { return null; }
        }
    }

    private static void openScreen(Minecraft mc, Screen screen) {
        try { mc.gui.setScreen(screen); return; } catch (Throwable ignored) {}
        try { Minecraft.class.getMethod("setScreen", Screen.class).invoke(mc, screen); } catch (Throwable ignored) {}
    }

    private static void openChatWith(Minecraft mc, String initial) {
        try {
            try {
                var ctor = ChatScreen.class.getConstructor(String.class);
                openScreen(mc, ctor.newInstance(initial));
                return;
            } catch (Throwable ignored) {}
            try {
                var ctor = ChatScreen.class.getConstructor(String.class, boolean.class);
                openScreen(mc, ctor.newInstance(initial, false));
                return;
            } catch (Throwable ignored) {}
            try {
                openScreen(mc, ChatScreen.class.getDeclaredConstructor().newInstance());
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public void onScreenOpen(ScreenEvent.Opening event) {
        try {
            Screen next = event.getScreen();
            if (next == null) return;
            if (next instanceof TTTitleScreen || next instanceof TTConnectScreen
                    || next instanceof TTDisconnectedScreen) return;

            String n = next.getClass().getName().toLowerCase();
            String simple = next.getClass().getSimpleName().toLowerCase();

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
                event.setNewScreen(new TTDisconnectedScreen(reason));
            }
        } catch (Throwable ignored) {}
    }

    private static String extractDisconnectReason(Screen s) {
        try {
            for (var f : s.getClass().getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object v = f.get(s);
                    if (v instanceof Component c) {
                        String t = c.getString();
                        if (t != null && !t.isBlank() && t.length() > 2) return t;
                    }
                    if (v instanceof String str && str.length() > 2) return str;
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return "Disconnected from server";
    }

    @SubscribeEvent
    public void onScreenRender(ScreenEvent.Render.Post event) {
        try {
            Screen s = event.getScreen();
            if (s == null) return;
            if (s instanceof TTConnectScreen || s instanceof TTDisconnectedScreen
                    || s instanceof TTTitleScreen) return;
            GuiGraphicsExtractor g = event.getGuiGraphics();
            if (g == null) return;
            TTBrand.drawAll(g, s);
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        ChunkMeshReload.tick();
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        try {
            long win = mc.getWindow().handle();
            boolean down = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_PERIOD) == GLFW.GLFW_PRESS;
            if (down && !periodWasDown && currentScreen(mc) == null && mc.player != null) {
                openChatWith(mc, ".");
            }
            periodWasDown = down;
        } catch (Throwable ignored) {}

        try {
            if (modules != null) {
                ClickGUIModule gui = modules.getModule(ClickGUIModule.class);
                int guiKey = gui != null && gui.getKeyBind() > 0 ? gui.getKeyBind() : GLFW.GLFW_KEY_RIGHT_SHIFT;
                if (guiKey < MOUSE_BIND_BASE) {
                    long win = mc.getWindow().handle();
                    boolean down = GLFW.glfwGetKey(win, guiKey) == GLFW.GLFW_PRESS;
                    if (down && !guiKeyWasDown) {
                        Screen screen = currentScreen(mc);
                        if (screen instanceof ClickGUIScreen) {
                            openScreen(mc, null);
                            if (config != null) config.save();
                        } else if (screen == null || screen instanceof TTTitleScreen) {
                            openScreen(mc, new ClickGUIScreen());
                        }
                    }
                    guiKeyWasDown = down;
                }
            }
        } catch (Throwable ignored) {}

        if (modules == null) return;
        if (mc.player == null) {
            wasDead = false;
            return;
        }

        try {
            boolean dead = mc.player.isDeadOrDying() || mc.player.getHealth() <= 0.0f;
            if (dead && !wasDead) {
                modules.disableModulesOnDeath();
            }
            wasDead = dead;
        } catch (Throwable ignored) {}

        modules.onTick();
    }

    @SubscribeEvent
    public void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Freecam fc = modules != null ? modules.getModule(Freecam.class) : null;
        if (fc != null && fc.isEnabled()) fc.onCameraAngles(event);
    }

    @SubscribeEvent
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        if (modules != null) {
            try { modules.disableModulesOnServerChange(); } catch (Throwable ignored) {}
        }
        wasDead = false;
        if (config != null) config.save();
        FriendsManager.save();
    }

    @SubscribeEvent
    public void onKey(InputEvent.Key event) {
        if (modules == null) return;
        int action = event.getAction();
        if (action != GLFW.GLFW_PRESS && action != 1) return;
        int key = event.getKey();
        if (key <= 0 || key == GLFW.GLFW_KEY_UNKNOWN) return;
        Minecraft mc = Minecraft.getInstance();
        Screen screen = currentScreen(mc);

        if (screen == null && (key == GLFW.GLFW_KEY_PERIOD || key == 46)) {
            openChatWith(mc, ".");
            return;
        }

        StreamerMode streamer = modules.getModule(StreamerMode.class);
        if (streamer != null && streamer.getKeyBind() > 0 && streamer.getKeyBind() == key) {
            if (screen == null || screen instanceof ClickGUIScreen) {
                streamer.toggle();
                if (config != null) config.save();
            }
            return;
        }

        if (screen != null && !(screen instanceof ClickGUIScreen) && !(screen instanceof TTTitleScreen)) return;

        for (Module m : modules.getModules()) {
            if (m.getKeyBind() > 0 && m.getKeyBind() == key && m.getKeyBind() < MOUSE_BIND_BASE) {
                m.toggle();
                if (config != null) config.save();
                return;
            }
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatEvent event) {
        try {
            if (ClientCommands.handle(event.getMessage())) {
                event.setCanceled(true);
            }
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        try {
            AutoChatGame acg = modules != null ? modules.getModule(AutoChatGame.class) : null;
            if (acg != null && acg.isEnabled()) acg.onChat(event);
        } catch (Throwable ignored) {}
    }
}
