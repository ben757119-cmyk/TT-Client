package com.ttclient.modules;

public enum Category {
    COMBAT("Combat", 0xFF5555),
    MOVEMENT("Movement", 0x55FF55),
    PLAYER("Player", 0x5555FF),
    RENDER("Render", 0xFF55FF),
    WORLD("World", 0x55FFFF),
    MISC("Misc", 0xFFFF55),
    CLIENT("Client", 0xAAAAAA);

    public final String name;
    public final int color;

    Category(String name, int color) {
        this.name = name;
        this.color = color;
    }
}
