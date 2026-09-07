package kr.dutchpay;

import android.content.Context;
import android.graphics.Color;
import android.text.*;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.Locale;

final class RowEditor extends LinearLayout {
    final CheckBox selected, reviewed;
    final EditText name, unit, quantity, percent;
    final TextView result;
    final TextView tvNameDisplay;
    final TextView tvSubInfo;
    final TextView tvToggleChevron;
    final LinearLayout detailLayout;
    final NameChoice nameChoices;
    final Item source;

    RowEditor(Context c, Item item, Runnable changed) {
        super(c);
        setOrientation(VERTICAL);
        setBackground(Ui.roundedRect(Color.WHITE, Ui.COLOR_STROKE, 10, 1));
        int pad = Ui.dp(c, 10);
        setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, Ui.dp(c, 8));
        setLayoutParams(cardLp);

        source = item;
        String issue = Receipt.issue(item);

        // ============================================================
        // 1. COMPACT ROW (항상 노출되는 컴팩트 1줄 요약 뷰)
        // ============================================================
        LinearLayout compactRow = new LinearLayout(c);
        compactRow.setOrientation(HORIZONTAL);
        compactRow.setGravity(Gravity.CENTER_VERTICAL);
        compactRow.setPadding(0, Ui.dp(c, 2), 0, Ui.dp(c, 2));

        // 1-1. 체크박스
        selected = new CheckBox(c);
        selected.setChecked(true);
        compactRow.addView(selected);

        // 1-2. 중앙 정보: 품목명 + 서브정보(수량·단가 / 오독보정 뱃지)
        LinearLayout infoCol = new LinearLayout(c);
        infoCol.setOrientation(VERTICAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        infoCol.setLayoutParams(infoLp);

        LinearLayout titleRow = new LinearLayout(c);
        titleRow.setOrientation(HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        tvNameDisplay = Ui.text(c, item.name, 15);
        tvNameDisplay.setTypeface(null, android.graphics.Typeface.BOLD);
        tvNameDisplay.setTextColor(Ui.COLOR_TEXT_MAIN);
        titleRow.addView(tvNameDisplay);

        if (item.originalName != null && !item.originalName.isEmpty() && !item.originalName.equals(item.name)) {
            TextView autoBadge = Ui.badge(c, "보정", Color.parseColor("#ECFDF5"), Color.parseColor("#059669"));
            autoBadge.setTextSize(10);
            autoBadge.setPadding(Ui.dp(c, 6), Ui.dp(c, 1), Ui.dp(c, 6), Ui.dp(c, 1));
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bLp.setMargins(Ui.dp(c, 6), 0, 0, 0);
            autoBadge.setLayoutParams(bLp);
            titleRow.addView(autoBadge);
        }
        infoCol.addView(titleRow);

        tvSubInfo = Ui.text(c, formatSubInfo(), 12);
        tvSubInfo.setTextColor(issue.isEmpty() ? Ui.COLOR_TEXT_MUTED : Ui.COLOR_WARNING);
        infoCol.addView(tvSubInfo);
        compactRow.addView(infoCol);

        // 1-3. 우측: 계산된 금액 + 펼치기 아이콘 (▼)
        LinearLayout rightCol = new LinearLayout(c);
        rightCol.setOrientation(HORIZONTAL);
        rightCol.setGravity(Gravity.CENTER_VERTICAL);

        result = Ui.text(c, "", 15);
        result.setTypeface(null, android.graphics.Typeface.BOLD);
        result.setTextColor(Ui.COLOR_PRIMARY);
        result.setGravity(Gravity.END);
        rightCol.addView(result);

        tvToggleChevron = Ui.text(c, " ▾", 16);
        tvToggleChevron.setTextColor(Ui.COLOR_TEXT_MUTED);
        tvToggleChevron.setPadding(Ui.dp(c, 4), 0, Ui.dp(c, 2), 0);
        rightCol.addView(tvToggleChevron);

        compactRow.addView(rightCol);
        addView(compactRow);

        // ============================================================
        // 2. EXPANDABLE DETAIL VIEW (터치 시 펼쳐지는 세부 편집기)
        // ============================================================
        detailLayout = new LinearLayout(c);
        detailLayout.setOrientation(VERTICAL);
        detailLayout.setVisibility(View.GONE);
        detailLayout.setPadding(Ui.dp(c, 8), Ui.dp(c, 10), Ui.dp(c, 8), Ui.dp(c, 6));

        View divider = new View(c);
        divider.setBackgroundColor(Ui.COLOR_STROKE);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(c, 1));
        divLp.setMargins(0, 0, 0, Ui.dp(c, 8));
        divider.setLayoutParams(divLp);
        detailLayout.addView(divider);

        // 2-1. 품목명 수정 & 대안 후보
        TextView lblName = Ui.text(c, "품목명 수정", 12);
        lblName.setTextColor(Ui.COLOR_TEXT_MUTED);
        detailLayout.addView(lblName);

        name = Ui.input(c, "품목명", item.name, false);
        detailLayout.addView(name);
        nameChoices = new NameChoice(c, item, name);
        detailLayout.addView(nameChoices);

        // 2-2. 수량 증감 스텝퍼 [-] [수량] [+] 및 단가, 비율을 나란히 배치
        LinearLayout fieldsGrid = new LinearLayout(c);
        fieldsGrid.setOrientation(HORIZONTAL);

        // 단가 필드
        LinearLayout unitCol = new LinearLayout(c);
        unitCol.setOrientation(VERTICAL);
        LinearLayout.LayoutParams uLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f);
        uLp.setMargins(0, 0, Ui.dp(c, 6), 0);
        unitCol.setLayoutParams(uLp);

