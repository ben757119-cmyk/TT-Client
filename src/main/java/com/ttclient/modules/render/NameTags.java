package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.render.EspDraw;
import com.ttclient.render.EspProjection;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Screen-space nametags (draw on HUD → visible through walls).
 * Players + optional ground items, with configurable range.
 */
public class NameTags extends Module {
    public final BoolSetting players = addSetting(new BoolSetting("Players", "Show player nametags", true));
    public final BoolSetting items = addSetting(new BoolSetting("Items", "Show ground item nametags", true));
    public final BoolSetting health = addSetting(new BoolSetting("Health", "Show player health", true));
    public final BoolSetting distance = addSetting(new BoolSetting("Distance", "Show distance", true));
    public final BoolSetting self = addSetting(new BoolSetting("Self", "Show on yourself", false));
    public final BoolSetting itemCount = addSetting(new BoolSetting("ItemCount", "Show stack size", true));
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Max distance (blocks)", 128, 16, 512, 8));
    public final NumberSetting itemRange = addSetting(new NumberSetting("ItemRange", "Max item distance", 64, 8, 256, 4));
    public final NumberSetting scale = addSetting(new NumberSetting("Scale", "Reserved", 1.0, 0.5, 2.0, 0.1));

    public NameTags() {
        super("NameTags", "Nametags through walls — players & ground items", Category.RENDER);
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor g, float partialTick) {
        Minecraft mc = mc();
        if (mc == null || mc.level == null || mc.player == null || mc.font == null) return;

        double[] screen = new double[2];
        double maxPlayer = range.get();
        double maxItem = itemRange.get();

        if (players.get()) {
            for (Player p : mc.level.players()) {
                if (!(p instanceof AbstractClientPlayer)) continue;
                if (!self.get() && p == mc.player) continue;
                // Still show invisible players (through walls / vanish) — that's the point

                double dist = mc.player.distanceTo(p);
                if (dist > maxPlayer) continue;

                double x = p.getX();
                double y = p.getY() + p.getBbHeight() + 0.35;
                double z = p.getZ();
                if (!EspProjection.worldToScreen(x, y, z, screen)) continue;

                StringBuilder sb = new StringBuilder(p.getName().getString());
                if (health.get()) {
                    float hp = p.getHealth() + p.getAbsorptionAmount();
                    sb.append(String.format(" %.0fHP", hp));
                }
                if (distance.get()) {
                    sb.append(String.format(" [%.0fm]", dist));
                }

                int color = p.isInvisible() ? 0xFFAAAAAA : 0xFFFFFFFF;
                EspDraw.label(g, mc.font, sb.toString(), screen[0], screen[1], color);
            }
        }

        if (items.get()) {
            // Scan loaded item entities in range (inflate AABB around player)
            double r = maxItem;
            AABB box = mc.player.getBoundingBox().inflate(r);
            for (Entity e : mc.level.getEntities(mc.player, box)) {
                if (!(e instanceof ItemEntity itemEnt) || !itemEnt.isAlive()) continue;

                double dist = mc.player.distanceTo(itemEnt);
                if (dist > maxItem) continue;

                ItemStack stack = itemEnt.getItem();
                if (stack.isEmpty()) continue;

                double x = itemEnt.getX();
                double y = itemEnt.getY() + 0.4;
                double z = itemEnt.getZ();
                if (!EspProjection.worldToScreen(x, y, z, screen)) continue;

                String name;
                try {
                    name = stack.getHoverName().getString();
                } catch (Throwable t) {
                    name = stack.getItem().toString();
                }

                StringBuilder sb = new StringBuilder(name);
                if (itemCount.get() && stack.getCount() > 1) {
                    sb.append(" x").append(stack.getCount());
                }
                if (distance.get()) {
                    sb.append(String.format(" [%.0fm]", dist));
                }

                EspDraw.label(g, mc.font, sb.toString(), screen[0], screen[1], 0xFFFFD07A);
            }
        }
    }
}
