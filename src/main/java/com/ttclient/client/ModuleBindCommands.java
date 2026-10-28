package com.ttclient.client;

import com.ttclient.modules.Module;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/** .bind command implementation. */
public final class ModuleBindCommands {
    private ModuleBindCommands() {}

    public static void handle(String[] args) {
        if (args.length < 1) {
            chat("§f.bind <module> <key|none|mouse4|mouse5> §8· §f.bind list");
            return;
        }
        if (args[0].equalsIgnoreCase("list")) {
            if (TTClientClient.modules == null) return;
            int n = 0;
            for (Module m : TTClientClient.modules.getModules()) {
                int k = m.getKeyBind();
                if (k > 0) {
                    chat("§a" + m.getName() + " §7→ §f" + name(k));
                    n++;
                }
            }
            if (n == 0) chat("§7No binds set");
            return;
        }
        if (args.length < 2) {
            chat("§cUsage: .bind <module> <key|none|mouse4|mouse5>");
            return;
        }
        if (TTClientClient.modules == null) return;
        Module mod = TTClientClient.modules.getModule(args[0]);
        if (mod == null) {
            String q = args[0].toLowerCase(Locale.ROOT);
            for (Module m : TTClientClient.modules.getModules()) {
                if (m.getName().toLowerCase(Locale.ROOT).startsWith(q)) { mod = m; break; }
            }
        }
        if (mod == null) { chat("§cModule not found: " + args[0]); return; }
        String keyArg = args[1].toLowerCase(Locale.ROOT);
        int code = -1;
        if (keyArg.equals("none") || keyArg.equals("clear") || keyArg.equals("off")) {
            code = -1;
        } else if (keyArg.equals("mouse4") || keyArg.equals("mb4") || keyArg.equals("m4")) {
            code = 2000 + 3;
        } else if (keyArg.equals("mouse5") || keyArg.equals("mb5") || keyArg.equals("m5")) {
            code = 2000 + 4;
        } else if (keyArg.equals("mmb") || keyArg.equals("middle")) {
            code = 2000 + 2;
        } else if (keyArg.length() == 1) {
            char c = keyArg.charAt(0);
            if (c >= 'a' && c <= 'z') code = GLFW.GLFW_KEY_A + (c - 'a');
            else if (c >= '0' && c <= '9') code = GLFW.GLFW_KEY_0 + (c - '0');
            else { chat("§cUnknown key"); return; }
        } else {
            try {
                var f = GLFW.class.getField("GLFW_KEY_" + keyArg.toUpperCase(Locale.ROOT));
                code = f.getInt(null);
            } catch (Throwable t) {
                chat("§cUnknown key: " + args[1]);
                return;
            }
        }
        mod.setKeyBind(code);
        if (TTClientClient.config != null) TTClientClient.config.save();
        chat("§a" + mod.getName() + " §7bound to §f" + name(code));
    }

    private static String name(int code) {
        if (code <= 0) return "NONE";
        if (code >= 2000) {
            return switch (code - 2000) {
                case 0 -> "LMB"; case 1 -> "RMB"; case 2 -> "MMB";
                case 3 -> "MOUSE4"; case 4 -> "MOUSE5";
                default -> "M" + (code - 2000);
            };
        }
        try {
            String n = GLFW.glfwGetKeyName(code, 0);
            if (n != null && !n.isBlank()) return n.toUpperCase(Locale.ROOT);
        } catch (Throwable ignored) {}
        return "KEY" + code;
    }

    private static void chat(String msg) {
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            Object component = Class.forName("net.minecraft.network.chat.Component")
                    .getMethod("literal", String.class).invoke(null, msg);
            for (var m : mc.player.getClass().getMethods()) {
                if (!m.getName().equals("displayClientMessage") && !m.getName().equals("sendSystemMessage")) continue;
                Class<?>[] pts = m.getParameterTypes();
                if (pts.length == 2 && pts[1] == boolean.class) { m.invoke(mc.player, component, false); return; }
                if (pts.length == 1) { m.invoke(mc.player, component); return; }
            }
        } catch (Throwable ignored) {}
    }
}
