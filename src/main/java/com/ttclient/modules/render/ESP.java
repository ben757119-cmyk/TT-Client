package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.render.EspDraw;
import com.ttclient.render.EspProjection;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ColorSetting;
import com.ttclient.settings.ModeSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Entity ESP with Meteor-style aim coloring: red when aimable, blue otherwise. */
public class ESP extends Module {
    public final ModeSetting mode = addSetting(new ModeSetting("Mode", "ESP draw mode", "Box", "Box", "Fill", "Both"));
    public final BoolSetting players = addSetting(new BoolSetting("Players", "Highlight players", true));
    public final BoolSetting hostiles = addSetting(new BoolSetting("Hostiles", "Highlight hostiles", true));
    public final BoolSetting animals = addSetting(new BoolSetting("Animals", "Highlight animals", false));
    public final BoolSetting aimColor = addSetting(new BoolSetting("AimColor", "Red when aimable / blue when not", true));
    public final ColorSetting aimOn = addSetting(new ColorSetting("AimOn", "Color when in crosshair/hit range", 0xFFFF3030));
    public final ColorSetting aimOff = addSetting(new ColorSetting("AimOff", "Color when not aimed", 0xFF3080FF));
    public final ColorSetting playerColor = addSetting(new ColorSetting("Player Color", "Fallback player color", 0xFF55FF55));
    public final ColorSetting hostileColor = addSetting(new ColorSetting("Hostile Color", "Fallback hostile color", 0xFFFF5555));
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Max ESP distance", 64, 8, 128, 1));
    public final NumberSetting aimFov = addSetting(new NumberSetting("AimFov", "Degrees from crosshair for red", 8, 1, 45, 0.5));
    public final NumberSetting hitRange = addSetting(new NumberSetting("HitRange", "Range treated as attackable", 4.5, 2, 8, 0.1));

    public ESP() {
        super("ESP", "Entity highlighting (aim colors)", Category.RENDER);
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor g, float partialTick) {
        Minecraft mc = mc();
        if (mc == null || mc.level == null || mc.player == null || g == null) return;
        LocalPlayer self = mc.player;
        double maxR = range.get();
        double maxR2 = maxR * maxR;
        String m = mode.get();
        boolean doBox = "Box".equalsIgnoreCase(m) || "Both".equalsIgnoreCase(m);
        boolean doFill = "Fill".equalsIgnoreCase(m) || "Both".equalsIgnoreCase(m);

        for (Entity e : mc.level.getEntities(self, self.getBoundingBox().inflate(maxR))) {
            if (e == null || e == self || !(e instanceof LivingEntity living) || !living.isAlive()) continue;
            if (!passesFilter(living)) continue;
            double d2 = self.distanceToSqr(e);
            if (d2 > maxR2) continue;

            int color = resolveColor(self, living, Math.sqrt(d2));
            AABB bb = living.getBoundingBox().inflate(0.05);
            double[][] corners = corners(bb);
            double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
            int ok = 0;
            double[] out = new double[2];
            for (double[] c : corners) {
                if (!EspProjection.worldToScreen(c[0], c[1], c[2], out)) continue;
                ok++;
                minX = Math.min(minX, out[0]);
                minY = Math.min(minY, out[1]);
                maxX = Math.max(maxX, out[0]);
                maxY = Math.max(maxY, out[1]);
            }
            if (ok < 2) continue;
            if (maxX - minX > 400 || maxY - minY > 400) continue;
            if (doFill) EspDraw.fillBox(g, minX, minY, maxX, maxY, (color & 0x00FFFFFF) | 0x33000000);
            if (doBox) EspDraw.box(g, minX, minY, maxX, maxY, color, 2);
        }
    }

    private boolean passesFilter(LivingEntity e) {
        if (e instanceof Player) return Boolean.TRUE.equals(players.get());
        if (e instanceof Monster) return Boolean.TRUE.equals(hostiles.get());
        return Boolean.TRUE.equals(animals.get());
    }

    private int resolveColor(LocalPlayer self, LivingEntity target, double dist) {
        if (Boolean.TRUE.equals(aimColor.get())) {
            boolean aimed = dist <= hitRange.get() && lookingAt(self, target, aimFov.get());
            return aimed ? aimOn.get() : aimOff.get();
        }
        if (target instanceof Player) return playerColor.get();
        if (target instanceof Monster) return hostileColor.get();
        return playerColor.get();
    }

    private static boolean lookingAt(LocalPlayer self, LivingEntity target, double fovDeg) {
        Vec3 eye = self.getEyePosition();
        Vec3 look = self.getViewVector(1f);
        Vec3 to = target.getBoundingBox().getCenter().subtract(eye).normalize();
        double dot = look.dot(to);
        double cos = Math.cos(Math.toRadians(Math.max(1.0, fovDeg)));
        return dot >= cos;
    }

    private static double[][] corners(AABB bb) {
        return new double[][]{
                {bb.minX, bb.minY, bb.minZ}, {bb.maxX, bb.minY, bb.minZ},
                {bb.minX, bb.maxY, bb.minZ}, {bb.maxX, bb.maxY, bb.minZ},
                {bb.minX, bb.minY, bb.maxZ}, {bb.maxX, bb.minY, bb.maxZ},
                {bb.minX, bb.maxY, bb.maxZ}, {bb.maxX, bb.maxY, bb.maxZ}
        };
    }
}
