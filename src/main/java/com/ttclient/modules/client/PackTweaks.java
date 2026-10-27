package com.ttclient.modules.client;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Resource-pack style visual toggles. Rainbow = inventory GUI only. */
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
            "RainbowTools", "Rainbow glint on tools + weapons in inventory only", false));
    public final BoolSetting rainbowBlocks = addSetting(new BoolSetting(
            "RainbowBlocks", "Rainbow glint on block items in inventory only", false));
    public final BoolSetting forceGlint = addSetting(new BoolSetting(
            "ForceGlint", "Force enchanted foil layer when rainbow is on", true));
    public final NumberSetting rainbowSpeed = addSetting(new NumberSetting(
            "RainbowSpeed", "Rainbow cycle speed", 4.0, 0.5, 20.0, 0.5));
    public final NumberSetting totemScale = addSetting(new NumberSetting(
            "TotemScale", "Totem size multiplier", 0.55, 0.25, 1.0, 0.05));

    public PackTweaks() {
        super("PackTweaks", "Resource-pack style toggles (Client category)", Category.CLIENT);
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
            return mc != null && mc.screen != null;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean shouldRainbow(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        PackTweaks pt = get();
        if (pt == null || !pt.isEnabled()) return false;
        if (!inGuiContext()) return false;
        if (pt.rainbowTools.get() && isToolOrWeapon(stack)) return true;
        if (pt.rainbowBlocks.get() && isBlockItem(stack)) return true;
        return false;
    }

    public static boolean shouldForceFoil(ItemStack stack) {
        PackTweaks pt = get();
        if (pt == null || !pt.isEnabled() || !pt.forceGlint.get()) return false;
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
        if (pt == null || !pt.isEnabled() || !pt.smallTotems.get()) return 1f;
        return pt.totemScale.get().floatValue();
    }

    public static int rainbowArgb(float alpha) {
        PackTweaks pt = get();
        double speed = pt != null ? pt.rainbowSpeed.get().doubleValue() : 4.0;
        float hue = (float) ((System.currentTimeMillis() * speed * 0.00015) % 1.0);
        int rgb = java.awt.Color.HSBtoRGB(hue, 1f, 1f) & 0x00FFFFFF;
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        return (a << 24) | rgb;
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
