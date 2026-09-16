package com.ttclient.modules.misc;
import com.ttclient.modules.Category; import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
public class Announcer extends Module {
    public final BoolSetting kills = addSetting(new BoolSetting("Kills", "Announce kills", true));
    public Announcer() { super("Announcer", "Chat announcements", Category.MISC); }
}
