package com.ttclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;

/** Lightning Stork activation splash + machine-gun strike FX. */
public final class StorkFx {
    private static final Identifier TEX_ID =
            Identifier.fromNamespaceAndPath("ttclient", "gui/lightning_stork_dyn");
    private static boolean textureReady;
    private static boolean textureFailed;
    private static final Random RND = new Random();

    private static long splashUntil;
    private static boolean gunActive;
    private static long nextFlashAt;
    private static int flashAlpha;
    private static float spinAngle;

    private StorkFx() {}

    public static void startSplash() {
        splashUntil = System.currentTimeMillis() + 3400L;
        spinAngle = 0f;
        ensureTexture();
    }

    public static void setGunActive(boolean on) {
        gunActive = on;
        if (!on) flashAlpha = 0;
    }

    public static void onHit() {
        if (!gunActive) return;
        flashAlpha = 230;
        nextFlashAt = System.currentTimeMillis() + 30L;
    }

    private static String loadB64Part(String name) {
        try (InputStream in = StorkFx.class.getResourceAsStream(
                "/assets/ttclient/textures/gui/" + name)) {
            if (in == null) return "";
            return new String(in.readAllBytes(), StandardCharsets.US_ASCII).replaceAll("\\s", "");
        } catch (Throwable t) {
            return "";
        }
    }

    private static void ensureTexture() {
        if (textureReady || textureFailed) return;
        try {
            String b64 = loadB64Part("lightning_stork_a.b64") + loadB64Part("lightning_stork_b.b64");
            if (b64.isEmpty()) {
                textureFailed = true;
                return;
            }
            byte[] raw = Base64.getDecoder().decode(b64);

            Class<?> niCl = Class.forName("com.mojang.blaze3d.platform.NativeImage");
            Object nativeImg = niCl.getMethod("read", InputStream.class)
                    .invoke(null, new ByteArrayInputStream(raw));
            DynamicTexture dyn = DynamicTexture.class.getConstructor(niCl).newInstance(nativeImg);
            Object texMgr = Minecraft.getInstance().getTextureManager();
            texMgr.getClass().getMethod("register", Identifier.class,
                            Class.forName("net.minecraft.client.renderer.texture.AbstractTexture"))
                    .invoke(texMgr, TEX_ID, dyn);
            textureReady = true;
        } catch (Throwable t) {
            textureFailed = true;
        }
    }

    public static void render(GuiGraphicsExtractor g, Font font, int sw, int sh) {
        long now = System.currentTimeMillis();
        if (now < splashUntil) renderSplash(g, font, sw, sh, now);
        if (gunActive) renderGun(g, font, sw, sh, now);
    }

