package me.englishhugging.core.settings;

/**
 * 桌面悬浮窗的交互模式枚举。
 * 
 * <p>用于控制桌面悬浮窗口是否可以响应鼠标事件（如拖拽移动、调整大小等）。
 * 如果设置为穿透模式，则鼠标点击会直接穿过悬浮窗，不影响用户的背景工作。
 * 
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 当用户希望悬浮窗安静地呆在屏幕角落且不遮挡鼠标点击时：
 * appSettings.setOverlayMode(OverlayMode.CLICK_THROUGH);
 * </code></pre>
 */
public enum OverlayMode {
    
    /**
     * 可拖拽模式。
     * 悬浮窗会显示控制手柄，用户可以自由拖动其位置或修改其宽度和高度。
     */
    DRAGGABLE("可拖拽"),
    
    /**
     * 鼠标穿透模式。
     * 悬浮窗完全无视鼠标事件，鼠标点击直接作用于悬浮窗下方的其他应用程序。
     */
    CLICK_THROUGH("鼠标穿透");

    /**
     * 在 UI 设置界面中展示给用户的中文标签。
     */
    private final String label;

    /**
     * 构造一个新的交互模式枚举值。
     *
     * @param label 用于显示的中文名称
     */
    OverlayMode(String label) {
        this.label = label;
    }

    /**
     * 获取供 UI 界面展示的中文描述。
     *
     * @return 中文标签字符串
     */
    public String getLabel() {
        return this.label;
    }

    /**
     * 获取所有的可用交互模式中文标签，常用于填充 UI 下拉列表。
     *
     * @return 包含所有标签的字符串数组
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
