package kr.dutchpay;

import android.content.Context;
import android.graphics.Color;
import android.text.*;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.*;

final class RowEditor extends LinearLayout {
    final CheckBox selected, reviewed;
    final EditText name, unit, quantity, percent;
    final TextView result;
    final TextView tvNameDisplay;
    final TextView tvSubInfo;
    final TextView tvToggleChevron;
    final LinearLayout detailLayout;
    final Item source;

    RowEditor(Context c, Item item, Runnable changed) {
        super(c);
        setOrientation(VERTICAL);
        setBackgroundColor(Color.TRANSPARENT);
        int padH = Ui.dp(c, 4);
        int padV = Ui.dp(c, 8);
        setPadding(padH, padV, padH, padV);

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(rowLp);

        source = item;
        String issue = Receipt.issue(item);

        // ============================================================
        // 1. TOSS-STYLE COMPACT LIST ROW (기본 1줄 요약)
        // ============================================================
        LinearLayout compactRow = new LinearLayout(c);
        compactRow.setOrientation(HORIZONTAL);
        compactRow.setGravity(Gravity.CENTER_VERTICAL);

        selected = new CheckBox(c);
        selected.setChecked(true);
        compactRow.addView(selected);

        LinearLayout infoCol = new LinearLayout(c);
        infoCol.setOrientation(VERTICAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        infoLp.setMargins(Ui.dp(c, 4), 0, 0, 0);
        infoCol.setLayoutParams(infoLp);

        LinearLayout nameRow = new LinearLayout(c);
        nameRow.setOrientation(HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);

        tvNameDisplay = Ui.text(c, item.name, 15);
        tvNameDisplay.setTypeface(null, android.graphics.Typeface.BOLD);
        tvNameDisplay.setTextColor(Ui.COLOR_TEXT_MAIN);
        nameRow.addView(tvNameDisplay);

        if (item.originalName != null && !item.originalName.isEmpty() && !item.originalName.equals(item.name)) {
            TextView autoBadge = Ui.badge(c, "보정", Color.parseColor("#E8F5E9"), Color.parseColor("#2E7D32"));
            autoBadge.setTextSize(10);
            autoBadge.setPadding(Ui.dp(c, 5), Ui.dp(c, 1), Ui.dp(c, 5), Ui.dp(c, 1));
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bLp.setMargins(Ui.dp(c, 6), 0, 0, 0);
            autoBadge.setLayoutParams(bLp);
            nameRow.addView(autoBadge);
        }
        infoCol.addView(nameRow);

        tvSubInfo = Ui.text(c, formatSubInfo(), 12);
        tvSubInfo.setTextColor(issue.isEmpty() ? Ui.COLOR_TEXT_MUTED : Ui.COLOR_WARNING);
        tvSubInfo.setPadding(0, Ui.dp(c, 2), 0, 0);
        infoCol.addView(tvSubInfo);
        compactRow.addView(infoCol);

        LinearLayout rightCol = new LinearLayout(c);
        rightCol.setOrientation(HORIZONTAL);
        rightCol.setGravity(Gravity.CENTER_VERTICAL);

        result = Ui.text(c, "", 15);
        result.setTypeface(null, android.graphics.Typeface.BOLD);
        result.setTextColor(Ui.COLOR_TEXT_MAIN);
        result.setGravity(Gravity.END);
        rightCol.addView(result);

        tvToggleChevron = Ui.text(c, " ▾", 15);
        tvToggleChevron.setTextColor(Ui.COLOR_TEXT_MUTED);
        tvToggleChevron.setPadding(Ui.dp(c, 4), 0, 0, 0);
        rightCol.addView(tvToggleChevron);

        compactRow.addView(rightCol);
        addView(compactRow);

        // ============================================================
        // 2. POLISHED ACCORDION CARD (예쁘고 정돈된 세부 편집 카드)
        // ============================================================
        detailLayout = new LinearLayout(c);
        detailLayout.setOrientation(VERTICAL);
        detailLayout.setVisibility(View.GONE);
        detailLayout.setBackground(Ui.roundedRect(Color.parseColor("#F8FAFC"), Ui.COLOR_STROKE, 12, 1));
        int dPad = Ui.dp(c, 14);
        detailLayout.setPadding(dPad, dPad, dPad, dPad);
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dLp.setMargins(0, Ui.dp(c, 6), 0, Ui.dp(c, 4));
        detailLayout.setLayoutParams(dLp);

        // 2-1. 품목명 입력란
        TextView lblName = Ui.text(c, "품목명", 12);
        lblName.setTypeface(null, android.graphics.Typeface.BOLD);
        lblName.setTextColor(Color.parseColor("#475569"));
        detailLayout.addView(lblName);

        name = Ui.input(c, "품목명 입력", item.name, false);
        name.setBackground(Ui.roundedRect(Color.WHITE, Ui.COLOR_STROKE, 8, 1));
        detailLayout.addView(name);

        // 2-2. 대안 후보 칩 목록 (가로 스크롤 가능한 세련된 알약 칩)
        LinkedHashSet<String> candidateSet = new LinkedHashSet<>();
        if (item.originalName != null && !item.originalName.trim().isEmpty()) candidateSet.add(item.originalName);
        for (String cand : item.nameCandidates) {
            if (cand != null && !cand.trim().isEmpty()) candidateSet.add(cand);
        }
        candidateSet.remove(item.name); // 현재 선택된 이름 제외

        if (!candidateSet.isEmpty()) {
            HorizontalScrollView chipScroll = new HorizontalScrollView(c);
            chipScroll.setHorizontalScrollBarEnabled(false);
            LinearLayout chipRow = new LinearLayout(c);
            chipRow.setOrientation(HORIZONTAL);
            chipRow.setGravity(Gravity.CENTER_VERTICAL);
            chipRow.setPadding(0, Ui.dp(c, 4), 0, Ui.dp(c, 10));

            TextView chipPrefix = Ui.text(c, "추천 후보: ", 12);
            chipPrefix.setTextColor(Ui.COLOR_TEXT_MUTED);
            chipRow.addView(chipPrefix);

            for (String cand : candidateSet) {
                Button chip = new Button(c);
                chip.setText(cand);
                chip.setTextSize(12);
                chip.setTextColor(Color.parseColor("#2563EB"));
                chip.setBackground(Ui.roundedRect(Color.parseColor("#EFF6FF"), Color.parseColor("#BFDBFE"), 14, 1));
                chip.setPadding(Ui.dp(c, 10), Ui.dp(c, 3), Ui.dp(c, 10), Ui.dp(c, 3));
                LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                cLp.setMargins(0, 0, Ui.dp(c, 6), 0);
                chip.setLayoutParams(cLp);
                chip.setOnClickListener(v -> name.setText(cand));
                chipRow.addView(chip);
            }
            chipScroll.addView(chipRow);
            detailLayout.addView(chipScroll);
        }

        // 2-3. 수량 통합 스텝퍼 (iOS/Toss 스타일의 일체형 [-] N개 [+] 바)
        LinearLayout stepperContainer = new LinearLayout(c);
        stepperContainer.setOrientation(HORIZONTAL);
        stepperContainer.setGravity(Gravity.CENTER_VERTICAL);
        stepperContainer.setBackground(Ui.roundedRect(Color.WHITE, Ui.COLOR_STROKE, 8, 1));
        stepperContainer.setPadding(Ui.dp(c, 4), Ui.dp(c, 4), Ui.dp(c, 4), Ui.dp(c, 4));
        LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scLp.setMargins(0, Ui.dp(c, 4), 0, Ui.dp(c, 12));
        stepperContainer.setLayoutParams(scLp);

        Button btnMinus = new Button(c);
        btnMinus.setText("－");
        btnMinus.setTextSize(16);
        btnMinus.setTextColor(Color.parseColor("#1E293B"));
        btnMinus.setBackground(Ui.roundedRect(Color.parseColor("#F1F5F9"), Color.TRANSPARENT, 6, 0));
        int btnPad = Ui.dp(c, 12);
        btnMinus.setPadding(btnPad, Ui.dp(c, 6), btnPad, Ui.dp(c, 6));
        btnMinus.setOnClickListener(v -> adjustQty(-1));
        stepperContainer.addView(btnMinus);

        TextView tvQtyLabel = Ui.text(c, "수량 조절 (1개 단위)", 13);
        tvQtyLabel.setTextColor(Color.parseColor("#64748B"));
        tvQtyLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams qlLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tvQtyLabel.setLayoutParams(qlLp);
        stepperContainer.addView(tvQtyLabel);

        Button btnPlus = new Button(c);
        btnPlus.setText("＋");
        btnPlus.setTextSize(16);
        btnPlus.setTextColor(Color.parseColor("#1E293B"));
        btnPlus.setBackground(Ui.roundedRect(Color.parseColor("#F1F5F9"), Color.TRANSPARENT, 6, 0));
        btnPlus.setPadding(btnPad, Ui.dp(c, 6), btnPad, Ui.dp(c, 6));
        btnPlus.setOnClickListener(v -> adjustQty(1));
        stepperContainer.addView(btnPlus);
        detailLayout.addView(stepperContainer);

        // 2-4. 단가 | 수량 | 비율(%) 3열 그리드
        LinearLayout grid = new LinearLayout(c);
        grid.setOrientation(HORIZONTAL);

        LinearLayout uCol = new LinearLayout(c);
        uCol.setOrientation(VERTICAL);
        LinearLayout.LayoutParams uLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f);
        uLp.setMargins(0, 0, Ui.dp(c, 6), 0);
        uCol.setLayoutParams(uLp);
        TextView lblU = Ui.text(c, "단가(원)", 11);
        lblU.setTextColor(Ui.COLOR_TEXT_MUTED);
        uCol.addView(lblU);
        unit = Ui.input(c, "단가", "" + item.unit, true);
        unit.setBackground(Ui.roundedRect(Color.WHITE, Ui.COLOR_STROKE, 8, 1));
        uCol.addView(unit);
        grid.addView(uCol);

        LinearLayout qCol = new LinearLayout(c);
        qCol.setOrientation(VERTICAL);
        LinearLayout.LayoutParams qLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        qLp.setMargins(0, 0, Ui.dp(c, 6), 0);
        qCol.setLayoutParams(qLp);
        TextView lblQ = Ui.text(c, "수량", 11);
        lblQ.setTextColor(Ui.COLOR_TEXT_MUTED);
        qCol.addView(lblQ);
        quantity = Ui.input(c, "수량", item.quantityKnown ? "" + Math.abs(item.count) : "", true);
        quantity.setBackground(Ui.roundedRect(Color.WHITE, Ui.COLOR_STROKE, 8, 1));
        qCol.addView(quantity);
        grid.addView(qCol);

        LinearLayout pCol = new LinearLayout(c);
        pCol.setOrientation(VERTICAL);
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.9f);
        pCol.setLayoutParams(pLp);
        TextView lblP = Ui.text(c, "비율(%)", 11);
        lblP.setTextColor(Ui.COLOR_TEXT_MUTED);
        pCol.addView(lblP);
        percent = Ui.input(c, "100", "100", true);
        percent.setBackground(Ui.roundedRect(Color.WHITE, Ui.COLOR_STROKE, 8, 1));
        pCol.addView(percent);
        grid.addView(pCol);

