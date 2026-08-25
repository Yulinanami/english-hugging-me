package me.englishhugging.android.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.Arrays;

import me.englishhugging.android.R;

/**
 * Android 端界面辅助工具。
 *
 * <p>这个类专门用来处理那些无法完全写在 XML 布局文件里的界面操作，比如：
 * <ul>
 *   <li>加载和设置 Material Icons 矢量图标字体；</li>
 *   <li>给下拉选择框绑定全部选项数据，并处理好点击展开与收起的交互；</li>
 *   <li>把代码里的 dp 尺寸换算成当前手机屏幕的真实像素大小（px）。</li>
 * </ul>
 *
 * <p><b>Usage Example:</b>
 * <pre><code>
 * // 在 Activity 或 Tab 中创建 UI 助手
 * AndroidUi ui = new AndroidUi(context);
 *
 * // 1. 给图标按钮应用图标字体
 * ui.styleIcon(binding.homeIcon);
 *
 * // 2. 绑定下拉框候选数据
 * String[] modes = new String[]{"顺序播放", "完全随机", "随机不重复"};
 * ui.bindDropdown(binding.modeDropdown, modes);
 *
 * // 3. 将 16dp 转换为屏幕像素
 * int paddingPx = ui.dp(16);
 * </code></pre>
 */
public final class AndroidUi {
    /** 上下文对象，用于读取资源和系统配置 */
    private final Context context;

    /** 缓存已加载的 Material Icons 图标字体，避免重复读取 */
    private Typeface materialIconFont;

    /**
     * 创建界面辅助工具对象。
     *
     * @param context 当前界面的上下文对象
     */
    public AndroidUi(Context context) {
        this.context = context;
    }

    /**
     * 获取用于渲染纯文字图标的 Material Icons 字体。
     *
     * <p>如果字体文件加载失败，会自动使用系统默认字体，防止应用崩溃。
     *
     * @return 加载成功的字体对象，或默认的系统字体
     */
    public Typeface getIconFont() {
        if (this.materialIconFont == null) {
            try {
                this.materialIconFont = Typeface.createFromAsset(
                        this.context.getAssets(),
                        "fonts/MaterialIcons-Regular.ttf"
                );
            } catch (RuntimeException ignored) {
                this.materialIconFont = Typeface.DEFAULT;
            }
        }
        return this.materialIconFont;
    }

    /**
     * 为指定的文本控件或按钮应用图标字体，使其能够正常显示图标。
     *
     * @param icon 需要应用图标字体的文本或按钮控件
     */
    public void styleIcon(TextView icon) {
        icon.setTypeface(getIconFont());
    }

    /**
     * 为下拉选择框绑定候选项数据与点击事件。
     *
     * <p>1. 禁用输入过滤，保证点击展开时始终显示全部选项；
     * <p>2. 记录关闭时间戳，防止点击过快导致重复展开。
     *
     * @param dropdown 目标下拉框控件
     * @param values   候选项字符串数组
     */
    public void bindDropdown(MaterialAutoCompleteTextView dropdown, String[] values) {
        // 禁用输入过滤，确保每次点击都展示全部选项
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this.context,
                R.layout.item_dropdown,
                new ArrayList<>(Arrays.asList(values))
        ) {
            @Override
            public Filter getFilter() {
                return new Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();
                        results.values = values;
                        results.count = values.length;
                        return results;
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
        dropdown.setAdapter(adapter);
        dropdown.setThreshold(0);

        // 记录最近一次下拉列表收起的时间（毫秒）
        long[] lastDismissTime = new long[1];
        dropdown.setOnDismissListener(() -> lastDismissTime[0] = android.os.SystemClock.elapsedRealtime());

        // 避免频繁连续点击：如果刚触发了收起（< 250ms），则不重新展开下拉列表
        dropdown.setOnClickListener(view -> {
            if (android.os.SystemClock.elapsedRealtime() - lastDismissTime[0] < 250) {
                return;
            }
            adapter.getFilter().filter(null);
            dropdown.showDropDown();
        });
    }

    /**
     * 获取下拉框当前选中的合法文本值；如果用户填写了不在候选项中的非法内容，则安全返回指定的默认值。
     *
     * @param dropdown 目标下拉输入框
     * @param values   所有合法的候选项字符串数组
     * @param fallback 当输入框内容不在 values 中时的默认值
     * @return 校验后的有效字符串
     */
    public String selectedValue(
            MaterialAutoCompleteTextView dropdown,
            String[] values,
            String fallback
    ) {
        String value = dropdown.getText().toString();
        for (String item : values) {
            if (item.equals(value)) {
                return item;
            }
        }
        return fallback;
    }

    /**
     * 获取下拉框当前选中文本在候选项数组中的位置；若未匹配到任何项，则默认使用第 0 项。
     *
     * @param dropdown 目标下拉输入框
     * @param values   候选项字符串数组
     * @return 选中文本在数组中的位置（0 ~ values.length - 1）
     */
    public int selectedIndex(MaterialAutoCompleteTextView dropdown, String[] values) {
        String value = dropdown.getText().toString();
        for (int index = 0; index < values.length; index++) {
            if (values[index].equals(value)) {
                return index;
            }
        }
        return 0;
    }

    /**
     * 把 dp 尺寸换算为当前手机屏幕的像素大小（px）。
     *
     * @param value 以 dp 为单位的尺寸数值
     * @return 换算后的像素值（px）
     */
    public int dp(int value) {
        return (int) (value * this.context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
