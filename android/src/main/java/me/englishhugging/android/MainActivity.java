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

public final class MainActivity extends Activity {
    private AndroidUi ui;
    private LinearLayout pageContainer;
    private LinearLayout pageHeaderContainer;
    private LinearLayout pageContent;
    private MaterialButton homeTabBtn;
    private MaterialButton recordsTabBtn;
    private MaterialButton customVocabTabBtn;

    private HomeTab homeTab;
    private SettingsTab settingsTab;
    private RecordsTab recordsTab;
    private CustomVocabularyTab customVocabTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = new AndroidUi(this);
        homeTab = new HomeTab(this, ui, this::showSettingsPage);
        settingsTab = new SettingsTab(this, ui, this::showHomePage);
        recordsTab = new RecordsTab(this, ui, this::showRecordsPage, this::showHomePage);
        customVocabTab = new CustomVocabularyTab(this, ui);

        styleSystemBars();
        requestNotificationPermissionIfNeeded();
        setContentView(createContentView());
        showHomePage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (homeTab != null) {
            homeTab.updateStartCircleState();
        }
    }

    private View createContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(AndroidUi.PAGE_BACKGROUND);

        pageContainer = new LinearLayout(this);
        pageContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(pageContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1
        ));

        pageHeaderContainer = new LinearLayout(this);
        pageHeaderContainer.setOrientation(LinearLayout.VERTICAL);
        pageHeaderContainer.setPadding(ui.dp(24), ui.getStatusBarHeight() + ui.dp(28), ui.dp(24), 0);
        pageContainer.addView(pageHeaderContainer, ui.matchWidthWrapHeight());

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(AndroidUi.PAGE_BACKGROUND);
        
        pageContent = new LinearLayout(this);
        pageContent.setOrientation(LinearLayout.VERTICAL);
        pageContent.setPadding(ui.dp(24), ui.dp(16), ui.dp(24), ui.dp(18));
        
        scrollView.addView(pageContent, ui.matchWidthWrapHeight());
        pageContainer.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1
        ));

        LinearLayout navWrap = new LinearLayout(this);
        navWrap.setGravity(Gravity.CENTER);
        navWrap.setPadding(ui.dp(28), ui.dp(4), ui.dp(28), ui.dp(18));
        navWrap.addView(createBottomNavigation(), ui.matchWidthWrapHeight());
        root.addView(navWrap, ui.matchWidthWrapHeight());
        return root;
    }

    private LinearLayout createBottomNavigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(ui.dp(4), ui.dp(4), ui.dp(4), ui.dp(4));
        nav.setBackground(ui.rounded(Color.rgb(243, 241, 248), Color.rgb(226, 224, 234), ui.dp(24)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            nav.setElevation(0);
            nav.setTranslationZ(0);
        }

        homeTabBtn = ui.tabButton("home");
        recordsTabBtn = ui.tabButton("history");
        customVocabTabBtn = ui.tabButton("edit");
        homeTabBtn.setOnClickListener(view -> showHomePage());
        recordsTabBtn.setOnClickListener(view -> showRecordsPage());
        customVocabTabBtn.setOnClickListener(view -> showCustomVocabPage());
        nav.addView(homeTabBtn, ui.tabLayoutParams());
        nav.addView(recordsTabBtn, ui.tabLayoutParams());
        nav.addView(customVocabTabBtn, ui.tabLayoutParams());
        return nav;
    }

    private void switchPage(MaterialButton tabBtn, Runnable buildContent) {
        if (pageContent.getChildCount() > 0 || pageHeaderContainer.getChildCount() > 0) {
            pageContainer.animate().alpha(0f).translationY(ui.dp(10)).setDuration(150).withEndAction(() -> {
                selectTab(tabBtn);
                pageHeaderContainer.removeAllViews();
                pageContent.removeAllViews();
                buildContent.run();
                pageContainer.setTranslationY(ui.dp(-10));
                pageContainer.animate().alpha(1f).translationY(0).setDuration(150).start();
            }).start();
        } else {
            selectTab(tabBtn);
            pageHeaderContainer.removeAllViews();
            pageContent.removeAllViews();
            buildContent.run();
            pageContainer.setAlpha(0f);
            pageContainer.setTranslationY(ui.dp(10));
            pageContainer.animate().alpha(1f).translationY(0).setDuration(300).start();
        }
    }

    private void showHomePage() {
        switchPage(homeTabBtn, () -> homeTab.buildContent(pageHeaderContainer, pageContent));
    }

    private void showSettingsPage() {
        switchPage(null, () -> settingsTab.buildContent(pageHeaderContainer, pageContent));
    }

    private void showRecordsPage() {
        switchPage(recordsTabBtn, () -> recordsTab.buildContent(pageHeaderContainer, pageContent));
    }

    private void showCustomVocabPage() {
        switchPage(customVocabTabBtn, () -> {
            LinearLayout header = ui.headerRow("自定义词汇", "");
            TextView backIcon = new TextView(this);
            backIcon.setText("chevron_left");
            backIcon.setTextSize(32);
            backIcon.setTypeface(ui.getIconFont());
            backIcon.setTextColor(AndroidUi.TEXT_PRIMARY);
            backIcon.setGravity(Gravity.CENTER);
            backIcon.setPadding(0, 0, ui.dp(8), 0);
            backIcon.setOnClickListener(v -> showHomePage());
            header.addView(backIcon, 0, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            
            pageHeaderContainer.addView(header, ui.matchWidthWithBottomMargin(12));
            pageContent.addView(customVocabTab.getView());
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
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    private void selectTab(MaterialButton selected) {
        ui.styleTab(homeTabBtn, selected == homeTabBtn);
        ui.styleTab(recordsTabBtn, selected == recordsTabBtn);
        ui.styleTab(customVocabTabBtn, selected == customVocabTabBtn);
    }
}
