package com.ttclient.modules.client;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * ClickGUI "Resource Pack" strip — client visual tweaks that complement the TT RP.
 * Rainbow glint applies only while a screen is open (inventory / containers), never world drops.
 */
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
        super("PackTweaks", "Resource-pack style toggles (bottom of ClickGUI)", Category.CLIENT);
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
        Item item = stack.getItem();
        String id = itemId(item);
        if (id.isEmpty()) return false;
        return id.contains("sword") || id.contains("axe") || id.contains("pickaxe")
                || id.contains("shovel") || id.contains("hoe") || id.contains("mace")
                || id.contains("spear") || id.contains("trident") || id.contains("bow")
                || id.contains("crossbow") || id.contains("mace");
    }

    public static boolean isBlockItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        try {
            return stack.getItem() instanceof BlockItem;
        } catch (Throwable t) {
            String id = itemId(stack.getItem());
            return !id.isEmpty() && !isToolOrWeapon(stack);
        }
    }

    public static boolean isTotem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        try {
            return stack.is(Items.TOTEM_OF_UNDYING);
        } catch (Throwable t) {
            return itemId(stack.getItem()).contains("totem");
        }
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
            ResourceLocation rl = BuiltInRegistries.ITEM.getKey(item);
            return rl != null ? rl.getPath().toLowerCase() : "";
        } catch (Throwable t) {
            return "";
        }
    }
}
