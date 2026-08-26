package me.englishhugging.core.settings;

/**
 * 悬浮窗的交互模式。
 * 
 * <p>控制悬浮窗口是否响应交互事件（可拖拽移动/调整大小，或点击穿透不遮挡背景操作）。
 */
public enum OverlayMode {
    
    /**
     * 可拖拽模式。
     * 悬浮窗会显示移动与缩放把手，用户可以自由拖动位置或调整宽高。
     */
    DRAGGABLE("可拖拽"),
    
    /**
     * 点击穿透模式。
     * 悬浮窗完全无视触摸与点击事件，直接作用于悬浮窗下方的其他应用程序。
     */
    CLICK_THROUGH("点击穿透");

    /**
     * 下拉框中显示的中文名称
     */
    private final String label;

    /**
     * 初始化交互模式。
     *
     * @param label 下拉框中显示的中文名称
     */
    OverlayMode(String label) {
        this.label = label;
    }

    /**
     * 返回下拉框中显示的中文名称。
     *
     * @return 中文标签字符串
     */
    @Override
    public String toString() {
        return this.label;
    }

    /**
     * 获取所有交互模式的中文名称数组。
     *
     * @return 包含所有交互模式中文名称的字符串数组
     */
    public static String[] labels() {
        OverlayMode[] values = values();
        String[] labelsList = new String[values.length];
        
        for (int i = 0; i < values.length; i++) {
            labelsList[i] = values[i].label;
        }
        
        return labelsList;
    }
}
