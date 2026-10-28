package com.ttclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Random;

/**
 * Im Storking It FX — giant crosshair activation splash, spoken lines, strike flashes.
 * No stork image; pure geometric crosshair.
 */
public final class StorkFx {
    private static final Random RND = new Random();

    private static long splashUntil;
    private static boolean gunActive;
    private static long nextFlashAt;
    private static int flashAlpha;
    private static float spinAngle;
    private static boolean spokenActivate;
    private static boolean spokenDeath;

    private StorkFx() {}

    public static void startSplash() {
        splashUntil = System.currentTimeMillis() + 3200L;
        spinAngle = 0f;
        spokenActivate = false;
        spokenDeath = false;
        speak("Want to see death in its cold dark eyes?",
                "Want to see death in its cold dark eyes?");
        spokenActivate = true;
    }

    public static void speakDeath() {
        if (spokenDeath) return;
        spokenDeath = true;
        speak("Death embraces you.", "Death embraces you.");
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
                            return;
                        }
                        if (p.length == 1 && p[0] == String.class) {
                            m.invoke(narrator, narratorLine);
                            return;
                        }
                    }
                }
            } catch (Throwable ignored) {}
            try {
                Class<?> nCl = Class.forName("com.mojang.text2speech.Narrator");
                Object n = nCl.getMethod("getNarrator").invoke(null);
                nCl.getMethod("say", String.class, boolean.class).invoke(n, narratorLine, true);
                return;
            } catch (Throwable ignored) {}
            try {
                Class.forName("com.ttclient.client.NotificationManager")
                        .getMethod("push", String.class, String.class)
                        .invoke(null, "Im Storking It", notifyBody);
            } catch (Throwable ignored) {}
            try {
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§c" + notifyBody), false);
                }
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    public static void render(GuiGraphicsExtractor g, Font font, int sw, int sh) {
        long now = System.currentTimeMillis();
        if (now < splashUntil) renderCrosshairSplash(g, sw, sh, now);
        if (gunActive) renderGun(g, font, sw, sh, now);
    }

    /** Giant animated crosshair — replaces the old stork image. */
    private static void renderCrosshairSplash(GuiGraphicsExtractor g, int sw, int sh, long now) {
        long left = splashUntil - now;
        float life = 1f - (left / 3200f);
        float alpha;
        if (life < 0.15f) alpha = life / 0.15f;
        else if (life > 0.75f) alpha = (1f - life) / 0.25f;
        else alpha = 1f;
        int a = Math.max(0, Math.min(255, (int) (alpha * 230)));

        // dark veil
        g.fill(0, 0, sw, sh, (Math.min(180, a) << 24));

        int cx = sw / 2;
        int cy = sh / 2;
        spinAngle += 6f;

        // outer rotating ring ticks
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

        // giant crosshair arms
        int arm = Math.min(sw, sh) * 22 / 100;
        int gap = 18;
        int t = 4;
        int col = (a << 24) | 0xFF3040;
        int colCore = (a << 24) | 0xFFFFFF;

        // horizontal
        g.fill(cx - arm, cy - t, cx - gap, cy + t, col);
        g.fill(cx + gap, cy - t, cx + arm, cy + t, col);
        // vertical
        g.fill(cx - t, cy - arm, cx + t, cy - gap, col);
        g.fill(cx - t, cy + gap, cx + t, cy + arm, col);

        // inner bright core
        g.fill(cx - 2, cy - 2, cx + 2, cy + 2, colCore);

        // corner brackets
        int br = arm + 12;
        int bl = 16;
        int bt = 3;
        // top-left
        g.fill(cx - br, cy - br, cx - br + bl, cy - br + bt, col);
        g.fill(cx - br, cy - br, cx - br + bt, cy - br + bl, col);
        // top-right
        g.fill(cx + br - bl, cy - br, cx + br, cy - br + bt, col);
        g.fill(cx + br - bt, cy - br, cx + br, cy - br + bl, col);
        // bottom-left
        g.fill(cx - br, cy + br - bt, cx - br + bl, cy + br, col);
        g.fill(cx - br, cy + br - bl, cx - br + bt, cy + br, col);
        // bottom-right
        g.fill(cx + br - bl, cy + br - bt, cx + br, cy + br, col);
        g.fill(cx + br - bt, cy + br - bl, cx + br, cy + br, col);

        // pulsing diamond
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
            // crosshair flash
            int s = 14 + RND.nextInt(28);
            g.fill(cx - s, cy - 2, cx + s, cy + 2, (a << 24) | 0xFFE8A0);
            g.fill(cx - 2, cy - s, cx + 2, cy + s, (a << 24) | 0xFFC040);
            // lightning spokes
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
