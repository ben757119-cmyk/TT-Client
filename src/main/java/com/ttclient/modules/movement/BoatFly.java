package com.ttclient.modules.movement;
import com.ttclient.modules.Category; import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
public class BoatFly extends Module {
    public final NumberSetting speed = addSetting(new NumberSetting("Speed", "Boat fly speed", 1.5, 0.5, 5, 0.1));
    public BoatFly() { super("BoatFly", "Fly with boats", Category.MOVEMENT); }
}
