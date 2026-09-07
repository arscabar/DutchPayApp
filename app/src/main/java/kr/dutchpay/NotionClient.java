package kr.dutchpay;

import android.os.Handler;
import android.os.Looper;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.*;

public final class NotionClient {
    private static final String NOTION_API_URL = "https://api.notion.com/v1/pages";
    private static final String NOTION_VERSION = "2022-06-28";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(String pageUrl);
        void onError(String message);
    }

    public static void createPage(NotionSettings settings, String title, long amount, String date, String note, Callback callback) {
        if (!settings.isConfigured()) {
            callback.onError("노션 연동이 설정되지 않았습니다. 우측 상단 ⚙️ 설정에서 API Key와 데이터베이스 ID를 입력하세요.");
            return;
        }

        EXECUTOR.execute(() -> {
            HttpURLConnection conn = null;
            try {
                JSONObject payload = new JSONObject();

                // 1. Parent database
                JSONObject parent = new JSONObject();
                parent.put("database_id", settings.getDatabaseId());
                payload.put("parent", parent);

                // 2. Properties
                JSONObject properties = new JSONObject();

                // Title property
                JSONObject titleProp = new JSONObject();
                JSONArray titleArr = new JSONArray();
                JSONObject titleText = new JSONObject();
                titleText.put("text", new JSONObject().put("content", title.isEmpty() ? "영수증 정산" : title));
                titleArr.put(titleText);
                titleProp.put("title", titleArr);
                properties.put(settings.getPropTitle(), titleProp);

                // Amount (number)
                JSONObject amountProp = new JSONObject();
                amountProp.put("number", amount);
                properties.put(settings.getPropAmount(), amountProp);

                // Date property (if available, format YYYY-MM-DD)
                String formattedDate = normalizeDate(date);
                if (!formattedDate.isEmpty()) {
                    JSONObject dateProp = new JSONObject();
                    JSONObject dateVal = new JSONObject();
                    dateVal.put("start", formattedDate);
                    dateProp.put("date", dateVal);
                    properties.put(settings.getPropDate(), dateProp);
                }

                // Note / Details (rich_text, chunked by 1800 chars for Notion API limits)
                if (note != null && !note.isEmpty()) {
                    JSONObject noteProp = new JSONObject();
                    JSONArray noteArr = new JSONArray();
                    int start = 0;
                    while (start < note.length()) {
                        int end = Math.min(start + 1800, note.length());
                        JSONObject noteText = new JSONObject();
                        noteText.put("text", new JSONObject().put("content", note.substring(start, end)));
                        noteArr.put(noteText);
                        start = end;
                    }
                    noteProp.put("rich_text", noteArr);
                    properties.put(settings.getPropNote(), noteProp);
                }

                payload.put("properties", properties);

                // Execute HTTPS POST
                URL url = new URL(NOTION_API_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Bearer " + settings.getApiKey());
                conn.setRequestProperty("Notion-Version", NOTION_VERSION);
                conn.setRequestProperty("Content-Type", "application/json");

                byte[] bodyBytes = payload.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bodyBytes);
                    os.flush();
                }

                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                String responseBody = "";
                if (is != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line).append('\n');
                        responseBody = sb.toString();
                    }
                }

                if (code >= 200 && code < 300) {
                    JSONObject respJson = new JSONObject(responseBody);
                    String pageUrl = respJson.optString("url", "https://notion.so");
                    MAIN_HANDLER.post(() -> callback.onSuccess(pageUrl));
                } else {
                    String errorMsg = "노션 전송 오류 (HTTP " + code + ")";
                    try {
                        JSONObject errJson = new JSONObject(responseBody);
                        if (errJson.has("message")) errorMsg += ": " + errJson.getString("message");
                    } catch (Exception ignored) {
                        if (!responseBody.isEmpty()) errorMsg += ": " + responseBody;
                    }
                    final String finalMsg = errorMsg;
                    MAIN_HANDLER.post(() -> callback.onError(finalMsg));
                }
            } catch (Exception e) {
                final String msg = "네트워크 요청 실패: " + e.getMessage();
                MAIN_HANDLER.post(() -> callback.onError(msg));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private static String normalizeDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        }
        // Match 2026-05-20 or 2026.05.20 or 2026/05/20
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{4})[-./](\\d{2})[-./](\\d{2})").matcher(dateStr);
        if (m.find()) {
            return m.group(1) + "-" + m.group(2) + "-" + m.group(3);
        }
        return new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
    }
}
