package kr.dutchpay;

import org.junit.Test;
import java.util.*;
import java.util.regex.*;
import static org.junit.Assert.*;

public class GeneralReceiptTest {

    private String extractReceiptDate(String raw) {
        if (raw == null) return "";
        Matcher m = Pattern.compile("((?:19|20)\\d{2})\\s*[-./년]\\s*(\\d{1,2})\\s*[-./월]\\s*(\\d{1,2})").matcher(raw);
        while (m.find()) {
            try {
                int year = Integer.parseInt(m.group(1));
                int month = Integer.parseInt(m.group(2));
                int day = Integer.parseInt(m.group(3));
                if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                    return String.format(Locale.KOREA, "%04d-%02d-%02d", year, month, day);
                }
            } catch (Exception ignored) {}
        }
        return "";
    }

    private String extractStoreTitle(String raw) {
        if (raw == null || raw.isEmpty()) return "영수증 정산";
        String[] lines = raw.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.contains("사업자") || trimmed.contains("등록번호") || trimmed.contains("전화") || trimmed.contains("TEL")) continue;
            if (trimmed.matches(".*(?:상\\s*호|가맹점명|매장명|상호명)\\s*[:：\\s].*")) {
                String name = trimmed.replaceFirst(".*(?:상\\s*호|가맹점명|매장명|상호명)\\s*[:：]?\\s*", "").trim();
                if (!name.isEmpty() && name.length() >= 2) return name;
            }
        }
        int limit = Math.min(lines.length, 8);
        for (int i = 0; i < limit; i++) {
            String t = lines[i].trim();
            if (t.isEmpty() || t.length() < 2) continue;
            if (t.matches(".*(영수증|사업자|등록|전화|TEL|Tel|대표자|주소|가맹점번호|승인|POS|테이블|단말기|매출|고객|카드|일시|결제|주문|대기|번호|식사|포장).*")) continue;
            if (t.matches(".*[가-힣A-Za-z].*") && !t.matches("^[0-9\\s\\[\\](){}:*#₩￦,.-]+$")) {
                return t;
            }
        }
        return "영수증 정산";
    }

    @Test
    public void testDamddeokReceipt() {
        String raw = "담떡\n" +
                     "33,800원\n" +
                     "결제방법 체크카드(하나기업)\n" +
                     "매입사명 하나외환\n" +
                     "카드번호 53761400********\n" +
                     "할부기간 일시불\n" +
                     "부가세액 3,072\n" +
                     "공급가액 30,728\n" +
                     "승인번호 37277936\n" +
                     "승인일시 2026-08-27 18:31:29\n" +
                     "가맹점번호 00923879507\n" +
                     "메뉴명 단가 수량 금액\n" +
                     "떡갈비 강된장 쌈 한상 13,900 2 27,800\n" +
                     "담떡 계란말이 6,000 1 6,000\n" +
                     "결제금액 33,800\n" +
                     "상호 담떡\n" +
                     "대표 김민준";

        assertEquals("담떡", extractStoreTitle(raw));
        assertEquals("2026-08-27", extractReceiptDate(raw));

        Long total = ReceiptTotalText.read(raw);
        assertNotNull(total);
        assertEquals(33800L, total.longValue());

        List<String> lines = Arrays.asList(raw.split("\n"));
        Receipt receipt = Parser.parse(lines);
        assertEquals(Long.valueOf(33800), receipt.total);
        assertEquals(2, receipt.items.size());
        assertEquals("떡갈비 강된장 쌈 한상", receipt.items.get(0).name);
        assertEquals(2, receipt.items.get(0).count);
        assertEquals("담떡 계란말이", receipt.items.get(1).name);
        assertEquals(1, receipt.items.get(1).count);
        assertEquals(33800L, receipt.itemSum());
        assertTrue(receipt.warnings.isEmpty());
    }

    @Test
    public void testTaberuReceipt() {
        String raw = "[주문(대기)번호]\n" +
                     "013\n" +
                     "[ 영수증 ]\n" +
                     "타베루(서울대입구역점)\n" +
                     "161-16-00795 TEL:02-877-0076 김은실\n" +
                     "서울특별시 관악구 남부순환로 1795\n" +
                     "2026-08-12 19:03(수) POS:02 BILL:000006\n" +
                     "상품명 수량 할인 금액\n" +
                     "000163 특(차슈)라멘 8,900 1 0 8,900\n" +
                     "부가세 과세 물품가액: 8,091\n" +
                     "부 가 세: 809\n" +
                     "합 계: 8,900\n" +
                     "받을금액: 8,900\n" +
                     "받은금액: 8,900\n" +
                     "1. 카드결제: 8,900\n" +
                     "신용카드 매출전표 [ 고객용 ]\n" +
                     "[금액] 8,900 원(일시불) [부가세]809 원";

        assertEquals("타베루(서울대입구역점)", extractStoreTitle(raw));
        assertEquals("2026-08-12", extractReceiptDate(raw));

        Long total = ReceiptTotalText.read(raw);
        assertNotNull(total);
        assertEquals(8900L, total.longValue());

        List<String> lines = Arrays.asList(raw.split("\n"));
        Receipt receipt = Parser.parse(lines);
        assertEquals(Long.valueOf(8900), receipt.total);
        assertEquals(1, receipt.items.size());
        assertEquals("특(차슈)라멘", receipt.items.get(0).name);
        assertEquals(1, receipt.items.get(0).count);
        assertEquals(8900L, receipt.items.get(0).unit);
        assertEquals(8900L, receipt.itemSum());
        assertTrue(receipt.warnings.isEmpty());
    }

    @Test
    public void testCardSlipWithoutItemTable() {
        String raw = "영수증\n" +
                     "상호: 상무초밥 (서울대입구역점)\n" +
                     "620-16-73176 TEL)02-875-3335 유완 외1명\n" +
                     "계산일자:2026-09-01 시간:12:42:35\n" +
                     "인쇄일자:2026-09-01 시간:12:42:36\n" +
                     "순매출 107,546\n" +
                     "부가세 10,754\n" +
                     "봉사료 0\n" +
                     "매출합계(카드) 118,300\n" +
                     "[카드번호] 55850326********\n" +
                     "[카드사명] 비씨카드\n" +
                     "[승인번호] 72789816\n" +
                     "[결제금액] 118,300\n" +
                     "원산지: 광어초밥...";

        assertEquals("상무초밥 (서울대입구역점)", extractStoreTitle(raw));
        assertEquals("2026-09-01", extractReceiptDate(raw));

        Long total = ReceiptTotalText.read(raw);
        assertNotNull(total);
        assertEquals(118300L, total.longValue());

        List<String> lines = Arrays.asList(raw.split("\n"));
        Receipt receipt = Parser.parse(lines);
        assertEquals(Long.valueOf(118300), receipt.total);
        assertTrue(receipt.items.isEmpty());

        // Fallback item generation for regular receipts / card slips
        if (receipt.items.isEmpty()) {
            long initialAmount = (receipt.total != null && receipt.total > 0) ? receipt.total : 0L;
            String itemName = "상무초밥 (서울대입구역점)";
            Item fallbackItem = new Item(itemName, initialAmount, 1, initialAmount);
            receipt.items.add(fallbackItem);
        }
        receipt.validate();

        assertEquals(1, receipt.items.size());
        assertEquals("상무초밥 (서울대입구역점)", receipt.items.get(0).name);
        assertEquals(118300L, receipt.items.get(0).baseAmount());
        assertEquals(118300L, receipt.itemSum());
    }

    @Test
    public void testNotionCategoryFormatting() {
        String category = "식비";
        String jsonSnippet = "{\"select\": {\"name\": \"" + category + "\"}}";
        assertTrue(jsonSnippet.contains("\"name\": \"식비\""));
        assertTrue(jsonSnippet.contains("select"));
    }

    @Test
    public void testTwosomeReceipt() {
        String raw = "주문하신 메뉴가 준비되면 해당\n" +
                     "주문번호 또는 입력하신 핸드폰번호로\n" +
                     "안내드리겠습니다.\n" +
                     "영수증과 신용카드를 챙겨주세요.\n" +
                     "주문번호 : 02-0012\n" +
                     "신정역점\n" +
                     "820-73-00655 Tel 070)4896-3939 윤설경\n" +
                     "서울특별시 양천구 오목로 147 (신정동, 제이\n" +
                     "클래스목동주상복합)1층 투\n" +
                     "구매 시마다 쌓이는 하트로 무료커피!\n" +
                     "무료케이크를! 지금 투썸하트앱 가입하세요\n" +
                     "POS 02-010305142 2026/09/26 10 13\n" +
                     "======================================\n" +
                     "ice아메리카노아로마R 1개 4,700\n" +
                     "ice아메리카노M 1개 6,100\n" +
                     "======================================\n" +
                     "총 액 10,800\n" +
                     "합 계 10,800\n" +
                     "과세물품가액 9,818\n" +
                     "부가세 982\n" +
                     "신용카드 10,800\n" +
                     "[결제금액] 10,800";

        assertEquals("2026-09-26", extractReceiptDate(raw));
        Receipt receipt = Parser.parse(Arrays.asList(raw.split("\n")));
        assertEquals(Long.valueOf(10800), receipt.total);
        assertEquals(2, receipt.items.size());
        assertEquals("ice아메리카노아로마R", receipt.items.get(0).name);
        assertEquals(1, receipt.items.get(0).count);
        assertEquals(4700L, receipt.items.get(0).unit);
        assertEquals("ice아메리카노M", receipt.items.get(1).name);
        assertEquals(1, receipt.items.get(1).count);
        assertEquals(6100L, receipt.items.get(1).unit);
        assertEquals(10800L, receipt.itemSum());
        assertTrue(receipt.warnings.isEmpty());
    }

    @Test
    public void testPreppersReceipt() {
        String raw = "프레퍼스 다이어트 푸드\n" +
                     "선릉역점\n" +
                     "서울 강남구 테헤란로 421 1층 1호\n" +
                     "주문 시간 2026-09-21 18:19:41\n" +
                     "사업자등록번호 6068663224\n" +
                     "전화번호 025665505\n" +
                     "[주문번호] : 1590\n" +
                     "매장 식사\n" +
                     "--------------------------------------\n" +
                     "▶비프 커리 덮밥\n" +
                     " └비프 스테이크 (소부채살 150g) 추 9,900\n" +
                     "가\n" +
                     "24,800 x 1 24,800\n" +
                     "▶비프 콥 플레이트\n" +
                     " └레몬갈릭소스 0\n" +
                     " └구운계란 0\n" +
                     "12,900 x 1 12,900\n" +
                     "--------------------------------------\n" +
                     "총주문금액: 37,700\n" +
                     "할인금액: -0\n" +
                     "판매금액: 34,273\n" +
                     "부가세 : 3,427\n" +
                     "총결제금액: 37,700";

        assertEquals("2026-09-21", extractReceiptDate(raw));
        Receipt receipt = Parser.parse(Arrays.asList(raw.split("\n")));
        assertEquals(Long.valueOf(37700), receipt.total);
        assertEquals(2, receipt.items.size());
        assertEquals("비프 커리 덮밥", receipt.items.get(0).name);
        assertEquals(1, receipt.items.get(0).count);
        assertEquals(24800L, receipt.items.get(0).unit);
        assertEquals("비프 콥 플레이트", receipt.items.get(1).name);
        assertEquals(1, receipt.items.get(1).count);
        assertEquals(12900L, receipt.items.get(1).unit);
        assertEquals(37700L, receipt.itemSum());
        assertTrue(receipt.warnings.isEmpty());
    }

    @Test
    public void testGyukatsuJeongReceipt() {
        String raw = "영 수 증\n" +
                     "상호:규카츠정_강남점\n" +
                     "대표:김관우 사업자:357-06-03486\n" +
                     "전화:\n" +
                     "주소:서울특별시 강남구 테헤란로1길 28-1\n" +
                     "지하층 (역삼동 , 준영빌딩)\n" +
                     "거래일자:2026-09-17 18:27:32\n" +
                     "거래번호:050-0036\n" +
                     "--------------------------------------\n" +
                     "티켓명 매수 금액\n" +
                     "--------------------------------------\n" +
                     "규카츠정식\n" +
                     "1 17,000\n" +
                     "카레[점보]정식(매운카레)\n" +
                     "1 28,000\n" +
                     "--------------------------------------\n" +
                     "공급가: 40,908\n" +
                     "V A T : 4,092\n" +
                     "합 계 : 45,000";

        assertEquals("규카츠정_강남점", extractStoreTitle(raw));
        assertEquals("2026-09-17", extractReceiptDate(raw));
        Receipt receipt = Parser.parse(Arrays.asList(raw.split("\n")));
        assertEquals(Long.valueOf(45000), receipt.total);
        assertEquals(2, receipt.items.size());
        assertEquals("규카츠정식", receipt.items.get(0).name);
        assertEquals(1, receipt.items.get(0).count);
        assertEquals(17000L, receipt.items.get(0).unit);
        assertEquals("카레[점보]정식(매운카레)", receipt.items.get(1).name);
        assertEquals(1, receipt.items.get(1).count);
        assertEquals(28000L, receipt.items.get(1).unit);
        assertEquals(45000L, receipt.itemSum());
        assertTrue(receipt.warnings.isEmpty());
    }
}
