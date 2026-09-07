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
    // Premium Color Palette
    static final int COLOR_BG = Color.parseColor("#F1F5F9");        // Slate 100
    static final int COLOR_CARD = Color.parseColor("#FFFFFF");      // White
    static final int COLOR_STROKE = Color.parseColor("#E2E8F0");    // Slate 200
    static final int COLOR_TEXT_MAIN = Color.parseColor("#0F172A"); // Slate 900
    static final int COLOR_TEXT_MUTED = Color.parseColor("#64748B");// Slate 500
    static final int COLOR_PRIMARY = Color.parseColor("#2563EB");   // Blue 600
    static final int COLOR_PRIMARY_HOVER = Color.parseColor("#1D4ED8");
    static final int COLOR_NOTION = Color.parseColor("#0F172A");    // Notion Black
    static final int COLOR_SUCCESS = Color.parseColor("#059669");   // Emerald 600
    static final int COLOR_SUCCESS_LIGHT = Color.parseColor("#ECFDF5");
    static final int COLOR_WARNING = Color.parseColor("#D97706");   // Amber 600
    static final int COLOR_WARNING_LIGHT = Color.parseColor("#FFFBEB");

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
        LinearLayout layout = new LinearLayout(c);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(roundedRect(COLOR_CARD, COLOR_STROKE, 14, 1));
        layout.setElevation(dp(c, 2));
        int pad = dp(c, 16);
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

        GradientDrawable normal = roundedRect(bgColor, bgColor, 10, 0);
        RippleDrawable ripple = new RippleDrawable(
            ColorStateList.valueOf(Color.argb(70, 255, 255, 255)), normal, null);
        b.setBackground(ripple);

        b.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(c, 4), 0, dp(c, 8));
        b.setLayoutParams(lp);
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
        e.setBackground(roundedRect(Color.parseColor("#F8FAFC"), COLOR_STROKE, 8, 1));
        int padH = dp(c, 12);
        int padV = dp(c, 10);
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
