package com.ttclient.modules.world;
import com.ttclient.modules.Category; import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
public class AutoFarm extends Module {
    public final BoolSetting replant = addSetting(new BoolSetting("Replant", "Replant crops", true));
    public AutoFarm() { super("AutoFarm", "Auto farm crops", Category.WORLD); }
}
