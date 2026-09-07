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
    final String sourceSummary;
    final NotionSettings notionSettings;
    final Receipt receipt;
    final Uri imageUri;
    String note = "";
    long currentSelectedSum = 0;

    ReceiptEditor(Context c, LinearLayout body, Receipt receipt, Uri uri) {
        this(c, body, receipt, uri, new NotionSettings(c));
    }

    ReceiptEditor(Context c, LinearLayout body, Receipt receipt, Uri uri, NotionSettings settings) {
        this.receipt = receipt;
        this.imageUri = uri;
        this.notionSettings = settings;
        receipt.validate();

        String sum;
        try { sum = String.format(Locale.KOREA, "%,d원", receipt.itemSum()); }
        catch (ArithmeticException e) { sum = "계산 불가"; }

        String printed = (receipt.total == null) ? "미인식" : String.format(Locale.KOREA, "%,d원", receipt.total);
        sourceSummary = "원본 인쇄 합계: " + printed + "\n원본 추출 합계: " + sum;

        // 1. Receipt Overview Card
        LinearLayout summaryCard = Ui.card(c);
        TextView cardTitle = Ui.text(c, "영수증 인식 결과", 17);
        cardTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        summaryCard.addView(cardTitle);

        TextView totalsText = Ui.text(c, sourceSummary, 15);
        totalsText.setPadding(0, Ui.dp(c, 6), 0, Ui.dp(c, 10));
        summaryCard.addView(totalsText);

        Button btnViewOrig = Ui.button(c, "🖼️  원본 사진 대조하기", Color.parseColor("#475569"), Color.WHITE, () -> {
            try {
                c.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "image/*")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(c, "사진 보기 앱을 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            }
        });
        summaryCard.addView(btnViewOrig);
        body.addView(summaryCard);

        // 2. Warnings Card (if any issues need review)
        if (!receipt.warnings.isEmpty()) {
            LinearLayout warnCard = Ui.card(c);
            warnCard.setBackground(Ui.roundedRect(Ui.COLOR_WARNING_LIGHT, Ui.COLOR_WARNING, 14, 1));

            TextView warnTitle = Ui.text(c, "⚠️ 확인 필요 사항 (" + receipt.warnings.size() + "건)", 15);
            warnTitle.setTextColor(Ui.COLOR_WARNING);
            warnTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            warnCard.addView(warnTitle);

            for (String w : receipt.warnings) {
                TextView item = Ui.text(c, "• " + w, 13);
                item.setTextColor(Color.parseColor("#92400E"));
                item.setPadding(0, Ui.dp(c, 2), 0, Ui.dp(c, 2));
                warnCard.addView(item);
            }
            body.addView(warnCard);
        }

        // Reviewed Checkbox
        reviewed = new CheckBox(c);
        reviewed.setText("원본의 전체 금액과 할인·품목 내역을 확인했습니다");
        reviewed.setTextColor(Ui.COLOR_TEXT_MAIN);
        reviewed.setTextSize(14);
        reviewed.setPadding(Ui.dp(c, 6), Ui.dp(c, 6), Ui.dp(c, 6), Ui.dp(c, 10));
        reviewed.setVisibility(receipt.warnings.isEmpty() ? View.GONE : View.VISIBLE);
        body.addView(reviewed);

        // 3. Items List Section Header
        TextView itemsHeader = Ui.text(c, "정산 항목 및 품목별 편집", 16);
        itemsHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        itemsHeader.setPadding(Ui.dp(c, 4), Ui.dp(c, 8), Ui.dp(c, 4), Ui.dp(c, 8));
        body.addView(itemsHeader);

        LinearLayout rows = new LinearLayout(c);
        rows.setOrientation(LinearLayout.VERTICAL);
        body.addView(rows);

        for (Item item : receipt.items) add(c, rows, item);

        if (receipt.paymentOnly() && receipt.total != null) {
            Button payment = Ui.button(c, "품목 없이 전표 총액 적용", Ui.COLOR_PRIMARY, Color.WHITE, () -> {
                Item item = new Item("전표 결제금액 (품목 미확인)", receipt.total, 1, receipt.total);
                item.warning = "개별 품목이 아닌 전표의 결제 총액입니다. 원본을 확인하세요";
                add(c, rows, item);
                update();
            });
            body.addView(payment);
        }

        Button btnAddManual = Ui.button(c, "➕  품목 또는 할인 직접 추가", Color.parseColor("#475569"), Color.WHITE, () -> {
            Item manual = new Item("직접 입력", 0, 1, 0);
            manual.warning = "직접 입력한 금액·수량을 확인하세요";
            add(c, rows, manual);
            update();
        });
        body.addView(btnAddManual);

        // 4. Final Settlement & Action Card (Floating-style Bottom Card)
        LinearLayout actionCard = Ui.card(c);
        actionCard.setBackground(Ui.roundedRect(Color.WHITE, Ui.COLOR_PRIMARY, 14, 2));

        TextView settleTitle = Ui.text(c, "최종 정산 결과", 14);
        settleTitle.setTextColor(Ui.COLOR_TEXT_MUTED);
        actionCard.addView(settleTitle);

        summary = Ui.text(c, "", 22);
        summary.setTypeface(null, android.graphics.Typeface.BOLD);
        summary.setTextColor(Ui.COLOR_PRIMARY);
        summary.setPadding(0, Ui.dp(c, 4), 0, Ui.dp(c, 12));
        actionCard.addView(summary);

        // Notion Integration Button
        btnNotion = Ui.button(c, "📝  Notion 테이블에 적재하기", Ui.COLOR_NOTION, Color.WHITE, () -> exportToNotion(c));
        actionCard.addView(btnNotion);

        // Copy / Preview Button
        copy = Ui.button(c, "📋  비고 미리보기 및 복사", Color.parseColor("#334155"), Color.WHITE, () -> {
            new AlertDialog.Builder(c)
                .setTitle("계산 내역 확인")
                .setMessage(note)
                .setNegativeButton("돌아가기", null)
                .setPositiveButton("클립보드에 복사", (dialog, which) -> {
                    ((ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE))
                        .setPrimaryClip(ClipData.newPlainText("정산 비고", note));
                    Toast.makeText(c, "정산 내역을 복사했습니다.", Toast.LENGTH_SHORT).show();
                }).show();
        });
        actionCard.addView(copy);
        body.addView(actionCard);

        reviewed.setOnCheckedChangeListener((v, checked) -> update());

        // Raw OCR Collapsible Card
        LinearLayout rawCard = Ui.card(c);
        TextView raw = Ui.text(c, receipt.raw, 12);
        raw.setTextIsSelectable(true);
        raw.setTextColor(Color.parseColor("#475569"));
        raw.setVisibility(View.GONE);

        Button btnToggleRaw = Ui.button(c, "📜  OCR 인식 원문 보기", Color.parseColor("#64748B"), Color.WHITE, () -> {
            raw.setVisibility(raw.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });
        rawCard.addView(btnToggleRaw);
        rawCard.addView(raw);
        body.addView(rawCard);

        update();
    }

    void add(Context c, LinearLayout rows, Item item) {
        RowEditor editor = new RowEditor(c, item, this::update);
        editors.add(editor);
        rows.addView(editor);
    }

    void update() {
        long sum = 0;
        StringBuilder text = new StringBuilder(sourceSummary + "\n\n");
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
                e.result.setText("품목·금액·수량·비율을 확인하세요 (0~100%)");
                valid = false;
            }
            selected |= e.selected.isChecked();
            confirmed &= e.confirmed();
        }

        currentSelectedSum = sum;
        note = text + "\n선택 합계: " + String.format(Locale.KOREA, "%,d원", sum);

        if (valid) {
            String sumStr = String.format(Locale.KOREA, "%,d원", sum);
            summary.setText("선택 합계: " + sumStr + (confirmed ? "" : "\n(확인 필요 항목 원본 대조 요망)"));
        } else {
            summary.setText("입력값 확인 필요");
        }

        boolean canExport = valid && selected && confirmed;
        copy.setEnabled(canExport);
        btnNotion.setEnabled(canExport);
    }

    private void exportToNotion(Context c) {
        if (!notionSettings.isConfigured()) {
            new AlertDialog.Builder(c)
                .setTitle("Notion 설정 필요")
                .setMessage("노션 데이터베이스에 적재하려면 API 키와 데이터베이스 ID 설정이 필요합니다. 지금 설정하시겠습니까?")
                .setPositiveButton("설정하기", (d, w) -> notionSettings.showDialog(c, () -> exportToNotion(c)))
                .setNegativeButton("취소", null)
                .show();
            return;
        }

        // Extract title & date from receipt raw text
        String title = extractStoreTitle(receipt.raw);
        String date = extractReceiptDate(receipt.raw);

        ProgressDialog progress = new ProgressDialog(c);
        progress.setMessage("Notion 테이블에 적재 중입니다…");
        progress.setCancelable(false);
        progress.show();

        NotionClient.createPage(notionSettings, title, currentSelectedSum, date, note, new NotionClient.Callback() {
            @Override
            public void onSuccess(String pageUrl) {
                progress.dismiss();
                new AlertDialog.Builder(c)
                    .setTitle("🎉 Notion 적재 완료")
                    .setMessage("영수증 정산 내역이 노션 테이블에 성공적으로 추가되었습니다!\n\n• 상호: " + title + "\n• 금액: " + String.format(Locale.KOREA, "%,d원", currentSelectedSum) + "\n• 날짜: " + date)
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
        for (String line : raw.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.contains("호:") || trimmed.contains("점:") || trimmed.contains("상호")) {
                String name = trimmed.replaceFirst(".*(?:상\\s*호|가맹점명|매장명)\\s*[:：]?\\s*", "").trim();
                if (!name.isEmpty()) return name;
            }
        }
        String first = raw.split("\n")[0].trim();
        return first.isEmpty() ? "영수증 정산" : first;
    }

    private String extractReceiptDate(String raw) {
        if (raw == null) return "";
        Matcher m = Pattern.compile("(\\d{4}[-./]\\d{2}[-./]\\d{2})").matcher(raw);
        if (m.find()) return m.group(1);
        return "";
    }
}
