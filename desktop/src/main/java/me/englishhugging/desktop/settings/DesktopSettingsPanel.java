package me.englishhugging.desktop.settings;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import me.englishhugging.core.settings.AppSettings;
import me.englishhugging.desktop.overlay.DesktopOverlayController;
import me.englishhugging.desktop.ui.DesktopUi;

import java.io.InputStream;

public final class DesktopSettingsPanel {
    private static final String APP_ICON_RESOURCE = "/icons/app.png";

    private final AppSettings settings;
    private final DesktopSettingsStore settingsStore;
    private final DesktopOverlayController overlayController;
    private final Runnable onSettingsChanged;
    private final Runnable onVocabularyChanged;

    private Stage settingsStage;
    private PlaybackRecordsTab recordsTab;
    private Image appIcon;

    public DesktopSettingsPanel(
            AppSettings settings,
            DesktopSettingsStore settingsStore,
            DesktopOverlayController overlayController,
            Runnable onSettingsChanged,
            Runnable onVocabularyChanged
    ) {
        this.settings = settings;
        this.settingsStore = settingsStore;
        this.overlayController = overlayController;
        this.onSettingsChanged = onSettingsChanged;
        this.onVocabularyChanged = onVocabularyChanged;
    }

    public void init() {
        settingsStage = createSettingsStage();
    }

    public void show() {
        settingsStage.show();
        settingsStage.setIconified(false);
        settingsStage.toFront();
    }

    public void refreshPlaybackRecords() {
        if (recordsTab != null) {
            recordsTab.refresh();
        }
    }

    private Stage createSettingsStage() {
        Stage stage = new Stage();
        stage.setTitle("English Hugging Me 首选项");
        applyStageIcon(stage);
        stage.setOnCloseRequest(event -> { event.consume(); stage.hide(); });
        stage.iconifiedProperty().addListener((o, ov, nv) -> {
            if (nv) { stage.setIconified(false); stage.hide(); }
        });

        GeneralSettingsTab generalTab = new GeneralSettingsTab(settings, settingsStore, overlayController, onSettingsChanged, onVocabularyChanged);
        VocabularySettingsTab vocabularyTab = new VocabularySettingsTab(settings, settingsStore, stage, onVocabularyChanged);
        CustomVocabularyTab customTab = new CustomVocabularyTab(onVocabularyChanged);
        AppearanceSettingsTab appearanceTab = new AppearanceSettingsTab(settings, settingsStore, overlayController);
        recordsTab = new PlaybackRecordsTab(settings, settingsStore, vocabularyTab);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-background-color: #F6F8FC; -fx-tab-min-height: 30px; -fx-tab-max-height: 30px;");
        tabs.getTabs().addAll(
                DesktopUi.settingsTab("常规", generalTab.createContent()),
                DesktopUi.settingsTab("词库", vocabularyTab.createContent()),
                DesktopUi.settingsTab("自定义", customTab.createContent()),
                DesktopUi.settingsTab("外观", appearanceTab.createContent()),
                DesktopUi.settingsTab("记录", recordsTab.createContent())
        );

        Button close = DesktopUi.compactButton("退出程序");
        close.setOnAction(event -> {
            javafx.application.Platform.exit();
            System.exit(0);
        });
        Region spacer = new Region();
        HBox bottom = new HBox(8, spacer, close);
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(0, 8, 0, 8));

        VBox root = new VBox(8, tabs, bottom);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #F6F8FC; -fx-font-family: 'Microsoft YaHei UI', 'Microsoft YaHei', 'SimSun'; -fx-font-size: 13px;");
        stage.setScene(new Scene(root, 560, 460));
        return stage;
    }

    private void applyStageIcon(Stage stage) {
        Image icon = appIcon();
        if (icon != null) stage.getIcons().add(icon);
    }

    private Image appIcon() {
        if (appIcon == null) {
            try (InputStream in = DesktopSettingsPanel.class.getResourceAsStream(APP_ICON_RESOURCE)) {
                if (in != null) appIcon = new Image(in);
            } catch (Exception e) {
                System.err.println("Failed to load app icon: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return appIcon;
    }
}
