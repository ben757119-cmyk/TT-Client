package com.ttclient.modules.combat;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.InvUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

/** Throw XP bottles (armor mend). */
public class AutoXP extends Module {
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Ticks between throws", 2, 0, 20, 1));

    private int timer;

    public AutoXP() {
        super("AutoXP", "Spam experience bottles", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.gameMode == null) return;
        if (timer > 0) { timer--; return; }

        LocalPlayer p = mc.player;
        int slot = InvUtils.findHotbar(p, Items.EXPERIENCE_BOTTLE);
        if (slot < 0) return;

        int prev = p.getInventory().getSelectedSlot();
        InvUtils.selectHotbar(p, slot);
        try {
            mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
            p.swing(InteractionHand.MAIN_HAND);
        } catch (Throwable ignored) {}
        InvUtils.selectHotbar(p, prev);
        timer = delay.getInt();
    }
}
