package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;

public class Announcer extends Module {
    public final BoolSetting breaks = addSetting(new BoolSetting("Breaks", "Announce block breaks", false));
    public final BoolSetting places = addSetting(new BoolSetting("Places", "Announce block places", false));
    public final BoolSetting kills = addSetting(new BoolSetting("Kills", "Announce kills", true));

    public Announcer() {
        super("Announcer", "Announce actions in chat", Category.MISC);
    }
}
