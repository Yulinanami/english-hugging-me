package me.englishhugging.android.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Android 端的纯代码 UI 组件工厂与设计系统。
 *
 * <p>这个类充当了 Android 端的所有“原子组件”（Atom Components）的生成器。
 * 我们完全抛弃了繁琐且难以复用的 XML 布局文件，所有的边距、圆角、颜色、字体
 * 全部都在这里通过纯 Java 代码进行集中管理，从而实现了像素级的一致性和极速的渲染性能。
 *
 * <p><b>设计语言规范：</b>
 * 采用类似 iOS 和现代 Web 的新拟态/扁平化混合设计。
 * 大量使用纯白卡片、浅灰背景、大圆角以及低饱和度的莫兰迪色系。
 */
public final class AndroidUi {
    
    // --- 全局设计令牌 (Design Tokens) ---
    public static final int PAGE_BACKGROUND = Color.rgb(250, 248, 255);
    public static final int CARD_BACKGROUND = Color.rgb(246, 244, 251);
    public static final int PRIMARY = Color.rgb(82, 105, 154);
    public static final int TEXT_PRIMARY = Color.rgb(39, 43, 54);
    public static final int TEXT_SECONDARY = Color.rgb(95, 96, 110);

    private final Context context;
    private Typeface materialIconFont;

    /**
     * 绑定到一个 Context 上以换取尺寸计算能力。
     */
    public AndroidUi(Context context) {
        this.context = context;
    }

    /**
     * 懒加载并获取 Material Icons 字体库，
     * 用于通过纯文本渲染所有的矢量图标，极大地减小了 APK 体积。
     */
    public Typeface getIconFont() {
        if (this.materialIconFont == null) {
            try {
                this.materialIconFont = Typeface.createFromAsset(this.context.getAssets(), "fonts/MaterialIcons-Regular.ttf");
            } catch (Exception e) {
                // 如果资产丢失，优雅降级到系统默认字体
                this.materialIconFont = Typeface.DEFAULT;
            }
        }
        return this.materialIconFont;
    }

