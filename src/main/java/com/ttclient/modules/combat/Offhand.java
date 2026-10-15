package com.ttclient.modules.combat;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.ModeSetting;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.InvUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;

/** Keep preferred item in offhand (totem / crystal / gapple). */
public class Offhand extends Module {
    public final ModeSetting mode = addSetting(new ModeSetting(
            "Mode", "Offhand item", "Totem", "Totem", "Crystal", "Gapple"));
    public final NumberSetting totemHp = addSetting(new NumberSetting(
            "TotemHP", "Force totem at or below this HP", 14, 1, 20, 1));

    public Offhand() {
        super("Offhand", "Auto offhand totem / crystal / gapple", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        LocalPlayer p = mc.player;

        // Always prefer totem when low
        if (p.getHealth() + p.getAbsorptionAmount() <= totemHp.get()) {
            if (!p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
                InvUtils.moveToOffhand(p, Items.TOTEM_OF_UNDYING);
            }
            return;
        }

        switch (mode.get()) {
            case "Crystal" -> {
                if (!p.getOffhandItem().is(Items.END_CRYSTAL))
                    InvUtils.moveToOffhand(p, Items.END_CRYSTAL);
            }
            case "Gapple" -> {
                if (!(p.getOffhandItem().is(Items.GOLDEN_APPLE) || p.getOffhandItem().is(Items.ENCHANTED_GOLDEN_APPLE))) {
                    if (!InvUtils.moveToOffhand(p, Items.ENCHANTED_GOLDEN_APPLE))
                        InvUtils.moveToOffhand(p, Items.GOLDEN_APPLE);
                }
            }
            default -> {
                if (!p.getOffhandItem().is(Items.TOTEM_OF_UNDYING))
                    InvUtils.moveToOffhand(p, Items.TOTEM_OF_UNDYING);
            }
        }
    }
}
