package com.example.power_x;

public class Device {
    private String name;
    private int icon;

    public Device(String name, int icon) {
        this.name = name;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public int getIcon() {
        return icon;
    }
}
