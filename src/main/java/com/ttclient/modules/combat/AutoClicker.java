package com.ttclient.modules.combat;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;

public class AutoClicker extends Module {
    public final NumberSetting cps = addSetting(new NumberSetting("CPS", "Clicks per second", 10, 1, 20, 1));
    private int timer;

    public AutoClicker() {
        super("AutoClicker", "Hold attack to auto-click", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.options == null || mc.gameMode == null) return;
        if (!mc.options.keyAttack.isDown()) { timer = 0; return; }
        int interval = Math.max(1, (int) Math.round(20.0 / cps.get()));
        if (++timer % interval != 0) return;
        if (mc.hitResult instanceof EntityHitResult ehr) {
            mc.gameMode.attack(mc.player, ehr.getEntity());
        }
        mc.player.swing(InteractionHand.MAIN_HAND);
    }
}
