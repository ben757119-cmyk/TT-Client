package com.ttclient.modules.misc;
import com.ttclient.modules.Category; import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
public class DiscordRPC extends Module {
    public final BoolSetting showServer = addSetting(new BoolSetting("Show Server", "Show server", true));
    public DiscordRPC() { super("DiscordRPC", "Discord presence", Category.MISC); }
}
