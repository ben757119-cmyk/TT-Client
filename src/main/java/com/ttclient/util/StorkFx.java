package com.ttclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Random;

/**
 * Im Storking It FX — giant crosshair stays up from activation until target death
 * (or module disable). Chat lines + strike flashes.
 */
public final class StorkFx {
    private static final Random RND = new Random();

    /** Crosshair active from enable until death/disable (not a short timed splash). */
    private static boolean splashActive;
    private static long splashStartedAt;
    private static long splashFadeOutUntil; // after death, brief fade then clear
    private static boolean gunActive;
    private static long nextFlashAt;
    private static int flashAlpha;
    private static float spinAngle;
    private static boolean spokenActivate;
    private static boolean spokenDeath;

    private StorkFx() {}

    public static void startSplash() {
        splashActive = true;
        splashStartedAt = System.currentTimeMillis();
        splashFadeOutUntil = 0L;
        spinAngle = 0f;
        spokenActivate = false;
        spokenDeath = false;
        speak("Want to see death in its cold dark eyes?",
                "Want to see death in its cold dark eyes?");
        spokenActivate = true;
    }

    /** Target died — say the line, then fade crosshair out over ~1.2s. */
    public static void speakDeath() {
        if (spokenDeath) return;
        spokenDeath = true;
        speak("Death embraces you.", "Death embraces you.");
        // Keep crosshair visible briefly then fade; user asked it last until they die
        splashFadeOutUntil = System.currentTimeMillis() + 1200L;
    }

    /** Module disabled / cancelled — drop FX immediately. */
    public static void endSplash() {
        splashActive = false;
        splashFadeOutUntil = 0L;
        gunActive = false;
        flashAlpha = 0;
    }

    public static void setGunActive(boolean on) {
        gunActive = on;
        if (!on) flashAlpha = 0;
    }

    public static void onHit() {
        flashAlpha = 240;
        nextFlashAt = System.currentTimeMillis() + 24L;
    }

