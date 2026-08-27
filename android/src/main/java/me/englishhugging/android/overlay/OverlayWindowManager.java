package me.englishhugging.android.overlay;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import me.englishhugging.android.databinding.OverlayWindowBinding;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.core.settings.OverlayMode;

/**
 * 悬浮窗主卡片窗口管理辅助工具。
 *
 * <p>负责主悬浮卡片视图的创建、窗口布局参数（LayoutParams）管理以及与 WindowManager 的交互。
 */
public final class OverlayWindowManager {

    /** 上下文环境对象 */
    private final Context context;
    /** 系统窗口管理服务 */
    private final WindowManager windowManager;

    /** 悬浮窗根布局容器 */
    private FrameLayout overlayRoot;
    /** 悬浮窗中展示单词富文本的文本控件 */
    private TextView overlayText;
    /** 可拖拽模式下的卡片内置调整把手控件 */
    private TextView internalResizeHandle;
    /** 悬浮窗的窗口布局参数 */
    private WindowManager.LayoutParams layoutParams;

    public OverlayWindowManager(Context context, WindowManager windowManager) {
        this.context = context;
        this.windowManager = windowManager;
    }

    /**
     * 加载并创建悬浮窗卡片布局视图，绑定触摸手势监听。
     *
     * @param settings                   当前应用配置
     * @param overlayTouchListener       主卡片拖拽平移手势监听
     * @param internalResizeTouchListener 内置把手缩放手势监听
     */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    public void createOverlayView(
            AppSettings settings,
            View.OnTouchListener overlayTouchListener,
            View.OnTouchListener internalResizeTouchListener
    ) {
        OverlayWindowBinding binding = OverlayWindowBinding.inflate(LayoutInflater.from(this.context));
        this.overlayRoot = binding.getRoot();
        this.overlayRoot.setAlpha((float) settings.getOpacity());

        this.overlayText = binding.overlayText;
        this.internalResizeHandle = binding.internalResizeHandle;

        try {
            this.internalResizeHandle.setTypeface(Typeface.createFromAsset(this.context.getAssets(), "fonts/MaterialIcons-Regular.ttf"));
        } catch (Exception ignored) {
            this.internalResizeHandle.setText("↘");
        }

        DisplayMetrics metrics = this.context.getResources().getDisplayMetrics();
        this.overlayText.setMaxWidth((int) (metrics.widthPixels * 0.9f));

        this.overlayRoot.setOnTouchListener(overlayTouchListener);
        this.internalResizeHandle.setOnTouchListener(internalResizeTouchListener);
    }

    /**
     * 根据当前显示模式与尺寸配置创建窗口布局参数。
     * 若为点击穿透模式，添加 FLAG_NOT_TOUCHABLE 标记使触摸事件穿透到底层应用。
     *
     * @param settings 当前应用配置
     * @return 计算得到的窗口布局参数对象
     */
    private WindowManager.LayoutParams createLayoutParams(AppSettings settings) {
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        if (settings.getOverlayMode() == OverlayMode.CLICK_THROUGH) {
            flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }

        DisplayMetrics metrics = this.context.getResources().getDisplayMetrics();
        int width = toPixelSize(settings.getWidth(), metrics);
        int height = toPixelSize(settings.getHeight(), metrics);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width, height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = (int) settings.getX();
        params.y = (int) settings.getY();

        return params;
    }

    /**
     * 将 dp 尺寸转换为像素 px；若小于等于 0 则返回 WRAP_CONTENT。
     */
    private int toPixelSize(double dpValue, DisplayMetrics metrics) {
        return dpValue > 0 ? (int) (dpValue * metrics.density + 0.5f) : WindowManager.LayoutParams.WRAP_CONTENT;
    }

    /**
     * 将悬浮窗主卡片添加到系统窗口中。
     */
    public void addToWindow(AppSettings settings) {
        if (this.overlayRoot == null) {
            return;
        }
        this.layoutParams = createLayoutParams(settings);
        this.windowManager.addView(this.overlayRoot, this.layoutParams);
    }

    /**
     * 从系统窗口中安全移除悬浮窗主卡片。
     */
    public void removeFromWindow() {
        if (this.overlayRoot != null) {
            try {
                this.windowManager.removeView(this.overlayRoot);
            } catch (Exception ignored) {
            }
            this.overlayRoot = null;
            this.overlayText = null;
            this.internalResizeHandle = null;
        }
    }

    /**
     * 更新悬浮窗的位置坐标（像素 px），并同步刷新系统窗口布局。
     */
    public void updatePosition(int xPx, int yPx) {
        if (this.layoutParams != null && this.overlayRoot != null) {
            this.layoutParams.x = xPx;
            this.layoutParams.y = yPx;
            try {
                this.windowManager.updateViewLayout(this.overlayRoot, this.layoutParams);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 更新悬浮窗的宽度和高度（像素 px），并同步刷新系统窗口布局。
     */
    public void updateSize(int widthPx, int heightPx) {
        if (this.layoutParams != null && this.overlayRoot != null) {
            this.layoutParams.width = widthPx;
            this.layoutParams.height = heightPx;
            try {
                this.windowManager.updateViewLayout(this.overlayRoot, this.layoutParams);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 更新悬浮窗根布局的透明度（0.0 ~ 1.0）。
     *
     * @param opacity 透明度比率（0.0 ~ 1.0）
     */
    public void updateAlpha(double opacity) {
        if (this.overlayRoot != null) {
            this.overlayRoot.setAlpha((float) opacity);
        }
    }

    /**
     * 根据最新的应用配置全量更新主悬浮窗布局参数。
     */
    public void reloadLayout(AppSettings settings) {
        if (this.overlayRoot != null) {
            this.overlayRoot.setAlpha((float) settings.getOpacity());
            this.layoutParams = createLayoutParams(settings);
            try {
                this.windowManager.updateViewLayout(this.overlayRoot, this.layoutParams);
            } catch (Exception ignored) {
            }
        }
    }

    public FrameLayout getOverlayRoot() {
        return this.overlayRoot;
    }

    public TextView getOverlayText() {
        return this.overlayText;
    }

    public TextView getInternalResizeHandle() {
        return this.internalResizeHandle;
    }

    public WindowManager.LayoutParams getLayoutParams() {
        return this.layoutParams;
    }
}
