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

        switch (cmd) {
            case "help", "h", "?" -> help();
            case "v", "version" -> chat("TT Client " + TTClient.VERSION);
            case "t", "toggle" -> toggle(args);
            case "modules", "mods", "list" -> listModules(args);
            case "b", "block", "blockesp", "be" -> blockEsp(args);
            case "chest", "chests", "cr", "chestrelay" -> chestRelay(args);
            case "bind" -> bind(args);
            case "drawn", "hide" -> chat("Use ClickGUI to manage module visibility");
            case "say" -> chat("Drop the . to talk on the server");
            case "friends", "friend" -> chat("Friends list not enabled in this build");
            case "ping" -> chat("pong");
            case "coords", "pos" -> coords();
            case "clearnotifs", "cn" -> chat("Notifications clear on their own");
            default -> chat("Unknown: ." + cmd + "  ·  .help");
        }
        return true;
    }

    /** Tab-complete suggestions for current chat buffer (without leading newline). */
    public static String tabComplete(String buffer) {
        if (buffer == null || !buffer.startsWith(PREFIX)) return null;
        String body = buffer.substring(PREFIX.length());
        // complete command name
        if (!body.contains(" ")) {
            String partial = body.toLowerCase(Locale.ROOT);
            List<String> cmds = List.of(
                    "help", "version", "toggle", "modules", "block", "chest", "bind", "coords", "ping"
            );
            List<String> hits = new ArrayList<>();
            for (String c : cmds) if (c.startsWith(partial)) hits.add(PREFIX + c);
            if (TTClientClient.modules != null && (partial.isEmpty() || "toggle".startsWith(partial))) {
                // also module names after .toggle space handled below
            }
            if (hits.isEmpty()) return null;
            if (hits.size() == 1) return hits.get(0) + (hits.get(0).equals(PREFIX + "toggle") ? " " : "");
            chat("§7" + String.join(" §8|§7 ", hits).replace('§', '§'));
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
            chat("§7" + String.join(" §8|§7 ", hits.stream().limit(8).toList()).replace('§', '§'));
            return commonPrefix(hits);
        }
        if (cmd.equals("chest") || cmd.equals("cr")) {
            String partial = parts.size() > 1 ? parts.get(1).toLowerCase(Locale.ROOT) : "";
            List<String> subs = List.of("add", "list", "clear", "remove");
            List<String> hits = new ArrayList<>();
            for (String s : subs) if (s.startsWith(partial)) hits.add(PREFIX + "chest " + s);
            if (hits.size() == 1) return hits.get(0);
            if (!hits.isEmpty()) return commonPrefix(hits);
        }
        if (cmd.equals("b") || cmd.equals("block")) {
            String partial = parts.size() > 1 ? parts.get(1).toLowerCase(Locale.ROOT) : "";
            List<String> subs = List.of("add", "remove", "list", "clear");
            List<String> hits = new ArrayList<>();
            for (String s : subs) if (s.startsWith(partial)) hits.add(PREFIX + "b " + s);
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
        chat("§a§lTT Commands §8· §7prefix .");
        chat("§f.help §8· §f.version §8· §f.ping §8· §f.coords");
        chat("§f.toggle <module> §8· §f.modules [filter]");
        chat("§f.b add|remove|list|clear <block>");
        chat("§f.chest add|list|clear|remove <#>  §8(look at chest)");
        chat("§7Tab autocompletes. Messages stay client-side.");
    }

    private static void coords() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            chat(String.format("XYZ %.1f %.1f %.1f",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        } catch (Throwable ignored) {}
    }

    private static void bind(List<String> args) {
        chat("Right-click a module in ClickGUI to set its bind");
    }

    private static void chestRelay(List<String> args) {
        ChestRelay relay = TTClientClient.modules != null
                ? TTClientClient.modules.getModule(ChestRelay.class) : null;
        if (relay == null) { chat("ChestRelay missing"); return; }
        if (args.isEmpty()) {
            chat(".chest add | list | clear | remove <index>");
            chat("Chain: loot #1 → fill #2 → loot #2 → fill #3 …");
            return;
        }
        String sub = args.get(0).toLowerCase(Locale.ROOT);
        switch (sub) {
            case "add", "a", "mark" -> {
                BlockPos pos = targetedBlock();
                if (pos == null) { chat("Look at a chest first"); return; }
                boolean ok = relay.addChest(pos);
                chat(ok ? "Added #" + relay.getChests().size() + " " + pos.toShortString()
                        : "Already listed " + pos.toShortString());
                if (ok) NotificationManager.push("ChestRelay", "#" + relay.getChests().size() + " added");
            }
            case "list", "l" -> {
                List<BlockPos> list = relay.getChests();
                if (list.isEmpty()) { chat("(empty)"); return; }
                for (int i = 0; i < list.size(); i++) {
                    BlockPos p = list.get(i);
                    boolean full = relay.getFullMarked().contains(p);
                    chat("#" + (i + 1) + " " + p.getX() + " " + p.getY() + " " + p.getZ()
                            + (full ? " FULL" : ""));
                }
            }
            case "clear", "c" -> {
                relay.clearChests();
                chat("Cleared");
            }
            case "remove", "rm", "del", "r" -> {
                if (args.size() < 2) { chat(".chest remove <index>"); return; }
                try {
                    int idx = Integer.parseInt(args.get(1)) - 1;
                    chat(relay.removeIndex(idx) ? "Removed #" + (idx + 1) : "Bad index");
                } catch (NumberFormatException e) {
                    chat("Index must be a number");
                }
            }
            default -> chat(".chest add|list|clear|remove <index>");
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
        if (args.isEmpty()) { chat("Usage: .toggle <module>"); return; }
        if (TTClientClient.modules == null) return;
        String name = String.join(" ", args);
        Module mod = TTClientClient.modules.getModule(name);
        if (mod == null) {
            String q = name.toLowerCase(Locale.ROOT);
            for (Module m : TTClientClient.modules.getModules()) {
                if (m.getName().toLowerCase(Locale.ROOT).contains(q)) { mod = m; break; }
            }
        }
        if (mod == null) { chat("Not found: " + name); return; }
        mod.toggle();
        if (TTClientClient.config != null) TTClientClient.config.save();
        // client-only chat line
        chat(mod.getName() + (mod.isEnabled() ? " §aenabled" : " §cdisabled"));
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
            if (sb.length() > 160) { chat(sb.toString()); sb.setLength(0); }
        }
        if (sb.length() > 0) chat(sb.toString());
        chat(n + " modules");
    }

    private static void blockEsp(List<String> args) {
        BlockESP esp = TTClientClient.modules != null
                ? TTClientClient.modules.getModule(BlockESP.class) : null;
        if (esp == null) { chat("BlockESP missing"); return; }
        if (args.isEmpty()) { chat(".b add|remove|list|clear <block>"); return; }
        String sub = args.get(0).toLowerCase(Locale.ROOT);
        switch (sub) {
            case "add", "a" -> {
                if (args.size() < 2) { chat(".b add <block>"); return; }
                chat(esp.addBlock(args.get(1)) ? "Added " + args.get(1) : "Unknown block");
            }
            case "remove", "rm", "del", "r" -> {
                if (args.size() < 2) { chat(".b remove <block>"); return; }
                chat(esp.removeBlock(args.get(1)) ? "Removed " + args.get(1) : "Not in list");
            }
            case "clear", "c" -> { esp.clearBlocks(); chat("Cleared"); }
            case "list", "l" -> {
                String list = String.join(", ", esp.getSelected());
                chat(list.isEmpty() ? "(empty)" : list);
            }
            default -> chat(".b add|remove|list|clear <block>");
        }
    }

    /** Client-only chat line (only you see it). */
    public static void chat(String body) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.gui == null) return;
            Component comp = Component.literal("[TT] " + body.replace('§', '§'));
            try {
                Object chat = mc.gui.getClass().getMethod("getChat").invoke(mc.gui);
                chat.getClass().getMethod("addMessage", Component.class).invoke(chat, comp);
            } catch (Throwable t) {
                try {
                    mc.gui.getClass().getMethod("setOverlayMessage", Component.class, boolean.class)
                            .invoke(mc.gui, comp, false);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        try { NotificationManager.push("TT", body.replaceAll("§.", "")); } catch (Throwable ignored) {}
    }

    private static List<String> splitArgs(String body) {
        return new ArrayList<>(Arrays.asList(body.trim().split("\\s+")));
    }
}
