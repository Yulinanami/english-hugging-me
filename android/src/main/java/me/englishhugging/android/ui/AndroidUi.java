package me.englishhugging.android.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    /**
     * 给 XML 中的下拉控件绑定完整选项，并保证点击或获得焦点时展示全部内容。
     *
     * @param dropdown 需要初始化的下拉输入框
     * @param values   下拉框允许选择的完整值集合
     */
    public void bindDropdown(MaterialAutoCompleteTextView dropdown, String[] values) {
        ArrayAdapter<String> adapter = dropdownAdapter(values);
        dropdown.setAdapter(adapter);
        dropdown.setThreshold(0);
        dropdown.setInputType(InputType.TYPE_NULL);
        dropdown.setDropDownHeight(Math.min(dp(260), Math.max(dp(48), values.length * dp(54))));

        // 每次展开前清空过滤条件，防止当前文字把其它选项过滤掉。
        dropdown.setOnClickListener(view -> {
            adapter.getFilter().filter(null);
            dropdown.showDropDown();
        });
        dropdown.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                adapter.getFilter().filter(null);
                dropdown.showDropDown();
            }
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

    /**
     * 创建不会因输入文字而缩减结果集的适配器。
     *
     * <p>设置页下拉框用于选择固定配置，不承担搜索功能，因此过滤时始终发布完整列表。</p>
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
                        // 无论约束文字是什么，都返回原始固定选项。
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
    }

    /** 统一设置下拉框当前项和弹出列表项的字体颜色、字号与内边距。 */
    private View styleDropdown(View view) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            textView.setTextColor(this.context.getColor(R.color.text_primary));
            textView.setTextSize(15);
            textView.setPadding(dp(16), dp(12), dp(16), dp(12));
        }
        return view;
    }
}
