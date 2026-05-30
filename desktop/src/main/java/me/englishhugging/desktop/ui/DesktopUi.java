package me.englishhugging.desktop.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * 桌面端的 JavaFX  UI 组件工厂。
 *
 * <p>这个类集中管理了所有在设置面板中反复出现的基础控件（如按钮、输入框、表单容器）。
 * 它负责通过硬编码 CSS 注入圆角、阴影和现代的扁平化配色。
 * 集中在此处定义可以确保整个软件具备高度统一的视觉风格。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 在设置面板中快速生成一个具有苹果风格阴影的组合卡片
 * VBox card = DesktopUi.groupBox("词库设置", innerContentNode);
 * 
 * // 获取一个预设了悬浮色和圆角的次级按钮
 * Button btn = DesktopUi.compactButton("清空记录");
 * </code></pre>
 */
public final class DesktopUi {

    /**
     * 阻止工具类被实例化。
     */
    private DesktopUi() {
        // 无需实例化
    }

    /**
     * 生成一个标准间距的网格表单容器，常用于两列对齐的设置面板。
     *
     * @return 配置好间距的 {@link GridPane}
     */
    public static GridPane settingsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        return grid;
    }

    /**
     * 生成一个紧凑型的现代感按钮，带有淡蓝色背景和圆角边界。
     *
     * @param text 按钮上显示的文本
     * @return 预置样式的 {@link Button}
     */
    public static Button compactButton(String text) {
        Button button = new Button(text);
        button.setMinHeight(32);
        button.setPrefHeight(32);
        
        String modernStyle = "-fx-background-color: #EEF2FF; -fx-text-fill: #52699A; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #D8E0F3; -fx-padding: 5 14 5 14;";
        button.setStyle(modernStyle);
        
        return button;
    }

    /**
     * 生成一个固定宽高的紧凑型输入框，常用于需要修改数值的表单项。
     *
     * @return 预置样式的 {@link TextField}
     */
    public static TextField compactTextField() {
        TextField textField = new TextField();
        textField.setPrefWidth(330);
        textField.setPrefHeight(32);
        
        styleModernControl(textField);
        
        return textField;
    }

    /**
     * 核心组件：生成一个类似于 iOS 设置面板中的大圆角白色带阴影分组块。
     * 它通过向子节点外包一层带样式的 {@link VBox} 来实现边框和高亮效果。
     *
     * @param title   分组标题文本
     * @param content 具体的内部表单节点
     * @return 复合的容器节点
     */
    public static VBox groupBox(String title, Node content) {
        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #52699A; -fx-font-weight: bold; -fx-padding: 0 0 2 0;");
        
        VBox box = new VBox(8, label, content);
        box.setPadding(new Insets(12));
        
        String cardStyle = "-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #E4E8F2; -fx-border-radius: 14; -fx-effect: dropshadow(gaussian, rgba(15,23,42,0.06), 12, 0, 0, 3);";
        box.setStyle(cardStyle);
        
        return box;
    }

    /**
     * 提供一个通用的组件着色器，给任意节点注入一套基础的白底圆角描边 CSS。
     *
     * @param node 任意待着色的 JavaFX 节点
     */
    public static void styleModernControl(Node node) {
        String baseStyle = "-fx-background-color: white; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #D8E0F3; -fx-padding: 3 8 3 8;";
        node.setStyle(baseStyle);
    }

    /**
     * 辅助工厂：快速生成一个不可被关闭的 Tab 页签，供主设置面板调用。
     *
     * @param title   页签顶部的显示名
     * @param content 该页签对应的庞大内部节点
     * @return 设置好的 {@link Tab} 对象
     */
    public static Tab settingsTab(String title, Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }
}
