package me.englishhugging.desktop.overlay;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Windows 平台鼠标穿透与任务栏隐藏工具。
 *
 * <p>这个类通过 Windows 原生系统接口（User32.dll），让桌面悬浮窗支持“鼠标点击穿透”
 * （鼠标点击直接穿透到下层软件，不挡住正常操作），并从 Windows 任务栏中隐藏。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 设置悬浮窗鼠标穿透
 * WindowsClickThrough.apply(overlayStage, true);
 * 
 * // 从任务栏隐藏悬浮窗
 * WindowsClickThrough.hideFromTaskbar(overlayStage);
 * </code></pre>
 */
public final class WindowsClickThrough {

    /** 获取窗口扩展样式的标识 */
    private static final int GWL_EXSTYLE = -20;
    
    /** 鼠标穿透样式标志 */
    private static final int WS_EX_TRANSPARENT = 0x00000020;
    
    /** 工具窗口标志（使窗口不显示在任务栏和 Alt-Tab 中） */
    private static final int WS_EX_TOOLWINDOW = 0x00000080;
    /** 普通应用程序窗口标志 */
    private static final int WS_EX_APPWINDOW = 0x00040000;
    
    /** 分层窗口样式标志（透明窗口基础） */
    private static final int WS_EX_LAYERED = 0x00080000;
    
    /** 保持窗口当前尺寸不变 */
    private static final int SWP_NOSIZE = 0x0001;
    /** 保持窗口当前位置不变 */
    private static final int SWP_NOMOVE = 0x0002;
    /** 保持窗口 Z 轴层级不变 */
    private static final int SWP_NOZORDER = 0x0004;
    /** 通知系统立即刷新窗口框架样式 */
    private static final int SWP_FRAMECHANGED = 0x0020;
    
    /** Windows 任务栏折叠托盘窗口的类名 */
    private static final String NOTIFY_ICON_OVERFLOW_WINDOW = "NotifyIconOverflowWindow";

    /**
     * 私有构造函数，无需实例化。
     */
    private WindowsClickThrough() {
        // 无需实例化
    }

    /**
     * 设置窗口是否支持鼠标点击穿透。
     *
     * @param stage        目标窗口
     * @param clickThrough true 表示鼠标穿透，false 表示可以点击交互
     */
    public static void apply(Stage stage, boolean clickThrough) {
        if (!com.sun.jna.Platform.isWindows()) {
            return;
        }
        
        Platform.runLater(() -> applyNow(stage, clickThrough));
    }

    /**
     * 将目标窗口从 Windows 任务栏和 Alt-Tab 列表中隐藏。
     *
     * @param stage 目标窗口
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
            // 设置为工具窗口样式，使任务栏不显示图标
            int updated = (style | WS_EX_TOOLWINDOW) & ~WS_EX_APPWINDOW;
            
            User32.INSTANCE.SetWindowLong(hwnd, GWL_EXSTYLE, updated);
            
            // 刷新窗口样式
            User32.INSTANCE.SetWindowPos(hwnd, null, 0, 0, 0, 0,
                    SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED);
        });
    }

    /**
     * 检查 Windows 系统的托盘折叠菜单（"^" 菜单）是否正处于展开状态。
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
     * 实际设置窗口样式为穿透或正常交互。
     */
    private static void applyNow(Stage stage, boolean clickThrough) {
        HWND hwnd = User32.INSTANCE.FindWindow(null, stage.getTitle());
        if (hwnd == null) {
            return;
        }
        
        int style = User32.INSTANCE.GetWindowLong(hwnd, GWL_EXSTYLE);
        int updated;
        
        if (clickThrough) {
            // 添加鼠标穿透和分层窗口样式
            updated = style | WS_EX_TRANSPARENT | WS_EX_LAYERED;
        } else {
            // 移除鼠标穿透样式，恢复正常可点击状态
            updated = style & ~WS_EX_TRANSPARENT;
        }
        
        User32.INSTANCE.SetWindowLong(hwnd, GWL_EXSTYLE, updated);
    }
}
