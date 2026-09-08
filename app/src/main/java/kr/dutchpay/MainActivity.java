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
    LinearLayout mainLayout;
    ScrollView scroll;
    LinearLayout contentLayout;
    LinearLayout tab0Body, tab1Body, tab2Body;
    Button btnTab0, btnTab1, btnTab2;
    Button btnPickTop;
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

        // Main Root: Vertical LinearLayout containing Fixed Top Bar + Scrollable Content
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Ui.COLOR_BG);
        mainLayout.setFitsSystemWindows(true);

        // 1. Scrollable Body Area (화면 상단부터 채우고 스크롤 가능)
        scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.COLOR_BG);
        scroll.setFillViewport(true);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        scroll.setLayoutParams(scrollLp);

        contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        int padH = Ui.dp(this, 16);
        int padTop = Ui.dp(this, 28);
        int padBottom = Ui.dp(this, 36);
        contentLayout.setPadding(padH, padTop, padH, padBottom);
        scroll.addView(contentLayout);
        mainLayout.addView(scroll);

        // 2. Bottom Fixed Navigation Bar (화면 최하단에 항상 고정)
        LinearLayout bottomFixedBar = new LinearLayout(this);
        bottomFixedBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomFixedBar.setGravity(Gravity.CENTER_VERTICAL);
        bottomFixedBar.setBackground(Ui.roundedRect(Color.WHITE, Color.parseColor("#E2E8F0"), 0, 1));
        int barPadH = Ui.dp(this, 12);
        int barPadV = Ui.dp(this, 10);
        bottomFixedBar.setPadding(barPadH, barPadV, barPadH, barPadV);
        bottomFixedBar.setElevation(Ui.dp(this, 8));

        // Segmented Tabs Container
        LinearLayout tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setBackground(Ui.roundedRect(Color.parseColor("#F1F5F9"), Color.TRANSPARENT, 10, 0));
        int tbPad = Ui.dp(this, 3);
        tabBar.setPadding(tbPad, tbPad, tbPad, tbPad);
        LinearLayout.LayoutParams tbLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tbLp.setMargins(0, 0, Ui.dp(this, 8), 0);
        tabBar.setLayoutParams(tbLp);

        btnTab0 = createTabButton("🧾 정산 품목", 0);
        btnTab1 = createTabButton("📷 영수증 사진", 1);
        btnTab2 = createTabButton("⚙️ 설정", 2);

        tabBar.addView(btnTab0);
        tabBar.addView(btnTab1);
        tabBar.addView(btnTab2);
        bottomFixedBar.addView(tabBar);

        // Always-accessible "＋ 새 영수증" Bottom Action Button
        btnPickTop = new Button(this);
        btnPickTop.setText("＋ 새 영수증");
        btnPickTop.setTextSize(13);
        btnPickTop.setTypeface(null, android.graphics.Typeface.BOLD);
        btnPickTop.setTextColor(Color.WHITE);
        btnPickTop.setBackground(Ui.roundedRect(Ui.COLOR_PRIMARY, Ui.COLOR_PRIMARY, 10, 0));
        btnPickTop.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        btnPickTop.setOnClickListener(v -> pickReceiptPhoto());
        bottomFixedBar.addView(btnPickTop);

        mainLayout.addView(bottomFixedBar);

        setContentView(mainLayout);

        initUi();
    }

    void initUi() {
        contentLayout.removeAllViews();

        tab0Body = new LinearLayout(this);
        tab0Body.setOrientation(LinearLayout.VERTICAL);

        tab1Body = new LinearLayout(this);
        tab1Body.setOrientation(LinearLayout.VERTICAL);

        tab2Body = new LinearLayout(this);
        tab2Body.setOrientation(LinearLayout.VERTICAL);

        contentLayout.addView(tab0Body);
        contentLayout.addView(tab1Body);
        contentLayout.addView(tab2Body);

        buildTab0Initial();
        buildTab1Initial();
        buildTab2();

        switchTab(currentTab);
    }

    private Button createTabButton(String title, int tabIndex) {
        Button b = new Button(this);
        b.setText(title);
        b.setTextSize(12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> switchTab(tabIndex));
        return b;
    }

    void switchTab(int index) {
        currentTab = index;

        updateTabButtonStyle(btnTab0, index == 0);
        updateTabButtonStyle(btnTab1, index == 1);
        updateTabButtonStyle(btnTab2, index == 2);

        tab0Body.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        tab1Body.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        tab2Body.setVisibility(index == 2 ? View.VISIBLE : View.GONE);

        // 사진 탭 전환 시 맨 위로 스크롤
        scroll.smoothScrollTo(0, 0);
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
        b.setPadding(Ui.dp(this, 6), Ui.dp(this, 7), Ui.dp(this, 6), Ui.dp(this, 7));
    }

    void buildTab0Initial() {
        tab0Body.removeAllViews();

        // 넉넉하고 편안한 상단 여백과 완성도 높은 Hero 카드
        LinearLayout hero = Ui.card(this);
        hero.setPadding(Ui.dp(this, 24), Ui.dp(this, 36), Ui.dp(this, 24), Ui.dp(this, 36));
        hero.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        heroLp.setMargins(0, Ui.dp(this, 40), 0, Ui.dp(this, 18));
        hero.setLayoutParams(heroLp);

        TextView tvIcon = Ui.text(this, "🧾", 48);
        tvIcon.setGravity(Gravity.CENTER);
        hero.addView(tvIcon);

        TextView tvTitle = Ui.text(this, "영수증을 선택해 주세요", 19);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(Ui.COLOR_TEXT_MAIN);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 6));
        hero.addView(tvTitle);

        TextView tvSub = Ui.text(this, "갤러리에서 영수증 사진을 불러오면\n품목과 금액을 자동으로 인식하여 정산표를 생성합니다.", 13);
        tvSub.setTextColor(Ui.COLOR_TEXT_MUTED);
        tvSub.setGravity(Gravity.CENTER);
        tvSub.setLineSpacing(Ui.dp(this, 4), 1.0f);
        tvSub.setPadding(0, 0, 0, Ui.dp(this, 24));
        hero.addView(tvSub);

        Button btnPick = Ui.button(this, "📷  영수증 사진 선택하기", Ui.COLOR_PRIMARY, Color.WHITE, this::pickReceiptPhoto);
        btnPick.setTextSize(15);
        btnPick.setTypeface(null, android.graphics.Typeface.BOLD);
        btnPick.setPadding(Ui.dp(this, 24), Ui.dp(this, 14), Ui.dp(this, 24), Ui.dp(this, 14));
        hero.addView(btnPick);

        tab0Body.addView(hero);

        LinearLayout statusCard = Ui.card(this);
        statusCard.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 12));
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        progress.setVisibility(View.GONE);
        statusCard.addView(progress);

        status = Ui.text(this, "준비 완료", 13);
        status.setTextColor(Ui.COLOR_TEXT_MUTED);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));
        statusCard.addView(status);
        tab0Body.addView(statusCard);
    }

    void buildTab1Initial() {
        tab1Body.removeAllViews();
        LinearLayout emptyCard = Ui.card(this);
        emptyCard.setPadding(Ui.dp(this, 24), Ui.dp(this, 36), Ui.dp(this, 24), Ui.dp(this, 36));
        emptyCard.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams emptyLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        emptyLp.setMargins(0, Ui.dp(this, 40), 0, Ui.dp(this, 20));
        emptyCard.setLayoutParams(emptyLp);

        TextView tvEmptyIcon = Ui.text(this, "📷", 48);
        tvEmptyIcon.setGravity(Gravity.CENTER);
        emptyCard.addView(tvEmptyIcon);

        TextView tvEmptyTitle = Ui.text(this, "선택된 영수증이 없습니다", 17);
        tvEmptyTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvEmptyTitle.setTextColor(Ui.COLOR_TEXT_MAIN);
        tvEmptyTitle.setGravity(Gravity.CENTER);
        tvEmptyTitle.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 20));
        emptyCard.addView(tvEmptyTitle);

        Button btnSelect = Ui.button(this, "영수증 사진 선택하기", Ui.COLOR_PRIMARY, Color.WHITE, this::pickReceiptPhoto);
        btnSelect.setTextSize(15);
        btnSelect.setTypeface(null, android.graphics.Typeface.BOLD);
        btnSelect.setPadding(Ui.dp(this, 24), Ui.dp(this, 14), Ui.dp(this, 24), Ui.dp(this, 14));
        emptyCard.addView(btnSelect);
        tab1Body.addView(emptyCard);
    }

    void buildTab2() {
        tab2Body.removeAllViews();

        // 1. Notion Integration Settings Card
        LinearLayout notionCard = Ui.card(this);
        notionCard.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18));

        TextView tvNotionTitle = Ui.text(this, "📝 Notion 연동 설정", 16);
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

        TextView lblKey = Ui.text(this, "Notion API Key", 12);
        lblKey.setTypeface(null, android.graphics.Typeface.BOLD);
        lblKey.setTextColor(Color.parseColor("#475569"));
        notionCard.addView(lblKey);

        EditText etApiKey = Ui.input(this, "secret_... 또는 ntn_...", notionSettings.getApiKey(), false);
        etApiKey.setBackground(Ui.roundedRect(Color.WHITE, Ui.COLOR_STROKE, 8, 1));
        notionCard.addView(etApiKey);

        TextView lblDb = Ui.text(this, "Database ID", 12);
        lblDb.setTypeface(null, android.graphics.Typeface.BOLD);
        lblDb.setTextColor(Color.parseColor("#475569"));
        lblDb.setPadding(0, Ui.dp(this, 8), 0, 0);
        notionCard.addView(lblDb);

        EditText etDbId = Ui.input(this, "데이터베이스 32자리 ID", notionSettings.getDatabaseId(), false);
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
            testPd.setMessage("Notion 연결을 확인하고 있습니다…");
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

        private void detachFromParent(View v) {
        if (v != null && v.getParent() instanceof ViewGroup) {
            ((ViewGroup) v.getParent()).removeView(v);
        }
    }

    void pickReceiptPhoto() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "영수증 사진 선택"), 1);
        } catch (Exception e) {
            Intent fallback = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(fallback, 1);
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == 1 && res == RESULT_OK && data != null && data.getData() != null) {
            handlePhoto(data.getData());
        }
    }

    void handlePhoto(Uri uri) {
        currentUri = uri;
        final int id = ++scanId;

        try {
            getContentResolver().takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}

        tab0Body.removeAllViews();

        detachFromParent(progress);
        detachFromParent(status);

        if (progress == null) {
            progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progress.setIndeterminate(true);
        }
        progress.setVisibility(View.VISIBLE);

        if (status == null) {
            status = Ui.text(this, "", 13);
            status.setGravity(Gravity.CENTER);
        }
        status.setText("영수증을 분석하고 있습니다…");

        LinearLayout loadingCard = Ui.card(this);
        loadingCard.setPadding(Ui.dp(this, 20), Ui.dp(this, 24), Ui.dp(this, 20), Ui.dp(this, 24));
        loadingCard.addView(progress);
        loadingCard.addView(status);
        tab0Body.addView(loadingCard);

        switchTab(0);

        try {
            Scan.read(this, uri, engine).addOnCompleteListener(task -> {
                if (id != scanId) return;
                if (!task.isSuccessful() || task.getResult() == null) {
                    Exception ex = task.getException();
                    String err = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "인식 실패";
                    status.setText("인식 실패: 다시 시도해 주세요.\n(" + err + ")");
                    progress.setVisibility(View.GONE);
                    return;
                }

                Receipt receipt = task.getResult();
                currentReceipt = receipt;

                tab0Body.removeAllViews();
                try {
                    new ReceiptEditor(this, tab0Body, tab1Body, receipt, uri, notionSettings,
                        () -> switchTab(1),
                        () -> switchTab(0)
                    );
                } catch (Throwable t) {
                    t.printStackTrace();
                    new AlertDialog.Builder(this)
                        .setTitle("화면 표시 오류")
                        .setMessage("영수증 화면을 구성하는 중 오류가 발생했습니다: " + t.getMessage())
                        .setPositiveButton("확인", null)
                        .show();
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            status.setText("이미지를 불러올 수 없습니다: " + e.getMessage());
            progress.setVisibility(View.GONE);
            new AlertDialog.Builder(this)
                .setTitle("이미지 로드 오류")
                .setMessage("선택한 이미지를 처리할 수 없습니다: " + e.getMessage())
                .setPositiveButton("확인", null)
                .show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (engine != null) engine.close();
    }
}
