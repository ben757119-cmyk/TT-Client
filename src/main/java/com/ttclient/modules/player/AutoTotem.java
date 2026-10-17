package com.ttclient.modules.player;

import com.ttclient.gui.ClickGUIScreen;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.InvUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;

/**
 * Auto-equip Totem of Undying to offhand.
 *
 * Silent only: uses inventory SWAP → offhand (button 40).
 * Never selects a hotbar slot / never sends F-key swap.
 * Works for hotbar AND main-inventory totems.
 */
public class AutoTotem extends Module {
    public final NumberSetting delay = addSetting(new NumberSetting(
            "Delay", "Ticks between equip attempts", 1, 0, 20, 1));
    public final BoolSetting onlyEmpty = addSetting(new BoolSetting(
            "OnlyEmpty", "Only swap when offhand is empty (after pop)", false));
    public final NumberSetting health = addSetting(new NumberSetting(
            "Health", "Only equip at or below this HP (0 = always)", 0, 0, 20, 1));
    public final BoolSetting soft = addSetting(new BoolSetting(
            "Soft", "Don't replace crystal/gapple/shield unless empty or low HP", false));

    private int cooldown;

    public AutoTotem() {
        super("AutoTotem", "Silent auto totem (no hotbar switch)", Category.PLAYER);
    }

    @Override
    public void onDisable() {
        cooldown = 0;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.gameMode == null || mc.level == null) return;
        LocalPlayer p = mc.player;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
        if (!shouldEquip(p)) return;
        if (shouldPause(mc)) return;

        int slot = InvUtils.findItemSlot(p, Items.TOTEM_OF_UNDYING);
        if (slot < 0) {
            cooldown = 2;
            return;
        }

        boolean ok = InvUtils.swapToOffhandSilent(p, slot);
        cooldown = ok ? Math.max(1, delay.getInt()) : 2;
    }

    private boolean shouldEquip(LocalPlayer p) {
        if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return false;
        if (onlyEmpty.get() && !p.getOffhandItem().isEmpty()) return false;

        float hp = p.getHealth() + p.getAbsorptionAmount();
        if (health.get() > 0 && hp > health.get()) return false;

        if (soft.get() && !p.getOffhandItem().isEmpty() && hp > 8.0f) {
            var off = p.getOffhandItem();
            if (off.is(Items.END_CRYSTAL) || off.is(Items.GOLDEN_APPLE)
                    || off.is(Items.ENCHANTED_GOLDEN_APPLE) || off.is(Items.SHIELD)) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldPause(Minecraft mc) {
        try {
            var s = mc.gui.screen();
            if (s == null) return false;
            if (s instanceof ClickGUIScreen) return false;
            String n = s.getClass().getName().toLowerCase();
            return n.contains("chest") || n.contains("shulker") || n.contains("hopper")
                    || n.contains("barrel") || n.contains("furnace") || n.contains("brewing")
                    || n.contains("anvil") || n.contains("beacon") || n.contains("container");
        } catch (Throwable t) {
            return false;
        }
    }
}
