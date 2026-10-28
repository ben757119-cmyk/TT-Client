package com.ttclient.modules;

import com.ttclient.client.NotificationManager;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public enum Risk {
        /** Subtle / visual / QoL — less obvious to staff. */
        GHOST,
        /** Obvious combat / movement cheats. */
        BLATANT
    }

    private final String name;
    private final String description;
    private final Category category;
    private Risk risk;
    private boolean enabled;
    private int keyBind = -1;
    private final List<Setting<?>> settings = new ArrayList<>();

    protected BoolSetting disableOnDeath;
    protected BoolSetting disableOnServer;

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
        // Combat defaults to blatant; everything else ghost unless overridden.
        this.risk = category == Category.COMBAT ? Risk.BLATANT : Risk.GHOST;
    }

    public Module(String name, String description, Category category, Risk risk) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.risk = risk != null ? risk : Risk.GHOST;
    }

    protected void setRisk(Risk risk) {
        if (risk != null) this.risk = risk;
    }

    public Risk getRisk() {
        return risk == null ? Risk.GHOST : risk;
    }

    /** Minimal tag for GUI: "G" or "B". */
    public String riskTag() {
        return getRisk() == Risk.BLATANT ? "B" : "G";
    }

    protected void addCombatSafetyToggles() {
        disableOnDeath = addSetting(new BoolSetting(
                "DisableOnDeath",
                "Turn this module off when you die",
                true));
        disableOnServer = addSetting(new BoolSetting(
                "DisableOnServer",
                "Turn this module off when you leave or change servers",
                true));
        setRisk(Risk.BLATANT);
    }

    public boolean shouldDisableOnDeath() {
        return disableOnDeath != null && Boolean.TRUE.equals(disableOnDeath.get());
    }

    public boolean shouldDisableOnServerChange() {
        return disableOnServer != null && Boolean.TRUE.equals(disableOnServer.get());
    }

    protected static Minecraft mc() {
        return Minecraft.getInstance();
    }

    public boolean isLocked() {
        return false;
    }

    public String lockReason() {
        return "";
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) onEnable();
        else onDisable();
        try {
            NotificationManager.pushToggle(name, enabled);
        } catch (Exception ignored) {}
    }

    public boolean isEnabled() { return enabled; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public int getKeyBind() { return keyBind; }
    public void setKeyBind(int key) { this.keyBind = key; }
    public List<Setting<?>> getSettings() { return settings; }

    protected <T extends Setting<?>> T addSetting(T setting) {
        settings.add(setting);
        return setting;
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    public void onRender2D(GuiGraphicsExtractor graphics, float partialTick) {}
    public void onRender3D(float partialTick) {}
}
