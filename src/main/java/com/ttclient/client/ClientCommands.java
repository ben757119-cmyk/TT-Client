package com.ttclient.client;

import com.ttclient.TTClient;
import com.ttclient.modules.Module;
import com.ttclient.modules.player.ChestRelay;
import com.ttclient.modules.render.BlockESP;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ClientCommands {
    public static final String PREFIX = ".";

    private ClientCommands() {}

    public static boolean handle(String raw) {
        if (raw == null) return false;
        String msg = raw.trim();
        if (!msg.startsWith(PREFIX)) return false;

        String body = msg.substring(PREFIX.length()).trim();
        if (body.isEmpty()) {
            help();
            return true;
        }

        List<String> parts = splitArgs(body);
        if (parts.isEmpty()) {
            help();
            return true;
        }

        String cmd = parts.get(0).toLowerCase(Locale.ROOT);
        List<String> args = parts.size() > 1 ? parts.subList(1, parts.size()) : List.of();

        if (cmd.equals("say")) return false;

        switch (cmd) {
            case "help", "h", "?" -> help();
            case "v", "version" -> chat("TT Client " + TTClient.VERSION);
            case "t", "toggle" -> toggle(args);
            case "modules", "mods", "list" -> listModules(args);
            case "b", "block", "blockesp", "be" -> blockEsp(args);
            case "chest", "chests", "cr", "chestrelay" -> chestRelay(args);
            case "bind" -> chat("Right-click a module in ClickGUI to set its bind");
            case "ping" -> chat("pong");
            case "coords", "pos" -> coords();
            default -> chat("Unknown: ." + cmd + "  ·  .help");
        }
        return true;
    }

    public static String sayPayload(String raw) {
        if (raw == null) return null;
        String msg = raw.trim();
        if (!msg.startsWith(PREFIX)) return null;
        String body = msg.substring(PREFIX.length()).trim();
        if (!body.toLowerCase(Locale.ROOT).startsWith("say")) return null;
        if (body.length() <= 3) return "";
        return body.substring(3).trim();
    }

    public static String tabComplete(String buffer) {
        if (buffer == null || !buffer.startsWith(PREFIX)) return null;
        String body = buffer.substring(PREFIX.length());
        if (!body.contains(" ")) {
            String partial = body.toLowerCase(Locale.ROOT);
            List<String> cmds = List.of(
                    "help", "version", "toggle", "modules", "block", "chest", "say", "coords", "ping", "bind"
            );
            List<String> hits = new ArrayList<>();
            for (String c : cmds) if (c.startsWith(partial)) hits.add(PREFIX + c);
            if (hits.isEmpty()) return null;
            if (hits.size() == 1) {
                String h = hits.get(0);
                if (h.equals(PREFIX + "toggle") || h.equals(PREFIX + "say") || h.equals(PREFIX + "chest") || h.equals(PREFIX + "block"))
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
        chatOnly("§a§lTT Commands §8· §7prefix .");
        chatOnly("§f.help §8· §f.version §8· §f.ping §8· §f.coords");
        chatOnly("§f.toggle <module> §8· §f.modules [filter]");
        chatOnly("§f.say <message> §8· talk on the server");
        chatOnly("§f.b add|remove|list|clear <block>");
        chatOnly("§f.chest add|list|clear|remove <#>  §8(look at chest)");
        chatOnly("§7Tab autocompletes. Press . to open chat with prefix.");
    }

    private static void coords() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            chatOnly(String.format("XYZ %.1f %.1f %.1f",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        } catch (Throwable ignored) {}
    }

    private static void chestRelay(List<String> args) {
        ChestRelay relay = TTClientClient.modules != null
                ? TTClientClient.modules.getModule(ChestRelay.class) : null;
        if (relay == null) { chatOnly("ChestRelay missing"); return; }
        if (args.isEmpty()) {
            chatOnly(".chest add | list | clear | remove <index>");
            return;
        }
        String sub = args.get(0).toLowerCase(Locale.ROOT);
        switch (sub) {
            case "add", "a", "mark" -> {
                BlockPos pos = targetedBlock();
                if (pos == null) { chatOnly("Look at a chest first"); return; }
                boolean ok = relay.addChest(pos);
                chatOnly(ok ? "Added #" + relay.getChests().size() + " " + pos.toShortString()
                        : "Already listed " + pos.toShortString());
            }
            case "list", "l" -> {
                List<BlockPos> list = relay.getChests();
                if (list.isEmpty()) { chatOnly("(empty)"); return; }
                for (int i = 0; i < list.size(); i++) {
                    BlockPos p = list.get(i);
                    boolean full = relay.getFullMarked().contains(p);
                    chatOnly("#" + (i + 1) + " " + p.getX() + " " + p.getY() + " " + p.getZ()
                            + (full ? " FULL" : ""));
                }
            }
            case "clear", "c" -> { relay.clearChests(); chatOnly("Cleared"); }
            case "remove", "rm", "del", "r" -> {
                if (args.size() < 2) { chatOnly(".chest remove <index>"); return; }
                try {
                    int idx = Integer.parseInt(args.get(1)) - 1;
                    chatOnly(relay.removeIndex(idx) ? "Removed #" + (idx + 1) : "Bad index");
                } catch (NumberFormatException e) {
                    chatOnly("Index must be a number");
                }
            }
            default -> chatOnly(".chest add|list|clear|remove <index>");
        }
    }

    private static BlockPos targetedBlock() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.hitResult == null) return null;
            HitResult hr = mc.hitResult;
            if (hr.getType() == HitResult.Type.BLOCK && hr instanceof BlockHitResult bhr)
                return bhr.getBlockPos();
        } catch (Throwable ignored) {}
        return null;
    }

    private static void toggle(List<String> args) {
        if (args.isEmpty()) { chatOnly("Usage: .toggle <module>"); return; }
        if (TTClientClient.modules == null) return;
        String name = String.join(" ", args);
        Module mod = TTClientClient.modules.getModule(name);
        if (mod == null) {
            String q = name.toLowerCase(Locale.ROOT);
            for (Module m : TTClientClient.modules.getModules()) {
                if (m.getName().toLowerCase(Locale.ROOT).contains(q)) { mod = m; break; }
            }
        }
        if (mod == null) { chatOnly("Not found: " + name); return; }
        mod.toggle();
        if (TTClientClient.config != null) TTClientClient.config.save();
        chatOnly(mod.getName() + (mod.isEnabled() ? " §aenabled" : " §cdisabled"));
    }

    private static void listModules(List<String> args) {
        if (TTClientClient.modules == null) return;
        String filter = args.isEmpty() ? "" : args.get(0).toLowerCase(Locale.ROOT);
        int n = 0;
        StringBuilder sb = new StringBuilder();
        for (Module m : TTClientClient.modules.getModules()) {
            if (!filter.isEmpty() && !m.getName().toLowerCase(Locale.ROOT).contains(filter)) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(m.isEnabled() ? "+" : "-").append(m.getName());
            n++;
            if (sb.length() > 160) { chatOnly(sb.toString()); sb.setLength(0); }
        }
        if (sb.length() > 0) chatOnly(sb.toString());
        chatOnly(n + " modules");
    }

    private static void blockEsp(List<String> args) {
        BlockESP esp = TTClientClient.modules != null
                ? TTClientClient.modules.getModule(BlockESP.class) : null;
        if (esp == null) { chatOnly("BlockESP missing"); return; }
        if (args.isEmpty()) { chatOnly(".b add|remove|list|clear <block>"); return; }
        String sub = args.get(0).toLowerCase(Locale.ROOT);
        switch (sub) {
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
                String list = String.join(", ", esp.getSelected());
                chatOnly(list.isEmpty() ? "(empty)" : list);
            }
            default -> chatOnly(".b add|remove|list|clear <block>");
        }
    }

    /**
     * Client-only chat feedback for NeoForge 26.2.
     * No direct displayClientMessage — method is not on LocalPlayer in this mapping.
     * Uses reflection against player + Gui chat component only.
     */
    public static void chatOnly(String body) {
        if (body == null) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            Component comp = Component.literal("[TT] " + stripCodesForPlain(body));
            // Prefer styled component when body already has section signs
            try {
                if (body.indexOf('§') >= 0) {
                    comp = Component.literal("§8[§aTT§8] §f" + body);
                }
            } catch (Throwable ignored) {}

            // 1) Player methods via reflection (sendSystemMessage / displayClientMessage / etc.)
            if (mc.player != null) {
                Object player = mc.player;
                Class<?> c = player.getClass();
                while (c != null && c != Object.class) {
                    for (var m : c.getDeclaredMethods()) {
                        String name = m.getName();
                        if (!(name.equals("displayClientMessage")
                                || name.equals("sendSystemMessage")
                                || name.equals("sendMessage")
                                || name.equals("displayClientMessage"))) continue;
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

            // 2) Gui chat component
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

    private static String stripCodesForPlain(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '§' && i + 1 < s.length()) { i++; continue; }
            out.append(ch);
        }
        return out.toString();
    }

    public static void chat(String body) {
        chatOnly(body);
    }

    private static List<String> splitArgs(String body) {
        return new ArrayList<>(Arrays.asList(body.trim().split("\\s+")));
    }
}
