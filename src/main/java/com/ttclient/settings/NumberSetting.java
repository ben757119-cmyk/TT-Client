package com.ttclient.settings;

public class NumberSetting extends Setting<Double> {
    private final double min, max;
    private final double step;

    public NumberSetting(String name, String description, double defaultValue, double min, double max, double step) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }

    @Override
    public void set(Double value) {
        this.value = Math.max(min, Math.min(max, value));
    }

    public int getInt() { return get().intValue(); }
    public float getFloat() { return get().floatValue(); }
}
