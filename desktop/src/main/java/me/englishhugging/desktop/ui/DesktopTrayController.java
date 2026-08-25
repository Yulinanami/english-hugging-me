package me.englishhugging.desktop.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
 * 桌面系统托盘图标及自定义右键菜单。
 *
 * <p>这个类负责在系统任务栏通知区域创建托盘图标，并在用户点击时弹出菜单（支持打开设置、退出程序等操作）。
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * DesktopTrayController tray = new DesktopTrayController(stage, () -> openSettings(), () -> exit());
 * if (tray.install()) {
 *     // 托盘安装成功，应用可在后台运行
 * }
 * </code></pre>
 */
public final class DesktopTrayController {
    
    /** 托盘图标资源路径 */
    private static final String APP_ICON_RESOURCE = "/icons/app.png";

    /** 弹出菜单所属的主窗口 */
    private final Stage owner;
    /** 点击“打开设置”时的回调 */
    private final Runnable openSettings;
    /** 点击“退出程序”时的回调 */
    private final Runnable exitApplication;
    
    /** 系统托盘图标对象 */
    private TrayIcon trayIcon;
    /** 托盘弹出菜单 */
    private Popup trayMenu;
    /** 鼠标离开托盘菜单时自动关闭的定时任务 */
    private Timeline trayMenuWatcher;

    /**
     * 创建系统托盘。
     *
     * @param owner           弹出菜单所属的主窗口
     * @param openSettings    点击“打开设置”时的回调
     * @param exitApplication 点击“退出”时的回调
     */
    public DesktopTrayController(Stage owner, Runnable openSettings, Runnable exitApplication) {
        this.owner = owner;
        this.openSettings = openSettings;
        this.exitApplication = exitApplication;
    }

    /**
     * 隐藏菜单并执行对应动作。
     */
    private void hideAndRun(Runnable action) {
        if (this.trayMenu != null) {
            this.trayMenu.hide();
        }
        action.run();
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
     * 将托盘图标从系统中移除并关闭弹出菜单。
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

        VBox menuContent = DesktopUi.loadFxml("/fxml/tray-menu.fxml");
        menuContent.getChildren().get(0).setOnMouseClicked(e -> hideAndRun(this.openSettings));
        menuContent.getChildren().get(2).setOnMouseClicked(e -> hideAndRun(this.exitApplication));

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
     * 监听 Windows 任务栏折叠菜单状态。
     * 当任务栏折叠区域关闭时，同步隐藏右键托盘菜单。
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
     * 停止托盘菜单状态监听定时任务。
     */
    private void stopTrayMenuWatcher() {
        if (this.trayMenuWatcher != null) {
            this.trayMenuWatcher.stop();
            this.trayMenuWatcher = null;
        }
    }

    /**
     * 加载应用图标，加载失败则动态生成备用图标。
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
            // 图标读取失败时，动态绘制一个备用图标
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
