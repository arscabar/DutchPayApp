package kr.dutchpay;

import android.content.*;
import android.net.Uri;
import android.widget.*;
import java.util.*;

final class ReceiptEditor {
    final List<RowEditor> editors = new ArrayList<>();
    final TextView summary;
    final Button copy;
    final CheckBox reviewed;
    final String sourceSummary;
    String note = "";
    ReceiptEditor(Context c, LinearLayout body, Receipt receipt, Uri uri) {
        receipt.validate();
        String sum;
        try { sum=receipt.itemSum()+"원"; } catch (ArithmeticException e) { sum="계산 불가"; }
        sourceSummary="원본 인쇄 합계: "+(receipt.total==null?"미인식":receipt.total+"원")
            +"\n원본 추출 합계: "+sum;
        body.addView(Ui.text(c,sourceSummary,16));
        body.addView(Ui.button(c, "원본 사진 보기", () -> {
            try { c.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"image/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)); }
            catch (ActivityNotFoundException e) { Toast.makeText(c,"사진 보기 앱이 없습니다",0).show(); }
        }));
        for (String w : receipt.warnings) body.addView(Ui.text(c, "확인: " + w, 16));
        reviewed=new CheckBox(c); reviewed.setText("원본의 전체 금액과 할인 내역을 확인했습니다");
        reviewed.setVisibility(receipt.warnings.isEmpty()?8:0); body.addView(reviewed);
        summary = Ui.text(c, "", 20); body.addView(summary);
        copy = Ui.button(c, "비고 미리보기·복사", () -> new android.app.AlertDialog.Builder(c)
            .setTitle("계산 내역 확인").setMessage(note).setNegativeButton("돌아가기",null)
            .setPositiveButton("확인하고 복사", (dialog, which) -> {
            ((ClipboardManager)c.getSystemService(Context.CLIPBOARD_SERVICE))
                .setPrimaryClip(ClipData.newPlainText("비고",note));
            Toast.makeText(c,"비고를 복사했습니다",0).show();
        }).show()); body.addView(copy);
        reviewed.setOnCheckedChangeListener((v, checked) -> update());
        LinearLayout rows = new LinearLayout(c); rows.setOrientation(1); body.addView(rows);
        for (Item item : receipt.items) add(c, rows, item);
        if(receipt.paymentOnly() && receipt.total!=null) {
            Button payment=new Button(c);payment.setText("품목 없이 전표 총액 적용");body.addView(payment);
            payment.setOnClickListener(v -> {
                Item item=new Item("전표 결제금액 (품목 미확인)",receipt.total,1,receipt.total);
                item.warning="개별 품목이 아닌 전표의 결제 총액입니다. 원본을 확인하세요";
                add(c,rows,item);payment.setEnabled(false);update();
            });
        }
        body.addView(Ui.button(c,"품목 또는 할인 직접 추가", () -> {
            Item manual=new Item("직접 입력",0,1,0); manual.warning="직접 입력한 금액·수량을 확인하세요";
            add(c, rows,manual); update();
        }));
        TextView raw = Ui.text(c,receipt.raw,13); raw.setTextIsSelectable(true); raw.setVisibility(8);
        body.addView(Ui.button(c,"OCR 원문 펼치기/접기", () -> raw.setVisibility(raw.getVisibility()==0?8:0)));
        body.addView(raw); update();
    }
    void add(Context c, LinearLayout rows, Item item) {
        RowEditor editor = new RowEditor(c,item,this::update); editors.add(editor); rows.addView(editor);
    }
    void update() {
        long sum=0; StringBuilder text=new StringBuilder(sourceSummary+"\n\n"); boolean valid=true;
        boolean confirmed=reviewed.getVisibility()!=0 || reviewed.isChecked(), selected=false;
        for (RowEditor e : editors) {
            try { sum=Math.addExact(sum,e.amount()); if(e.selected.isChecked() || e.source.includedDiscount) text.append(e.note()).append('\n'); }
            catch (RuntimeException ex) { e.result.setText("품목·금액·수량·비율을 확인하세요 (0~100%)"); valid=false; }
            selected|=e.selected.isChecked(); confirmed&=e.confirmed();
        }
        note=text + "\n선택 합계: " + sum + "원";
        summary.setText(valid ? "선택 합계: " + sum + "원"+(confirmed?"":"\n확인 필요 항목을 원본과 대조하세요") : "입력값 확인 필요");
        copy.setEnabled(valid && selected && confirmed);
    }
}
