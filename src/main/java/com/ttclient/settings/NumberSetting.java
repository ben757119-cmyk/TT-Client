package com.ttclient.settings;

import java.util.Locale;

public class NumberSetting extends Setting<Double> {
    private final double min, max;
    private final double step;

    public NumberSetting(String name, String description, double defaultValue, double min, double max, double step) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step <= 0 ? 0.01 : step;
        set(defaultValue);
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }

    @Override
    public void set(Double value) {
        double v = value == null ? min : value;
        v = Math.max(min, Math.min(max, v));
        v = Math.round(v / step) * step;
        int decimals = decimalPlaces(step);
        double scale = Math.pow(10, decimals);
        v = Math.round(v * scale) / scale;
        v = Math.max(min, Math.min(max, v));
        this.value = v;
    }

    public String getDisplay() {
        double v = get();
        int decimals = decimalPlaces(step);
        if (decimals <= 0) {
            return String.valueOf((long) Math.round(v));
        }
        double scale = Math.pow(10, decimals);
        long scaled = Math.round(v * scale);
        long whole = scaled / (long) scale;
        long frac = Math.abs(scaled % (long) scale);
        String fracStr = String.valueOf(frac);
        while (fracStr.length() < decimals) {
            fracStr = "0" + fracStr;
        }
        return whole + "." + fracStr;
    }

    private static int decimalPlaces(double step) {
        String s = String.format(Locale.ROOT, "%.12f", step).replaceAll("0+$", "");
        int dot = s.indexOf('.');
        if (dot < 0) return 0;
        return Math.min(6, s.length() - dot - 1);
    }

    public int getInt() { return (int) Math.round(get()); }
    public float getFloat() { return get().floatValue(); }
}
