package com.ttclient.modules.combat;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.*;

public class KillAura extends Module {
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Attack range", 4.2, 3.0, 6.0, 0.1));
    public final NumberSetting aps = addSetting(new NumberSetting("APS", "Attacks per second", 12, 1, 20, 1));
    public final ModeSetting priority = addSetting(new ModeSetting("Priority", "Target priority", "Distance", "Distance", "Health", "Angle"));
    public final BoolSetting players = addSetting(new BoolSetting("Players", "Attack players", true));
    public final BoolSetting hostiles = addSetting(new BoolSetting("Hostiles", "Attack hostiles", true));
    public final BoolSetting rotate = addSetting(new BoolSetting("Rotate", "Rotate to target", true));

    public KillAura() {
        super("KillAura", "Automatically attack nearby entities", Category.COMBAT);
    }
}