        detailLayout.addView(grid);

        // 2-5. 비율 원터치 프리셋 버튼 [ 0% (제외) ] [ 50% (절반) ] [ 100% (전액) ]
        LinearLayout presetRow = new LinearLayout(c);
        presetRow.setOrientation(HORIZONTAL);
        presetRow.setGravity(Gravity.CENTER_VERTICAL);
        presetRow.setPadding(0, 0, 0, Ui.dp(c, 10));

        TextView tvPre = Ui.text(c, "빠른 비율: ", 11);
        tvPre.setTextColor(Ui.COLOR_TEXT_MUTED);
        presetRow.addView(tvPre);

        for (int pVal : new int[]{0, 50, 100}) {
            Button pBtn = new Button(c);
            pBtn.setText(pVal + "%");
            pBtn.setTextSize(11);
            pBtn.setTextColor(Color.parseColor("#475569"));
            pBtn.setBackground(Ui.roundedRect(Color.WHITE, Ui.COLOR_STROKE, 6, 1));
            pBtn.setPadding(Ui.dp(c, 8), Ui.dp(c, 2), Ui.dp(c, 8), Ui.dp(c, 2));
            LinearLayout.LayoutParams pBLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pBLp.setMargins(0, 0, Ui.dp(c, 6), 0);
            pBtn.setLayoutParams(pBLp);
            pBtn.setOnClickListener(v -> percent.setText(String.valueOf(pVal)));
            presetRow.addView(pBtn);
        }
        detailLayout.addView(presetRow);

