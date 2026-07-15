package me.englishhugging.android;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;

import com.google.android.material.button.MaterialButton;

import java.util.function.Supplier;

import me.englishhugging.android.databinding.ActivityMainBinding;
import me.englishhugging.android.ui.AndroidUi;
import me.englishhugging.android.ui.tabs.CustomVocabularyTab;
import me.englishhugging.android.ui.tabs.HomeTab;
import me.englishhugging.android.ui.tabs.RecordsTab;
import me.englishhugging.android.ui.tabs.SettingsTab;

/**
 * Android 端唯一的 Activity 宿主。
 *
 * <p>页面静态结构由 XML 描述，并通过 View Binding 获取控件引用。首页、设置、记录和
 * 自定义词库仍然共用同一个 Activity；切换页面时只替换 {@code pageContainer} 内的 View，
 * 从而保留原项目轻量的单 Activity 导航方式。</p>
 */
public final class MainActivity extends ComponentActivity {
    /** Activity 根布局生成的绑定对象。 */
    private ActivityMainBinding binding;

    /** 少量不能静态写入 XML 的公共 UI 行为，例如图标字体和 dp 换算。 */
    private AndroidUi ui;

    /** 各页面的视图绑定与业务控制器。 */
    private HomeTab homeTab;
    private SettingsTab settingsTab;
    private RecordsTab recordsTab;
    private CustomVocabularyTab customVocabularyTab;

    /** 二级页面启用此回调，使手机系统返回键先回首页。 */
    private OnBackPressedCallback backToHomeCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 先加载 Activity 的 XML 外壳，页面内容随后注入 pageContainer。
        this.binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(this.binding.getRoot());

        // 页面之间通过回调导航，避免页面控制器直接操作 Activity 的私有状态。
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
     * 用新页面替换内容容器，并统一执行淡出、替换、淡入动画。
     *
     * @param selectedTab 当前需要高亮的底部按钮；设置页没有对应按钮时传 {@code null}
     * @param pageFactory 延迟创建目标页面的工厂，确保旧页面退出后才加载新布局
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

            // 新页面从上方轻微位移处淡入，延续原界面的切换手感。
            this.binding.pageContainer.setTranslationY(-this.ui.dp(10));
            this.binding.pageContainer.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(150)
                    .start();
        };

        // 冷启动没有旧页面，不需要先播放退出动画。
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

    /** 更新底部导航按钮的 selected 状态，颜色由 XML selector 自动处理。 */
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

    @SuppressWarnings("deprecation")
    /**
     * 让状态栏、导航栏及其图标跟随系统浅色/深色模式。
     *
     * <p>颜色来自普通 {@code values} 或 {@code values-night} 资源，图标明暗则由
     * {@code light_system_bars} 布尔资源决定。</p>
     */
    private void styleSystemBars() {
        getWindow().setStatusBarColor(getColor(R.color.page_background));
        getWindow().setNavigationBarColor(getColor(R.color.page_background));
        int systemUiVisibility = 0;
        if (getResources().getBoolean(R.bool.light_system_bars)) {
            systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        getWindow().getDecorView().setSystemUiVisibility(systemUiVisibility);
    }
}
