package me.englishhugging.android.overlay;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import me.englishhugging.android.databinding.OverlayResizeHandleBinding;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.core.settings.OverlayMode;

/**
 * 悬浮窗调整大小把手管理辅助工具。
 *
 * <p>负责在两种悬浮窗模式下管理把手的显示、销毁以及位置对齐：
 * 1. 可拖拽模式下使用卡片内部把手；
 * 2. 点击穿透模式下使用外部独立微型小窗口把手，并紧跟主悬浮窗右下角。
 */
public final class OverlayResizeHandleManager {

    /** 上下文环境对象 */
    private final Context context;
    /** 系统窗口管理服务 */
    private final WindowManager windowManager;
    /** 主悬浮窗窗口管理组件 */
    private final OverlayWindowManager windowManagerHelper;

    /** 点击穿透模式下的独立外部调整把手文本控件 */
    private TextView externalResizeHandleView;
    /** 点击穿透模式下独立外部调整把手的窗口布局参数 */
    private WindowManager.LayoutParams externalResizeHandleParams;
    /** 标识当前是否正在拖拽把手缩放中，避免异步排版监听回调干扰 */
    private boolean isResizing = false;

    /** 监听主悬浮窗尺寸变化以同步更新独立外部把手位置的全局布局监听回调 */
    private final ViewTreeObserver.OnGlobalLayoutListener layoutListener = this::syncExternalPosition;

    public OverlayResizeHandleManager(Context context, WindowManager windowManager, OverlayWindowManager windowManagerHelper) {
        this.context = context;
        this.windowManager = windowManager;
        this.windowManagerHelper = windowManagerHelper;
    }

    /**
     * 统一管理调整大小把手：根据当前显示模式和开关状态，动态显示内置把手或创建外部把手窗口。
     *
     * @param settings             当前应用配置
     * @param externalTouchListener 外部把手的触摸手势监听
     */
    public void manageHandles(AppSettings settings, View.OnTouchListener externalTouchListener) {
        FrameLayout root = this.windowManagerHelper.getOverlayRoot();
        if (root == null) {
            return;
        }

        OverlayMode mode = settings.getOverlayMode();
        boolean resizeMode = settings.isResizeMode();
        TextView internalHandle = this.windowManagerHelper.getInternalResizeHandle();

        if (mode == OverlayMode.DRAGGABLE) {
            // 可拖拽模式：移除外部独立窗口，显示卡片内置把手
            removeExternalHandle();
            if (internalHandle != null) {
                internalHandle.setVisibility(resizeMode ? View.VISIBLE : View.GONE);
            }
        } else {
            // 点击穿透模式：隐藏卡片内置把手，根据设置开启或关闭外部独立小窗口
            if (internalHandle != null) {
                internalHandle.setVisibility(View.GONE);
            }

            if (resizeMode) {
                setupExternalHandle(externalTouchListener);
            } else {
                removeExternalHandle();
            }
        }
    }

    /**
     * 初始化或更新点击穿透模式下的独立外部调整把手窗口。
     */
    public void setupExternalHandle(View.OnTouchListener externalTouchListener) {
        if (this.externalResizeHandleView == null) {
            OverlayResizeHandleBinding resizeBinding = OverlayResizeHandleBinding.inflate(LayoutInflater.from(this.context));
            this.externalResizeHandleView = resizeBinding.getRoot();
            try {
                this.externalResizeHandleView.setTypeface(Typeface.createFromAsset(this.context.getAssets(), "fonts/MaterialIcons-Regular.ttf"));
            } catch (Exception ignored) {
                this.externalResizeHandleView.setText("↘");
            }
            // 初始先设为 INVISIBLE，等待测量出主悬浮窗宽高后再显示，防止在 (0, 0) 闪现
            this.externalResizeHandleView.setVisibility(View.INVISIBLE);
            this.externalResizeHandleView.setOnTouchListener(externalTouchListener);

            this.externalResizeHandleParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
            this.externalResizeHandleParams.gravity = Gravity.TOP | Gravity.START;

            this.windowManager.addView(this.externalResizeHandleView, this.externalResizeHandleParams);
        }

        FrameLayout root = this.windowManagerHelper.getOverlayRoot();
        if (root != null) {
            root.getViewTreeObserver().removeOnGlobalLayoutListener(this.layoutListener);
            root.getViewTreeObserver().addOnGlobalLayoutListener(this.layoutListener);
            root.post(this::syncExternalPosition);
        }
        syncExternalPosition();
    }

    /**
     * 移除并关闭点击穿透模式下的独立外部调整把手窗口。
     */
    public void removeExternalHandle() {
        if (this.externalResizeHandleView != null) {
            try {
                this.windowManager.removeView(this.externalResizeHandleView);
            } catch (Exception ignored) {
            }
            this.externalResizeHandleView = null;
            this.externalResizeHandleParams = null;
        }

        FrameLayout root = this.windowManagerHelper.getOverlayRoot();
        if (root != null) {
            root.getViewTreeObserver().removeOnGlobalLayoutListener(this.layoutListener);
        }
    }

    /**
     * 同步外部独立调整把手位置，使其紧跟主悬浮窗右下角。
     */
    public void syncExternalPosition() {
        FrameLayout root = this.windowManagerHelper.getOverlayRoot();
        WindowManager.LayoutParams mainParams = this.windowManagerHelper.getLayoutParams();

        if (this.externalResizeHandleView != null && root != null && mainParams != null && this.externalResizeHandleParams != null) {
            if (this.isResizing) {
                // 拖拽期间把手位置由触摸手势即时推导更新，避免异步排版回调干扰
                return;
            }

            int width = root.getWidth();
            int height = root.getHeight();

            if (width <= 0 && mainParams.width > 0) {
                width = mainParams.width;
            }
            if (height <= 0 && mainParams.height > 0) {
                height = mainParams.height;
            }

            if (width > 0 && height > 0) {
                this.externalResizeHandleView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int handleW = this.externalResizeHandleView.getMeasuredWidth();
                int handleH = this.externalResizeHandleView.getMeasuredHeight();

                this.externalResizeHandleParams.x = mainParams.x + width - handleW;
                this.externalResizeHandleParams.y = mainParams.y + height - handleH;

                if (this.externalResizeHandleView.getVisibility() != View.VISIBLE) {
                    this.externalResizeHandleView.setVisibility(View.VISIBLE);
                }
                try {
                    this.windowManager.updateViewLayout(this.externalResizeHandleView, this.externalResizeHandleParams);
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * 在拖拽调整大小时更新外部独立把手的绝对坐标（像素 px）。
     */
    public void updateExternalPosition(int xPx, int yPx) {
        if (this.externalResizeHandleParams != null && this.externalResizeHandleView != null) {
            this.externalResizeHandleParams.x = xPx;
            this.externalResizeHandleParams.y = yPx;
            try {
                this.windowManager.updateViewLayout(this.externalResizeHandleView, this.externalResizeHandleParams);
            } catch (Exception ignored) {
            }
        }
    }

    public void setResizing(boolean resizing) {
        this.isResizing = resizing;
    }

    public TextView getExternalResizeHandleView() {
        return this.externalResizeHandleView;
    }

    public WindowManager.LayoutParams getExternalResizeHandleParams() {
        return this.externalResizeHandleParams;
    }
}
