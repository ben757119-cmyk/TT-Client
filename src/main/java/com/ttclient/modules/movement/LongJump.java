package com.ttclient.modules.movement;
import com.ttclient.modules.Category; import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
public class LongJump extends Module {
    public final NumberSetting boost = addSetting(new NumberSetting("Boost", "Jump boost", 2.5, 1, 5, 0.1));
    public LongJump() { super("LongJump", "Jump further", Category.MOVEMENT); }
}
