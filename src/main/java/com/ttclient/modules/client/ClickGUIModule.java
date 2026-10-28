package com.ttclient.modules.client;

import com.ttclient.client.TTClientClient;
import com.ttclient.gui.ClickGUIScreen;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ColorSetting;
import com.ttclient.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

public class ClickGUIModule extends Module {
    public final ColorSetting accent = addSetting(new ColorSetting("Accent", "Panel accent color", 0xFF00E8A0));
    public final BoolSetting dim = addSetting(new BoolSetting("SolidDim", "Optional solid shade behind panels. Never vanilla blur.", false));
    public final ModeSetting style = addSetting(new ModeSetting(
            "Style",
            "ClickGUI layout style",
            "Center",
            "Center", "Classic", "Sidebar", "Meteor", "Wurst", "Radial"));
    public final ModeSetting showFilter = addSetting(new ModeSetting(
            "Show",
            "Filter modules by risk: Ghost (subtle), Blatant (obvious), or Both",
            "Both",
            "Both", "Ghost", "Blatant"));

    public ClickGUIModule() {
        super("ClickGUI", "Opens the TT Client ClickGUI (Right Shift)", Category.CLIENT);
        setKeyBind(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public String styleName() {
        try {
            String s = style.get();
            if (s == null) return "Center";
            if (s.equalsIgnoreCase("Island")) return "Center";
            return s;
        } catch (Throwable t) {
            return "Center";
        }
    }

    public String filterName() {
        try {
            String s = showFilter.get();
            return s == null ? "Both" : s;
        } catch (Throwable t) {
            return "Both";
        }
    }

    public static String globalFilter() {
        try {
            if (TTClientClient.modules != null) {
                ClickGUIModule m = TTClientClient.modules.getModule(ClickGUIModule.class);
                if (m != null) return m.filterName();
            }
        } catch (Throwable ignored) {}
        return "Both";
    }

    public static String globalStyle() {
        try {
            if (TTClientClient.modules != null) {
                ClickGUIModule m = TTClientClient.modules.getModule(ClickGUIModule.class);
                if (m != null) return m.styleName();
            }
        } catch (Throwable ignored) {}
        return "Center";
    }

    public int liveAccent() {
        if (isSeizure()) {
            float hue = (System.currentTimeMillis() % 1200L) / 1200f;
            int rgb = java.awt.Color.HSBtoRGB(hue, 1f, 1f);
            return 0xFF000000 | (rgb & 0x00FFFFFF);
        }
        return accent.get();
    }

    public static int globalAccent() {
        try {
            if (TTClientClient.modules != null) {
                ClickGUIModule m = TTClientClient.modules.getModule(ClickGUIModule.class);
                if (m != null) return m.liveAccent();
            }
        } catch (Throwable ignored) {}
        return 0xFF00E8A0;
    }

    public static boolean isSeizure() {
        try {
            if (TTClientClient.modules == null) return false;
            Module s = TTClientClient.modules.getModule("SeizureMode");
            return s != null && s.isEnabled();
        } catch (Throwable ignored) {}
        return false;
    }

    @Override
    public void onEnable() {
        Minecraft minecraft = mc();
        if (minecraft == null) {
            setEnabled(false);
            return;
        }
        try {
            minecraft.gui.setScreen(new ClickGUIScreen());
        } catch (Throwable t) {
            try {
                Minecraft.class.getMethod("setScreen", Screen.class).invoke(minecraft, new ClickGUIScreen());
            } catch (Throwable ignored) {}
        }
        setEnabled(false);
    }
}
