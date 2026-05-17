package me.englishhugging.android;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import me.englishhugging.core.AppSettings;
import me.englishhugging.core.DisplayMode;
import me.englishhugging.core.OverlayMode;

public final class MainActivity extends Activity {
    private Spinner vocabularySpinner;
    private Spinner displayModeSpinner;
    private Spinner overlayModeSpinner;
    private EditText intervalSeconds;
    private SeekBar opacitySeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestNotificationPermissionIfNeeded();
        setContentView(createContentView());
        bindSettings(AndroidSettingsStore.load(this));
    }

    private LinearLayout createContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("English Hugging Me");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWidthWrapHeight());

        vocabularySpinner = addSpinner(root, "词库", AndroidSettingsStore.VOCABULARY_FILES);
        displayModeSpinner = addSpinner(root, "显示内容", enumNames(DisplayMode.values()));
        overlayModeSpinner = addSpinner(root, "悬浮行为", enumNames(OverlayMode.values()));

        root.addView(label("切换间隔（秒）"));
        intervalSeconds = new EditText(this);
        intervalSeconds.setInputType(InputType.TYPE_CLASS_NUMBER);
        root.addView(intervalSeconds, matchWidthWrapHeight());

        root.addView(label("透明度"));
        opacitySeekBar = new SeekBar(this);
        opacitySeekBar.setMax(80);
        root.addView(opacitySeekBar, matchWidthWrapHeight());

        Button start = new Button(this);
        start.setText("启动悬浮背词");
        start.setOnClickListener(view -> startOverlay());
        root.addView(start, matchWidthWrapHeight());

        Button stop = new Button(this);
        stop.setText("停止悬浮背词");
        stop.setOnClickListener(view -> stopService(new Intent(this, OverlayService.class)));
        root.addView(stop, matchWidthWrapHeight());

        TextView note = new TextView(this);
        note.setText("这不是 Web 页面；启动后词汇会悬浮在其它 App 上方。");
        note.setGravity(Gravity.CENTER);
        root.addView(note, matchWidthWrapHeight());

        return root;
    }

    private Spinner addSpinner(LinearLayout root, String label, String[] values) {
        root.addView(label(label));
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values));
        root.addView(spinner, matchWidthWrapHeight());
        return spinner;
    }

    private TextView label(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(14);
        label.setPadding(0, 20, 0, 4);
        return label;
    }

    private void bindSettings(AppSettings settings) {
        vocabularySpinner.setSelection(AndroidSettingsStore.vocabularyIndex(settings.getVocabularyFileName()));
        displayModeSpinner.setSelection(settings.getDisplayMode().ordinal());
        overlayModeSpinner.setSelection(settings.getOverlayMode().ordinal());
        intervalSeconds.setText(Integer.toString(settings.getIntervalSeconds()));
        opacitySeekBar.setProgress((int) Math.round(settings.getOpacity() * 100) - 20);
    }

    private void startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            startActivity(intent);
            Toast.makeText(this, "请先允许悬浮窗权限", Toast.LENGTH_LONG).show();
            return;
        }

        AppSettings settings = collectSettings();
        AndroidSettingsStore.save(this, settings);
        Intent intent = new Intent(this, OverlayService.class);
        intent.setAction(OverlayService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private AppSettings collectSettings() {
        AppSettings settings = AndroidSettingsStore.load(this);
        settings.setVocabularyFileName(AndroidSettingsStore.VOCABULARY_FILES[vocabularySpinner.getSelectedItemPosition()]);
        settings.setDisplayMode(DisplayMode.values()[displayModeSpinner.getSelectedItemPosition()]);
        settings.setOverlayMode(OverlayMode.values()[overlayModeSpinner.getSelectedItemPosition()]);
        try {
            settings.setIntervalSeconds(Integer.parseInt(intervalSeconds.getText().toString()));
        } catch (RuntimeException ignored) {
            settings.setIntervalSeconds(8);
        }
        settings.setOpacity((opacitySeekBar.getProgress() + 20) / 100.0);
        return settings;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    private static ViewGroup.LayoutParams matchWidthWrapHeight() {
        return new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private static String[] enumNames(Enum<?>[] values) {
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].name();
        }
        return names;
    }
}
