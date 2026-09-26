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
                String prefTitle = settings.getPropTitle();
                String prefAmount = settings.getPropAmount();
                String prefDate = settings.getPropDate();
                String prefNote = settings.getPropNote();
                String prefCat = settings.getPropCategory();

                String titleCol = prefTitle.isEmpty() ? "title" : prefTitle;
                String amountCol = prefAmount;
                String dateCol = prefDate;
                String noteCol = prefNote;
                String catCol = prefCat;
                String catType = "multi_select";
                String placeCol = null;

                // Dynamically discover database properties if possible
                try {
                    URL dbUrl = new URL("https://api.notion.com/v1/databases/" + settings.getDatabaseId());
                    HttpURLConnection dbConn = (HttpURLConnection) dbUrl.openConnection();
                    dbConn.setRequestMethod("GET");
                    dbConn.setConnectTimeout(8000);
                    dbConn.setReadTimeout(8000);
                    dbConn.setRequestProperty("Authorization", "Bearer " + settings.getApiKey().trim());
                    dbConn.setRequestProperty("Notion-Version", NOTION_VERSION);
                    if (dbConn.getResponseCode() == 200) {
                        try (BufferedReader r = new BufferedReader(new InputStreamReader(dbConn.getInputStream(), StandardCharsets.UTF_8))) {
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = r.readLine()) != null) sb.append(line);
                            JSONObject dbJson = new JSONObject(sb.toString());
                            JSONObject props = dbJson.optJSONObject("properties");
                            if (props != null) {
                                String foundTitle = null;
                                String foundAmount = null, fallbackAmount = null;
                                String foundDate = null, fallbackDate = null;
                                String foundCat = null, fallbackCat = null;
                                String foundCatType = null, fallbackCatType = null;
                                String foundNote = null;
                                String foundPlace = null;

                                Iterator<String> keys = props.keys();
                                while (keys.hasNext()) {
                                    String k = keys.next();
                                    JSONObject pObj = props.optJSONObject(k);
                                    if (pObj == null) continue;
                                    String type = pObj.optString("type");
                                    String lower = k.toLowerCase(Locale.ROOT);
                                    if ("title".equals(type)) {
                                        foundTitle = k;
                                    } else if ("number".equals(type)) {
                                        if (fallbackAmount == null) fallbackAmount = k;
                                        if (k.equals(prefAmount) || k.contains("금액") || k.contains("비용") || k.contains("가격")
                                            || k.contains("합계") || k.contains("지출") || k.contains("결제")
                                            || lower.contains("amount") || lower.contains("price") || lower.contains("cost") || lower.contains("total")) {
                                            foundAmount = k;
                                        }
                                    } else if ("date".equals(type)) {
                                        if (fallbackDate == null) fallbackDate = k;
                                        if (k.equals(prefDate) || k.contains("날짜") || k.contains("일시") || k.contains("일자")
                                            || k.contains("결제일") || k.contains("거래일") || k.contains("사용일") || lower.contains("date")) {
                                            foundDate = k;
                                        }
                                    } else if ("select".equals(type) || "multi_select".equals(type) || "status".equals(type)) {
                                        if (fallbackCat == null && !"status".equals(type)) {
                                            fallbackCat = k;
                                            fallbackCatType = type;
                                        }
                                        if (k.equals(prefCat) || k.contains("범주") || k.contains("카테고리") || k.contains("분류")
                                            || k.contains("구분") || k.contains("종류") || k.contains("태그") || k.contains("항목")
                                            || lower.contains("category") || lower.contains("type") || lower.contains("tag")) {
                                            foundCat = k;
                                            foundCatType = type;
                                        }
                                    } else if ("rich_text".equals(type)) {
                                        if (k.equals(prefNote) || k.contains("비고") || k.contains("내역") || k.contains("메모")
                                            || k.contains("상세") || k.contains("내용") || k.contains("품목") || k.contains("설명")
                                            || lower.contains("note") || lower.contains("memo") || lower.contains("detail")) {
                                            foundNote = k;
                                        } else if (k.contains("장소") || k.contains("상호") || k.contains("매장") || k.contains("가맹점")) {
                                            foundPlace = k;
                                        } else if (k.equals(prefCat) && foundCat == null) {
                                            foundCat = k;
                                            foundCatType = "rich_text";
                                        }
                                    }
                                }

                                // Only send columns that actually exist in this Notion database schema
                                titleCol = (foundTitle != null) ? foundTitle : "title";
                                amountCol = (foundAmount != null) ? foundAmount : fallbackAmount;
                                dateCol = (foundDate != null) ? foundDate : fallbackDate;
                                catCol = (foundCat != null) ? foundCat : fallbackCat;
                                if (catCol != null) {
                                    catType = (foundCat != null) ? foundCatType : fallbackCatType;
                                }
                                noteCol = foundNote;
                                placeCol = foundPlace;

                                settings.saveConfig(
                                    settings.getApiKey(),
                                    settings.getDatabaseId(),
                                    titleCol,
                                    amountCol != null ? amountCol : prefAmount,
                                    dateCol != null ? dateCol : prefDate,
                                    noteCol != null ? noteCol : prefNote,
                                    catCol != null ? catCol : prefCat
                                );
                            }
                        }
                    }
                    dbConn.disconnect();
                } catch (Exception ignored) {}

                // Send request with automatic type/property correction if needed
                sendWithRetry(settings, title, amount, date, note, category,
                    titleCol, amountCol, dateCol, noteCol, catCol, catType, placeCol, true, callback, 4);

            } catch (Exception e) {
                final String msg = "네트워크 요청 실패: " + e.getMessage();
                MAIN_HANDLER.post(() -> callback.onError(msg));
            }
        });
    }

    private static void sendWithRetry(NotionSettings settings, String title, long amount, String date, String note, String category,
                                      String titleCol, String amountCol, String dateCol, String noteCol, String catCol, String catType, String placeCol,
                                      boolean includeChildren, Callback callback, int retryCount) {
        HttpURLConnection conn = null;
        try {
            JSONObject payload = new JSONObject();

            // 1. Parent database
            JSONObject parent = new JSONObject();
            parent.put("database_id", settings.getDatabaseId());
            payload.put("parent", parent);

            // 2. Properties
            JSONObject properties = new JSONObject();

            // Title property (e.g. 목록 or 이름 or "title")
            String effectiveTitleCol = (titleCol == null || titleCol.isEmpty()) ? "title" : titleCol;
            JSONObject titleProp = new JSONObject();
            JSONArray titleArr = new JSONArray();
            JSONObject titleText = new JSONObject();
            titleText.put("text", new JSONObject().put("content", (title == null || title.isEmpty()) ? "영수증 정산" : title));
            titleArr.put(titleText);
            titleProp.put("title", titleArr);
            properties.put(effectiveTitleCol, titleProp);

            // Amount (number)
            if (amountCol != null && !amountCol.isEmpty()) {
                JSONObject amountProp = new JSONObject();
                amountProp.put("number", amount);
                properties.put(amountCol, amountProp);
            }

            // Place (rich_text, if exists)
            if (placeCol != null && !placeCol.isEmpty() && title != null && !title.isEmpty()) {
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

            // Category (supports multi_select, select, status, rich_text)
            if (catCol != null && !catCol.isEmpty() && category != null && !category.isEmpty()) {
                JSONObject catProp = new JSONObject();
                if ("select".equals(catType)) {
                    catProp.put("select", new JSONObject().put("name", category));
                } else if ("status".equals(catType)) {
                    catProp.put("status", new JSONObject().put("name", category));
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

            // Note / Details property (rich_text, chunked by 1800 chars for Notion API limits)
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

            // 3. Page Body Children Blocks (품목 상세 내역을 노션 페이지 본문 블록으로도 저장)
            if (includeChildren && note != null && !note.trim().isEmpty()) {
                JSONArray children = buildPageChildren(note);
                if (children.length() > 0) {
                    payload.put("children", children);
                }
            }

            // Execute HTTPS POST
            URL url = new URL(NOTION_API_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "Bearer " + settings.getApiKey().trim());
            conn.setRequestProperty("Notion-Version", NOTION_VERSION);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

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
                // Automatic property/type recovery on HTTP 400 validation errors
                if (retryCount > 0 && code == 400) {
                    if (responseBody.contains("is expected to be multi_select")) {
                        sendWithRetry(settings, title, amount, date, note, category,
                            effectiveTitleCol, amountCol, dateCol, noteCol, catCol, "multi_select", placeCol, includeChildren, callback, retryCount - 1);
                        return;
                    } else if (responseBody.contains("is expected to be select")) {
                        sendWithRetry(settings, title, amount, date, note, category,
                            effectiveTitleCol, amountCol, dateCol, noteCol, catCol, "select", placeCol, includeChildren, callback, retryCount - 1);
                        return;
                    } else if (responseBody.contains("is expected to be status")) {
                        sendWithRetry(settings, title, amount, date, note, category,
                            effectiveTitleCol, amountCol, dateCol, noteCol, catCol, "status", placeCol, includeChildren, callback, retryCount - 1);
                        return;
                    } else if (responseBody.contains("is expected to be rich_text")) {
                        sendWithRetry(settings, title, amount, date, note, category,
                            effectiveTitleCol, amountCol, dateCol, noteCol, catCol, "rich_text", placeCol, includeChildren, callback, retryCount - 1);
                        return;
                    } else if (responseBody.contains("is not a property that exists")) {
                        String nextTitle = effectiveTitleCol;
                        String nextAmount = amountCol;
                        String nextDate = dateCol;
                        String nextNote = noteCol;
                        String nextCat = catCol;
                        String nextPlace = placeCol;
                        boolean changed = false;

                        if (noteCol != null && responseBody.contains(noteCol)) { nextNote = null; changed = true; }
                        if (catCol != null && responseBody.contains(catCol)) { nextCat = null; changed = true; }
                        if (placeCol != null && responseBody.contains(placeCol)) { nextPlace = null; changed = true; }
                        if (dateCol != null && responseBody.contains(dateCol)) { nextDate = null; changed = true; }
                        if (amountCol != null && responseBody.contains(amountCol)) { nextAmount = null; changed = true; }
                        if (!"title".equals(effectiveTitleCol) && responseBody.contains(effectiveTitleCol)) {
                            nextTitle = "title";
                            changed = true;
                        }
                        if (!changed) {
                            // Strip non-essential columns and use universal "title" property key
                            nextTitle = "title";
                            nextNote = null;
                            nextCat = null;
                            nextPlace = null;
                        }
                        sendWithRetry(settings, title, amount, date, note, category,
                            nextTitle, nextAmount, nextDate, nextNote, nextCat, catType, nextPlace, includeChildren, callback, retryCount - 1);
                        return;
                    } else if (retryCount > 1) {
                        // Fallback to universal "title" + amount + date + page body children
                        sendWithRetry(settings, title, amount, date, note, category,
                            "title", amountCol, dateCol, null, null, catType, null, includeChildren, callback, 1);
                        return;
                    } else if (retryCount == 1) {
                        // Ultimate fallback: universal "title" only + page body children
                        sendWithRetry(settings, title, amount, date, note, category,
                            "title", null, null, null, null, catType, null, false, callback, 0);
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

    private static JSONArray buildPageChildren(String note) throws JSONException {
        JSONArray children = new JSONArray();
        String[] lines = note.split("\n");
        int count = 0;
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;
            if (line.length() > 1800) line = line.substring(0, 1800);
            boolean isItemLine = line.contains("×") || line.contains("=") || line.startsWith("•") || line.startsWith("-");
            String cleanText = line.replaceFirst("^[•\\-]\\s*", "");
            String blockType = isItemLine ? "bulleted_list_item" : "paragraph";

            JSONObject block = new JSONObject();
            block.put("object", "block");
            block.put("type", blockType);

            JSONArray richText = new JSONArray();
            JSONObject textObj = new JSONObject();
            textObj.put("type", "text");
            textObj.put("text", new JSONObject().put("content", cleanText));
            richText.put(textObj);

            block.put(blockType, new JSONObject().put("rich_text", richText));
            children.put(block);
            if (++count >= 80) break;
        }
        return children;
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
