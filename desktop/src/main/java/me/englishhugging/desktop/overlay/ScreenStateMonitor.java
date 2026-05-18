package me.englishhugging.desktop.overlay;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

public final class ScreenStateMonitor {

    private static final int WM_POWERBROADCAST = 0x021B;
    private static final int PBT_APMSUSPEND = 0x0004;
    private static final int PBT_APMRESUMESUSPEND = 0x0007;
    private static final int PBT_APMRESUMEAUTOMATIC = 0x0012;

    private final Runnable onLock;
    private final Runnable onUnlock;
    private volatile Thread listenerThread;
    private volatile WinDef.HWND hwnd;

    public ScreenStateMonitor(Runnable onLock, Runnable onUnlock) {
        this.onLock = onLock;
        this.onUnlock = onUnlock;
    }

    public void start() {
        if (listenerThread != null) return;
        listenerThread = new Thread(this::run, "screen-state-monitor");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void stop() {
        if (hwnd != null) {
            User32.INSTANCE.PostMessage(hwnd, WinUser.WM_QUIT, null, null);
        }
        listenerThread = null;
    }

    private void run() {
        String className = "EHMScreenStateMonitor_" + System.nanoTime();
        WinDef.HMODULE hModule = Kernel32.INSTANCE.GetModuleHandle(null);

        WinUser.WNDCLASSEX wndClass = new WinUser.WNDCLASSEX();
        wndClass.hInstance = hModule;
        wndClass.lpszClassName = className;
        wndClass.lpfnWndProc = (WinUser.WindowProc) (hwnd, uMsg, wParam, lParam) -> {
            if (uMsg == WM_POWERBROADCAST) {
                int powerEvent = wParam.intValue();
                if (powerEvent == PBT_APMSUSPEND && onLock != null) {
                    onLock.run();
                } else if ((powerEvent == PBT_APMRESUMESUSPEND || powerEvent == PBT_APMRESUMEAUTOMATIC) && onUnlock != null) {
                    onUnlock.run();
                }
                return new WinDef.LRESULT(1);
            }
            return User32.INSTANCE.DefWindowProc(hwnd, uMsg, wParam, lParam);
        };
        User32.INSTANCE.RegisterClassEx(wndClass);

        hwnd = User32.INSTANCE.CreateWindowEx(
                0, className, "EHMScreenState", 0,
                0, 0, 0, 0,
                null, null, hModule, null
        );

        if (hwnd == null) return;

        WinUser.MSG msg = new WinUser.MSG();
        while (User32.INSTANCE.GetMessage(msg, null, 0, 0) > 0) {
            User32.INSTANCE.TranslateMessage(msg);
            User32.INSTANCE.DispatchMessage(msg);
        }

        if (hwnd != null) {
            User32.INSTANCE.DestroyWindow(hwnd);
            hwnd = null;
        }
        User32.INSTANCE.UnregisterClass(className, hModule);
    }
}
