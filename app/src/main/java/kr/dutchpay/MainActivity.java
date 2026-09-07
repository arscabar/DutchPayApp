package kr.dutchpay;

import android.app.Activity;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.android.gms.tasks.*;
import com.google.mlkit.vision.text.TextRecognizer;

public class MainActivity extends Activity {
    LinearLayout body;
    TextView status;
    ProgressBar progress;
    TextRecognizer engine;
    NotionSettings notionSettings;
    int scanId;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        engine = Ocr.create();
        notionSettings = new NotionSettings(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.COLOR_BG);
        scroll.setFillViewport(true);

        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 20);
        body.setPadding(pad, pad * 2, pad, pad * 2);
        scroll.addView(body);
        setContentView(scroll);
        home();
    }

    void home() {
        body.removeAllViews();

        // 1. Top Header Bar (Title & Settings Icon)
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 0, 0, Ui.dp(this, 14));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        titles.setLayoutParams(titleLp);

        TextView appTitle = Ui.text(this, "DutchPay OCR", 24);
        appTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        titles.addView(appTitle);

        TextView appSub = Ui.text(this, "온디바이스 NPU 영수증 정산 · Notion 연동", 13);
        appSub.setTextColor(Ui.COLOR_TEXT_MUTED);
        titles.addView(appSub);
        topBar.addView(titles);

        // Notion Settings Button
        Button btnSettings = new Button(this);
        btnSettings.setText("⚙️ 노션 설정");
        btnSettings.setTextSize(12);
        btnSettings.setTextColor(Ui.COLOR_TEXT_MAIN);
        btnSettings.setBackground(Ui.roundedRect(Ui.COLOR_CARD, Ui.COLOR_STROKE, 8, 1));
        btnSettings.setPadding(Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 12), Ui.dp(this, 6));
        btnSettings.setOnClickListener(v -> notionSettings.showDialog(this, this::updateNotionStatus));
        topBar.addView(btnSettings);
        body.addView(topBar);

        // 2. Hardware Acceleration Status Badge
        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.setPadding(0, 0, 0, Ui.dp(this, 16));

        TextView npuBadge = Ui.badge(this, "⚡ NPU / 온디바이스 가속 활성화됨", Color.parseColor("#ECFDF5"), Color.parseColor("#059669"));
        badgeRow.addView(npuBadge);

        TextView spacer = new TextView(this);
        spacer.setText(" ");
        spacer.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
        badgeRow.addView(spacer);

        TextView notionBadge = Ui.badge(this,
            notionSettings.isConfigured() ? "✓ Notion 연동됨" : "Notion 미설정",
            notionSettings.isConfigured() ? Color.parseColor("#EFF6FF") : Color.parseColor("#F1F5F9"),
            notionSettings.isConfigured() ? Color.parseColor("#2563EB") : Ui.COLOR_TEXT_MUTED);
        notionBadge.setId(View.generateViewId());
        badgeRow.addView(notionBadge);
        body.addView(badgeRow);

        // 3. Hero Card with Action Button
        LinearLayout hero = Ui.card(this);
        hero.setPadding(Ui.dp(this, 20), Ui.dp(this, 24), Ui.dp(this, 20), Ui.dp(this, 24));

        TextView heroTitle = Ui.text(this, "영수증 사진을 선택하세요", 18);
        heroTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        hero.addView(heroTitle);

        TextView heroDesc = Ui.text(this,
            "오프라인 온디바이스 신경망 엔진이 영수증 내 품목명, 수량, 단가, 할인 내역을 정밀하게 추출하고 오독을 자동 보정합니다.", 14);
        heroDesc.setTextColor(Ui.COLOR_TEXT_MUTED);
        heroDesc.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 18));
        hero.addView(heroDesc);

        Button btnPick = Ui.button(this, "📷  영수증 사진 선택", Ui.COLOR_PRIMARY, Color.WHITE, () -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .setType("image/*")
                .addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, 1);
        });
        hero.addView(btnPick);
        body.addView(hero);

        // 4. Status & Progress Card
        LinearLayout statusCard = Ui.card(this);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        progress.setVisibility(View.GONE);
        statusCard.addView(progress);

        status = Ui.text(this, "대기 중 · 준비 완료", 13);
        status.setTextColor(Ui.COLOR_TEXT_MUTED);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        statusCard.addView(status);
        body.addView(statusCard);
    }

    void updateNotionStatus() {
        home();
    }

    @Override
    protected void onActivityResult(int req, int code, Intent data) {
        super.onActivityResult(req, code, data);
        if (req != 1 || code != RESULT_OK || data == null) return;
        home();
        int current = ++scanId;
        Uri uri = data.getData();

        if (progress != null) progress.setVisibility(View.VISIBLE);
        status.setText("영수증과 품목 영역을 읽고 있습니다 (NPU 가속)…");

        try {
            PaddleScan.read(this, uri).continueWithTask(task -> {
                if (task.isSuccessful()) return Tasks.forResult(task.getResult());
                if (current != scanId || isDestroyed()) return Tasks.forException(new IllegalStateException("Scan was replaced"));
                status.setText("한국어 OCR을 다시 읽고 있습니다…");
                return Scan.read(this, uri, engine).continueWith(fallback -> {
                    if (!fallback.isSuccessful()) throw fallback.getException();
                    Receipt r = fallback.getResult();
                    r.method = "ML Kit 대체 인식 · " + r.method;
                    return r;
                });
            }).addOnSuccessListener(this, receipt -> {
                if (current != scanId) return;
                if (progress != null) progress.setVisibility(View.GONE);
                status.setText("✓ " + receipt.method + " 완료");
                new ReceiptEditor(this, body, receipt, uri, notionSettings);
            }).addOnFailureListener(this, e -> {
                if (current == scanId) {
                    if (progress != null) progress.setVisibility(View.GONE);
                    status.setText("인식 실패: 사진을 다시 선택하세요. " + e.getMessage());
                }
            });
        } catch (Exception e) {
            if (progress != null) progress.setVisibility(View.GONE);
            status.setText("사진을 열 수 없습니다: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        engine.close();
        super.onDestroy();
    }
}
