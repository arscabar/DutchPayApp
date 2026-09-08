package kr.dutchpay;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.*;
import java.util.regex.*;

final class ReceiptEditor {
    final List<RowEditor> editors = new ArrayList<>();
    final TextView summary;
    final Button copy;
    final Button btnNotion;
    final CheckBox reviewed;
    final NotionSettings notionSettings;
    final Receipt receipt;
    final Uri imageUri;
    final String storeTitle;
    final String receiptDate;
    final EditText etStore;
    final Spinner spCategory;
    
    // Notion Preview Views
    private LinearLayout notionPreviewCard;
    private TextView tvNotionPreviewTitle;
    private TextView tvNotionPreviewAmount;
    private TextView tvNotionPreviewDate;
    private TextView tvNotionPreviewCategory;
    private TextView tvNotionPreviewBlocks;

    String note = "";
    long currentSelectedSum = 0;

    ReceiptEditor(Context c, LinearLayout body, Receipt receipt, Uri uri) {
        this(c, body, null, receipt, uri, new NotionSettings(c), null, null);
    }

    ReceiptEditor(Context c, LinearLayout itemsBody, LinearLayout photoBody, Receipt receipt, Uri uri, NotionSettings settings) {
        this(c, itemsBody, photoBody, receipt, uri, settings, null, null);
    }

