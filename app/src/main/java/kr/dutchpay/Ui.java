package kr.dutchpay;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

final class Ui {
    // Premium Toss / KakaoPay Style Palette
    static final int COLOR_BG = Color.parseColor("#F2F4F6");        // Toss soft gray
    static final int COLOR_CARD = Color.parseColor("#FFFFFF");      // White
    static final int COLOR_STROKE = Color.parseColor("#E5E8EB");    // Toss light border
    static final int COLOR_STROKE_LIGHT = Color.parseColor("#F0F2F5");
    static final int COLOR_TEXT_MAIN = Color.parseColor("#191F28"); // Toss primary dark
    static final int COLOR_TEXT_MUTED = Color.parseColor("#8B95A1");// Toss secondary gray
    static final int COLOR_TEXT_HINT = Color.parseColor("#B0B8C1");
    static final int COLOR_PRIMARY = Color.parseColor("#3182F6");   // Toss signature blue
    static final int COLOR_PRIMARY_HOVER = Color.parseColor("#1B64DA");
    static final int COLOR_PRIMARY_LIGHT = Color.parseColor("#E8F3FF");
    static final int COLOR_NOTION = Color.parseColor("#1F2328");    // Sleek Notion dark
    static final int COLOR_NOTION_BG = Color.parseColor("#2F343B"); // Notion dark block
    static final int COLOR_SUCCESS = Color.parseColor("#059669");   // Emerald 600
    static final int COLOR_SUCCESS_LIGHT = Color.parseColor("#E6F4EA");
    static final int COLOR_WARNING = Color.parseColor("#F04452");   // Toss warning red
    static final int COLOR_WARNING_LIGHT = Color.parseColor("#FEECEE");

    static int dp(Context c, float dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, c.getResources().getDisplayMetrics()));
    }

    static GradientDrawable roundedRect(int bgColor, int strokeColor, float radiusDp, float strokeDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setColor(bgColor);
        if (radiusDp > 0) gd.setCornerRadius(radiusDp * 2.5f);
        if (strokeDp > 0) gd.setStroke((int) strokeDp, strokeColor);
        return gd;
    }

    static LinearLayout card(Context c) {
        return card(c, 16);
    }

    static LinearLayout card(Context c, int radiusDp) {
        LinearLayout layout = new LinearLayout(c);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(roundedRect(COLOR_CARD, COLOR_STROKE, radiusDp, 1));
        layout.setElevation(dp(c, 2));
        int pad = dp(c, 18);
        layout.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(c, 14));
        layout.setLayoutParams(lp);
        return layout;
    }

    static TextView text(Context c, String value, int sizeSp) {
        TextView v = new TextView(c);
        v.setText(value);
        v.setTextSize(sizeSp);
        v.setTextColor(COLOR_TEXT_MAIN);
        return v;
    }

    static TextView badge(Context c, String text, int bgColor, int textColor) {
        TextView b = new TextView(c);
        b.setText(text);
        b.setTextSize(12);
        b.setTextColor(textColor);
        b.setTypeface(null, android.graphics.Typeface.BOLD);
        b.setBackground(roundedRect(bgColor, bgColor, 12, 0));
        b.setPadding(dp(c, 10), dp(c, 4), dp(c, 10), dp(c, 4));
        return b;
    }

    static TextView chip(Context c, String text, int bgColor, int textColor) {
        TextView ch = new TextView(c);
        ch.setText(text);
        ch.setTextSize(13);
        ch.setTextColor(textColor);
        ch.setTypeface(null, android.graphics.Typeface.BOLD);
        ch.setBackground(roundedRect(bgColor, COLOR_STROKE, 14, 1));
        ch.setPadding(dp(c, 12), dp(c, 6), dp(c, 12), dp(c, 6));
        return ch;
    }

    static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(COLOR_STROKE_LIGHT);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 1));
        lp.setMargins(0, dp(c, 12), 0, dp(c, 12));
        v.setLayoutParams(lp);
        return v;
    }

    static LinearLayout infoRow(Context c, String label, String value) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(c, 4), 0, dp(c, 4));

        TextView tvLabel = text(c, label, 14);
        tvLabel.setTextColor(COLOR_TEXT_MUTED);
        LinearLayout.LayoutParams lpLabel = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tvLabel.setLayoutParams(lpLabel);
        row.addView(tvLabel);

        TextView tvValue = text(c, value, 14);
        tvValue.setTextColor(COLOR_TEXT_MAIN);
        tvValue.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(tvValue);

        return row;
    }

    static Button button(Context c, String label, Runnable action) {
        return button(c, label, COLOR_PRIMARY, Color.WHITE, action);
    }

    static Button button(Context c, String label, int bgColor, int textColor, Runnable action) {
        Button b = new Button(c);
        b.setText(label);
        b.setTextColor(textColor);
        b.setTextSize(15);
        b.setTypeface(null, android.graphics.Typeface.BOLD);
        b.setElevation(dp(c, 2));
        b.setStateListAnimator(null);

        GradientDrawable normal = roundedRect(bgColor, bgColor, 12, 0);
        RippleDrawable ripple = new RippleDrawable(
            ColorStateList.valueOf(Color.argb(70, 255, 255, 255)), normal, null);
        b.setBackground(ripple);

        b.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(c, 6), 0, dp(c, 8));
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    static Button subButton(Context c, String label, Runnable action) {
        Button b = new Button(c);
        b.setText(label);
        b.setTextColor(COLOR_PRIMARY);
        b.setTextSize(14);
        b.setTypeface(null, android.graphics.Typeface.BOLD);
        b.setElevation(0);
        b.setStateListAnimator(null);

        GradientDrawable normal = roundedRect(COLOR_PRIMARY_LIGHT, COLOR_PRIMARY_LIGHT, 10, 0);
        RippleDrawable ripple = new RippleDrawable(
            ColorStateList.valueOf(Color.argb(50, 49, 130, 246)), normal, null);
        b.setBackground(ripple);

        b.setPadding(dp(c, 14), dp(c, 10), dp(c, 14), dp(c, 10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    static Button outlineButton(Context c, String label, int strokeColor, int textColor, Runnable action) {
        Button b = new Button(c);
        b.setText(label);
        b.setTextColor(textColor);
        b.setTextSize(14);
        b.setTypeface(null, android.graphics.Typeface.BOLD);
        b.setElevation(0);
        b.setStateListAnimator(null);

        GradientDrawable normal = roundedRect(Color.WHITE, strokeColor, 10, 1);
        RippleDrawable ripple = new RippleDrawable(
            ColorStateList.valueOf(Color.argb(30, 0, 0, 0)), normal, null);
        b.setBackground(ripple);

        b.setPadding(dp(c, 14), dp(c, 10), dp(c, 14), dp(c, 10));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    static EditText input(Context c, String label, String value, boolean numeric) {
        EditText e = new EditText(c);
        e.setHint(label);
        e.setContentDescription(label);
        e.setSingleLine(true);
        e.setText(value);
        e.setTextColor(COLOR_TEXT_MAIN);
        e.setHintTextColor(COLOR_TEXT_MUTED);
        e.setTextSize(15);
        e.setBackground(roundedRect(Color.parseColor("#F8FAFC"), COLOR_STROKE, 10, 1));
        int padH = dp(c, 14);
        int padV = dp(c, 12);
        e.setPadding(padH, padV, padH, padV);
        if (numeric) {
            e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(c, 4), 0, dp(c, 8));
        e.setLayoutParams(lp);
        return e;
    }
}
