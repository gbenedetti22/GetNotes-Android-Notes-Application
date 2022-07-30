package com.unipi.sam.getnotes.home;

public class HomeIcon {
    public static final int TYPE_FOLDER = 0;
    public static final int TYPE_NOTE = 1;
    private int type;
    private String label;

    public HomeIcon(int type, String label) {
        this.type = type;
        this.label = label;
    }

    public int getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }
}
