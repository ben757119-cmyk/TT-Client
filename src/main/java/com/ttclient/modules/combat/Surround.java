package com.ttclient.modules.combat;
import com.ttclient.modules.Category; import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
public class Surround extends Module {
    public final BoolSetting center = addSetting(new BoolSetting("Center", "Center player", true));
    public Surround() { super("Surround", "Obsidian surround", Category.COMBAT); }
}
