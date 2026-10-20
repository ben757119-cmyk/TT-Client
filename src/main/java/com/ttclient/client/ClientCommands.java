package com.ttclient.client;

import com.ttclient.TTClient;
import com.ttclient.modules.Module;
import com.ttclient.modules.player.ChestRelay;
import com.ttclient.modules.render.BlockESP;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Meteor-style client commands. Prefix: {@code .}
 * Anything starting with {@code .} is client-only and never sent to the server.
 */
public final class ClientCommands {
    public static final String PREFIX = ".";

    private ClientCommands() {}

    /** @return true if the message was a client command (caller must cancel chat send). */
    public static boolean handle(String raw) {
        if (raw == null) return false;
        String msg = raw.trim();
        if (!msg.startsWith(PREFIX)) return false;

        String body = msg.substring(PREFIX.length()).trim();
        if (body.isEmpty()) {
            reply("TT", "Commands: .help");
            return true;
        }

        List<String> parts = splitArgs(body);
        if (parts.isEmpty()) {
            reply("TT", "Commands: .help");
            return true;
        }

        String cmd = parts.get(0).toLowerCase(Locale.ROOT);
        List<String> args = parts.size() > 1 ? parts.subList(1, parts.size()) : List.of();

        switch (cmd) {
            case "help", "h", "?" -> help();
            case "v", "version" -> reply("TT", "TT Client " + TTClient.VERSION);
            case "t", "toggle" -> toggle(args);
            case "modules", "mods" -> listModules(args);
            case "b", "block", "blockesp", "be" -> blockEsp(args);
            case "chest", "chests", "cr", "chestrelay" -> chestRelay(args);
            case "say" -> reply("TT", "Use normal chat (no .) to talk on the server");
            default -> reply("TT", "Unknown command: ." + cmd + "  (.help)");
        }
        return true;
    }

    private static void help() {
        reply("TT", ".help  .version  .toggle <module>  .modules [filter]");
        reply("TT", ".b add|remove|list|clear <block>  (BlockESP)");
        reply("TT", ".chest add|list|clear|remove <#>  (ChestRelay — look at chest for add)");
        reply("TT", "Anything starting with . is client-only and is not sent to the server.");
    }

    private static void chestRelay(List<String> args) {
        ChestRelay relay = TTClientClient.modules != null
                ? TTClientClient.modules.getModule(ChestRelay.class) : null;
        if (relay == null) {
            reply("ChestRelay", "Module missing");
            return;
        }
        if (args.isEmpty()) {
            reply("ChestRelay", ".chest add | list | clear | remove <index>");
            reply("ChestRelay", "Mark chests in order, enable ChestRelay, it does #1->#2 then #2->#3...");
            return;
        }
        String sub = args.get(0).toLowerCase(Locale.ROOT);
        switch (sub) {
            case "add", "a", "mark" -> {
                BlockPos pos = targetedBlock();
                if (pos == null) {
                    reply("ChestRelay", "Look at a chest/barrel/shulker first");
                    return;
                }
                Minecraft mc = Minecraft.getInstance();
                if (!ChestRelay.looksLikeChest(mc, pos)) {
                    reply("ChestRelay", "That block does not look like storage — adding anyway");
                }
                boolean ok = relay.addChest(pos);
                if (ok) {
                    reply("ChestRelay", "#" + relay.getChests().size() + " added " + pos.toShortString());
                    if (TTClientClient.config != null) TTClientClient.config.save();
                } else {
                    reply("ChestRelay", "Already in list: " + pos.toShortString());
                }
            }
            case "list", "l" -> {
                List<BlockPos> list = relay.getChests();
                if (list.isEmpty()) {
                    reply("ChestRelay", "(empty) — look at chests and .chest add");
                    return;
                }
                for (int i = 0; i < list.size(); i++) {
                    BlockPos p = list.get(i);
                    reply("ChestRelay", "#" + (i + 1) + " " + p.getX() + " " + p.getY() + " " + p.getZ());
                }
            }
            case "clear", "c" -> {
                relay.clearChests();
                reply("ChestRelay", "Cleared");
                if (TTClientClient.config != null) TTClientClient.config.save();
            }
            case "remove", "rm", "del", "r" -> {
                if (args.size() < 2) {
                    reply("ChestRelay", ".chest remove <index starting at 1>");
                    return;
                }
                try {
                    int idx = Integer.parseInt(args.get(1)) - 1;
                    boolean ok = relay.removeIndex(idx);
                    reply("ChestRelay", ok ? "Removed #" + (idx + 1) : "Bad index");
                    if (ok && TTClientClient.config != null) TTClientClient.config.save();
                } catch (NumberFormatException e) {
                    reply("ChestRelay", "Index must be a number");
                }
            }
            default -> reply("ChestRelay", ".chest add|list|clear|remove <index>");
        }
    }

