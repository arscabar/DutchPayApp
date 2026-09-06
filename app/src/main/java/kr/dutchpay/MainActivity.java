package kr.dutchpay;

import android.app.Activity;
import android.os.Bundle;
import android.content.*;
import android.net.Uri;
import android.widget.*;
import com.google.android.gms.tasks.*;
import com.google.mlkit.vision.text.TextRecognizer;

public class MainActivity extends Activity {
    LinearLayout body; TextView status; TextRecognizer engine;
    int scanId;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); engine = Ocr.create();
        ScrollView scroll = new ScrollView(this);
        body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(24, 48, 24, 48); scroll.addView(body); setContentView(scroll);
        home();
    }
    void home() {
        body.removeAllViews();
        body.addView(Ui.text(this, "DutchPay · 영수증 OCR 실험", 24));
        body.addView(Ui.text(this, "사진은 휴대폰에서 인식합니다. 추출 결과는 확인·수정하세요.", 16));
        body.addView(Ui.button(this, "영수증 사진 선택", () -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*")
                .addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, 1);
        }));
        status = Ui.text(this, "한국어 모델 포함 · Notion 전송 전 단계의 실험 앱", 14);
        body.addView(status);
    }
    @Override protected void onActivityResult(int req,int code,Intent data) {
        super.onActivityResult(req,code,data);
        if (req != 1 || code != RESULT_OK || data == null) return;
        home(); int current=++scanId; Uri uri = data.getData(); status.setText("영수증과 품목 영역을 읽고 있습니다…");
        try {
            PaddleScan.read(this,uri).continueWithTask(task -> {
                if(task.isSuccessful())return Tasks.forResult(task.getResult());
                if(current!=scanId || isDestroyed())return Tasks.forException(new IllegalStateException("Scan was replaced"));
                status.setText("한국어 OCR을 다시 읽고 있습니다…");
                return Scan.read(this,uri,engine).continueWith(fallback -> {
                    if(!fallback.isSuccessful())throw fallback.getException();
                    Receipt r=fallback.getResult();r.method="ML Kit 대체 인식 · "+r.method;return r;
                });
            })
                .addOnSuccessListener(this,receipt -> {
                    if(current!=scanId)return;
                    status.setText(receipt.method+" · 모든 항목을 확인하세요");
                    new ReceiptEditor(this, body, receipt, uri);
                }).addOnFailureListener(this,e -> {if(current==scanId)status.setText("인식 실패: 사진을 다시 선택하세요. " + e.getMessage());});
        } catch (Exception e) { status.setText("사진을 열 수 없습니다: " + e.getMessage()); }
    }
    @Override protected void onDestroy() { engine.close(); super.onDestroy(); }
}
