package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ModeSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Swaps hotbar / equip slots based on item attribute scores
 * (attack damage, attack speed, armor).
 */
public class AttributeSwapper extends Module {
    public final ModeSetting mode = addSetting(new ModeSetting(
            "Mode", "What to optimize",
            "Weapon", "Weapon", "Armor", "Both"));
    public final BoolSetting onlyOnAttack = addSetting(new BoolSetting(
            "OnlyOnAttack", "Only swap weapons while attacking",
            true));
    public final BoolSetting preferSpeed = addSetting(new BoolSetting(
            "PreferSpeed", "Weight attack speed higher when scoring weapons",
            false));
    public final NumberSetting armorInterval = addSetting(new NumberSetting(
            "ArmorTicks", "Ticks between armor equip attempts",
            10, 2, 40, 1));

    private int armorTimer;

    public AttributeSwapper() {
        super("AttributeSwapper",
                "Swap to the best weapon/armor by item attributes",
                Category.PLAYER);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.gameMode == null) return;
        LocalPlayer player = mc.player;
        String m = mode.get();

        if ("Weapon".equals(m) || "Both".equals(m)) {
            boolean should = !onlyOnAttack.get()
                    || (mc.options != null && mc.options.keyAttack.isDown()
                    && mc.hitResult != null
                    && mc.hitResult.getType() == HitResult.Type.ENTITY
                    && mc.hitResult instanceof EntityHitResult);
            if (should) {
                int best = findBestWeaponSlot(player);
                if (best >= 0 && best != player.getInventory().getSelectedSlot()) {
                    player.getInventory().setSelectedSlot(best);
                }
            }
        }

        if ("Armor".equals(m) || "Both".equals(m)) {
            if (armorTimer > 0) {
                armorTimer--;
            } else {
                if (tryEquipBestArmor(player, mc)) {
                    armorTimer = armorInterval.getInt();
                }
            }
        }
    }

    private int findBestWeaponSlot(LocalPlayer player) {
        Inventory inv = player.getInventory();
        int bestSlot = -1;
        double bestScore = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            double score = scoreWeapon(stack);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestScore > 0 ? bestSlot : -1;
    }

    private double scoreWeapon(ItemStack stack) {
        double damage = readAttribute(stack, Attributes.ATTACK_DAMAGE, EquipmentSlot.MAINHAND);
        double speed = readAttribute(stack, Attributes.ATTACK_SPEED, EquipmentSlot.MAINHAND);

        // Fallback heuristics when attribute components are missing / zero
        if (damage <= 0) {
            damage = heuristicDamage(stack);
        }
        if (speed <= 0) {
            speed = 1.0;
        }

        if (preferSpeed.get()) {
            return damage * 0.55 + speed * 2.5;
        }
        // Prefer raw DPS-ish score
        return damage * Math.max(0.1, speed);
    }

    private boolean tryEquipBestArmor(LocalPlayer player, Minecraft mc) {
        boolean swapped = false;
        swapped |= tryEquipSlot(player, mc, EquipmentSlot.HEAD, "helmet", "skull");
        swapped |= tryEquipSlot(player, mc, EquipmentSlot.CHEST, "chestplate", "elytra");
        swapped |= tryEquipSlot(player, mc, EquipmentSlot.LEGS, "leggings");
        swapped |= tryEquipSlot(player, mc, EquipmentSlot.FEET, "boots");
        return swapped;
    }

    private boolean tryEquipSlot(LocalPlayer player, Minecraft mc, EquipmentSlot slot, String... nameHints) {
        ItemStack current = player.getItemBySlot(slot);
        double currentScore = scoreArmorPiece(current, slot);

        Inventory inv = player.getInventory();
        int bestHotbar = -1;
        double bestScore = currentScore;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            if (!matchesArmorHint(stack, nameHints) && scoreArmorPiece(stack, slot) <= 0) continue;
            double score = scoreArmorPiece(stack, slot);
            if (score <= 0) {
                // name-based allow if slot empty
                if (current.isEmpty() && matchesArmorHint(stack, nameHints)) {
                    score = heuristicArmor(stack);
                } else {
                    continue;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestHotbar = i;
            }
        }

        if (bestHotbar < 0) return false;

        int prev = inv.getSelectedSlot();
        inv.setSelectedSlot(bestHotbar);
        mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        inv.setSelectedSlot(prev);
        return true;
    }

    private double scoreArmorPiece(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) return 0;
        double armor = readAttribute(stack, Attributes.ARMOR, slot);
        double toughness = readAttribute(stack, Attributes.ARMOR_TOUGHNESS, slot);
        if (armor <= 0 && toughness <= 0) {
            return heuristicArmor(stack);
        }
        return armor + toughness * 0.5;
    }

    private double readAttribute(ItemStack stack, Holder<Attribute> attribute, EquipmentSlot slot) {
        AtomicReference<Double> total = new AtomicReference<>(0.0);
        try {
            // 1.21+ style forEachModifier if present
            stack.forEachModifier(slot, (attr, modifier) -> {
                if (attr == attribute || (attr != null && attr.is(attribute))) {
                    total.updateAndGet(v -> v + modifier.amount());
                }
            });
        } catch (Throwable ignored) {
            try {
                // Some mappings use EquipmentSlotGroup; try MAINHAND/any via reflection-free amount from item name only
            } catch (Throwable ignored2) {}
        }

        if (total.get() != 0) return total.get();

        // Try any-slot modifiers
        try {
            for (EquipmentSlot s : EquipmentSlot.values()) {
                stack.forEachModifier(s, (attr, modifier) -> {
                    if (attr == attribute || (attr != null && attr.is(attribute))) {
                        total.updateAndGet(v -> v + modifier.amount());
                    }
                });
            }
        } catch (Throwable ignored) {}

        return total.get();
    }

    private static boolean matchesArmorHint(ItemStack stack, String... hints) {
        String n = stack.getItem().toString().toLowerCase();
        for (String h : hints) {
            if (n.contains(h)) return true;
        }
        return false;
    }

    private static double heuristicDamage(ItemStack stack) {
        String n = stack.getItem().toString().toLowerCase();
        double base = 0;
        if (n.contains("netherite")) base = 8;
        else if (n.contains("diamond")) base = 7;
        else if (n.contains("iron")) base = 6;
        else if (n.contains("stone")) base = 5;
        else if (n.contains("golden") || n.contains("gold")) base = 4;
        else if (n.contains("wooden") || n.contains("wood")) base = 4;
        else return 0;

        if (n.contains("sword")) return base + 3;
        if (n.contains("axe")) return base + 4;
        if (n.contains("trident")) return 9;
        if (n.contains("mace")) return 10;
        if (n.contains("spear")) return base + 3;
        return base;
    }

    private static double heuristicArmor(ItemStack stack) {
        String n = stack.getItem().toString().toLowerCase();
        double mat = 0;
        if (n.contains("netherite")) mat = 4;
        else if (n.contains("diamond")) mat = 3.5;
        else if (n.contains("iron")) mat = 2.5;
        else if (n.contains("chain")) mat = 2;
        else if (n.contains("golden") || n.contains("gold")) mat = 1.5;
        else if (n.contains("leather")) mat = 1;
        else if (n.contains("elytra")) mat = 0.5; // not armor but wearable chest
        else return 0;

        if (n.contains("helmet")) return mat + 1;
        if (n.contains("chestplate") || n.contains("elytra")) return mat + 3;
        if (n.contains("leggings")) return mat + 2;
        if (n.contains("boots")) return mat + 1;
        return mat;
    }
}
