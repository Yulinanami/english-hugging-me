package me.englishhugging.desktop.ui;

import javafx.fxml.FXMLLoader;

import java.io.IOException;

/**
 * 桌面端 FXML 资源加载器。
 *
 * <p>视图结构与样式完全由 FXML 与 CSS 声明；Java 端仅提供标准 FXML 加载封装。</p>
 */
public final class DesktopUi {

    /**
     * 阻止工具类被实例化。
     */
    private DesktopUi() {
        // 无需实例化
    }

    /**
     * 加载无独立控制器的 FXML 视图（直接调用 JavaFX 官方静态加载器）。
     *
     * @param resourcePath classpath 中的 FXML 资源路径
     * @return FXML 根节点
     */
    public static <T> T loadFxml(String resourcePath) {
        try {
            return FXMLLoader.load(DesktopUi.class.getResource(resourcePath));
        } catch (IOException exception) {
            throw new IllegalStateException("无法加载 FXML 资源：" + resourcePath, exception);
        }
    }

    /**
     * 使用已有控制器实例加载 FXML。
     *
     * @param resourcePath classpath 中的 FXML 资源路径
     * @param controller   控制器实例
     * @return FXML 根节点
     */
    public static <T> T loadFxml(String resourcePath, Object controller) {
        FXMLLoader loader = new FXMLLoader(DesktopUi.class.getResource(resourcePath));
        loader.setController(controller);
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("无法加载 FXML 资源：" + resourcePath, exception);
        }
    }
}
