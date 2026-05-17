package me.englishhugging.core.settings;

public enum OverlayMode {
    DRAGGABLE("可拖动"),
    LOCKED("锁定位置"),
    CLICK_THROUGH("点击穿透");

    private final String label;

    OverlayMode(String label) { this.label = label; }

    public String getLabel() { return label; }

    public static String[] labels() {
        OverlayMode[] values = values();
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            labels[i] = values[i].label;
        }
        return labels;
    }
}