        TextView lblUnit = Ui.text(c, "단가(원)", 12);
        lblUnit.setTextColor(Ui.COLOR_TEXT_MUTED);
        unitCol.addView(lblUnit);
        unit = Ui.input(c, "단가", "" + item.unit, true);
        unitCol.addView(unit);
        fieldsGrid.addView(unitCol);

        // 수량 필드
        LinearLayout qtyCol = new LinearLayout(c);
        qtyCol.setOrientation(VERTICAL);
        LinearLayout.LayoutParams qLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        qLp.setMargins(0, 0, Ui.dp(c, 6), 0);
        qtyCol.setLayoutParams(qLp);

        TextView lblQty = Ui.text(c, "수량", 12);
        lblQty.setTextColor(Ui.COLOR_TEXT_MUTED);
        qtyCol.addView(lblQty);
        quantity = Ui.input(c, "수량", item.quantityKnown ? "" + Math.abs(item.count) : "", true);
        qtyCol.addView(quantity);
        fieldsGrid.addView(qtyCol);

        // 비율 필드
        LinearLayout pctCol = new LinearLayout(c);
        pctCol.setOrientation(VERTICAL);
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.8f);
        pctCol.setLayoutParams(pLp);

        TextView lblPct = Ui.text(c, "비율(%)", 12);
        lblPct.setTextColor(Ui.COLOR_TEXT_MUTED);
        pctCol.addView(lblPct);
        percent = Ui.input(c, "비율", "100", true);
        pctCol.addView(percent);
        fieldsGrid.addView(pctCol);

        detailLayout.addView(fieldsGrid);

        // 빠른 수량 조절 버튼 (Stepper: -1, +1)
        LinearLayout stepperRow = new LinearLayout(c);
        stepperRow.setOrientation(HORIZONTAL);
        stepperRow.setGravity(Gravity.CENTER_VERTICAL);
        stepperRow.setPadding(0, 0, 0, Ui.dp(c, 8));

        Button btnMinus = new Button(c);
        btnMinus.setText("－ 1개");
        btnMinus.setTextSize(11);
        btnMinus.setBackground(Ui.roundedRect(Color.parseColor("#F1F5F9"), Ui.COLOR_STROKE, 6, 1));
        btnMinus.setPadding(Ui.dp(c, 8), Ui.dp(c, 4), Ui.dp(c, 8), Ui.dp(c, 4));
        btnMinus.setOnClickListener(v -> adjustQty(-1));
        stepperRow.addView(btnMinus);

        TextView stepSpace = new TextView(c);
        stepSpace.setText(" ");
        stepSpace.setPadding(Ui.dp(c, 4), 0, Ui.dp(c, 4), 0);
        stepperRow.addView(stepSpace);

        Button btnPlus = new Button(c);
        btnPlus.setText("＋ 1개");
        btnPlus.setTextSize(11);
        btnPlus.setBackground(Ui.roundedRect(Color.parseColor("#F1F5F9"), Ui.COLOR_STROKE, 6, 1));
        btnPlus.setPadding(Ui.dp(c, 8), Ui.dp(c, 4), Ui.dp(c, 8), Ui.dp(c, 4));
        btnPlus.setOnClickListener(v -> adjustQty(1));
        stepperRow.addView(btnPlus);

        detailLayout.addView(stepperRow);

        // 문제 사항 알림
        if (!issue.isEmpty()) {
            TextView issueView = Ui.text(c, "확인 필요: " + issue, 12);
            issueView.setTextColor(Ui.COLOR_WARNING);
            issueView.setPadding(0, 0, 0, Ui.dp(c, 4));
            detailLayout.addView(issueView);
        }

        reviewed = new CheckBox(c);
        reviewed.setText("원본 대조 확인 완료");
        reviewed.setTextSize(12);
        reviewed.setTextColor(Ui.COLOR_TEXT_MUTED);
        reviewed.setVisibility(issue.isEmpty() ? GONE : VISIBLE);
        detailLayout.addView(reviewed);

        // 접기 버튼
        Button btnClose = new Button(c);
        btnClose.setText("접기 ▲");
        btnClose.setTextSize(11);
        btnClose.setTextColor(Ui.COLOR_TEXT_MUTED);
        btnClose.setBackground(Ui.roundedRect(Color.TRANSPARENT, Color.TRANSPARENT, 0, 0));
        btnClose.setOnClickListener(v -> toggleExpand());
        detailLayout.addView(btnClose);

        addView(detailLayout);

        // 클릭 이벤트: infoCol 또는 rightCol 터치 시 아코디언 토글
        View.OnClickListener toggleClick = v -> toggleExpand();
        infoCol.setOnClickListener(toggleClick);
        rightCol.setOnClickListener(toggleClick);

        // 리스너 및 텍스트 감시자
        watch(() -> {
            if (!source.quantityKnown && !source.amountBased && !source.includedDiscount)
                unit.setEnabled(!quantity.getText().toString().trim().isEmpty());
            tvNameDisplay.setText(name.getText().toString());
            tvSubInfo.setText(formatSubInfo());
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

        reviewed.setOnCheckedChangeListener((v, chk) -> changed.run());
        selected.setOnCheckedChangeListener((v, chk) -> changed.run());
    }

    private String formatSubInfo() {
        String u = unit != null ? unit.getText().toString() : "" + source.unit;
        String q = quantity != null ? quantity.getText().toString() : (source.quantityKnown ? "" + Math.abs(source.count) : "1");
        long uVal = 0;
        try { uVal = Long.parseLong(u.replaceAll("[^0-9-]", "")); } catch (Exception ignored) {}
        return (q.isEmpty() ? "수량 미입력" : q + "개") + " · " + String.format(Locale.KOREA, "%,d원", uVal);
    }

    private void adjustQty(int delta) {
        String cur = quantity.getText().toString().trim();
        int val = 0;
        try { val = Integer.parseInt(cur); } catch (Exception ignored) {}
        int next = Math.max(1, val + delta);
        quantity.setText(String.valueOf(next));
    }

    private void toggleExpand() {
        boolean willExpand = detailLayout.getVisibility() != View.VISIBLE;
        detailLayout.setVisibility(willExpand ? View.VISIBLE : View.GONE);
        tvToggleChevron.setText(willExpand ? " ▴" : " ▾");
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

    long amount() {
        if (source.includedDiscount) {
            result.setText("0원");
            return 0;
        }
        if (!selected.isChecked()) {
            result.setText("제외");
            return 0;
        }
        if (name.getText().toString().trim().isEmpty()) throw new IllegalArgumentException();
        long value = source.portion(quantity.getText().toString(), percent.getText().toString(),
            Long.parseLong(unit.getText().toString()));
        result.setText(String.format(Locale.KOREA, "%,d원", value));
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
