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
 * Keeps a Totem of Undying in the offhand.
 * Uses hotbar + SWAP_ITEM_WITH_OFFHAND (vanilla F-key packet), then inventory clicks.
 */
public class AutoTotem extends Module {
    public final NumberSetting delay = addSetting(new NumberSetting(
            "Delay", "Ticks between attempts after a swap", 2, 0, 20, 1));
    public final BoolSetting onlyEmpty = addSetting(new BoolSetting(
            "OnlyEmpty", "Only swap when offhand is empty (after pop)", false));
    public final NumberSetting health = addSetting(new NumberSetting(
            "Health", "Force equip at or below this HP (0 = always)", 0, 0, 20, 1));

    private int cooldown;

    public AutoTotem() {
        super("AutoTotem", "Auto equip totem in offhand", Category.PLAYER);
    }

    @Override
    public void onDisable() {
        cooldown = 0;
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.gameMode == null) return;
        LocalPlayer p = mc.player;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        // Already good
        if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;

        // Optional: only when empty (right after pop)
        if (onlyEmpty.get() && !p.getOffhandItem().isEmpty()) return;

        // Optional health gate
        if (health.get() > 0) {
            float hp = p.getHealth() + p.getAbsorptionAmount();
            if (hp > health.get()) return;
        }

        // Don't fight chests / inv screens — still OK in world / F3
        if (shouldPause(mc)) return;

        if (InvUtils.equipTotem(p)) {
            cooldown = Math.max(1, delay.getInt());
        } else {
            cooldown = Math.max(1, delay.getInt());
        }
    }

    private boolean shouldPause(Minecraft mc) {
        try {
            var s = mc.gui.screen();
            if (s == null) return false;
            if (s instanceof ClickGUIScreen) return false; // still allow while GUI open
            String n = s.getClass().getName().toLowerCase();
            return n.contains("container") || n.contains("chest") || n.contains("inventory")
                    || n.contains("furnace") || n.contains("shulker") || n.contains("hopper");
        } catch (Throwable t) {
            return false;
        }
    }
}
