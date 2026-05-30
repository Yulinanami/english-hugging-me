package me.englishhugging.android;

import android.Manifest;
import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

import me.englishhugging.android.ui.AndroidUi;
import me.englishhugging.android.ui.tabs.HomeTab;
import me.englishhugging.android.ui.tabs.RecordsTab;
import me.englishhugging.android.ui.tabs.SettingsTab;
import me.englishhugging.android.ui.tabs.CustomVocabularyTab;

/**
 * Android 端的单 Activity 宿主应用入口。
 *
 * <p>这个类摒弃了 Android 传统的基于 XML 和 Fragment 的重型开发模式，
 * 采用了极其轻量级的“纯 Java 代码拼装 UI”加“假 Tab 切换”的架构。
 * 整个 APP 只有一个真实的 Activity，通过在一个预定义的容器里来回添加、移除
 * {@link HomeTab} 等封装类生成的 View 来实现页面的切换。
 *
 * <p>这种极简的架构不仅绕过了 Fragment 生命周期黑洞的折磨，
 * 还使得我们可以轻松实现全应用级别的一致性淡入淡出过场动画。
 */
public final class MainActivity extends Activity {
    
    // 全局通用的 UI 工厂引用
    private AndroidUi ui;
    
    // 页面结构容器，自顶向下
    private LinearLayout pageContainer;
    private LinearLayout pageHeaderContainer;
    private LinearLayout pageContent;
    
    // 底部导航栏上的三个圆角 Material 图标按钮
    private MaterialButton homeTabBtn;
    private MaterialButton recordsTabBtn;
    private MaterialButton customVocabTabBtn;

    // 页面的逻辑控制器
    private HomeTab homeTab;
    private SettingsTab settingsTab;
    private RecordsTab recordsTab;
    private CustomVocabularyTab customVocabTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 1. 初始化纯代码 UI 工厂
        this.ui = new AndroidUi(this);
        
        // 2. 实例化各个子页面的逻辑生成器
        this.homeTab = new HomeTab(this, this.ui, this::showSettingsPage);
        this.settingsTab = new SettingsTab(this, this.ui, this::showHomePage);
        this.recordsTab = new RecordsTab(this, this.ui, this::showRecordsPage, this::showHomePage);
        this.customVocabTab = new CustomVocabularyTab(this, this.ui);

        // 3. 抹平 Android 的顶部状态栏和底部导航条色差
        styleSystemBars();
        
        // 4. 动态请求 Android 13+ 运行前台服务必须的通知权限
        requestNotificationPermissionIfNeeded();
        