    ReceiptEditor(Context c, LinearLayout itemsBody, LinearLayout photoBody, Receipt receipt, Uri uri, NotionSettings settings, Runnable onNavigateToPhoto, Runnable onBackToItems) {
        this.receipt = receipt;
        this.imageUri = uri;
        this.notionSettings = settings;

        storeTitle = extractStoreTitle(receipt.raw);
        receiptDate = extractReceiptDate(receipt.raw);

        // 일반 영수증 / 카드전표 등 품목 표가 없는 경우에도 총액을 기반으로 즉시 정산 및 노션 저장이 가능하도록 자동 생성
        if (receipt.items.isEmpty()) {
            long initialAmount = (receipt.total != null && receipt.total > 0) ? receipt.total : 0L;
            String itemName = (storeTitle != null && !storeTitle.isEmpty() && !storeTitle.equals("영수증 정산")) ? storeTitle : "일반 결제";
            Item fallbackItem = new Item(itemName, initialAmount, 1, initialAmount);
            fallbackItem.warning = (initialAmount > 0) ? "품목 없는 일반 영수증: 총 결제액이 자동 반영되었습니다." : "금액을 직접 확인 후 입력하세요.";
            receipt.items.add(fallbackItem);
        }
        receipt.validate();

        String sumFormatted;
        try { sumFormatted = String.format(Locale.KOREA, "%,d원", receipt.itemSum()); }
        catch (ArithmeticException e) { sumFormatted = "계산 불가"; }

        String printFormatted = (receipt.total == null) ? "미인식" : String.format(Locale.KOREA, "%,d원", receipt.total);

        // ============================================================
        // 1. TOSS-STYLE RECEIPT HERO HEADER (토스 전자영수증 헤더)
        // ============================================================
        LinearLayout headerCard = Ui.card(c, 18);
        headerCard.setPadding(Ui.dp(c, 20), Ui.dp(c, 22), Ui.dp(c, 20), Ui.dp(c, 20));

        // Subtitle badge
        TextView tvReceiptBadge = Ui.badge(c, "RECEIPT · 전자영수증", Ui.COLOR_PRIMARY_LIGHT, Ui.COLOR_PRIMARY);
        headerCard.addView(tvReceiptBadge);

        // Editable Store Title Row (사용자가 직접 상호명/제목 수정 가능)
        LinearLayout titleRow = new LinearLayout(c);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 2));

        etStore = new EditText(c);
        etStore.setText(storeTitle);
        etStore.setTextSize(22);
        etStore.setTypeface(null, android.graphics.Typeface.BOLD);
        etStore.setTextColor(Ui.COLOR_TEXT_MAIN);
        etStore.setBackground(null);
        etStore.setPadding(0, 0, Ui.dp(c, 8), Ui.dp(c, 2));
        etStore.setHint("상호명 / 정산 제목 입력");
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        etStore.setLayoutParams(etLp);
        titleRow.addView(etStore);

        TextView tvEditIcon = Ui.text(c, "✏️", 16);
        tvEditIcon.setPadding(Ui.dp(c, 6), Ui.dp(c, 6), Ui.dp(c, 6), Ui.dp(c, 6));
        tvEditIcon.setBackground(Ui.roundedRect(Ui.COLOR_BG, Ui.COLOR_STROKE, 8, 1));
        tvEditIcon.setOnClickListener(v -> {
            etStore.requestFocus();
            etStore.setSelection(etStore.getText().length());
        });
        titleRow.addView(tvEditIcon);
        headerCard.addView(titleRow);

        // Metadata Chips (결제일시 / 결제수단)
        LinearLayout metaChips = new LinearLayout(c);
        metaChips.setOrientation(LinearLayout.HORIZONTAL);
        metaChips.setGravity(Gravity.CENTER_VERTICAL);
        metaChips.setPadding(0, Ui.dp(c, 4), 0, Ui.dp(c, 10));

        String dateDisplay = (receiptDate == null || receiptDate.isEmpty()) ? "날짜 미확인" : receiptDate;
        TextView tvDateChip = Ui.chip(c, "📅 " + dateDisplay, Ui.COLOR_BG, Ui.COLOR_TEXT_MUTED);
        metaChips.addView(tvDateChip);

        headerCard.addView(metaChips);

        // Category Dropdown Row (노션 '범주' 연동 - 모던 캡슐 드롭박스)
        LinearLayout categoryRow = new LinearLayout(c);
        categoryRow.setOrientation(LinearLayout.HORIZONTAL);
        categoryRow.setGravity(Gravity.CENTER_VERTICAL);
        categoryRow.setBackground(Ui.roundedRect(Color.parseColor("#F8FAFC"), Ui.COLOR_STROKE, 10, 1));
        categoryRow.setPadding(Ui.dp(c, 12), Ui.dp(c, 4), Ui.dp(c, 12), Ui.dp(c, 4));

        TextView tvCatLabel = Ui.text(c, "🏷️ 지출 범주", 13);
        tvCatLabel.setTextColor(Ui.COLOR_TEXT_MUTED);
        tvCatLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        categoryRow.addView(tvCatLabel);

        final String[] categories = new String[]{"식비", "카페/간식", "생활/마트", "교통/차량", "쇼핑", "문화/여가", "의료/건강", "기타"};
        spCategory = new Spinner(c);
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(c, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(catAdapter);
        spCategory.setSelection(0); // 기본값: "식비"
        spCategory.setBackground(null);
        spCategory.setPadding(Ui.dp(c, 10), Ui.dp(c, 8), Ui.dp(c, 6), Ui.dp(c, 8));
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        spCategory.setLayoutParams(spLp);
        categoryRow.addView(spCategory);

        LinearLayout.LayoutParams crLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        crLp.setMargins(0, 0, 0, Ui.dp(c, 14));
        categoryRow.setLayoutParams(crLp);
        headerCard.addView(categoryRow);

        // Total Amount Display (Toss 32sp bold typography)
        summary = Ui.text(c, (receipt.total != null ? printFormatted : sumFormatted), 30);
        summary.setTypeface(null, android.graphics.Typeface.BOLD);
        summary.setTextColor(Ui.COLOR_TEXT_MAIN);
        headerCard.addView(summary);

        TextView tvSubTotal = Ui.text(c, "인쇄 합계: " + printFormatted + "  |  선택 계산: " + sumFormatted, 13);
        tvSubTotal.setTextColor(Ui.COLOR_TEXT_MUTED);
        tvSubTotal.setPadding(0, Ui.dp(c, 4), 0, 0);
        headerCard.addView(tvSubTotal);

        // 원본 영수증 탭으로 바로 이동하는 버튼
        if (onNavigateToPhoto != null) {
            Button btnViewFullPhoto = Ui.subButton(c, "📷 영수증 사진 보기  ›", onNavigateToPhoto);
            LinearLayout.LayoutParams vfLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            vfLp.setMargins(0, Ui.dp(c, 14), 0, 0);
            btnViewFullPhoto.setLayoutParams(vfLp);
            headerCard.addView(btnViewFullPhoto);
        }

        itemsBody.addView(headerCard);

        // Warnings Checkbox
        reviewed = new CheckBox(c);
        reviewed.setText("영수증 원본 대조 확인 완료");
        reviewed.setTextColor(Color.parseColor("#991B1B"));
        reviewed.setTextSize(13);
        reviewed.setPadding(Ui.dp(c, 4), Ui.dp(c, 6), 0, 0);

        if (receipt.warnings.isEmpty()) {
            reviewed.setChecked(true);
            reviewed.setVisibility(View.GONE);
        }

        // Populate photoBody (원본 영수증 탭)
        if (photoBody != null) {
            photoBody.removeAllViews();

            if (onBackToItems != null) {
                Button btnBackToItems = Ui.outlineButton(c, "←  정산 품목으로 돌아가기", Ui.COLOR_STROKE, Ui.COLOR_PRIMARY, onBackToItems);
                btnBackToItems.setTextSize(14);
                btnBackToItems.setPadding(Ui.dp(c, 14), Ui.dp(c, 12), Ui.dp(c, 14), Ui.dp(c, 12));
                LinearLayout.LayoutParams bbLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                bbLp.setMargins(0, 0, 0, Ui.dp(c, 12));
                btnBackToItems.setLayoutParams(bbLp);
                photoBody.addView(btnBackToItems);
            }

            // 1. Original Receipt Photo Card
            LinearLayout photoCard = Ui.card(c);
            photoCard.setPadding(Ui.dp(c, 16), Ui.dp(c, 16), Ui.dp(c, 16), Ui.dp(c, 16));

            TextView tvPhotoTitle = Ui.text(c, "📷 원본 영수증 사진", 16);
            tvPhotoTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            tvPhotoTitle.setTextColor(Ui.COLOR_TEXT_MAIN);
            photoCard.addView(tvPhotoTitle);

            ImageView ivPhoto = new ImageView(c);
            ivPhoto.setImageURI(imageUri);
            ivPhoto.setAdjustViewBounds(true);
            ivPhoto.setScaleType(ImageView.ScaleType.FIT_CENTER);
            ivPhoto.setPadding(0, Ui.dp(c, 10), 0, Ui.dp(c, 10));
            photoCard.addView(ivPhoto);

            Button btnExternal = Ui.subButton(c, "🔍 외부 사진 뷰어로 열기", () -> {
                try {
                    c.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(imageUri, "image/*")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
                } catch (Exception e) {
                    Toast.makeText(c, "사진 보기 앱이 없습니다", Toast.LENGTH_SHORT).show();
                }
            });
            photoCard.addView(btnExternal);
            photoBody.addView(photoCard);

            // 2. Warnings Banner (if any)
            if (!receipt.warnings.isEmpty()) {
                LinearLayout warnCard = Ui.card(c);
                warnCard.setBackground(Ui.roundedRect(Ui.COLOR_WARNING_LIGHT, Color.parseColor("#FCA5A5"), 12, 1));
                warnCard.setPadding(Ui.dp(c, 16), Ui.dp(c, 14), Ui.dp(c, 16), Ui.dp(c, 14));

                TextView warnTitle = Ui.text(c, "확인 필요 항목 (" + receipt.warnings.size() + "건)", 14);
                warnTitle.setTextColor(Ui.COLOR_WARNING);
                warnTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                warnCard.addView(warnTitle);

                for (String w : receipt.warnings) {
                    TextView item = Ui.text(c, "• " + w, 12);
                    item.setTextColor(Color.parseColor("#7F1D1D"));
                    item.setPadding(0, Ui.dp(c, 2), 0, Ui.dp(c, 2));
                    warnCard.addView(item);
                }
                warnCard.addView(reviewed);
                photoBody.addView(warnCard);
            }

            // 3. Raw OCR Text Card
            LinearLayout rawCard = Ui.card(c);
            rawCard.setPadding(Ui.dp(c, 16), Ui.dp(c, 16), Ui.dp(c, 16), Ui.dp(c, 16));

            TextView tvRawTitle = Ui.text(c, "📄 OCR 추출 원문 텍스트", 16);
            tvRawTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            tvRawTitle.setTextColor(Ui.COLOR_TEXT_MAIN);
            rawCard.addView(tvRawTitle);

            TextView raw = Ui.text(c, receipt.raw, 12);
            raw.setTextIsSelectable(true);
            raw.setTextColor(Ui.COLOR_TEXT_MUTED);
            raw.setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 10));
            rawCard.addView(raw);

            Button btnCopyRaw = Ui.subButton(c, "📋 원문 복사", () -> {
                ((ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE))
                    .setPrimaryClip(ClipData.newPlainText("OCR 원문", receipt.raw));
                Toast.makeText(c, "OCR 원문이 클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show();
            });
            rawCard.addView(btnCopyRaw);
            photoBody.addView(rawCard);
        }

        // ============================================================
        // 2. ITEMS LIST CONTAINER (토스 스타일의 상세 내역 카드)
        // ============================================================
        LinearLayout itemsCard = Ui.card(c, 16);
        itemsCard.setPadding(Ui.dp(c, 18), Ui.dp(c, 18), Ui.dp(c, 18), Ui.dp(c, 18));

        LinearLayout itemsHeader = new LinearLayout(c);
        itemsHeader.setOrientation(LinearLayout.HORIZONTAL);
        itemsHeader.setGravity(Gravity.CENTER_VERTICAL);
        itemsHeader.setPadding(0, 0, 0, Ui.dp(c, 12));

        TextView tvItemsTitle = Ui.text(c, "결제 및 정산 품목 (" + receipt.items.size() + "건)", 15);
        tvItemsTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvItemsTitle.setTextColor(Ui.COLOR_TEXT_MAIN);
        LinearLayout.LayoutParams itLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tvItemsTitle.setLayoutParams(itLp);
        itemsHeader.addView(tvItemsTitle);

        Button btnQuickAdd = Ui.subButton(c, "＋ 직접 추가", () -> {
            Item manual = new Item("직접 추가", 0, 1, 0);
            manual.warning = "직접 입력한 금액·수량을 확인하세요";
            add(c, (LinearLayout) itemsCard.getChildAt(1), manual);
            update();
        });
        itemsHeader.addView(btnQuickAdd);
        itemsCard.addView(itemsHeader);

        LinearLayout rows = new LinearLayout(c);
        rows.setOrientation(LinearLayout.VERTICAL);
        itemsCard.addView(rows);

        for (int i = 0; i < receipt.items.size(); i++) {
            Item item = receipt.items.get(i);
            add(c, rows, item);
            if (i < receipt.items.size() - 1) {
                View div = new View(c);
                div.setBackgroundColor(Color.parseColor("#F2F4F6"));
                LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(c, 1));
                dLp.setMargins(0, Ui.dp(c, 2), 0, Ui.dp(c, 2));
                div.setLayoutParams(dLp);
                rows.addView(div);
            }
        }
        itemsBody.addView(itemsCard);

        // ============================================================
        // 3. NOTION UPLOAD PREVIEW (노션에 어떻게 저장되는지 시각화 카드)
        // ============================================================
        createNotionPreviewCard(c, itemsBody);

        // ============================================================
        // 4. TOSS-STYLE PRIMARY ACTION BUTTON & SUB ACTIONS
        // ============================================================
        LinearLayout actionCard = Ui.card(c, 16);
        actionCard.setPadding(Ui.dp(c, 18), Ui.dp(c, 20), Ui.dp(c, 18), Ui.dp(c, 18));

        btnNotion = Ui.button(c, "Notion에 저장하기  🚀", Ui.COLOR_PRIMARY, Color.WHITE, () -> exportToNotion(c));
        btnNotion.setTextSize(16);
        actionCard.addView(btnNotion);

        copy = Ui.button(c, "계산 내역 복사하기", Color.parseColor("#F2F4F6"), Ui.COLOR_TEXT_MAIN, () -> {
            new AlertDialog.Builder(c)
                .setTitle("정산 내역 확인")
                .setMessage(note)
                .setNegativeButton("돌아가기", null)
                .setPositiveButton("클립보드에 복사", (dialog, which) -> {
                    ((ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE))
                        .setPrimaryClip(ClipData.newPlainText("정산 비고", note));
                    Toast.makeText(c, "정산 내역이 클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show();
                }).show();
        });
        copy.setTextSize(14);
        actionCard.addView(copy);

        LinearLayout subBar = new LinearLayout(c);
        subBar.setOrientation(LinearLayout.HORIZONTAL);
        subBar.setGravity(Gravity.CENTER);
        subBar.setPadding(0, Ui.dp(c, 8), 0, 0);



        TextView raw = Ui.text(c, receipt.raw, 11);
        raw.setTextIsSelectable(true);
        raw.setTextColor(Ui.COLOR_TEXT_MUTED);
        raw.setVisibility(View.GONE);

        Button btnRaw = createSubTextButton(c, "OCR 원문", () -> {
            raw.setVisibility(raw.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });
        subBar.addView(btnRaw);
        actionCard.addView(subBar);

        actionCard.addView(raw);
        itemsBody.addView(actionCard);

        reviewed.setOnCheckedChangeListener((v, checked) -> update());
        spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { updateNotionPreview(); }
            @Override
            public void onNothingSelected(AdapterView<?> p) {}
        });

        // 실시간 상호명 변경 리스너
        etStore.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { updateNotionPreview(); }
            public void afterTextChanged(android.text.Editable s) {}
        });

        update();
    }

    private void createNotionPreviewCard(Context c, LinearLayout parent) {
        notionPreviewCard = Ui.card(c, 16);
        notionPreviewCard.setPadding(Ui.dp(c, 18), Ui.dp(c, 16), Ui.dp(c, 18), Ui.dp(c, 16));

        // Header with Notion Icon & Toggle
        LinearLayout previewHeader = new LinearLayout(c);
        previewHeader.setOrientation(LinearLayout.HORIZONTAL);
        previewHeader.setGravity(Gravity.CENTER_VERTICAL);
        previewHeader.setPadding(0, 0, 0, Ui.dp(c, 10));

        TextView tvTitle = Ui.text(c, "📋 Notion에 이렇게 올라가요", 14);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(Ui.COLOR_TEXT_MAIN);
        LinearLayout.LayoutParams thLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tvTitle.setLayoutParams(thLp);
        previewHeader.addView(tvTitle);

        TextView tvBadge = Ui.badge(c, "미리보기", Color.parseColor("#F1F5F9"), Color.parseColor("#475569"));
        previewHeader.addView(tvBadge);
        notionPreviewCard.addView(previewHeader);

        // Simulated Notion Table Row Container
        LinearLayout tableBox = new LinearLayout(c);
        tableBox.setOrientation(LinearLayout.VERTICAL);
        tableBox.setBackground(Ui.roundedRect(Color.parseColor("#F8FAFC"), Color.parseColor("#E2E8F0"), 12, 1));
        tableBox.setPadding(Ui.dp(c, 14), Ui.dp(c, 12), Ui.dp(c, 14), Ui.dp(c, 12));

        // 1. Title property
        tvNotionPreviewTitle = createPreviewPropertyRow(c, tableBox, "🏷️ 제목", storeTitle);
        // 2. Amount property
        tvNotionPreviewAmount = createPreviewPropertyRow(c, tableBox, "💰 금액", "0원");
        // 3. Date property
        tvNotionPreviewDate = createPreviewPropertyRow(c, tableBox, "📅 날짜", receiptDate.isEmpty() ? "오늘" : receiptDate);
        // 4. Category property
        tvNotionPreviewCategory = createPreviewPropertyRow(c, tableBox, "📂 범주", "식비");

        // Divider
        tableBox.addView(Ui.divider(c));

        // 5. Page body bullet blocks
        TextView tvBlockLabel = Ui.text(c, "📝 페이지 본문 블록 (품목 상세 내역):", 12);
        tvBlockLabel.setTextColor(Ui.COLOR_TEXT_MUTED);
        tvBlockLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvBlockLabel.setPadding(0, 0, 0, Ui.dp(c, 4));
        tableBox.addView(tvBlockLabel);

        tvNotionPreviewBlocks = Ui.text(c, "• 품목 내역이 생성됩니다.", 12);
        tvNotionPreviewBlocks.setTextColor(Color.parseColor("#334155"));
        tvNotionPreviewBlocks.setLineSpacing(Ui.dp(c, 2), 1.0f);
        tableBox.addView(tvNotionPreviewBlocks);

        notionPreviewCard.addView(tableBox);
        parent.addView(notionPreviewCard);
    }

    private TextView createPreviewPropertyRow(Context c, LinearLayout container, String label, String initialValue) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dp(c, 3), 0, Ui.dp(c, 3));

        TextView tvLabel = Ui.text(c, label, 12);
        tvLabel.setTextColor(Ui.COLOR_TEXT_MUTED);
        LinearLayout.LayoutParams lpLabel = new LinearLayout.LayoutParams(Ui.dp(c, 68), ViewGroup.LayoutParams.WRAP_CONTENT);
        tvLabel.setLayoutParams(lpLabel);
        row.addView(tvLabel);

        TextView tvVal = Ui.text(c, initialValue, 13);
        tvVal.setTextColor(Ui.COLOR_TEXT_MAIN);
        tvVal.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(tvVal);

        container.addView(row);
        return tvVal;
    }

    private void updateNotionPreview() {
        if (tvNotionPreviewTitle == null) return;
        String curStore = (etStore != null && !etStore.getText().toString().trim().isEmpty())
            ? etStore.getText().toString().trim() : storeTitle;
        tvNotionPreviewTitle.setText(curStore);

        tvNotionPreviewAmount.setText(String.format(Locale.KOREA, "%,d원", currentSelectedSum));

        String curDate = (receiptDate != null && !receiptDate.trim().isEmpty()) ? receiptDate : "오늘 자동 설정";
        tvNotionPreviewDate.setText(curDate);

        String curCat = (spCategory != null && spCategory.getSelectedItem() != null)
            ? spCategory.getSelectedItem().toString() : "식비";
        tvNotionPreviewCategory.setText(curCat);

        // Build simplified bullet list
        StringBuilder blocks = new StringBuilder();
        int count = 0;
        for (RowEditor e : editors) {
            if (e.selected.isChecked() && !e.source.includedDiscount) {
                blocks.append("• ").append(e.name.getText().toString()).append(" - ")
                      .append(e.result.getText().toString()).append("\n");
                count++;
                if (count >= 5) {
                    blocks.append("... 외 ").append(editors.size() - 5).append("건\n");
                    break;
                }
            }
        }
        if (count == 0) {
            blocks.append("• 선택된 품목이 없습니다.");
        }
        tvNotionPreviewBlocks.setText(blocks.toString().trim());
    }

    private Button createSubTextButton(Context c, String label, Runnable action) {
        Button b = new Button(c);
        b.setText(label);
        b.setTextSize(12);
        b.setTextColor(Ui.COLOR_TEXT_MUTED);
        b.setBackground(Ui.roundedRect(Color.TRANSPARENT, Color.TRANSPARENT, 0, 0));
        b.setPadding(Ui.dp(c, 8), Ui.dp(c, 4), Ui.dp(c, 8), Ui.dp(c, 4));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private TextView createDotSeparator(Context c) {
        TextView d = new TextView(c);
        d.setText("·");
        d.setTextColor(Color.parseColor("#CBD5E1"));
        d.setPadding(Ui.dp(c, 4), 0, Ui.dp(c, 4), 0);
        return d;
    }

    void add(Context c, LinearLayout rows, Item item) {
        RowEditor editor = new RowEditor(c, item, this::update);
        editors.add(editor);
        rows.addView(editor);
    }

    void update() {
        long sum = 0;
        String curStore = (etStore != null && !etStore.getText().toString().trim().isEmpty())
            ? etStore.getText().toString().trim() : storeTitle;
        StringBuilder text = new StringBuilder("상호: " + curStore + " (" + receiptDate + ")\n\n");
        boolean valid = true;
        boolean confirmed = reviewed.getVisibility() != View.VISIBLE || reviewed.isChecked();
        boolean selected = false;

        for (RowEditor e : editors) {
            try {
                long amt = e.amount();
                sum = Math.addExact(sum, amt);
                if (e.selected.isChecked() || e.source.includedDiscount) {
                    text.append(e.note()).append('\n');
                }
            } catch (RuntimeException ex) {
                e.result.setText("확인 필요");
                valid = false;
            }
            selected |= e.selected.isChecked();
            confirmed &= e.confirmed();
        }

        currentSelectedSum = sum;
        note = text + "\n최종 선택 합계: " + String.format(Locale.KOREA, "%,d원", sum);

        if (valid) {
            String sumStr = String.format(Locale.KOREA, "%,d원", sum);
            summary.setText(sumStr);
        } else {
            summary.setText("금액 확인 필요");
        }

        boolean canExport = valid && selected && confirmed;
        copy.setEnabled(canExport);
        btnNotion.setEnabled(canExport);

        updateNotionPreview();
    }

    private void exportToNotion(Context c) {
        final String finalStoreTitle = (etStore != null && !etStore.getText().toString().trim().isEmpty())
            ? etStore.getText().toString().trim() : storeTitle;

        if (!notionSettings.isConfigured()) {
            new AlertDialog.Builder(c)
                .setTitle("Notion 설정 필요")
                .setMessage("노션 데이터베이스에 적재하려면 API 키와 데이터베이스 ID 설정이 필요합니다. 지금 설정하시겠습니까?")
                .setPositiveButton("설정하기", (d, w) -> notionSettings.showDialog(c, () -> exportToNotion(c)))
                .setNegativeButton("취소", null)
                .show();
            return;
        }

        ProgressDialog progress = new ProgressDialog(c);
        progress.setMessage("Notion 테이블에 등록하는 중입니다…");
        progress.setCancelable(false);
        progress.show();

        final String finalCategory = (spCategory != null && spCategory.getSelectedItem() != null) ? spCategory.getSelectedItem().toString().trim() : "식비";
        final String finalDate = (receiptDate != null && !receiptDate.trim().isEmpty())
            ? receiptDate
            : new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.KOREA).format(new java.util.Date());

        NotionClient.createPage(notionSettings, finalStoreTitle, currentSelectedSum, finalDate, note, finalCategory, new NotionClient.Callback() {
            @Override
            public void onSuccess(String pageUrl) {
                progress.dismiss();
                new AlertDialog.Builder(c)
                    .setTitle("🎉 Notion 등록 완료!")
                    .setMessage("영수증 정산 내역이 노션 데이터베이스에 성공적으로 저장되었습니다.\n\n"
                        + "• 상호: " + finalStoreTitle + "\n"
                        + "• 금액: " + String.format(Locale.KOREA, "%,d원", currentSelectedSum) + "\n"
                        + "• 날짜: " + finalDate + "\n"
                        + "• 범주: " + (finalCategory.isEmpty() ? "없음" : finalCategory))
                    .setPositiveButton("노션 페이지 열기 ↗", (d, w) -> {
                        try {
                            c.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(pageUrl)));
                        } catch (Exception ignored) {}
                    })
                    .setNegativeButton("확인", null)
                    .show();
            }

            @Override
            public void onError(String message) {
                progress.dismiss();
                new AlertDialog.Builder(c)
                    .setTitle("Notion 등록 실패")
                    .setMessage(message + "\n\n우측 상단 ⚙️ 노션 설정에서 API Key, Database ID, 컬럼명이 올바른지 확인해 주세요.")
                    .setPositiveButton("설정 확인", (d, w) -> notionSettings.showDialog(c, null))
                    .setNegativeButton("닫기", null)
                    .show();
            }
        });
    }

        private String extractStoreTitle(String raw) {
        // 1. Explicit store labels across all lines (상호, 가맹점명, 매장명, 상호명, 점포명, 가맹점)
        if (raw != null && !raw.trim().isEmpty()) {
            String[] lines = raw.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                // 절대 대표자나 사업자 라인을 상호로 취급하지 않음
                if (trimmed.startsWith("대표") || trimmed.startsWith("사업자") || trimmed.contains("사업자번호")) continue;
                if (trimmed.matches(".*(?:상\\s*호|가\\s*[맹행]\\s*점(?:명|범)?|매\\s*장\\s*명|상\\s*호\\s*명|점\\s*포\\s*명)\\s*[:：\\s].*")) {
                    String cand = trimmed.replaceFirst(".*(?:상\\s*호|가\\s*[맹행]\\s*점(?:명|범)?|매\\s*장\\s*명|상\\s*호\\s*명|점\\s*포\\s*명)\\s*[:：]?\\s*", "");
                    cand = cleanStoreCandidate(cand);
                    if (isValidStoreName(cand)) return cand;
                }
            }

            // 2. Known brand dictionary matching across all lines
            String[] brands = {
                "GS25", "CU", "씨유", "세븐일레븐", "7-ELEVEN", "이마트24", "올리브영", "다이소",
                "버거킹", "BURGER KING", "맥도날드", "MCDONALD", "스타벅스", "STARBUCKS",
                "이디야", "투썸플레이스", "메가커피", "컴포즈", "빽다방", "애슐리", "애슐리퀸즈",
                "파리바게뜨", "뚜레쥬르", "써브웨이", "SUBWAY", "롯데리아",
                "맘스터치", "KFC", "한솥도시락", "김밥천국", "육전국밥", "육전 국밥", "신사골감자탕"
            };
            for (String line : lines) {
                String lineClean = cleanStoreCandidate(line);
                if (isSystemOrHeaderLine(line.replaceAll("\\s+", ""))) continue;
                for (String brand : brands) {
                    if (lineClean.toLowerCase().contains(brand.toLowerCase()) && isValidStoreName(lineClean)) {
                        return lineClean;
                    }
                }
            }

            // 3. Store suffix pattern (점, 식당, 국밥, 초밥, 갈비, 통닭, 치킨, 피자, 베이커리, 커피, 카페 등)
            java.util.regex.Pattern suffixPattern = java.util.regex.Pattern.compile("([가-힣A-Za-z0-9\\s]{2,}(?:점|식당|국밥|초밥|갈비|통닭|치킨|피자|베이커리|커피|카페|헤어|약국|마트|상회|본점|직영점))");
            for (int i = 0; i < Math.min(lines.length, 15); i++) {
                String line = lines[i].trim();
                String noSpace = line.replaceAll("\\s+", "");
                if (isSystemOrHeaderLine(noSpace)) continue;
                java.util.regex.Matcher m = suffixPattern.matcher(line);
                if (m.find()) {
                    String cand = cleanStoreCandidate(m.group(1));
                    if (isValidStoreName(cand) && !cand.equals("가맹점") && !cand.equals("지점")) {
                        return cand;
                    }
                }
            }

            // 4. Fallback: inspect top 8 non-system lines
            for (int i = 0; i < Math.min(lines.length, 8); i++) {
                String line = lines[i].trim();
                String noSpace = line.replaceAll("\\s+", "");
                if (isSystemOrHeaderLine(noSpace)) continue;
                String cand = cleanStoreCandidate(line);
                if (isValidStoreName(cand) && cand.matches(".*[가-힣A-Za-z].*")) {
                    return cand;
                }
            }
        }

        // 5. Smart Item-based fallback: 영수증에 상호명이 없는 경우 품목명 기반으로 생성 (예: "베노프 단백질바 외 2건")
        if (receipt != null && receipt.items != null && !receipt.items.isEmpty()) {
            Item first = receipt.items.get(0);
            String firstName = first.name.replaceAll("^[0-9\\[\\](){}:*#₩￦,._\\s\\-~`'\"|]+", "").trim();
            if (firstName.length() > 10) firstName = firstName.substring(0, 10).trim();
            int otherCount = receipt.items.size() - 1;
            if (otherCount > 0) {
                return firstName + " 외 " + otherCount + "건";
            } else if (!firstName.isEmpty()) {
                return firstName + " 결제";
            }
        }

        return "영수증 정산";
    }

    private boolean isValidStoreName(String cand) {
        if (cand == null || cand.trim().length() < 2) return false;
        String noSpace = cand.replaceAll("\\s+", "");
        if (isSystemOrHeaderLine(noSpace)) return false;
        // 대표자, 사업자, 전화번호 패턴 철저 배제
        if (noSpace.contains("대표") || noSpace.contains("사업자") || noSpace.contains("등록번호")
            || noSpace.contains("TEL") || noSpace.contains("전화") || noSpace.matches(".*\\d{3}-\\d{2}-\\d{5}.*")) {
            return false;
        }
        return true;
    }

    private String cleanStoreCandidate(String name) {
        if (name == null) return "";
        // Strip metadata keywords that follow store name on the same line
        name = name.replaceFirst("(?:사업자|등록번호|TEL|Tel|대표자|대표|주소|전화|단말기|POS|가맹점번호).*", "");
        // Strip company forms and receipt status tags like (주), [고객용], [매출], [정상]
        name = name.replaceFirst("^\\s*[\\(\\[\\{]?(?:주|유|합|사단법인|주식회사|고객용|가맹점용|정상|매출|재발행|영수증|식사|주문|포장)[\\)\\]\\}]?\\s*", "");
        // Strip order numbers like "284번"
        name = name.replaceAll("^\\d{1,4}\\s*번\\s*", "");
        name = name.replaceAll("\\s*\\d{1,4}\\s*번$", "");
        // Strip junk symbols from start and end
        name = name.replaceAll("^[0-9\\[\\](){}:*#₩￦,._\\s\\-~`'\"|]+", "");
        name = name.replaceAll("[0-9\\[\\](){}:*#₩￦,._\\s\\-~`'\"|]+$", "");
        return name.trim();
    }

    private boolean isSystemOrHeaderLine(String noSpace) {
        if (noSpace == null || noSpace.isEmpty()) return true;
        String[] bad = {
            "영수증", "사업자", "등록번호", "전화", "TEL", "Tel", "대표자", "대표", "주소",
            "가맹점번호", "신용승인", "카드전표", "승인번호", "POS", "테이블", "단말기",
            "매출전표", "고객용", "카드종류", "판매일자", "결제금액", "주문번호", "제품받는곳",
            "상품명", "품목명", "단가", "수량", "금액", "합계", "총결제", "취소", "재발행"
        };
        for (String b : bad) {
            if (noSpace.contains(b)) return true;
        }
        return false;
    }

    private String extractReceiptDate(String raw) {
        if (raw == null) return "";
        // 1. Standard 20xx / 19xx YYYY-MM-DD
        Matcher m = Pattern.compile("((?:19|20)\\d{2})\\s*[-./년]\\s*(\\d{1,2})\\s*[-./월]\\s*(\\d{1,2})").matcher(raw);
        while (m.find()) {
            try {
                int year = Integer.parseInt(m.group(1));
                int month = Integer.parseInt(m.group(2));
                int day = Integer.parseInt(m.group(3));
                if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                    return String.format(Locale.KOREA, "%04d-%02d-%02d", year, month, day);
                }
            } catch (Exception ignored) {}
        }
        // 2. OCR font variations like 202F, 1026, 202b
        Matcher m2 = Pattern.compile("([12][09]2[0-9a-zA-Z])\\s*[-./년노]\\s*(\\d{1,2})\\s*[-./월노]\\s*(\\d{1,2})").matcher(raw);
        while (m2.find()) {
            try {
                int month = Integer.parseInt(m2.group(2));
                int day = Integer.parseInt(m2.group(3));
                if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                    return String.format(Locale.KOREA, "2026-%02d-%02d", month, day);
                }
            } catch (Exception ignored) {}
        }
        // 3. 2-digit year YY-MM-DD
        Matcher m3 = Pattern.compile("(?<!\\d)(2[0-9])\\s*[-./년]\\s*(\\d{1,2})\\s*[-./월]\\s*(\\d{1,2})").matcher(raw);
        while (m3.find()) {
            try {
                int year = Integer.parseInt(m3.group(1)) + 2000;
                int month = Integer.parseInt(m3.group(2));
                int day = Integer.parseInt(m3.group(3));
                if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                    return String.format(Locale.KOREA, "%04d-%02d-%02d", year, month, day);
                }
            } catch (Exception ignored) {}
        }
        return "";
    }
}
