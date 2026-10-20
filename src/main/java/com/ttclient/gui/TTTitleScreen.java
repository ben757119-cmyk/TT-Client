package com.ttclient.gui;

import com.ttclient.TTClient;
import com.ttclient.client.TTClientClient;
import com.ttclient.modules.client.ClickGUIModule;
import com.ttclient.modules.client.CustomMainMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Clean TT Client title screen — ClickGUI palette, falling particles,
 * watermark + recent updates. Opened via ScreenEvent when CustomMainMenu is on.
 */
public class TTTitleScreen extends Screen {
    private static final int ACCENT = 0xFF00E8A0;
    private static final int TEXT = 0xFFE8EAED;
    private static final int TEXT_DIM = 0xFF6B7280;
    private static final int BTN_BG = 0xF0161A24;
    private static final int BTN_BG_HOV = 0xF0202630;
    private static final int PANEL = 0xE00C0E14;

    private static final String[] UPDATES = {
            "v1.28.8  panel layout saves across relaunch",
            "v1.28.7  ClickGUI contrast + particle rain",
            "v1.28.7  ChestRelay settle + hardened loot",
            "v1.28.6  full settings UI restored",
            "v1.28.5  safer mixins (no ChatScreen crash)",
    };

    private final List<Particle> particles = new ArrayList<>();
    private final List<MenuButton> buttons = new ArrayList<>();

    public TTTitleScreen() {
        super(Component.literal("TT Client"));
    }

    private int accent() {
        try {
            if (TTClientClient.modules != null) {
                ClickGUIModule m = TTClientClient.modules.getModule(ClickGUIModule.class);
                if (m != null) return m.accent.get();
            }
        } catch (Throwable ignored) {}
        return ACCENT;
    }

    @Override
    protected void init() {
        buttons.clear();
        int bw = 160, bh = 22, gap = 6;
        int cx = this.width / 2 - bw / 2;
        int cy = this.height / 2 - 10;
        buttons.add(new MenuButton("Singleplayer", cx, cy, bw, bh, this::openSingleplayer));
        buttons.add(new MenuButton("Multiplayer", cx, cy + (bh + gap), bw, bh, this::openMultiplayer));
        buttons.add(new MenuButton("Options", cx, cy + 2 * (bh + gap), bw, bh, this::openOptions));
        buttons.add(new MenuButton("Quit", cx, cy + 3 * (bh + gap), bw, bh, this::quitGame));
        if (particles.isEmpty() && width > 0) {
            for (int i = 0; i < 90; i++) {
                particles.add(new Particle((float) (Math.random() * Math.max(1, width)),
                        (float) (Math.random() * Math.max(1, height))));
            }
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xFF02040A);
        g.fill(0, 0, width, height, 0x66000000);
        g.fill(0, 0, width, 80, 0x1800E8A0);

        if (particles.isEmpty() && width > 0) {
            for (int i = 0; i < 90; i++)
                particles.add(new Particle((float) (Math.random() * width), (float) (Math.random() * height)));
        }
        int ac = accent() & 0x00FFFFFF;
        for (Particle p : particles) {
            p.y += p.vy;
            if (p.y > height) {
                p.y = -6;
                p.x = (float) (Math.random() * Math.max(1, width));
                p.vy = 0.18f + (float) Math.random() * 1.1f;
                p.size = 1f + (float) Math.random() * 4f;
                p.alpha = 0x28 + (int) (Math.random() * 0x48);
            }
            int s = Math.max(1, (int) p.size);
            g.fill((int) p.x, (int) p.y, (int) p.x + s, (int) p.y + s, (p.alpha << 24) | ac);
        }
    }

    @Override protected void extractBlurredBackground(GuiGraphicsExtractor graphics) {}
    @Override public void extractTransparentBackground(GuiGraphicsExtractor graphics) {}
    @Override protected void extractMenuBackground(GuiGraphicsExtractor graphics) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int ac = accent();
        String title = "TT CLIENT";
        int tw = font.width(title);
        g.text(font, title, width / 2 - tw / 2, height / 2 - 68, ac, false);
        String sub = "v" + TTClient.VERSION + "  ·  utility client";
        g.text(font, sub, width / 2 - font.width(sub) / 2, height / 2 - 54, TEXT_DIM, false);
        int lineW = Math.max(tw, 120);
        g.fill(width / 2 - lineW / 2, height / 2 - 46, width / 2 + lineW / 2, height / 2 - 45, ac & 0x88FFFFFF);

        for (MenuButton b : buttons) {
            boolean hov = mouseX >= b.x && mouseX <= b.x + b.w && mouseY >= b.y && mouseY <= b.y + b.h;
            drawButton(g, b, hov, ac);
        }

