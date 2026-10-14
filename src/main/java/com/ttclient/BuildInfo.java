package com.ttclient;

import java.io.InputStream;
import java.util.Properties;

/**
 * Edition flag loaded from /ttclient-edition.properties (written by Gradle).
 * master = full unlock + KeyGen. beta = requires license key.
 */
public final class BuildInfo {
    public static final boolean MASTER;
    public static final String EDITION;

    static {
        boolean master = true;
        try (InputStream in = BuildInfo.class.getResourceAsStream("/ttclient-edition.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                String ed = p.getProperty("edition", "master").trim();
                master = !"beta".equalsIgnoreCase(ed);
            }
        } catch (Exception ignored) {}
        // Runtime override for testing
        String prop = System.getProperty("ttclient.edition");
        if (prop != null) {
            master = !"beta".equalsIgnoreCase(prop.trim());
        }
        String env = System.getenv("TTCLIENT_EDITION");
        if (env != null) {
            master = !"beta".equalsIgnoreCase(env.trim());
        }
        MASTER = master;
        EDITION = master ? "master" : "beta";
    }

    private BuildInfo() {}
}
