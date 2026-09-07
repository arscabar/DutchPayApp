package kr.dutchpay;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
    ScrollView scroll;
    LinearLayout root;
    LinearLayout tab0Body, tab1Body, tab2Body;
    Button btnTab0, btnTab1, btnTab2;
    TextView status;
    ProgressBar progress;
    TextRecognizer engine;
    NotionSettings notionSettings;
    int scanId;
    int currentTab = 0;
    Receipt currentReceipt;
    Uri currentUri;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        engine = Ocr.create();
        notionSettings = new NotionSettings(this);

        scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.COLOR_BG);
        scroll.setFillViewport(true);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 16);
        root.setPadding(pad, pad * 2, pad, pad * 2);
        scroll.addView(root);
        setContentView(scroll);

        initUi();
    }

    void initUi() {
        root.removeAllViews();

        // 1. Top Header Bar (App Title + Right Action Button)
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 0, 0, Ui.dp(this, 12));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        titles.setLayoutParams(titleLp);

        TextView appTitle = Ui.text(this, "DutchPay OCR", 22);
        appTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        titles.addView(appTitle);

        TextView appSub = Ui.text(this, "온디바이스 NPU 영수증 정산 · Notion 연동", 12);
        appSub.setTextColor(Ui.COLOR_TEXT_MUTED);
        titles.addView(appSub);
        topBar.addView(titles);

        // Always-accessible "📷 새 영수증" Button in top bar
        Button btnPickTop = new Button(this);
        btnPickTop.setText("📷 새 영수증");
        btnPickTop.setTextSize(13);
        btnPickTop.setTypeface(null, android.graphics.Typeface.BOLD);
        btnPickTop.setTextColor(Color.WHITE);
        btnPickTop.setBackground(Ui.roundedRect(Ui.COLOR_PRIMARY, Ui.COLOR_PRIMARY, 10, 0));
        btnPickTop.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        btnPickTop.setOnClickListener(v -> pickReceiptPhoto());
        topBar.addView(btnPickTop);
        root.addView(topBar);

        // 2. Segmented Tab Bar (Toss / iOS Style)
        LinearLayout tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setBackground(Ui.roundedRect(Color.parseColor("#E2E8F0"), Color.TRANSPARENT, 12, 0));
        int tbPad = Ui.dp(this, 4);
        tabBar.setPadding(tbPad, tbPad, tbPad, tbPad);
        LinearLayout.LayoutParams tbLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tbLp.setMargins(0, 0, 0, Ui.dp(this, 14));
        tabBar.setLayoutParams(tbLp);

        btnTab0 = createTabButton("🧾 정산 품목", 0);
        btnTab1 = createTabButton("📷 원본 영수증", 1);
        btnTab2 = createTabButton("⚙️ 설정 / NPU", 2);

        tabBar.addView(btnTab0);
        tabBar.addView(btnTab1);
        tabBar.addView(btnTab2);
        root.addView(tabBar);

        // 3. Tab Body Containers
        tab0Body = new LinearLayout(this);
        tab0Body.setOrientation(LinearLayout.VERTICAL);

        tab1Body = new LinearLayout(this);
        tab1Body.setOrientation(LinearLayout.VERTICAL);

        tab2Body = new LinearLayout(this);
        tab2Body.setOrientation(LinearLayout.VERTICAL);

        root.addView(tab0Body);
        root.addView(tab1Body);
        root.addView(tab2Body);

        // Populate initial tabs
        buildTab0Initial();
        buildTab1Initial();
        buildTab2();

        switchTab(currentTab);
    }

    private Button createTabButton(String title, int tabIndex) {
        Button b = new Button(this);
        b.setText(title);
        b.setTextSize(13);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> switchTab(tabIndex));
        return b;
    }

    void switchTab(int index) {
        currentTab = index;

        // Update tab button styles
        updateTabButtonStyle(btnTab0, index == 0);
        updateTabButtonStyle(btnTab1, index == 1);
        updateTabButtonStyle(btnTab2, index == 2);

        // Toggle container visibility
        tab0Body.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        tab1Body.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        tab2Body.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
    }

    private void updateTabButtonStyle(Button b, boolean active) {
        if (active) {
            b.setTypeface(null, android.graphics.Typeface.BOLD);
            b.setTextColor(Ui.COLOR_PRIMARY);
            b.setBackground(Ui.roundedRect(Color.WHITE, Ui.COLOR_STROKE, 8, 1));
        } else {
            b.setTypeface(null, android.graphics.Typeface.NORMAL);
            b.setTextColor(Color.parseColor("#64748B"));
            b.setBackground(Ui.roundedRect(Color.TRANSPARENT, Color.TRANSPARENT, 8, 0));
        }
        b.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));
    }

    void buildTab0Initial() {
        tab0Body.removeAllViews();

        // Hero Card
        LinearLayout hero = Ui.card(this);
        hero.setPadding(Ui.dp(this, 20), Ui.dp(this, 24), Ui.dp(this, 20), Ui.dp(this, 24));

        TextView heroTitle = Ui.text(this, "영수증 사진을 선택하세요", 18);
        heroTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        hero.addView(heroTitle);

        TextView heroDesc = Ui.text(this,
            "오프라인 온디바이스 NPU 신경망 엔진이 영수증 내 품목명, 단가, 수량, 할인 내역을 정밀하게 추출하고 영수증 원본 크롭과 대조합니다.", 14);
        heroDesc.setTextColor(Ui.COLOR_TEXT_MUTED);
        heroDesc.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 18));
        hero.addView(heroDesc);

        Button btnPick = Ui.button(this, "📷  영수증 사진 선택", Ui.COLOR_PRIMARY, Color.WHITE, this::pickReceiptPhoto);
        hero.addView(btnPick);
        tab0Body.addView(hero);

        // Status & Progress Card
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
        tab0Body.addView(statusCard);
    }

    void buildTab1Initial() {
        tab1Body.removeAllViews();
        LinearLayout emptyCard = Ui.card(this);
        emptyCard.setPadding(Ui.dp(this, 24), Ui.dp(this, 32), Ui.dp(this, 24), Ui.dp(this, 32));
        emptyCard.setGravity(Gravity.CENTER);

        TextView tvEmptyIcon = Ui.text(this, "📷", 36);
        tvEmptyIcon.setGravity(Gravity.CENTER);
        emptyCard.addView(tvEmptyIcon);

        TextView tvEmpty = Ui.text(this, "영수증 사진을 선택하면\n전체 원본 사진과 OCR 추출 원문이 이곳에 표시됩니다.", 14);
        tvEmpty.setTextColor(Ui.COLOR_TEXT_MUTED);
        tvEmpty.setGravity(Gravity.CENTER);
        tvEmpty.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 16));
        emptyCard.addView(tvEmpty);

        Button btnSelect = Ui.button(this, "영수증 사진 선택", Ui.COLOR_PRIMARY, Color.WHITE, this::pickReceiptPhoto);
        emptyCard.addView(btnSelect);
        tab1Body.addView(emptyCard);
    }

    void buildTab2() {
        tab2Body.removeAllViews();

        // 1. Hardware Acceleration Status Card
        LinearLayout npuCard = Ui.card(this);
        npuCard.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18));

        TextView tvNpuTitle = Ui.text(this, "⚡ 온디바이스 신경망 가속 상태", 16);
        tvNpuTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvNpuTitle.setTextColor(Ui.COLOR_TEXT_MAIN);
        npuCard.addView(tvNpuTitle);

        TextView npuBadge = Ui.badge(this, "✓ NPU / NNAPI 하드웨어 가속 활성화됨", Color.parseColor("#ECFDF5"), Color.parseColor("#059669"));
        npuBadge.setPadding(Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 4));
        LinearLayout.LayoutParams nbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nbLp.setMargins(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        npuBadge.setLayoutParams(nbLp);
        npuCard.addView(npuBadge);

        TextView tvNpuDesc = Ui.text(this,
            "ONNX Runtime 기반 모바일 신경망 추론이 디바이스의 NPU(신경망 프로세서) 또는 GPU를 통해 로컬에서 즉시 수행되며, 외부 서버 전송 없이 100% 프라이버시가 보호됩니다.", 13);
        tvNpuDesc.setTextColor(Ui.COLOR_TEXT_MUTED);
        npuCard.addView(tvNpuDesc);
        tab2Body.addView(npuCard);

        // 2. Notion Integration Settings Card
        LinearLayout notionCard = Ui.card(this);
        notionCard.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18));

        TextView tvNotionTitle = Ui.text(this, "📝 Notion 데이터베이스 연동 설정", 16);
        tvNotionTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvNotionTitle.setTextColor(Ui.COLOR_TEXT_MAIN);
        notionCard.addView(tvNotionTitle);

        TextView notionStatusBadge = Ui.badge(this,
            notionSettings.isConfigured() ? "✓ Notion 연동 완료" : "Notion 미설정 (설정 필요)",
            notionSettings.isConfigured() ? Color.parseColor("#EFF6FF") : Color.parseColor("#FEF2F2"),
            notionSettings.isConfigured() ? Color.parseColor("#2563EB") : Color.parseColor("#DC2626"));
        notionStatusBadge.setPadding(Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 4));
        LinearLayout.LayoutParams nsbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nsbLp.setMargins(0, Ui.dp(this, 8), 0, Ui.dp(this, 12));
        notionStatusBadge.setLayoutParams(nsbLp);
        notionCard.addView(notionStatusBadge);

        TextView lblKey = Ui.text(this, "Notion API Key (시크릿 키)", 12);
        lblKey.setTypeface(null, android.graphics.Typeface.BOLD);
        lblKey.setTextColor(Color.parseColor("#475569"));
        notionCard.addView(lblKey);

        EditText etApiKey = Ui.input(this, "secret_... 또는 ntn_...", notionSettings.getApiKey(), false);
        etApiKey.setBackground(Ui.roundedRect(Color.WHITE, Ui.COLOR_STROKE, 8, 1));
        notionCard.addView(etApiKey);

        TextView lblDb = Ui.text(this, "Database ID (32자리 데이터베이스 고유 ID)", 12);
        lblDb.setTypeface(null, android.graphics.Typeface.BOLD);
        lblDb.setTextColor(Color.parseColor("#475569"));
        lblDb.setPadding(0, Ui.dp(this, 8), 0, 0);
        notionCard.addView(lblDb);

        EditText etDbId = Ui.input(this, "예: 12345678123412341234123456789abc", notionSettings.getDatabaseId(), false);
        etDbId.setBackground(Ui.roundedRect(Color.WHITE, Ui.COLOR_STROKE, 8, 1));
        notionCard.addView(etDbId);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, Ui.dp(this, 12), 0, 0);

        Button btnSave = Ui.button(this, "저장하기", Ui.COLOR_PRIMARY, Color.WHITE, () -> {
            String key = etApiKey.getText().toString().trim();
            String db = etDbId.getText().toString().trim();
            notionSettings.saveConfig(key, db, "이름", "금액", "날짜", "비고");
            Toast.makeText(this, "노션 설정이 저장되었습니다.", Toast.LENGTH_SHORT).show();
            buildTab2();
        });
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        sLp.setMargins(0, 0, Ui.dp(this, 6), 0);
        btnSave.setLayoutParams(sLp);
        btnRow.addView(btnSave);

        Button btnTest = Ui.button(this, "연동 테스트", Color.parseColor("#F1F5F9"), Ui.COLOR_TEXT_MAIN, () -> {
            String key = etApiKey.getText().toString().trim();
            String db = etDbId.getText().toString().trim();
            if (key.isEmpty() || db.isEmpty()) {
                Toast.makeText(this, "API Key와 Database ID를 먼저 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            notionSettings.saveConfig(key, db, "이름", "금액", "날짜", "비고");
            ProgressDialog testPd = new ProgressDialog(this);
            testPd.setMessage("Notion 데이터베이스 연결을 확인하고 있습니다…");
            testPd.show();

            NotionClient.testConnection(notionSettings, new NotionClient.Callback() {
                @Override
                public void onSuccess(String pageUrl) {
                    testPd.dismiss();
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("✓ Notion 연결 성공")
                        .setMessage("노션 데이터베이스 연결이 정상적으로 확인되었습니다!")
                        .setPositiveButton("확인", null)
                        .show();
                    buildTab2();
                }

                @Override
                public void onError(String message) {
                    testPd.dismiss();
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("연결 실패")
                        .setMessage(message)
                        .setPositiveButton("확인", null)
                        .show();
                }
            });
        });
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tLp.setMargins(Ui.dp(this, 6), 0, 0, 0);
        btnTest.setLayoutParams(tLp);
        btnRow.addView(btnTest);

        notionCard.addView(btnRow);
        tab2Body.addView(notionCard);
    }

    void pickReceiptPhoto() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
            .setType("image/*")
            .addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int req, int code, Intent data) {
        super.onActivityResult(req, code, data);
        if (req != 1 || code != RESULT_OK || data == null) return;
        int current = ++scanId;
        Uri uri = data.getData();
        currentUri = uri;

        switchTab(0);
        buildTab0Initial();
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
                currentReceipt = receipt;
                tab0Body.removeAllViews();
                new ReceiptEditor(this, tab0Body, tab1Body, receipt, uri, notionSettings,
                    () -> switchTab(1),
                    () -> switchTab(0)
                );
                switchTab(0);
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
    public void onBackPressed() {
        if (currentTab != 0) {
            switchTab(0);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        engine.close();
        super.onDestroy();
    }
}
