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

public final class ClientCommands {
    private ClientCommands() {}

    public static boolean handle(String raw) {
        if (raw == null) return false;
        String msg = raw.trim();
        if (!msg.startsWith(".")) return false;
        String body = msg.substring(1).trim();
        if (body.isEmpty()) return true;
        String[] parts = body.split("\\s+");
        String cmd = parts[0].toLowerCase(Locale.ROOT);
        List<String> args = new ArrayList<>(Arrays.asList(parts).subList(1, parts.length));
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
                if (args.isEmpty()) { chatOnly("Usage: .say <message>"); return true; }
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.player.connection != null)
                    mc.player.connection.sendChat(String.join(" ", args));
            }
            default -> chatOnly("Unknown command. .help for list");
            }
        } catch (Throwable t) {
            chatOnly("Command error: " + t.getClass().getSimpleName());
        }
        return true;
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
            case "list", "l" -> chatOnly("Friends: " + String.join(", ", FriendsManager.all()));
            default -> chatOnly(".friend add|remove|list <name>");
        }
    }

    private static void chestRelay(List<String> args) {
        ChestRelay relay = TTClientClient.modules != null
                ? TTClientClient.modules.getModule(ChestRelay.class) : null;
        if (relay == null) { chatOnly("ChestRelay missing"); return; }
        if (args.isEmpty()) { chatOnly(".chest list|clear|..."); return; }
        switch (args.get(0).toLowerCase()) {
            case "list", "l" -> chatOnly("Chests: " + relay.describeChests());
            case "clear", "c" -> { relay.clearChests(); chatOnly("Cleared"); }
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

    private static void chat(String s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(Component.literal("§7[TT] §f" + s), false);
    }

    private static void chatOnly(String s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(Component.literal("§7[TT] §f" + s), false);
    }
}
