package com.ttclient.modules.combat;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.ModeSetting;

public class Offhand extends Module {
    public final ModeSetting item = addSetting(new ModeSetting("Item", "Offhand item", "Totem", "Totem", "Crystal", "Gapple", "Shield"));

    public Offhand() {
        super("Offhand", "Manage offhand item", Category.COMBAT);
    }
}
