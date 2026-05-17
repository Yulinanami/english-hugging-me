package me.englishhugging.desktop.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import me.englishhugging.desktop.overlay.WindowsClickThrough;

import javax.imageio.ImageIO;
import java.awt.AWTException;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public final class DesktopTrayController {
    private static final String APP_ICON_RESOURCE = "/icons/app.png";

    private final Stage owner;
    private final Runnable openSettings;
    private final Runnable exitApplication;
    private TrayIcon trayIcon;
    private Popup trayMenu;
    private Timeline trayMenuWatcher;

    public DesktopTrayController(Stage owner, Runnable openSettings, Runnable exitApplication) {
        this.owner = owner;
        this.openSettings = openSettings;
        this.exitApplication = exitApplication;
    }

    public boolean install() {
        if (!SystemTray.isSupported()) return false;
        try {
            trayIcon = new TrayIcon(createTrayImage(), "English Hugging Me");
            trayIcon.setImageAutoSize(true);
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent event) {
                    if (event.isPopupTrigger() || event.getButton() == MouseEvent.BUTTON1) {
                        javafx.application.Platform.runLater(DesktopTrayController.this::showTrayMenu);
                    }
                }
            });
            SystemTray.getSystemTray().add(trayIcon);
            return true;
        } catch (AWTException e) {
            return false;
        }
    }

    public void remove() {
        stopTrayMenuWatcher();
        if (trayMenu != null) { trayMenu.hide(); trayMenu = null; }
        if (trayIcon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }
    }

    private void showTrayMenu() {
        if (trayMenu != null) trayMenu.hide();
        stopTrayMenuWatcher();

        Label openSettingsItem = trayMenuItem("打开设置");
        openSettingsItem.setOnMouseClicked(e -> { trayMenu.hide(); openSettings.run(); });
        Label exitItem = trayMenuItem("退出");
        exitItem.setOnMouseClicked(e -> { trayMenu.hide(); exitApplication.run(); });

        Region separator = new Region();
        separator.setPrefHeight(1);
        separator.setStyle("-fx-background-color: #E5E7EB;");
        VBox.setMargin(separator, new Insets(4, 0, 4, 0));

        VBox menuContent = new VBox(openSettingsItem, separator, exitItem);
        menuContent.setStyle("-fx-background-color: rgba(255,255,255,0.98); -fx-background-radius: 9; -fx-border-color: #DADDE3; -fx-border-width: 1; -fx-border-radius: 9; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 18, 0, 0, 6); -fx-font-family: 'Microsoft YaHei UI', 'Microsoft YaHei', 'SimSun'; -fx-font-size: 13px;");
        menuContent.setPadding(new Insets(6, 0, 6, 0));

        Point pointer = MouseInfo.getPointerInfo().getLocation();
        boolean openedFromOverflow = WindowsClickThrough.isNotifyIconOverflowVisible();
        trayMenu = new Popup();
        trayMenu.setAutoHide(true);
        trayMenu.setHideOnEscape(true);
        trayMenu.getContent().add(menuContent);
        trayMenu.setOnHidden(e -> stopTrayMenuWatcher());
        trayMenu.show(owner, pointer.x + 8, pointer.y - 8);
        startTrayMenuWatcher(openedFromOverflow);
    }

    private Label trayMenuItem(String text) {
        Label item = new Label(text);
        item.setMinWidth(132);
        item.setMinHeight(30);
        item.setPadding(new Insets(7, 18, 7, 18));
        item.setCursor(Cursor.HAND);
        item.setStyle("-fx-text-fill: #1F2328; -fx-background-radius: 6;");
        item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: #F3F6FA; -fx-text-fill: #1F2328; -fx-background-radius: 6;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-text-fill: #1F2328; -fx-background-radius: 6;"));
        return item;
    }

    private void startTrayMenuWatcher(boolean openedFromOverflow) {
        if (!openedFromOverflow) return;
        trayMenuWatcher = new Timeline(new KeyFrame(Duration.millis(150), e -> {
            if (trayMenu != null && trayMenu.isShowing() && !WindowsClickThrough.isNotifyIconOverflowVisible()) {
                trayMenu.hide();
            }
        }));
        trayMenuWatcher.setCycleCount(Timeline.INDEFINITE);
        trayMenuWatcher.play();
    }

    private void stopTrayMenuWatcher() {
        if (trayMenuWatcher != null) { trayMenuWatcher.stop(); trayMenuWatcher = null; }
    }

    private BufferedImage createTrayImage() {
        try (InputStream in = DesktopTrayController.class.getResourceAsStream(APP_ICON_RESOURCE)) {
            if (in != null) {
                BufferedImage image = ImageIO.read(in);
                if (image != null) return image;
            }
        } catch (IOException ignored) {}
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new java.awt.Color(47, 111, 237));
        g.fillRoundRect(1, 1, 14, 14, 4, 4);
        g.setColor(java.awt.Color.WHITE);
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
        g.drawString("E", 4, 12);
        g.dispose();
        return image;
    }
}