        if (!issue.isEmpty()) {
            TextView issueView = Ui.text(c, "⚠️ " + issue, 12);
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

        Button btnClose = new Button(c);
        btnClose.setText("접기 ▲");
        btnClose.setTextSize(12);
        btnClose.setTextColor(Ui.COLOR_TEXT_MUTED);
        btnClose.setBackground(Ui.roundedRect(Color.TRANSPARENT, Color.TRANSPARENT, 0, 0));
        btnClose.setOnClickListener(v -> toggleExpand());
        detailLayout.addView(btnClose);

        addView(detailLayout);

        // Toggle Expand Click
        View.OnClickListener toggleClick = v -> toggleExpand();
        infoCol.setOnClickListener(toggleClick);
        rightCol.setOnClickListener(toggleClick);

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
        int val = 1;
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

        // 비율이 비어있으면 0으로 자동 처리
        String pctStr = percent.getText().toString().trim();
        if (pctStr.isEmpty()) pctStr = "0";

        // 수량이 비어있으면 0으로 자동 처리
        String qtyStr = quantity.getText().toString().trim();
        if (qtyStr.isEmpty()) qtyStr = source.quantityKnown ? "0" : "";

        // 단가가 비어있으면 0으로 자동 처리
        String unitStr = unit.getText().toString().trim();
        long unitVal = 0;
        try {
            if (!unitStr.isEmpty()) unitVal = Long.parseLong(unitStr.replaceAll("[^0-9-]", ""));
        } catch (Exception ignored) {}

        try {
            long value = source.portion(qtyStr, pctStr, unitVal);
            result.setText(String.format(Locale.KOREA, "%,d원", value));
            return value;
        } catch (Exception ex) {
            result.setText("0원");
            return 0;
        }
    }

