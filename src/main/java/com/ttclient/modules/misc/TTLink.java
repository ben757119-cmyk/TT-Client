package com.ttclient.modules.misc;

import com.ttclient.client.NotificationManager;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Peer link between two TT Client users on the same server.
 * Both must enable TTLink and share the same pair code.
 * Transport: public chat protocol tokens (visible to the server).
 *
 * Protocol: TTLINK|<code>|<CHAT|CMD>|<payload>
 */
public class TTLink extends Module {
    public final NumberSetting pairCode = addSetting(new NumberSetting(
            "PairCode", "Shared 4-digit code (both clients)", 1337, 1000, 9999, 1));
    public final BoolSetting acceptRemote = addSetting(new BoolSetting(
            "AcceptRemote", "Allow partner to make you chat/run cmds", true));
    public final BoolSetting allowChat = addSetting(new BoolSetting(
            "AllowChat", "Allow remote chat messages", true));
    public final BoolSetting allowCmds = addSetting(new BoolSetting(
            "AllowCmds", "Allow remote /commands", true));
    public final BoolSetting notify = addSetting(new BoolSetting(
            "Notify", "Toast on link traffic", true));

    private static final Pattern TOKEN = Pattern.compile(
            "TTLINK\\|(\\d{4})\\|(CHAT|CMD)\\|(.+)", Pattern.CASE_INSENSITIVE);

    private long lastRemoteAt;

    public TTLink() {
        super("TTLink",
                "Link two TT Clients — shared pair code to send chat/cmds",
                Category.MISC);
    }

    /** Send a chat line through your partner (they will type it). */
    public void sendRemoteChat(String message) {
        if (!isEnabled() || message == null || message.isBlank()) return;
        transmit("CHAT", message.trim());
    }

    /** Send a command through your partner (include leading /). */
    public void sendRemoteCmd(String command) {
        if (!isEnabled() || command == null || command.isBlank()) return;
        String c = command.trim();
        if (!c.startsWith("/")) c = "/" + c;
        transmit("CMD", c);
    }

    private void transmit(String type, String payload) {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.player.connection == null) return;
        int code = pairCode.getInt();
        // Strip separators from payload
        payload = payload.replace('|', ' ');
        String packet = "TTLINK|" + code + "|" + type + "|" + payload;
        try {
            mc.player.connection.sendChat(packet);
            if (notify.get()) NotificationManager.push("TTLink", "Sent " + type);
        } catch (Throwable ignored) {}
    }

    /** Called from chat receive pipeline. */
    public void handleChat(String raw) {
        if (!isEnabled() || !acceptRemote.get() || raw == null) return;
        String plain = strip(raw);
        Matcher m = TOKEN.matcher(plain);
        if (!m.find()) {
            // token may be embedded after a name prefix
            int idx = plain.toUpperCase(Locale.ROOT).indexOf("TTLINK|");
            if (idx < 0) return;
            m = TOKEN.matcher(plain.substring(idx));
            if (!m.find()) return;
        }

        int code;
        try {
            code = Integer.parseInt(m.group(1));
        } catch (Exception e) {
            return;
        }
        if (code != pairCode.getInt()) return;

        // Ignore our own echo within 1.5s of send
        if (System.currentTimeMillis() - lastRemoteAt < 400) return;

        String type = m.group(2).toUpperCase(Locale.ROOT);
        String payload = m.group(3).trim();
        if (payload.isEmpty() || payload.length() > 200) return;

        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.player.connection == null) return;

        lastRemoteAt = System.currentTimeMillis();

        try {
            if ("CHAT".equals(type) && allowChat.get()) {
                if (payload.toUpperCase(Locale.ROOT).startsWith("TTLINK|")) return;
                mc.player.connection.sendChat(payload);
                if (notify.get()) NotificationManager.push("TTLink", "Chat: " + payload);
            } else if ("CMD".equals(type) && allowCmds.get()) {
                if (!payload.startsWith("/")) payload = "/" + payload;
                // Prefer command packet if available
                try {
                    mc.player.connection.sendCommand(payload.substring(1));
                } catch (Throwable t) {
                    mc.player.connection.sendChat(payload);
                }
                if (notify.get()) NotificationManager.push("TTLink", "Cmd: " + payload);
            }
        } catch (Throwable ignored) {}
    }

    private static String strip(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\u00A7' && i + 1 < s.length()) {
                i++;
                continue;
            }
            sb.append(c);
        }
        return sb.toString().trim();
    }
}
