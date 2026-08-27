package me.englishhugging.android.overlay;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.function.Supplier;

import me.englishhugging.android.settings.AndroidSettingsStore;
import me.englishhugging.core.WordScheduler;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.core.settings.OverlayMode;

/**
 * 悬浮窗触摸手势与拖拽/缩放交互处理组件。
 *
 * <p>负责处理主悬浮窗平移拖拽、内置把手缩放以及外部把手缩放手势，
 * 封装 TouchSlop 防误触机制以及定时播放的暂停与恢复操作。
 */
public final class OverlayTouchHandler {

    /** 上下文环境对象 */
    private final Context context;
    /** 主悬浮窗窗口管理组件 */
    private final OverlayWindowManager windowManagerHelper;
    /** 调整把手管理组件 */
    private final OverlayResizeHandleManager resizeHandleManager;
    /** 获取当前单词播放控制对象的提供者函数 */
    private final Supplier<WordScheduler> schedulerSupplier;
    /** 获取当前应用配置的提供者函数 */
    private final Supplier<AppSettings> settingsSupplier;

    /** 区分点击与拖拽的防误触最小滑动距离（像素 px） */
    private final int touchSlop;
    /** 标记当前是否正在拖拽平移悬浮窗 */
    private boolean isDragging = false;
    /** 标记当前是否正在拖拽把手调整悬浮窗大小 */
    private boolean isResizing = false;

    /** 拖拽平移时窗口初始 X 坐标（像素 px） */
    private int initialX;
    /** 拖拽平移时窗口初始 Y 坐标（像素 px） */
    private int initialY;
    /** 拖拽平移时手指按下起始 X 坐标（像素 px） */
    private float initialTouchX;
    /** 拖拽平移时手指按下起始 Y 坐标（像素 px） */
    private float initialTouchY;

    /** 调整大小时窗口初始宽度（像素 px） */
    private int initialWidth;
    /** 调整大小时窗口初始高度（像素 px） */
    private int initialHeight;
    /** 调整大小时手指按下起始 X 坐标（像素 px） */
    private float initialResizeTouchX;
    /** 调整大小时手指按下起始 Y 坐标（像素 px） */
    private float initialResizeTouchY;

