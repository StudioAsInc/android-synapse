package com.synapse.social.studioasinc.model;

import androidx.annotation.DrawableRes;

public class Setting {

    public enum Type {
        HEADER,
        SWITCH,
        SEEKBAR,
        BUTTON,
        PREVIEW
    }

    private final Type type;
    private String title;
    private String summary;
    private @DrawableRes int iconRes;
    private boolean isEnabled;
    private int value;
    private int minValue;
    private int maxValue;
    private String key;

    // Constructor for header
    public Setting(String title) {
        this.type = Type.HEADER;
        this.title = title;
    }

    // Constructor for switch
    public Setting(Type type, String title, String summary, @DrawableRes int iconRes, boolean isEnabled, String key) {
        this.type = type;
        this.title = title;
        this.summary = summary;
        this.iconRes = iconRes;
        this.isEnabled = isEnabled;
        this.key = key;
    }

    // Constructor for seekbar
    public Setting(Type type, String title, String summary, @DrawableRes int iconRes, int value, int minValue, int maxValue, String key) {
        this.type = type;
        this.title = title;
        this.summary = summary;
        this.iconRes = iconRes;
        this.value = value;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.key = key;
    }

    // Constructor for button
    public Setting(Type type, String title, String summary, @DrawableRes int iconRes, String key) {
        this.type = type;
        this.title = title;
        this.summary = summary;
        this.iconRes = iconRes;
        this.key = key;
    }

    // Constructor for preview
    public Setting(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public int getIconRes() {
        return iconRes;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public String getKey() {
        return key;
    }
}
