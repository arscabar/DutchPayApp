package kr.dutchpay;

import android.content.Context;
import android.widget.*;

final class Ui {
    static TextView text(Context c, String value, int size) {
        TextView v = new TextView(c); v.setText(value); v.setTextSize(size);
        v.setPadding(0, 10, 0, 10); return v;
    }
    static Button button(Context c, String label, Runnable action) {
        Button b = new Button(c); b.setText(label); b.setOnClickListener(v -> action.run()); return b;
    }
    static EditText input(Context c, String label, String value, boolean numeric) {
        EditText e = new EditText(c); e.setHint(label); e.setContentDescription(label);
        e.setSingleLine(); e.setText(value);
        if (numeric) e.setInputType(2 | 4096 | 8192);
        return e;
    }
}
