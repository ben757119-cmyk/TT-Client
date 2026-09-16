package com.ttclient.modules.player;
import com.ttclient.modules.Category; import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
public class AutoGapple extends Module {
    public final NumberSetting health = addSetting(new NumberSetting("Health", "Eat below HP", 14, 1, 20, 1));
    public AutoGapple() { super("AutoGapple", "Auto golden apple", Category.PLAYER); }
}
