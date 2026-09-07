package kr.dutchpay;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.*;

public final class NotionSettings {
    private static final String PREF_NAME = "notion_config";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_DATABASE_ID = "database_id";
    private static final String KEY_PROP_TITLE = "prop_title";
    private static final String KEY_PROP_AMOUNT = "prop_amount";
    private static final String KEY_PROP_DATE = "prop_date";
    private static final String KEY_PROP_NOTE = "prop_note";

    private final SharedPreferences prefs;

    public NotionSettings(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getApiKey() {
        return prefs.getString(KEY_API_KEY, "").trim();
    }

    public String getDatabaseId() {
        String id = prefs.getString(KEY_DATABASE_ID, "").trim();
        // Notion database ID may be a full URL (https://www.notion.so/.../DATABASE_ID?v=...)
        // Extract 32-char hex string if URL is entered
        if (id.contains("/")) {
            String path = id.substring(id.lastIndexOf('/') + 1);
            if (path.contains("?")) path = path.substring(0, path.indexOf('?'));
            if (path.length() >= 32) return path.replaceAll("-", "");
        }
        return id.replaceAll("-", "");
    }

    public String getPropTitle() {
        return prefs.getString(KEY_PROP_TITLE, "이름").trim();
    }

    public String getPropAmount() {
        return prefs.getString(KEY_PROP_AMOUNT, "금액").trim();
    }

    public String getPropDate() {
        return prefs.getString(KEY_PROP_DATE, "날짜").trim();
    }

    public String getPropNote() {
        return prefs.getString(KEY_PROP_NOTE, "비고").trim();
    }

    public boolean isConfigured() {
        return !getApiKey().isEmpty() && !getDatabaseId().isEmpty();
    }

    public void saveConfig(String apiKey, String dbId, String title, String amount, String date, String note) {
        prefs.edit()
            .putString(KEY_API_KEY, apiKey)
            .putString(KEY_DATABASE_ID, dbId)
            .putString(KEY_PROP_TITLE, title == null || title.isEmpty() ? "이름" : title)
            .putString(KEY_PROP_AMOUNT, amount == null || amount.isEmpty() ? "금액" : amount)
            .putString(KEY_PROP_DATE, date == null || date.isEmpty() ? "날짜" : date)
            .putString(KEY_PROP_NOTE, note == null || note.isEmpty() ? "비고" : note)
            .apply();
    }

    public void showDialog(Context context, Runnable onSaved) {
        ScrollView scroll = new ScrollView(context);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(context, 20);
        layout.setPadding(pad, pad, pad, pad);
        scroll.addView(layout);

        TextView intro = new TextView(context);
        intro.setText("영수증 계산 결과를 노션 데이터베이스 테이블의 새 행으로 자동 적재합니다.");
        intro.setTextColor(Color.parseColor("#475569"));
        intro.setTextSize(14);
        intro.setPadding(0, 0, 0, Ui.dp(context, 16));
        layout.addView(intro);

        EditText etApiKey = createInputField(context, layout, "Notion API 통합 시크릿 토큰 (secret_...)", getApiKey(), false);
        EditText etDbId = createInputField(context, layout, "데이터베이스 ID (32자리 또는 노션 링크)", prefs.getString(KEY_DATABASE_ID, ""), false);

        TextView colIntro = new TextView(context);
        colIntro.setText("데이터베이스 컬럼명 매핑 (노션 테이블의 열 이름)");
        colIntro.setTextColor(Color.parseColor("#1E293B"));
        colIntro.setTextSize(14);
        colIntro.setTypeface(null, android.graphics.Typeface.BOLD);
        colIntro.setPadding(0, Ui.dp(context, 12), 0, Ui.dp(context, 8));
        layout.addView(colIntro);

        EditText etTitle = createInputField(context, layout, "제목 열 (기본: 이름)", getPropTitle(), false);
        EditText etAmount = createInputField(context, layout, "숫자 금액 열 (기본: 금액)", getPropAmount(), false);
        EditText etDate = createInputField(context, layout, "날짜 열 (기본: 날짜)", getPropDate(), false);
        EditText etNote = createInputField(context, layout, "비고/상세 열 (기본: 비고)", getPropNote(), false);

        new AlertDialog.Builder(context)
            .setTitle("Notion 테이블 연동 설정")
            .setView(scroll)
            .setPositiveButton("저장", (dialog, which) -> {
                prefs.edit()
                    .putString(KEY_API_KEY, etApiKey.getText().toString().trim())
                    .putString(KEY_DATABASE_ID, etDbId.getText().toString().trim())
                    .putString(KEY_PROP_TITLE, etTitle.getText().toString().trim().isEmpty() ? "이름" : etTitle.getText().toString().trim())
                    .putString(KEY_PROP_AMOUNT, etAmount.getText().toString().trim().isEmpty() ? "금액" : etAmount.getText().toString().trim())
                    .putString(KEY_PROP_DATE, etDate.getText().toString().trim().isEmpty() ? "날짜" : etDate.getText().toString().trim())
                    .putString(KEY_PROP_NOTE, etNote.getText().toString().trim().isEmpty() ? "비고" : etNote.getText().toString().trim())
                    .apply();
                Toast.makeText(context, "노션 연동 설정이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                if (onSaved != null) onSaved.run();
            })
            .setNegativeButton("취소", null)
            .show();
    }

    private EditText createInputField(Context c, LinearLayout parent, String hint, String value, boolean isPassword) {
        TextView label = new TextView(c);
        label.setText(hint);
        label.setTextSize(12);
        label.setTextColor(Color.parseColor("#64748B"));
        label.setPadding(0, Ui.dp(c, 4), 0, Ui.dp(c, 2));
        parent.addView(label);

        EditText et = new EditText(c);
        et.setText(value);
        et.setHint(hint);
        et.setTextSize(14);
        et.setSingleLine(true);
        et.setBackground(Ui.roundedRect(Color.parseColor("#F8FAFC"), Color.parseColor("#CBD5E1"), 8, 1));
        int pad = Ui.dp(c, 10);
        et.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, Ui.dp(c, 10));
        et.setLayoutParams(lp);
        parent.addView(et);
        return et;
    }
}