    private static void speak(String narratorLine, String notifyBody) {
        chatClientOnly("§c[Stork] §f" + notifyBody);
        try {
            Minecraft mc = Minecraft.getInstance();
            try {
                Object narrator = mc.getClass().getMethod("getNarrator").invoke(mc);
                if (narrator != null) {
                    for (var m : narrator.getClass().getMethods()) {
                        if (!m.getName().equals("say") && !m.getName().equals("saySystemMessage")) continue;
                        Class<?>[] p = m.getParameterTypes();
                        if (p.length == 2 && p[0] == String.class && p[1] == boolean.class) {
                            m.invoke(narrator, narratorLine, true);
                            break;
                        }
                        if (p.length == 1 && p[0] == String.class) {
                            m.invoke(narrator, narratorLine);
                            break;
                        }
                    }
                }
            } catch (Throwable ignored) {}
            try {
                Class.forName("com.ttclient.client.NotificationManager")
                        .getMethod("push", String.class, String.class)
                        .invoke(null, "Im Storking It", notifyBody);
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    private static void chatClientOnly(String body) {
        if (body == null) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            Component comp = Component.literal(body);

            if (mc.player != null) {
                Object player = mc.player;
                Class<?> c = player.getClass();
                while (c != null && c != Object.class) {
                    for (var m : c.getDeclaredMethods()) {
                        String name = m.getName();
                        if (!(name.equals("displayClientMessage")
                                || name.equals("sendSystemMessage")
                                || name.equals("sendMessage"))) continue;
                        Class<?>[] pt = m.getParameterTypes();
                        try {
                            m.setAccessible(true);
                            if (pt.length == 1 && Component.class.isAssignableFrom(pt[0])) {
                                m.invoke(player, comp);
                                return;
                            }
                            if (pt.length == 2 && Component.class.isAssignableFrom(pt[0])
                                    && (pt[1] == boolean.class || pt[1] == Boolean.class)) {
                                m.invoke(player, comp, false);
                                return;
                            }
                        } catch (Throwable ignored) {}
                    }
                    c = c.getSuperclass();
                }
            }

            if (mc.gui != null) {
                Object chat = null;
                try {
                    chat = mc.gui.getClass().getMethod("getChat").invoke(mc.gui);
                } catch (Throwable ignored) {}
                if (chat == null) {
                    Class<?> gc = mc.gui.getClass();
                    while (gc != null && gc != Object.class && chat == null) {
                        for (var f : gc.getDeclaredFields()) {
                            try {
                                String cn = f.getType().getName().toLowerCase(Locale.ROOT);
                                if (cn.contains("chat")) {
                                    f.setAccessible(true);
                                    Object v = f.get(mc.gui);
                                    if (v != null) { chat = v; break; }
                                }
                            } catch (Throwable ignored) {}
                        }
                        gc = gc.getSuperclass();
                    }
                }
                if (chat != null) {
                    for (var am : chat.getClass().getMethods()) {
                        if (!am.getName().equals("addMessage")) continue;
                        Class<?>[] pt = am.getParameterTypes();
                        try {
                            if (pt.length == 1 && Component.class.isAssignableFrom(pt[0])) {
                                am.invoke(chat, comp);
                                return;
                            }
                            if (pt.length >= 1 && Component.class.isAssignableFrom(pt[0])) {
                                Object[] args = new Object[pt.length];
                                args[0] = comp;
                                for (int i = 1; i < pt.length; i++) {
                                    if (pt[i] == boolean.class || pt[i] == Boolean.class) args[i] = false;
                                    else if (pt[i] == int.class || pt[i] == Integer.class) args[i] = 0;
                                    else args[i] = null;
                                }
                                am.invoke(chat, args);
                                return;
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void render(GuiGraphicsExtractor g, Font font, int sw, int sh) {
        long now = System.currentTimeMillis();

        // After death: fade then clear
        if (splashFadeOutUntil > 0L) {
            if (now >= splashFadeOutUntil) {
                splashActive = false;
                splashFadeOutUntil = 0L;
            }
        }

        if (splashActive) renderCrosshairSplash(g, sw, sh, now);
        if (gunActive) renderGun(g, font, sw, sh, now);
    }

    private static void renderCrosshairSplash(GuiGraphicsExtractor g, int sw, int sh, long now) {
        float alpha = 1f;
        // Fade in first 400ms
        long alive = now - splashStartedAt;
        if (alive < 400L) alpha = alive / 400f;
        // Fade out after death
        if (splashFadeOutUntil > 0L) {
            long left = splashFadeOutUntil - now;
            alpha = Math.min(alpha, Math.max(0f, left / 1200f));
        }
        int a = Math.max(0, Math.min(255, (int) (alpha * 230)));
        if (a <= 0) return;

        g.fill(0, 0, sw, sh, (Math.min(160, a) << 24));

        int cx = sw / 2;
        int cy = sh / 2;
        spinAngle += 6f;

        int ringR = Math.min(sw, sh) * 28 / 100;
        for (int i = 0; i < 24; i++) {
            double ang = Math.toRadians(spinAngle + i * 15.0);
            int len = (i % 3 == 0) ? 18 : 10;
            int x1 = cx + (int) (Math.cos(ang) * ringR);
            int y1 = cy + (int) (Math.sin(ang) * ringR);
            int x2 = cx + (int) (Math.cos(ang) * (ringR + len));
            int y2 = cy + (int) (Math.sin(ang) * (ringR + len));
            thickLine(g, x1, y1, x2, y2, 2, (a << 24) | 0xE8F0FF);
        }

        int arm = Math.min(sw, sh) * 22 / 100;
        int gap = 18;
        int t = 4;
        int col = (a << 24) | 0xFF3040;
        int colCore = (a << 24) | 0xFFFFFF;

        g.fill(cx - arm, cy - t, cx - gap, cy + t, col);
        g.fill(cx + gap, cy - t, cx + arm, cy + t, col);
        g.fill(cx - t, cy - arm, cx + t, cy - gap, col);
        g.fill(cx - t, cy + gap, cx + t, cy + arm, col);
        g.fill(cx - 2, cy - 2, cx + 2, cy + 2, colCore);

        int br = arm + 12;
        int bl = 16;
        int bt = 3;
        g.fill(cx - br, cy - br, cx - br + bl, cy - br + bt, col);
        g.fill(cx - br, cy - br, cx - br + bt, cy - br + bl, col);
        g.fill(cx + br - bl, cy - br, cx + br, cy - br + bt, col);
        g.fill(cx + br - bt, cy - br, cx + br, cy - br + bl, col);
        g.fill(cx - br, cy + br - bt, cx - br + bl, cy + br, col);
        g.fill(cx - br, cy + br - bl, cx - br + bt, cy + br, col);
        g.fill(cx + br - bl, cy + br - bt, cx + br, cy + br, col);
        g.fill(cx + br - bt, cy + br - bl, cx + br, cy + br, col);

        int pulse = 6 + (int) (4 * Math.sin(now / 80.0));
        g.fill(cx - pulse, cy - 1, cx + pulse, cy + 1, (a << 24) | 0xFF6680);
        g.fill(cx - 1, cy - pulse, cx + 1, cy + pulse, (a << 24) | 0xFF6680);
    }

    private static void thickLine(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int t, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        if (steps < 1) {
            g.fill(x1 - t / 2, y1 - t / 2, x1 + t / 2 + 1, y1 + t / 2 + 1, color);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            g.fill(x - t / 2, y - t / 2, x + t / 2 + 1, y + t / 2 + 1, color);
        }
    }

    private static void renderGun(GuiGraphicsExtractor g, Font font, int sw, int sh, long now) {
        if (now >= nextFlashAt) {
            flashAlpha = 180 + RND.nextInt(70);
            nextFlashAt = now + 22 + RND.nextInt(30);
        }
        if (flashAlpha > 0) {
            int a = Math.min(255, flashAlpha);
            int cx = sw / 2, cy = sh / 2;
            int s = 14 + RND.nextInt(28);
            g.fill(cx - s, cy - 2, cx + s, cy + 2, (a << 24) | 0xFFE8A0);
            g.fill(cx - 2, cy - s, cx + 2, cy + s, (a << 24) | 0xFFC040);
            for (int i = 0; i < 8; i++) {
                double ang = RND.nextDouble() * Math.PI * 2;
                int len = 30 + RND.nextInt(70);
                int x2 = cx + (int) (Math.cos(ang) * len);
                int y2 = cy + (int) (Math.sin(ang) * len);
                thickLine(g, cx, cy, x2, y2, 2, ((a * 2 / 3) << 24) | 0xFFEE88);
            }
            g.fill(0, 0, sw, 5, ((a / 3) << 24) | 0xFF3030);
            g.fill(0, sh - 5, sw, sh, ((a / 3) << 24) | 0xFF3030);
            flashAlpha = Math.max(0, flashAlpha - 32);
        }
        String tag = "STRIKING";
        int tw = font.width(tag);
        g.fill(sw / 2 - tw / 2 - 8, 8, sw / 2 + tw / 2 + 8, 22, 0xD0080A0E);
        g.fill(sw / 2 - tw / 2 - 8, 8, sw / 2 - tw / 2 - 5, 22, 0xFFFF4466);
        g.text(font, tag, (sw - tw) / 2, 11, 0xFFFF6680, false);
    }
}
