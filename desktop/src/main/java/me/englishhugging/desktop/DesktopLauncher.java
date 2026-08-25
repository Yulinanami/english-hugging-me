package me.englishhugging.desktop;

import javafx.application.Application;

/**
 * 桌面端主程序的启动入口类。
 *
 * <p>这个类包含标准的 `main` 方法，用于引导启动 JavaFX 桌面应用程序。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 在命令行或 IDE 中直接运行 main 方法启动
 * DesktopLauncher.main(new String[]{});
 * </code></pre>
 */
public final class DesktopLauncher {
    /**
     * 私有构造函数，无需实例化。
     */
    private DesktopLauncher() {
        // 无需实例化
    }

    /**
     * 桌面应用主函数入口。
     *
     * @param args 命令行启动参数
     */
    public static void main(String[] args) {
        Application.launch(FloatingWordsDesktopApp.class, args);
    }
}
