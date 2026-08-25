package me.englishhugging.desktop.overlay;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

/**
 * Windows 平台息屏与锁屏状态监控工具。
 *
 * <p>监控 Windows 系统的电源和锁屏事件，在用户锁屏、休眠或息屏时自动暂停播放，亮屏解锁后自动恢复。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * ScreenStateMonitor monitor = new ScreenStateMonitor(
 *     () -> scheduler.pause(),
 *     () -> scheduler.resume()
 * );
 * monitor.start();
 * </code></pre>
 */
public final class ScreenStateMonitor {

    /** Windows 电源广播消息标识 */
    private static final int WM_POWERBROADCAST = 0x021B;

    /** 系统进入睡眠或待机状态事件 */
    private static final int PBT_APMSUSPEND = 0x0004;

    /** 系统被唤醒事件（由用户操作触发） */
    private static final int PBT_APMRESUMESUSPEND = 0x0007;

    /** 系统自动恢复运行事件 */
    private static final int PBT_APMRESUMEAUTOMATIC = 0x0012;

    /** 锁屏或休眠时触发的回调 */
    private final Runnable onLock;
    
    /** 解锁或恢复时触发的回调 */
    private final Runnable onUnlock;
    
    /** 专门用于运行 Windows 消息循环的后台线程 */
    private volatile Thread listenerThread;
    
    /** 隐藏消息窗口对象 */
    private volatile WinDef.HWND hwnd;

    /**
     * 初始化屏幕电源状态监听。
     *
     * @param onLock   系统息屏或锁定时的回调
     * @param onUnlock 系统亮屏或解锁时的回调
     */
    public ScreenStateMonitor(Runnable onLock, Runnable onUnlock) {
        this.onLock = onLock;
        this.onUnlock = onUnlock;
    }

    /**
     * 启动 Windows 消息监听。
     */
    public void start() {
        if (this.listenerThread != null) {
            return;
        }
        
        this.listenerThread = new Thread(this::run, "screen-state-monitor");
        this.listenerThread.setDaemon(true);
        this.listenerThread.start();
    }

    /**
     * 停止监听并释放资源。
     */
    public void stop() {
        if (this.hwnd != null) {
            User32.INSTANCE.PostMessage(this.hwnd, WinUser.WM_QUIT, null, null);
        }
        this.listenerThread = null;
    }

    /**
     * 创建隐藏窗口并运行 Windows 消息循环，用于监听系统电源事件。
     */
    private void run() {
        String className = "EHMScreenStateMonitor_" + System.nanoTime();
        WinDef.HMODULE hModule = Kernel32.INSTANCE.GetModuleHandle(null);

        WinUser.WNDCLASSEX wndClass = new WinUser.WNDCLASSEX();
        wndClass.hInstance = hModule;
        wndClass.lpszClassName = className;
        
        // 监听 Windows 系统消息
        wndClass.lpfnWndProc = (WinUser.WindowProc) (hwndProc, uMsg, wParam, lParam) -> {
            if (uMsg == WM_POWERBROADCAST) {
                int powerEvent = wParam.intValue();
                
                // 收到休眠或锁屏信号
                if (powerEvent == PBT_APMSUSPEND) {
                    if (this.onLock != null) {
                        this.onLock.run();
                    }
                } 
                // 收到恢复信号
                else if (powerEvent == PBT_APMRESUMESUSPEND || powerEvent == PBT_APMRESUMEAUTOMATIC) {
                    if (this.onUnlock != null) {
                        this.onUnlock.run();
                    }
                }
                
                // 返回 1 表示我们已经处理了该广播消息
                return new WinDef.LRESULT(1);
            }
            // 其它非电源消息，由系统默认处理
            return User32.INSTANCE.DefWindowProc(hwndProc, uMsg, wParam, lParam);
        };
        
        User32.INSTANCE.RegisterClassEx(wndClass);

        // 创建不可见窗口
        this.hwnd = User32.INSTANCE.CreateWindowEx(
                0, className, "EHMScreenState", 0,
                0, 0, 0, 0,
                null, null, hModule, null
        );

        if (this.hwnd == null) {
            return;
        }

        // 持续处理 Windows 系统消息，直到收到退出信号
        WinUser.MSG msg = new WinUser.MSG();
        while (User32.INSTANCE.GetMessage(msg, null, 0, 0) > 0) {
            User32.INSTANCE.TranslateMessage(msg);
            User32.INSTANCE.DispatchMessage(msg);
        }

        // 退出循环后执行清理操作
        if (this.hwnd != null) {
            User32.INSTANCE.DestroyWindow(this.hwnd);
            this.hwnd = null;
        }
        
        User32.INSTANCE.UnregisterClass(className, hModule);
    }
}