        int lx = 12, ly = height - 14 - UPDATES.length * 11 - 28;
        roundedFill(g, lx - 4, ly - 6, lx + 210, height - 10, 6, PANEL);
        g.fill(lx - 4, ly - 6, lx - 2, height - 10, ac);
        g.text(font, "TT Client  v" + TTClient.VERSION, lx, ly, ac, false);
        g.text(font, "recent updates", lx, ly + 12, TEXT_DIM, false);
        for (int i = 0; i < UPDATES.length; i++) {
            g.text(font, UPDATES[i], lx, ly + 24 + i * 11, TEXT, false);
        }

        String mark = "tt";
        g.text(font, mark, width - font.width(mark) - 10, height - 14, ac & 0xAAFFFFFF, false);
    }

    private void drawButton(GuiGraphicsExtractor g, MenuButton b, boolean hov, int ac) {
        roundedFill(g, b.x, b.y, b.x + b.w, b.y + b.h, 5, hov ? BTN_BG_HOV : BTN_BG);
        if (hov) g.fill(b.x, b.y + 3, b.x + 2, b.y + b.h - 3, ac);
        int col = hov ? ac : TEXT;
        g.text(font, b.label, b.x + (b.w - font.width(b.label)) / 2, b.y + (b.h - 8) / 2, col, false);
    }

    private static void roundedFill(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int r, int color) {
        if (x2 <= x1 || y2 <= y1) return;
        if (r < 1) { g.fill(x1, y1, x2, y2, color); return; }
        r = Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
        g.fill(x1 + r, y1, x2 - r, y2, color);
        g.fill(x1, y1 + r, x1 + r, y2 - r, color);
        g.fill(x2 - r, y1 + r, x2, y2 - r, color);
        for (int i = 0; i < r; i++) {
            int off = r - 1 - (int) Math.sqrt((double) (r * r - (r - 1 - i) * (r - 1 - i)));
            g.fill(x1 + off, y1 + i, x1 + r, y1 + i + 1, color);
            g.fill(x2 - r, y1 + i, x2 - off, y1 + i + 1, color);
            g.fill(x1 + off, y2 - 1 - i, x1 + r, y2 - i, color);
            g.fill(x2 - r, y2 - 1 - i, x2 - off, y2 - i, color);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double mx = event.x(), my = event.y();
        for (MenuButton b : buttons) {
            if (mx >= b.x && mx <= b.x + b.w && my >= b.y && my <= b.y + b.h) {
                b.action.run();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void openSingleplayer() {
        openVanilla("net.minecraft.client.gui.screens.worldselection.SelectWorldScreen", this);
    }

    private void openMultiplayer() {
        openVanilla("net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen", this);
    }

    private void openOptions() {
        if (!openVanilla("net.minecraft.client.gui.screens.options.OptionsScreen", this)) {
            openVanilla("net.minecraft.client.gui.screens.OptionsScreen", this);
        }
    }

    private void quitGame() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) mc.stop();
    }

    private boolean openVanilla(String className, Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return false;
        try {
            Class<?> cls = Class.forName(className);
            Object screen = null;
            for (var c : cls.getConstructors()) {
                Class<?>[] p = c.getParameterTypes();
                try {
                    if (p.length == 1 && Screen.class.isAssignableFrom(p[0])) {
                        screen = c.newInstance(parent);
                        break;
                    }
                    if (p.length == 2 && Minecraft.class.isAssignableFrom(p[0]) && Screen.class.isAssignableFrom(p[1])) {
                        screen = c.newInstance(mc, parent);
                        break;
                    }
                    if (p.length == 2 && Screen.class.isAssignableFrom(p[0])) {
                        screen = c.newInstance(parent, mc);
                        break;
                    }
                    if (p.length == 0) {
                        screen = c.newInstance();
                        break;
                    }
                } catch (Throwable ignored) {}
            }
            if (screen instanceof Screen s) {
                try { mc.gui.setScreen(s); return true; } catch (Throwable ignored) {}
                try {
                    Minecraft.class.getMethod("setScreen", Screen.class).invoke(mc, s);
                    return true;
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean isEnabled() {
        try {
            if (TTClientClient.modules == null) return true;
            CustomMainMenu m = TTClientClient.modules.getModule(CustomMainMenu.class);
            return m == null || m.isEnabled();
        } catch (Throwable t) {
            return true;
        }
    }

    private static final class Particle {
        float x, y, vy, size;
        int alpha;
        Particle(float x, float y) {
            this.x = x; this.y = y;
            this.vy = 0.18f + (float) Math.random() * 1.1f;
            this.size = 1f + (float) Math.random() * 4f;
            this.alpha = 0x28 + (int) (Math.random() * 0x48);
        }
    }

    private static final class MenuButton {
        final String label;
        final int x, y, w, h;
        final Runnable action;
        MenuButton(String label, int x, int y, int w, int h, Runnable action) {
            this.label = label; this.x = x; this.y = y; this.w = w; this.h = h; this.action = action;
        }
    }
}
