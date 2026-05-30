package me.englishhugging.desktop.overlay;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

/**
 * Windows 平台息屏/锁屏状态监控器。
 *
 * <p>这个类通过 JNA 深度集成了 Windows 底层的电源和会话事件广播（WM_POWERBROADCAST）。
 * 它的核心目的是在用户锁屏、电脑休眠或关闭显示器时，自动挂起后台的背单词轮播调度器；
 * 并在用户重新亮屏解锁时，无缝恢复播放。这样可以极大地节省系统资源并避免浪费学习进度。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * ScreenStateMonitor monitor = new ScreenStateMonitor(
 *     () -> scheduler.pause(),
 *     () -> scheduler.resume()
 * );
 * monitor.start();
 * 
 * // 退出应用前销毁
 * monitor.stop();
 * </code></pre>
 */
public final class ScreenStateMonitor {

    // --- Windows API 常量 ---
    private static final int WM_POWERBROADCAST = 0x021B;
    private static final int PBT_APMSUSPEND = 0x0004;
    private static final int PBT_APMRESUMESUSPEND = 0x0007;
    private static final int PBT_APMRESUMEAUTOMATIC = 0x0012;

    /** 锁屏或休眠时触发的回调 */
    private final Runnable onLock;
    
    /** 解锁或恢复时触发的回调 */
    private final Runnable onUnlock;
    
    /** 专门用于挂起 Windows 消息循环的守护线程 */
    private volatile Thread listenerThread;
    
    /** 我们注册在系统中的隐藏消息接收窗口句柄 */
    private volatile WinDef.HWND hwnd;

    /**
     * 构造屏幕状态监控器。
     *
     * @param onLock   当系统挂起、息屏或锁定时执行的操作
     * @param onUnlock 当系统恢复、亮屏或解锁时执行的操作
     */
    public ScreenStateMonitor(Runnable onLock, Runnable onUnlock) {
        this.onLock = onLock;
        this.onUnlock = onUnlock;
    }

    /**
     * 启动底层的 Windows 消息循环监听。
     * 保证只会启动一次，避免线程泄漏。
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
     * 停止监听并向底层的消息循环发送退出信号。
     */
    public void stop() {
        if (this.hwnd != null) {
            User32.INSTANCE.PostMessage(this.hwnd, WinUser.WM_QUIT, null, null);
        }
        this.listenerThread = null;
    }

    /**
     * 独立线程的运行实体，它在系统深处构建了一个不可见的幽灵窗口，
     * 专门用于拦截并消化操作系统广播出来的电源事件。
     */
    private void run() {
        String className = "EHMScreenStateMonitor_" + System.nanoTime();
        WinDef.HMODULE hModule = Kernel32.INSTANCE.GetModuleHandle(null);

        WinUser.WNDCLASSEX wndClass = new WinUser.WNDCLASSEX();
        wndClass.hInstance = hModule;
        wndClass.lpszClassName = className;
        
        // 核心回调：拦截 Windows 系统消息
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
            // 其它非电源消息，原样丢回给系统默认处理器
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

        // 死循环阻塞：不断抽取 Windows 消息直到收到 WM_QUIT
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
