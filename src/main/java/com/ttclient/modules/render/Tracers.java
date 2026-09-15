package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ColorSetting;
import com.ttclient.settings.NumberSetting;

public class Tracers extends Module {
    public final BoolSetting players = addSetting(new BoolSetting("Players", "Trace players", true));
    public final BoolSetting hostiles = addSetting(new BoolSetting("Hostiles", "Trace hostiles", false));
    public final BoolSetting animals = addSetting(new BoolSetting("Animals", "Trace animals", false));
    public final ColorSetting color = addSetting(new ColorSetting("Color", "Tracer color", 0xFF00FFAA));
    public final NumberSetting width = addSetting(new NumberSetting("Width", "Line width", 1.5, 0.5, 5.0, 0.5));

    public Tracers() {
        super("Tracers", "Draw lines to entities", Category.RENDER);
    }
}
