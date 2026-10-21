package com.ttclient.util;

import com.ttclient.TTClient;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Local friends list (name-based). Persist under config/ttclient/friends.txt */
public final class FriendsManager {
    private static final Path FILE = Path.of("config", "ttclient", "friends.txt");
    private static final LinkedHashSet<String> FRIENDS = new LinkedHashSet<>();
    private static boolean loaded;

    private FriendsManager() {}

    public static synchronized void load() {
        FRIENDS.clear();
        loaded = true;
        try {
            Files.createDirectories(FILE.getParent());
            if (!Files.exists(FILE)) return;
            for (String line : Files.readAllLines(FILE)) {
                String n = normalize(line);
                if (!n.isEmpty()) FRIENDS.add(n);
            }
        } catch (Throwable t) {
            TTClient.LOGGER.warn("Friends load failed: {}", t.toString());
        }
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());
            List<String> lines = new ArrayList<>(FRIENDS);
            Files.write(FILE, lines);
        } catch (Throwable t) {
            TTClient.LOGGER.warn("Friends save failed: {}", t.toString());
        }
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT);
    }

    public static synchronized boolean add(String name) {
        if (!loaded) load();
        String n = normalize(name);
        if (n.isEmpty()) return false;
        boolean ok = FRIENDS.add(n);
        if (ok) save();
        return ok;
    }

    public static synchronized boolean remove(String name) {
        if (!loaded) load();
        String n = normalize(name);
        boolean ok = FRIENDS.remove(n);
        if (ok) save();
        return ok;
    }

    public static synchronized boolean isFriend(String name) {
        if (!loaded) load();
        return FRIENDS.contains(normalize(name));
    }

    public static synchronized List<String> list() {
        if (!loaded) load();
        return new ArrayList<>(FRIENDS);
    }

    public static synchronized void clear() {
        FRIENDS.clear();
        save();
    }
}
