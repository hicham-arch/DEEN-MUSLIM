package com.example.myapplication;

public class Feature {

    private final String title;
    private final int icon;
    private final int background;

    public Feature(String title, int icon, int background) {
        this.title = title;
        this.icon = icon;
        this.background = background;
    }

    public String getTitle() {
        return title;
    }

    public int getIcon() {
        return icon;
    }

    public int getBackground() {
        return background;
    }
}