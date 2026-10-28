package com.ttclient.modules.client;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;

/** GUI-only pack visual toggles (footer chips in ClickGUI). */
public class PackTweaks extends Module {
    public final BoolSetting lowFire = addSetting(new BoolSetting("LowFire", "Lower fire overlay", false));
    public final BoolSetting smallTotems = addSetting(new BoolSetting("SmallTotems", "Smaller totems", false));
    public final BoolSetting noPumpkin = addSetting(new BoolSetting("NoPumpkin", "Hide pumpkin blur", false));
    public final BoolSetting rainbowTools = addSetting(new BoolSetting("RainbowTools", "Rainbow tool tints", false));
    public final BoolSetting rainbowBlocks = addSetting(new BoolSetting("RainbowBlocks", "Rainbow block tints", false));

    public PackTweaks() {
        super("PackTweaks", "Resource pack visual toggles", Category.CLIENT);
    }
}
