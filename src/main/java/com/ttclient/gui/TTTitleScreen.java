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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/** Dense-particle TT title — ClickGUI + SeizureMode available from here. */
public class TTTitleScreen extends Screen {
    private static final int TEXT = 0xFFE8EAED;
    private static final int TEXT_DIM = 0xFF6B7280;
    private static final int BTN = 0xE0121620;
    private static final int BTN_HOV = 0xF01A2030;
    private static final int PARTICLE_COUNT = 360;

    private final List<Particle> particles = new ArrayList<>();
    private final List<MenuButton> buttons = new ArrayList<>();

    public TTTitleScreen() {
        super(Component.literal("TT Client"));
    }

    private int accent() {
        return ClickGUIModule.globalAccent();
    }

    private void ensureParticles() {
        if (width <= 0 || height <= 0) return;
        while (particles.size() < PARTICLE_COUNT) {
            particles.add(new Particle(
                    (float) (Math.random() * width),
                    (float) (Math.random() * height)));
        }
    }

    @Override
    protected void init() {
        buttons.clear();
        int bw = 180, bh = 26, gap = 8;
        int cx = width / 2 - bw / 2;
        int cy = height / 2 - 20;
        buttons.add(new MenuButton("Singleplayer", cx, cy, bw, bh, this::openSingleplayer));
        buttons.add(new MenuButton("Multiplayer", cx, cy + (bh + gap), bw, bh, this::openMultiplayer));
        buttons.add(new MenuButton("ClickGUI", cx, cy + 2 * (bh + gap), bw, bh, this::openClickGui));
        buttons.add(new MenuButton("Options", cx, cy + 3 * (bh + gap), bw, bh, this::openOptions));
        buttons.add(new MenuButton("Quit Game", cx, cy + 4 * (bh + gap), bw, bh, this::quitGame));
        particles.clear();
        ensureParticles();
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xFF05060A);
        ensureParticles();
        boolean seizure = ClickGUIModule.isSeizure();
        for (Particle p : particles) {
            p.y += p.vy;
            p.x += p.vx;
            if (p.y > height + 6) {
                p.y = -6;
                p.x = (float) (Math.random() * Math.max(1, width));
                p.respawn();
            }
            if (p.x < -8) p.x = width + 4;
            if (p.x > width + 8) p.x = -4;
            int s = Math.max(1, (int) p.size);
            int rgb;
            if (seizure) {
                float hue = ((System.currentTimeMillis() + p.hueOffset) % 1200L) / 1200f;
                rgb = java.awt.Color.HSBtoRGB(hue, 1f, 1f) & 0x00FFFFFF;
            } else {
                rgb = accent() & 0x00FFFFFF;
            }
            g.fill((int) p.x, (int) p.y, (int) p.x + s, (int) p.y + s, (p.alpha << 24) | rgb);
        }
        g.fill(0, 0, width, 36, 0x40000000);
        g.fill(0, height - 36, width, height, 0x50000000);
    }

    @Override protected void extractBlurredBackground(GuiGraphicsExtractor graphics) {}
    @Override public void extractTransparentBackground(GuiGraphicsExtractor graphics) {}
    @Override protected void extractMenuBackground(GuiGraphicsExtractor graphics) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int ac = accent();
        String title = "TT CLIENT";
        int tw = font.width(title);
        int titleY = height / 2 - 88;
        g.text(font, title, width / 2 - tw / 2, titleY, TEXT, false);
        String sub = "v" + TTClient.VERSION;
        g.text(font, sub, width / 2 - font.width(sub) / 2, titleY + 14, TEXT_DIM, false);
        int lineW = 48;
        g.fill(width / 2 - lineW / 2, titleY + 28, width / 2 + lineW / 2, titleY + 29, ac);

        for (MenuButton b : buttons) {
            boolean hov = mouseX >= b.x && mouseX <= b.x + b.w && mouseY >= b.y && mouseY <= b.y + b.h;
            drawButton(g, b, hov, ac);
        }

        String tip = ClickGUIModule.isSeizure()
                ? "seizure mode  ·  rshift / clickgui"
                : "rshift or clickgui  ·  the third";
        g.text(font, tip, 12, height - 16, TEXT_DIM, false);
    }

    private void drawButton(GuiGraphicsExtractor g, MenuButton b, boolean hov, int ac) {
        roundedFill(g, b.x, b.y, b.x + b.w, b.y + b.h, 6, hov ? BTN_HOV : BTN);
        if (hov) g.fill(b.x, b.y + 4, b.x + 2, b.y + b.h - 4, ac);
        g.fill(b.x + 6, b.y + b.h - 1, b.x + b.w - 6, b.y + b.h, hov ? (ac & 0x55FFFFFF) : 0x18FFFFFF);
        g.text(font, b.label, b.x + (b.w - font.width(b.label)) / 2, b.y + (b.h - 8) / 2, hov ? ac : TEXT, false);
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

    private void openClickGui() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        setScreen(mc, new ClickGUIScreen());
    }

    private void openSingleplayer() {
        if (!openVanilla("net.minecraft.client.gui.screens.worldselection.SelectWorldScreen", this)) {
            openVanilla("net.minecraft.client.gui.screens.worldselection.WorldSelectionList", this);
        }
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

    private static void setScreen(Minecraft mc, Screen s) {
        try { mc.gui.setScreen(s); return; } catch (Throwable ignored) {}
        try { Minecraft.class.getMethod("setScreen", Screen.class).invoke(mc, s); } catch (Throwable ignored) {}
        try {
            for (var m : Minecraft.class.getMethods()) {
                if (m.getName().equals("setScreen") && m.getParameterCount() == 1) {
                    m.invoke(mc, s); return;
                }
            }
        } catch (Throwable ignored) {}
    }

    private boolean openVanilla(String className, Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return false;
        try {
            Class<?> cls = Class.forName(className);
            Object screen = null;
            for (Constructor<?> c : cls.getConstructors()) {
                Class<?>[] p = c.getParameterTypes();
                try {
                    if (p.length == 1 && Screen.class.isAssignableFrom(p[0])) {
                        screen = c.newInstance(parent); break;
                    }
                } catch (Throwable ignored) {}
            }
            if (screen == null) {
                for (Constructor<?> c : cls.getConstructors()) {
                    Class<?>[] p = c.getParameterTypes();
                    try {
                        if (p.length == 0) { screen = c.newInstance(); break; }
                        if (p.length == 2 && Screen.class.isAssignableFrom(p[0])) {
                            Object a2 = Minecraft.class.isAssignableFrom(p[1]) ? mc : null;
                            screen = c.newInstance(parent, a2); break;
                        }
                        if (p.length == 2 && Minecraft.class.isAssignableFrom(p[0]) && Screen.class.isAssignableFrom(p[1])) {
                            screen = c.newInstance(mc, parent); break;
                        }
                    } catch (Throwable ignored) {}
                }
            }
            if (screen instanceof Screen s) {
                setScreen(mc, s);
                return true;
            }
        } catch (Throwable t) {
            TTClient.LOGGER.warn("TTTitleScreen open failed for {}: {}", className, t.toString());
        }
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
        float x, y, vx, vy, size;
        int alpha;
        int hueOffset;

        Particle(float x, float y) {
            this.x = x;
            this.y = y;
            respawn();
        }

        void respawn() {
            this.vy = 0.15f + (float) Math.random() * 1.1f;
            this.vx = ((float) Math.random() - 0.5f) * 0.4f;
            this.size = 1f + (float) Math.random() * 3.2f;
            this.alpha = 0x22 + (int) (Math.random() * 0x48);
            this.hueOffset = (int) (Math.random() * 1200);
        }
    }

    private static final class MenuButton {
        final String label;
        final int x, y, w, h;
        final Runnable action;

        MenuButton(String label, int x, int y, int w, int h, Runnable action) {
            this.label = label;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.action = action;
        }
    }
}
