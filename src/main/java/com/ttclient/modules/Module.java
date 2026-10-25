package com.ttclient.modules;

import com.ttclient.client.NotificationManager;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;
    private int keyBind = -1;
    private final List<Setting<?>> settings = new ArrayList<>();

    /** Optional safety toggles — set by {@link #addCombatSafetyToggles()}. */
    protected BoolSetting disableOnDeath;
    protected BoolSetting disableOnServer;

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    /**
     * Adds DisableOnDeath + DisableOnServer toggles (default on).
     * Call from combat modules that auto-hit / rage.
     */
    protected void addCombatSafetyToggles() {
        disableOnDeath = addSetting(new BoolSetting(
                "DisableOnDeath",
                "Turn this module off when you die",
                true));
        disableOnServer = addSetting(new BoolSetting(
                "DisableOnServer",
                "Turn this module off when you leave or change servers",
                true));
    }

    public boolean shouldDisableOnDeath() {
        return disableOnDeath != null && disableOnDeath.get();
    }

    public boolean shouldDisableOnServerChange() {
        return disableOnServer != null && disableOnServer.get();
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
