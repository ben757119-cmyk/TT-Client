package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.modules.client.ClickGUIModule;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.SilentRotate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Vape-style target HUD — clean panel with HP bar, name, distance.
 */
public class TargetInfo extends Module {
    public final NumberSetting scale = addSetting(new NumberSetting("Scale", "HUD scale", 1.0, 0.7, 1.5, 0.05));
    public final NumberSetting xOff = addSetting(new NumberSetting("X", "Offset from center X", 0, -400, 400, 1));
    public final NumberSetting yOff = addSetting(new NumberSetting("Y", "Offset from center Y", 60, -200, 200, 1));
    public final BoolSetting playersOnly = addSetting(new BoolSetting("PlayersOnly", "Only show players", false));
    public final NumberSetting linger = addSetting(new NumberSetting("Linger", "Keep last target ms", 2500, 500, 6000, 100));

    public TargetInfo() {
        super("TargetInfo", "Vape-style target HUD", Category.RENDER);
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor g, float partialTick) {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.font == null) return;

        LivingEntity target = resolve(mc);
        if (target == null) return;

        Font font = mc.font;
        int ac = ClickGUIModule.globalAccent();
        float sc = (float) scale.get().doubleValue();

        int panelW = (int) (140 * sc);
        int panelH = (int) (42 * sc);
        int cx = mc.getWindow().getGuiScaledWidth() / 2 + xOff.getInt();
        int cy = mc.getWindow().getGuiScaledHeight() / 2 + yOff.getInt();
        int x = cx - panelW / 2;
        int y = cy - panelH / 2;

        g.fill(x, y, x + panelW, y + panelH, 0xE0080A0E);
        g.fill(x, y, x + 2, y + panelH, ac);
        g.fill(x, y, x + panelW, y + 1, (ac & 0x00FFFFFF) | 0x88000000);
        g.fill(x, y + panelH - 1, x + panelW, y + panelH, (ac & 0x00FFFFFF) | 0x44000000);

        String name = target.getName().getString();
        if (name.length() > 16) name = name.substring(0, 15) + "…";
        g.text(font, name, x + 8, y + 5, 0xFFFFFFFF, false);

        float hp = target.getHealth();
        float max = Math.max(1f, target.getMaxHealth());
        String hpStr = String.format("%.1f / %.0f", hp, max);
        int hpW = font.width(hpStr);
        g.text(font, hpStr, x + panelW - hpW - 6, y + 5, 0xFFB0B8C0, false);

        int barX = x + 8;
        int barY = y + panelH - 14;
        int barW = panelW - 16;
        int barH = 6;
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF1A1E24);

        float pct = Math.max(0f, Math.min(1f, hp / max));
        int fill = (int) (barW * pct);
        int barCol = healthColor(pct);
        if (fill > 0) g.fill(barX, barY, barX + fill, barY + barH, barCol);

        if (target.hurtTime > 0) {
            int flash = Math.min(120, target.hurtTime * 12);
            g.fill(x, y, x + panelW, y + panelH, (flash << 24) | 0xFF3030);
        }

        double dist = mc.player.distanceTo(target);
        String dStr = String.format("%.1fm", dist);
        g.text(font, dStr, x + 8, y + 16, 0xFF808890, false);

        String tag = target instanceof Player ? "PLAYER" : "ENTITY";
        g.text(font, tag, x + panelW - font.width(tag) - 6, y + 16, ac, false);
    }

    private static int healthColor(float pct) {
        if (pct > 0.6f) return 0xFF3DDC97;
        if (pct > 0.3f) return 0xFFE8C04A;
        return 0xFFE05050;
    }

    private LivingEntity resolve(Minecraft mc) {
        try {
            HitResult hr = mc.hitResult;
            if (hr instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living && living.isAlive()) {
                if (!(playersOnly.get() && !(living instanceof Player))) {
                    SilentRotate.markTarget(living);
                    return living;
                }
            }
        } catch (Throwable ignored) {}

        try {
            Entity e = mc.crosshairPickEntity;
            if (e instanceof LivingEntity living && living.isAlive()) {
                if (!(playersOnly.get() && !(living instanceof Player))) {
                    SilentRotate.markTarget(living);
                    return living;
                }
            }
        } catch (Throwable ignored) {}

        Entity last = SilentRotate.currentTarget(linger.getInt());
        if (last instanceof LivingEntity living && living.isAlive()) {
            if (!(playersOnly.get() && !(living instanceof Player))) return living;
        }
        return null;
    }
}