    private static void renderSplash(GuiGraphicsExtractor g, Font font, int sw, int sh, long now) {
        long left = splashUntil - now;
        float life = 1f - (left / 3400f);
        float alpha;
        if (life < 0.12f) alpha = life / 0.12f;
        else if (life > 0.78f) alpha = (1f - life) / 0.22f;
        else alpha = 1f;
        int a = Math.max(0, Math.min(255, (int) (alpha * 240)));

        g.fill(0, 0, sw, sh, (Math.min(190, a) << 24));

        int img = Math.min(200, Math.min(sw, sh) / 2);
        int ix = (sw - img) / 2;
        int iy = sh / 2 - img / 2 - 36;

        g.fill(ix - 14, iy - 14, ix + img + 14, iy + img + 72, (a << 24) | 0x080A12);
        g.fill(ix - 14, iy - 14, ix - 11, iy + img + 72, (a << 24) | 0x00A8FF);
        g.fill(ix - 14, iy - 14, ix + img + 14, iy - 11, (a << 24) | 0x00A8FF);

        if (textureReady) {
            tryBlit(g, ix, iy, img, img);
        } else {
            int cx = ix + img / 2, cy = iy + img / 2;
            g.fill(cx - 8, cy - 40, cx + 8, cy + 30, (a << 24) | 0xE8F0FF);
            g.fill(cx - 50, cy - 10, cx + 50, cy + 6, (a << 24) | 0xA0C8FF);
            g.fill(cx + 20, cy - 8, cx + 70, cy + 4, (a << 24) | 0x60A0FF);
            g.fill(cx - 6, cy - 55, cx + 18, cy - 35, (a << 24) | 0xFFFFFF);
            g.fill(cx + 14, cy - 48, cx + 36, cy - 42, (a << 24) | 0xFF6A20);
        }

        String t1 = "LIGHTING STORK ACTIVATED";
        String t2 = "360 SPIN INITIADED";
        int ty = iy + img + 20;
        g.text(font, t1, (sw - font.width(t1)) / 2, ty, (a << 24) | 0x00E8F4FF, false);
        g.text(font, t2, (sw - font.width(t2)) / 2, ty + 12, (a << 24) | 0x00FFD24A, false);

        spinAngle += 10f;
        int cx = sw / 2, cy = iy + img / 2;
        for (int i = 0; i < 16; i++) {
            double ang = Math.toRadians(spinAngle + i * 22.5);
            int x1 = cx + (int) (Math.cos(ang) * (img / 2 + 6));
            int y1 = cy + (int) (Math.sin(ang) * (img / 2 + 6));
            int x2 = cx + (int) (Math.cos(ang) * (img / 2 + 20));
            int y2 = cy + (int) (Math.sin(ang) * (img / 2 + 20));
            g.fill(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2) + 2, Math.max(y1, y2) + 2,
                    (a << 24) | 0x00A8FF);
        }
    }

    private static void renderGun(GuiGraphicsExtractor g, Font font, int sw, int sh, long now) {
        if (now >= nextFlashAt) {
            flashAlpha = 170 + RND.nextInt(70);
            nextFlashAt = now + 28 + RND.nextInt(25);
        }
        if (flashAlpha > 0) {
            int a = Math.min(255, flashAlpha);
            int cx = sw / 2, cy = sh / 2;
            int s = 8 + RND.nextInt(22);
            g.fill(cx - s, cy - 2, cx + s, cy + 2, (a << 24) | 0xFFE8A0);
            g.fill(cx - 2, cy - s, cx + 2, cy + s, (a << 24) | 0xFFC040);
            for (int i = 0; i < 7; i++) {
                double ang = RND.nextDouble() * Math.PI * 2;
                int len = 24 + RND.nextInt(60);
                int x2 = cx + (int) (Math.cos(ang) * len);
                int y2 = cy + (int) (Math.sin(ang) * len);
                g.fill(Math.min(cx, x2), Math.min(cy, y2), Math.max(cx, x2) + 1, Math.max(cy, y2) + 1,
                        ((a * 2 / 3) << 24) | 0xFFEE88);
            }
            g.fill(0, 0, sw, 4, ((a / 3) << 24) | 0xFF3030);
            g.fill(0, sh - 4, sw, sh, ((a / 3) << 24) | 0xFF3030);
            g.fill(0, 0, 4, sh, ((a / 4) << 24) | 0xFF3030);
            g.fill(sw - 4, 0, sw, sh, ((a / 4) << 24) | 0xFF3030);
            flashAlpha = Math.max(0, flashAlpha - 28);
        }
        String tag = "MACHINE GUN // STRIKING";
        int tw = font.width(tag);
        g.fill(sw / 2 - tw / 2 - 8, 8, sw / 2 + tw / 2 + 8, 22, 0xD0080A0E);
        g.fill(sw / 2 - tw / 2 - 8, 8, sw / 2 - tw / 2 - 5, 22, 0xFFFF4466);
        g.text(font, tag, (sw - tw) / 2, 11, 0xFFFF6680, false);
    }

    private static void tryBlit(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        try {
            for (var m : g.getClass().getMethods()) {
                if (!m.getName().toLowerCase().contains("blit")) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length >= 9 && Identifier.class.isAssignableFrom(p[0])) {
                    m.invoke(g, TEX_ID, x, y, 0f, 0f, w, h, w, h);
                    return;
                }
                if (p.length >= 6 && Identifier.class.isAssignableFrom(p[0])) {
                    m.invoke(g, TEX_ID, x, y, 0, 0, w, h);
                    return;
                }
            }
            for (var f : g.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object inner = f.get(g);
                if (inner == null) continue;
                for (var m : inner.getClass().getMethods()) {
                    if (!m.getName().toLowerCase().contains("blit")) continue;
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length >= 9 && Identifier.class.isAssignableFrom(p[0])) {
                        m.invoke(inner, TEX_ID, x, y, 0f, 0f, w, h, w, h);
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}
