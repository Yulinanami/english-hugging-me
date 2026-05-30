package me.englishhugging.desktop.overlay;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Windows 平台 UI 特效深度挂载器。
 *
 * <p>原生 JavaFX 无法实现“点击穿透”以及“彻底从任务栏隐藏”。
 * 这个工具类通过 JNA 调用 User32.dll，直接修改 JavaFX 窗口在操作系统级别的
 * 扩展样式属性（EXSTYLE），强制为其打上 {@code WS_EX_TRANSPARENT}（鼠标穿透）
 * 和 {@code WS_EX_TOOLWINDOW}（隐藏任务栏）标签。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 让悬浮窗对鼠标点击完全免疫，变成一层幽灵贴图
 * WindowsClickThrough.apply(overlayStage, true);
 * 
 * // 彻底从 Alt-Tab 和任务栏中抹除悬浮窗的痕迹
 * WindowsClickThrough.hideFromTaskbar(overlayStage);
 * </code></pre>
 */
public final class WindowsClickThrough {

    // --- Windows API 常量 ---
    private static final int GWL_EXSTYLE = -20;
    
    // 鼠标穿透核心标签
    private static final int WS_EX_TRANSPARENT = 0x00000020;
    
    // 工具窗口标签，会使其从 Alt-Tab 和任务栏隐藏
    private static final int WS_EX_TOOLWINDOW = 0x00000080;
    private static final int WS_EX_APPWINDOW = 0x00040000;
    
    // 分层窗口标签，是透明度特效的基础
    private static final int WS_EX_LAYERED = 0x00080000;
    
    // 窗口位置重绘标志位
    private static final int SWP_NOSIZE = 0x0001;
    private static final int SWP_NOMOVE = 0x0002;
    private static final int SWP_NOZORDER = 0x0004;
    private static final int SWP_FRAMECHANGED = 0x0020;
    
    // 系统托盘溢出菜单的特殊类名
    private static final String NOTIFY_ICON_OVERFLOW_WINDOW = "NotifyIconOverflowWindow";

    /**
     * 阻止工具类被实例化。
     */
    private WindowsClickThrough() {
        // 无需实例化
    }

    /**
     * 为 JavaFX 舞台热切换“鼠标穿透”状态。
     * 所有的 JNA 操作都被强制推送到 JavaFX UI 线程以防止并发闪退。
     *
     * @param stage        目标舞台
     * @param clickThrough true 表示鼠标穿透，false 表示可以点击交互
     */
    public static void apply(Stage stage, boolean clickThrough) {
        if (!com.sun.jna.Platform.isWindows()) {
            return;
        }
        
        Platform.runLater(() -> applyNow(stage, clickThrough));
    }

    /**
     * 扒掉目标窗口的普通应用外衣，强行替换为工具窗口（Tool Window），
     * 从而使得用户无法在任务栏找到它的踪迹。
     *
     * @param stage 目标舞台
     */
    public static void hideFromTaskbar(Stage stage) {
        if (!com.sun.jna.Platform.isWindows()) {
            return;
        }
        
        Platform.runLater(() -> {
            HWND hwnd = User32.INSTANCE.FindWindow(null, stage.getTitle());
            if (hwnd == null) {
                return;
            }
            
            int style = User32.INSTANCE.GetWindowLong(hwnd, GWL_EXSTYLE);
            // 剥夺 APPWINDOW，赋予 TOOLWINDOW
            int updated = (style | WS_EX_TOOLWINDOW) & ~WS_EX_APPWINDOW;
            
            User32.INSTANCE.SetWindowLong(hwnd, GWL_EXSTYLE, updated);
            
            // 强制刷新系统对该窗口样式的缓存，否则任务栏图标不会立刻消失
            User32.INSTANCE.SetWindowPos(hwnd, null, 0, 0, 0, 0,
                    SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED);
        });
    }

    /**
     * 探测当前 Windows 系统的折叠托盘（就是那个“^”符号点开的溢出面板）是否处于展开状态。
     *
     * @return 如果正在显示则返回 true
     */
    public static boolean isNotifyIconOverflowVisible() {
        if (!com.sun.jna.Platform.isWindows()) {
            return false;
        }
        
        HWND hwnd = User32.INSTANCE.FindWindow(NOTIFY_ICON_OVERFLOW_WINDOW, null);
        
        if (hwnd != null) {
            return User32.INSTANCE.IsWindowVisible(hwnd);
        } else {
            return false;
        }
    }

    /**
     * 内部真实的穿透挂载逻辑。
     */
    private static void applyNow(Stage stage, boolean clickThrough) {
        HWND hwnd = User32.INSTANCE.FindWindow(null, stage.getTitle());
        if (hwnd == null) {
            return;
        }
        
        int style = User32.INSTANCE.GetWindowLong(hwnd, GWL_EXSTYLE);
        int updated;
        
        if (clickThrough) {
            // 加入鼠标穿透和图层标签
            updated = style | WS_EX_TRANSPARENT | WS_EX_LAYERED;
        } else {
            // 剔除穿透标签，恢复正常拦截点击
            updated = style & ~WS_EX_TRANSPARENT;
        }
        
        User32.INSTANCE.SetWindowLong(hwnd, GWL_EXSTYLE, updated);
    }
}
