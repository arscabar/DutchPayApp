package kr.dutchpay;

import android.content.Context;
import android.graphics.Color;
import android.text.*;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.*;
import java.util.Locale;

final class RowEditor extends LinearLayout {
    final CheckBox selected, reviewed;
    final EditText name, unit, quantity, percent;
    final TextView result;
    final NameChoice nameChoices;
    final Item source;

    RowEditor(Context c, Item item, Runnable changed) {
        super(c);
        setOrientation(VERTICAL);
        setBackground(Ui.roundedRect(Color.WHITE, Ui.COLOR_STROKE, 12, 1));
        setElevation(Ui.dp(c, 1));
        int pad = Ui.dp(c, 14);
        setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, Ui.dp(c, 12));
        setLayoutParams(cardLp);

        source = item;

        // Header Row: Checkbox + Auto-correction Badge
        LinearLayout header = new LinearLayout(c);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, Ui.dp(c, 6));

        selected = new CheckBox(c);
        selected.setText("이 항목 정산에 반영");
        selected.setTextSize(14);
        selected.setTypeface(null, android.graphics.Typeface.BOLD);
        selected.setTextColor(Ui.COLOR_TEXT_MAIN);
        selected.setChecked(true);
        LinearLayout.LayoutParams selLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        selected.setLayoutParams(selLp);
        header.addView(selected);

        if (item.originalName != null && !item.originalName.isEmpty() && !item.originalName.equals(item.name)) {
            TextView autoBadge = Ui.badge(c, "✓ 오독 보정됨 (" + item.originalName + ")", Color.parseColor("#ECFDF5"), Color.parseColor("#059669"));
            header.addView(autoBadge);
        }
        addView(header);

        // Name input & choices
        name = Ui.input(c, "품목명", item.name, false);
        addView(name);
        nameChoices = new NameChoice(c, item, name);
        addView(nameChoices);

        // Price, Quantity, Percent fields
        unit = field(c, item.amountBased ? "행 금액 (원수량 " + (item.quantityKnown ? item.count + "개" : "미인식") + ")"
            : item.count < 0 ? "단가 (제거·반품은 양수 입력)" : item.quantityKnown ? "단가 (할인은 음수)"
            : "단가 (적용 수량 입력 시 사용)", "" + item.unit);

        quantity = field(c, item.count < 0 ? "적용 제거·반품 수량 (양수 입력)" : item.quantityKnown ? "적용 수량"
            : "적용 수량 (비우면 행 금액 전체 적용)", item.quantityKnown ? "" + Math.abs(item.count) : "");

        if (item.count < 0) {
            TextView t = Ui.text(c, "원본 수량 " + item.count + "개 · 제거·반품 금액으로 차감합니다", 13);
            t.setTextColor(Color.parseColor("#DC2626"));
            addView(t);
        }
        if (!item.quantityKnown) {
            quantity.setEnabled(!item.amountBased);
            unit.setEnabled(item.amountBased);
            TextView t = Ui.text(c, "원수량 미인식 · 수량이 비어 있으면 행 금액 전체에 비율만 적용합니다", 13);
            t.setTextColor(Ui.COLOR_TEXT_MUTED);
            addView(t);
        }

        percent = field(c, "적용 비율 (%)", "100");

        // Result Amount
        result = Ui.text(c, "", 16);
        result.setTypeface(null, android.graphics.Typeface.BOLD);
        result.setTextColor(Ui.COLOR_PRIMARY);
        result.setPadding(0, Ui.dp(c, 4), 0, Ui.dp(c, 4));
        addView(result);

        TextView printed = Ui.text(c, "원본 행 인쇄 금액: " + String.format(Locale.KOREA, "%,d원", item.printedTotal), 13);
        printed.setTextColor(Ui.COLOR_TEXT_MUTED);
        addView(printed);

        String issue = Receipt.issue(item);
        if (!issue.isEmpty()) {
            TextView issueText = Ui.text(c, "확인 필요: " + issue, 13);
            issueText.setTextColor(Ui.COLOR_WARNING);
            addView(issueText);
        }

        reviewed = new CheckBox(c);
        reviewed.setText("원본과 품목·금액·수량을 대조 확인했습니다");
        reviewed.setTextSize(13);
        reviewed.setTextColor(Ui.COLOR_TEXT_MUTED);
        reviewed.setVisibility(issue.isEmpty() ? GONE : VISIBLE);
        addView(reviewed);

        watch(() -> {
            if (!source.quantityKnown && !source.amountBased && !source.includedDiscount)
                unit.setEnabled(!quantity.getText().toString().trim().isEmpty());
            reviewed.setVisibility(VISIBLE);
            reviewed.setChecked(false);
            changed.run();
        }, name, unit, quantity);
        watch(changed, percent);

        if (item.includedDiscount) {
            selected.setChecked(false);
            selected.setEnabled(false);
            for (EditText e : new EditText[]{name, unit, quantity, percent}) e.setEnabled(false);
        }

        reviewed.setOnCheckedChangeListener((v, checked) -> changed.run());
        selected.setOnCheckedChangeListener((v, checked) -> changed.run());
    }

    private void watch(Runnable changed, EditText... fields) {
        TextWatcher watcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int co, int af) {}
            public void onTextChanged(CharSequence s, int st, int before, int co) { changed.run(); }
            public void afterTextChanged(Editable e) {}
        };
        for (EditText e : fields) e.addTextChangedListener(watcher);
    }

    boolean confirmed() {
        return !selected.isChecked() || reviewed.getVisibility() != VISIBLE || reviewed.isChecked();
    }

    private EditText field(Context c, String label, String value) {
        TextView labelView = Ui.text(c, label, 12);
        labelView.setTextColor(Ui.COLOR_TEXT_MUTED);
        labelView.setPadding(0, Ui.dp(c, 4), 0, Ui.dp(c, 2));
        addView(labelView);
        EditText e = Ui.input(c, label, value, true);
        addView(e);
        return e;
    }

    long amount() {
        if (source.includedDiscount) {
            result.setText("품목 금액에 이미 반영 · 추가 차감 없음");
            return 0;
        }
        if (!selected.isChecked()) {
            result.setText("정산 제외");
            return 0;
        }
        if (name.getText().toString().trim().isEmpty()) throw new IllegalArgumentException();
        long value = source.portion(quantity.getText().toString(), percent.getText().toString(),
            Long.parseLong(unit.getText().toString()));
        result.setText("반영 금액: " + String.format(Locale.KOREA, "%,d원", value));
        return value;
    }

    String note() {
        if (source.includedDiscount) return name.getText() + ": " + String.format(Locale.KOREA, "%,d원", source.printedTotal) + " (품목에 이미 반영, 추가 차감 없음)";
        if (source.count < 0) return name.getText() + ": 원본 수량 " + source.count + "개, 인쇄 행 금액 " + source.printedTotal
            + "원 / 적용 제거·반품 수량 " + quantity.getText() + "개 × " + percent.getText() + "% = " + amount() + "원"
            + " (" + (source.amountBased ? "편집 행 금액 " : "편집 단가 ") + unit.getText() + "원)";
        if (!source.quantityKnown && quantity.getText().toString().trim().isEmpty())
            return name.getText() + ": " + (source.amountBased ? unit.getText() : source.printedTotal) + "원 (행 금액 전체 적용, 원수량 미인식"
                + (source.amountBased ? "" : ", 인쇄 단가 " + source.unit + "원") + ") × " + percent.getText() + "% = " + amount() + "원";
        return name.getText() + ": "
            + (source.unit < 0 && source.printedTotal > 0 ? "원본 인쇄 행 금액 " + source.printedTotal + "원 / 적용 계산 " : "")
            + unit.getText() + "원 × " + quantity.getText()
            + (!source.quantityKnown ? " (직접 입력한 적용 수량, 원수량 미인식)" : source.amountBased ? " / 원수량 " + source.count : "")
            + " × " + percent.getText() + "% = " + amount() + "원";
    }
}
