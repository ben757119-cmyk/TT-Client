package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ModeSetting;
import com.ttclient.settings.NumberSetting;
import com.ttclient.util.InvUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Attribute swap: switch to best-scoring weapon for the hit, then swap back.
 * Matches common client pattern — same tick when possible via attack key + hit entity.
 */
public class AttributeSwapper extends Module {
    public final ModeSetting mode = addSetting(new ModeSetting(
            "Mode", "What to optimize",
            "Weapon", "Weapon", "Armor", "Both"));
    public final BoolSetting onlyOnAttack = addSetting(new BoolSetting(
            "OnlyOnAttack", "Only swap weapons while attacking",
            true));
    public final BoolSetting swapBack = addSetting(new BoolSetting(
            "SwapBack", "Restore previous hotbar slot after swap",
            true));
    public final BoolSetting preferSpeed = addSetting(new BoolSetting(
            "PreferSpeed", "Weight attack speed higher when scoring weapons",
            false));
    public final NumberSetting armorInterval = addSetting(new NumberSetting(
            "ArmorTicks", "Ticks between armor equip attempts",
            10, 2, 40, 1));

    private int armorTimer;
    /** Slot held before an attack swap; -1 = nothing to restore. */
    private int restoreSlot = -1;
    private boolean swappedThisPress;

    public AttributeSwapper() {
        super("AttributeSwapper",
                "Swap to the best weapon/armor by item attributes",
                Category.PLAYER);
    }

    @Override
    public void onDisable() {
        restoreIfNeeded();
        restoreSlot = -1;
        swappedThisPress = false;
        super.onDisable();
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.gameMode == null) return;
        LocalPlayer player = mc.player;
        String m = mode.get();

        if ("Weapon".equals(m) || "Both".equals(m)) {
            boolean attacking = mc.options != null
                    && mc.options.keyAttack.isDown()
                    && mc.hitResult != null
                    && mc.hitResult.getType() == HitResult.Type.ENTITY
                    && mc.hitResult instanceof EntityHitResult;

            boolean should = !onlyOnAttack.get() || attacking;

            if (should) {
                int best = findBestWeaponSlot(player);
                int cur = player.getInventory().getSelectedSlot();
                if (best >= 0 && best != cur) {
                    if (swapBack.get() && restoreSlot < 0) {
                        restoreSlot = cur;
                    }
                    InvUtils.selectHotbar(player, best);
                    swappedThisPress = true;
                }
            } else if (swapBack.get() && restoreSlot >= 0) {
                // Attack released / not aiming entity → restore
                InvUtils.selectHotbar(player, restoreSlot);
                restoreSlot = -1;
                swappedThisPress = false;
            }

            // Same-tick style when fully charged: if we just swapped for an attack and
            // attack strength is high, restore immediately after the swing frame.
            // (KillAura handles its own same-tick path; this covers manual clicks.)
            if (swapBack.get()
                    && onlyOnAttack.get()
                    && swappedThisPress
                    && restoreSlot >= 0
                    && attacking
                    && player.getAttackStrengthScale(0.5f) < 0.15f) {
                // Just swung — put original item back
                InvUtils.selectHotbar(player, restoreSlot);
                restoreSlot = -1;
                swappedThisPress = false;
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

    private void restoreIfNeeded() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || restoreSlot < 0) return;
        InvUtils.selectHotbar(mc.player, restoreSlot);
        restoreSlot = -1;
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

        if (damage <= 0) damage = heuristicDamage(stack);
        if (speed <= 0) speed = 1.0;

        if (preferSpeed.get()) {
            return damage * 0.55 + speed * 2.5;
        }
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

    private boolean tryEquipSlot(LocalPlayer player, Minecraft mc, EquipmentSlot slot, String... hints) {
        Inventory inv = player.getInventory();
        ItemStack current = player.getItemBySlot(slot);
        double currentScore = scoreArmor(current);

        int bestInv = -1;
        double bestScore = currentScore;
        // Search main inv (9–35) + hotbar
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (!matchesArmorHint(s, hints)) continue;
            double sc = scoreArmor(s);
            if (sc > bestScore) {
                bestScore = sc;
                bestInv = i;
            }
        }
        if (bestInv < 0) return false;

        // Prefer silent container swap when possible
        try {
            int armorMenuSlot = switch (slot) {
                case HEAD -> 5;
                case CHEST -> 6;
                case LEGS -> 7;
                case FEET -> 8;
                default -> -1;
            };
            if (armorMenuSlot < 0 || mc.gameMode == null) return false;
            int from = bestInv < 9 ? bestInv + 36 : bestInv;
            // SWAP click: button = armor menu slot index in player inventory menu
            // Use pickup/swap via InvUtils if available pattern — fall back to selected+use
            mc.gameMode.handleInventoryMouseClick(
                    player.containerMenu.containerId,
                    from,
                    armorMenuSlot,
                    net.minecraft.world.inventory.ClickType.SWAP,
                    player);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private double scoreArmor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        double armor = readAttribute(stack, Attributes.ARMOR, null);
        double toughness = readAttribute(stack, Attributes.ARMOR_TOUGHNESS, null);
        if (armor <= 0) armor = heuristicArmor(stack);
        return armor + toughness * 0.5;
    }

    private double readAttribute(ItemStack stack, Attribute attribute, EquipmentSlot preferred) {
        AtomicReference<Double> total = new AtomicReference<>(0.0);
        try {
            if (preferred != null) {
                stack.forEachModifier(preferred, (attr, modifier) -> {
                    if (attr == attribute || (attr != null && attr.is(attribute))) {
                        total.updateAndGet(v -> v + modifier.amount());
                    }
                });
            }
        } catch (Throwable ignored) {}

        if (total.get() != 0) return total.get();

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
        else if (n.contains("elytra")) mat = 0.5;
        else return 0;

        if (n.contains("helmet")) return mat + 1;
        if (n.contains("chestplate") || n.contains("elytra")) return mat + 3;
        if (n.contains("leggings")) return mat + 2;
        if (n.contains("boots")) return mat + 1;
        return mat;
    }
}
