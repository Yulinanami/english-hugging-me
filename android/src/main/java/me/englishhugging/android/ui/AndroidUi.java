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
 * XML 页面仍需共享的少量运行时 UI 行为。
 *
 * <p>静态尺寸、颜色和圆角已经迁移到资源文件；这里只保留无法完全静态化的图标字体、
 * 下拉适配器和尺寸换算，避免再次把页面布局堆回 Java。</p>
 */
public final class AndroidUi {
    /** 创建字体和适配器所需的应用上下文。 */
    private final Context context;

    /** 延迟加载的 Material Icons 字体，所有图标控件共享同一实例。 */
    private Typeface materialIconFont;

    /** 创建与当前 Activity 资源配置绑定的 UI 辅助对象。 */
    public AndroidUi(Context context) {
        this.context = context;
    }

    /**
     * 获取用于渲染文字图标的字体；资源缺失时退回系统字体，避免界面崩溃。
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

    /** 为 XML 中声明的图标 TextView 或 MaterialButton 设置图标字体。 */
    public void styleIcon(TextView icon) {
        icon.setTypeface(getIconFont());
    }

    public void bindDropdown(MaterialAutoCompleteTextView dropdown, String[] values) {
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
        long[] lastDismissTime = new long[1];
        dropdown.setOnDismissListener(() -> lastDismissTime[0] = android.os.SystemClock.elapsedRealtime());
        dropdown.setOnClickListener(view -> {
            if (android.os.SystemClock.elapsedRealtime() - lastDismissTime[0] < 250) {
                return;
            }
            adapter.getFilter().filter(null);
            dropdown.showDropDown();
        });
    }

    /**
     * 返回下拉框当前合法值；用户输入不在集合内时使用指定默认值。
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

    /** 返回下拉框当前值在候选集合中的索引，未匹配时回退到第一项。 */
    public int selectedIndex(MaterialAutoCompleteTextView dropdown, String[] values) {
        String value = dropdown.getText().toString();
        for (int index = 0; index < values.length; index++) {
            if (values[index].equals(value)) {
                return index;
            }
        }
        return 0;
    }

    /** 将布局使用的 dp 转换为当前屏幕密度对应的物理像素。 */
    public int dp(int value) {
        return (int) (value * this.context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
