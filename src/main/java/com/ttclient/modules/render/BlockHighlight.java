package com.ttclient.modules.render;
import com.ttclient.modules.Category; import com.ttclient.modules.Module;
import com.ttclient.settings.ColorSetting;
public class BlockHighlight extends Module {
    public final ColorSetting color = addSetting(new ColorSetting("Color", "Outline color", 0xFF00FFAA));
    public BlockHighlight() { super("BlockHighlight", "Custom block outline", Category.RENDER); }
}