    private static BlockPos targetedBlock() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.hitResult == null) return null;
            HitResult hr = mc.hitResult;
            if (hr.getType() == HitResult.Type.BLOCK && hr instanceof BlockHitResult bhr) {
                return bhr.getBlockPos();
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static void toggle(List<String> args) {
        if (args.isEmpty()) {
            reply("TT", "Usage: .toggle <module>");
            return;
        }
        if (TTClientClient.modules == null) return;
        String name = String.join(" ", args);
        Module mod = TTClientClient.modules.getModule(name);
        if (mod == null) {
            String q = name.toLowerCase(Locale.ROOT);
            for (Module m : TTClientClient.modules.getModules()) {
                if (m.getName().toLowerCase(Locale.ROOT).contains(q)) {
                    mod = m;
                    break;
                }
            }
        }
        if (mod == null) {
            reply("TT", "Module not found: " + name);
            return;
        }
        mod.toggle();
        if (TTClientClient.config != null) TTClientClient.config.save();
        reply("TT", mod.getName() + (mod.isEnabled() ? " enabled" : " disabled"));
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
            if (sb.length() > 180) {
                reply("TT", sb.toString());
                sb.setLength(0);
            }
        }
        if (sb.length() > 0) reply("TT", sb.toString());
        reply("TT", n + " modules" + (filter.isEmpty() ? "" : " matching " + filter));
    }

    private static void blockEsp(List<String> args) {
        BlockESP esp = TTClientClient.modules != null
                ? TTClientClient.modules.getModule(BlockESP.class) : null;
        if (esp == null) {
            reply("BlockESP", "Module missing");
            return;
        }
        if (args.isEmpty()) {
            reply("BlockESP", ".b add|remove|list|clear <block>");
            return;
        }
        String sub = args.get(0).toLowerCase(Locale.ROOT);
        switch (sub) {
            case "add", "a" -> {
                if (args.size() < 2) {
                    reply("BlockESP", ".b add <block>");
                    return;
                }
                boolean ok = esp.addBlock(args.get(1));
                reply("BlockESP", ok ? "Added " + args.get(1) : "Unknown block");
                if (ok && TTClientClient.config != null) TTClientClient.config.save();
            }
            case "remove", "rm", "del", "r" -> {
                if (args.size() < 2) {
                    reply("BlockESP", ".b remove <block>");
                    return;
                }
                boolean ok = esp.removeBlock(args.get(1));
                reply("BlockESP", ok ? "Removed " + args.get(1) : "Not in list");
                if (ok && TTClientClient.config != null) TTClientClient.config.save();
            }
            case "clear", "c" -> {
                esp.clearBlocks();
                reply("BlockESP", "Cleared");
                if (TTClientClient.config != null) TTClientClient.config.save();
            }
            case "list", "l" -> {
                String list = String.join(", ", esp.getSelected());
                reply("BlockESP", list.isEmpty() ? "(empty)" : list);
            }
            default -> reply("BlockESP", ".b add|remove|list|clear <block>");
        }
    }

    private static void reply(String title, String body) {
        try {
            NotificationManager.push(title, body);
        } catch (Throwable ignored) {}
        try {
            TTClient.LOGGER.info("[{}] {}", title, body);
        } catch (Throwable ignored) {}
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc != null && mc.gui != null) {
                var comp = net.minecraft.network.chat.Component.literal(
                        "[" + title + "] " + body.replace('§', ' '));
                try {
                    mc.gui.getClass().getMethod("setOverlayMessage", net.minecraft.network.chat.Component.class, boolean.class)
                            .invoke(mc.gui, comp, false);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    private static List<String> splitArgs(String body) {
        return new ArrayList<>(Arrays.asList(body.trim().split("\\s+")));
    }
}
