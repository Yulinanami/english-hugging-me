package me.englishhugging.android;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;

import java.util.function.Supplier;

import me.englishhugging.android.databinding.ActivityMainBinding;
import me.englishhugging.android.ui.AndroidUi;
import me.englishhugging.android.ui.tabs.CustomVocabularyTab;
import me.englishhugging.android.ui.tabs.HomeTab;
import me.englishhugging.android.ui.tabs.RecordsTab;
import me.englishhugging.android.ui.tabs.SettingsTab;

/**
 * Android 手机端主界面。
 *
 * <p>这个类作为整个手机 App 的主窗口和导航中心，负责：
 * <ul>
 *   <li><b>界面切换</b>：在主窗口中切换首页、设置、学习记录和自定义词库页面；</li>
 *   <li><b>返回键处理</b>：在二级页面（如设置、词库）按返回键先回到首页，在首页再退出应用；</li>
 *   <li><b>系统状态栏适配</b>：让手机顶部状态栏和底部导航栏颜色自适应浅色/深色主题；</li>
 *   <li><b>权限申请</b>：在 Android 13+ 系统上动态申请通知权限，确保悬浮窗服务正常运行。</li>
 * </ul>
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 在 AndroidManifest.xml 中注册为启动入口 Activity
 * &lt;activity
 *     android:name=".MainActivity"
 *     android:exported="true"
 *     android:windowSoftInputMode="adjustPan"&gt;
 *     &lt;intent-filter&gt;
 *         &lt;action android:name="android.intent.action.MAIN" /&gt;
 *         &lt;category android:name="android.intent.category.LAUNCHER" /&gt;
 *     &lt;/intent-filter&gt;
 * &lt;/activity&gt;
 * </code></pre>
 */
public final class MainActivity extends ComponentActivity {
    /** Activity 布局绑定对象 */
    private ActivityMainBinding binding;

    /** 界面辅助工具（图标字体、尺寸转换等） */
    private AndroidUi ui;

    /** 首页页面对象 */
    private HomeTab homeTab;

    /** 设置页面对象 */
    private SettingsTab settingsTab;

    /** 学习记录页面对象 */
    private RecordsTab recordsTab;

    /** 自定义生词页面对象 */
    private CustomVocabularyTab customVocabularyTab;

    /** 系统返回键回调，在子页面按返回键时返回首页 */
    private OnBackPressedCallback backToHomeCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(this.binding.getRoot());

        this.ui = new AndroidUi(this);
        this.homeTab = new HomeTab(this, this.ui, this::showSettingsPage);
        this.settingsTab = new SettingsTab(this, this.ui, this::showHomePage);
        this.recordsTab = new RecordsTab(this, this.ui, this::showRecordsPage, this::showHomePage);
        this.customVocabularyTab = new CustomVocabularyTab(this, this.ui, this::showHomePage);

        // 首页禁用此回调，让系统返回键按默认行为退出；二级页则返回首页。
        this.backToHomeCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                showHomePage();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, this.backToHomeCallback);

        bindBottomNavigation();
        styleSystemBars();
        requestNotificationPermissionIfNeeded();
        showHomePage();
    }

    /**
     * 应用从后台回到前台时刷新悬浮服务状态，避免首页按钮显示过期状态。
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (this.homeTab != null) {
            this.homeTab.updateStartCircleState();
        }
    }

    /** 为底部导航按钮设置 Material Icons 字体并绑定页面跳转事件。 */
    private void bindBottomNavigation() {
        this.ui.styleIcon(this.binding.homeTabButton);
        this.ui.styleIcon(this.binding.recordsTabButton);
        this.ui.styleIcon(this.binding.customVocabularyTabButton);

        this.binding.homeTabButton.setOnClickListener(view -> showHomePage());
        this.binding.recordsTabButton.setOnClickListener(view -> showRecordsPage());
        this.binding.customVocabularyTabButton.setOnClickListener(view -> showCustomVocabularyPage());
    }

    /**
     * 切换主页面，并统一执行淡出、替换、淡入动画。
     *
     * @param selectedTab 当前需要高亮的底部按钮；设置页没有对应按钮时传 {@code null}
     * @param pageFactory 创建目标页面的回调函数，确保旧页面退出后才加载新布局
     */
    private void switchPage(MaterialButton selectedTab, Supplier<View> pageFactory) {
        Runnable replacePage = () -> {
            selectTab(selectedTab);

            // 每次进入页面都重新加载其绑定，保证设置值和记录数据是最新的。
            this.binding.pageContainer.removeAllViews();
            View page = pageFactory.get();
            this.binding.pageContainer.addView(
                    page,
                    new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    )
            );

            // 新页面执行淡入动画并回到原位。
            this.binding.pageContainer.setTranslationY(-this.ui.dp(10));
            this.binding.pageContainer.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(150)
                    .start();
        };

        // 首次进入时没有旧页面，直接显示新页面。
        if (this.binding.pageContainer.getChildCount() == 0) {
            this.binding.pageContainer.setAlpha(0f);
            this.binding.pageContainer.setTranslationY(this.ui.dp(10));
            replacePage.run();
            return;
        }

        // 已有页面时先淡出，动画结束后再执行 replacePage。
        this.binding.pageContainer.animate()
                .alpha(0f)
                .translationY(this.ui.dp(10))
                .setDuration(150)
                .withEndAction(replacePage)
                .start();
    }

    /** 显示首页，并恢复系统返回键的默认退出行为。 */
    private void showHomePage() {
        this.backToHomeCallback.setEnabled(false);
        switchPage(this.binding.homeTabButton, this.homeTab::getView);
    }

    /** 显示设置页；系统返回键会先返回首页。 */
    private void showSettingsPage() {
        this.backToHomeCallback.setEnabled(true);
        switchPage(null, this.settingsTab::getView);
    }

    /** 显示播放记录页；系统返回键会先返回首页。 */
    private void showRecordsPage() {
        this.backToHomeCallback.setEnabled(true);
        switchPage(this.binding.recordsTabButton, this.recordsTab::getView);
    }

    /** 显示自定义词库页；系统返回键会先返回首页。 */
    private void showCustomVocabularyPage() {
        this.backToHomeCallback.setEnabled(true);
        switchPage(this.binding.customVocabularyTabButton, this.customVocabularyTab::getView);
    }

    /** 更新底部导航按钮的选中高亮状态。 */
    private void selectTab(MaterialButton selected) {
        this.binding.homeTabButton.setSelected(selected == this.binding.homeTabButton);
        this.binding.recordsTabButton.setSelected(selected == this.binding.recordsTabButton);
        this.binding.customVocabularyTabButton.setSelected(
                selected == this.binding.customVocabularyTabButton
        );
    }

    /** Android 13 及以上需要运行时申请前台服务通知权限。 */
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    /**
     * 配置系统状态栏与导航栏图标明暗。
     *
     * <p>根据浅色/深色主题动态切换状态栏与导航栏图标的前景色。</p>
     */
    private void styleSystemBars() {
        boolean isLight = getResources().getBoolean(R.bool.light_system_bars);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(isLight);
        controller.setAppearanceLightNavigationBars(isLight);
    }
}
