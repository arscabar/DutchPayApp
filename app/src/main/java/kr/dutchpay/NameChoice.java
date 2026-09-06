package kr.dutchpay;

import android.app.AlertDialog;
import android.content.Context;
import android.text.*;
import android.widget.*;
import java.util.LinkedHashSet;

final class NameChoice extends LinearLayout {
    final TextView original;
    final Button button;
    AlertDialog dialog;

    @Override protected void onDetachedFromWindow() {
        if (dialog != null) { dialog.dismiss(); dialog = null; }
        super.onDetachedFromWindow();
    }

    NameChoice(Context c, Item item, EditText name) {
        super(c); setOrientation(VERTICAL);
        original = Ui.text(c, "처음 인식: " + item.originalName, 14);
        addView(original);
        Runnable update = () -> original.setVisibility(!item.originalName.trim().isEmpty()
            && !item.originalName.contentEquals(name.getText()) ? VISIBLE : GONE);
        update.run();
        name.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable text) { update.run(); }
        });
        LinkedHashSet<String> choices = new LinkedHashSet<>();
        if (!item.originalName.trim().isEmpty()) choices.add(item.originalName);
        boolean candidates = false;
        for (String value : item.nameCandidates) {
            if (value != null && !value.trim().isEmpty()) {
                choices.add(value); candidates = true;
            }
        }
        String[] values = choices.toArray(new String[0]);
        button = Ui.button(c, "품목명 후보 보기", () -> {
            if (!name.isEnabled()) return;
            dialog = new AlertDialog.Builder(c).setTitle("품목명 선택 · 규격도 확인하세요")
                .setItems(values, (d, which) -> {
                    String value = values[which];
                    if (!value.contentEquals(name.getText())) name.setText(value);
                }).setNegativeButton("취소", null).show();
        });
        button.setVisibility(candidates ? VISIBLE : GONE);
        button.setEnabled(!item.includedDiscount); addView(button);
    }
}
