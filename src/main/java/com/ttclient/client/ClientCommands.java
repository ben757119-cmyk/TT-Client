package com.ttclient.client;

import com.ttclient.TTClient;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.modules.player.ChestRelay;
import com.ttclient.modules.render.BlockESP;
import com.ttclient.modules.world.Nuker;
import com.ttclient.util.FriendsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Client-only chat commands starting with ".". Keep public PREFIX, chatOnly, tabComplete. */
public final class ClientCommands {
    public static final String PREFIX = ".";

    private ClientCommands() {}

    public static boolean handle(String raw) {
        if (raw == null) return false;
        String msg = raw.trim();
        if (!msg.startsWith(PREFIX)) return false;
        String body = msg.substring(PREFIX.length()).trim();
        if (body.isEmpty()) return true;
        List<String> parts = splitArgs(body);
        if (parts.isEmpty()) return true;
        String cmd = parts.get(0).toLowerCase(Locale.ROOT);
        List<String> args = parts.size() > 1 ? parts.subList(1, parts.size()) : List.of();
        try {
            switch (cmd) {
                case "help", "h", "?" -> help();
                case "v", "version" -> chat("TT Client " + TTClient.VERSION);
                case "t", "toggle" -> toggle(args);
                case "modules", "mods", "list" -> listModules(args);
                case "b", "block", "blockesp", "be" -> blockEsp(args);
                case "nuker", "n", "nuke" -> nukerCmd(args);
                case "chest", "chests", "cr", "chestrelay" -> chestRelay(args);
                case "friend", "friends", "f" -> friends(args);
                case "bind" -> chat("Right-click a module in ClickGUI to set its bind");
                case "ping" -> chat("pong");
                case "coords", "pos" -> coords();
                case "say" -> {
                    if (args.isEmpty()) chatOnly("Usage: .say <message>");
                    else {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null && mc.player.connection != null)
                            mc.player.connection.sendChat(String.join(" ", args));
                    }
                }
                default -> chatOnly("Unknown command. .help for list");
            }
        } catch (Throwable t) {
            chatOnly("Command error: " + t.getClass().getSimpleName());
        }
        return true;
    }

    public static boolean isClientCommand(String msg) {
        return msg != null && msg.trim().startsWith(PREFIX);
    }

    public static String tabComplete(String buffer) {
        if (buffer == null || !buffer.startsWith(PREFIX)) return null;
        String body = buffer.substring(PREFIX.length());
        if (!body.contains(" ")) {
            String partial = body.toLowerCase(Locale.ROOT);
            List<String> cmds = List.of(
                    "help", "version", "toggle", "modules", "block", "nuker", "chest", "friend", "say", "coords", "ping", "bind"
            );
            List<String> hits = new ArrayList<>();
            for (String c : cmds) if (c.startsWith(partial)) hits.add(PREFIX + c);
            if (hits.isEmpty()) return null;
            if (hits.size() == 1) {
                String h = hits.get(0);
                if (h.equals(PREFIX + "toggle") || h.equals(PREFIX + "say") || h.equals(PREFIX + "chest")
                        || h.equals(PREFIX + "block") || h.equals(PREFIX + "friend") || h.equals(PREFIX + "nuker"))
                    return h + " ";
                return h;
            }
            chatOnly("§7" + String.join(" §8| §7", hits));
            return commonPrefix(hits);
        }
        List<String> parts = splitArgs(body);
        if (parts.isEmpty()) return null;
        String cmd = parts.get(0).toLowerCase(Locale.ROOT);
        if (cmd.equals("toggle") || cmd.equals("t")) {
            String partial = parts.size() > 1 ? parts.get(1).toLowerCase(Locale.ROOT) : "";
            if (TTClientClient.modules == null) return null;
            List<String> hits = new ArrayList<>();
            for (Module m : TTClientClient.modules.getModules()) {
                if (m.getName().toLowerCase(Locale.ROOT).startsWith(partial))
                    hits.add(PREFIX + "toggle " + m.getName());
            }
            if (hits.isEmpty()) return null;
            if (hits.size() == 1) return hits.get(0);
            chatOnly("§7" + String.join(" §8| §7", hits.stream().limit(8).toList()));
            return commonPrefix(hits);
        }
        if (cmd.equals("friend") || cmd.equals("f") || cmd.equals("friends")) {
            String partial = parts.size() > 1 ? parts.get(1).toLowerCase(Locale.ROOT) : "";
            List<String> hits = new ArrayList<>();
            for (String s : List.of("add", "remove", "list", "clear"))
                if (s.startsWith(partial)) hits.add(PREFIX + "friend " + s);
            if (hits.size() == 1) return hits.get(0);
            if (!hits.isEmpty()) return commonPrefix(hits);
        }
        if (cmd.equals("chest") || cmd.equals("cr")) {
            String partial = parts.size() > 1 ? parts.get(1).toLowerCase(Locale.ROOT) : "";
            List<String> hits = new ArrayList<>();
            for (String s : List.of("add", "list", "clear", "remove"))
                if (s.startsWith(partial)) hits.add(PREFIX + "chest " + s);
            if (hits.size() == 1) return hits.get(0);
            if (!hits.isEmpty()) return commonPrefix(hits);
        }
        if (cmd.equals("b") || cmd.equals("block")) {
            String partial = parts.size() > 1 ? parts.get(1).toLowerCase(Locale.ROOT) : "";
            List<String> hits = new ArrayList<>();
            for (String s : List.of("add", "remove", "list", "clear"))
                if (s.startsWith(partial)) hits.add(PREFIX + "b " + s);
            if (hits.size() == 1) return hits.get(0);
            if (!hits.isEmpty()) return commonPrefix(hits);
        }
        if (cmd.equals("nuker") || cmd.equals("n") || cmd.equals("nuke")) {
            String partial = parts.size() > 1 ? parts.get(1).toLowerCase(Locale.ROOT) : "";
            List<String> hits = new ArrayList<>();
            for (String s : List.of("add", "remove", "list", "clear"))
                if (s.startsWith(partial)) hits.add(PREFIX + "nuker " + s);
            if (hits.size() == 1) return hits.get(0);
            if (!hits.isEmpty()) return commonPrefix(hits);
        }
        return null;
    }

    private static String commonPrefix(List<String> hits) {
        if (hits.isEmpty()) return null;
        String p = hits.get(0);
        for (String h : hits) {
            while (!h.startsWith(p) && !p.isEmpty()) p = p.substring(0, p.length() - 1);
        }
        return p.isEmpty() ? null : p;
    }

    private static void help() {
        chatOnly("§f.help §7— this list");
        chatOnly("§f.t <module> §7— toggle module");
        chatOnly("§f.modules [cat] §7— list modules");
        chatOnly("§f.b add|remove|list|clear <block>");
        chatOnly("§f.nuker add|remove|list|clear <block>");
        chatOnly("§f.chest ... §7— ChestRelay helpers");
        chatOnly("§f.friend add|remove|list <name>");
        chatOnly("§f.coords §7— print position");
        chatOnly("§f.say <msg> §7— send chat to server");
    }

    private static void toggle(List<String> args) {
        if (args.isEmpty()) { chatOnly("Usage: .t <module>"); return; }
        if (TTClientClient.modules == null) return;
        Module m = TTClientClient.modules.getModule(args.get(0));
        if (m == null) { chatOnly("No module: " + args.get(0)); return; }
        m.toggle();
        chatOnly(m.getName() + " " + (m.isEnabled() ? "§aON" : "§cOFF"));
        if (TTClientClient.config != null) TTClientClient.config.save();
    }

    private static void listModules(List<String> args) {
        if (TTClientClient.modules == null) return;
        if (!args.isEmpty()) {
            String cat = args.get(0).toLowerCase(Locale.ROOT);
            for (Category c : Category.values()) {
                if (c.name.toLowerCase(Locale.ROOT).startsWith(cat)) {
                    var list = TTClientClient.modules.getByCategory(c);
                    chatOnly(c.name + " (" + list.size() + "): " +
                            list.stream().map(Module::getName).collect(Collectors.joining(", ")));
                    return;
                }
            }
        }
        chatOnly("Modules: " + TTClientClient.modules.getModules().size());
    }

    private static void coords() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        chatOnly(String.format("%.1f %.1f %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));
    }

    private static void friends(List<String> args) {
        if (args.isEmpty()) { chatOnly(".friend add|remove|list <name>"); return; }
        switch (args.get(0).toLowerCase()) {
            case "add", "a" -> {
                if (args.size() < 2) { chatOnly(".friend add <name>"); return; }
                FriendsManager.add(args.get(1));
                chatOnly("Friend + " + args.get(1));
            }
            case "remove", "rm", "del", "r" -> {
                if (args.size() < 2) { chatOnly(".friend remove <name>"); return; }
                FriendsManager.remove(args.get(1));
                chatOnly("Friend - " + args.get(1));
            }
            case "list", "l" -> chatOnly("Friends: " + String.join(", ", FriendsManager.list()));
            case "clear", "c" -> { FriendsManager.clear(); chatOnly("Friends cleared"); }
            default -> chatOnly(".friend add|remove|list <name>");
        }
    }

    private static void chestRelay(List<String> args) {
        ChestRelay relay = TTClientClient.modules != null
                ? TTClientClient.modules.getModule(ChestRelay.class) : null;
        if (relay == null) { chatOnly("ChestRelay missing"); return; }
        if (args.isEmpty()) { chatOnly(".chest list|clear"); return; }
        switch (args.get(0).toLowerCase()) {
            case "list", "l" -> chatOnly("Chests: (use ClickGUI / mark via .chest add)");
            case "clear", "c" -> {
                try { relay.clearChests(); chatOnly("Cleared"); }
                catch (Throwable t) { chatOnly("Clear failed"); }
            }
            default -> chatOnly(".chest list|clear");
        }
    }

    private static void nukerCmd(List<String> args) {
        Nuker nuker = TTClientClient.modules != null
                ? TTClientClient.modules.getModule(Nuker.class) : null;
        if (nuker == null) { chatOnly("Nuker missing"); return; }
        if (args.isEmpty()) { chatOnly(".nuker add|remove|list|clear <block>"); return; }
        switch (args.get(0).toLowerCase()) {
            case "add", "a" -> {
                if (args.size() < 2) { chatOnly(".nuker add <block>"); return; }
                chatOnly(nuker.addBlock(args.get(1)) ? "Nuker + " + args.get(1) : "Unknown/duplicate block");
            }
            case "remove", "rm", "del", "r" -> {
                if (args.size() < 2) { chatOnly(".nuker remove <block>"); return; }
                chatOnly(nuker.removeBlock(args.get(1)) ? "Nuker - " + args.get(1) : "Not in list");
            }
            case "clear", "c" -> { nuker.clearBlocks(); chatOnly("Nuker list cleared"); }
            case "list", "l" -> {
                var set = nuker.getSelected();
                if (set.isEmpty()) chatOnly("Nuker list empty (Selective mode needs blocks)");
                else chatOnly("Nuker (" + set.size() + "): " + String.join(", ", set));
            }
            default -> chatOnly(".nuker add|remove|list|clear <block>");
        }
    }

    private static void blockEsp(List<String> args) {
        BlockESP esp = TTClientClient.modules != null
                ? TTClientClient.modules.getModule(BlockESP.class) : null;
        if (esp == null) { chatOnly("BlockESP missing"); return; }
        if (args.isEmpty()) { chatOnly(".b add|remove|list|clear <block>"); return; }
        switch (args.get(0).toLowerCase()) {
            case "add", "a" -> {
                if (args.size() < 2) { chatOnly(".b add <block>"); return; }
                chatOnly(esp.addBlock(args.get(1)) ? "Added " + args.get(1) : "Unknown block");
            }
            case "remove", "rm", "del", "r" -> {
                if (args.size() < 2) { chatOnly(".b remove <block>"); return; }
                chatOnly(esp.removeBlock(args.get(1)) ? "Removed " + args.get(1) : "Not in list");
            }
            case "clear", "c" -> { esp.clearBlocks(); chatOnly("Cleared"); }
            case "list", "l" -> {
                var set = esp.getSelected();
                chatOnly(set.isEmpty() ? "Empty" : String.join(", ", set));
            }
            default -> chatOnly(".b add|remove|list|clear <block>");
        }
    }

    /** Client-only feedback — no server chat. Reflection for 26.2 API drift. */
    public static void chatOnly(String body) {
        if (body == null) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            Component comp = Component.literal("§8[§aTT§8] §f" + body);

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

    public static void chat(String body) {
        chatOnly(body);
    }

    private static List<String> splitArgs(String body) {
        return new ArrayList<>(Arrays.asList(body.trim().split("\\s+")));
    }
}
