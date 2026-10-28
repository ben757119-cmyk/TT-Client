package com.ttclient.client;

import com.ttclient.TTClient;
import com.ttclient.modules.Module;
import com.ttclient.modules.player.ChestRelay;
import com.ttclient.modules.render.BlockESP;
import com.ttclient.modules.world.Nuker;
import com.ttclient.util.FriendsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
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

        switch (cmd) {
            case "help", "h", "?" -> help();
            case "v", "version" -> chat("TT Client " + TTClient.VERSION);
            case "t", "toggle" -> toggle(args);
            case "modules", "mods", "list" -> listModules(args);
            case "b", "block", "blockesp", "be" -> blockEsp(args);
            case "nuker", "n", "nuke" -> nukerCmd(args);
            case "chest", "chests", "cr", "chestrelay" -> chestRelay(args);
            case "friend", "friends", "f" -> friends(args);
            case "bind", "keybind" -> ModuleBindCommands.handle(args.toArray(new String[0]));
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
                    "help", "version", "toggle", "modules", "block", "nuker", "chest", "friend", "say", "coords", "ping", "bind"
            );
            List<String> hits = new ArrayList<>();
            for (String c : cmds) if (c.startsWith(partial)) hits.add(PREFIX + c);
            if (hits.isEmpty()) return null;
            if (hits.size() == 1) {
                String h = hits.get(0);
                if (h.equals(PREFIX + "toggle") || h.equals(PREFIX + "say") || h.equals(PREFIX + "chest")
                        || h.equals(PREFIX + "block") || h.equals(PREFIX + "friend") || h.equals(PREFIX + "nuker")
                        || h.equals(PREFIX + "bind"))
                    return h + " ";
                return h;
            }
            chatOnly("§7" + String.join(" §8| §7", hits));
            return commonPrefix(hits);
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
        chatOnly("§f.help §8· §f.version §8· §f.ping §8· §f.coords §8· §f.bind");
        chatOnly("§f.toggle <module> §8· §f.modules [filter]");
        chatOnly("§f.bind <module> <key|none|mouse4|mouse5> §8· §f.bind list");
        chatOnly("§f.friend add|remove|list|clear <name>");
        chatOnly("§f.say <message> §8· talk on the server");
        chatOnly("§f.b add|remove|list|clear <block>");
        chatOnly("§f.nuker add|remove|list|clear <block>");
        chatOnly("§f.chest add|list|clear|remove <#>  §8(look at chest)");
        chatOnly("§7Tab autocompletes. Press . to open chat with prefix.");
    }

    private static void friends(List<String> args) {
        if (args.isEmpty()) {
            chatOnly(".friend add|remove|list|clear <name>");
            return;
        }
        switch (args.get(0).toLowerCase(Locale.ROOT)) {
            case "add", "a" -> {
                if (args.size() < 2) { chatOnly(".friend add <name>"); return; }
                String name = args.get(1);
                chatOnly(FriendsManager.add(name) ? "Added friend §a" + name : "Already friends with " + name);
            }
            case "remove", "rm", "del", "r" -> {
                if (args.size() < 2) { chatOnly(".friend remove <name>"); return; }
                String name = args.get(1);
                chatOnly(FriendsManager.remove(name) ? "Removed §c" + name : "Not in friends: " + name);
            }
            case "list", "l" -> {
                List<String> list = FriendsManager.list();
                if (list.isEmpty()) { chatOnly("(no friends)"); return; }
                chatOnly("Friends (" + list.size() + "):");
                for (String f : list) chatOnly("  §a" + f);
            }
            case "clear", "c" -> {
                FriendsManager.clear();
                chatOnly("Friends cleared");
            }
            default -> chatOnly(".friend add|remove|list|clear <name>");
        }
    }

    private static void coords() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        chatOnly(String.format("XYZ %.1f %.1f %.1f",
                mc.player.getX(), mc.player.getY(), mc.player.getZ()));
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
        if (args.isEmpty()) { chatOnly("Usage: .t <module>"); return; }
        if (TTClientClient.modules == null) return;
        String name = args.get(0);
        Module mod = TTClientClient.modules.getModule(name);
        if (mod == null) {
            for (Module m : TTClientClient.modules.getModules()) {
                if (m.getName().toLowerCase(Locale.ROOT).startsWith(name.toLowerCase(Locale.ROOT))) {
                    mod = m; break;
                }
            }
        }
        if (mod == null) { chatOnly("No module: " + name); return; }
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

    public static void chatOnly(String body) {
        if (body == null) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            Component comp = Component.literal("§8[§aTT§8] §f" + body);
            if (mc.player != null) {
                try {
                    mc.player.displayClientMessage(comp, false);
                    return;
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    private static void chat(String body) { chatOnly(body); }

    private static List<String> splitArgs(String body) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quote = false;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '"') { quote = !quote; continue; }
            if (c == ' ' && !quote) {
                if (cur.length() > 0) { out.add(cur.toString()); cur.setLength(0); }
                continue;
            }
            cur.append(c);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }
}
