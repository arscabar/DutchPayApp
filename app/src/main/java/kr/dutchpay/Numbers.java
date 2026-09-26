package kr.dutchpay;

import java.util.*;
import java.util.regex.*;

final class Numbers {
    static final Pattern VALUE = Pattern.compile("(?<![\\p{L}\\d])[-−]?\\d[\\d,]*(?:원|개)?(?![\\p{L}\\d])");
    static String clean(String s) {
        return s.replaceAll("(?<!\\d)-\\s*(\\d{1,3})\\s*-(?=\\s|$)", "$1")
                .replaceAll("(?<=\\d)[.,]\\s*(?=\\d{3}(?:\\D|$))", ",")
                .replaceAll("(?<=\\d)\\s*[xX×*]\\s*(?=\\d)", " ")
                .replace('−', '-').replaceAll("-\\s+(?=\\d)", "-");
    }
    static List<Long> values(String s) {
        List<Long> out = new ArrayList<>();
        Matcher m = VALUE.matcher(clean(s));
        while (m.find()) {
            String n = m.group().replaceAll("[^0-9-]", "");
            if (n.replace("-", "").length() <= 9) out.add(Long.parseLong(n));
        }
        return out;
    }
    static String name(String s) {
        String cleaned = clean(s);
        Matcher m = Pattern.compile("(?:\\t|\\s)+-?\\d[\\d,]*(?:원|개)?(?:\\s|$)").matcher(cleaned);
        return (m.find() ? cleaned.substring(0, m.start()) : cleaned).trim();
    }
}
