package com.ttclient.modules.combat;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.InvUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

/** Throw wind charge for mace setups (jump boost / reposition). */
public class WindChargeAssist extends Module {
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Cooldown ticks", 20, 5, 60, 1));
    public final BoolSetting onJump = addSetting(new BoolSetting("OnJump", "Use when you press jump in air", true));
    public final BoolSetting hold = addSetting(new BoolSetting("WhileEnabled", "Throw periodically while on", false));

    private int timer;
    private boolean wasJump;

    public WindChargeAssist() {
        super("WindCharge", "Auto-use wind charges for mace setups", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.gameMode == null || mc.options == null) return;
        if (timer > 0) { timer--; return; }

        LocalPlayer p = mc.player;
        int slot = InvUtils.findHotbar(p, Items.WIND_CHARGE);
        if (slot < 0) return;

        boolean jump = mc.options.keyJump.isDown();
        boolean trigger = false;
        if (hold.get()) trigger = true;
        if (onJump.get() && jump && !wasJump && !p.onGround()) trigger = true;
        wasJump = jump;

        if (!trigger) return;

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
