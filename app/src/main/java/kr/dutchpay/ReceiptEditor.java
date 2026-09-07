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
        receipt.validate();

        storeTitle = extractStoreTitle(receipt.raw);
        receiptDate = extractReceiptDate(receipt.raw);

        String sumFormatted;
        try { sumFormatted = String.format(Locale.KOREA, "%,d원", receipt.itemSum()); }
        catch (ArithmeticException e) { sumFormatted = "계산 불가"; }

        String printFormatted = (receipt.total == null) ? "미인식" : String.format(Locale.KOREA, "%,d원", receipt.total);

        // ============================================================
        // 1. TOSS-STYLE RECEIPT HERO HEADER (토스 전자영수증 헤더)
        // ============================================================
        LinearLayout headerCard = Ui.card(c);
        headerCard.setPadding(Ui.dp(c, 20), Ui.dp(c, 20), Ui.dp(c, 20), Ui.dp(c, 20));

        // Editable Store Title Row (사용자가 직접 상호명/제목 수정 가능)
        LinearLayout titleRow = new LinearLayout(c);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        etStore = new EditText(c);
        etStore.setText(storeTitle);
        etStore.setTextSize(20);
        etStore.setTypeface(null, android.graphics.Typeface.BOLD);
        etStore.setTextColor(Ui.COLOR_TEXT_MAIN);
        etStore.setBackground(null);
        etStore.setPadding(0, 0, Ui.dp(c, 8), Ui.dp(c, 4));
        etStore.setHint("상호명 / 정산 제목 입력");
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        etStore.setLayoutParams(etLp);
        titleRow.addView(etStore);

        TextView tvEditIcon = Ui.text(c, "✏️", 16);
        tvEditIcon.setPadding(Ui.dp(c, 4), 0, Ui.dp(c, 4), 0);
        tvEditIcon.setOnClickListener(v -> {
            etStore.requestFocus();
            etStore.setSelection(etStore.getText().length());
        });
        titleRow.addView(tvEditIcon);
        headerCard.addView(titleRow);

        TextView tvMeta = Ui.text(c, (receiptDate.isEmpty() ? "일시 미확인" : receiptDate) + " · " + receipt.method, 13);
        tvMeta.setTextColor(Ui.COLOR_TEXT_MUTED);
        tvMeta.setPadding(0, Ui.dp(c, 2), 0, Ui.dp(c, 14));
        headerCard.addView(tvMeta);

        summary = Ui.text(c, (receipt.total != null ? printFormatted : sumFormatted), 32);
        summary.setTypeface(null, android.graphics.Typeface.BOLD);
        summary.setTextColor(Ui.COLOR_TEXT_MAIN);
        headerCard.addView(summary);

        TextView tvSubTotal = Ui.text(c, "인쇄 합계: " + printFormatted + "  |  추출 합계: " + sumFormatted, 13);
        tvSubTotal.setTextColor(Ui.COLOR_TEXT_MUTED);
        tvSubTotal.setPadding(0, Ui.dp(c, 4), 0, 0);
        headerCard.addView(tvSubTotal);

        // 원본 영수증 탭으로 바로 이동하는 버튼
        if (onNavigateToPhoto != null) {
            Button btnViewFullPhoto = new Button(c);
            btnViewFullPhoto.setText("📷 원본 영수증 전체 보기  ›");
            btnViewFullPhoto.setTextSize(13);
            btnViewFullPhoto.setTypeface(null, android.graphics.Typeface.BOLD);
            btnViewFullPhoto.setTextColor(Color.parseColor("#2563EB"));
            btnViewFullPhoto.setBackground(Ui.roundedRect(Color.parseColor("#EFF6FF"), Color.parseColor("#BFDBFE"), 10, 1));
            btnViewFullPhoto.setPadding(Ui.dp(c, 12), Ui.dp(c, 10), Ui.dp(c, 12), Ui.dp(c, 10));
            LinearLayout.LayoutParams vfLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            vfLp.setMargins(0, Ui.dp(c, 12), 0, 0);
            btnViewFullPhoto.setLayoutParams(vfLp);
            btnViewFullPhoto.setOnClickListener(v -> onNavigateToPhoto.run());
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

            // 0. 정산 품목으로 돌아가기 버튼
            if (onBackToItems != null) {
                Button btnBackToItems = new Button(c);
                btnBackToItems.setText("←  정산 품목으로 돌아가기");
                btnBackToItems.setTextSize(14);
                btnBackToItems.setTypeface(null, android.graphics.Typeface.BOLD);
                btnBackToItems.setTextColor(Ui.COLOR_PRIMARY);
                btnBackToItems.setBackground(Ui.roundedRect(Color.WHITE, Ui.COLOR_STROKE, 10, 1));
                btnBackToItems.setPadding(Ui.dp(c, 14), Ui.dp(c, 12), Ui.dp(c, 14), Ui.dp(c, 12));
                LinearLayout.LayoutParams bbLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                bbLp.setMargins(0, 0, 0, Ui.dp(c, 12));
                btnBackToItems.setLayoutParams(bbLp);
                btnBackToItems.setOnClickListener(v -> onBackToItems.run());
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

            Button btnExternal = createSubTextButton(c, "🔍 외부 사진 뷰어로 열기", () -> {
                try {
                    c.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(imageUri, "image/*")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
                } catch (Exception e) {
                    Toast.makeText(c, "사진 보기 앱이 없습니다", Toast.LENGTH_SHORT).show();
                }
            });
            btnExternal.setTextColor(Color.parseColor("#2563EB"));
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

            Button btnCopyRaw = createSubTextButton(c, "📋 원문 복사", () -> {
                ((ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE))
                    .setPrimaryClip(ClipData.newPlainText("OCR 원문", receipt.raw));
                Toast.makeText(c, "OCR 원문이 클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show();
            });
            rawCard.addView(btnCopyRaw);
            photoBody.addView(rawCard);
        }

        // ============================================================
        // 2. ITEMS LIST CONTAINER (토스 스타일의 하나의 연속된 카드)
        // ============================================================
        LinearLayout itemsCard = Ui.card(c);
        itemsCard.setPadding(Ui.dp(c, 16), Ui.dp(c, 16), Ui.dp(c, 16), Ui.dp(c, 16));

        TextView tvItemsTitle = Ui.text(c, "결제 및 정산 품목 (" + receipt.items.size() + "건)", 15);
        tvItemsTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvItemsTitle.setTextColor(Ui.COLOR_TEXT_MAIN);
        tvItemsTitle.setPadding(0, 0, 0, Ui.dp(c, 12));
        itemsCard.addView(tvItemsTitle);

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
        // 3. TOSS-STYLE PRIMARY ACTION BUTTON & SUB ACTIONS
        // ============================================================
        LinearLayout actionCard = Ui.card(c);
        actionCard.setPadding(Ui.dp(c, 16), Ui.dp(c, 18), Ui.dp(c, 16), Ui.dp(c, 16));

        // Primary Action: Notion Export (Toss Blue)
        btnNotion = Ui.button(c, "Notion에 저장하기", Ui.COLOR_PRIMARY, Color.WHITE, () -> exportToNotion(c));
        btnNotion.setTextSize(16);
        actionCard.addView(btnNotion);

        // Secondary Action: Copy Note
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

        // Sub Actions Bar (직접 추가, 원본 사진, 원문 보기)
        LinearLayout subBar = new LinearLayout(c);
        subBar.setOrientation(LinearLayout.HORIZONTAL);
        subBar.setGravity(Gravity.CENTER);
        subBar.setPadding(0, Ui.dp(c, 8), 0, 0);

        Button btnAdd = createSubTextButton(c, "＋ 품목 추가", () -> {
            Item manual = new Item("직접 추가", 0, 1, 0);
            manual.warning = "직접 입력한 금액·수량을 확인하세요";
            add(c, rows, manual);
            update();
        });
        subBar.addView(btnAdd);

        subBar.addView(createDotSeparator(c));

        Button btnOrig = createSubTextButton(c, "원본 사진", () -> {
            try {
                c.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(imageUri, "image/*")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
            } catch (Exception e) {
                Toast.makeText(c, "사진 보기 앱이 없습니다", Toast.LENGTH_SHORT).show();
            }
        });
        subBar.addView(btnOrig);

        subBar.addView(createDotSeparator(c));

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
        update();
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
        StringBuilder text = new StringBuilder("상호: " + storeTitle + " (" + receiptDate + ")\n\n");
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
        progress.setMessage("Notion 테이블에 적재 중입니다…");
        progress.setCancelable(false);
        progress.show();

        NotionClient.createPage(notionSettings, finalStoreTitle, currentSelectedSum, receiptDate, note, new NotionClient.Callback() {
            @Override
            public void onSuccess(String pageUrl) {
                progress.dismiss();
                new AlertDialog.Builder(c)
                    .setTitle("🎉 Notion 적재 완료")
                    .setMessage("영수증 정산 내역이 노션 테이블에 성공적으로 추가되었습니다!\n\n• 상호: " + finalStoreTitle + "\n• 금액: " + String.format(Locale.KOREA, "%,d원", currentSelectedSum) + "\n• 날짜: " + receiptDate)
                    .setPositiveButton("노션 페이지 열기", (d, w) -> {
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
                    .setTitle("Notion 적재 실패")
                    .setMessage(message + "\n\n우측 상단 ⚙️ 노션 설정에서 API Key, Database ID, 컬럼명이 올바른지 확인해 주세요.")
                    .setPositiveButton("설정 확인", (d, w) -> notionSettings.showDialog(c, null))
                    .setNegativeButton("닫기", null)
                    .show();
            }
        });
    }

    private String extractStoreTitle(String raw) {
        if (raw == null || raw.isEmpty()) return "영수증 정산";
        String[] lines = raw.split("\n");
        // 1. Look for explicit store name labels (excluding business registration numbers)
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.contains("사업자") || trimmed.contains("등록번호") || trimmed.contains("전화") || trimmed.contains("TEL")) continue;
            if (trimmed.matches(".*(?:상\\s*호|가맹점명|매장명|상호명)\\s*[:：].*")) {
                String name = trimmed.replaceFirst(".*(?:상\\s*호|가맹점명|매장명|상호명)\\s*[:：]?\\s*", "").trim();
                if (!name.isEmpty() && name.length() >= 2) return name;
            }
        }
        // 2. Look for top candidate lines in first 8 lines
        int limit = Math.min(lines.length, 8);
        for (int i = 0; i < limit; i++) {
            String t = lines[i].trim();
            if (t.isEmpty() || t.length() < 2) continue;
            if (t.matches(".*(영수증|사업자|등록|전화|TEL|Tel|대표자|주소|가맹점번호|승인|POS|테이블|단말기|매출|고객|카드|일시|결제).*")) continue;
            if (t.matches(".*[가-힣A-Za-z].*") && !t.matches("^[0-9\\W_]+$")) {
                return t;
            }
        }
        return "영수증 정산";
    }

    private String extractReceiptDate(String raw) {
        if (raw == null) return "";
        Matcher m = Pattern.compile("(\\d{4}[-./]\\d{2}[-./]\\d{2})").matcher(raw);
        if (m.find()) return m.group(1);
        return "";
    }
}
