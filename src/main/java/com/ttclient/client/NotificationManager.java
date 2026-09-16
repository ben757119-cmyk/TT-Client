package com.ttclient.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class NotificationManager {
    private static final List<Note> NOTES = new ArrayList<>();
    private static final int ACCENT = 0xFF00E8A0;
    private static final int PANEL = 0xDD0A0B0F;

    private NotificationManager() {}

    public static void push(String title, String body) {
        NOTES.add(0, new Note(title, body, System.currentTimeMillis()));
        while (NOTES.size() > 8) NOTES.remove(NOTES.size() - 1);
    }

    public static void pushToggle(String module, boolean enabled) {
        push(module, enabled ? "enabled" : "disabled");
    }

    public static void render(GuiGraphicsExtractor g, Font font, int sw, int sh) {
        long now = System.currentTimeMillis();
        int y = sh / 2 - 40;
        Iterator<Note> it = NOTES.iterator();
        while (it.hasNext()) {
            Note n = it.next();
            long age = now - n.time;
            if (age > 2500) {
                it.remove();
                continue;
            }
            float alpha = age < 200 ? age / 200f : (age > 2200 ? (2500 - age) / 300f : 1f);
            int a = Math.max(0, Math.min(255, (int) (alpha * 220)));
            int panel = (a << 24) | 0x0A0B0F;
            String line = n.title + " §7" + n.body;
            int w = font.width(n.title + " " + n.body) + 14;
            int x = sw - w - 8;
            g.fill(x, y, sw - 4, y + 16, panel);
            g.fill(sw - 4, y, sw - 2, y + 16, ACCENT);
            g.text(font, n.title, x + 6, y + 4, ACCENT, false);
            g.text(font, n.body, x + 10 + font.width(n.title), y + 4, 0xFFAAAAAA, false);
            y += 18;
        }
    }

    private static final class Note {
        final String title;
        final String body;
        final long time;

        Note(String title, String body, long time) {
            this.title = title;
            this.body = body;
            this.time = time;
        }
    }
}
