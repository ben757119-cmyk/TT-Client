package com.ttclient.client;

import com.ttclient.TTClient;
import com.ttclient.modules.Module;
import com.ttclient.modules.client.ArrayListMod;
import com.ttclient.modules.client.HUD;
import com.ttclient.modules.client.Keystrokes;
import com.ttclient.modules.client.Notifications;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Draws client HUD overlays after vanilla HUD (RenderGuiEvent.Post).
 */
public class HudRenderer {

    private static final int ACCENT = 0xFF00E8A0;
    private static final int TEXT = 0xFFE8EAED;
    private static final int DIM = 0xFF8B95A8;
    private static final int PANEL = 0xAA0A0B0F;

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (mc.gui.screen() != null) return; // don't draw over menus

        GuiGraphicsExtractor g = event.getGuiGraphics();
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        HUD hud = TTClient.modules != null ? TTClient.modules.getModule(HUD.class) : null;
        if (hud != null && hud.isEnabled()) {
            drawHud(g, font, mc, hud, sw, sh);
        }

        ArrayListMod array = TTClient.modules != null ? TTClient.modules.getModule(ArrayListMod.class) : null;
        if (array != null && array.isEnabled()) {
            drawArrayList(g, font, mc, array, sw);
        }

        Keystrokes keys = TTClient.modules != null ? TTClient.modules.getModule(Keystrokes.class) : null;
        if (keys != null && keys.isEnabled()) {
            drawKeystrokes(g, font, mc, keys, sw, sh);
        }

        Notifications notifMod = TTClient.modules != null ? TTClient.modules.getModule(Notifications.class) : null;
        if (notifMod != null && notifMod.isEnabled()) {
            NotificationManager.render(g, font, sw, sh);
        }
    }

    private void drawHud(GuiGraphicsExtractor g, Font font, Minecraft mc, HUD hud, int sw, int sh) {
        int y = 4;
        if (hud.watermark.get()) {
            String w = "TT Client " + TTClient.VERSION;
            g.fill(2, y - 1, 6 + font.width(w), y + 10, PANEL);
            g.text(font, w, 4, y, ACCENT, false);
            y += 12;
        }
        if (hud.fps.get()) {
            String s = mc.getFps() + " FPS";
            g.text(font, s, 4, y, TEXT, false);
            y += 10;
        }
        if (hud.coords.get() && mc.player != null) {
            String s = String.format("XYZ %.1f %.1f %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            g.text(font, s, 4, y, TEXT, false);
            y += 10;
        }
        if (hud.direction.get() && mc.player != null) {
            String s = "Facing " + mc.player.getDirection().getSerializedName();
            g.text(font, s, 4, y, DIM, false);
        }
    }

    private void drawArrayList(GuiGraphicsExtractor g, Font font, Minecraft mc, ArrayListMod mod, int sw) {
        if (TTClient.modules == null) return;
        List<Module> enabled = new ArrayList<>();
        for (Module m : TTClient.modules.getModules()) {
            if (m.isEnabled() && !(m instanceof ArrayListMod) && !(m instanceof HUD)) {
                enabled.add(m);
            }
        }
        enabled.sort(Comparator.comparingInt((Module m) -> -font.width(m.getName())));

        int y = 4;
        long time = System.currentTimeMillis();
        for (int i = 0; i < enabled.size(); i++) {
            Module m = enabled.get(i);
            String name = m.getName();
            int w = font.width(name);
            int x = sw - w - 6;
            int color = ACCENT;
            if (mod.rainbow.get()) {
                float hue = ((time / 20f) + i * 12) % 360 / 360f;
                color = 0xFF000000 | java.awt.Color.HSBtoRGB(hue, 0.7f, 1f);
            }
            g.fill(x - 3, y - 1, sw - 2, y + 10, PANEL);
            g.fill(sw - 2, y - 1, sw, y + 10, color);
            g.text(font, name, x, y, color, false);
            y += 11;
        }
    }

    private void drawKeystrokes(GuiGraphicsExtractor g, Font font, Minecraft mc, Keystrokes mod, int sw, int sh) {
        if (mc.options == null) return;
        long window = mc.getWindow().handle();
        int size = 18;
        int gap = 2;
        int baseX = sw / 2 - (size * 3 + gap * 2) / 2;
        int baseY = sh - 90;

        drawKey(g, font, baseX + size + gap, baseY, size, "W", mc.options.keyUp.isDown());
        drawKey(g, font, baseX, baseY + size + gap, size, "A", mc.options.keyLeft.isDown());
        drawKey(g, font, baseX + size + gap, baseY + size + gap, size, "S", mc.options.keyDown.isDown());
        drawKey(g, font, baseX + (size + gap) * 2, baseY + size + gap, size, "D", mc.options.keyRight.isDown());
        drawKey(g, font, baseX, baseY + (size + gap) * 2, size * 3 + gap * 2, "SPACE", mc.options.keyJump.isDown());

        if (mod.mouse.get()) {
            boolean lmb = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean rmb = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            int mx = baseX + (size + gap) * 3 + 8;
            drawKey(g, font, mx, baseY + size + gap, size, "L", lmb);
            drawKey(g, font, mx + size + gap, baseY + size + gap, size, "R", rmb);
        }
    }

    private void drawKey(GuiGraphicsExtractor g, Font font, int x, int y, int w, String label, boolean down) {
        int bg = down ? 0xCC00E8A0 : PANEL;
        int fg = down ? 0xFF04120C : TEXT;
        g.fill(x, y, x + w, y + 18, bg);
        int tw = font.width(label);
        g.text(font, label, x + (w - tw) / 2, y + 5, fg, false);
    }
}
