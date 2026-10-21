package com.ttclient.client;

import com.ttclient.modules.client.ClickGUIModule;
import com.ttclient.modules.client.StreamerMode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Compact toast notifications — top-right. */
public final class NotificationManager {
    private static final List<Note> NOTES = new ArrayList<>();
    private static final long LIFE_MS = 2600;
    private static final long IN_MS = 180;
    private static final long OUT_MS = 280;

    private NotificationManager() {}

    public static void push(String title, String body) {
        if (StreamerMode.active()) return;
        NOTES.add(0, new Note(title, body, System.currentTimeMillis()));
        while (NOTES.size() > 6) NOTES.remove(NOTES.size() - 1);
    }

    public static void pushToggle(String module, boolean enabled) {
        if (StreamerMode.active()) return;
        if ("StreamerMode".equalsIgnoreCase(module)) return;
        push(module, enabled ? "enabled" : "disabled");
    }

    public static void render(GuiGraphicsExtractor g, Font font, int sw, int sh) {
        if (StreamerMode.active()) {
            NOTES.clear();
            return;
        }
        long now = System.currentTimeMillis();
        int y = 28;
        int accent = ClickGUIModule.globalAccent();
        Iterator<Note> it = NOTES.iterator();
        while (it.hasNext()) {
            Note n = it.next();
            long age = now - n.time;
            if (age > LIFE_MS) {
                it.remove();
                continue;
            }

            float alpha;
            float slide;
            if (age < IN_MS) {
                float t = age / (float) IN_MS;
                t = 1f - (1f - t) * (1f - t);
                alpha = t;
                slide = 1f - t;
            } else if (age > LIFE_MS - OUT_MS) {
                float t = (age - (LIFE_MS - OUT_MS)) / (float) OUT_MS;
                t = t * t;
                alpha = 1f - t;
                slide = t;
            } else {
                alpha = 1f;
                slide = 0f;
            }

            int a = Math.max(0, Math.min(255, (int) (alpha * 230)));
            int panel = (a << 24) | 0x080A0E;
            boolean on = n.body.toLowerCase().contains("enabled") || n.body.toLowerCase().contains("added");
            boolean bad = n.body.toLowerCase().contains("disabled") || n.body.toLowerCase().contains("fail")
                    || n.body.toLowerCase().contains("removed");
            int bar = on && !bad ? accent : (bad ? 0xFFFF4466 : 0xFF6B8CFF);

            int textW = Math.max(font.width(n.title), font.width(n.body));
            int w = textW + 18;
            int h = 24;
            int x = sw - w - 10 + (int) (slide * (w + 20));

            g.fill(x + 1, y + 1, x + w + 1, y + h + 1, (a / 4) << 24);
            g.fill(x, y, x + w, y + h, panel);
            g.fill(x, y, x + 2, y + h, (a << 24) | (bar & 0x00FFFFFF));
            g.text(font, n.title, x + 8, y + 3, (a << 24) | 0x00E8EAED, false);
            g.text(font, n.body, x + 8, y + 13, (a << 24) | (bar & 0x00FFFFFF), false);
            y += h + 5;
        }
    }

    private record Note(String title, String body, long time) {}
}
