package com.ttclient;

/**
 * Build flavor. CI/gradle sets MASTER via -PttMaster=true|false by rewriting this file,
 * or defaults to master for local builds.
 */
public final class BuildInfo {
    /** true = unlocked master (key generator). false = beta (requires license key). */
    public static final boolean MASTER = Boolean.parseBoolean(
            System.getProperty("ttclient.master", "@TT_MASTER@"));

    public static final String EDITION = MASTER ? "master" : "beta";

    private BuildInfo() {}
}