        // 5. 组装全局的框架并在根节点渲染首页
        setContentView(createContentView());
        showHomePage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次应用从后台切换到前台时，主动去同步一次当前悬浮窗是否正在运行的状态
        if (this.homeTab != null) {
            this.homeTab.updateStartCircleState();
        }
    }

    /**
     * 构建无 XML 的纯 Java 根布局骨架。
     */
    private View createContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(AndroidUi.PAGE_BACKGROUND);

        // 主页面容器，包含头部和可滚动的内容区，占据除去底部导航条之外的所有空间
        this.pageContainer = new LinearLayout(this);
        this.pageContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(this.pageContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1
        ));

        // 悬浮在上方的动态标题栏容器
        this.pageHeaderContainer = new LinearLayout(this);
        this.pageHeaderContainer.setOrientation(LinearLayout.VERTICAL);
        // 为顶部的沉浸式状态栏预留高度
        this.pageHeaderContainer.setPadding(this.ui.dp(24), this.ui.getStatusBarHeight() + this.ui.dp(28), this.ui.dp(24), 0);
        this.pageContainer.addView(this.pageHeaderContainer, this.ui.matchWidthWrapHeight());

        // 可滚动的正文区域容器
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(AndroidUi.PAGE_BACKGROUND);
        
        this.pageContent = new LinearLayout(this);
        this.pageContent.setOrientation(LinearLayout.VERTICAL);
        this.pageContent.setPadding(this.ui.dp(24), this.ui.dp(16), this.ui.dp(24), this.ui.dp(18));
        
        scrollView.addView(this.pageContent, this.ui.matchWidthWrapHeight());
        this.pageContainer.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1
        ));

        // 底部悬浮样式的导航栏外层包裹器
        LinearLayout navWrap = new LinearLayout(this);
        navWrap.setGravity(Gravity.CENTER);
        navWrap.setPadding(this.ui.dp(28), this.ui.dp(4), this.ui.dp(28), this.ui.dp(18));
        navWrap.addView(createBottomNavigation(), this.ui.matchWidthWrapHeight());
        root.addView(navWrap, this.ui.matchWidthWrapHeight());
        
        return root;
    }

    /**
     * 构建漂浮于界面底部的类似 iOS 样式的胶囊导航栏。
     */
    private LinearLayout createBottomNavigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(this.ui.dp(4), this.ui.dp(4), this.ui.dp(4), this.ui.dp(4));
        nav.setBackground(this.ui.rounded(Color.rgb(243, 241, 248), Color.rgb(226, 224, 234), this.ui.dp(24)));
        
        // 抹去 Android 原生的 Z 轴阴影
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            nav.setElevation(0);
            nav.setTranslationZ(0);
        }

        // 初始化三个 Tab 的触控按钮，直接通过 MaterialIcon 的字母来渲染图案
        this.homeTabBtn = this.ui.tabButton("home");
        this.recordsTabBtn = this.ui.tabButton("history");
        this.customVocabTabBtn = this.ui.tabButton("edit");
        
        this.homeTabBtn.setOnClickListener(view -> showHomePage());
        this.recordsTabBtn.setOnClickListener(view -> showRecordsPage());
        this.customVocabTabBtn.setOnClickListener(view -> showCustomVocabPage());
        
        nav.addView(this.homeTabBtn, this.ui.tabLayoutParams());
        nav.addView(this.recordsTabBtn, this.ui.tabLayoutParams());
        nav.addView(this.customVocabTabBtn, this.ui.tabLayoutParams());
        
        return nav;
    }

    /**
     * 无 Fragment 的“假 Tab 切换”核心引擎，带平滑过度动画。
     *
     * @param tabBtn       要高亮的底部导航按钮（可传 null，比如跳转到二级页面时不选中任何 Tab）
     * @param buildContent 一段闭包逻辑，用于在新容器中填入新的 UI View
     */
    private void switchPage(MaterialButton tabBtn, Runnable buildContent) {
        if (this.pageContent.getChildCount() > 0 || this.pageHeaderContainer.getChildCount() > 0) {
            // 如果容器内已经有老页面，先做一个 150ms 的向下褪去动画
            this.pageContainer.animate()
                    .alpha(0f)
                    .translationY(this.ui.dp(10))
                    .setDuration(150)
                    .withEndAction(() -> {
                        // 老页面离场完毕，彻底清空容器
                        selectTab(tabBtn);
                        this.pageHeaderContainer.removeAllViews();
                        this.pageContent.removeAllViews();
                        
                        // 注入新页面的控件
                        buildContent.run();
                        
                        // 从下方 10dp 开始重新入场
                        this.pageContainer.setTranslationY(this.ui.dp(-10));
                        this.pageContainer.animate().alpha(1f).translationY(0).setDuration(150).start();
                    }).start();
        } else {
            // 这是冷启动时第一次进入，不需要清空老页面，直接一个 300ms 慢速浮出
            selectTab(tabBtn);
            this.pageHeaderContainer.removeAllViews();
            this.pageContent.removeAllViews();
            buildContent.run();
            
            this.pageContainer.setAlpha(0f);
            this.pageContainer.setTranslationY(this.ui.dp(10));
            this.pageContainer.animate().alpha(1f).translationY(0).setDuration(300).start();
        }
    }

    private void showHomePage() {
        switchPage(this.homeTabBtn, () -> this.homeTab.buildContent(this.pageHeaderContainer, this.pageContent));
    }

    private void showSettingsPage() {
        switchPage(null, () -> this.settingsTab.buildContent(this.pageHeaderContainer, this.pageContent));
    }

    private void showRecordsPage() {
        switchPage(this.recordsTabBtn, () -> this.recordsTab.buildContent(this.pageHeaderContainer, this.pageContent));
    }

    private void showCustomVocabPage() {
        switchPage(this.customVocabTabBtn, () -> {
            // 自定义词库页面的头部带一个返回箭头的二级 Header
            LinearLayout header = this.ui.headerRow("自定义词汇", "");
            
            TextView backIcon = new TextView(this);
            backIcon.setText("chevron_left");
            backIcon.setTextSize(32);
            backIcon.setTypeface(this.ui.getIconFont());
            backIcon.setTextColor(AndroidUi.TEXT_PRIMARY);
            backIcon.setGravity(Gravity.CENTER);
            backIcon.setPadding(0, 0, this.ui.dp(8), 0);
            backIcon.setOnClickListener(v -> showHomePage());
            
            header.addView(backIcon, 0, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            
            this.pageHeaderContainer.addView(header, this.ui.matchWidthWithBottomMargin(12));
            this.pageContent.addView(this.customVocabTab.getView());
        });
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    @SuppressWarnings("deprecation")
    private void styleSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(AndroidUi.PAGE_BACKGROUND);
            getWindow().setNavigationBarColor(AndroidUi.PAGE_BACKGROUND);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 要求状态栏图标变成深色（因为我们整个页面背景都是白灰色）
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    /**
     * 更新底部三个 Tab 的高亮状态。
     */
    private void selectTab(MaterialButton selected) {
        this.ui.styleTab(this.homeTabBtn, selected == this.homeTabBtn);
        this.ui.styleTab(this.recordsTabBtn, selected == this.recordsTabBtn);
        this.ui.styleTab(this.customVocabTabBtn, selected == this.customVocabTabBtn);
    }
}
