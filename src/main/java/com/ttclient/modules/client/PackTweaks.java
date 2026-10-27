package com.ttclient.modules.client;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Resource-pack style visual toggles. Lives in its own Pack ClickGUI panel. */
public class PackTweaks extends Module {
    public final BoolSetting lowFire = addSetting(new BoolSetting(
            "LowFire", "Shorter / quieter fire overlay (pairs with TT RP fire textures)", true));
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

    /** 26.2: screen is mc.gui.screen(), not mc.screen. */
    public static boolean inGuiContext() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return false;
            try {
                if (mc.gui != null && mc.gui.screen() != null) return true;
            } catch (Throwable ignored) {}
            try {
                Object gui = mc.getClass().getField("gui").get(mc);
                if (gui != null) {
                    Object s = gui.getClass().getMethod("screen").invoke(gui);
                    if (s != null) return true;
                }
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

    public static boolean lowShieldEnabled() {
        PackTweaks pt = get();
        return pt != null && pt.isEnabled() && Boolean.TRUE.equals(pt.lowShield.get());
    }

    public static boolean cleanScoreboardEnabled() {
        PackTweaks pt = get();
        return pt != null && pt.isEnabled() && Boolean.TRUE.equals(pt.cleanScoreboard.get());
    }

    public static boolean smallTotemsEnabled() {
        PackTweaks pt = get();
        return pt != null && pt.isEnabled() && Boolean.TRUE.equals(pt.smallTotems.get());
    }

    /** Texture path looks like pumpkin blur overlay. */
    public static boolean isPumpkinOverlay(Object texture) {
        if (texture == null) return false;
        String s = texture.toString().toLowerCase();
        return s.contains("pumpkin") || s.contains("blur");
    }

    /** Texture path looks like fire overlay. */
    public static boolean isFireOverlay(Object texture) {
        if (texture == null) return false;
        String s = texture.toString().toLowerCase();
        return s.contains("fire") || s.contains("burning");
    }

    public static boolean shouldRainbow(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        PackTweaks pt = get();
        if (pt == null || !pt.isEnabled()) return false;
        if (!inGuiContext()) return false;
        if (Boolean.TRUE.equals(pt.rainbowTools.get()) && isToolOrWeapon(stack)) return true;
        if (Boolean.TRUE.equals(pt.rainbowBlocks.get()) && isBlockItem(stack)) return true;
        return false;
    }

    public static boolean shouldForceFoil(ItemStack stack) {
        PackTweaks pt = get();
        if (pt == null || !pt.isEnabled() || !Boolean.TRUE.equals(pt.forceGlint.get())) return false;
        return shouldRainbow(stack);
    }

    public static boolean isToolOrWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String id = itemId(stack.getItem());
        if (id.isEmpty()) return false;
        return id.contains("sword") || id.contains("axe") || id.contains("pickaxe")
                || id.contains("shovel") || id.contains("hoe") || id.contains("mace")
                || id.contains("spear") || id.contains("trident") || id.contains("bow")
                || id.contains("crossbow");
    }

    public static boolean isBlockItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        try { return stack.getItem() instanceof BlockItem; }
        catch (Throwable t) { return false; }
    }

    public static boolean isTotem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return itemId(stack.getItem()).contains("totem");
    }

    public static float totemScaleOrDefault() {
        PackTweaks pt = get();
        if (pt == null || !pt.isEnabled() || !Boolean.TRUE.equals(pt.smallTotems.get())) return 1f;
        return pt.totemScale.getFloat();
    }

    public static int rainbowArgb(float alphaOverride, int slotSalt) {
        PackTweaks pt = get();
        double speed = pt != null ? pt.rainbowSpeed.get() : 1.0;
        if (speed < 0.01) speed = 0.01;
        float alpha = alphaOverride;
        if (alpha < 0f && pt != null) alpha = pt.rainbowAlpha.getFloat();
        if (alpha < 0f) alpha = 0.45f;
        double t = System.nanoTime() / 1_000_000_000.0;
        float hue = (float) ((t * speed + slotSalt * 0.07) % 1.0);
        if (hue < 0f) hue += 1f;
        int rgb = java.awt.Color.HSBtoRGB(hue, 1f, 1f) & 0x00FFFFFF;
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        return (a << 24) | rgb;
    }

    public static int rainbowArgb(float alpha) {
        return rainbowArgb(alpha, 0);
    }

    private static String itemId(Item item) {
        try {
            String s = String.valueOf(item);
            int slash = s.lastIndexOf(':');
            int dot = s.lastIndexOf('.');
            int cut = Math.max(slash, dot);
            if (cut >= 0 && cut + 1 < s.length()) s = s.substring(cut + 1);
            return s.toLowerCase().replace("}", "").trim();
        } catch (Throwable t) {
            return "";
        }
    }
}
