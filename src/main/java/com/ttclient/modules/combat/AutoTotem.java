package com.ttclient.modules.combat;
import com.ttclient.modules.Category; import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
public class AutoTotem extends Module {
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Equip delay", 1, 0, 10, 1));
    public AutoTotem() { super("AutoTotem", "Auto equip totems", Category.COMBAT); }
}