    String note() {
        if (source.includedDiscount) return name.getText() + ": " + String.format(Locale.KOREA, "%,d원", source.printedTotal) + " (품목에 이미 반영, 추가 차감 없음)";
        String pctStr = percent.getText().toString().trim();
        if (pctStr.isEmpty()) pctStr = "0";
        String qtyStr = quantity.getText().toString().trim();
        if (qtyStr.isEmpty()) qtyStr = "0";

        if (source.count < 0) return name.getText() + ": 원본 수량 " + source.count + "개, 인쇄 행 금액 " + source.printedTotal
            + "원 / 적용 제거·반품 수량 " + qtyStr + "개 × " + pctStr + "% = " + amount() + "원"
            + " (" + (source.amountBased ? "편집 행 금액 " : "편집 단가 ") + unit.getText() + "원)";
        if (!source.quantityKnown && quantity.getText().toString().trim().isEmpty())
            return name.getText() + ": " + (source.amountBased ? unit.getText() : source.printedTotal) + "원 (행 금액 전체 적용, 원수량 미인식"
                + (source.amountBased ? "" : ", 인쇄 단가 " + source.unit + "원") + ") × " + pctStr + "% = " + amount() + "원";
        return name.getText() + ": "
            + (source.unit < 0 && source.printedTotal > 0 ? "원본 인쇄 행 금액 " + source.printedTotal + "원 / 적용 계산 " : "")
            + unit.getText() + "원 × " + qtyStr
            + (!source.quantityKnown ? " (직접 입력한 적용 수량, 원수량 미인식)" : source.amountBased ? " / 원수량 " + source.count : "")
            + " × " + pctStr + "% = " + amount() + "원";
    }
}
