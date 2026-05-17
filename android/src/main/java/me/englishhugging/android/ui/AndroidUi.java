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

public final class AndroidUi {
    public static final int PAGE_BACKGROUND = Color.rgb(250, 248, 255);
    public static final int CARD_BACKGROUND = Color.rgb(246, 244, 251);
    public static final int PRIMARY = Color.rgb(82, 105, 154);
    public static final int TEXT_PRIMARY = Color.rgb(39, 43, 54);
    public static final int TEXT_SECONDARY = Color.rgb(95, 96, 110);

    private final Context context;
    private Typeface materialIconFont;

    public AndroidUi(Context context) { this.context = context; }

    public Typeface getIconFont() {
        if (materialIconFont == null) {
            try {
                materialIconFont = Typeface.createFromAsset(context.getAssets(), "fonts/MaterialIcons-Regular.ttf");
            } catch (Exception e) {
                materialIconFont = Typeface.DEFAULT;
            }
        }
        return materialIconFont;
    }

    // --- Layout Params ---

    public LinearLayout.LayoutParams tabLayoutParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1);
        p.setMargins(dp(2), 0, dp(2), 0);
        return p;
    }

    public ViewGroup.LayoutParams matchWidthWrapHeight() {
        return new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public LinearLayout.LayoutParams matchWidthWithBottomMargin(int bottomDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, dp(bottomDp));
        return p;
    }

    public LinearLayout.LayoutParams matchWidthWithTopMargin(int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(topDp), 0, 0);
        return p;
    }

    public int getStatusBarHeight() {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return resourceId > 0 ? context.getResources().getDimensionPixelSize(resourceId) : dp(24);
    }

    public int dp(int value) { return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f); }

    // --- Drawables ---

    public GradientDrawable rounded(int color, int strokeColor, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        if (strokeColor != Color.TRANSPARENT) d.setStroke(dp(1), strokeColor);
        return d;
    }

    public GradientDrawable oval(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    // --- Text Views ---

    public LinearLayout headerRow(String title, String actionText) {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(34);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextColor(TEXT_PRIMARY);
        header.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        if (actionText.length() > 0) {
            MaterialButton action = smallActionButton(actionText);
            header.addView(action, new LinearLayout.LayoutParams(dp(86), dp(46)));
        }
        return header;
    }

    public LinearLayout homeMetric(String label, String value) {
        LinearLayout column = new LinearLayout(context);
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

    public TextView sectionLabel(String text) {
        TextView label = new TextView(context);
        label.setText(text);
        label.setTextSize(18);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(PRIMARY);
        label.setPadding(dp(8), 0, 0, 0);
        return label;
    }

    public TextView titleText(String text) {
        TextView t = new TextView(context);
        t.setText(text);
        t.setTextSize(22);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(TEXT_PRIMARY);
        return t;
    }

    public TextView bodyText(String text) {
        TextView t = new TextView(context);
        t.setText(text);
        t.setTextSize(16);
        t.setTextColor(TEXT_SECONDARY);
        t.setLineSpacing(dp(2), 1.0f);
        return t;
    }

    public TextView circularIcon(String iconName, int background, int foreground) {
        TextView icon = new TextView(context);
        icon.setText(iconName);
        icon.setTextSize(26);
        icon.setTypeface(getIconFont());
        icon.setTextColor(foreground);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(oval(background));
        return icon;
    }

    // --- Cards & Items ---

    public LinearLayout card() {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(rounded(CARD_BACKGROUND, Color.TRANSPARENT, dp(28)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) card.setElevation(dp(1));
        return card;
    }

    public View settingItem(String title, String subtitle, View control) {
        LinearLayout item = new LinearLayout(context);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(0, dp(10), 0, dp(14));
        LinearLayout.LayoutParams controlParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        controlParams.setMargins(0, dp(10), 0, 0);
        item.addView(itemTitleText(title), matchWidthWrapHeight());
        item.addView(itemSubtitleText(subtitle), matchWidthWrapHeight());
        item.addView(control, controlParams);
        return item;
    }

    public View recordRow(String text) {
        LinearLayout row = new LinearLayout(context);
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

    // --- Buttons ---

    public MaterialButton tabButton(String text) {
        MaterialButton button = new MaterialButton(context);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(15);
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

    public void styleTab(MaterialButton button, boolean selected) {
        button.setAllCaps(false);
        button.setTextColor(selected ? Color.WHITE : Color.rgb(98, 99, 110));
        button.setTextSize(15);
        button.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        button.setCornerRadius(dp(20));
        button.setStrokeWidth(0);
        button.setBackgroundTintList(ColorStateList.valueOf(selected ? PRIMARY : Color.TRANSPARENT));
        clearElevation(button);
    }

    public MaterialButton primaryButton(String text) {
        MaterialButton button = new MaterialButton(context);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTextSize(16);
        button.setCornerRadius(dp(16));
        button.setStrokeWidth(0);
        button.setBackgroundTintList(ColorStateList.valueOf(PRIMARY));
        button.setPadding(0, dp(9), 0, dp(9));
        return button;
    }

    public MaterialButton secondaryButton(String text) {
        MaterialButton button = new MaterialButton(context);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(PRIMARY);
        button.setTextSize(16);
        button.setCornerRadius(dp(16));
        button.setStrokeWidth(0);
        button.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(235, 238, 249)));
        button.setPadding(0, dp(9), 0, dp(9));
        return button;
    }

    public MaterialButton iconButton(String iconName) {
        MaterialButton button = new MaterialButton(context);
        button.setText(iconName);
        button.setAllCaps(false);
        button.setTextColor(TEXT_PRIMARY);
        button.setTextSize(28);
        button.setTypeface(getIconFont());
        button.setGravity(Gravity.CENTER);
        button.setCornerRadius(dp(28));
        button.setStrokeWidth(0);
        button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        return button;
    }

    // --- Inputs ---

    public MaterialAutoCompleteTextView dropdown(String[] values) {
        MaterialAutoCompleteTextView dropdown = new MaterialAutoCompleteTextView(context);
        ArrayAdapter<String> adapter = dropdownAdapter(values);
        dropdown.setAdapter(adapter);
        dropdown.setText(values.length == 0 ? "" : values[0], false);
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
        dropdown.setOnClickListener(v -> { adapter.getFilter().filter(null); dropdown.showDropDown(); });
        dropdown.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) { adapter.getFilter().filter(null); dropdown.showDropDown(); } });
        return dropdown;
    }

    public EditText input(String value) {
        EditText input = new EditText(context);
        input.setText(value);
        input.setSingleLine(true);
        input.setTextColor(TEXT_PRIMARY);
        input.setTextSize(15);
        input.setPadding(dp(10), dp(6), dp(10), dp(6));
        input.setBackground(rounded(Color.WHITE, Color.rgb(218, 216, 226), dp(12)));
        return input;
    }

    public String selectedValue(MaterialAutoCompleteTextView dropdown, String[] values, String fallback) {
        String value = dropdown.getText().toString();
        for (String item : values) { if (item.equals(value)) return item; }
        return fallback;
    }

    public int selectedIndex(MaterialAutoCompleteTextView dropdown, String[] values) {
        String value = dropdown.getText().toString();
        for (int i = 0; i < values.length; i++) { if (values[i].equals(value)) return i; }
        return 0;
    }

    // --- Private Helpers ---

    private MaterialButton smallActionButton(String text) {
        MaterialButton button = new MaterialButton(context);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(PRIMARY);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setCornerRadius(dp(18));
        button.setStrokeWidth(0);
        button.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(235, 238, 249)));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        return button;
    }

    private TextView itemTitleText(String text) {
        TextView t = new TextView(context);
        t.setText(text);
        t.setTextSize(17);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(TEXT_PRIMARY);
        return t;
    }

    private TextView itemSubtitleText(String text) {
        TextView t = new TextView(context);
        t.setText(text);
        t.setTextSize(13);
        t.setTextColor(TEXT_SECONDARY);
        return t;
    }

    private ArrayAdapter<String> dropdownAdapter(String[] values) {
        List<String> items = new ArrayList<>(Arrays.asList(values));
        return new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, items) {
            @Override public View getView(int position, View convertView, ViewGroup parent) { return styleDropdown(super.getView(position, convertView, parent)); }
            @Override public View getDropDownView(int position, View convertView, ViewGroup parent) { return styleDropdown(super.getDropDownView(position, convertView, parent)); }
            @Override public Filter getFilter() {
                return new Filter() {
                    @Override protected FilterResults performFiltering(CharSequence constraint) { FilterResults r = new FilterResults(); r.values = values; r.count = values.length; return r; }
                    @Override protected void publishResults(CharSequence constraint, FilterResults results) { clear(); addAll(values); notifyDataSetChanged(); }
                };
            }
        };
    }

    private View styleDropdown(View view) {
        if (view instanceof TextView) {
            TextView t = (TextView) view;
            t.setTextColor(TEXT_PRIMARY);
            t.setTextSize(15);
            t.setPadding(dp(16), dp(12), dp(16), dp(12));
        }
        return view;
    }

    private void clearElevation(MaterialButton button) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            button.setElevation(0);
            button.setTranslationZ(0);
            button.setStateListAnimator(null);
        }
    }
}
