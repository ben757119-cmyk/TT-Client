package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;

public class DiscordRPC extends Module {
    public final BoolSetting showServer = addSetting(new BoolSetting("Show Server", "Show current server", true));
    public final BoolSetting showModule = addSetting(new BoolSetting("Show Module", "Show enabled module count", true));

    public DiscordRPC() {
        super("DiscordRPC", "Discord Rich Presence", Category.MISC);
    }
}
