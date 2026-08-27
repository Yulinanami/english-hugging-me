package me.englishhugging.desktop.ui;

import javafx.fxml.FXMLLoader;

import java.io.IOException;
import java.net.URL;

/**
 * 桌面端界面文件加载工具。
 *
 * <p>用于加载桌面端界面布局文件，支持直接加载或绑定界面对象。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 1. 直接加载界面
 * Parent root = DesktopUi.loadFxml("/fxml/settings-panel.fxml");
 *
 * // 2. 加载界面并绑定对应的页面对象
 * GeneralSettingsTab tab = new GeneralSettingsTab(settings, store, overlay, () -> {}, () -> {});
 * Node content = DesktopUi.loadFxml("/fxml/general-settings.fxml", tab);
 * </code></pre>
 */
public final class DesktopUi {

    /**
     * 私有构造函数，无需实例化。
     */
    private DesktopUi() {
        // 无需实例化
    }

    /**
     * 加载无需外部依赖的界面文件。
     *
     * @param <T>          返回的根节点类型（如 Parent、Node 等）
     * @param resourcePath 界面资源文件路径（如 "/fxml/settings-panel.fxml"）
     * @return 加载完成的界面根节点
     * @throws IllegalStateException 当资源文件不存在或加载失败时抛出异常
     */
    public static <T> T loadFxml(String resourcePath) {
        try {
            return FXMLLoader.load(getFxmlUrl(resourcePath));
        } catch (IOException exception) {
            throw new IllegalStateException("无法加载 FXML 资源：" + resourcePath, exception);
        }
    }

    /**
     * 使用指定的页面对象来加载界面文件。
     *
     * @param <T>          返回的根节点类型
     * @param resourcePath 界面资源文件路径
     * @param controller   要绑定的页面对象
     * @return 加载并绑定完成的界面根节点
     * @throws IllegalStateException 当资源文件不存在或加载失败时抛出异常
     */
    public static <T> T loadFxml(String resourcePath, Object controller) {
        FXMLLoader loader = new FXMLLoader(getFxmlUrl(resourcePath));
        loader.setControllerFactory(type -> controller);
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("无法加载 FXML 资源：" + resourcePath, exception);
        }
    }

    /**
     * 获取指定资源路径的 URL 对象，并在资源不存在时抛出清晰的异常。
     *
     * @param resourcePath 界面资源文件路径
     * @return 对应的 URL 对象
     */
    private static URL getFxmlUrl(String resourcePath) {
        URL url = DesktopUi.class.getResource(resourcePath);
        if (url == null) {
            throw new IllegalStateException("找不到 FXML 资源文件：" + resourcePath);
        }
        return url;
    }
}