    public OverlayTouchHandler(
            Context context,
            OverlayWindowManager windowManagerHelper,
            OverlayResizeHandleManager resizeHandleManager,
            Supplier<WordScheduler> schedulerSupplier,
            Supplier<AppSettings> settingsSupplier
    ) {
        this.context = context;
        this.windowManagerHelper = windowManagerHelper;
        this.resizeHandleManager = resizeHandleManager;
        this.schedulerSupplier = schedulerSupplier;
        this.settingsSupplier = settingsSupplier;
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    /**
     * 响应可拖拽模式下的手指拖拽平移事件。
     */
    public boolean onOverlayTouch(View view, MotionEvent event) {
        AppSettings settings = this.settingsSupplier.get();
        if (settings == null || settings.getOverlayMode() != OverlayMode.DRAGGABLE) {
            return false;
        }

        WindowManager.LayoutParams layoutParams = this.windowManagerHelper.getLayoutParams();
        if (layoutParams == null) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.isDragging = false;
                this.initialX = layoutParams.x;
                this.initialY = layoutParams.y;
                this.initialTouchX = event.getRawX();
                this.initialTouchY = event.getRawY();
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - this.initialTouchX;
                float dy = event.getRawY() - this.initialTouchY;

                // 只有移动距离突破防误触阈值后，才真正进入拖拽状态并暂停播放
                if (!this.isDragging && Math.hypot(dx, dy) > this.touchSlop) {
                    this.isDragging = true;
                    WordScheduler scheduler = this.schedulerSupplier.get();
                    if (scheduler != null) {
                        scheduler.pause();
                    }
                }

                if (this.isDragging) {
                    int newX = this.initialX + (int) dx;
                    int newY = this.initialY + (int) dy;
                    settings.setX(newX);
                    settings.setY(newY);
                    this.windowManagerHelper.updatePosition(newX, newY);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (this.isDragging) {
                    this.isDragging = false;
                    AndroidSettingsStore.save(this.context, settings);
                    WordScheduler scheduler = this.schedulerSupplier.get();
                    if (scheduler != null) {
                        // 继承剩余倒计时，并提供 1.5 秒保底防闪跳
                        scheduler.resumeWithRemaining(1500);
                    }
                }
                return true;
            default:
                return true;
        }
    }

    /**
     * 响应可拖拽模式下内置把手的手指拖拽，调整悬浮窗的宽度和高度（像素 px）。
     */
    public boolean onInternalResizeTouch(View view, MotionEvent event) {
        AppSettings settings = this.settingsSupplier.get();
        if (settings == null) {
            return false;
        }

        WindowManager.LayoutParams layoutParams = this.windowManagerHelper.getLayoutParams();
        if (layoutParams == null) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.isResizing = false;
                if (view.getParent() != null) {
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                }
                this.initialWidth = layoutParams.width > 0 ? layoutParams.width : this.windowManagerHelper.getOverlayRoot().getWidth();
                this.initialHeight = layoutParams.height > 0 ? layoutParams.height : this.windowManagerHelper.getOverlayRoot().getHeight();
                this.initialResizeTouchX = event.getRawX();
                this.initialResizeTouchY = event.getRawY();
                return true;
            case MotionEvent.ACTION_MOVE:
                float internalDx = event.getRawX() - this.initialResizeTouchX;
                float internalDy = event.getRawY() - this.initialResizeTouchY;

                if (!this.isResizing && Math.hypot(internalDx, internalDy) > this.touchSlop) {
                    this.isResizing = true;
                    WordScheduler scheduler = this.schedulerSupplier.get();
                    if (scheduler != null) {
                        scheduler.pause();
                    }
                }

                if (this.isResizing) {
                    DisplayMetrics metrics = this.context.getResources().getDisplayMetrics();
                    int minWidth = (int) (180 * metrics.density + 0.5f);
                    int minHeight = (int) (60 * metrics.density + 0.5f);

                    int newWidth = Math.max(minWidth, this.initialWidth + (int) internalDx);
                    int newHeight = Math.max(minHeight, this.initialHeight + (int) internalDy);

                    settings.setWidth(newWidth / metrics.density);
                    settings.setHeight(newHeight / metrics.density);

                    this.windowManagerHelper.updateSize(newWidth, newHeight);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (this.isResizing) {
                    this.isResizing = false;
                    AndroidSettingsStore.save(this.context, settings);
                    WordScheduler scheduler = this.schedulerSupplier.get();
                    if (scheduler != null) {
                        scheduler.resumeWithRemaining(1500);
                    }
                }
                return true;
            default:
                return true;
        }
    }

    /**
     * 响应点击穿透模式下外部独立把手的手指拖拽，调整悬浮窗的宽度和高度（像素 px）。
     */
    public boolean onExternalResizeTouch(View view, MotionEvent event) {
        AppSettings settings = this.settingsSupplier.get();
        if (settings == null) {
            return false;
        }

        WindowManager.LayoutParams layoutParams = this.windowManagerHelper.getLayoutParams();
        if (layoutParams == null) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.isResizing = false;
                this.resizeHandleManager.setResizing(true);
                this.initialWidth = layoutParams.width > 0 ? layoutParams.width : this.windowManagerHelper.getOverlayRoot().getWidth();
                this.initialHeight = layoutParams.height > 0 ? layoutParams.height : this.windowManagerHelper.getOverlayRoot().getHeight();
                this.initialResizeTouchX = event.getRawX();
                this.initialResizeTouchY = event.getRawY();
                return true;
            case MotionEvent.ACTION_MOVE:
                float externalDx = event.getRawX() - this.initialResizeTouchX;
                float externalDy = event.getRawY() - this.initialResizeTouchY;

                if (!this.isResizing && Math.hypot(externalDx, externalDy) > this.touchSlop) {
                    this.isResizing = true;
                    WordScheduler scheduler = this.schedulerSupplier.get();
                    if (scheduler != null) {
                        scheduler.pause();
                    }
                }

                if (this.isResizing) {
                    DisplayMetrics metrics = this.context.getResources().getDisplayMetrics();
                    int minWidth = (int) (180 * metrics.density + 0.5f);
                    int minHeight = (int) (60 * metrics.density + 0.5f);

                    int newWidth = Math.max(minWidth, this.initialWidth + (int) externalDx);
                    int newHeight = Math.max(minHeight, this.initialHeight + (int) externalDy);

                    settings.setWidth(newWidth / metrics.density);
                    settings.setHeight(newHeight / metrics.density);

                    this.windowManagerHelper.updateSize(newWidth, newHeight);

                    TextView handleView = this.resizeHandleManager.getExternalResizeHandleView();
                    if (handleView != null) {
                        int handleW = handleView.getMeasuredWidth();
                        int handleH = handleView.getMeasuredHeight();
                        this.resizeHandleManager.updateExternalPosition(layoutParams.x + newWidth - handleW, layoutParams.y + newHeight - handleH);
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                this.resizeHandleManager.setResizing(false);
                if (this.isResizing) {
                    this.isResizing = false;
                    AndroidSettingsStore.save(this.context, settings);
                    WordScheduler scheduler = this.schedulerSupplier.get();
                    if (scheduler != null) {
                        scheduler.resumeWithRemaining(1500);
                    }
                }
                return true;
            default:
                return true;
        }
    }
}
