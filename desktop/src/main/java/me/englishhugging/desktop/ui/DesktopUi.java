package me.englishhugging.desktop.ui;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

import java.net.URL;

/**
 * 桌面端的 JavaFX UI 组件工厂。
 *
 * <p>这个类集中管理了所有在设置面板中反复出现的基础控件（如按钮、输入框、表单容器）。
 * 具体的颜色、圆角、阴影定义在 {@code /styles/desktop.css} 样式表中，
 * 工厂方法只负责挂 styleClass；宿主 Scene 必须先调用 {@link #applyStylesheet(Scene)}，
 * 否则这些 class 不会生效。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * Scene scene = new Scene(root);
 * DesktopUi.applyStylesheet(scene);
 *
 * // 在设置面板中快速生成一个带阴影的组合卡片
 * VBox card = DesktopUi.groupBox("词库设置", innerContentNode);
 *
 * // 获取一个预设了悬浮色和圆角的次级按钮
 * Button btn = DesktopUi.compactButton("清空记录");
 * </code></pre>
 */
public final class DesktopUi {

    /** 静态样式表的 classpath 资源路径 */
    private static final String STYLESHEET_RESOURCE = "/styles/desktop.css";

    /**
     * 阻止工具类被实例化。
     */
    private DesktopUi() {
        // 无需实例化
    }

    /**
     * 将本工厂配套的样式表挂载到 Scene 上。
     * 每个承载 DesktopUi 控件的 Scene 都需要调用一次。
     *
     * @param scene 目标场景
     */
    public static void applyStylesheet(Scene scene) {
        URL url = DesktopUi.class.getResource(STYLESHEET_RESOURCE);
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        }
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
        button.getStyleClass().add("compact-button");
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
        label.getStyleClass().add("group-box-title");

        VBox box = new VBox(8, label, content);
        box.setPadding(new Insets(12));
        box.getStyleClass().add("group-box");

        return box;
    }

    /**
     * 提供一个通用的组件着色器，给任意节点挂上白底圆角描边的通用样式。
     *
     * @param node 任意待着色的 JavaFX 节点
     */
    public static void styleModernControl(Node node) {
        node.getStyleClass().add("modern-control");
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
