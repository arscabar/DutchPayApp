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
        int padH = Ui.dp(c, 2);
        int padV = Ui.dp(c, 10);
        setPadding(padH, padV, padH, padV);

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(rowLp);

        source = item;
        String issue = Receipt.issue(item);

        // ============================================================
        // 1. TOSS-STYLE CLEAN & DYNAMIC COMPACT LIST ROW
        // ============================================================
        LinearLayout compactRow = new LinearLayout(c);
        compactRow.setOrientation(HORIZONTAL);
        compactRow.setGravity(Gravity.CENTER_VERTICAL);

        selected = new CheckBox(c);
        selected.setChecked(true);
        compactRow.addView(selected);

        // Center Info Column (가변 너비: 텍스트가 길어도 우측 금액과 겹치지 않고 자연스럽게 확장/줄바꿈)
        LinearLayout infoCol = new LinearLayout(c);
        infoCol.setOrientation(VERTICAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        infoLp.setMargins(Ui.dp(c, 8), 0, Ui.dp(c, 12), 0);
        infoCol.setLayoutParams(infoLp);

        tvNameDisplay = Ui.text(c, item.name, 15);
        tvNameDisplay.setTypeface(null, android.graphics.Typeface.BOLD);
        tvNameDisplay.setTextColor(Ui.COLOR_TEXT_MAIN);
        tvNameDisplay.setMaxLines(2);
        tvNameDisplay.setEllipsize(TextUtils.TruncateAt.END);
        infoCol.addView(tvNameDisplay);

        // Sub Info Row: 수량 · 단가 정보
        LinearLayout subRow = new LinearLayout(c);
        subRow.setOrientation(HORIZONTAL);
        subRow.setGravity(Gravity.CENTER_VERTICAL);
        subRow.setPadding(0, Ui.dp(c, 3), 0, 0);

        tvSubInfo = Ui.text(c, formatSubInfo(), 13);
        tvSubInfo.setTextColor(issue.isEmpty() ? Ui.COLOR_TEXT_MUTED : Ui.COLOR_WARNING);
        subRow.addView(tvSubInfo);

        if (item.originalName != null && !item.originalName.isEmpty() && !item.originalName.equals(item.name)) {
            TextView autoBadge = Ui.badge(c, "자동보정", Color.parseColor("#E8F5E9"), Color.parseColor("#2E7D32"));
            autoBadge.setTextSize(10);
            autoBadge.setPadding(Ui.dp(c, 5), Ui.dp(c, 1), Ui.dp(c, 5), Ui.dp(c, 1));
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bLp.setMargins(Ui.dp(c, 6), 0, 0, 0);
            autoBadge.setLayoutParams(bLp);
            subRow.addView(autoBadge);
        }

        infoCol.addView(subRow);
        compactRow.addView(infoCol);

        // Right Column: 금액 & 토글 아이콘 (절대 찌그러지지 않도록 WRAP_CONTENT 고정)
        LinearLayout rightCol = new LinearLayout(c);
        rightCol.setOrientation(HORIZONTAL);
        rightCol.setGravity(Gravity.CENTER_VERTICAL);

        result = Ui.text(c, "", 16);
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
        // 2. POLISHED ACCORDION CARD (펼쳤을 때 나타나는 세부 편집 카드)
        // ============================================================
        detailLayout = new LinearLayout(c);
        detailLayout.setOrientation(VERTICAL);
        detailLayout.setVisibility(View.GONE);
        detailLayout.setBackground(Ui.roundedRect(Color.parseColor("#F8FAFC"), Ui.COLOR_STROKE, 12, 1));
        int dPad = Ui.dp(c, 14);
        detailLayout.setPadding(dPad, dPad, dPad, dPad);
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dLp.setMargins(0, Ui.dp(c, 8), 0, Ui.dp(c, 4));
        detailLayout.setLayoutParams(dLp);

        // 1. 품목명 편집
        TextView tvEditLabel = Ui.text(c, "품목명 수정", 12);
        tvEditLabel.setTextColor(Ui.COLOR_TEXT_MUTED);
        detailLayout.addView(tvEditLabel);

        name = Ui.input(c, "품목명", item.name, false);
        detailLayout.addView(name);

        // 2. 단가 & 수량 조절 Row
        LinearLayout qtyRow = new LinearLayout(c);
        qtyRow.setOrientation(HORIZONTAL);
        qtyRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout unitBox = new LinearLayout(c);
        unitBox.setOrientation(VERTICAL);
        LinearLayout.LayoutParams ubLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f);
        ubLp.setMargins(0, 0, Ui.dp(c, 8), 0);
        unitBox.setLayoutParams(ubLp);

        TextView tvUnitLabel = Ui.text(c, "단가 (원)", 12);
        tvUnitLabel.setTextColor(Ui.COLOR_TEXT_MUTED);
        unitBox.addView(tvUnitLabel);

        unit = Ui.input(c, "단가", String.valueOf(item.unit), true);
        unitBox.addView(unit);
        qtyRow.addView(unitBox);

        LinearLayout qtyBox = new LinearLayout(c);
        qtyBox.setOrientation(VERTICAL);
        LinearLayout.LayoutParams qbLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        qtyBox.setLayoutParams(qbLp);

        TextView tvQtyLabel = Ui.text(c, "수량 (개)", 12);
        tvQtyLabel.setTextColor(Ui.COLOR_TEXT_MUTED);
        qtyBox.addView(tvQtyLabel);

        LinearLayout stepBox = new LinearLayout(c);
        stepBox.setOrientation(HORIZONTAL);
        stepBox.setGravity(Gravity.CENTER_VERTICAL);

        Button btnMinus = new Button(c);
        btnMinus.setText("－");
        btnMinus.setTextSize(14);
        btnMinus.setTextColor(Ui.COLOR_TEXT_MAIN);
        btnMinus.setBackground(Ui.roundedRect(Color.WHITE, Ui.COLOR_STROKE, 8, 1));
        int btnPad = Ui.dp(c, 6);
        btnMinus.setPadding(btnPad, btnPad, btnPad, btnPad);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(Ui.dp(c, 36), Ui.dp(c, 40));
        btnMinus.setLayoutParams(sLp);
        btnMinus.setOnClickListener(v -> adjustQty(-1));
        stepBox.addView(btnMinus);

        quantity = Ui.input(c, "수량", item.quantityKnown ? String.valueOf(Math.abs(item.count)) : "1", true);
        quantity.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams qLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        qLp.setMargins(Ui.dp(c, 4), 0, Ui.dp(c, 4), 0);
        quantity.setLayoutParams(qLp);
        stepBox.addView(quantity);

        Button btnPlus = new Button(c);
        btnPlus.setText("＋");
        btnPlus.setTextSize(14);
        btnPlus.setTextColor(Ui.COLOR_TEXT_MAIN);
        btnPlus.setBackground(Ui.roundedRect(Color.WHITE, Ui.COLOR_STROKE, 8, 1));
        btnPlus.setPadding(btnPad, btnPad, btnPad, btnPad);
        btnPlus.setLayoutParams(sLp);
        btnPlus.setOnClickListener(v -> adjustQty(1));
        stepBox.addView(btnPlus);

        qtyBox.addView(stepBox);
        qtyRow.addView(qtyBox);
        detailLayout.addView(qtyRow);

        // 3. 부담 비율 (더치페이 100%, 50% 등)
        TextView tvPctLabel = Ui.text(c, "내 부담 비율 (%)", 12);
        tvPctLabel.setTextColor(Ui.COLOR_TEXT_MUTED);
        tvPctLabel.setPadding(0, Ui.dp(c, 4), 0, 0);
        detailLayout.addView(tvPctLabel);

        percent = Ui.input(c, "부담 비율 (%)", "100", true);
        detailLayout.addView(percent);

        // 4. 원본 영수증 인쇄 영역 확인 버튼 (품목별 사진이 있는 경우에만 상세 카드 안에서 여유롭게 제공)
        if (item.crop != null) {
            Button btnCropZoom = Ui.subButton(c, "📷 이 품목 영수증 원본 사진 대조", () -> showZoomDialog(c, item));
            btnCropZoom.setTextSize(13);
            LinearLayout.LayoutParams czLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            czLp.setMargins(0, Ui.dp(c, 6), 0, Ui.dp(c, 6));
            btnCropZoom.setLayoutParams(czLp);
            detailLayout.addView(btnCropZoom);
        }

        // 5. 검토 완료 체크박스
        reviewed = new CheckBox(c);
        reviewed.setText("항목 확인 완료");
        reviewed.setTextColor(Color.parseColor("#991B1B"));
        reviewed.setTextSize(12);
        reviewed.setChecked(item.warning.isEmpty());
        reviewed.setVisibility(item.warning.isEmpty() ? View.GONE : View.VISIBLE);
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

        String pctStr = percent.getText().toString().trim();
        if (pctStr.isEmpty()) pctStr = "0";

        String qtyStr = quantity.getText().toString().trim();
        if (qtyStr.isEmpty()) qtyStr = source.quantityKnown ? "0" : "";

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

    private void showZoomDialog(Context c, Item item) {
        if (item.crop == null) return;
        android.app.Dialog dialog = new android.app.Dialog(c);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        LinearLayout dialogLayout = new LinearLayout(c);
        dialogLayout.setOrientation(VERTICAL);
        dialogLayout.setBackground(Ui.roundedRect(Color.WHITE, Ui.COLOR_STROKE, 16, 1));
        int pad = Ui.dp(c, 20);
        dialogLayout.setPadding(pad, pad, pad, pad);

        TextView title = Ui.text(c, item.name, 17);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(Ui.COLOR_TEXT_MAIN);
        dialogLayout.addView(title);

        TextView sub = Ui.text(c, "영수증 원본 인쇄 영역 대조", 12);
        sub.setTextColor(Ui.COLOR_TEXT_MUTED);
        sub.setPadding(0, Ui.dp(c, 2), 0, Ui.dp(c, 14));
        dialogLayout.addView(sub);

        ImageView zoomIv = new ImageView(c);
        zoomIv.setImageBitmap(item.crop);
        zoomIv.setAdjustViewBounds(true);
        zoomIv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        zoomIv.setBackground(Ui.roundedRect(Color.parseColor("#F8FAFC"), Ui.COLOR_STROKE, 8, 1));
        int imgPad = Ui.dp(c, 10);
        zoomIv.setPadding(imgPad, imgPad, imgPad, imgPad);
        LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ivLp.setMargins(0, 0, 0, Ui.dp(c, 16));
        zoomIv.setLayoutParams(ivLp);
        dialogLayout.addView(zoomIv);

        Button btnClose = Ui.button(c, "닫기", Ui.COLOR_PRIMARY, Color.WHITE, dialog::dismiss);
        btnClose.setTextSize(15);
        dialogLayout.addView(btnClose);

        dialog.setContentView(dialogLayout);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
    }
}
