package kr.dutchpay;

import org.junit.Test;
import static org.junit.Assert.*;

public class NotionTest {
    @Test
    public void testDatabaseIdExtractionFromUrl() {
        String fullUrl = "https://www.notion.so/myworkspace/8a9b1c2d3e4f5a6b7c8d9e0f1a2b3c4d?v=123456";
        String id = fullUrl;
        if (id.contains("/")) {
            String path = id.substring(id.lastIndexOf('/') + 1);
            if (path.contains("?")) path = path.substring(0, path.indexOf('?'));
            id = path.replaceAll("-", "");
        }
        assertEquals("8a9b1c2d3e4f5a6b7c8d9e0f1a2b3c4d", id);
    }

    @Test
    public void testDatabaseIdExtractionWithHyphens() {
        String rawWithHyphens = "8a9b1c2d-3e4f-5a6b-7c8d-9e0f1a2b3c4d";
        assertEquals("8a9b1c2d3e4f5a6b7c8d9e0f1a2b3c4d", rawWithHyphens.replaceAll("-", ""));
    }

    @Test
    public void testDateNormalization() {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{4})[-./](\\d{2})[-./](\\d{2})")
            .matcher("결제일시: 2026/05/20 19:10:13");
        assertTrue(m.find());
        assertEquals("2026-05-20", m.group(1) + "-" + m.group(2) + "-" + m.group(3));
    }
}
