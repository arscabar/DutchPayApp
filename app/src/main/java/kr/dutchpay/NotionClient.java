package kr.dutchpay;

import android.os.Handler;
import android.os.Looper;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
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

    public static void testConnection(NotionSettings settings, Callback callback) {
        createPage(settings, "DutchPay OCR 연동 테스트", 0, "", "노션 데이터베이스 연동 테스트 성공입니다.", "식비", callback);
    }

    public static void createPage(NotionSettings settings, String title, long amount, String date, String note, String category, Callback callback) {
        if (!settings.isConfigured()) {
            callback.onError("노션 연동이 설정되지 않았습니다. ⚙️ 설정에서 API Key와 데이터베이스 ID를 입력하세요.");
            return;
        }

        EXECUTOR.execute(() -> {
            try {
                String titleCol = settings.getPropTitle();
                String amountCol = settings.getPropAmount();
                String dateCol = settings.getPropDate();
                String noteCol = settings.getPropNote();
                String catCol = settings.getPropCategory();
                String catType = "multi_select"; // Default to multi_select (standard in most expense templates)
                String placeCol = null;

                // Dynamically discover database properties if possible
                try {
                    URL dbUrl = new URL("https://api.notion.com/v1/databases/" + settings.getDatabaseId());
                    HttpURLConnection dbConn = (HttpURLConnection) dbUrl.openConnection();
                    dbConn.setRequestMethod("GET");
                    dbConn.setConnectTimeout(8000);
                    dbConn.setReadTimeout(8000);
                    dbConn.setRequestProperty("Authorization", "Bearer " + settings.getApiKey());
                    dbConn.setRequestProperty("Notion-Version", NOTION_VERSION);
                    if (dbConn.getResponseCode() == 200) {
                        try (BufferedReader r = new BufferedReader(new InputStreamReader(dbConn.getInputStream(), StandardCharsets.UTF_8))) {
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = r.readLine()) != null) sb.append(line);
                            JSONObject dbJson = new JSONObject(sb.toString());
                            JSONObject props = dbJson.optJSONObject("properties");
                            if (props != null) {
                                Iterator<String> keys = props.keys();
                                while (keys.hasNext()) {
                                    String k = keys.next();
                                    JSONObject pObj = props.optJSONObject(k);
                                    if (pObj == null) continue;
                                    String type = pObj.optString("type");
                                    if ("title".equals(type)) {
                                        titleCol = k;
                                    } else if ("number".equals(type) && (amountCol.isEmpty() || "금액".equals(amountCol) || k.contains("금액") || k.contains("비용") || k.contains("가격"))) {
                                        amountCol = k;
                                    } else if ("date".equals(type) && (dateCol.isEmpty() || "날짜".equals(dateCol) || k.contains("날짜") || k.contains("일시"))) {
                                        dateCol = k;
                                    } else if (("select".equals(type) || "multi_select".equals(type)) && (catCol.isEmpty() || "범주".equals(catCol) || k.contains("범주") || k.contains("카테고리") || k.contains("분류"))) {
                                        catCol = k;
                                        catType = type;
                                    } else if ("rich_text".equals(type)) {
                                        if (k.contains("비고") || k.contains("내역") || k.contains("메모")) {
                                            noteCol = k;
                                        } else if (k.contains("장소") || k.contains("상호") || k.contains("매장")) {
                                            placeCol = k;
                                        }
                                    }
                                }
                                if (props.has(catCol)) {
                                    catType = props.optJSONObject(catCol).optString("type", catType);
                                }
                                settings.saveConfig(settings.getApiKey(), settings.getDatabaseId(), titleCol, amountCol, dateCol, noteCol, catCol);
                            }
                        }
                    }
                    dbConn.disconnect();
                } catch (Exception ignored) {}

                // Send request with automatic type correction if needed
                sendWithRetry(settings, title, amount, date, note, category, titleCol, amountCol, dateCol, noteCol, catCol, catType, placeCol, callback, 1);

            } catch (Exception e) {
                final String msg = "네트워크 요청 실패: " + e.getMessage();
                MAIN_HANDLER.post(() -> callback.onError(msg));
            }
        });
    }

    private static void sendWithRetry(NotionSettings settings, String title, long amount, String date, String note, String category,
                                      String titleCol, String amountCol, String dateCol, String noteCol, String catCol, String catType, String placeCol,
                                      Callback callback, int retryCount) {
        HttpURLConnection conn = null;
        try {
            JSONObject payload = new JSONObject();

            // 1. Parent database
            JSONObject parent = new JSONObject();
            parent.put("database_id", settings.getDatabaseId());
            payload.put("parent", parent);

            // 2. Properties
            JSONObject properties = new JSONObject();

            // Title property (e.g. 목록 or 이름)
            JSONObject titleProp = new JSONObject();
            JSONArray titleArr = new JSONArray();
            JSONObject titleText = new JSONObject();
            titleText.put("text", new JSONObject().put("content", title.isEmpty() ? "영수증 정산" : title));
            titleArr.put(titleText);
            titleProp.put("title", titleArr);
            properties.put(titleCol, titleProp);

            // Amount (number)
            if (amountCol != null && !amountCol.isEmpty()) {
                JSONObject amountProp = new JSONObject();
                amountProp.put("number", amount);
                properties.put(amountCol, amountProp);
            }

            // Place (rich_text, if exists)
            if (placeCol != null && !placeCol.isEmpty() && !title.isEmpty()) {
                JSONObject placeProp = new JSONObject();
                JSONArray placeArr = new JSONArray();
                JSONObject placeText = new JSONObject();
                placeText.put("text", new JSONObject().put("content", title));
                placeArr.put(placeText);
                placeProp.put("rich_text", placeArr);
                properties.put(placeCol, placeProp);
            }

            // Date property (if available, format YYYY-MM-DD)
            if (dateCol != null && !dateCol.isEmpty()) {
                String formattedDate = normalizeDate(date);
                if (!formattedDate.isEmpty()) {
                    JSONObject dateProp = new JSONObject();
                    JSONObject dateVal = new JSONObject();
                    dateVal.put("start", formattedDate);
                    dateProp.put("date", dateVal);
                    properties.put(dateCol, dateProp);
                }
            }

            // Category (supports multi_select, select, rich_text)
            if (catCol != null && !catCol.isEmpty() && category != null && !category.isEmpty()) {
                JSONObject catProp = new JSONObject();
                if ("select".equals(catType)) {
                    catProp.put("select", new JSONObject().put("name", category));
                } else if ("rich_text".equals(catType)) {
                    JSONArray catArr = new JSONArray();
                    JSONObject catText = new JSONObject();
                    catText.put("text", new JSONObject().put("content", category));
                    catArr.put(catText);
                    catProp.put("rich_text", catArr);
                } else {
                    // multi_select
                    JSONArray msArr = new JSONArray();
                    msArr.put(new JSONObject().put("name", category));
                    catProp.put("multi_select", msArr);
                }
                properties.put(catCol, catProp);
            }

            // Note / Details (rich_text, chunked by 1800 chars for Notion API limits)
            if (noteCol != null && !noteCol.isEmpty() && note != null && !note.isEmpty()) {
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
                properties.put(noteCol, noteProp);
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
                // Check if error is due to property type mismatch and retry
                if (retryCount > 0 && code == 400) {
                    if (responseBody.contains("is expected to be multi_select")) {
                        sendWithRetry(settings, title, amount, date, note, category, titleCol, amountCol, dateCol, noteCol, catCol, "multi_select", placeCol, callback, retryCount - 1);
                        return;
                    } else if (responseBody.contains("is expected to be select")) {
                        sendWithRetry(settings, title, amount, date, note, category, titleCol, amountCol, dateCol, noteCol, catCol, "select", placeCol, callback, retryCount - 1);
                        return;
                    } else if (responseBody.contains("is expected to be rich_text")) {
                        sendWithRetry(settings, title, amount, date, note, category, titleCol, amountCol, dateCol, noteCol, catCol, "rich_text", placeCol, callback, retryCount - 1);
                        return;
                    }
                }

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
    }

    private static String normalizeDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{4})[-./](\\d{2})[-./](\\d{2})").matcher(dateStr);
        if (m.find()) {
            return m.group(1) + "-" + m.group(2) + "-" + m.group(3);
        }
        return new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
    }
}
