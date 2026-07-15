package me.englishhugging.desktop.ui;

import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;

import java.io.IOException;
import java.net.URL;

/**
 * 桌面端 FXML 与 CSS 资源加载器。
 *
 * <p>设置页面的节点结构由 {@code /fxml} 下的资源声明，颜色、圆角和间距由
 * {@code /styles/desktop.css} 管理；Java 只保留资源加载和控制器注入。</p>
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
     * 使用已有控制器实例加载 FXML，使控制器可以继续通过构造函数接收业务依赖。
     *
     * @param resourcePath classpath 中的 FXML 绝对资源路径
     * @param controller   已完成依赖注入的控制器实例
     * @return FXML 声明的根节点
     */
    public static <T> T loadFxml(String resourcePath, Object controller) {
        URL resource = DesktopUi.class.getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("找不到 FXML 资源：" + resourcePath);
        }

        FXMLLoader loader = new FXMLLoader(resource);
        loader.setController(controller);
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("无法加载 FXML 资源：" + resourcePath, exception);
        }
    }

}
