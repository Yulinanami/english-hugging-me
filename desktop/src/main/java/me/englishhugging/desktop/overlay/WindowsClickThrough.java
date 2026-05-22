package me.englishhugging.desktop.overlay;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import javafx.application.Platform;
import javafx.stage.Stage;

public final class WindowsClickThrough {
    private static final int GWL_EXSTYLE = -20;
    private static final int WS_EX_TRANSPARENT = 0x00000020;
    private static final int WS_EX_TOOLWINDOW = 0x00000080;
    private static final int WS_EX_APPWINDOW = 0x00040000;
    private static final int WS_EX_LAYERED = 0x00080000;
    private static final int SWP_NOSIZE = 0x0001;
    private static final int SWP_NOMOVE = 0x0002;
    private static final int SWP_NOZORDER = 0x0004;
    private static final int SWP_FRAMECHANGED = 0x0020;
    private static final String NOTIFY_ICON_OVERFLOW_WINDOW = "NotifyIconOverflowWindow";

    private WindowsClickThrough() {
    }

    public static void apply(Stage stage, boolean clickThrough) {
        if (!com.sun.jna.Platform.isWindows())
            return;
        Platform.runLater(() -> applyNow(stage, clickThrough));
    }

    public static void hideFromTaskbar(Stage stage) {
        if (!com.sun.jna.Platform.isWindows()) return;
        Platform.runLater(() -> {
            HWND hwnd = User32.INSTANCE.FindWindow(null, stage.getTitle());
            if (hwnd == null) return;
            int style = User32.INSTANCE.GetWindowLong(hwnd, GWL_EXSTYLE);
            int updated = (style | WS_EX_TOOLWINDOW) & ~WS_EX_APPWINDOW;
            User32.INSTANCE.SetWindowLong(hwnd, GWL_EXSTYLE, updated);
            User32.INSTANCE.SetWindowPos(hwnd, null, 0, 0, 0, 0,
                    SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED);
        });
    }

    public static boolean isNotifyIconOverflowVisible() {
        if (!com.sun.jna.Platform.isWindows())
            return false;
        HWND hwnd = User32.INSTANCE.FindWindow(NOTIFY_ICON_OVERFLOW_WINDOW, null);
        return hwnd != null && User32.INSTANCE.IsWindowVisible(hwnd);
    }

    private static void applyNow(Stage stage, boolean clickThrough) {
        HWND hwnd = User32.INSTANCE.FindWindow(null, stage.getTitle());
        if (hwnd == null)
            return;
        int style = User32.INSTANCE.GetWindowLong(hwnd, GWL_EXSTYLE);
        int updated = clickThrough
                ? style | WS_EX_TRANSPARENT | WS_EX_LAYERED
                : style & ~WS_EX_TRANSPARENT;
        User32.INSTANCE.SetWindowLong(hwnd, GWL_EXSTYLE, updated);
    }
}
