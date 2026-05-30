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

/**
 * 桌面系统右下角托盘图标及自定义菜单控制器。
 *
 * <p>这个类通过 AWT 的 {@link SystemTray} API 将程序挂载到 Windows/Mac 的系统托盘中，
 * 并在用户点击图标时，弹出由纯 JavaFX 绘制的精美无边框弹出菜单（取代了丑陋的原生菜单）。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * DesktopTrayController tray = new DesktopTrayController(stage, () -> openSettings(), () -> exit());
 * if (tray.install()) {
 *     // 安装成功，应用可完全后台运行
 * } else {
 *     // 安装失败（如无桌面环境），必须让主界面保持显示
 * }
 * </code></pre>
 */
public final class DesktopTrayController {
    
    /** 托盘图标资源的路径 */
    private static final String APP_ICON_RESOURCE = "/icons/app.png";

    private final Stage owner;
    private final Runnable openSettings;
    private final Runnable exitApplication;
    
    private TrayIcon trayIcon;
    private Popup trayMenu;
    private Timeline trayMenuWatcher;

    /**
     * 构建托盘控制器。
     *
     * @param owner           弹出菜单的归属父舞台
     * @param openSettings    点击“打开设置”时的回调
     * @param exitApplication 点击“退出”时的回调
     */
    public DesktopTrayController(Stage owner, Runnable openSettings, Runnable exitApplication) {
        this.owner = owner;
        this.openSettings = openSettings;
        this.exitApplication = exitApplication;
    }

    /**
     * 尝试将图标安装到系统托盘区。
     *
     * @return 成功返回 true，如果系统不支持则返回 false
     */
    public boolean install() {
        if (!SystemTray.isSupported()) {
            return false;
        }
        
        try {
            this.trayIcon = new TrayIcon(createTrayImage(), "English Hugging Me");
            this.trayIcon.setImageAutoSize(true);
            
            this.trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent event) {
                    boolean isClick = event.isPopupTrigger() || event.getButton() == MouseEvent.BUTTON1;
                    if (isClick) {
                        javafx.application.Platform.runLater(DesktopTrayController.this::showTrayMenu);
                    }
                }
            });
            
            SystemTray.getSystemTray().add(this.trayIcon);
            return true;
        } catch (AWTException e) {
            return false;
        }
    }

    /**
     * 将托盘图标从系统中移除并销毁弹出菜单。
     */
    public void remove() {
        stopTrayMenuWatcher();
        
        if (this.trayMenu != null) {
            this.trayMenu.hide();
            this.trayMenu = null;
        }
        
        if (this.trayIcon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(this.trayIcon);
            this.trayIcon = null;
        }
    }

    /**
     * 构建并展示 JavaFX 绘制的右键菜单。
     * 如果菜单已经在展示，则会将其先隐藏再重新于鼠标位置弹出。
     */
    private void showTrayMenu() {
        if (this.trayMenu != null) {
            this.trayMenu.hide();
        }
        stopTrayMenuWatcher();

        // 1. 构造菜单项：打开设置
        Label openSettingsItem = trayMenuItem("打开设置");
        openSettingsItem.setOnMouseClicked(e -> {
            this.trayMenu.hide();
            this.openSettings.run();
        });
        
        // 2. 构造菜单项：退出
        Label exitItem = trayMenuItem("退出");
        exitItem.setOnMouseClicked(e -> {
            this.trayMenu.hide();
            this.exitApplication.run();
        });

        // 3. 构造优雅的分割线
        Region separator = new Region();
        separator.setPrefHeight(1);
        separator.setStyle("-fx-background-color: #E5E7EB;");
        VBox.setMargin(separator, new Insets(4, 0, 4, 0));

        // 4. 组装面板
        VBox menuContent = new VBox(openSettingsItem, separator, exitItem);
        menuContent.setStyle("-fx-background-color: rgba(255,255,255,0.98); -fx-background-radius: 9; -fx-border-color: #DADDE3; -fx-border-width: 1; -fx-border-radius: 9; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 18, 0, 0, 6); -fx-font-family: 'Microsoft YaHei UI', 'Microsoft YaHei', 'SimSun'; -fx-font-size: 13px;");
        menuContent.setPadding(new Insets(6, 0, 6, 0));

        // 5. 获取鼠标真实坐标，并计算弹出方向
        Point pointer = MouseInfo.getPointerInfo().getLocation();
        boolean openedFromOverflow = WindowsClickThrough.isNotifyIconOverflowVisible();
        
        this.trayMenu = new Popup();
        this.trayMenu.setAutoHide(true);
        this.trayMenu.setHideOnEscape(true);
        this.trayMenu.getContent().add(menuContent);
        
        this.trayMenu.setOnHidden(e -> stopTrayMenuWatcher());
        this.trayMenu.show(this.owner, pointer.x + 8, pointer.y - 8);
        
        startTrayMenuWatcher(openedFromOverflow);
    }

    /**
     * 辅助工厂：生成带有悬浮变色特效的标签作为菜单项。
     */
    private Label trayMenuItem(String text) {
        Label item = new Label(text);
        item.setMinWidth(132);
        item.setMinHeight(30);
        item.setPadding(new Insets(7, 18, 7, 18));
        item.setCursor(Cursor.HAND);
        
        String defaultStyle = "-fx-text-fill: #1F2328; -fx-background-radius: 6;";
        String hoverStyle = "-fx-background-color: #F3F6FA; -fx-text-fill: #1F2328; -fx-background-radius: 6;";
        
        item.setStyle(defaultStyle);
        item.setOnMouseEntered(e -> item.setStyle(hoverStyle));
        item.setOnMouseExited(e -> item.setStyle(defaultStyle));
        
        return item;
    }

    /**
     * 启动折叠菜单监视器：
     * 由于 JavaFX Popup 无法感知到 Windows 托盘折叠菜单（Overflow Window）收起的状态，
     * 我们通过定时轮询底层系统 API，如果发现外壳折叠菜单被收起了，就立刻强制隐藏我们的 Popup。
     */
    private void startTrayMenuWatcher(boolean openedFromOverflow) {
        if (!openedFromOverflow) {
            return;
        }
        
        this.trayMenuWatcher = new Timeline(new KeyFrame(Duration.millis(150), e -> {
            boolean shouldHide = this.trayMenu != null 
                    && this.trayMenu.isShowing() 
                    && !WindowsClickThrough.isNotifyIconOverflowVisible();
                    
            if (shouldHide) {
                this.trayMenu.hide();
            }
        }));
        
        this.trayMenuWatcher.setCycleCount(Timeline.INDEFINITE);
        this.trayMenuWatcher.play();
    }

    /**
     * 关闭监视器定时器。
     */
    private void stopTrayMenuWatcher() {
        if (this.trayMenuWatcher != null) {
            this.trayMenuWatcher.stop();
            this.trayMenuWatcher = null;
        }
    }

    /**
     * 解析或者生成托盘显示的像素图标。
     */
    private BufferedImage createTrayImage() {
        try (InputStream in = DesktopTrayController.class.getResourceAsStream(APP_ICON_RESOURCE)) {
            if (in != null) {
                BufferedImage image = ImageIO.read(in);
                if (image != null) {
                    return image;
                }
            }
        } catch (IOException e) {
            // 解析失败时优雅降级，由代码在内存中画一个简单的兜底图标
        }
        
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