    /**
     * 底部导航栏 Tab 专用的布局参数（等分宽度）。
     */
    public LinearLayout.LayoutParams tabLayoutParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1);
        p.setMargins(dp(2), 0, dp(2), 0);
        return p;
    }

    /**
     * 最常用的填满宽度、自适应高度的布局参数。
     */
    public ViewGroup.LayoutParams matchWidthWrapHeight() {
        return new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /**
     * 填满宽度、自适应高度，并带有一个向下的间隔。
     */
    public LinearLayout.LayoutParams matchWidthWithBottomMargin(int bottomDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, dp(bottomDp));
        return p;
    }

    /**
     * 填满宽度、自适应高度，并带有一个向上的间隔。
     */
    public LinearLayout.LayoutParams matchWidthWithTopMargin(int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(topDp), 0, 0);
        return p;
    }

    /**
     * 动态计算系统状态栏的物理像素高度，用于实现沉浸式布局时的顶部留白。
     */
    public int getStatusBarHeight() {
        int resourceId = this.context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return this.context.getResources().getDimensionPixelSize(resourceId);
        } else {
            return dp(24);
        }
    }

    /**
     * 核心工具方法：将逻辑像素（DP）转换为设备物理像素（PX）。
     */
    public int dp(int value) {
        return (int) (value * this.context.getResources().getDisplayMetrics().density + 0.5f);
    }

    /**
     * 生成一个具有纯色背景和指定圆角的可复用 Drawable。
     */
    public GradientDrawable rounded(int color, int strokeColor, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        
        if (strokeColor != Color.TRANSPARENT) {
            d.setStroke(dp(1), strokeColor);
        }
        return d;
    }

    /**
     * 生成一个完美的正圆形纯色背景。
     */
    public GradientDrawable oval(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    /**
     * 构造页面顶部的标准大标题栏（可带一个操作图标）。
     */
    public LinearLayout headerRow(String title, String actionIcon) {
        LinearLayout header = new LinearLayout(this.context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView titleView = new TextView(this.context);
        titleView.setText(title);
        titleView.setTextSize(34);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextColor(TEXT_PRIMARY);
        
        header.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        
        if (actionIcon != null && actionIcon.length() > 0) {
            TextView icon = new TextView(this.context);
            icon.setText(actionIcon);
            icon.setTextSize(24);
            icon.setTypeface(getIconFont());
            icon.setTextColor(TEXT_SECONDARY);
            icon.setGravity(Gravity.CENTER);
            icon.setBackground(rounded(Color.rgb(243, 241, 248), Color.rgb(226, 224, 234), dp(22)));
            icon.setPadding(dp(10), dp(10), dp(10), dp(10));
            
            header.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));
        }
        
        return header;
    }

    /**
     * 首页专用的数据指标柱（上下结构：标签 + 巨大数字）。
     */
    public LinearLayout homeMetric(String label, String value) {
        LinearLayout column = new LinearLayout(this.context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);
        
        TextView labelView = bodyText(label);
        labelView.setGravity(Gravity.CENTER);
        
        TextView valueView = titleText(value);
        valueView.setGravity(Gravity.CENTER);
        
        column.addView(labelView, matchWidthWrapHeight());
        column.addView(valueView, matchWidthWrapHeight());
        
        return column;
    }

    /**
     * 列表分段的蓝色小标题。
     */
    public TextView sectionLabel(String text) {
        TextView label = new TextView(this.context);
        label.setText(text);
        label.setTextSize(18);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(PRIMARY);
        label.setPadding(dp(8), 0, 0, 0);
        return label;
    }

    /**
     * 粗体的主标题文字。
     */
    public TextView titleText(String text) {
        TextView t = new TextView(this.context);
        t.setText(text);
        t.setTextSize(22);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(TEXT_PRIMARY);
        return t;
    }

    /**
     * 普通的深灰色正文段落文字。
     */
    public TextView bodyText(String text) {
        TextView t = new TextView(this.context);
        t.setText(text);
        t.setTextSize(16);
        t.setTextColor(TEXT_SECONDARY);
        t.setLineSpacing(dp(2), 1.0f);
        return t;
    }

    /**
     * 将 Material Icon 文本包裹成一个圆形小图标组件。
     */
    public TextView circularIcon(String iconName, int background, int foreground) {
        TextView icon = new TextView(this.context);
        icon.setText(iconName);
        icon.setTextSize(26);
        icon.setTypeface(getIconFont());
        icon.setTextColor(foreground);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(oval(background));
        return icon;
    }

    /**
     * 生成一个带超大圆角（28dp）的灰色背景包裹卡片。
     */
    public LinearLayout card() {
        LinearLayout card = new LinearLayout(this.context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(rounded(CARD_BACKGROUND, Color.TRANSPARENT, dp(28)));
        return card;
    }

    /**
     * 组装标准的设置项（左侧标题+副标题，右侧控件，或者是上下排列）。
     */
    public View settingItem(String title, String subtitle, View control) {
        LinearLayout item = new LinearLayout(this.context);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(0, dp(10), 0, dp(14));
        
        LinearLayout.LayoutParams controlParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        controlParams.setMargins(0, dp(10), 0, 0);
        
        item.addView(itemTitleText(title), matchWidthWrapHeight());
        item.addView(itemSubtitleText(subtitle), matchWidthWrapHeight());
        item.addView(control, controlParams);
        
        return item;
    }

    /**
     * 专门用于包裹 Switch 开关按钮的左右排列设置项。
     */
    public View settingSwitchItem(String title, String subtitle, View switchControl) {
        LinearLayout item = new LinearLayout(this.context);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(0, dp(10), 0, dp(14));
        
        LinearLayout textLayout = new LinearLayout(this.context);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.addView(itemTitleText(title), matchWidthWrapHeight());
        textLayout.addView(itemSubtitleText(subtitle), matchWidthWrapHeight());
        
        item.addView(textLayout, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        item.addView(switchControl, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        
        return item;
    }

    /**
     * 在播放记录列表里渲染一行（左边播放小图标，右边文字描述）。
     */
    public View recordRow(String text) {
        LinearLayout row = new LinearLayout(this.context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(14), 0, dp(14));
        
        row.addView(circularIcon("play_arrow", Color.rgb(232, 232, 238), PRIMARY), new LinearLayout.LayoutParams(dp(52), dp(52)));
        
        TextView value = bodyText(text);
        value.setTextSize(15);
        value.setPadding(dp(14), 0, 0, 0);
        
        row.addView(value, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    /**
     * 构造悬浮底部导航栏专用的单色扁平化图标按钮。
     */
    public MaterialButton tabButton(String text) {
        MaterialButton button = new MaterialButton(this.context);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(24);
        button.setTypeface(getIconFont());
        button.setGravity(Gravity.CENTER);
        
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setStrokeWidth(0);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setMinimumWidth(0);
        button.setMinimumHeight(0);
        
        clearElevation(button);
        return button;
    }

    /**
     * 高亮或熄灭某一个底部导航 Tab 按钮。
     */
    public void styleTab(MaterialButton button, boolean selected) {
        button.setAllCaps(false);
        button.setTextColor(selected ? Color.WHITE : Color.rgb(98, 99, 110));
        button.setTextSize(24);
        button.setTypeface(getIconFont());
        button.setCornerRadius(dp(20));
        button.setStrokeWidth(0);
        button.setBackgroundTintList(ColorStateList.valueOf(selected ? PRIMARY : Color.TRANSPARENT));
        clearElevation(button);
    }    
    
    /**
     * 构建页面中次级的扁平化行动按钮（如：清除记录、编辑等）。
     */
    public MaterialButton secondaryButton(String text) {
        MaterialButton button = new MaterialButton(this.context);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(PRIMARY);
        button.setTextSize(16);
        button.setCornerRadius(dp(16));
        button.setStrokeWidth(0);
        button.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(235, 238, 249)));
        button.setPadding(0, dp(9), 0, dp(9));
        
        clearElevation(button);
        return button;
    }
    
    /**
     * 生成一个苹果风格的下拉选择框。
     */
    public MaterialAutoCompleteTextView dropdown(String[] values) {
        MaterialAutoCompleteTextView dropdown = new MaterialAutoCompleteTextView(this.context);
        ArrayAdapter<String> adapter = dropdownAdapter(values);
        
        dropdown.setAdapter(adapter);
        
        if (values.length > 0) {
            dropdown.setText(values[0], false);
        } else {
            dropdown.setText("", false);
        }
        
        dropdown.setThreshold(0);
        dropdown.setInputType(InputType.TYPE_NULL);
        dropdown.setSingleLine(true);
        dropdown.setTextSize(14);
        dropdown.setTextColor(TEXT_PRIMARY);
        dropdown.setHintTextColor(TEXT_SECONDARY);
        dropdown.setPadding(dp(12), dp(9), dp(12), dp(9));
        
        dropdown.setBackground(rounded(Color.WHITE, Color.rgb(218, 216, 226), dp(14)));
        dropdown.setDropDownBackgroundDrawable(rounded(Color.WHITE, Color.TRANSPARENT, dp(14)));
        dropdown.setDropDownHeight(Math.min(dp(260), Math.max(dp(48), values.length * dp(54))));
        
        dropdown.setOnClickListener(v -> { 
            adapter.getFilter().filter(null); 
            dropdown.showDropDown(); 
        });
        
        dropdown.setOnFocusChangeListener((v, hasFocus) -> { 
            if (hasFocus) { 
                adapter.getFilter().filter(null); 
                dropdown.showDropDown(); 
            } 
        });
        
        return dropdown;
    }

    /**
     * 生成一个通用的单行文本输入框。
     */
    public EditText input(String value) {
        EditText input = new EditText(this.context);
        input.setText(value);
        input.setSingleLine(true);
        input.setTextColor(TEXT_PRIMARY);
        input.setTextSize(15);
        input.setPadding(dp(10), dp(6), dp(10), dp(6));
        input.setBackground(rounded(Color.WHITE, Color.rgb(218, 216, 226), dp(12)));
        return input;
    }

    /**
     * 将一个输入框包裹，左右加上减号和加号按钮，变成数字微调器（Stepper）。
     */
    public LinearLayout numberAdjuster(EditText input, int step, int min, int max) {
        LinearLayout layout = new LinearLayout(this.context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        
        // 减号按钮
        MaterialButton minusBtn = new MaterialButton(this.context);
        minusBtn.setText("-");
        minusBtn.setTextColor(TEXT_PRIMARY);
        minusBtn.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        minusBtn.setStrokeColor(ColorStateList.valueOf(Color.rgb(200, 200, 200)));
        minusBtn.setStrokeWidth(dp(1));
        minusBtn.setMinWidth(0);
        minusBtn.setMinHeight(0);
        minusBtn.setMinimumWidth(0);
        minusBtn.setMinimumHeight(0);
        minusBtn.setCornerRadius(dp(18));
        minusBtn.setPadding(0, 0, 0, 0);
        clearElevation(minusBtn);
        
        minusBtn.setOnClickListener(v -> {
            try {
                int val = Integer.parseInt(input.getText().toString());
                if (val > min) {
                    input.setText(String.valueOf(val - step));
                }
            } catch (Exception ignored) {
                // 如果用户乱输导致转换异常，直接忽略
            }
        });
        
        // 加号按钮
        MaterialButton plusBtn = new MaterialButton(this.context);
        plusBtn.setText("+");
        plusBtn.setTextColor(TEXT_PRIMARY);
        plusBtn.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        plusBtn.setStrokeColor(ColorStateList.valueOf(Color.rgb(200, 200, 200)));
        plusBtn.setStrokeWidth(dp(1));
        plusBtn.setMinWidth(0);
        plusBtn.setMinHeight(0);
        plusBtn.setMinimumWidth(0);
        plusBtn.setMinimumHeight(0);
        plusBtn.setCornerRadius(dp(18));
        plusBtn.setPadding(0, 0, 0, 0);
        clearElevation(plusBtn);
        
        plusBtn.setOnClickListener(v -> {
            try {
                int val = Integer.parseInt(input.getText().toString());
                if (val < max) {
                    input.setText(String.valueOf(val + step));
                }
            } catch (Exception ignored) {
                // 异常忽略
            }
        });

        // 整理中间的输入框
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setGravity(Gravity.CENTER);
        input.setBackground(rounded(Color.rgb(243, 241, 248), Color.rgb(226, 224, 234), dp(8)));
        input.setPadding(dp(4), dp(4), dp(4), dp(4));
        
        // 组装并设置边距
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        layout.addView(minusBtn, btnParams);
        
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(dp(60), ViewGroup.LayoutParams.WRAP_CONTENT);
        inputParams.setMargins(dp(4), 0, dp(4), 0);
        layout.addView(input, inputParams);
        
        layout.addView(plusBtn, btnParams);
        
        return layout;
    }

    /**
     * 获取下拉菜单当前选中的值。
     */
    public String selectedValue(MaterialAutoCompleteTextView dropdown, String[] values, String fallback) {
        String value = dropdown.getText().toString();
        for (String item : values) { 
            if (item.equals(value)) {
                return item; 
            }
        }
        return fallback;
    }

    /**
     * 获取下拉菜单当前选中的索引位置。
     */
    public int selectedIndex(MaterialAutoCompleteTextView dropdown, String[] values) {
        String value = dropdown.getText().toString();
        for (int i = 0; i < values.length; i++) { 
            if (values[i].equals(value)) {
                return i; 
            }
        }
        return 0;
    }

    /**
     * 配置项的主标题模板。
     */
    private TextView itemTitleText(String text) {
        TextView t = new TextView(this.context);
        t.setText(text);
        t.setTextSize(17);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(TEXT_PRIMARY);
        return t;
    }

    /**
     * 配置项的副标题模板。
     */
    private TextView itemSubtitleText(String text) {
        TextView t = new TextView(this.context);
        t.setText(text);
        t.setTextSize(13);
        t.setTextColor(TEXT_SECONDARY);
        return t;
    }

    /**
     * 针对 MaterialAutoCompleteTextView 内部数据进行定制的适配器，
     * 重写 Filter 逻辑以确保无论输入什么都能直接展示全量下拉列表。
     */
    private ArrayAdapter<String> dropdownAdapter(String[] values) {
        List<String> items = new ArrayList<>(Arrays.asList(values));
        
        return new ArrayAdapter<String>(this.context, android.R.layout.simple_list_item_1, items) {
            @Override 
            public View getView(int position, View convertView, ViewGroup parent) { 
                return styleDropdown(super.getView(position, convertView, parent)); 
            }
            
            @Override 
            public View getDropDownView(int position, View convertView, ViewGroup parent) { 
                return styleDropdown(super.getDropDownView(position, convertView, parent)); 
            }
            
            @Override 
            public Filter getFilter() {
                return new Filter() {
                    @Override 
                    protected FilterResults performFiltering(CharSequence constraint) { 
                        FilterResults r = new FilterResults(); 
                        r.values = values; 
                        r.count = values.length; 
                        return r; 
                    }
                    
                    @Override 
                    protected void publishResults(CharSequence constraint, FilterResults results) { 
                        clear(); 
                        addAll(values); 
                        notifyDataSetChanged(); 
                    }
                };
            }
        };
    }

    /**
     * 修改原生 Android 下拉菜单中每一行 Item 的颜色和内边距，使之符合应用基调。
     */
    private View styleDropdown(View view) {
        if (view instanceof TextView) {
            TextView t = (TextView) view;
            t.setTextColor(TEXT_PRIMARY);
            t.setTextSize(15);
            t.setPadding(dp(16), dp(12), dp(16), dp(12));
        }
        return view;
    }

    /**
     * 剥夺 MaterialButton 所有的自带 Z 轴阴影和点击水波纹动画效果，使其变得纯粹扁平。
     */
    private void clearElevation(MaterialButton button) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            button.setElevation(0);
            button.setTranslationZ(0);
            button.setStateListAnimator(null);
        }
    }
}
