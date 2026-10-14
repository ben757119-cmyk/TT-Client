package com.ttclient.license;

import com.ttclient.BuildInfo;
import com.ttclient.TTClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

/**
 * Time-limited, single-use license keys for the beta build.
 * Master is always unlocked and can generate keys.
 *
 * Offline DRM cannot be made unbreakable — this blocks casual key sharing/reuse.
 */
public final class LicenseManager {
    private static final String SECRET = "TT-Client-2026-License-Hmac-Key-v1";
    private static final Path STORE = Path.of("config", "ttclient", "license.dat");
    private static final Path USED = Path.of("config", "ttclient", "used_keys.dat");

    private static String activeKey;
    private static long expiresAtMs;
    private static boolean loaded;

    private LicenseManager() {}

    public static boolean isMaster() {
        return BuildInfo.MASTER;
    }

    public static boolean isUnlocked() {
        if (isMaster()) return true;
        ensureLoaded();
        return expiresAtMs > System.currentTimeMillis();
    }

    public static long millisRemaining() {
        if (isMaster()) return Long.MAX_VALUE;
        ensureLoaded();
        return Math.max(0, expiresAtMs - System.currentTimeMillis());
    }

    public static String statusText() {
        if (isMaster()) return "MASTER (unlocked)";
        ensureLoaded();
        if (expiresAtMs <= 0) return "BETA — no key (type: TTKEY <key>)";
        long left = expiresAtMs - System.currentTimeMillis();
        if (left <= 0) return "BETA — key expired";
        long mins = left / 60000;
        if (mins < 60) return "BETA — " + mins + "m left";
        long hours = mins / 60;
        if (hours < 48) return "BETA — " + hours + "h left";
        return "BETA — " + (hours / 24) + "d left";
    }

    public static String generateKey(long durationMs) {
        if (!isMaster()) return null;
        long exp = System.currentTimeMillis() + Math.max(60_000L, durationMs);
        String nonce = randomNonce(10);
        String payload = exp + ":" + nonce;
        String sig = hmac(payload);
        String raw = payload + ":" + sig;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static String redeem(String key) {
        if (isMaster()) return "Master build does not need keys";
        if (key == null || key.isBlank()) return "Empty key";
        key = key.trim();

        String decoded;
        try {
            decoded = new String(Base64.getUrlDecoder().decode(key), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "Invalid key format";
        }

        String[] parts = decoded.split(":");
        if (parts.length != 3) return "Invalid key format";

        long exp;
        try {
            exp = Long.parseLong(parts[0]);
        } catch (Exception e) {
            return "Invalid expiry";
        }
        String nonce = parts[1];
        String sig = parts[2];
        if (!hmac(exp + ":" + nonce).equals(sig)) return "Invalid key signature";
        if (exp <= System.currentTimeMillis()) return "Key already expired";

        Set<String> used = loadUsed();
        if (used.contains(nonce)) return "Key already used";

        used.add(nonce);
        saveUsed(used);

        activeKey = key;
        expiresAtMs = exp;
        saveActive();
        TTClient.LOGGER.info("License redeemed until {}", Instant.ofEpochMilli(exp));
        return null;
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            if (Files.exists(STORE)) {
                String line = Files.readString(STORE).trim();
                String[] p = line.split("\\|", 2);
                if (p.length == 2) {
                    activeKey = p[0];
                    expiresAtMs = Long.parseLong(p[1]);
                }
            }
        } catch (Exception e) {
            activeKey = null;
            expiresAtMs = 0;
        }
    }

    private static void saveActive() {
        try {
            Files.createDirectories(STORE.getParent());
            Files.writeString(STORE, activeKey + "|" + expiresAtMs);
        } catch (Exception e) {
            TTClient.LOGGER.error("Failed to save license", e);
        }
    }

    private static Set<String> loadUsed() {
        Set<String> set = new HashSet<>();
        try {
            if (Files.exists(USED)) {
                for (String line : Files.readAllLines(USED)) {
                    if (!line.isBlank()) set.add(line.trim());
                }
            }
        } catch (Exception ignored) {}
        return set;
    }

    private static void saveUsed(Set<String> used) {
        try {
            Files.createDirectories(USED.getParent());
            Files.write(USED, used);
        } catch (Exception e) {
            TTClient.LOGGER.error("Failed to save used keys", e);
        }
    }

    private static String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) sb.append(String.format("%02x", out[i]));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String randomNonce(int len) {
        SecureRandom r = new SecureRandom();
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }
}
