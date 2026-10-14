package com.ttclient.modules;

import com.ttclient.client.NotificationManager;
import com.ttclient.license.LicenseManager;
import com.ttclient.settings.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;
    private int keyBind = -1;
    private final List<Setting<?>> settings = new ArrayList<>();

    /** Modules incomplete / disabled in beta builds. */
    private static final Set<String> BETA_LOCKED_FEATURES = Set.of(
            "ESP", "Tracers", "NameTags", "StorageESP", "HoleESP", "Search",
            "XRay", "KillAura", "AimAssist", "TriggerBot", "AutoCrystal",
            "Surround", "Scaffold", "Nuker", "ChinaHat", "PopChams",
            "Trajectories", "Radar", "Breadcrumbs", "BlockHighlight",
            "ViewModel", "CustomSky", "NewChunks", "FakePlayer",
            "StashFinder", "LiquidPlace", "AutoFarm"
    );

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected static Minecraft mc() {
        return Minecraft.getInstance();
    }

    /** True if this feature is locked on beta builds. */
    public boolean isBetaFeatureLocked() {
        if (LicenseManager.isMaster()) return false;
        return BETA_LOCKED_FEATURES.contains(name);
    }

    /** True if user cannot enable this module right now. */
    public boolean isLocked() {
        if ("License".equals(name) || "KeyGen".equals(name)) return false;
        if (isBetaFeatureLocked()) return true;
        if (!LicenseManager.isMaster() && !LicenseManager.isUnlocked()) return true;
        return false;
    }

    public String lockReason() {
        if (isBetaFeatureLocked()) return "Locked in beta";
        if (!LicenseManager.isMaster() && !LicenseManager.isUnlocked()) return "License required";
        return "";
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean enabled) {
        if (enabled && isLocked()) {
            try {
                NotificationManager.push(name, lockReason());
            } catch (Exception ignored) {}
            return;
        }
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
