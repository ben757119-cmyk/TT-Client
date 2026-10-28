package com.ttclient.modules.client;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Resource-pack style visual toggles. Static helpers used by mixins. */
public class PackTweaks extends Module {
    public final BoolSetting lowFire = addSetting(new BoolSetting(
            "LowFire", "Shorter / quieter fire overlay", true));
    public final BoolSetting smallTotems = addSetting(new BoolSetting(
            "SmallTotems", "Scale down Totem of Undying in GUI / hand", true));
    public final BoolSetting lowShield = addSetting(new BoolSetting(
            "LowShield", "Slightly lower shield in first person", false));
    public final BoolSetting noPumpkin = addSetting(new BoolSetting(
            "NoPumpkin", "Hide pumpkin blur overlay", true));
    public final BoolSetting cleanScoreboard = addSetting(new BoolSetting(
            "CleanScoreboard", "Dim scoreboard background", false));
    public final BoolSetting rainbowTools = addSetting(new BoolSetting(
            "RainbowTools", "Animated rainbow on tools + weapons in inventory only", true));
    public final BoolSetting rainbowBlocks = addSetting(new BoolSetting(
            "RainbowBlocks", "Animated rainbow on block items in inventory only", false));
    public final BoolSetting forceGlint = addSetting(new BoolSetting(
            "ForceGlint", "Also force enchanted foil under the rainbow", false));
    public final NumberSetting rainbowSpeed = addSetting(new NumberSetting(
            "RainbowSpeed", "Hue cycles per second (higher = faster)", 1.0, 0.1, 8.0, 0.1));
    public final NumberSetting rainbowAlpha = addSetting(new NumberSetting(
            "RainbowAlpha", "Rainbow overlay opacity", 0.45, 0.1, 0.9, 0.05));
    public final NumberSetting totemScale = addSetting(new NumberSetting(
            "TotemScale", "Totem size multiplier", 0.55, 0.25, 1.0, 0.05));

    public PackTweaks() {
        super("PackTweaks", "Resource-pack style visuals + inventory rainbow", Category.PACK);
        setEnabled(true);
    }

    public static PackTweaks get() {
        try {
            var mods = com.ttclient.client.TTClientClient.modules;
            return mods == null ? null : mods.getModule(PackTweaks.class);
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean inGuiContext() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return false;
            try {
                if (mc.gui != null && mc.gui.screen() != null) return true;
            } catch (Throwable ignored) {}
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean noPumpkinEnabled() {
        PackTweaks pt = get();
        return pt != null && pt.isEnabled() && Boolean.TRUE.equals(pt.noPumpkin.get());
    }

    public static boolean lowFireEnabled() {
        PackTweaks pt = get();
        return pt != null && pt.isEnabled() && Boolean.TRUE.equals(pt.lowFire.get());
    }

    public static boolean isPumpkinOverlay(Object texture) {
        if (texture == null) return false;
        String s = String.valueOf(texture).toLowerCase();
        return s.contains("pumpkinblur") || s.contains("pumpkin_blur") || s.contains("pumpkin");
    }

    public static boolean isFireOverlay(Object texture) {
        if (texture == null) return false;
        String s = String.valueOf(texture).toLowerCase();
        return s.contains("fire") || s.contains("burning");
    }

    public static boolean shouldRainbow(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        PackTweaks pt = get();
        if (pt == null || !pt.isEnabled() || !inGuiContext()) return false;
        Item item = stack.getItem();
        if (item instanceof BlockItem) return Boolean.TRUE.equals(pt.rainbowBlocks.get());
        return Boolean.TRUE.equals(pt.rainbowTools.get());
    }

    public static boolean shouldForceFoil(ItemStack stack) {
        PackTweaks pt = get();
        if (pt == null || !pt.isEnabled()) return false;
        if (!Boolean.TRUE.equals(pt.forceGlint.get())) return false;
        return shouldRainbow(stack);
    }

    public static int rainbowArgb(float alphaOverride, int salt) {
        PackTweaks pt = get();
        double speed = pt != null ? pt.rainbowSpeed.get() : 1.0;
        double baseA = pt != null ? pt.rainbowAlpha.get() : 0.45;
        float hue = (float) (((System.currentTimeMillis() / 1000.0) * speed + salt * 0.07) % 1.0);
        int rgb = java.awt.Color.HSBtoRGB(hue, 1f, 1f);
        int a = alphaOverride >= 0f
                ? Math.max(0, Math.min(255, (int) (alphaOverride * 255)))
                : Math.max(0, Math.min(255, (int) (baseA * 255)));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}
