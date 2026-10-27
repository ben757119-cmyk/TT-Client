package com.ttclient.client;

import com.ttclient.TTClient;
import com.ttclient.config.ConfigManager;
import com.ttclient.gui.ClickGUIScreen;
import com.ttclient.gui.TTBanScreen;
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
import net.neoforged.neoforge.client.event.RenderGuiEvent;
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
                    || next instanceof TTDisconnectedScreen || next instanceof TTBanScreen) return;

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
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || modules == null) return;

        try { ChunkMeshReload.tick(); } catch (Throwable ignored) {}

        try {
            if (mc.player != null) {
                boolean dead = mc.player.isDeadOrDying();
                if (dead && !wasDead) {
                    for (Module m : modules.getModules()) {
                        try {
                            if (m.isEnabled() && m.shouldDisableOnDeath()) m.setEnabled(false);
                        } catch (Throwable ignored) {}
                    }
                }
                wasDead = dead;
            }
        } catch (Throwable ignored) {}

        try {
            long win = mc.getWindow().handle();
            boolean down = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_PERIOD) == GLFW.GLFW_PRESS;
            if (down && !periodWasDown) {
                if (currentScreen(mc) == null) openChatWith(mc, ".");
            }
            periodWasDown = down;
        } catch (Throwable ignored) {}

        try {
            ClickGUIModule gui = modules.getModule(ClickGUIModule.class);
            int guiKey = gui != null && gui.getKeyBind() > 0 ? gui.getKeyBind() : GLFW.GLFW_KEY_RIGHT_SHIFT;
            long win = mc.getWindow().handle();
            boolean down = GLFW.glfwGetKey(win, guiKey) == GLFW.GLFW_PRESS;
            if (down && !guiKeyWasDown) {
                Screen screen = currentScreen(mc);
                if (screen instanceof ClickGUIScreen) openScreen(mc, null);
                else if (screen == null || screen instanceof TTTitleScreen) openScreen(mc, new ClickGUIScreen());
            }
            guiKeyWasDown = down;
        } catch (Throwable ignored) {}

        try { modules.onTick(); } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        try {
            Freecam fc = modules != null ? modules.getModule(Freecam.class) : null;
            if (fc != null) fc.onCameraAngles(event);
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public void onKey(InputEvent.Key event) {
        if (modules == null) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        int key = event.getKey();
        if (key <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        Screen screen = currentScreen(mc);

        ClickGUIModule gui = modules.getModule(ClickGUIModule.class);
        int guiKey = gui != null && gui.getKeyBind() > 0 ? gui.getKeyBind() : GLFW.GLFW_KEY_RIGHT_SHIFT;
        if (key == guiKey) return;

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
        try {
            modules.onRender2D(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
        } catch (Throwable ignored) {}
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
            if (acg != null && acg.isEnabled()) {
                acg.handleChat(event.getMessage().getString());
            }
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        try {
            if (modules == null) return;
            for (Module m : modules.getModules()) {
                try {
                    if (m.isEnabled() && m.shouldDisableOnServerChange()) m.setEnabled(false);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }
}
