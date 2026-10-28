package com.ttclient.modules.client;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Visual tool flair: dual-wield axe illusion, per-tool swing feel.
 * Rendering hooks live in mixins (soft require=0 for 26.2 name variance).
 */
public class ToolStyles extends Module {
    public final BoolSetting dualAxe = addSetting(new BoolSetting(
            "DualAxe", "When holding an axe with empty offhand, show a second axe in the offhand", true));
    public final BoolSetting customSwings = addSetting(new BoolSetting(
            "CustomSwings", "Personalized swing feel per tool type", true));
    public final NumberSetting swordSwing = addSetting(new NumberSetting(
            "SwordSwing", "Sword swing speed multiplier (jab / slash feel)", 1.25, 0.5, 2.0, 0.05));
    public final NumberSetting axeSwing = addSetting(new NumberSetting(
            "AxeSwing", "Axe swing speed multiplier (heavy chop)", 0.75, 0.4, 1.5, 0.05));
    public final NumberSetting pickSwing = addSetting(new NumberSetting(
            "PickSwing", "Pickaxe swing speed (fast dig pulses)", 1.35, 0.5, 2.0, 0.05));
    public final NumberSetting shovelSwing = addSetting(new NumberSetting(
            "ShovelSwing", "Shovel swing speed", 1.1, 0.5, 2.0, 0.05));
    public final NumberSetting hoeSwing = addSetting(new NumberSetting(
            "HoeSwing", "Hoe swing speed", 1.15, 0.5, 2.0, 0.05));

    public ToolStyles() {
        super("ToolStyles", "Dual axe visual + per-tool swing styles", Category.RENDER);
        setEnabled(true);
    }

    public static ToolStyles get() {
        try {
            var mods = com.ttclient.client.TTClientClient.modules;
            return mods == null ? null : mods.getModule(ToolStyles.class);
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean dualAxeActive() {
        ToolStyles ts = get();
        return ts != null && ts.isEnabled() && Boolean.TRUE.equals(ts.dualAxe.get());
    }

    public static boolean swingsActive() {
        ToolStyles ts = get();
        return ts != null && ts.isEnabled() && Boolean.TRUE.equals(ts.customSwings.get());
    }

    public static boolean isAxe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String id = stack.getItem().toString().toLowerCase();
        return id.contains("axe") && !id.contains("pickaxe");
    }

    public static boolean isSword(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem().toString().toLowerCase().contains("sword");
    }

    public static boolean isPick(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem().toString().toLowerCase().contains("pickaxe");
    }

    public static boolean isShovel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem().toString().toLowerCase().contains("shovel");
    }

    public static boolean isHoe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem().toString().toLowerCase().contains("hoe");
    }

    public static boolean shouldDualAxeRender(Player player) {
        if (!dualAxeActive() || player == null) return false;
        try {
            ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
            ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
            return isAxe(main) && (off == null || off.isEmpty());
        } catch (Throwable t) {
            return false;
        }
    }

    public static float swingMultiplier(ItemStack stack) {
        if (!swingsActive() || stack == null || stack.isEmpty()) return 1f;
        ToolStyles ts = get();
        if (ts == null) return 1f;
        try {
            if (isSword(stack)) return ts.swordSwing.getFloat();
            if (isAxe(stack)) return ts.axeSwing.getFloat();
            if (isPick(stack)) return ts.pickSwing.getFloat();
            if (isShovel(stack)) return ts.shovelSwing.getFloat();
            if (isHoe(stack)) return ts.hoeSwing.getFloat();
        } catch (Throwable ignored) {}
        return 1f;
    }

    public static ItemStack mainHand(Player p) {
        try {
            return p.getItemInHand(InteractionHand.MAIN_HAND);
        } catch (Throwable t) {
            return ItemStack.EMPTY;
        }
    }
}
