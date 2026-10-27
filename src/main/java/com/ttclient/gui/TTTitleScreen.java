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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

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
        int ac = accent();
        for (Particle p : particles) {
            p.x += p.vx;
            p.y += p.vy;
            if (p.y > height + 4) {
                p.y = -4;
                p.x = (float) (Math.random() * width);
                p.respawn();
            }
            int col;
            if (seizure) {
                float hue = (float) (((System.currentTimeMillis() / 8 + p.hueOffset) % 360) / 360.0);
                col = (java.awt.Color.HSBtoRGB(hue, 1f, 1f) & 0x00FFFFFF) | (p.alpha << 24);
            } else {
                col = (p.alpha << 24) | (ac & 0x00FFFFFF);
            }
            int s = Math.max(1, (int) p.size);
            g.fill((int) p.x, (int) p.y, (int) p.x + s, (int) p.y + s, col);
        }
    }

    @Override protected void extractBlurredBackground(GuiGraphicsExtractor g) {}
    @Override public void extractTransparentBackground(GuiGraphicsExtractor g) {}
    @Override protected void extractMenuBackground(GuiGraphicsExtractor g) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int ac = accent();
        g.fill(0, 0, width, 2, ac);
        g.fill(0, height - 2, width, height, ac);

        String title = "TT CLIENT";
        g.text(font, title, width / 2 - font.width(title) / 2, height / 2 - 68, ac, false);
        String ver = "v" + TTClient.VERSION;
        g.text(font, ver, width / 2 - font.width(ver) / 2, height / 2 - 54, TEXT_DIM, false);

        for (MenuButton b : buttons) {
            boolean hov = mouseX >= b.x && mouseX <= b.x + b.w && mouseY >= b.y && mouseY <= b.y + b.h;
            drawButton(g, b, hov, ac);
        }
    }

    private void drawButton(GuiGraphicsExtractor g, MenuButton b, boolean hov, int ac) {
        roundedFill(g, b.x, b.y, b.x + b.w, b.y + b.h, 4, hov ? BTN_HOV : BTN);
        if (hov) {
            g.fill(b.x, b.y, b.x + 2, b.y + b.h, ac);
        }
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
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        Object options = resolveOptions(mc);
        String[] names = {
                "net.minecraft.client.gui.screens.options.OptionsScreen",
                "net.minecraft.client.gui.screens.OptionsScreen",
                "net.minecraft.client.gui.screens.options.OptionsSubScreen"
        };

        for (String className : names) {
            if (tryOpenOptionsClass(mc, className, options)) return;
        }

        // Last resort: scan loaded classes for *OptionsScreen with a usable ctor
        try {
            for (Package pkg : Package.getPackages()) {
                // too broad — skip
            }
        } catch (Throwable ignored) {}

        TTClient.LOGGER.warn("Could not open OptionsScreen — ctor mismatch (options={})",
                options == null ? "null" : options.getClass().getName());
    }

    private static Object resolveOptions(Minecraft mc) {
        Object options = null;
        try { options = mc.options; } catch (Throwable ignored) {}
        if (options != null) return options;
        try {
            Field f = Minecraft.class.getField("options");
            options = f.get(mc);
            if (options != null) return options;
        } catch (Throwable ignored) {}
        try {
            for (Field f : Minecraft.class.getDeclaredFields()) {
                f.setAccessible(true);
                Object v = f.get(mc);
                if (v == null) continue;
                String n = v.getClass().getName().toLowerCase();
                if (n.endsWith(".options") || n.contains("clientoptions") || n.endsWith("options"))
                    return v;
            }
        } catch (Throwable ignored) {}
        try { return Minecraft.class.getMethod("getOptions").invoke(mc); } catch (Throwable ignored) {}
        return null;
    }

    private boolean tryOpenOptionsClass(Minecraft mc, String className, Object options) {
        try {
            Class<?> cls = Class.forName(className);
            if (Modifier.isAbstract(cls.getModifiers())) return false;

            Constructor<?>[] ctors = cls.getDeclaredConstructors();
            // Prefer (Screen, Options)
            for (Constructor<?> c : ctors) {
                c.setAccessible(true);
                Class<?>[] p = c.getParameterTypes();
                if (p.length != 2) continue;
                if (!Screen.class.isAssignableFrom(p[0])) continue;
                if (options == null) continue;
                if (p[1].isInstance(options) || p[1].isAssignableFrom(options.getClass())
                        || p[1].getName().toLowerCase().contains("options")) {
                    try {
                        Object screen = c.newInstance(this, options);
                        if (screen instanceof Screen s) {
                            setScreen(mc, s);
                            return true;
                        }
                    } catch (Throwable t) {
                        TTClient.LOGGER.debug("Options ctor failed: {}", t.toString());
                    }
                }
            }
            // (Options, Screen)
            for (Constructor<?> c : ctors) {
                c.setAccessible(true);
                Class<?>[] p = c.getParameterTypes();
                if (p.length != 2 || options == null) continue;
                if (Screen.class.isAssignableFrom(p[1])
                        && (p[0].isInstance(options) || p[0].getName().toLowerCase().contains("options"))) {
                    try {
                        Object screen = c.newInstance(options, this);
                        if (screen instanceof Screen s) {
                            setScreen(mc, s);
                            return true;
                        }
                    } catch (Throwable ignored) {}
                }
            }
            // (Screen only)
            for (Constructor<?> c : ctors) {
                c.setAccessible(true);
                Class<?>[] p = c.getParameterTypes();
                if (p.length == 1 && Screen.class.isAssignableFrom(p[0])) {
                    try {
                        Object screen = c.newInstance(this);
                        if (screen instanceof Screen s) {
                            setScreen(mc, s);
                            return true;
                        }
                    } catch (Throwable ignored) {}
                }
            }
            // (Minecraft, Screen) / (Screen, Minecraft)
            for (Constructor<?> c : ctors) {
                c.setAccessible(true);
                Class<?>[] p = c.getParameterTypes();
                if (p.length != 2) continue;
                try {
                    Object screen = null;
                    if (Screen.class.isAssignableFrom(p[0]) && Minecraft.class.isAssignableFrom(p[1]))
                        screen = c.newInstance(this, mc);
                    else if (Minecraft.class.isAssignableFrom(p[0]) && Screen.class.isAssignableFrom(p[1]))
                        screen = c.newInstance(mc, this);
                    if (screen instanceof Screen s) {
                        setScreen(mc, s);
                        return true;
                    }
                } catch (Throwable ignored) {}
            }
        } catch (ClassNotFoundException e) {
            // not present on this mapping
        } catch (Throwable t) {
            TTClient.LOGGER.warn("Options open via {}: {}", className, t.toString());
        }
        return false;
    }

    private void quitGame() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        setScreen(mc, new TTConfirmScreen(this, "Quit Game?", "Close Minecraft completely.", () -> {
            try { mc.stop(); } catch (Throwable ignored) {}
        }));
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
            for (Constructor<?> c : cls.getDeclaredConstructors()) {
                c.setAccessible(true);
                Class<?>[] p = c.getParameterTypes();
                try {
                    if (p.length == 1 && Screen.class.isAssignableFrom(p[0])) {
                        screen = c.newInstance(parent); break;
                    }
                } catch (Throwable ignored) {}
            }
            if (screen == null) {
                for (Constructor<?> c : cls.getDeclaredConstructors()) {
                    c.setAccessible(true);
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
