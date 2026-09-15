package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ColorSetting;
import com.ttclient.settings.ModeSetting;

public class ESP extends Module {
    public final ModeSetting mode = addSetting(new ModeSetting("Mode", "ESP mode", "Box", "Box", "Outline", "Glow"));
    public final BoolSetting players = addSetting(new BoolSetting("Players", "Highlight players", true));
    public final BoolSetting hostiles = addSetting(new BoolSetting("Hostiles", "Highlight hostiles", true));
    public final BoolSetting animals = addSetting(new BoolSetting("Animals", "Highlight animals", false));
    public final ColorSetting playerColor = addSetting(new ColorSetting("Player Color", "Player ESP color", 0xFF55FF55));
    public final ColorSetting hostileColor = addSetting(new ColorSetting("Hostile Color", "Hostile ESP color", 0xFFFF5555));

    public ESP() {
        super("ESP", "Entity highlighting", Category.RENDER);
    }
}
