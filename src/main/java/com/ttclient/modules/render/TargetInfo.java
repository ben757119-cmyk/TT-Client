package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.modules.client.ClickGUIModule;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ModeSetting;
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
 * Clean target HUD — preset positions (not stuck on crosshair).
 * Default: BottomCenter under crosshair but not covering it.
 */
public class TargetInfo extends Module {
    public final ModeSetting position = addSetting(new ModeSetting(
            "Position", "Screen anchor", "BottomCenter",
            "BottomCenter", "BottomLeft", "BottomRight", "TopLeft", "TopRight", "Custom"));
    public final NumberSetting scale = addSetting(new NumberSetting("Scale", "HUD scale", 1.0, 0.7, 1.5, 0.05));
    public final NumberSetting xOff = addSetting(new NumberSetting("X", "Custom / fine X offset", 0, -500, 500, 1));
    public final NumberSetting yOff = addSetting(new NumberSetting("Y", "Custom / fine Y offset", 0, -300, 300, 1));
    public final BoolSetting playersOnly = addSetting(new BoolSetting("PlayersOnly", "Only show players", false));
    public final NumberSetting linger = addSetting(new NumberSetting("Linger", "Keep last target ms", 2500, 500, 6000, 100));

    public TargetInfo() {
        super("TargetInfo", "Clean target HUD with position presets", Category.RENDER);
        setEnabled(true);
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

        int panelW = (int) (152 * sc);
        int panelH = (int) (46 * sc);
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        int x, y;
        String pos = position.get();
        int xo = xOff.getInt();
        int yo = yOff.getInt();
        switch (pos) {
            case "BottomLeft" -> { x = 10 + xo; y = sh - panelH - 40 + yo; }
            case "BottomRight" -> { x = sw - panelW - 10 + xo; y = sh - panelH - 40 + yo; }
            case "TopLeft" -> { x = 10 + xo; y = 28 + yo; }
            case "TopRight" -> { x = sw - panelW - 10 + xo; y = 28 + yo; }
            case "Custom" -> { x = sw / 2 - panelW / 2 + xo; y = sh / 2 + yo; }
            default -> { // BottomCenter — under crosshair, not on it
                x = sw / 2 - panelW / 2 + xo;
                y = sh / 2 + 48 + yo;
            }
        }

        // Panel
        g.fill(x, y, x + panelW, y + panelH, 0xE0080A0E);
        g.fill(x, y, x + 3, y + panelH, ac);
        g.fill(x, y, x + panelW, y + 1, (ac & 0x00FFFFFF) | 0x99000000);

        String name = target.getName().getString();
        if (name.length() > 14) name = name.substring(0, 13) + "…";
        g.text(font, name, x + 10, y + 6, 0xFFFFFFFF, false);

        float hp = target.getHealth() + target.getAbsorptionAmount();
        float max = Math.max(1f, target.getMaxHealth());
        String hpStr = String.format("%.1f", hp);
        g.text(font, hpStr, x + panelW - font.width(hpStr) - 8, y + 6, 0xFFB0B8C0, false);

        double dist = mc.player.distanceTo(target);
        String dStr = String.format("%.1fm", dist);
        g.text(font, dStr, x + 10, y + 17, 0xFF808890, false);

        // HP bar
        int barX = x + 10;
        int barY = y + panelH - 12;
        int barW = panelW - 20;
        int barH = 5;
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF1A1E24);
        float pct = Math.max(0f, Math.min(1f, hp / max));
        int fill = (int) (barW * pct);
        if (fill > 0) g.fill(barX, barY, barX + fill, barY + barH, healthColor(pct));

        if (target.hurtTime > 0) {
            int flash = Math.min(90, target.hurtTime * 10);
            g.fill(x, y, x + panelW, y + panelH, (flash << 24) | 0xFF3030);
        }
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
