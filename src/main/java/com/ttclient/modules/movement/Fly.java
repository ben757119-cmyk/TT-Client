package com.ttclient.modules.movement;
import com.ttclient.modules.Category; import com.ttclient.modules.Module;
import com.ttclient.settings.ModeSetting; import com.ttclient.settings.NumberSetting;
public class Fly extends Module {
    public final ModeSetting mode = addSetting(new ModeSetting("Mode", "Fly mode", "Creative", "Creative", "Vanilla", "Packet"));
    public final NumberSetting speed = addSetting(new NumberSetting("Speed", "Fly speed", 1.0, 0.1, 5, 0.1));
    public Fly() { super("Fly", "Survival flight", Category.MOVEMENT); }
}
